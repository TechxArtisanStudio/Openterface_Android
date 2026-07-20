PROJ_PATH	:= $(call my-dir)

# NOTE: LOCAL_LDFLAGS must be set in each sub-module AFTER include $(CLEAR_VARS),
# not here. CLEAR_VARS wipes all LOCAL_* variables, so setting them before is dead code.
# Each sub-module (UVCCamera, libjpeg-turbo, libyuv, libuvc, libusb) already has its own:
#   LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384

include $(CLEAR_VARS)
include $(PROJ_PATH)/libjpeg-turbo/Android.mk
include $(PROJ_PATH)/libyuv/Android.mk
include $(PROJ_PATH)/libusb/android/jni/Android.mk
include $(PROJ_PATH)/libuvc/android/jni/Android.mk
include $(PROJ_PATH)/UVCCamera/Android.mk