LOCAL_PATH := $(call my-dir)
LIBVNCROOT := $(LOCAL_PATH)/../libvnc
LIBJPEG_ROOT := $(LOCAL_PATH)/../../../../../libuvccamera/src/main/jni/libjpeg-turbo
CONFIG_INCLUDE := $(LOCAL_PATH)/config_include

include $(CLEAR_VARS)

LOCAL_MODULE := vncserver

# NOTE: CONFIG_INCLUDE is listed first so our hand-crafted rfbconfig.h
# takes precedence over anything the libvnc submodule might provide
# (upstream .gitignores rfbconfig.h, so it is absent on a fresh clone).
LOCAL_C_INCLUDES := \
    $(CONFIG_INCLUDE) \
    $(LIBVNCROOT)/include \
    $(LIBVNCROOT)/src/common \
    $(LIBVNCROOT)/src/libvncserver \
    $(LIBJPEG_ROOT) \
    $(LIBJPEG_ROOT)/include

# Exclude template include files (they are #included by translate.c)
# Exclude encodings that need external deps we don't have (libjpeg, lzo, openssl)
LOCAL_SRC_FILES := \
    $(LIBVNCROOT)/src/common/d3des.c \
    $(LIBVNCROOT)/src/common/vncauth.c \
    $(LIBVNCROOT)/src/common/sockets.c \
    $(LIBVNCROOT)/src/common/sha1.c \
    $(LIBVNCROOT)/src/common/base64.c \
    $(LIBVNCROOT)/src/common/crypto_included.c \
    $(LIBVNCROOT)/src/libvncserver/auth.c \
    $(LIBVNCROOT)/src/libvncserver/cargs.c \
    $(LIBVNCROOT)/src/libvncserver/corre.c \
    $(LIBVNCROOT)/src/libvncserver/cursor.c \
    $(LIBVNCROOT)/src/libvncserver/cutpaste.c \
    $(LIBVNCROOT)/src/libvncserver/draw.c \
    $(LIBVNCROOT)/src/libvncserver/font.c \
    $(LIBVNCROOT)/src/libvncserver/hextile.c \
    $(LIBVNCROOT)/src/libvncserver/httpd.c \
    $(LIBVNCROOT)/src/libvncserver/main.c \
    $(LIBVNCROOT)/src/libvncserver/rfbserver.c \
    $(LIBVNCROOT)/src/libvncserver/rfbregion.c \
    $(LIBVNCROOT)/src/libvncserver/rfbssl_none.c \
    $(LIBVNCROOT)/src/libvncserver/rre.c \
    $(LIBVNCROOT)/src/libvncserver/scale.c \
    $(LIBVNCROOT)/src/libvncserver/selbox.c \
    $(LIBVNCROOT)/src/libvncserver/sockets.c \
    $(LIBVNCROOT)/src/libvncserver/stats.c \
    $(LIBVNCROOT)/src/libvncserver/translate.c \
    $(LIBVNCROOT)/src/libvncserver/tight.c \
    $(LIBVNCROOT)/src/libvncserver/websockets.c \
    $(LIBVNCROOT)/src/libvncserver/ws_decode.c \
    $(LIBVNCROOT)/src/libvncserver/zlib.c \
    $(LIBVNCROOT)/src/libvncserver/zrle.c \
    $(LIBVNCROOT)/src/libvncserver/zrleoutstream.c \
    $(LIBVNCROOT)/src/libvncserver/zrlepalettehelper.c \
    $(LIBVNCROOT)/src/common/minilzo.c \
    $(LIBVNCROOT)/src/libvncserver/ultra.c \
    vnc_server_jni.c

