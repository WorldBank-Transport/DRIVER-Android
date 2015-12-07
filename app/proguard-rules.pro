# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /crucial/android-sdk/sdk/tools/proguard/proguard-android.txt
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

-optimizationpasses 3
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-dump class_files.txt
-printseeds seeds.txt
-printusage unused.txt
-printmapping mapping.txt

-dontobfuscate # comes in handy sometimes

-keepattributes EnclosingMethod,Signature,*Annotation*,SourceFile,LineNumberTable,Exceptions,InnerClasses,Deprecated

# http://proguard.sourceforge.net/manual/examples.html#beans
-adaptresourcefilenames    **.properties,**.gif,**.jpg
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF

-allowaccessmodification
-renamesourcefileattribute SourceFile
-repackageclasses ''

# keep things in this app (necessary for dynamic model references)
-keep class org.worldbank.transport.driver.** { *; }
-keepclassmembers class org.worldbank.transport.driver.** { *; }
-keepnames class org.worldbank.transport.driver.**

# keep annotation classes so they do not get obfuscated, even if not referenced directly
-keep class org.jsonschema2pojo.annotations.** { *; }
-keep class com.fasterxml.jackson.annotation.** { public *; }
-keep class com.google.gson.annotations.** { *; }
-keep class javax.annotation.** { public *; }

-keep class com.google.vending.licensing.ILicensingService { *; }
-keep class org.w3c.dom.** { *; }
-keep class sun.nio.cs.** { public *; }
-keep class javax.lang.model.** { public *; }

# needed for dynamic references
-keep class libcore.icu.** { public *; }
-keep class android.graphics.** { public *; }

-dontwarn org.w3c.dom.**
-dontwarn sun.nio.cs.**
-dontwarn javax.lang.model.**

-keepclassmembers class * {
    @javax.annotation.Resource *;
    @org.springframework.beans.factory.annotation.Autowired *;
    @android.webkit.JavascriptInterface <methods>;
    @org.codehaus.jackson.annotate.* <fields>;
    @org.codehaus.jackson.annotate.* <init>(...);
}

####################
# below configurations based on:
# https://github.com/krschultz/android-proguard-snippets

## Joda Time 2.3

-dontwarn org.joda.convert.**
-dontwarn org.joda.time.**

# TODO: refine for joda time usage, or remove if not needed
#-keep class org.joda.time.** { public *; }
#-keep interface org.joda.time.** { public *; }

## Joda Convert 1.6

#-keep class org.joda.convert.** { public *; }
#-keep interface org.joda.convert.** { public *; }


# support design

-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

-keep public class * extends android.support.design.widget.** {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# app compat v7

-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

## GSON 2.2.4 specific rules ##

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }


# Proguard configuration for Jackson 2.x (fasterxml package instead of codehaus package)
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
