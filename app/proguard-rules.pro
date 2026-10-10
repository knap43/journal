# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.knap43.journal.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
