# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/arunesh/Library/Android/sdk/tools/proguard/proguard-android.txt
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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#-keep public class com.inceptai.dobby.*
#-keep class com.inceptai.** { *; }
#-keep public class com.inceptai.** {
#  public protected *;
#}
#-keep class com.apache.logging.** { *; }

-dontshrink
-dontoptimize
-dontpreverify
-dontskipnonpubliclibraryclasses
-verbose

-dontwarn javax.management.**
-dontwarn java.lang.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**
-dontwarn org.json.*

-keep class com.inceptai.** { *; }
-keep class com.google.common.** { *; }
-keep class fr.bmartel.** { *; }
-keep class javax.** { *; }
-keep class ai.api.** { *; }
-keep class commons-io.** { *; }
#-keep class java.** { *; }



-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
