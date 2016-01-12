
### Test dependencies
# These will only be included when running tests, so keep them all

-keep class com.robotium.solo.** { *; }
-keep interface com.robotium.solo.** { *; }
-keepclassmembers class com.robotium.solo.** { *; }
-keepnames class com.robotium.solo.** { *; }
-keepnames interface com.robotium.solo.** { *; }

-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-keepclassmembers class com.squareup.okhttp.** { *; }
-keepnames class com.squareup.okhttp.**
-keepnames interface com.squareup.okhttp.**

-keepclassmembers class * {
    @javax.naming.** *;
    @java.nio.file.** *;
    @com.squareup.okhttp.internal.huc.** *;
}

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

-keep class android.test.** { *; }
-keep interface android.test.** { *; }
-keepclassmembers class android.test.** { *; }
-keepnames class android.test.**
-keepnames interface android.test.**

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
-keep class com.android.org.conscrypt.** { public *; }
-keep class org.apache.harmony.xnet.** { public *; }
-keep interface com.android.org.conscrypt.** { public *; }
-keep interface org.apache.harmony.xnet.** { public *; }

-keepnames class org.codehaus.**
-keepnames class javax.naming.**
-keepnames class java.nio.file.**
-keepnames class org.junit.**
-keepnames class com.android.**
-keepnames class org.apache.harmony.**
-keepnames class java.net.**
-keepnames class com.android.org.**

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

## Can ignore warnings from some unused things in here
-dontwarn javax.naming.**
-dontwarn java.nio.file.**
-dontwarn com.squareup.okhttp.internal.huc.**

# ignore "library class implements program class" errors
-dontwarn android.test.**

