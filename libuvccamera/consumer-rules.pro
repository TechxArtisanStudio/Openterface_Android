################libuvccamera##################
-keep class com.openterface.AOS.** { *; }
-keep class com.serenegiant.usb.** { *; }
-keepclassmembers class * implements com.serenegiant.usb.IButtonCallback {*;}
-keepclassmembers class * implements com.serenegiant.usb.IFrameCallback {*;}
-keepclassmembers class * implements com.serenegiant.usb.IStatusCallback {*;}
-keepclassmembers class * implements com.serenegiant.opengl.IDrawer2D {*;}
-keepclassmembers class * implements com.serenegiant.opengl.renderer.IRendererHolder {*;}