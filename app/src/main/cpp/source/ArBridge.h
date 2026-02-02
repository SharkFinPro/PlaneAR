#pragma once
#include <vector>
#include <mutex>
#include <android/hardware_buffer.h>

struct ArAnchor {
    int id;
    float matrix[16];
    int trackingState;
};

struct ArState {
    float cameraMatrix[16];
    bool hasCamera = false;
    int trackingState = 0;
    std::vector<ArAnchor> anchors;

    AHardwareBuffer* cameraHwBuffer = nullptr;
    bool hasCameraBuffer = false;
    std::mutex mtx;
};

extern "C" {
void nativeSetArReady(bool ready);
}

extern ArState gArState;

