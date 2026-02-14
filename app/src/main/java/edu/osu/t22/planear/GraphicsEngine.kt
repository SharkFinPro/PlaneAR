package edu.osu.t22.planear

class Renderer2D(private val ptr: Long) {
    external fun fill(r: Int, g: Int, b: Int, a: Int = 255)
    external fun rect(x: Float, y: Float, width: Float, height: Float)
    external fun ellipse(x: Float, y: Float, width: Float, height: Float)
    external fun textFont(fontName: String, size: Int)
    external fun textSize(size: Int)
    external fun text(text: String, x: Float, y: Float)
    external fun image(image: String, x: Float, y: Float, width: Float, height: Float)
}

class GraphicsEngineWrapper(private val ptr: Long) {
    fun getRenderer2D(): Renderer2D {
        return Renderer2D(nativeGetRenderer2DPtr(ptr))
    }

    private external fun nativeGetRenderer2DPtr(enginePtr: Long): Long
}