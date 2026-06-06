# Top-level Android.mk for libvncserver module
LIBVNC_PATH := $(call my-dir)

# Build libvncserver library
include $(LIBVNC_PATH)/vnc_server/Android.mk
