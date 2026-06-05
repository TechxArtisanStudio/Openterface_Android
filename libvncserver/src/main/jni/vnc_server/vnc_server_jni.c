/*
 * JNI bridge for VNC Server (libvncserver)
 *
 * Exposes libvncserver functionality to Java:
 * - Server lifecycle (start/stop)
 * - Frame pushing (raw RGBX frames from UVC camera)
 * - Input event callbacks (mouse/keyboard from VNC clients)
 */

#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <string.h>
#include <rfb/rfb.h>
#include <rfb/rfbconfig.h>

#define TAG "VncServerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

/* Frame buffer format: 32-bit RGBA */
#define BPP 4

/* Native handle structure */
typedef struct {
    rfbScreenInfoPtr screen;
    pthread_t server_thread;
    volatile int running;
    volatile int client_connected;
    JavaVM *jvm;
    jobject java_callbacks;
    char password[9];
    int width;
    int height;
    int port;
    int preferred_encoding;  /* -1 = auto (client decides), else RFB encoding constant */
    int quality_level;       /* -1 = client decides, 0-100 JPEG quality */
    int compress_level;      /* -1 = client decides, 0-9 zlib compression */
} vnc_server_t;

/* Forward declarations */
static void *vnc_server_thread(void *arg);
static void ptr_input(int buttonMask, int x, int y, rfbClientPtr cl);
static void kbd_input(rfbBool down, rfbKeySym keySym, rfbClientPtr cl);
static enum rfbNewClientAction client_connected(rfbClientPtr cl);
static void client_gone(rfbClientPtr cl);

/* JNI global references */
static jmethodID mid_on_client_connected;
static jmethodID mid_on_client_disconnected;
static jmethodID mid_on_pointer_event;
static jmethodID mid_on_keyboard_event;

/* Client gone hook - per-client data */
typedef struct {
    vnc_server_t *server;
    char client_host[256];
    int has_reported_encoding;
} client_data_t;

/* ============================================================
 * JNI_OnLoad - Register native methods and cache method IDs
 * ============================================================ */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    LOGI("VNC Server JNI loaded");
    return JNI_VERSION_1_6;
}

/* ============================================================
 * vncServerStart - Start the VNC server
 * Returns native handle as jlong (0 on failure)
 * ============================================================ */
JNIEXPORT jlong JNICALL Java_com_openterface_AOS_vnc_VncServerNative_vncServerStart(
    JNIEnv *env, jclass clazz, jstring j_password, jint j_port, jint j_width, jint j_height,
    jint j_encoding, jint j_quality, jint j_compress)
{
    vnc_server_t *server = calloc(1, sizeof(vnc_server_t));
    if (!server) {
        LOGE("Failed to allocate server structure");
        return 0;
    }

    /* Copy password */
    if (j_password) {
        const char *pass = (*env)->GetStringUTFChars(env, j_password, NULL);
        strncpy(server->password, pass, sizeof(server->password) - 1);
        server->password[sizeof(server->password) - 1] = '\0';
        (*env)->ReleaseStringUTFChars(env, j_password, pass);
    }

    server->width = j_width;
    server->height = j_height;
    server->port = j_port;
    server->preferred_encoding = j_encoding;
    server->quality_level = j_quality;
    server->compress_level = j_compress;

    /* Clamp compress level to valid Tight encoding range */
    if (server->compress_level > 3) server->compress_level = 3;
    server->running = 1;
    server->client_connected = 0;

    /* Set server-level forced encoding (-1 = auto, >= 0 = forced) */
    /* Will be applied to screen after rfbGetScreen returns */

    /* Save Java VM for callbacks from other threads */
    (*env)->GetJavaVM(env, &server->jvm);
    server->java_callbacks = NULL;

    /* Create libvncserver screen */
    server->screen = rfbGetScreen(NULL, NULL, j_width, j_height, 8, 3, BPP);
    if (!server->screen) {
        LOGE("Failed to create VNC screen");
        free(server);
        return 0;
    }

    /* Set screenData so callbacks can find our server struct */
    server->screen->screenData = server;

    server->screen->frameBuffer = (char *)malloc(j_width * j_height * BPP);
    if (!server->screen->frameBuffer) {
        LOGE("Failed to allocate framebuffer");
        free(server);
        return 0;
    }
    memset(server->screen->frameBuffer, 0, j_width * j_height * BPP);

    /* Configure server callbacks */
    server->screen->ptrAddEvent = ptr_input;
    server->screen->kbdAddEvent = kbd_input;
    server->screen->newClientHook = client_connected;

    server->screen->port = j_port;

    /* Set VNC password if provided */
    if (server->password[0] != '\0') {
        char **passwds = malloc(sizeof(char*) * 2);
        if (passwds) {
            passwds[0] = strdup(server->password);
            passwds[1] = NULL;
            server->screen->authPasswdData = (void*)passwds;
            server->screen->passwordCheck = rfbCheckPasswordByList;
        }
    }

    /* Set server-level forced encoding (-1 = auto/client decides, >= 0 = force specific encoding) */
    server->screen->forcePreferredEncoding = server->preferred_encoding;
    if (server->preferred_encoding >= 0) {
        LOGI("Forcing encoding=%d on all clients", server->preferred_encoding);
    }

    /* Set server-level quality and compression defaults */
    server->screen->forceQualityLevel = server->quality_level;
    server->screen->forceCompressLevel = server->compress_level;
    if (server->quality_level >= 0 || server->compress_level >= 0) {
        LOGI("Forcing quality=%d compress=%d on all clients", server->quality_level, server->compress_level);
    }

    LOGI("Starting VNC server on port %d, %dx%d", j_port, j_width, j_height);

    /* Start server thread */
    if (pthread_create(&server->server_thread, NULL, vnc_server_thread, server) != 0) {
        LOGE("Failed to create server thread");
        free(server->screen->frameBuffer);
        rfbScreenCleanup(server->screen);
        free(server);
        return 0;
    }

    return (jlong)server;
}

