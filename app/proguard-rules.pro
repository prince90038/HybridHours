# ProGuard rules for HybridHours

# Keep Gson internals
-keep class com.google.gson.** { *; }

# Keep the Reminder data class fields from being renamed
-keep class com.example.hybridhours.Reminder {
    <fields>;
    <methods>;
}

# General Android rules
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
