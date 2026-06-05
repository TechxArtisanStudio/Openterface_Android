# Application.mk for libvncserver NDK build

APP_PLATFORM := android-21
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64

APP_CFLAGS := \
  -DLOG_NDEBUG

APP_OPTIM := release
