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

-keep class org.joda.time.** { public *; }
-keep interface org.joda.time.** { public *; }

## Joda Convert 1.6

-keep class org.joda.convert.** { public *; }
-keep interface org.joda.convert.** { public *; }


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

####### hibernate validator

# hang onto dynamic things
# TODO: refine
-keep class org.hibernate.validator.** { public *; }
-keep interface org.hibernate.validator.** { public *; }
-keepnames class org.hibernate.validator.**
#-keepclassmembers class org.hibernate.** { *; }

-keep class javax.el.** { public *; }
-keep interface javax.el.** { public *; }
-keepclassmembers class javax.el.** { public *; }

-keep class com.sun.el.** { public *; }
-keep interface com.sun.el.** { public *; }

# keep the constraint annotation defninitions
-keep class javax.validation.** { public *; }
-keep interface javax.validation.** { public *; }
-keepclassmembers class javax.validation.constraints.** { *; }
-keepnames class javax.validation.constraints.**

# so obfuscation doesn't break hibernate validator
-keepnames class javax.**
-keepnames class ext.javax.**
-keepnames class org.joda.**
-keepnames class com.fasterxml.**
-keepnames class com.sun.el.**

-keep class com.fasterxml.jackson.databind.** { public *; }
-keep interface com.fasterxml.jackson.databind.** { public *; }

# TODO: wat
-dontwarn com.fasterxml.jackson.databind.**

-keepnames class org.jboss.logmanager.**
-keepnames class org.apache.log4j.**
-keepnames class org.slf4j.**

# loggers
-dontwarn org.jboss.logmanager.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**

-dontwarn javax.script.**
-dontwarn com.thoughtworks.paranamer.** # optional dependency (what does it do?)
-dontwarn org.jsoup.** # optional dependency, for HTML parsing
-dontwarn com.sun.activation.** # UI stuff in here
-dontwarn javax.activation.** # more UI stuff

-dontwarn java.lang.**
-dontwarn java.beans.**

# logger deps, ugh
-dontwarn javax.swing.**
-dontwarn java.awt.**
-dontwarn javax.jms.**
-dontwarn javax.management.**

-dontwarn javax.xml.namespace.QName
