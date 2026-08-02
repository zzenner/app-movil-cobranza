# Reglas ProGuard para la aplicación de cobranza.

# Serialización kotlinx — conservar nombres de campos serializados
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
