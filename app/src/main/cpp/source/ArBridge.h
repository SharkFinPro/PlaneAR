#pragma once
#include <vector>
#include <mutex>

struct ArAnchor {
    int id;
    float matrix[16];
    int trackingState;
};

struct ArState {
    float cameraMatrix[16];
    bool hasCamera = false;

    std::vector<ArAnchor> anchors;
    int trackingState = 0;

    std::mutex mtx;
};

extern "C" {
void nativeSetArReady(bool ready);
}

extern ArState gArState;

