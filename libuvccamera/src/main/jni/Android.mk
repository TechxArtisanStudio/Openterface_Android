PROJ_PATH	:= $(call my-dir)

# Enable 16KB page size support for Android 15+
# Add linker flags for proper alignment
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384

include $(CLEAR_VARS)
include $(PROJ_PATH)/libjpeg-turbo/Android.mk
include $(PROJ_PATH)/libyuv/Android.mk
include $(PROJ_PATH)/libusb/android/jni/Android.mk
include $(PROJ_PATH)/libuvc/android/jni/Android.mk
include $(PROJ_PATH)/UVCCamera/Android.mk