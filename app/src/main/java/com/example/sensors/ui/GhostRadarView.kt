package com.example.sensors.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*
import kotlin.random.Random

class GhostRadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300FF9D")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8800FF9D")
        textSize = 28f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ghostBlipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1744")
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.SOLID)
    }

    private val ghostBlipInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var sweepAngle = 0f
    private var sweepAnimator: ValueAnimator? = null
    private var ghostScore = 0

    // Random blip positions
    private val blips = mutableListOf<PointF>()
    private var lastBlipSpawn = 0L

    init {
        startSweepAnimation()
    }

    fun setGhostScore(score: Int) {
        this.ghostScore = score
        val now = System.currentTimeMillis()
        if (score > 50 && now - lastBlipSpawn > 1200) {
            lastBlipSpawn = now
            val radiusFactor = Random.nextFloat() * 0.7f + 0.15f
            val angleRad = Math.toRadians(Random.nextDouble(0.0, 360.0))
            blips.clear()
            val count = if (score > 80) 3 else 1
            for (i in 0 until count) {
                val r = radiusFactor * (width.coerceAtLeast(100) / 2f - 40f)
                val a = angleRad + (i * 1.2)
                blips.add(PointF(cos(a).toFloat() * r, sin(a).toFloat() * r))
            }
        } else if (score <= 30) {
            blips.clear()
        }
        invalidate()
    }

    private fun startSweepAnimation() {
        sweepAnimator?.cancel()
        sweepAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                sweepAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val sweepGradient = SweepGradient(
            w / 2f,
            h / 2f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#1500FF9D"),
                Color.parseColor("#8000FF9D")
            ),
            floatArrayOf(0.0f, 0.75f, 1.0f)
        )
        sweepPaint.shader = sweepGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 20f

        if (radius <= 0) return

        // 1. Concentric circles
        for (i in 1..4) {
            val r = radius * (i / 4f)
            canvas.drawCircle(cx, cy, r, ringPaint)
        }

        // 2. Crosshair grid lines
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint)
        canvas.drawLine(
            cx - radius * 0.707f, cy - radius * 0.707f,
            cx + radius * 0.707f, cy + radius * 0.707f,
            gridPaint
        )
        canvas.drawLine(
            cx - radius * 0.707f, cy + radius * 0.707f,
            cx + radius * 0.707f, cy - radius * 0.707f,
            gridPaint
        )

        // 3. Cardinal Labels
        canvas.drawText("N", cx, cy - radius + 30f, textPaint)
        canvas.drawText("S", cx, cy + radius - 15f, textPaint)
        canvas.drawText("W", cx - radius + 25f, cy + 10f, textPaint)
        canvas.drawText("E", cx + radius - 25f, cy + 10f, textPaint)

        // 4. Rotating sweep sector
        canvas.save()
        canvas.rotate(sweepAngle, cx, cy)
        canvas.drawCircle(cx, cy, radius, sweepPaint)
        canvas.restore()

        // 5. Draw ghost blips if activity is detected
        for (blip in blips) {
            val bx = cx + blip.x
            val by = cy + blip.y
            val pulseRadius = 16f + (ghostScore * 0.12f)
            canvas.drawCircle(bx, by, pulseRadius, ghostBlipPaint)
            canvas.drawCircle(bx, by, 6f, ghostBlipInner)
        }
    }

    override fun onDetachedFromWindow() {
        sweepAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