/* ============================================================
 * vncServerStop
 * ============================================================ */
JNIEXPORT void JNICALL Java_com_openterface_AOS_vnc_VncServerNative_vncServerStop(
    JNIEnv *env, jclass clazz, jlong j_handle)
{
    vnc_server_t *server = (vnc_server_t *)j_handle;
    if (!server) return;

    server->running = 0;

    pthread_join(server->server_thread, NULL);

    if (server->screen) {
        if (server->screen->frameBuffer) {
            free(server->screen->frameBuffer);
        }
        rfbScreenCleanup(server->screen);
    }

    if (server->java_callbacks) {
        JNIEnv *cb_env;
        if ((*server->jvm)->GetEnv(server->jvm, (void **)&cb_env, JNI_VERSION_1_6) == JNI_OK) {
            (*cb_env)->DeleteGlobalRef(cb_env, server->java_callbacks);
        }
    }

    LOGI("VNC server stopped");
    free(server);
}

/* ============================================================
 * vncServerPushFrame - Push raw RGBX framebuffer
 * ============================================================ */
JNIEXPORT void JNICALL Java_com_openterface_AOS_vnc_VncServerNative_vncServerPushFrame(
    JNIEnv *env, jclass clazz, jlong j_handle, jobject j_buffer, jint j_width, jint j_height)
{
    vnc_server_t *server = (vnc_server_t *)j_handle;
    if (!server || !server->running || !server->screen || !server->screen->frameBuffer) return;

    void *data = (*env)->GetDirectBufferAddress(env, j_buffer);
    if (!data) {
        LOGE("Failed to get direct buffer address");
        return;
    }

    jlong capacity = (*env)->GetDirectBufferCapacity(env, j_buffer);
    if (capacity != j_width * j_height * BPP) {
        LOGE("Buffer size mismatch: expected %lld, got %lld",
             (long long)(j_width * j_height * BPP), (long long)capacity);
        return;
    }

    /* Copy frame data to libvncserver's framebuffer */
    memcpy(server->screen->frameBuffer, data, j_width * j_height * BPP);

    /* Mark entire screen as dirty for next client update */
    rfbMarkRectAsModified(server->screen, 0, 0, j_width, j_height);
}

/* ============================================================
 * vncServerIsRunning
 * ============================================================ */
JNIEXPORT jboolean JNICALL Java_com_openterface_AOS_vnc_VncServerNative_vncServerIsRunning(
    JNIEnv *env, jclass clazz, jlong j_handle)
{
    vnc_server_t *server = (vnc_server_t *)j_handle;
    if (!server) return JNI_FALSE;
    return server->running ? JNI_TRUE : JNI_FALSE;
}

/* ============================================================
 * vncServerSetCallbacks - Set Java callback object
 * ============================================================ */
