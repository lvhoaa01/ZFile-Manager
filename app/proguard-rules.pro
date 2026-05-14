# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep model classes for reflection-based ops
-keep class com.zfile.manager.model.** { *; }
