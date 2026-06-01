package edu.osu.t22.planear

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * Accumulates all gesture and touch input each frame.
 * Call [onTouchEvent] from Activity.onTouchEvent to feed events.
 * Call [reset] once per frame (e.g. at the start of onArUpdateFrame) to clear state.
 * Query any val to read what happened during the previous frame.
 */
class FrameGestureDetector(context: Context) {

    /* Tap */
    /** Fires immediately on finger-lift. No double-tap delay. Use for most UI buttons/rows. */
    var singleTapUp: Boolean = false
        private set
    var singleTapUpPosition: Pair<Float, Float>? = null
        private set

    /** Fires ~300ms after finger-lift once a double-tap is ruled out. Use sparingly. */
    var singleTap: Boolean = false
        private set
    var singleTapPosition: Pair<Float, Float>? = null
        private set

    var doubleTap: Boolean = false
        private set
    var doubleTapPosition: Pair<Float, Float>? = null
        private set

    /* Long Press */
    var longPress: Boolean = false
        private set
    var longPressPosition: Pair<Float, Float>? = null
        private set

    /* Scroll / Drag */
    var isScrolling: Boolean = false
        private set
    /** Accumulated scroll delta for the frame. Positive X = finger moved left. */
    var scrollDelta: Pair<Float, Float> = Pair(0f, 0f)
        private set
    /** Current finger position during a scroll/drag. Use this for zone checks. */
    var scrollPosition: Pair<Float, Float>? = null
        private set

    /* Fling */
    var flung: Boolean = false
        private set
    var flingVelocity: Pair<Float, Float>? = null
        private set
    var flingDirection: FlingDirection? = null
        private set
    /** Where the fling gesture started (finger-down position). */
    var flingStartPosition: Pair<Float, Float>? = null
        private set

    /* Pinch / Scale */
    var isScaling: Boolean = false
        private set
    /** Accumulated scale factor product for the frame (1.0 = no change). */
    var scaleFactor: Float = 1f
        private set
    var scaleFocusPosition: Pair<Float, Float>? = null
        private set

    /* Raw pointer state */
    /** Number of fingers currently on screen. Persists until next ACTION_UP/CANCEL. */
    var fingerCount: Int = 0
        private set
    /** True while at least one finger is on screen. NOT reset each frame. */
    var isTouching: Boolean = false
        private set
    var anyTouchDown: Boolean = false
        private set
    /** Position of the initial finger-down this frame. */
    var touchDownPosition: Pair<Float, Float>? = null
        private set

    /* Internals */
    private var touchDownConsumed = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            anyTouchDown = true
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            singleTapUp = true
            singleTapUpPosition = Pair(e.x, e.y)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            singleTap = true
            singleTapPosition = Pair(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleTap = true
            doubleTapPosition = Pair(e.x, e.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            longPress = true
            longPressPosition = Pair(e.x, e.y)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            isScrolling = true
            scrollDelta = Pair(scrollDelta.first + distanceX, scrollDelta.second + distanceY)
            scrollPosition = Pair(e2.x, e2.y)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            flung = true
            flingVelocity = Pair(velocityX, velocityY)
            flingDirection = resolveFlingDirection(velocityX, velocityY)
            flingStartPosition = e1?.let { Pair(it.x, it.y) }
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                scaleFactor *= detector.scaleFactor
                scaleFocusPosition = Pair(detector.focusX, detector.focusY)
                return true
            }
        }
    )

    /* Public API */

    fun markTouchDownConsumed() {
        touchDownConsumed = true
    }

    /** Feed a MotionEvent from Activity.onTouchEvent. */
    fun onTouchEvent(event: MotionEvent) {
        // Update persistent touch state — intentionally NOT reset each frame
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                fingerCount = event.pointerCount
                isTouching  = true
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    anyTouchDown      = true
                    touchDownPosition = Pair(event.x, event.y)
                    touchDownConsumed = false
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                fingerCount = 0
                isTouching  = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                fingerCount = event.pointerCount - 1
            }
        }

        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
    }

    /**
     * Reset per-frame gesture state. Call once per frame before reading any values.
     * Does NOT reset [isTouching] or [fingerCount] — those persist across frames.
     */
    fun reset() {
        singleTapUp = false
        singleTapUpPosition = null

        singleTap = false
        singleTapPosition = null

        doubleTap = false
        doubleTapPosition = null

        longPress = false
        longPressPosition = null

        isScrolling = false
        scrollDelta = Pair(0f, 0f)
        scrollPosition = null

        flung = false
        flingVelocity = null
        flingDirection = null
        flingStartPosition = null

        isScaling = false
        scaleFactor = 1f
        scaleFocusPosition = null

        anyTouchDown = false

        if (touchDownConsumed) {
            touchDownPosition = null
            touchDownConsumed = false
        }

        // isTouching and fingerCount intentionally NOT reset here
    }

    /** True if any movement-type gesture occurred this frame. */
    val hasMovement: Boolean
        get() = isScrolling || isScaling || flung

    /* Helpers */

    enum class FlingDirection { UP, DOWN, LEFT, RIGHT }

    private fun resolveFlingDirection(vx: Float, vy: Float): FlingDirection {
        return if (Math.abs(vx) > Math.abs(vy)) {
            if (vx > 0) FlingDirection.RIGHT else FlingDirection.LEFT
        } else {
            if (vy > 0) FlingDirection.DOWN else FlingDirection.UP
        }
    }
}