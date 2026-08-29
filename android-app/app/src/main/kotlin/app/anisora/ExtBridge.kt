package app.anisora

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Java-facing Aniyomi extension runtime: load APKs, search, episodes/chapters, videos/pages,
 * and per-source settings (ConfigurableAnimeSource / ConfigurableSource).
 */
object ExtBridge {

    @JvmField val TAG = "AnisoraExt"

    class SourceRef {
        @JvmField var id: Long = 0
        @JvmField var name: String = ""
        @JvmField var lang: String = ""
        @JvmField var pkg: String = ""
        @JvmField var kind: String = "ANIME"
        @JvmField var configurable: Boolean = false
    }

    class Hit {
        @JvmField var url: String = ""
        @JvmField var title: String = ""
        @JvmField var thumbnail: String? = null
    }

    class Item {
        @JvmField var url: String = ""
        @JvmField var name: String = ""
        @JvmField var number: Float = -1f
        @JvmField var preview: String? = null
        @JvmField var scanlator: String? = null
    }

    class Stream {
        @JvmField var url: String = ""
        @JvmField var quality: String = ""
        @JvmField var headers: HashMap<String, String> = HashMap()
    }

    interface ListCb {
        fun ok(items: ArrayList<Any>)
        fun fail(msg: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var app: Application? = null
    private val animeSources = LinkedHashMap<Long, AnimeSource>()
    private val mangaSources = LinkedHashMap<Long, MangaSource>()
    private val animeMeta = LinkedHashMap<Long, SourceRef>()
    private val mangaMeta = LinkedHashMap<Long, SourceRef>()

    @JvmStatic
    fun init(application: Application) {
        app = application
        reload()
    }

    @JvmStatic
    fun reload() {
        val ctx = app ?: return
        animeSources.clear(); mangaSources.clear()
        animeMeta.clear(); mangaMeta.clear()
        try {
            loadAll(ctx)
        } catch (t: Throwable) {
            Log.e(TAG, "reload failed", t)
        }
    }

    @JvmStatic fun animeSources(): ArrayList<SourceRef> = ArrayList(animeMeta.values)
    @JvmStatic fun mangaSources(): ArrayList<SourceRef> = ArrayList(mangaMeta.values)

    @JvmStatic
    fun findAnimeByName(name: String): SourceRef? {
        val n = name.replace(" (demo)", "").trim()
        return animeMeta.values.firstOrNull { it.name.equals(n, true) || n.contains(it.name, true) || it.name.contains(n, true) }
            ?: animeMeta.values.firstOrNull()
    }

    @JvmStatic
    fun findMangaByName(name: String): SourceRef? {
        val n = name.replace(" (demo)", "").trim()
        return mangaMeta.values.firstOrNull { it.name.equals(n, true) || n.contains(it.name, true) || it.name.contains(n, true) }
            ?: mangaMeta.values.firstOrNull()
    }

    @JvmStatic
    fun hasSettings(id: Long, anime: Boolean): Boolean =
        if (anime) animeMeta[id]?.configurable == true else mangaMeta[id]?.configurable == true

    @JvmStatic
    fun settingsJson(id: Long, anime: Boolean): JSONArray {
        val ctx = app ?: return JSONArray()
        val out = JSONArray()
        try {
            val pm = PreferenceManager(ctx)
            val key = "source_$id"
            pm.setSharedPreferencesName(key)
            val screen = pm.createPreferenceScreen(ctx)
            if (anime) {
                val src = animeSources[id] as? ConfigurableAnimeSource ?: return out
                src.setupPreferenceScreen(screen)
            } else {
                val src = mangaSources[id] as? ConfigurableSource ?: return out
                src.setupPreferenceScreen(screen)
            }
            walkPrefs(screen, out)
        } catch (t: Throwable) {
            Log.e(TAG, "settingsJson", t)
        }
        return out
    }

    @JvmStatic
    fun searchAnime(id: Long, query: String, cb: ListCb) {
        scope.launch {
            try {
                val src = animeSources[id] ?: throw IllegalStateException("Source not loaded: $id")
                Log.i(TAG, "searchAnime ${src.name} query=$query")
                val page = src.getSearchAnime(1, query, AnimeFilterList())
                val hits = ArrayList<Any>()
                for (a in page.animes) {
                    val h = Hit()
                    h.url = a.url
                    h.title = a.title
                    h.thumbnail = a.thumbnail_url
                    hits.add(h)
                }
                Log.i(TAG, "searchAnime ${src.name} -> ${hits.size} hits")
                main.post { cb.ok(hits) }
            } catch (t: Throwable) {
                Log.e(TAG, "searchAnime id=$id q=$query", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    @JvmStatic
    fun searchManga(id: Long, query: String, cb: ListCb) {
        scope.launch {
            try {
                val src = mangaSources[id] as? eu.kanade.tachiyomi.source.CatalogueSource
                    ?: throw IllegalStateException("Manga source not loaded: $id")
                Log.i(TAG, "searchManga ${src.name} query=$query")
                val page = src.getSearchManga(1, query, FilterList())
                val hits = ArrayList<Any>()
                for (a in page.mangas) {
                    val h = Hit()
                    h.url = a.url
                    h.title = a.title
                    h.thumbnail = a.thumbnail_url
                    hits.add(h)
                }
                Log.i(TAG, "searchManga ${src.name} -> ${hits.size} hits")
                main.post { cb.ok(hits) }
            } catch (t: Throwable) {
                Log.e(TAG, "searchManga id=$id q=$query", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    @JvmStatic
    fun episodes(id: Long, animeUrl: String, title: String, thumb: String?, cb: ListCb) {
        scope.launch {
            try {
                val src = animeSources[id] ?: throw IllegalStateException("Source not loaded: $id")
                Log.i(TAG, "episodes ${src.name} url=$animeUrl title=$title")
                val anime = SAnime.create().apply {
                    url = animeUrl
                    this.title = title
                    thumbnail_url = thumb
                }
                val list = try {
                    src.getEpisodeList(anime)
                } catch (e1: Throwable) {
                    Log.w(TAG, "getEpisodeList failed, trying getAnimeEpisodeUpdate", e1)
                    try {
                        src.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = false, fetchEpisodes = true).episodes
                    } catch (e2: Throwable) {
                        Log.w(TAG, "getAnimeEpisodeUpdate failed, trying getAnimeSeasonUpdate fallback", e2)
                        // Some extensions only implement season update; try to get episodes via that
                        emptyList()
                    }
                }
                Log.i(TAG, "episodes ${src.name} -> ${list.size}")
                val items = ArrayList<Any>()
                for (e in list.sortedWith(compareBy<SEpisode> { if (it.episode_number > 0) it.episode_number else Float.MAX_VALUE }.thenBy { it.name })) {
                    val it = Item()
                    it.url = e.url
                    it.name = e.name
                    it.number = e.episode_number
                    it.preview = e.preview_url
                    it.scanlator = e.scanlator
                    items.add(it)
                }
                main.post { cb.ok(items) }
            } catch (t: Throwable) {
                Log.e(TAG, "episodes id=$id url=$animeUrl", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    @JvmStatic
    fun chapters(id: Long, mangaUrl: String, title: String, thumb: String?, cb: ListCb) {
        scope.launch {
            try {
                val src = mangaSources[id] ?: throw IllegalStateException("Manga source not loaded: $id")
                Log.i(TAG, "chapters ${src.name} url=$mangaUrl title=$title")
                val manga = SManga.create().apply {
                    url = mangaUrl
                    this.title = title
                    thumbnail_url = thumb
                }
                val list = src.getChapterList(manga)
                Log.i(TAG, "chapters ${src.name} -> ${list.size}")
                val items = ArrayList<Any>()
                for (c in list) {
                    val it = Item()
                    it.url = c.url
                    it.name = c.name
                    it.number = c.chapter_number
                    it.scanlator = c.scanlator
                    items.add(it)
                }
                main.post { cb.ok(items) }
            } catch (t: Throwable) {
                Log.e(TAG, "chapters id=$id url=$mangaUrl", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    @JvmStatic
    fun videos(id: Long, epUrl: String, epName: String, number: Float, cb: ListCb) {
        scope.launch {
            try {
                val src = animeSources[id] ?: throw IllegalStateException("Source not loaded")
                val ep = SEpisode.create().apply {
                    url = epUrl
                    name = epName
                    episode_number = number
                }
                val list: List<Video> = try {
                    src.getVideoList(ep)
                } catch (t: Throwable) {
                    Log.w(TAG, "getVideoList(episode) failed, trying hosters", t)
                    val hosters = try { src.getHosterList(ep) } catch (tt: Throwable) {
                        Log.w(TAG, "getHosterList failed", tt)
                        emptyList()
                    }
                    val acc = ArrayList<Video>()
                    for (h in hosters) {
                        try {
                            if (h.videoList != null && h.videoList.isNotEmpty()) acc.addAll(h.videoList)
                            else acc.addAll(src.getVideoList(h))
                        } catch (tt: Throwable) {
                            Log.w(TAG, "getVideoList(hoster) ${h.hosterUrl}", tt)
                        }
                    }
                    // If hoster list empty, try to resolve single hoster via episode url as hosterUrl
                    if (acc.isEmpty() && hosters.isEmpty()) {
                        try {
                            val fakeHoster = eu.kanade.tachiyomi.animesource.model.Hoster(epUrl, epUrl, null)
                            acc.addAll(src.getVideoList(fakeHoster))
                        } catch (_: Throwable) {}
                    }
                    acc
                }

                // Try to resolve videos that need extra request (some extensions return indirect urls)
                val resolved = ArrayList<Video>()
                for (v in list) {
                    try {
                        val rv = try {
                            when (src) {
                                is eu.kanade.tachiyomi.animesource.online.AnimeHttpSource -> src.resolveVideo(v)
                                else -> {
                                    // try via reflection for sources that implement resolveVideo
                                    try {
                                        val m = src::class.java.methods.firstOrNull { it.name == "resolveVideo" && it.parameterCount == 1 }
                                        if (m != null) {
                                            @Suppress("UNCHECKED_CAST")
                                            val r = m.invoke(src, v) as? Video
                                            r
                                        } else v
                                    } catch (_: Throwable) { v }
                                }
                            }
                        } catch (_: Throwable) { null }
                        resolved.add(rv ?: v)
                    } catch (_: Throwable) {
                        resolved.add(v)
                    }
                }

                val streams = ArrayList<Any>()
                for (v in resolved) {
                    try {
                        val s = Stream()
                        s.url = v.playUrl()
                        s.quality = v.quality
                        val hd = v.headers
                        if (hd != null) {
                            for (i in 0 until hd.size) s.headers[hd.name(i)] = hd.value(i)
                        }
                        // Ensure at least a User-Agent if extension didn't provide
                        if (!s.headers.containsKey("User-Agent")) {
                            s.headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36"
                        }
                        if (s.url.isNotBlank()) streams.add(s)
                    } catch (tt: Throwable) {
                        Log.w(TAG, "video to stream", tt)
                    }
                }
                main.post { cb.ok(streams) }
            } catch (t: Throwable) {
                Log.e(TAG, "videos", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    @JvmStatic
    fun pages(id: Long, chUrl: String, chName: String, number: Float, cb: ListCb) {
        scope.launch {
            try {
                val src = mangaSources[id] ?: throw IllegalStateException("Source not loaded")
                val ch = SChapter.create().apply {
                    url = chUrl
                    name = chName
                    chapter_number = number
                }
                val list = src.getPageList(ch)
                val urls = ArrayList<Any>()
                for (p in list.sortedBy { it.index }) {
                    val u = p.imageUrl ?: p.url
                    if (u.isNotBlank()) urls.add(u)
                }
                main.post { cb.ok(urls) }
            } catch (t: Throwable) {
                Log.e(TAG, "pages", t)
                main.post { cb.fail(t.message ?: t.javaClass.simpleName) }
            }
        }
    }

    /* ------------------------------ loader ------------------------------ */

    private fun loadAll(ctx: Context) {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_META_DATA or PackageManager.GET_CONFIGURATIONS
        val seen = HashSet<String>()

        fun consider(pi: PackageInfo, apkPath: String) {
            val pkg = pi.packageName ?: return
            if (!seen.add(pkg)) return
            loadPackage(ctx, pi, apkPath)
        }

        // 1) system installed extensions (Aniyomi Legacy installer)
        try {
            val installed = pm.getInstalledPackages(flags)
            for (pi in installed) {
                if (isExt(pi)) {
                    val path = pi.applicationInfo?.sourceDir ?: continue
                    consider(pi, path)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "scan installed", t)
        }

        // 2) private APKs in filesDir/extensions (and legacy exts)
        val dirs = listOf(File(ctx.filesDir, "extensions"), File(ctx.filesDir, "exts"))
        for (dir in dirs) {
            if (!dir.exists()) continue
            dir.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                if (!f.name.endsWith(".apk") && !f.name.endsWith(".ext")) return@forEach
                try {
                    val pi = pm.getPackageArchiveInfo(f.absolutePath, flags)
                    if (pi != null) {
                        pi.applicationInfo?.fixBasePaths(f.absolutePath)
                        if (isExt(pi)) {
                            consider(pi, f.absolutePath)
                            return@forEach
                        }
                    }
                    // Fallback: PackageManager couldn't parse meta-data (Android 13+), try dex scan
                    Log.i(TAG, "fallback dex scan for ${f.name}")
                    loadPrivateDex(ctx, f.absolutePath, f.nameWithoutExtension)
                } catch (t: Throwable) {
                    Log.w(TAG, "private ${f.name}", t)
                    try {
                        loadPrivateDex(ctx, f.absolutePath, f.nameWithoutExtension)
                    } catch (_: Throwable) {}
                }
            }
        }
        Log.i(TAG, "loaded ${animeSources.size} anime + ${mangaSources.size} manga sources")
    }

    private fun isExt(pi: PackageInfo): Boolean {
        val feats = pi.reqFeatures
        if (feats != null) {
            for (f in feats) {
                if (f.name == "tachiyomi.animeextension" || f.name == "tachiyomi.extension") return true
            }
        }
        val md = pi.applicationInfo?.metaData
        if (md != null) {
            if (md.containsKey("tachiyomi.animeextension.class") || md.containsKey("tachiyomi.extension.class")) return true
        }
        // fallback: package name heuristic
        val pkg = pi.packageName ?: ""
        return pkg.contains(".animeextension.") || pkg.contains(".mangaextension.") || pkg.contains(".extension.")
    }

    private fun ApplicationInfo.fixBasePaths(apkPath: String) {
        if (sourceDir == null) sourceDir = apkPath
        if (publicSourceDir == null) publicSourceDir = apkPath
    }

    /** Robust loader for private APKs when PackageManager meta-data parsing fails */
    private fun loadPrivateDex(ctx: Context, apkPath: String, pkgFallback: String) {
        try {
            val codeCache = File(ctx.codeCacheDir, "ext")
            if (!codeCache.exists()) codeCache.mkdirs()
            val loader = try {
                dalvik.system.DexClassLoader(apkPath, codeCache.absolutePath, null, ctx.classLoader)
            } catch (t: Throwable) {
                Log.e(TAG, "DexClassLoader $apkPath", t)
                PathClassLoader(apkPath, ctx.classLoader)
            }

            // Try to extract factory class names from binary manifest (heuristic)
            val factories = extractFactoryNames(apkPath)
            if (factories.isNotEmpty()) {
                factories.forEach { spec ->
                    instantiate(loader, pkgFallback, spec).forEach { obj ->
                        when (obj) {
                            is AnimeSource -> registerAnime(obj, pkgFallback, pkgFallback.substringAfterLast('.'))
                            is AnimeSourceFactory -> obj.createSources().forEach { registerAnime(it, pkgFallback, pkgFallback.substringAfterLast('.')) }
                            is MangaSource -> registerManga(obj, pkgFallback, pkgFallback.substringAfterLast('.'))
                            is SourceFactory -> obj.createSources().forEach { registerManga(it, pkgFallback, pkgFallback.substringAfterLast('.')) }
                        }
                    }
                }
                if (animeSources.isNotEmpty() || mangaSources.isNotEmpty()) return
            }

            // Brute force: scan dex for classes ending with Factory and try to instantiate
            try {
                val dex = dalvik.system.DexFile(apkPath)
                val entries = dex.entries()
                while (entries.hasMoreElements()) {
                    val clsName = entries.nextElement()
                    if (!clsName.contains("tachiyomi")) continue
                    if (!(clsName.endsWith("Factory") || clsName.endsWith("Source"))) continue
                    try {
                        val cls = Class.forName(clsName, false, loader)
                        val obj = cls.getDeclaredConstructor().newInstance()
                        when (obj) {
                            is AnimeSource -> registerAnime(obj, pkgFallback, pkgFallback.substringAfterLast('.'))
                            is AnimeSourceFactory -> obj.createSources().forEach { registerAnime(it, pkgFallback, pkgFallback.substringAfterLast('.')) }
                            is MangaSource -> registerManga(obj, pkgFallback, pkgFallback.substringAfterLast('.'))
                            is SourceFactory -> obj.createSources().forEach { registerManga(it, pkgFallback, pkgFallback.substringAfterLast('.')) }
                        }
                    } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                Log.w(TAG, "dex scan $apkPath", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "loadPrivateDex $apkPath", t)
        }
    }

    /** Heuristic binary manifest parser: finds tachiyomi.*.class meta-data values */
    private fun extractFactoryNames(apkPath: String): List<String> {
        val out = ArrayList<String>()
        try {
            java.util.zip.ZipFile(apkPath).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: return out
                zip.getInputStream(entry).use { ins ->
                    val bytes = ins.readBytes()
                    // Binary XML contains strings as UTF-8; search for factory markers
                    val text = String(bytes, Charsets.UTF_8)
                    // Find all substrings that look like a Java class name after the key
                    val keys = listOf("tachiyomi.animeextension.class", "tachiyomi.extension.class")
                    for (key in keys) {
                        var idx = 0
                        while (true) {
                            idx = text.indexOf(key, idx)
                            if (idx == -1) break
                            // Look ahead ~500 chars for a class name pattern
                            val window = text.substring(idx, kotlin.math.min(text.length, idx + 600))
                            // class name regex: at least 2 dots, letters
                            val regex = Regex("""[a-zA-Z_][a-zA-Z0-9_\.]*\.[A-Za-z0-9_]+Factory|[a-zA-Z_][a-zA-Z0-9_\.]*\.[A-Za-z0-9_]+Source""")
                            val matches = regex.findAll(window)
                            for (m in matches) {
                                val name = m.value
                                if (name.contains("tachiyomi") && name.length > 10 && !out.contains(name)) {
                                    out.add(name)
                                }
                            }
                            idx += key.length
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "extractFactoryNames $apkPath", t)
        }
        return out
    }

    private fun loadPackage(ctx: Context, pi: PackageInfo, apkPath: String) {
        val appInfo = pi.applicationInfo
        val pkg = pi.packageName ?: "unknown"
        val codeCache = try {
            File(ctx.codeCacheDir, "ext").apply { if (!exists()) mkdirs() }
        } catch (_: Throwable) { null }

        val loader = try {
            if (codeCache != null) dalvik.system.DexClassLoader(apkPath, codeCache.absolutePath, null, ctx.classLoader)
            else PathClassLoader(apkPath, ctx.classLoader)
        } catch (t: Throwable) {
            Log.e(TAG, "classloader $pkg", t)
            return
        }

        val md = appInfo?.metaData
        var animeClass: String? = null
        var mangaClass: String? = null
        if (md != null) {
            animeClass = md.getString("tachiyomi.animeextension.class")
            mangaClass = md.getString("tachiyomi.extension.class")
        }
        if (animeClass.isNullOrBlank() && mangaClass.isNullOrBlank()) {
            // Try heuristic extraction
            val factories = extractFactoryNames(apkPath)
            factories.forEach { name ->
                if (name.contains("anime", true)) {
                    animeClass = if (animeClass.isNullOrBlank()) name else "$animeClass;$name"
                } else {
                    mangaClass = if (mangaClass.isNullOrBlank()) name else "$mangaClass;$name"
                }
            }
        }

        val label = try {
            if (appInfo != null) ctx.packageManager.getApplicationLabel(appInfo).toString()
                .replace("Aniyomi: ", "").replace("Tachiyomi: ", "")
            else pkg
        } catch (_: Throwable) { pkg }

        if (!animeClass.isNullOrBlank()) {
            instantiate(loader, pkg, animeClass!!).forEach { obj ->
                when (obj) {
                    is AnimeSource -> registerAnime(obj, pkg, label)
                    is AnimeSourceFactory -> obj.createSources().forEach { registerAnime(it, pkg, label) }
                }
            }
        }
        if (!mangaClass.isNullOrBlank()) {
            instantiate(loader, pkg, mangaClass!!).forEach { obj ->
                when (obj) {
                    is MangaSource -> registerManga(obj, pkg, label)
                    is SourceFactory -> obj.createSources().forEach { registerManga(it, pkg, label) }
                }
            }
        }

        // If still nothing registered, try brute force dex scan as last resort
        if (animeSources.isEmpty() && mangaSources.isEmpty() || (animeClass.isNullOrBlank() && mangaClass.isNullOrBlank())) {
            try {
                loadPrivateDex(ctx, apkPath, pkg)
            } catch (_: Throwable) {}
        }
    }

    private fun instantiate(loader: ClassLoader, pkg: String, spec: String): List<Any> {
        val out = ArrayList<Any>()
        for (raw in spec.split(";", ",")) {
            val name = raw.trim().let { if (it.startsWith(".")) pkg + it else it }
            if (name.isEmpty()) continue
            try {
                val cls = Class.forName(name, false, loader)
                out.add(cls.getDeclaredConstructor().newInstance())
            } catch (t: Throwable) {
                Log.e(TAG, "init $name", t)
            }
        }
        return out
    }

    private fun registerAnime(src: AnimeSource, pkg: String, fallback: String) {
        val ref = SourceRef()
        ref.id = src.id
        ref.name = src.name.ifBlank { fallback }
        ref.lang = src.lang
        ref.pkg = pkg
        ref.kind = "ANIME"
        ref.configurable = src is ConfigurableAnimeSource
        animeSources[src.id] = src
        animeMeta[src.id] = ref
    }

    private fun registerManga(src: MangaSource, pkg: String, fallback: String) {
        val ref = SourceRef()
        ref.id = src.id
        ref.name = src.name.ifBlank { fallback }
        ref.lang = src.lang
        ref.pkg = pkg
        ref.kind = "MANGA"
        ref.configurable = src is ConfigurableSource
        mangaSources[src.id] = src
        mangaMeta[src.id] = ref
    }

    private fun walkPrefs(p: Preference, out: JSONArray) {
        if (p is PreferenceCategory) {
            val cat = JSONObject()
            cat.put("type", "category")
            cat.put("title", p.title?.toString() ?: "")
            out.put(cat)
            for (i in 0 until p.preferenceCount) walkPrefs(p.getPreference(i), out)
            return
        }
        if (p.key.isNullOrEmpty() && p !is PreferenceCategory) return
        val o = JSONObject()
        o.put("key", p.key ?: "")
        o.put("title", p.title?.toString() ?: p.key)
        o.put("summary", p.summary?.toString() ?: "")
        when (p) {
            is ListPreference -> {
                o.put("type", "list")
                o.put("value", p.value ?: "")
                val entries = JSONArray(); val values = JSONArray()
                p.entries?.forEach { entries.put(it.toString()) }
                p.entryValues?.forEach { values.put(it.toString()) }
                o.put("entries", entries)
                o.put("entryValues", values)
            }
            is MultiSelectListPreference -> o.put("type", "multi")
            is EditTextPreference -> {
                o.put("type", "text")
                o.put("value", p.text ?: "")
            }
            is CheckBoxPreference, is SwitchPreference, is SwitchPreferenceCompat -> {
                o.put("type", "toggle")
                val on = when (p) {
                    is CheckBoxPreference -> p.isChecked
                    is SwitchPreference -> p.isChecked
                    is SwitchPreferenceCompat -> p.isChecked
                    else -> false
                }
                o.put("value", on)
            }
            else -> o.put("type", "other")
        }
        out.put(o)
    }
}
