package com.example.ingredio

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class MultiDotSpan(private val radius: Float = 5f, private val colors: List<Int>) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        val total = colors.size
        if (total == 0) return

        val oldColor = paint.color
        
        // Calculate the starting x position to center the dots
        // Spacing between dots is 2 * radius + some gap
        val gap = radius / 2
        val itemWidth = 2 * radius + gap
        val totalWidth = total * itemWidth - gap
        var x = (left + right) / 2f - totalWidth / 2f + radius

        for (color in colors) {
            paint.color = color
            canvas.drawCircle(x, (bottom + radius).toFloat(), radius, paint)
            x += itemWidth
        }
        
        paint.color = oldColor
    }
}
