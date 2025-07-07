#include <jni.h>

static const int diff[] = {3,7,-2,12,-8,5,-1,9};

jbyteArray obf(JNIEnv* env, jclass, jbyteArray arr){
    jsize len = (*env)->GetArrayLength(env, arr);
    jbyte* in = (*env)->GetByteArrayElements(env, arr, 0);
    jbyte* out = malloc(len);

    for(int i=0;i<len;i++){
        int r = i%5;
        int x = (in[i]^diff[i&7]) & 0xFF;
        out[i] = (x>>r)|(x<<(8-r));
    }
    jbyteArray ret = (*env)->NewByteArray(env,len);
    (*env)->SetByteArrayRegion(env,ret,0,len,out);
    free(out); (*env)->ReleaseByteArrayElements(env,arr,in,0);
    return ret;
}
jbyteArray reb(JNIEnv* env, jclass, jbyteArray arr){
    jsize len = (*env)->GetArrayLength(env, arr);
    jbyte* in = (*env)->GetByteArrayElements(env, arr, 0);
    jbyte* out = malloc(len);

    for(int i=0;i<len;i++){
        int r = i%5;
        int x = ((in[i]<<r)|(in[i]>>(8-r)))&0xFF;
        out[i] = (x^diff[i&7]) & 0xFF;
    }
    jbyteArray ret = (*env)->NewByteArray(env,len);
    (*env)->SetByteArrayRegion(env,ret,0,len,out);
    free(out); (*env)->ReleaseByteArrayElements(env,arr,in,0);
    return ret;
}

static JNINativeMethod tbl[]={
        {"o","([B)[B",(void*)obf},
        {"r","([B)[B",(void*)reb}
};
jint JNI_OnLoad(JavaVM* vm,void*){
    JNIEnv* e; (*vm)->GetEnv(vm,(void**)&e,JNI_VERSION_1_6);
    jclass cls = (*e)->FindClass(e,"x/y/SecBridge");
    (*e)->RegisterNatives(e,cls,tbl,2);
    return JNI_VERSION_1_6;
}
