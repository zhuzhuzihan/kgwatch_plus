-keep interface * extends java.lang.annotation.Annotation { *; }
-keep @momoi.anno.mixin.Mixin class * {
    *;
}

-keep class com.kugou.** { *; }
-keep class androidx.lifecycle.** { *; }
