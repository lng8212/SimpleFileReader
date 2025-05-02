package com.longkd.simplefilereader.presentation.pdfviewer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.withTranslation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class PinchZoomRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    // Touch tracking for gesture state
    private var activePointerId = INVALID_POINTER_ID
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    // For detecting when to intercept touch events
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var hasDisallowedParentIntercept = false

    // Zoom and pan state
    private var scaleFactor = 1f
    private var maxZoom = MAX_ZOOM
    private var zoomDuration = ZOOM_DURATION
    private var isZoomingInProgress = false

    // Panning offsets and touch memory
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f

    init {
        setWillNotDraw(false)
    }

    /**
     * Intercepts touch events when zoomed in to handle panning properly
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isZoomingInProgress) {
            return true
        }

        // If we're zoomed in, we need to evaluate whether to intercept
        if (scaleFactor > 1f) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = ev.x
                    initialTouchY = ev.y
                    lastTouchX = ev.x
                    lastTouchY = ev.y
                    activePointerId = ev.getPointerId(0)
                    hasDisallowedParentIntercept = false

                    // Initially request parent not to intercept
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(ev.x - initialTouchX)
                    val dy = abs(ev.y - initialTouchY)

                    // If we've moved enough to consider it a deliberate gesture
                    if (dx > touchSlop || dy > touchSlop) {
                        hasDisallowedParentIntercept = true

                        // We're zoomed in, so we want to handle all touch events
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }
        }

        // Not zoomed in, let RecyclerView decide normally
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let gesture detectors see all events
        gestureDetector.onTouchEvent(ev)
        scaleDetector.onTouchEvent(ev)

        if (isZoomingInProgress) {
            return true // Block RecyclerView scroll during zoom
        }

        // If zoomed in, handle panning
        if (scaleFactor > 1f) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = ev.x
                    lastTouchY = ev.y
                    activePointerId = ev.getPointerId(0)

                    // Ensure parent doesn't intercept when zoomed
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        val pointerIndex = ev.findPointerIndex(activePointerId)
                        if (pointerIndex != -1) {
                            val x = ev.getX(pointerIndex)
                            val y = ev.getY(pointerIndex)
                            val dx = x - lastTouchX
                            val dy = y - lastTouchY

                            posX += dx
                            posY += dy
                            clampPosition()
                            invalidate()

                            lastTouchX = x
                            lastTouchY = y
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = ev.actionIndex
                    val pointerId = ev.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        lastTouchX = ev.getX(newPointerIndex)
                        lastTouchY = ev.getY(newPointerIndex)
                        activePointerId = ev.getPointerId(newPointerIndex)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activePointerId = INVALID_POINTER_ID

                    // Reset intercept flag when touch sequence ends
                    if (hasDisallowedParentIntercept) {
                        hasDisallowedParentIntercept = false
                    }
                }
            }

            return true // We've handled it when zoomed in
        }

        // Not zoomed in, let RecyclerView handle normally
        return super.onTouchEvent(ev)
    }

    /**
     * Transforms canvas for zoom + pan before drawing children.
     */
    override fun onDraw(canvas: Canvas) {
        canvas.withTranslation(posX, posY) {
            scale(scaleFactor, scaleFactor)
            super.onDraw(this)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.withTranslation(posX, posY) {
            scale(scaleFactor, scaleFactor)
            super.dispatchDraw(this)
        }
    }

    /**
     * Only allow vertical scrolling when NOT zoomed in.
     * When zoomed in, we're handling touch events ourselves for panning.
     */
    override fun canScrollVertically(direction: Int): Boolean {
        return scaleFactor <= 1f && super.canScrollVertically(direction)
    }

    /**
     * Corrects scrollbar offset for zoom state.
     */
    override fun computeVerticalScrollOffset(): Int {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return 0
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val firstView = layoutManager.findViewByPosition(firstVisible) ?: return 0

        val scrolledPast = -layoutManager.getDecoratedTop(firstView)
        val itemHeight = firstView.height.takeIf { it > 0 } ?: height
        val offset = (firstVisible * itemHeight + scrolledPast)

        return (offset * scaleFactor).toInt()
    }

    /**
     * Corrects scrollbar range for zoom state.
     */
    override fun computeVerticalScrollRange(): Int {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return height
        val itemCount = adapter?.itemCount ?: return height

        val visibleHeights = (0 until layoutManager.childCount).mapNotNull {
            layoutManager.getChildAt(it)?.height
        }

        val averageHeight = visibleHeights.average().takeIf { it > 0 } ?: height.toDouble()
        return (averageHeight * itemCount * scaleFactor).toInt()
    }

    /**
     * Only allow fling when NOT zoomed in
     */
    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        return if (scaleFactor > 1f) {
            false // Disable fling when zoomed in
        } else {
            super.fling(velocityX, velocityY)
        }
    }

    /**
     * GestureListener handles double-tap zoom.
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Cycle through zoom levels based on current scale.
            val targetScale = when {
                scaleFactor < 1.5f -> 1.5f
                scaleFactor < maxZoom -> maxZoom
                else -> 1f
            }
            zoomTo(targetScale, e.x, e.y, zoomDuration)
            return true
        }

        private fun zoomTo(targetScale: Float, focusX: Float, focusY: Float, duration: Long) {
            val startScale = scaleFactor
            val focusXInContent = (focusX - posX) / scaleFactor
            val focusYInContent = (focusY - posY) / scaleFactor

            ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    val scale = startScale + (targetScale - startScale) * fraction
                    scaleFactor = scale

                    posX = focusX - focusXInContent * scaleFactor
                    posY = focusY - focusYInContent * scaleFactor

                    clampPosition()
                    invalidate()
                    awakenScrollBars()
                }
                start()
            }
        }
    }

    /**
     * Clamps the panning translation to avoid over-scrolling beyond the content bounds.
     */
    private fun clampPosition() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Calculate the total width and height of the zoomed content
        val zoomedWidth = viewWidth * scaleFactor
        val zoomedHeight = viewHeight * scaleFactor

        // Calculate how much the content can be panned
        val maxPanX = (zoomedWidth - viewWidth) / 2
        val maxPanY = (zoomedHeight - viewHeight) / 2

        // Clamp the position values
        posX = posX.coerceIn(-maxPanX, maxPanX)
        posY = posY.coerceIn(-maxPanY, maxPanY)
    }

    /**
     * Handles pinch-to-zoom scaling with focal-point centering.
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isZoomingInProgress = true
            suppressLayout(true)
            // Ensure parent doesn't intercept during scaling
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactorChange = detector.scaleFactor
            if (scaleFactorChange in 0.98f..1.02f) return true // ignore micro-changes

            val newScale = (scaleFactor * scaleFactorChange).coerceIn(1f, maxZoom)
            if (newScale != scaleFactor) {
                val focusXInContent = (detector.focusX - posX) / scaleFactor
                val focusYInContent = (detector.focusY - posY) / scaleFactor

                scaleFactor = newScale

                posX = detector.focusX - focusXInContent * scaleFactor
                posY = detector.focusY - focusYInContent * scaleFactor

                clampPosition()
                invalidate()
                awakenScrollBars()
            }

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isZoomingInProgress = false
            suppressLayout(false)
        }
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
        private const val MAX_ZOOM = 3.0f
        private const val ZOOM_DURATION = 300L
    }
}