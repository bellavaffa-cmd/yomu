# Keep Moshi model classes
-keep class com.yomu.reader.source.**.model.** { *; }
-keepclassmembers class ** {
    @com.squareup.moshi.Json <fields>;
}
