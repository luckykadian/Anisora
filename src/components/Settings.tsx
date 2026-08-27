import { AnimatePresence, motion } from "framer-motion";
import { ArrowDown, ArrowUp, BadgeCheck, Check, ChevronDown, Database, Info, Layers, LogOut, MonitorPlay, Moon, Palette, Plus, RefreshCw, Sparkles, Sun, Trash2, Type, User } from "lucide-react";
import { useState, type ReactNode } from "react";
import { ACCENTS, fmt } from "../api";
import { Chip, Logo, SectionHead, Seg, SPRING, Toggle } from "../bits";
import { defaultSettings, useApp } from "../store";
import type { Extension } from "../types";
import { cn } from "../utils/cn";

function Row({ label, sub, children }: { label: string; sub?: string; children: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line py-4 last:border-0 sm:flex-nowrap">
      <div className="min-w-0">
        <p className="text-[13.5px] font-semibold">{label}</p>
        {sub && <p className="mt-0.5 text-[11.5px] leading-snug text-mut">{sub}</p>}
      </div>
      <div className="flex shrink-0 items-center gap-2">{children}</div>
    </div>
  );
}

function Panel({ icon, title, sub, children }: { icon: any; title: string; sub?: string; children: ReactNode }) {
  return (
    <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={SPRING} className="rounded-3xl border border-line bg-bg1 p-5 sm:p-6">
      <SectionHead icon={icon} title={title} sub={sub} />
      {children}
    </motion.section>
  );
}

/* ------------------------------ extension card ------------------------------ */