# libjpeg-turbo core sources (needed by turbojpeg which tight.c depends on)
LIBJPEG_SRCS := \
    $(LIBJPEG_ROOT)/jcapimin.c \
    $(LIBJPEG_ROOT)/jcapistd.c \
    $(LIBJPEG_ROOT)/jccoefct.c \
    $(LIBJPEG_ROOT)/jccolor.c \
    $(LIBJPEG_ROOT)/jcdctmgr.c \
    $(LIBJPEG_ROOT)/jchuff.c \
    $(LIBJPEG_ROOT)/jcicc.c \
    $(LIBJPEG_ROOT)/jcinit.c \
    $(LIBJPEG_ROOT)/jcmainct.c \
    $(LIBJPEG_ROOT)/jcmarker.c \
    $(LIBJPEG_ROOT)/jcmaster.c \
    $(LIBJPEG_ROOT)/jcomapi.c \
    $(LIBJPEG_ROOT)/jcparam.c \
    $(LIBJPEG_ROOT)/jcphuff.c \
    $(LIBJPEG_ROOT)/jcprepct.c \
    $(LIBJPEG_ROOT)/jcsample.c \
    $(LIBJPEG_ROOT)/jctrans.c \
    $(LIBJPEG_ROOT)/jdapimin.c \
    $(LIBJPEG_ROOT)/jdapistd.c \
    $(LIBJPEG_ROOT)/jdatadst.c \
    $(LIBJPEG_ROOT)/jdatasrc.c \
    $(LIBJPEG_ROOT)/jdcoefct.c \
    $(LIBJPEG_ROOT)/jdcolor.c \
    $(LIBJPEG_ROOT)/jddctmgr.c \
    $(LIBJPEG_ROOT)/jdhuff.c \
    $(LIBJPEG_ROOT)/jdicc.c \
    $(LIBJPEG_ROOT)/jdinput.c \
    $(LIBJPEG_ROOT)/jdmainct.c \
    $(LIBJPEG_ROOT)/jdmarker.c \
    $(LIBJPEG_ROOT)/jdmaster.c \
    $(LIBJPEG_ROOT)/jdmerge.c \
    $(LIBJPEG_ROOT)/jdphuff.c \
    $(LIBJPEG_ROOT)/jdpostct.c \
    $(LIBJPEG_ROOT)/jdsample.c \
    $(LIBJPEG_ROOT)/jdtrans.c \
    $(LIBJPEG_ROOT)/jerror.c \
    $(LIBJPEG_ROOT)/jfdctflt.c \
    $(LIBJPEG_ROOT)/jfdctfst.c \
    $(LIBJPEG_ROOT)/jfdctint.c \
    $(LIBJPEG_ROOT)/jidctflt.c \
    $(LIBJPEG_ROOT)/jidctfst.c \
    $(LIBJPEG_ROOT)/jidctint.c \
    $(LIBJPEG_ROOT)/jidctred.c \
    $(LIBJPEG_ROOT)/jquant1.c \
    $(LIBJPEG_ROOT)/jquant2.c \
    $(LIBJPEG_ROOT)/jutils.c \
    $(LIBJPEG_ROOT)/jmemmgr.c \
    $(LIBJPEG_ROOT)/jmemnobs.c \
    $(LIBJPEG_ROOT)/jaricom.c \
    $(LIBJPEG_ROOT)/jcarith.c \
    $(LIBJPEG_ROOT)/jdarith.c \
    $(LIBJPEG_ROOT)/turbojpeg.c \
    $(LIBJPEG_ROOT)/transupp.c \
    $(LIBJPEG_ROOT)/jdatadst-tj.c \
    $(LIBJPEG_ROOT)/jdatasrc-tj.c \
    $(LIBJPEG_ROOT)/rdbmp.c \
    $(LIBJPEG_ROOT)/rdppm.c \
    $(LIBJPEG_ROOT)/wrbmp.c \
    $(LIBJPEG_ROOT)/wrppm.c \
    $(LIBJPEG_ROOT)/jsimd_none.c

LOCAL_SRC_FILES += $(LIBJPEG_SRCS)

LOCAL_CFLAGS := \
    -std=gnu99 \
    -D__ANDROID__ \
    -DLIBVNCSERVER_ALLOW=1 \
    -DLIBVNCSERVER_HAVE_LIBJPEG=1 \
    -DHAVE_LIBPTHREAD=1 \
    -DHAVE_PTHREAD_H=1 \
    -DHAVE_SYS_SYSMACROS_H=0 \
    -DHAVE_STDINT_H=1 \
    -DANDROID_NDK \
    -DBMP_SUPPORTED -DPPM_SUPPORTED \
    -Wno-unused-function \
    -Wno-unused-variable \
    -Wno-unused-parameter \
    -Wno-sign-compare \
    -Wno-int-conversion \
    -Wno-implicit-function-declaration \
    -Wno-unused-but-set-variable

# SIZEOF_SIZE_T depends on ABI
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_CFLAGS += -DSIZEOF_SIZE_T=8
else ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_CFLAGS += -DSIZEOF_SIZE_T=8
else
LOCAL_CFLAGS += -DSIZEOF_SIZE_T=4
endif

LOCAL_LDLIBS := -llog -landroid -lz

# Enable 16KB page size support for Android 15+
LOCAL_LDFLAGS := -Wl,-z,max-page-size=16384

include $(BUILD_SHARED_LIBRARY)
