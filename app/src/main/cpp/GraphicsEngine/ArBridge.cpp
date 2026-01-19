#include <jni.h>
#include <vector>
#include <mutex>
#include <algorithm>
#include "ArBridge.h"
#include "GraphicsEngine.h"


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
}

bool gArReady = false;

extern "C" JNIEXPORT void JNICALL
Java_edu_osu_t22_planear_MainActivity_nativeSetArReady(JNIEnv*, jobject, jboolean ready) {
    std::lock_guard<std::mutex> lock(gArState.mtx);
    gArReady = ready;
} // this is just to prove the arcore session is being created will later make the box green in next step

