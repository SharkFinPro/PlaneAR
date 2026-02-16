#include "InputUtils.h"

namespace InputUtils {
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
            } else if (actionMasked == AMOTION_EVENT_ACTION_MOVE) {
                *mouseX = GameActivityPointerAxes_getX(&motionEvent->pointers[0]);
                *mouseY = GameActivityPointerAxes_getY(&motionEvent->pointers[0]);
            }
        }

        android_app_clear_motion_events(inputBuffer);
        return tapDetected;
    }
}
