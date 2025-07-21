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

-dontwarn com.google.devtools.build.android.desugar.runtime.**

-keep class sun.misc.** { *; }
-dontwarn sun.misc.**
-dontnote **rx.Observable.**
-dontnote **

-keepclassmembers class com.eveningoutpost.dexdrip.** {
   public static boolean isRunning();
   public static boolean isCollecting();
   public static ** nanoStatus();
}

-dontnote rx.internal.util.PlatformDependent
-dontnote rx.**
-dontnote **rx.Observable.**
-dontnote com.squareup.**

-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

-dontwarn com.google.devtools.build
-dontwarn com.google.devtools.build.android.desugar.runtime.**

-keep class com.eveningoutpost.dexdrip.test.**
-keep class android.support.test.internal** { *; }
-keep class android.support.test.** { *; }
-keep class com.schibsted.spain.barista.** { *; }
-keep class org.junit.** { *; }
-keep @org.junit.runner.RunWith public class *
-keep class androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
-keep class org.robolectric.RobolectricTestRunner

-keep class com.newrelic.** { *; }
-dontwarn com.newrelic.**
-keepattributes Exceptions, Signature, InnerClasses, LineNumberTable, SourceFile, EnclosingMethod

-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.*
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.*
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.** { *; }
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer