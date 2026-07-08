# CMakeLists.txt for Openterface_Core JNI wrapper
# Builds libkeymod.so from Openterface_Core sources

cmake_minimum_required(VERSION 3.18.1)

project("keymod")

# Openterface_Core source files
set(KEYMOD_SOURCES
    Openterface_Core/src/keymod.c
    Openterface_Core/src/keymod_hid.c
    Openterface_Core/src/keymod_packets.c
    Openterface_Core/src/keymod_parser.c
    openterface_jni/keymod_jni.c
)

# Include directories
include_directories(
    Openterface_Core/include
)

# Build shared library
add_library(
    keymod
    SHARED
    ${KEYMOD_SOURCES}
)

# Link with log library
find_library(
    log-lib
    log
)

target_link_libraries(
    keymod
    ${log-lib}
)

# 16KB page alignment for Android 15+ Google Play requirement
target_link_options(keymod PRIVATE "-Wl,-z,max-page-size=16384")
