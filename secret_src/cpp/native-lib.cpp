#include <jni.h>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_x_y_SecBridge_o(JNIEnv* env, jclass, jbyteArray in) {
    return in;  // stub：直接回傳
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_x_y_SecBridge_r(JNIEnv* env, jclass, jbyteArray in) {
    return in;  // stub：直接回傳
}
