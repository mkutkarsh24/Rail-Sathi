# Gson reflects over DTO field names / @SerializedName annotations.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.railsathi.app.data.remote.dto.** { *; }

# Retrofit service interfaces (generic return types are read reflectively).
-keep,allowobfuscation interface com.railsathi.app.data.remote.api.RailApi
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
