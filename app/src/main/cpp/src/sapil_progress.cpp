#include "../include/sapil.h"

// =============================================================================
// sapil_progress.cpp — SliceResult JNI conversion
// =============================================================================

namespace sapil {

jobject sliceResultToJava(JNIEnv* env, const SliceResult& result) {
    jclass cls = env->FindClass("com/u1/slicer/data/SliceResult");
    if (!cls) {
        SAPIL_LOGE("SliceResult class not found");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(cls, "<init>",
        "(ZLjava/lang/String;Ljava/lang/String;IFFF)V");
    if (!constructor) {
        SAPIL_LOGE("SliceResult constructor not found");
        return nullptr;
    }

    jstring jerror = env->NewStringUTF(result.error_message.c_str());
    jstring jgcode_path = env->NewStringUTF(result.gcode_path.c_str());

    // Use NewObjectA (jvalue array) to avoid C++ float→double and bool→int
    // promotions in varargs, which cause argument misalignment in JNI calls.
    jvalue args[7];
    args[0].z = result.success ? JNI_TRUE : JNI_FALSE;
    args[1].l = jerror;
    args[2].l = jgcode_path;
    args[3].i = result.total_layers;
    args[4].f = result.estimated_time_seconds;
    args[5].f = result.estimated_filament_mm;
    args[6].f = result.estimated_filament_grams;
    jobject obj = env->NewObjectA(cls, constructor, args);

    env->DeleteLocalRef(jerror);
    env->DeleteLocalRef(jgcode_path);
    env->DeleteLocalRef(cls);
    return obj;
}

} // namespace sapil