function ExtCard({ ext, idx, count, onMove, onChange, onRemove }: { ext: Extension; idx: number; count: number; onMove: (dir: -1 | 1) => void; onChange: (e: Extension) => void; onRemove: () => void }) {
  const [open, setOpen] = useState(false);
  const isAnime = ext.kind === "ANIME";
  return (
    <motion.div layout transition={SPRING} className={cn("overflow-hidden rounded-2xl border bg-bg2/60 transition-colors", ext.enabled ? "border-line" : "border-line/60 opacity-80")}>
      <div className="flex flex-wrap items-center gap-3 p-4">
        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl text-[15px] font-black text-white shadow-card" style={{ background: `linear-gradient(140deg, ${ext.color}, ${ext.color}99)` }}>
          {ext.name.slice(0, 1)}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-[14px] font-bold">{ext.name}</p>
            <Chip className="px-1.5 py-0 text-[9.5px]">v{ext.version}</Chip>
            {ext.enabled && <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />}
          </div>
          <p className="mt-0.5 text-[11px] text-mut">
            {ext.lang} · {isAnime ? "Anime source" : "Manga source"} · {fmt(ext.downloads)} users
          </p>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={() => onMove(-1)} disabled={idx === 0} className="grid h-8 w-8 place-items-center rounded-lg text-mut transition hover:bg-bg1 hover:text-txt disabled:pointer-events-none disabled:opacity-25" aria-label="priority up">
            <ArrowUp size={14} />
          </button>
          <button onClick={() => onMove(1)} disabled={idx === count - 1} className="grid h-8 w-8 place-items-center rounded-lg text-mut transition hover:bg-bg1 hover:text-txt disabled:pointer-events-none disabled:opacity-25" aria-label="priority down">
            <ArrowDown size={14} />
          </button>
          <button onClick={() => setOpen((v) => !v)} className={cn("grid h-8 w-8 place-items-center rounded-lg text-mut transition hover:bg-bg1 hover:text-txt", open && "text-acc")} aria-label="extension settings">
            <ChevronDown size={15} className={cn("transition-transform", open && "rotate-180")} />
          </button>
          <Toggle on={ext.enabled} onChange={(v) => onChange({ ...ext, enabled: v })} />
        </div>
      </div>
      <AnimatePresence>
        {open && (
          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={SPRING} className="overflow-hidden">
            <div className="space-y-4 border-t border-line bg-bg1/50 p-4">
              <Row label="Preferred quality" sub={isAnime ? "Default stream quality" : "Image quality for pages"}>
                <Seg uid={`q-${ext.id}`} value={ext.cfg.quality} onChange={(q) => onChange({ ...ext, cfg: { ...ext.cfg, quality: q } })} options={[{ id: "1080p", label: "1080p" }, { id: "720p", label: "720p" }, { id: "480p", label: "480p" }]} />
              </Row>
              <Row label="Server" sub="Fallback tries the next one automatically">
                <Seg uid={`s-${ext.id}`} value={ext.cfg.server} onChange={(s) => onChange({ ...ext, cfg: { ...ext.cfg, server: s } })} options={ext.servers.map((s) => ({ id: s, label: s }))} />
              </Row>
              {isAnime && (
                <Row label="Audio preference">
                  <Seg uid={`d-${ext.id}`} value={ext.cfg.subDub} onChange={(sd) => onChange({ ...ext, cfg: { ...ext.cfg, subDub: sd } })} options={[{ id: "sub", label: "Sub" }, { id: "dub", label: "Dub" }]} />
                </Row>
              )}
              <div className="flex justify-end pt-1">
                <button onClick={onRemove} className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[12px] font-semibold text-rose-300 transition hover:bg-rose-400/10">
                  <Trash2 size={13} /> Uninstall extension
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

/* --------------------------------- page ------------------------------------ */

const SECTIONS = [
  { id: "appearance", label: "Appearance", icon: Palette },
  { id: "content", label: "Content", icon: Type },
  { id: "extensions", label: "Extensions", icon: Layers },
  { id: "player", label: "Player", icon: MonitorPlay },
  { id: "sync", label: "AniList Sync", icon: RefreshCw },
  { id: "about", label: "About", icon: Info },
] as const;

type SectionId = (typeof SECTIONS)[number]["id"];

export function SettingsPage() {
  const { session, settings, updateSettings, logout, toast } = useApp();
  const [sec, setSec] = useState<SectionId>("appearance");
  const [repo, setRepo] = useState("");
  const [syncing, setSyncing] = useState(false);

  const exts = settings.extensions;
  const setExts = (list: Extension[]) => updateSettings({ extensions: list });
  const updateExt = (id: string, e: Extension) => setExts(exts.map((x) => (x.id === id ? e : x)));
  const moveExt = (id: string, dir: -1 | 1) => {
    const i = exts.findIndex((x) => x.id === id);
    const j = i + dir;
    if (i < 0 || j < 0 || j >= exts.length) return;
    const copy = exts.slice();
    [copy[i], copy[j]] = [copy[j], copy[i]];
    setExts(copy);
  };

  const animeExts = exts.filter((x) => x.kind === "ANIME");
  const mangaExts = exts.filter((x) => x.kind === "MANGA");

  return (
    <div className="pb-28">
      <header className="pt-1">
        <p className="font-mono text-[11px] uppercase tracking-[0.22em] text-mut">Control room</p>
        <h1 className="mt-1.5 font-disp text-3xl font-bold tracking-tight sm:text-[34px]">
          Make it <span className="text-acc">yours.</span>
        </h1>
        <p className="mt-1 text-[13px] text-mut">Every change applies instantly and persists on this device.</p>
      </header>

      <div className="mt-8 grid gap-6 lg:grid-cols-[220px,1fr]">
        {/* section nav */}
        <nav className="lg:sticky lg:top-24 lg:self-start">
          <div className="flex gap-1.5 overflow-x-auto rounded-2xl border border-line bg-bg1 p-1.5 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden lg:flex-col">
            {SECTIONS.map((s) => (
              <button
                key={s.id}
                onClick={() => setSec(s.id)}
                className={cn("relative flex shrink-0 items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold transition-colors", sec === s.id ? "text-accInk" : "text-mut hover:text-txt")}
              >
                {sec === s.id && <motion.span layoutId="settings-nav" transition={SPRING} className="absolute inset-0 rounded-xl bg-acc shadow-glow" />}
                <span className="relative z-10 flex items-center gap-2.5">
                  <s.icon size={15} /> {s.label}
                </span>
              </button>
            ))}
          </div>
        </nav>

        {/* panels */}
        <div className="min-w-0 space-y-6">
          <AnimatePresence mode="wait">
            <motion.div key={sec} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.18 }} className="space-y-6">
              {sec === "appearance" && (
                <Panel icon={Palette} title="Appearance" sub="Skin the entire interface">
                  <Row label="Theme" sub="Dark, pure-black AMOLED, or light">
                    <Seg uid="theme" value={settings.theme} onChange={(v) => updateSettings({ theme: v })} options={[{ id: "dark", label: "Dark", icon: Moon }, { id: "amoled", label: "AMOLED", icon: Sparkles }, { id: "light", label: "Light", icon: Sun }]} />
                  </Row>
                  <Row label="Accent color" sub="Used for highlights, rings and sync states">
                    <div className="flex flex-wrap gap-2">
                      {ACCENTS.map((a) => (
                        <button key={a.value} title={a.name} onClick={() => updateSettings({ accent: a.value })} className="grid h-9 w-9 place-items-center rounded-full transition-transform hover:scale-110" style={{ background: `rgb(${a.value})` }}>
                          {settings.accent === a.value && <Check size={14} strokeWidth={3.5} className="text-black/70" />}
                        </button>
                      ))}
                    </div>
                  </Row>
                  <Row label="Card density" sub="Compact packs more posters per row">
                    <Seg uid="density" value={settings.density} onChange={(v) => updateSettings({ density: v })} options={[{ id: "cozy", label: "Cozy" }, { id: "compact", label: "Compact" }]} />
                  </Row>
                  <Row label="Poster corner radius" sub="Roundness of every cover">
                    <span className="flex items-center gap-3">
                      <input type="range" min={6} max={26} value={settings.posterRadius} onChange={(e) => updateSettings({ posterRadius: Number(e.target.value) })} className="range w-36" />
                      <span className="w-9 text-right font-mono text-[12px] font-bold tabular">{settings.posterRadius}px</span>
                    </span>
                  </Row>
                  <Row label="Reduce motion" sub="Minimise shared-element and spring animations">
                    <Toggle on={settings.reduceMotion} onChange={(v) => updateSettings({ reduceMotion: v })} />
                  </Row>
                  <Row label="Episode thumbnails" sub="Show provider thumbnails in the Watch tab">
                    <Toggle on={settings.showThumbs} onChange={(v) => updateSettings({ showThumbs: v })} />
                  </Row>
                </Panel>
              )}

              {sec === "content" && (
                <Panel icon={Type} title="Content" sub="How titles and results behave">
                  <Row label="Title language" sub="Renaming applies to every card instantly">
                    <Seg uid="tlang" value={settings.titleLang} onChange={(v) => updateSettings({ titleLang: v })} options={[{ id: "romaji", label: "Romaji" }, { id: "english", label: "English" }, { id: "native", label: "Native" }]} />
                  </Row>
                  <Row label="Adult content" sub="Include 18+ entries in search results">
                    <Toggle on={settings.nsfw} onChange={(v) => updateSettings({ nsfw: v })} />
                  </Row>
                  <Row label="Auto-update progress" sub="Advance your AniList entry after finishing an episode">
                    <Toggle on={settings.autoProgress} onChange={(v) => updateSettings({ autoProgress: v })} />
                  </Row>
                </Panel>
              )}

              {sec === "extensions" && (
                <>
                  <Panel icon={Layers} title="Extension repository" sub="Sources resolve episodes & chapters">
                    <div className="flex flex-wrap gap-2.5">
                      <input value={repo} onChange={(e) => setRepo(e.target.value)} placeholder="https://repo.example.dev/index.min.json" className="min-w-0 flex-1 rounded-xl border border-line bg-bg2 px-3.5 py-2.5 font-mono text-[12px] outline-none transition focus:border-accLine" />
                      <button
                        onClick={() => {
                          if (!repo.trim()) return;
                          setRepo("");
                          toast("Repository added — 24 extensions indexed", "check");
                        }}
                        className="flex items-center gap-2 rounded-xl bg-acc px-4 py-2.5 text-[12.5px] font-bold text-accInk shadow-glow transition active:scale-[0.97]"
                      >
                        <Plus size={14} strokeWidth={3} /> Add repo
                      </button>
                      <button onClick={() => toast("Repositories refreshed", "sync")} className="grid h-[42px] w-[42px] place-items-center rounded-xl border border-line bg-bg2 text-mut transition hover:border-accLine hover:text-txt" aria-label="refresh repos">
                        <RefreshCw size={15} />
                      </button>
                    </div>
                    <p className="mt-3 text-[11px] text-mut">Priority order decides which source loads first — use the arrows on each card.</p>
                  </Panel>

                  <Panel icon={Layers} title="Anime sources">
                    <div className="space-y-3">
                      {animeExts.map((x, i) => (
                        <ExtCard key={x.id} ext={x} idx={i} count={animeExts.length} onMove={(dir) => moveExt(x.id, dir)} onChange={(e) => updateExt(x.id, e)} onRemove={() => { setExts(exts.filter((y) => y.id !== x.id)); toast(`${x.name} uninstalled`, "trash"); }} />
                      ))}
                      {animeExts.length === 0 && <p className="py-4 text-center text-[12px] text-mut">No anime extensions installed</p>}
                    </div>
                  </Panel>

                  <Panel icon={Layers} title="Manga sources">
                    <div className="space-y-3">
                      {mangaExts.map((x, i) => (
                        <ExtCard key={x.id} ext={x} idx={i} count={mangaExts.length} onMove={(dir) => moveExt(x.id, dir)} onChange={(e) => updateExt(x.id, e)} onRemove={() => { setExts(exts.filter((y) => y.id !== x.id)); toast(`${x.name} uninstalled`, "trash"); }} />
                      ))}
                      {mangaExts.length === 0 && <p className="py-4 text-center text-[12px] text-mut">No manga extensions installed</p>}
                    </div>
                  </Panel>
                </>
              )}

              {sec === "player" && (
                <Panel icon={MonitorPlay} title="Player" sub="Playback behaviour for the internal player">
                  <Row label="Auto-play next episode" sub="Roll straight into the next episode">
                    <Toggle on={settings.autoNext} onChange={(v) => updateSettings({ autoNext: v })} />
                  </Row>
                  <Row label="Skip intro button" sub="Offer a skip control during openings">
                    <Toggle on={settings.skipIntro} onChange={(v) => updateSettings({ skipIntro: v })} />
                  </Row>
                  <Row label="Subtitle size" sub="Scales captions in the player">
                    <span className="flex items-center gap-3">
                      <input type="range" min={60} max={160} step={5} value={settings.subSize} onChange={(e) => updateSettings({ subSize: Number(e.target.value) })} className="range w-36" />
                      <span className="w-11 text-right font-mono text-[12px] font-bold tabular">{settings.subSize}%</span>
                    </span>
                  </Row>
                  <Row label="Default quality everywhere" sub="Overwrite quality for all anime sources">
                    <Seg
                      uid="qall"
                      value={animeExts[0]?.cfg.quality ?? "1080p"}
                      onChange={(q) => setExts(exts.map((x) => (x.kind === "ANIME" ? { ...x, cfg: { ...x.cfg, quality: q } } : x)))}
                      options={[{ id: "1080p", label: "1080p" }, { id: "720p", label: "720p" }, { id: "480p", label: "480p" }]}
                    />
                  </Row>
                </Panel>
              )}

              {sec === "sync" && (
                <>
                  <Panel icon={User} title="Account">
                    <div className="flex flex-wrap items-center gap-4 rounded-2xl border border-line bg-bg2/60 p-4">
                      <span className="grid h-14 w-14 place-items-center rounded-2xl bg-accSoft font-disp text-xl font-bold text-acc">{(session?.user.name ?? "G").slice(0, 1).toUpperCase()}</span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[15px] font-bold">{session?.user.name ?? "Guest"}</p>
                        <p className="mt-0.5 flex items-center gap-1.5 text-[11.5px] text-mut">
                          {session?.guest ? (
                            <>Local profile — progress stays on this device</>
                          ) : (
                            <>
                              <BadgeCheck size={12} className="text-acc" /> Connected to anilist.co
                            </>
                          )}
                        </p>
                      </div>
                      <button
                        onClick={() => {
                          logout();
                          toast("Signed out — see you soon", "info");
                        }}
                        className="flex items-center gap-2 rounded-xl border border-rose-400/30 bg-rose-400/10 px-4 py-2.5 text-[12.5px] font-bold text-rose-300 transition hover:bg-rose-400/20"
                      >
                        <LogOut size={14} /> Log out
                      </button>
                    </div>
                  </Panel>

                  <Panel icon={RefreshCw} title="Synchronisation" sub="How Anisora talks to AniList">
                    <Row label="Auto sync" sub="Push changes to AniList immediately">
                      <Toggle on={settings.syncAuto} onChange={(v) => updateSettings({ syncAuto: v })} />
                    </Row>
                    <Row label="Sync on launch" sub="Pull the latest lists when the app opens">
                      <Toggle on={settings.syncOnStart} onChange={(v) => updateSettings({ syncOnStart: v })} />
                    </Row>
                    <Row label="Score format" sub="How you rate titles">
                      <Seg uid="sf" value={settings.scoreFormat} onChange={(v) => updateSettings({ scoreFormat: v })} options={[{ id: "100", label: "0–100" }, { id: "10", label: "0–10" }, { id: "5", label: "★ 5" }]} />
                    </Row>
                    <Row label="Manual sync" sub="Force a full two-way sync now">
                      <button
                        onClick={() => {
                          setSyncing(true);
                          window.setTimeout(() => {
                            setSyncing(false);
                            toast("Everything is in sync", "sync");
                          }, 1400);
                        }}
                        className="flex items-center gap-2 rounded-xl bg-acc px-4 py-2.5 text-[12.5px] font-bold text-accInk shadow-glow transition active:scale-[0.97]"
                      >
                        <RefreshCw size={14} className={syncing ? "animate-spin" : ""} /> {syncing ? "Syncing…" : "Sync now"}
                      </button>
                    </Row>
                  </Panel>

                  <Panel icon={Database} title="Storage">
                    <Row label="Image cache" sub="Posters & thumbnails kept offline (demo)">
                      <span className="font-mono text-[12px] font-bold text-mut tabular">24.6 MB</span>
                    </Row>
                    <div className="flex flex-wrap gap-2.5 pt-4">
                      <button onClick={() => toast("Cache cleared", "trash")} className="flex items-center gap-2 rounded-xl border border-line bg-bg2 px-4 py-2.5 text-[12.5px] font-semibold transition hover:border-accLine">
                        <Trash2 size={14} /> Clear cache
                      </button>
                      <button
                        onClick={() => {
                          updateSettings({ ...defaultSettings, extensions: exts });
                          toast("Settings reset to defaults", "info");
                        }}
                        className="flex items-center gap-2 rounded-xl border border-line bg-bg2 px-4 py-2.5 text-[12.5px] font-semibold transition hover:border-rose-400/40 hover:text-rose-300"
                      >
                        <RefreshCw size={14} /> Reset settings
                      </button>
                    </div>
                  </Panel>
                </>
              )}

              {sec === "about" && (
                <Panel icon={Info} title="About Anisora">
                  <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-line bg-bg2/60 p-5">
                    <div className="flex items-center gap-4">
                      <Logo size={40} text={false} />
                      <div>
                        <p className="font-disp text-lg font-bold">Anisora</p>
                        <p className="font-mono text-[11px] text-mut">v1.0.0 · build 2026.02</p>
                      </div>
                    </div>
                    <Chip accent>
                      <Sparkles size={11} /> Demo build
                    </Chip>
                  </div>
                  <p className="mt-5 max-w-xl text-[13px] leading-relaxed text-mut">
                    A love letter to trackers like Dantotsu — an interface for the AniList universe with shared-element transitions, live community data and an extension model. Not
                    affiliated with AniList.
                  </p>
                  <div className="mt-6 space-y-2.5">
                    {[
                      ["Data & API", "AniList GraphQL — live, no key required"],
                      ["Inspiration", "Dantotsu by itsmechinmoy & rebelonion"],
                      ["Icons", "Lucide"],
                    ].map(([k, v]) => (
                      <div key={k} className="flex items-center justify-between rounded-xl border border-line bg-bg2/50 px-4 py-3">
                        <span className="text-[12.5px] font-semibold">{k}</span>
                        <span className="text-[12px] text-mut">{v}</span>
                      </div>
                    ))}
                  </div>
                </Panel>
              )}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
