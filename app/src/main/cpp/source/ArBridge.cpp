#include <jni.h>
#include <vector>
#include <mutex>
#include <algorithm>
#include "ArBridge.h"
#include <android/hardware_buffer_jni.h>
#include <android/hardware_buffer.h>
#include <atomic>
#include <android/log.h>
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "HB_TEST", __VA_ARGS__)


std::atomic<long long> gHwBufferCount{0};
ArState gArState;


extern "C" {

//camera pose
    JNIEXPORT void JNICALL
    Java_edu_osu_t22_planear_ARSessionManager_nativeUpdateCameraPose(
            JNIEnv* env,
            jobject /*thiz*/,
            jfloatArray poseMatrix_) {

        jsize len = env->GetArrayLength(poseMatrix_);
        if (len != 16) return;

        std::lock_guard<std::mutex> lock(gArState.mtx);
        env->GetFloatArrayRegion(poseMatrix_, 0, 16, gArState.cameraMatrix);
        gArState.hasCamera = true;
    }
//update existing anchor
    JNIEXPORT void JNICALL
    Java_edu_osu_t22_planear_ARSessionManager_nativeUpdateAnchorPose(
            JNIEnv* env,
            jobject  /*thiz*/,
            jint anchorId,
            jfloatArray poseMatrix_,
            jint trackingState) {

        jsize len = env->GetArrayLength(poseMatrix_);
        if (len != 16) return;

        float mat[16];
        env->GetFloatArrayRegion(poseMatrix_, 0, 16, mat);

        std::lock_guard<std::mutex> lock(gArState.mtx);

        auto& anchors = gArState.anchors;
        auto it = std::find_if(
                anchors.begin(),
                anchors.end(),
                [anchorId](const ArAnchor& a){ return a.id == anchorId; }
        );

        if (it == anchors.end()) return;

        std::copy(mat, mat + 16, it->matrix);
        it->trackingState = trackingState;
    }
//new anchor
    JNIEXPORT void JNICALL
    Java_edu_osu_t22_planear_ARSessionManager_nativeOnNewAircraftAnchor(
            JNIEnv* env,
            jobject /*thiz*/,
            jint anchorId,
            jfloatArray poseMatrix_) {

        jsize len = env->GetArrayLength(poseMatrix_);
        if (len != 16) return;

        float mat[16];
        env->GetFloatArrayRegion(poseMatrix_, 0, 16, mat);

        std::lock_guard<std::mutex> lock(gArState.mtx);

        ArAnchor anchor;
        anchor.id = anchorId;
        std::copy(mat, mat + 16, anchor.matrix);
        anchor.trackingState = 0;

        gArState.anchors.push_back(anchor);
    }
//tracking state
    JNIEXPORT void JNICALL
    Java_edu_osu_t22_planear_ARSessionManager_nativeOnTrackingStateChanged(
            JNIEnv* /*env*/,
            jobject /*thiz*/,
            jint trackingState) {

        std::lock_guard<std::mutex> lock(gArState.mtx);
        gArState.trackingState = trackingState;
    }

JNIEXPORT void JNICALL
Java_edu_osu_t22_planear_ARSessionManager_nativeOnHardwareBuffer(
        JNIEnv* env, jobject, jobject hardwareBufferObj, jlong) {

    if (!hardwareBufferObj) return;

    AHardwareBuffer* ahb = AHardwareBuffer_fromHardwareBuffer(env, hardwareBufferObj);
    if (!ahb) return;

    ++gHwBufferCount;
}

JNIEXPORT jlong JNICALL
Java_edu_osu_t22_planear_ARSessionManager_nativeGetHardwareBufferFrameCount(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    return (jlong)gHwBufferCount.load();
}

}
//just for testing to ensure successful session creation
bool gArReady = false;
extern "C" JNIEXPORT void JNICALL
Java_edu_osu_t22_planear_MainActivity_nativeSetArReady(JNIEnv*, jobject, jboolean ready) {
    std::lock_guard<std::mutex> lock(gArState.mtx);
    gArReady = ready;
}

