# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/stephenblack/adt-bundle-mac-x86_64-20140702 2/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes *Annotation*

-dontobfuscate

-dontwarn com.nightscout.**
-dontwarn com.squareup.**
-dontwarn net.tribe7.**
-dontwarn com.mongodb.**
-dontwarn com.google.common.**
-dontwarn okio.**
-dontwarn org.bson.**
-dontwarn org.slf4j.**
-dontwarn rx.internal.util.**
-dontwarn org.apache.commons.**
-dontwarn uk.com.robust-it.**
-dontwarn com.rits.cloning.**
-dontwarn obj.objenesis.instantiator.sun.**
-dontwarn obj.objenesis.instantiator.sun.UnsafeFactoryInstantiator
-dontwarn sun.misc.Unsafe
-dontwarn ar.com.hjg.pngj.**
-dontwarn com.ustwo.clockwise.**

-keep class com.nightscout.** { *; }
-keep class com.squareup.** { *; }
-keep class net.tribe7.** { *; }
#-keep class com.mongodb.** { *; }
#-keep class com.google.common.** { *; }
-keep class okay.** { *; }
-keep class org.bson.** { *; }
-keep class org.slf4j.** { *; }
-keep class rx.internal.util.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class com.eveningoutpost.dexdrip.models.** { *; }
-keep class ar.com.hjg.pngj.** { *; }
-keep class android.support.v7.widget.SearchView { *; }
-keep class com.ustwo.clockwise.** { *; }

-keep class com.google.devtools.build.android.desugar.runtime.ThrowableExtension

-dontwarn java.util.concurrent.**

-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}

-keepclassmembers class rx.internal.util.unsafe.** {
    long producerIndex;
    long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent

-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*
-dontnote **rx.Observable.**

-keepclassmembers class com.eveningoutpost.dexdrip.** {
   public static boolean isRunning();
   public static boolean isCollecting();
   public static ** nanoStatus();
}
-keep class com.google.android.gms.measurement.AppMeasurement { *; }
-keep class com.google.android.gms.measurement.AppMeasurement$OnEventListener { *; }
# TODO is this not too much ?
-keep class com.google.android.gms.** { *; }