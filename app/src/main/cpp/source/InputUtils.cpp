#include "InputUtils.h"

namespace InputUtils {
    static bool g_isTouching = false;

    bool handleTouchInput(struct android_app* pApp, float* mouseX, float* mouseY) {
        android_input_buffer* inputBuffer = android_app_swap_input_buffers(pApp);
        if (!inputBuffer) return false;

        bool tapDetected = false;
        for (uint64_t i = 0; i < inputBuffer->motionEventsCount; i++) {
            GameActivityMotionEvent* motionEvent = &inputBuffer->motionEvents[i];
            int action = motionEvent->action;
            int actionMasked = action & AMOTION_EVENT_ACTION_MASK;

            if (actionMasked == AMOTION_EVENT_ACTION_DOWN) {
                *mouseX = GameActivityPointerAxes_getX(&motionEvent->pointers[0]);
                *mouseY = GameActivityPointerAxes_getY(&motionEvent->pointers[0]);
                tapDetected = true;
                g_isTouching = true;
            } else if (actionMasked == AMOTION_EVENT_ACTION_MOVE) {
                *mouseX = GameActivityPointerAxes_getX(&motionEvent->pointers[0]);
                *mouseY = GameActivityPointerAxes_getY(&motionEvent->pointers[0]);
            } else if (actionMasked == AMOTION_EVENT_ACTION_UP ||
                       actionMasked == AMOTION_EVENT_ACTION_CANCEL) {
                g_isTouching = false;
            }
        }

        android_app_clear_motion_events(inputBuffer);
        return tapDetected;
    }

    bool isTouching() {
        return g_isTouching;
    }
}
