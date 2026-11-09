# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }

# Keeping models for Moshi or Retrofit if needed

# Keep navigation rules

# Keep ViewModel rules
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
