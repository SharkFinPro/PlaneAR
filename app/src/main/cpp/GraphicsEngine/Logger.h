#ifndef PLANEAR_LOGGER_H
#define PLANEAR_LOGGER_H

#include <android/log.h>

#define LOG_TAG "PlaneAR"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#endif //PLANEAR_LOGGER_H
