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

### Test dependencies
# These will only be included when running tests, so keep them all
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-keepclassmembers class com.squareup.okhttp.** { *; }
-keepnames class com.squareup.okhttp.**
-keepnames interface com.squareup.okhttp.**

-keep class okio.Okio.** { *; }
-keep interface okio.Okio.** { *; }
-keepclassmembers class okio.Okio.** { *; }
-keepnames class okio.**
-keepnames interface okio.**

-keep class org.bouncycastle.** { *; }
-keep interface org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.**
-keepnames interface org.bouncycastle.**

# Things referenced by testing libraries
-keep class java.nio.file.** { *; }
-keep class org.junit.** { *; }
-keep interface org.junit.** { *; }
-keep class org.codehaus.mojo.** { public *; }
-keep class javax.naming.** { *; }
-keep interface javax.naming.** { *; }
-keep class com.android.org.conscrypt.** { public *; }
-keep class org.apache.harmony.xnet.provider.** { public *; }
-keep class java.net.** { public *; }

-keepnames class org.codehaus.**
-keepnames class javax.naming.**
-keepnames class java.nio.file.**
-keepnames class org.junit.**
-keepnames class com.android.**
-keepnames class org.apache.harmony.**
-keepnames class java.net.**

-keepnames interface org.codehaus.**
-keepnames interface javax.naming.**
-keepnames interface java.nio.file.**
-keepnames interface org.junit.**
-keepnames interface com.android.**
-keepnames interface org.apache.harmony.**
-keepnames interface java.net.**

-keepclassmembers class org.junit.** { *; }
-keepclassmembers class com.squareup.okhttp.** { *; }
-keepclassmembers class okio.** { *; }
-keepclassmembers class java.nio.file.** { *; }
-keepclassmembers class javax.naming.** { *; }

-dontwarn junit.**

## Can ignore warnings from unused things
#-dontwarn okio.**
#-dontwarn com.squareup.okhttp.**
#-dontwarn org.bouncycastle.**
