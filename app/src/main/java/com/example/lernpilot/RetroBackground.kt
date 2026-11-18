package com.example.lernpilot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.drawable.Drawable

class RetroBackground(private val density: Float) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onBoundsChange(bounds: Rect) {
        paint.shader = LinearGradient(
            0f,
            0f,
            bounds.width().toFloat(),
            bounds.height().toFloat(),
            intArrayOf(
                Color.rgb(6, 12, 28),
                Color.rgb(9, 32, 43),
                Color.rgb(28, 9, 38)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        // Dark blue-to-purple background.
        canvas.drawRect(bounds, paint)

        val lines = Paint().apply {
            color = Color.argb(12, 70, 240, 235)
            strokeWidth = density
        }

        // Subtle CRT scanlines.
        var y = bounds.top.toFloat()
        while (y < bounds.bottom) {
            canvas.drawLine(
                bounds.left.toFloat(),
                y,
                bounds.right.toFloat(),
                y,
                lines
            )
            y += 5f * density
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}