JNIEXPORT void JNICALL Java_com_openterface_AOS_vnc_VncServerNative_vncServerSetCallbacks(
    JNIEnv *env, jclass clazz, jlong j_handle, jobject callbacks)
{
    vnc_server_t *server = (vnc_server_t *)j_handle;
    if (!server) return;

    if (server->java_callbacks) {
        (*env)->DeleteGlobalRef(env, server->java_callbacks);
    }

    server->java_callbacks = (*env)->NewGlobalRef(env, callbacks);

    /* Cache method IDs */
    jclass cb_class = (*env)->GetObjectClass(env, callbacks);
    mid_on_client_connected = (*env)->GetMethodID(env, cb_class, "onClientConnected", "(Ljava/lang/String;)V");
    mid_on_client_disconnected = (*env)->GetMethodID(env, cb_class, "onClientDisconnected", "(Ljava/lang/String;)V");
    mid_on_pointer_event = (*env)->GetMethodID(env, cb_class, "onPointerEvent", "(III)V");
    mid_on_keyboard_event = (*env)->GetMethodID(env, cb_class, "onKeyboardEvent", "(IZ)V");
    (*env)->DeleteLocalRef(env, cb_class);
}

/* ============================================================
 * VNC Server Main Loop
 * ============================================================ */
static void *vnc_server_thread(void *arg) {
    vnc_server_t *server = (vnc_server_t *)arg;

    LOGI("VNC server thread started");

    /* Initialize the server */
    rfbInitServer(server->screen);

    /* Run the main loop */
    while (server->running) {
        rfbProcessEvents(server->screen, 50000);  // 50ms
    }

    LOGI("VNC server thread exiting");
    return NULL;
}

/* ============================================================
 * libvncserver Callbacks
 * ============================================================ */

static enum rfbNewClientAction client_connected(rfbClientPtr cl) {
    vnc_server_t *server = (vnc_server_t *)cl->screen->screenData;
    if (!server) return RFB_CLIENT_ACCEPT;

    server->client_connected = 1;

    /* Store client data for disconnect callback */
    client_data_t *cd = malloc(sizeof(client_data_t));
    cd->server = server;
    strncpy(cd->client_host, cl->host, sizeof(cd->client_host) - 1);
    cd->client_host[sizeof(cd->client_host) - 1] = '\0';
    cl->clientData = cd;

    LOGI("Client connected: %s (server encoding setting: %d)",
         cd->client_host, server->preferred_encoding);

    /* Log actual negotiated encoding */
    {
        const char *enc_name = "Unknown";
        switch (cl->preferredEncoding) {
            case 0:  enc_name = "Raw"; break;
            case 5:  enc_name = "Hextile"; break;
            case 6:  enc_name = "Zlib"; break;
            case 7:  enc_name = "Tight"; break;
            case 8:  enc_name = "ZlibHex"; break;
            case 16: enc_name = "ZRLE"; break;
            case 17: enc_name = "ZYWRLE"; break;
            default: enc_name = "Other"; break;
        }
        LOGI("Client negotiated encoding: %s (%d)", enc_name, cl->preferredEncoding);
    }

    /* Register client_gone hook */
    cl->clientGoneHook = client_gone;

    /* Call Java callback */
    JNIEnv *env;
    if ((*server->jvm)->AttachCurrentThread(server->jvm, (void **)&env, NULL) != JNI_OK) {
        return RFB_CLIENT_ACCEPT;
    }

    jstring ip = (*env)->NewStringUTF(env, cd->client_host);
    (*env)->CallVoidMethod(env, server->java_callbacks, mid_on_client_connected, ip);
    (*env)->DeleteLocalRef(env, ip);

    (*server->jvm)->DetachCurrentThread(server->jvm);

    LOGI("Client connected callback done");
    return RFB_CLIENT_ACCEPT;
}

