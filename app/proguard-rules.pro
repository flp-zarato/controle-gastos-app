# Add project specific ProGuard rules here.
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.flpzarato.controlegastos.**$$serializer { *; }
-keepclassmembers class com.flpzarato.controlegastos.** {
    *** Companion;
}
-keepclasseswithmembers class com.flpzarato.controlegastos.** {
    kotlinx.serialization.KSerializer serializer(...);
}
