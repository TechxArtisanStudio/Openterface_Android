#ifndef _RFB_RFBCONFIG_H
#define _RFB_RFBCONFIG_H

/* rfb/rfbconfig.h - Hand-crafted for Android NDK (arm64-v8a / x86_64 / armeabi-v7a).
 *
 * This file replaces the cmake-generated rfbconfig.h that is normally created
 * by the libvncserver build system.  Android NDK (ndk-build) does not run
 * cmake, so we provide a static config with the values appropriate for the
 * Bionic C library on Android.
 */

/* Enable 24 bit per pixel in native framebuffer */
/* #undef LIBVNCSERVER_ALLOW24BPP */

/* work around when write() returns ENOENT but does not mean it */
/* #undef LIBVNCSERVER_ENOENT_WORKAROUND */

/* Android (Bionic) has <dirent.h> */
#define LIBVNCSERVER_HAVE_DIRENT_H 1

/* Android does not have <endian.h> directly; use <sys/endian.h> via sys/types.h */
/* #undef LIBVNCSERVER_HAVE_ENDIAN_H */

/* Android has <fcntl.h> */
#define LIBVNCSERVER_HAVE_FCNTL_H 1

/* Android has gettimeofday() */
#define LIBVNCSERVER_HAVE_GETTIMEOFDAY 1

/* Android does not have ftime() */
/* #undef LIBVNCSERVER_HAVE_FTIME */

/* Android has gethostbyname() */
#define LIBVNCSERVER_HAVE_GETHOSTBYNAME 1

/* Android has gethostname() */
#define LIBVNCSERVER_HAVE_GETHOSTNAME 1

/* Android has inet_ntoa() */
#define LIBVNCSERVER_HAVE_INET_NTOA 1

/* Android has memmove() */
#define LIBVNCSERVER_HAVE_MEMMOVE 1

/* Android has memset() */
#define LIBVNCSERVER_HAVE_MEMSET 1

/* Android has mkfifo() */
#define LIBVNCSERVER_HAVE_MKFIFO 1

/* Android has select() */
#define LIBVNCSERVER_HAVE_SELECT 1

/* Android has socket() */
#define LIBVNCSERVER_HAVE_SOCKET 1

/* Android has strchr() */
#define LIBVNCSERVER_HAVE_STRCHR 1

/* Android has strcspn() */
#define LIBVNCSERVER_HAVE_STRCSPN 1

/* Android has strdup() */
#define LIBVNCSERVER_HAVE_STRDUP 1

/* Android has strerror() */
#define LIBVNCSERVER_HAVE_STRERROR 1

/* Android has strstr() */
#define LIBVNCSERVER_HAVE_STRSTR 1

/* libjpeg-turbo is compiled in from libuvccamera/src/main/jni/libjpeg-turbo */
#define LIBVNCSERVER_HAVE_LIBJPEG 1

/* No libpng linked */
/* #undef LIBVNCSERVER_HAVE_LIBPNG */

/* Android has pthread via Bionic */
#define LIBVNCSERVER_HAVE_LIBPTHREAD 1

/* No win32 threads on Android */
/* #undef LIBVNCSERVER_HAVE_WIN32THREADS */

/* Android has zlib (linked via -lz) */
#define LIBVNCSERVER_HAVE_LIBZ 1

/* No lzo2 linked */
/* #undef LIBVNCSERVER_HAVE_LZO */

/* Android has <netinet/in.h> */
#define LIBVNCSERVER_HAVE_NETINET_IN_H 1

/* Android does not have <sys/endian.h> exposed in the same way */
/* #undef LIBVNCSERVER_HAVE_SYS_ENDIAN_H */

/* Android has <sys/socket.h> */
#define LIBVNCSERVER_HAVE_SYS_SOCKET_H 1

/* Android has <sys/stat.h> */
#define LIBVNCSERVER_HAVE_SYS_STAT_H 1

/* Android has <sys/time.h> */
#define LIBVNCSERVER_HAVE_SYS_TIME_H 1

/* Android has <sys/types.h> */
#define LIBVNCSERVER_HAVE_SYS_TYPES_H 1

/* Android has <sys/wait.h> */
#define LIBVNCSERVER_HAVE_SYS_WAIT_H 1

/* Android has <sys/uio.h> */
#define LIBVNCSERVER_HAVE_SYS_UIO_H 1

/* Android has <sys/resource.h> */
#define LIBVNCSERVER_HAVE_SYS_RESOURCE_H 1

/* Android has <unistd.h> */
#define LIBVNCSERVER_HAVE_UNISTD_H 1

/* Android has vfork() */
#define LIBVNCSERVER_HAVE_VFORK 1

/* Android does not have a separate <vfork.h> */
/* #undef LIBVNCSERVER_HAVE_VFORK_H */

/* Android has vprintf() */
#define LIBVNCSERVER_HAVE_VPRINTF 1

/* Android has fork() */
#define LIBVNCSERVER_HAVE_FORK 1

/* Android has working vfork() */
#define LIBVNCSERVER_HAVE_WORKING_VFORK 1

/* Android has mmap() */
#define LIBVNCSERVER_HAVE_MMAP 1

/* No <ws2tcpip.h> on Android (Windows only) */
/* #undef LIBVNCSERVER_HAVE_WS2TCPIP_H */

/* Enable IPv6 support - Android supports IPv6 */
#define LIBVNCSERVER_IPv6 1

/* Android's <sys/types.h> already defines in_addr_t */
/* #undef LIBVNCSERVER_NEED_INADDR_T */

/* Package name and version */
#define LIBVNCSERVER_PACKAGE_STRING  "LibVNCServer 0.9.14"
#define LIBVNCSERVER_PACKAGE_VERSION  "0.9.14"
#define LIBVNCSERVER_VERSION "0.9.14"
#define LIBVNCSERVER_VERSION_MAJOR 0
#define LIBVNCSERVER_VERSION_MINOR 9
#define LIBVNCSERVER_VERSION_PATCHLEVEL 14

/* No libgcrypt / GnuTLS / OpenSSL / SASL */
/* #undef LIBVNCSERVER_HAVE_LIBGCRYPT */
/* #undef LIBVNCSERVER_HAVE_GNUTLS */
/* #undef LIBVNCSERVER_HAVE_LIBSSL */
/* #undef LIBVNCSERVER_HAVE_SASL */

/* Enable websockets support */
#define LIBVNCSERVER_WITH_WEBSOCKETS 1

/* ARM64/x86_64 are little-endian; armeabi-v7a is also little-endian by default */
/* #undef LIBVNCSERVER_WORDS_BIGENDIAN */

/* pid_t, size_t, socklen_t are all defined by Android's <sys/types.h> */
/* So we do NOT define the fallback typedefs */
#define HAVE_LIBVNCSERVER_PID_T 1
#define HAVE_LIBVNCSERVER_SIZE_T 1
#define HAVE_LIBVNCSERVER_SOCKLEN_T 1

/* once: _RFB_RFBCONFIG_H */
#endif
