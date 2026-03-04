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

    jobject obj = env->NewObject(cls, constructor,
        result.success,
        jerror,
        jgcode_path,
        result.total_layers,
        result.estimated_time_seconds,
        result.estimated_filament_mm,
        result.estimated_filament_grams);

    env->DeleteLocalRef(jerror);
    env->DeleteLocalRef(jgcode_path);
    env->DeleteLocalRef(cls);
    return obj;
}

} // namespace sapil
