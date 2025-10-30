# Android build config for libusb
# Copyright © 2012-2013 RealVNC Ltd. <toby.gray@realvnc.com>
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
#

LOCAL_PATH := $(call my-dir)
LIBUSB_ROOT_REL := ../..
LIBUSB_ROOT_ABS := $(LOCAL_PATH)/../..

# libusb

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
  $(LIBUSB_ROOT_REL)/libusb/core.c \
  $(LIBUSB_ROOT_REL)/libusb/descriptor.c \
  $(LIBUSB_ROOT_REL)/libusb/hotplug.c \
  $(LIBUSB_ROOT_REL)/libusb/io.c \
  $(LIBUSB_ROOT_REL)/libusb/sync.c \
  $(LIBUSB_ROOT_REL)/libusb/strerror.c \
  $(LIBUSB_ROOT_REL)/libusb/os/linux_usbfs.c \
  $(LIBUSB_ROOT_REL)/libusb/os/events_posix.c \
  $(LIBUSB_ROOT_REL)/libusb/os/threads_posix.c \
  $(LIBUSB_ROOT_REL)/libusb/os/linux_netlink.c

LOCAL_C_INCLUDES += \
  $(LOCAL_PATH)/.. \
  $(LIBUSB_ROOT_ABS)/libusb \
  $(LIBUSB_ROOT_ABS)/libusb/os \
  $(LIBUSB_ROOT_ABS)/../ \
  $(LIBUSB_ROOT_ABS)/../include \

LOCAL_EXPORT_C_INCLUDES := \
  $(LIBUSB_ROOT_ABS)/ \
  $(LIBUSB_ROOT_ABS)/libusb

LOCAL_CFLAGS := -fvisibility=hidden -pthread

# add some flags
LOCAL_CFLAGS += $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_CFLAGS += -DANDROID_NDK
LOCAL_CFLAGS += -DACCESS_RAW_DESCRIPTORS
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays
LOCAL_EXPORT_LDLIBS += -llog
LOCAL_ARM_MODE := arm

LOCAL_LDLIBS := -llog

# Enable 16KB page size support for Android 15+
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384

ifeq ($(USE_PC_NAME),1)
  LOCAL_MODULE := usb-1.0
else
  LOCAL_MODULE := libusb1.0
  $(warning Building to legacy library name libusb1.0, which differs from pkg-config.)
  $(warning Use ndk-build USE_PC_NAME=1 to change the module name to the compatible usb-1.0.)
  $(warning USE_PC_NAME=1 may be the default in the future.)
endif

include $(BUILD_SHARED_LIBRARY)
