# ── libsu ──
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# ── Shizuku ──
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# ── Media3 / ExoPlayer ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil ──
-dontwarn coil.**

# ── DataStore ──
-keep class androidx.datastore.** { *; }
