# Violet — ProGuard rules

# Keep navigation component
-keepnames class androidx.navigation.fragment.NavHostFragment

# Keep all fragment classes
-keep class app.violet.ui.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
