# ─────────────────────────────────────────────────────────────────────────────
# TraceLedger ProGuard / R8 Rules
# app/proguard-rules.pro
#
# R8 runs in full mode for release builds (isMinifyEnabled = true).
# Without these rules the release APK crashes because R8 strips or renames
# classes that are accessed by reflection at runtime.
# ─────────────────────────────────────────────────────────────────────────────

# ── Room ──────────────────────────────────────────────────────────────────────
# Room uses reflection to access entity fields and DAO methods.
# Keep all Room-annotated classes and their members.

-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }

# Room's generated _Impl classes must not be renamed
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }

# Keep Room's internal query infrastructure
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# ── Kotlin Serialization ──────────────────────────────────────────────────────
# kotlinx.serialization uses reflection to find serializers.
# Keep all @Serializable classes and their companion serializer objects.

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**

-keep @kotlinx.serialization.Serializable class * {
    *;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Kotlin serializer lookup uses class name — prevent renaming
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ── Kotlin ────────────────────────────────────────────────────────────────────
# Kotlin coroutines and stdlib reflection
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Lazy { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Compose ───────────────────────────────────────────────────────────────────
# Compose uses reflection for @Preview and state restoration.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose stability inference — keep annotated classes intact
-keepclassmembers class * {
    @androidx.compose.runtime.Stable *;
    @androidx.compose.runtime.Immutable *;
}

# ── Android standard rules ────────────────────────────────────────────────────
# Keep Application, Activity, Service, BroadcastReceiver, ContentProvider
# (they are instantiated by the Android framework by name)
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum values (accessed by name in Room type converters)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── BuildConfig ───────────────────────────────────────────────────────────────
-keep class com.greenicephoenix.traceledger.BuildConfig { *; }

# ── Debugging ─────────────────────────────────────────────────────────────────
# Preserve source file names and line numbers in crash stack traces.
# This makes Firebase Crashlytics (or logcat) useful in production.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile