/*
 * JNI wrapper for Openterface_Core (keymod library)
 * Bridges Java KeyBoardManager/MouseManager to C HID implementation
 */

#include <jni.h>
#include <string.h>
#include "Openterface_Core/include/keymod.h"

#define TAG "KeymodJNI"

/* --- JNI Helpers --- */

static void throw_exception(JNIEnv *env, const char *msg) {
    jclass ex = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (ex) (*env)->ThrowNew(env, ex, msg);
}

/* --- Keyboard HID Functions --- */

JNIEXPORT jint JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_hidCode(JNIEnv *env, jclass clazz, jstring keyName) {
    const char *name = (*env)->GetStringUTFChars(env, keyName, NULL);
    int code = km_hid_code(name);
    (*env)->ReleaseStringUTFChars(env, keyName, name);
    return code;
}

JNIEXPORT jbyteArray JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_buildKeyboardPacket(
        JNIEnv *env, jclass clazz, jint modifiers, jintArray keys) {

    jsize num_keys = (*env)->GetArrayLength(env, keys);
    if (num_keys > 6) num_keys = 6;

    jint *key_array = (*env)->GetIntArrayElements(env, keys, NULL);
    uint8_t key_bytes[6] = {0};
    for (int i = 0; i < num_keys && i < 6; i++) {
        key_bytes[i] = (uint8_t)key_array[i];
    }
    (*env)->ReleaseIntArrayElements(env, keys, key_array, JNI_ABORT);

    uint8_t packet[KM_PKT_KEYBOARD_SIZE];
    km_build_keyboard(packet, (uint8_t)modifiers, key_bytes, num_keys);

    jbyteArray result = (*env)->NewByteArray(env, KM_PKT_KEYBOARD_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, KM_PKT_KEYBOARD_SIZE, (jbyte*)packet);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_buildPressRelease(
        JNIEnv *env, jclass clazz, jint modifiers, jint hidCode) {

    uint8_t packet[2 * KM_PKT_KEYBOARD_SIZE];
    km_build_press_release(packet, (uint8_t)modifiers, (uint8_t)hidCode);

    jbyteArray result = (*env)->NewByteArray(env, 2 * KM_PKT_KEYBOARD_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, 2 * KM_PKT_KEYBOARD_SIZE, (jbyte*)packet);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_buildKeyboardRelease(
        JNIEnv *env, jclass clazz) {

    // Build a release packet (all zeros after header)
    uint8_t packet[KM_PKT_KEYBOARD_SIZE];
    memset(packet, 0, sizeof(packet));
    packet[0] = 0x57;
    packet[1] = 0xAB;
    packet[2] = 0x00;
    packet[3] = KM_CMD_KB;
    packet[4] = DATA_LEN_KB;
    packet[KM_PKT_KEYBOARD_SIZE - 1] = km_checksum(packet, KM_PKT_KEYBOARD_SIZE);

    jbyteArray result = (*env)->NewByteArray(env, KM_PKT_KEYBOARD_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, KM_PKT_KEYBOARD_SIZE, (jbyte*)packet);
    return result;
}

/* --- Mouse HID Functions --- */

JNIEXPORT jbyteArray JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_buildMouseAbsPacket(
        JNIEnv *env, jclass clazz, jint buttons, jint x, jint y, jint wheel) {

    uint8_t packet[KM_PKT_MOUSE_ABS_SIZE];
    km_build_mouse_abs(packet, (uint8_t)buttons, (uint16_t)x, (uint16_t)y, (int8_t)wheel);

    jbyteArray result = (*env)->NewByteArray(env, KM_PKT_MOUSE_ABS_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, KM_PKT_MOUSE_ABS_SIZE, (jbyte*)packet);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_buildMouseRelPacket(
        JNIEnv *env, jclass clazz, jint buttons, jint dx, jint dy, jint wheel) {

    uint8_t packet[KM_PKT_MOUSE_REL_SIZE];
    km_build_mouse_rel(packet, (uint8_t)buttons, (int8_t)dx, (int8_t)dy, (int8_t)wheel);

    jbyteArray result = (*env)->NewByteArray(env, KM_PKT_MOUSE_REL_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, KM_PKT_MOUSE_REL_SIZE, (jbyte*)packet);
    return result;
}

/* --- Utility Functions --- */

JNIEXPORT jint JNICALL
Java_com_openterface_AOS_jni_KeymodJNI_checksum(JNIEnv *env, jclass clazz, jbyteArray data) {
    jsize len = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);

    uint8_t result = km_checksum((uint8_t*)bytes, len);

    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

/* --- Array Helpers --- */

static jbyteArray bytes_to_jbyteArray(JNIEnv *env, const uint8_t *data, size_t len) {
    jbyteArray result = (*env)->NewByteArray(env, len);
    if (result) {
        (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte*)data);
    }
    return result;
}
