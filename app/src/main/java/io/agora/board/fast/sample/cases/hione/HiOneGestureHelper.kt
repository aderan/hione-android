package io.agora.board.fast.sample.cases.hione

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import com.herewhite.sdk.WhiteboardView

@SuppressLint("ClickableViewAccessibility")
class HiOneGestureHelper(whiteboardView: WhiteboardView) {
    private var gestureDetector: GestureDetector

    private var gestureEnabled: Boolean = false

    private var onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        val SWIPE_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val deltaX: Float = e2.x - e1.x
            val deltaY: Float = e2.y - e1.y

            if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                if (deltaX > 0) {
                    gestureListener?.onRightSwipe()
                } else {
                    gestureListener?.onLeftSwipe()
                }
            } else if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                if (deltaY > 0) {
                    gestureListener?.onDownSwipe()
                } else {
                    gestureListener?.onUpSwipe()
                }
            }
            return true
        }
    }

    init {
        gestureDetector = GestureDetector(whiteboardView.context, onGestureListener)
        whiteboardView.setOnTouchListener { _, event ->
            if (gestureEnabled) {
                gestureDetector.onTouchEvent(event)
            }
            false
        }
    }

    fun setEnable(enable: Boolean) {
        this.gestureEnabled = enable
    }

    interface GestureListener {
        fun onLeftSwipe() {}
        fun onRightSwipe() {}
        fun onUpSwipe() {}
        fun onDownSwipe() {}
    }

    private var gestureListener: GestureListener? = null

    fun setGestureListener(listener: GestureListener) {
        gestureListener = listener
    }
}