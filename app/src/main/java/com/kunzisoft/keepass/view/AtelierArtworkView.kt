/* Local UI artwork. SPDX-License-Identifier: GPL-3.0-or-later */
package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.kunzisoft.keepass.R

/** Decorative vector artwork only. Never samples the screen or reads vault contents. */
class AtelierArtworkView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var preview: Boolean = false
        set(value) { field = value; invalidate() }

    private val colors = context.obtainStyledAttributes(attrs, R.styleable.AtelierArtworkView)
    private val start = colors.getColor(R.styleable.AtelierArtworkView_atelierSceneStart, 0xFFE4ECF8.toInt())
    private val end = colors.getColor(R.styleable.AtelierArtworkView_atelierSceneEnd, start)
    private val accent = colors.getColor(R.styleable.AtelierArtworkView_colorPrimary, 0xFF0062CC.toInt())
    private val surface = colors.getColor(R.styleable.AtelierArtworkView_colorSurface, 0xFFFFFFFF.toInt())
    private val ink = colors.getColor(R.styleable.AtelierArtworkView_colorOnSurface, 0xFF1B2432.toInt())
    private val motif = colors.getInt(R.styleable.AtelierArtworkView_atelierSceneMotif, 0)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private var gradient: LinearGradient? = null

    init {
        colors.recycle()
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gradient = if (w > 0 && h > 0) LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            start, end, Shader.TileMode.CLAMP) else null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = gradient
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.color = ColorUtils.setAlphaComponent(accent, 12)
        when (motif) {
            0, 4 -> for (i in 0..2) { // Ocean waves / rising dune contours.
                val base = h * (0.45f + i * 0.2f)
                path.reset()
                path.moveTo(0f, base)
                path.cubicTo(w * 0.35f, base - h * 0.55f,
                    w * 0.6f, base + h * (if (motif == 0) 0.4f else -0.25f), w, base - h * 0.15f)
                path.lineTo(w, h); path.lineTo(0f, h); path.close()
                canvas.drawPath(path, paint)
            }
            1 -> for (i in 0..3) { // Forest canopy.
                rect.set(w * (0.35f + i * 0.16f), -h * 0.5f, w * (0.85f + i * 0.16f), h * 1.15f)
                canvas.drawOval(rect, paint)
            }
            2 -> for (i in 0..3) { // Aurora ribbons.
                path.reset()
                path.moveTo(w * (0.3f + i * 0.18f), -h * 0.3f)
                path.cubicTo(w * 0.2f, h * 0.65f, w * 1.2f, h * 0.2f, w * 0.85f, h * 1.4f)
                path.lineTo(w * 1.3f, h); path.lineTo(w * 1.3f, 0f); path.close()
                canvas.drawPath(path, paint)
            }
            3 -> for (i in 0..3) { // Coral sunrise.
                canvas.drawCircle(w * 0.86f, h * 1.1f, h * (0.45f + i * 0.2f), paint)
            }
            5 -> for (i in 0..2) { // Polished jade layers.
                rect.set(w * (0.5f + i * 0.1f), -h * 0.3f, w * (1.2f + i * 0.1f), h * 1.4f)
                canvas.save()
                canvas.rotate(-35f, w * 0.8f, h * 0.5f)
                canvas.drawOval(rect, paint)
                canvas.restore()
            }
            6 -> for (i in 0..4) { // Sakura petals.
                canvas.save()
                canvas.rotate(i * 72f, w * 0.82f, h * 0.5f)
                rect.set(w * 0.66f, -h * 0.3f, w * 0.98f, h * 0.55f)
                canvas.drawOval(rect, paint)
                canvas.restore()
            }
            else -> for (i in 0..4) { // Graphite planes.
                path.reset()
                path.moveTo(w * (0.3f + i * 0.2f), 0f)
                path.lineTo(w * (0.6f + i * 0.2f), 0f)
                path.lineTo(w * (0.1f + i * 0.2f), h)
                path.lineTo(w * (-0.2f + i * 0.2f), h)
                path.close(); canvas.drawPath(path, paint)
            }
        }
        if (preview) {
            // Miniature UI made from the very same theme colors as the real components.
            paint.color = surface
            rect.set(w * 0.12f, h * 0.2f, w * 0.88f, h * 0.79f)
            canvas.drawRoundRect(rect, h * 0.09f, h * 0.09f, paint)
            paint.color = accent
            canvas.drawCircle(w * 0.25f, h * 0.39f, h * 0.065f, paint)
            rect.set(w * 0.19f, h * 0.6f, w * 0.81f, h * 0.68f)
            canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, paint)
            paint.color = ColorUtils.setAlphaComponent(ink, 95)
            rect.set(w * 0.37f, h * 0.35f, w * 0.76f, h * 0.4f)
            canvas.drawRoundRect(rect, h * 0.025f, h * 0.025f, paint)
            paint.color = ColorUtils.setAlphaComponent(ink, 40)
            rect.set(w * 0.37f, h * 0.46f, w * 0.63f, h * 0.5f)
            canvas.drawRoundRect(rect, h * 0.02f, h * 0.02f, paint)
        }
    }
}
