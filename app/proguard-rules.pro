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
-dontwarn retrofit2.**
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
-dontwarn okhttp3.**
-dontwarn org.influxdb.**

-keep class com.eveningoutpost.dexdrip.tidepool.** { *; }
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
-keep class com.eveningoutpost.dexdrip.models.** { *; }
-keep class com.eveningoutpost.dexdrip.importedlibraries.usbserial.** { *; }
-keep class com.eveningoutpost.dexdrip.importedlibraries.usbserial.** { *; }
-keep class ar.com.hjg.pngj.** { *; }
-keep class android.support.v7.widget.SearchView { *; }
-keep class kotlinx.serialization.Serializable { *; }

# As long as we only deserialize (from a JSON string into an `NSDeviceStatus`
# object in the class `AAPSStatusHandler`) we can simply ignore warnings related
# to kotlinx serialization.
# These rule should not cause problems: if a project actually relies on
# serialization, then much more than just this class will be required,
# so telling Proguard not to worry if this is missing will not prevent it
# from emitting errors for code that does use serialization but somehow forgot
# to depend on it.
-dontwarn kotlinx.serialization.Serializable

# The lib net.sf.kxml:kxml2:2.3.0 is referenced in same required libraries used for
# Android testing. R8 is showing missing classes warnings which can be safely ignored.
-dontwarn org.kxml2.io.KXmlParser
-dontwarn org.kxml2.io.KXmlSerializer

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

-keep public class * implements **.BitmapCacheProvider
-keep class ** implements **.Exposed { *; }
-keep class com.eveningoutpost.dexdrip.**.*$*Builder { *; }


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

-keepclassmembers class com.eveningoutpost.dexdrip.** {
   public static boolean isRunning();
   public static boolean isCollecting();
   public static ** nanoStatus();
}

-keep @com.google.gson.annotations.Expose public class *

-keepclassmembers public class * {
    @com.google.gson.annotations.Expose *;
}

-if class * {
    @com.google.gson.annotations.Expose <fields>;
}
-keepclassmembers class <1> {
    public <init>();
}

-dontnote rx.internal.util.PlatformDependent
-dontnote rx.**
-dontnote **rx.Observable.**
-dontnote com.squareup.**

-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

-dontwarn com.google.devtools.build.android.desugar.runtime.**

-keep @org.junit.runner.RunWith public class *

-keep class com.newrelic.** { *; }
-dontwarn com.newrelic.**
-keepattributes Exceptions, Signature, InnerClasses, LineNumberTable

-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.*
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.*
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.** { *; }
-keep class com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# New R8 rules for gradle 8 - may not all be needed

-keep class jamorham.keks.** { *; }
-keep class com.eveningoutpost.dexdrip.plugin.** { *; }
-keep class com.eveningoutpost.dexdrip.plugin.IPluginDA { *; }
-keep class * implements com.eveningoutpost.dexdrip.plugin.IPluginDA { *; }

-keepattributes Signature
-keep class com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify { *; }
-keep class com.eveningoutpost.dexdrip.utilitymodels.PrefsViewString { *; }
-keep class com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl { *; }
-keepattributes Signature
-keep class com.eveningoutpost.dexdrip.eassist.EmergencyContact { *; }
-keep class com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify { *; }
-keep class com.eveningoutpost.dexdrip.utilitymodels.PrefsViewString { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes Signature

# Keep Retrofit interfaces and their methods
-keep interface com.eveningoutpost.dexdrip.tidepool.TidepoolUploader$Tidepool { *; }

# Keep data models used in Retrofit calls to prevent R8 from stripping generic types
-keep class com.eveningoutpost.dexdrip.tidepool.M*Reply { *; }
-keep class com.eveningoutpost.dexdrip.tidepool.M*Request { *; }

# Ensure signature is kept for these as well (though generally covered by global rule)
-keepattributes Signature

# Generic rule to keep all Retrofit interfaces and their methods
# This targets any class/interface with methods annotated with Retrofit's @GET, @POST, etc.
-if interface * { @retrofit2.http.* <methods>; }
-keep interface <1> { *; }

-keep interface * {
    @retrofit2.http.GET <methods>;
    @retrofit2.http.POST <methods>;
    @retrofit2.http.PUT <methods>;
    @retrofit2.http.DELETE <methods>;
    @retrofit2.http.PATCH <methods>;
    @retrofit2.http.HEAD <methods>;
    @retrofit2.http.OPTIONS <methods>;
    @retrofit2.http.HTTP <methods>;
}

-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-keep class com.eveningoutpost.dexdrip.tidepool.M* { *; }

-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keep class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepnames class com.fasterxml.jackson.databind.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# questionable below maybe??
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn kotlinx.serialization.descriptors.SerialDescriptor
-dontwarn kotlinx.serialization.internal.GeneratedSerializer
-dontwarn kotlinx.serialization.internal.PluginGeneratedSerialDescriptor
-dontwarn org.apache.http.client.config.RequestConfig$Builder
-dontwarn org.apache.http.client.config.RequestConfig
-dontwarn org.apache.http.client.methods.HttpPatch
-dontwarn org.apache.http.conn.socket.LayeredConnectionSocketFactory
-dontwarn org.apache.http.conn.ssl.SSLConnectionSocketFactory
-dontwarn org.apache.http.impl.client.CloseableHttpClient
-dontwarn org.apache.http.impl.client.HttpClientBuilder
-dontwarn org.apache.http.impl.conn.SystemDefaultRoutePlanner