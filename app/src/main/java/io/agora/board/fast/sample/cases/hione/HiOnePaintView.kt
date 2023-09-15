package io.agora.board.fast.sample.cases.hione

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herewhite.sdk.domain.Appliance
import com.herewhite.sdk.domain.MemberState
import io.agora.board.fast.sample.R

class HiOnePaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val strokeWidths = doubleArrayOf(4.0, 8.0, 12.0, 16.0)

        val colors = intArrayOf(
            Color.parseColor("#EC3455"),
            Color.parseColor("#F5AD46"),
            Color.parseColor("#68AB5D"),
            Color.parseColor("#32C5FF"),
            Color.parseColor("#005BF6"),
            Color.parseColor("#6236FF"),
            Color.parseColor("#9E51B6"),
            Color.parseColor("#18FFE2"),

            Color.parseColor("#FFFFFF"),
            Color.parseColor("#65B1B2"),
            Color.parseColor("#333333"),
            Color.parseColor("#000000")
        )

        val tools = arrayOf(
            Appliance.PENCIL,
            Appliance.STRAIGHT,
            Appliance.RECTANGLE,
            Appliance.ELLIPSE
        )
    }

    private lateinit var toolsLayout: ViewGroup
    private lateinit var strokeLevelLayout: ViewGroup
    private lateinit var colorRecyclerView: RecyclerView
    private var colorAdapter = HiOneColorAdapter(colors)

    private var listener: PaintViewListener? = null

    init {
        initView(View.inflate(context, R.layout.layout_hi_one_paint, this))
    }

    private fun initView(root: View) {
        strokeLevelLayout = findViewById(R.id.stroke_level_layout)
        for (i in 0 until strokeLevelLayout.childCount) {
            strokeLevelLayout.getChildAt(i).setOnClickListener {
                listener?.onStrokeClick(strokeWidths[i])
            }
        }

        colorRecyclerView = findViewById(R.id.color_recycler_view)
        colorRecyclerView.adapter = colorAdapter
        colorRecyclerView.layoutManager = GridLayoutManager(context, 4)

        colorAdapter.onColorClickListener = {
            listener?.onColorClick(it)
        }

        toolsLayout = findViewById(R.id.tools_layout)
        for (i in 0 until toolsLayout.childCount) {
            toolsLayout.getChildAt(i).setOnClickListener {
                listener?.onApplianceClick(tools[i])
            }
        }

        this.setOnClickListener {
            // 消费点击事件
        }
    }

    fun updateMemberState(memberState: MemberState) {
        setStroke(memberState.strokeWidth)
        setAppliance(memberState.currentApplianceName)
        setColor(memberState.strokeColor)
    }

    private fun setColor(strokeColor: IntArray) {
        val color = Color.rgb(strokeColor[0], strokeColor[1], strokeColor[2]).toInt()
        colorAdapter.setColor(color)
    }

    private fun setAppliance(appliance: String) {
        for (i in 0 until toolsLayout.childCount) {
            toolsLayout.getChildAt(i).isSelected = tools[i] == appliance
        }
    }

    private fun setStroke(width: Double) {
        for (i in 0 until strokeLevelLayout.childCount) {
            strokeLevelLayout.getChildAt(i).isSelected = strokeWidths[i] == width
        }
    }

    interface PaintViewListener {
        fun onStrokeClick(width: Double)

        fun onColorClick(color: Int)

        fun onApplianceClick(appliance: String)
    }

    fun setPaintViewListener(listener: PaintViewListener) {
        this.listener = listener
    }
}