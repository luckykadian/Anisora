# Keep Aniyomi source-api so extension APKs can link against host classes
-keep class eu.kanade.tachiyomi.** { *; }
-keep class uy.kohesive.injekt.** { *; }
-keep class okhttp3.** { *; }
-keep class org.jsoup.** { *; }
-keep class rx.** { *; }
-keep class app.anisora.ExtBridge { *; }
-keep class app.anisora.AnisoraApp { *; }
