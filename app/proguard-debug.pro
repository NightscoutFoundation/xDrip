-include proguard-rules.pro
-keepattributes SourceFile,LineNumberTable

-keep class com.google.android.apps.common.testing.** { *; }
-keep class android.app.Instrumentation.** { *; }
-keep class android.content.** { *; }
-keep class org.easymock.** { *; }
-keep class java.beans.** { *; }
-keep class libcore.io.** { *; }

-keep class org.xmlpull.v1.** {*; }
-dontwarn org.xmlpull.v1.**

-dontwarn org.hamcrest.**
-dontwarn android.test.**
-dontwarn android.support.test.**

-keep class org.hamcrest.** { *; }

-keep class org.junit.** { *; }
-dontwarn org.junit.**

-keep class junit.** { *; }
-dontwarn junit.**

-keep class sun.misc.** { *; }
-dontwarn sun.misc.**

-dontnote **