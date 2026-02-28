package edu.osu.t22.planear.graphicsEngine

// Enums mirror the C++ ge:: enums exactly — ordinal values are passed over JNI via static_cast<int>.
// Declaration order must not change without updating the C++ side as well.

enum class RectMode {
    CORNER,   // 0
    CORNERS,  // 1
    CENTER,   // 2
    RADIUS    // 3
}

enum class EllipseMode {
    CENTER,   // 0
    RADIUS,   // 1
    CORNER,   // 2
    CORNERS   // 3
}

enum class ImageMode {
    CORNER,   // 0
    CORNERS,  // 1
    CENTER    // 2
}

enum class TextAlignH {
    LEFT,     // 0
    CENTER,   // 1
    RIGHT     // 2
}

enum class TextAlignV {
    BASELINE, // 0
    TOP,      // 1
    CENTER,   // 2
    BOTTOM    // 3
}

class Renderer2D(private val ptr: Long) {
    /* Fill */
    external fun fill(r: Int, g: Int, b: Int, a: Int = 255)
    external fun fill(rgb: Int, a: Int = 255)

    /* Transforms */
    fun rotate(angle: Number) = rotate(angle.toFloat())
    fun translate(x: Number, y: Number) = translate(x.toFloat(), y.toFloat())
    fun scale(x: Number, y: Number) = scale(x.toFloat(), y.toFloat())
    fun scale(xy: Number) = scale(xy.toFloat())

    private external fun rotate(angle: Float)
    private external fun translate(x: Float, y: Float)
    private external fun scale(x: Float, y: Float)
    private external fun scale(xy: Float)
    external fun pushMatrix()
    external fun popMatrix()
    external fun resetMatrix()

    /* Draw modes */
    fun rectMode(mode: RectMode) = rectMode(mode.ordinal)
    fun ellipseMode(mode: EllipseMode) = ellipseMode(mode.ordinal)
    fun imageMode(mode: ImageMode) = imageMode(mode.ordinal)

    private external fun rectMode(mode: Int)
    private external fun ellipseMode(mode: Int)
    private external fun imageMode(mode: Int)

    /* Shapes */
    fun rect(x: Number, y: Number, width: Number, height: Number) = rect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    fun ellipse(x: Number, y: Number, width: Number, height: Number) = ellipse(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    fun triangle(x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) = triangle(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), x3.toFloat(), y3.toFloat())

    private external fun rect(x: Float, y: Float, width: Float, height: Float)
    private external fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)
    private external fun ellipse(x: Float, y: Float, width: Float, height: Float)

    /* Text */
    external fun textFont(fontName: String, size: Int)
    external fun textSize(size: Int)

    fun textAlign(alignH: TextAlignH, alignV: TextAlignV = TextAlignV.BASELINE) =
        textAlign(alignH.ordinal, alignV.ordinal)

    fun text(text: String, x: Number, y: Number) = text(text, x.toFloat(), y.toFloat())

    private external fun textAlign(alignH: Int, alignV: Int)

    private external fun text(text: String, x: Float, y: Float)

    /* Image */
    fun image(image: String, x: Number, y: Number, width: Number, height: Number) = image(image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

    private external fun image(image: String, x: Float, y: Float, width: Float, height: Float)

    external fun updateCameraBuffer(buffer: android.hardware.HardwareBuffer)
}

class GraphicsEngineWrapper(private val ptr: Long) {
    fun getRenderer2D(): Renderer2D {
        return Renderer2D(nativeGetRenderer2DPtr(ptr))
    }

    private external fun nativeGetRenderer2DPtr(enginePtr: Long): Long
}