/* Helper to report encoding from a client, called once per client when first input arrives */
static void report_client_encoding(rfbClientPtr cl) {
    if (!cl->clientData) return;
    client_data_t *cd = (client_data_t *)cl->clientData;
    if (cd->has_reported_encoding) return;
    cd->has_reported_encoding = 1;

    vnc_server_t *server = cd->server;
    int enc = cl->preferredEncoding;
    const char *enc_name = "Unknown";
    switch (enc) {
        case 0:  enc_name = "Raw"; break;
        case 5:  enc_name = "Hextile"; break;
        case 6:  enc_name = "Zlib"; break;
        case 7:  enc_name = "Tight"; break;
        case 8:  enc_name = "ZlibHex"; break;
        case 16: enc_name = "ZRLE"; break;
        case 17: enc_name = "ZYWRLE"; break;
        default: enc_name = "Other"; break;
    }

    JNIEnv *env;
    if ((*server->jvm)->AttachCurrentThread(server->jvm, (void **)&env, NULL) != JNI_OK) {
        return;
    }

    jstring j_enc = (*env)->NewStringUTF(env, enc_name);
    /* Call onEncodingChanged(ip, encoding_name) on the Java callback object */
    jmethodID mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, server->java_callbacks),
                                         "onEncodingChanged", "(Ljava/lang/String;Ljava/lang/String;)V");
    if (mid != NULL) {
        jstring j_ip = (*env)->NewStringUTF(env, cd->client_host);
        (*env)->CallVoidMethod(env, server->java_callbacks, mid, j_ip, j_enc);
        (*env)->DeleteLocalRef(env, j_ip);
    } else {
        LOGE("onEncodingChanged method not found");
    }
    (*env)->DeleteLocalRef(env, j_enc);

    (*server->jvm)->DetachCurrentThread(server->jvm);
    LOGI("Client %s using encoding: %s (%d)", cd->client_host, enc_name, enc);
}

/* Client gone callback - called when client disconnects */
static void client_gone(rfbClientPtr cl) {
    if (cl->clientData) {
        client_data_t *cd = (client_data_t *)cl->clientData;
        cl->clientData = NULL;  /* prevent double-free if called again */
        vnc_server_t *server = cd->server;

        server->client_connected = 0;

        JNIEnv *env;
        if ((*server->jvm)->AttachCurrentThread(server->jvm, (void **)&env, NULL) == JNI_OK) {
            jstring ip = (*env)->NewStringUTF(env, cd->client_host);
            (*env)->CallVoidMethod(env, server->java_callbacks, mid_on_client_disconnected, ip);
            (*env)->DeleteLocalRef(env, ip);
            (*server->jvm)->DetachCurrentThread(server->jvm);
        }

        LOGI("Client disconnected: %s", cd->client_host);
        free(cd);
    }
}

static void ptr_input(int buttonMask, int x, int y, rfbClientPtr cl) {
    LOGI("ptr_input: button=%d x=%d y=%d clientData=%p", buttonMask, x, y, cl->clientData);
    report_client_encoding(cl);
    if (cl->clientData) {
        client_data_t *cd = (client_data_t *)cl->clientData;
        vnc_server_t *server = cd->server;

        JNIEnv *env;
        if ((*server->jvm)->AttachCurrentThread(server->jvm, (void **)&env, NULL) != JNI_OK) {
            LOGE("ptr_input: AttachCurrentThread failed");
            return;
        }

        if (mid_on_pointer_event == NULL) {
            LOGE("ptr_input: mid_on_pointer_event is NULL");
            (*server->jvm)->DetachCurrentThread(server->jvm);
            return;
        }

        (*env)->CallVoidMethod(env, server->java_callbacks, mid_on_pointer_event,
                              (jint)buttonMask, (jint)x, (jint)y);
        LOGI("ptr_input: Java callback called");

        (*server->jvm)->DetachCurrentThread(server->jvm);
    } else {
        LOGE("ptr_input: clientData is NULL");
    }
}

static void kbd_input(rfbBool down, rfbKeySym keySym, rfbClientPtr cl) {
    LOGI("kbd_input: down=%d keySym=0x%x clientData=%p", down, (int)keySym, cl->clientData);
    report_client_encoding(cl);
    if (cl->clientData) {
        client_data_t *cd = (client_data_t *)cl->clientData;
        vnc_server_t *server = cd->server;

        JNIEnv *env;
        if ((*server->jvm)->AttachCurrentThread(server->jvm, (void **)&env, NULL) != JNI_OK) {
            LOGE("kbd_input: AttachCurrentThread failed");
            return;
        }

        if (mid_on_keyboard_event == NULL) {
            LOGE("kbd_input: mid_on_keyboard_event is NULL");
            (*server->jvm)->DetachCurrentThread(server->jvm);
            return;
        }

        (*env)->CallVoidMethod(env, server->java_callbacks, mid_on_keyboard_event,
                              (jint)keySym, (jboolean)(down ? JNI_TRUE : JNI_FALSE));
        LOGI("kbd_input: Java callback called");

        (*server->jvm)->DetachCurrentThread(server->jvm);
    } else {
        LOGE("kbd_input: clientData is NULL");
    }
}
