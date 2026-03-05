#ifndef PLANEAR_SCENEIDS_H
#define PLANEAR_SCENEIDS_H

#include <cstdint>

enum class SceneId : uint32_t {
    Home = 1,
    AR = 2,
    FlightHistory = 4,
    Settings = 6,
    Favorites = 7
};

#endif //PLANEAR_SCENEIDS_H
