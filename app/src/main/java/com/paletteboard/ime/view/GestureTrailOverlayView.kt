package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.paletteboard.domain.model.TouchPoint

class GestureTrailOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun updateTrail(points: List<TouchPoint>, color: Int, strokeWidthPx: Float) {
        path.reset()
        if (points.isEmpty()) {
            invalidate()
            return
        }

        paint.color = color
        paint.strokeWidth = strokeWidthPx
        path.moveTo(points.first().x, points.first().y)
        points.drop(1).forEachIndexed { index, point ->
            val previous = points[index]
            val midpointX = (previous.x + point.x) / 2f
            val midpointY = (previous.y + point.y) / 2f
            path.quadTo(previous.x, previous.y, midpointX, midpointY)
        }
        invalidate()
    }

    fun clearTrail() {
        path.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }
}
