package com.example.screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.example.ui.theme.ScreenOverlayColors

/**
 * Draws the helper's finger (a circle) and strokes over every app on the shared phone.
 * Untouchable, so the user keeps using the phone normally. Needs "Display over other apps";
 * without it sharing still works, only the pointer is missing.
 */
object ScreenOverlay {
    private var view: PointerView? = null
    private val main = Handler(Looper.getMainLooper())

    fun canDraw(context: Context) = Settings.canDrawOverlays(context)

    fun show(context: Context) = main.post {
        if (view != null || !canDraw(context)) return@post
        val wm = context.getSystemService(WindowManager::class.java)
        val v = PointerView(context.applicationContext)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS }
        runCatching { wm.addView(v, lp) }.onSuccess {
            view = v
            ScreenShare.eventSink = { e -> main.post { v.onEvent(e) } }
        }
    }

    fun hide() = main.post {
        ScreenShare.eventSink = null
        val v = view ?: return@post
        runCatching { v.context.getSystemService(WindowManager::class.java).removeView(v) }
        view = null
    }

    @SuppressLint("ViewConstructor")
    private class PointerView(context: Context) : View(context) {
        private class Stroke(val path: Path = Path(), var endedAt: Long = 0)
        private val strokes = mutableListOf<Stroke>()
        private var current: Stroke? = null
        private var pointX = -1f
        private var pointY = -1f
        private var pointAt = 0L
        private val d = resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ScreenOverlayColors.POINTER }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ScreenOverlayColors.POINTER_RING; style = Paint.Style.STROKE; strokeWidth = 3 * d }
        private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ScreenOverlayColors.POINTER; style = Paint.Style.STROKE; strokeWidth = 5 * d
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }

        fun onEvent(e: ScreenEvent) {
            val now = System.currentTimeMillis()
            when (e) {
                is ScreenEvent.Point -> { pointX = e.x * width; pointY = e.y * height; pointAt = now }
                is ScreenEvent.Draw -> {
                    val x = e.x * width; val y = e.y * height
                    val s = current ?: Stroke().also { it.path.moveTo(x, y); current = it; strokes += it }
                    s.path.lineTo(x, y)
                }
                ScreenEvent.End -> { current?.endedAt = now; current = null }
            }
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: Canvas) {
            val now = System.currentTimeMillis()
            strokes.removeAll { it.endedAt > 0 && now - it.endedAt > STROKE_MS }
            for (s in strokes) {
                line.alpha = if (s.endedAt == 0L) 255 else (255 * (1f - (now - s.endedAt).toFloat() / STROKE_MS)).toInt().coerceIn(0, 255)
                canvas.drawPath(s.path, line)
            }
            val age = now - pointAt
            if (pointX >= 0 && age < POINT_MS) {
                val a = (255 * (1f - age.toFloat() / POINT_MS)).toInt().coerceIn(0, 255)
                fill.alpha = (a * 0.55f).toInt(); ring.alpha = a
                canvas.drawCircle(pointX, pointY, 22 * d, fill)
                canvas.drawCircle(pointX, pointY, 22 * d, ring)
            }
            if (strokes.isNotEmpty() || age < POINT_MS) postInvalidateOnAnimation()
        }

        companion object {
            const val STROKE_MS = 3_000L
            const val POINT_MS = 1_500L
        }
    }
}
