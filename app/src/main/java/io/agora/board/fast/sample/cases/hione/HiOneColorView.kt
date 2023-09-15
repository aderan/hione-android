package io.agora.board.fast.sample.cases.hione

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.agora.board.fast.sample.R
import kotlin.math.min

class HiOneColorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    companion object {
        val defaultColor = Color.parseColor("#FF0000")
        val defaultBorderColor = Color.parseColor("#FFFFFF")
    }

    private var colorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = defaultColor
    }

    private var boarderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = defaultBorderColor
    }

    private var checkIcon: Drawable? = null

    init {
        checkIcon = ContextCompat.getDrawable(context, R.drawable.hione_ic_check)
    }

    private val rect: RectF = RectF()
    private var borderWidth = 2

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h)

        borderWidth = size / 10
        boarderPaint.strokeWidth = borderWidth.toFloat()

        rect.set(
            borderWidth.toFloat(),
            borderWidth.toFloat(),
            (size - borderWidth).toFloat(),
            (size - borderWidth).toFloat()
        )

        val br = RectF(rect).apply {
            inset(rect.width() / 4, rect.height() / 4)
        }

        checkIcon?.setBounds(br.left.toInt(), br.top.toInt(), br.right.toInt(), br.bottom.toInt())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2, colorPaint)
        canvas?.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2, boarderPaint)

        if (isSelected) {
            if (luminance(colorPaint.color) > 230) {
                checkIcon?.setTint(Color.BLACK)
            } else {
                checkIcon?.setTint(Color.WHITE)
            }
            canvas?.run { checkIcon?.draw(canvas) }
        }
    }

    fun setColor(color: Int) {
        colorPaint.color = color
        invalidate()
    }

    fun setBorderColor(color: Int) {
        boarderPaint.color = color
        invalidate()
    }

    private fun luminance(color: Int): Double {
        return 0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)
    }
}