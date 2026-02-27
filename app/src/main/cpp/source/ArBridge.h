#pragma once
#include <vector>
#include <mutex>
#include <atomic>

struct AircraftDot {
    float x;
    float y;
    float distanceMeters;
};

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

    std::vector<AircraftDot> aircraftDots;

    std::mutex mtx;
};

extern "C" {
void nativeSetArReady(bool ready);
}
extern std::atomic<long long> gHwBufferCount;
extern ArState gArState;

