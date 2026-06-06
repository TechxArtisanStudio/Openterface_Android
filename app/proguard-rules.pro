# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# WebRTC
-keep class org.webrtc.** { *; }
-keep class io.getstream.webrtc.** { *; }

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Gson
-keep class com.google.gson.** { *; }

# USB Serial
-keep class com.hoho.android.usbserial.** { *; }

# Permission library
-keep class com.hjq.permissions.** { *; }

# Keep all classes in the app
-keep class com.openterface.AOS.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all exception classes
-keep public class * extends java.lang.Exception

# General R8 fix
-dontoptimize
-dontpreverify
-keepattributes *

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile