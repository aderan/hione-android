package io.agora.board.fast.sample.cases.hione

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.agora.board.fast.sample.R

class HiOneDotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    companion object {
        val colorSelected = Color.parseColor("#27f6b1")
        val colorNormal = Color.parseColor("#948f81")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.color = if (isSelected) colorSelected else colorNormal
        canvas?.drawCircle(width / 2f, height / 2f, width.toFloat() / 2, paint)
    }
}