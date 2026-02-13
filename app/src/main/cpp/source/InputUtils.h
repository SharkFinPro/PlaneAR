#ifndef PLANEAR_INPUTUTILS_H
#define PLANEAR_INPUTUTILS_H

#include <game-activity/native_app_glue/android_native_app_glue.h>

namespace InputUtils {
    bool handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY);
}

#endif //PLANEAR_INPUTUTILS_H
