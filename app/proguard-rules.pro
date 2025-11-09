# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep all compose related
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
