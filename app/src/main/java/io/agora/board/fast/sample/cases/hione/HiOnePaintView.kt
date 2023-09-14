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

    private lateinit var strokeLevelLayout: ViewGroup
    private lateinit var colorRecyclerView: RecyclerView
    private lateinit var toolPencil: View
    private lateinit var toolStraight: View
    private lateinit var toolRectangle: View
    private lateinit var toolCircle: View

    private var colorAdapter = HiOneColorAdapter(
        object : ArrayList<Int>() {
            init {
                add(Color.parseColor("#EC3455"))
                add(Color.parseColor("#F5AD46"))
                add(Color.parseColor("#68AB5D"))
                add(Color.parseColor("#32C5FF"))
                add(Color.parseColor("#005BF6"))
                add(Color.parseColor("#6236FF"))
                add(Color.parseColor("#9E51B6"))
                add(Color.parseColor("#6D7278"))

                add(Color.parseColor("#6D7278"))
                add(Color.parseColor("#6D7278"))
                add(Color.parseColor("#6D7278"))
                add(Color.parseColor("#6D7278"))
            }
        }
    )

    private var listener: PaintViewListener? = null

    init {
        initView(View.inflate(context, R.layout.layout_hi_one_paint, this))
    }

    private fun initView(root: View) {
        strokeLevelLayout = findViewById(R.id.stroke_level_layout)

        colorRecyclerView = findViewById(R.id.color_recycler_view)
        colorRecyclerView.adapter = colorAdapter
        colorRecyclerView.layoutManager = GridLayoutManager(context, 4)

        colorAdapter.onColorClickListener = {

        }

        toolPencil = findViewById(R.id.tool_pencil)
        toolStraight = findViewById(R.id.tool_straight)
        toolRectangle = findViewById(R.id.tool_rectangle)
        toolCircle = findViewById(R.id.tool_circle)
    }

    fun updateMemberState(memberState: MemberState) {
        setStroke(memberState.strokeWidth)
        setAppliance(memberState.currentApplianceName)
        setColor(memberState.strokeColor)
    }

    private fun setColor(strokeColor: IntArray) {
        val color = Color.rgb(strokeColor[0], strokeColor[1], strokeColor[2])
        colorAdapter.setColor(color)
    }

    private fun setAppliance(appliance: String) {
        toolPencil.isSelected = false
        toolStraight.isSelected = false
        toolRectangle.isSelected = false
        toolCircle.isSelected = false

        when (appliance) {
            Appliance.PENCIL -> toolPencil.isSelected = true
            Appliance.STRAIGHT -> toolStraight.isSelected = true
            Appliance.RECTANGLE -> toolRectangle.isSelected = true
            Appliance.ELLIPSE -> toolCircle.isSelected = true
        }
    }

    private fun setStroke(width: Double) {
        val index = when (width) {
            4.0 -> 1
            8.0 -> 2
            12.0 -> 3
            16.0 -> 4
            else -> -1
        }
        for (i in 0 until strokeLevelLayout.childCount) {
            strokeLevelLayout.getChildAt(i).isSelected = false
        }
        strokeLevelLayout.getChildAt(index)?.isSelected = true
    }

    interface PaintViewListener {
        fun onStrokeLevelClick(level: Int)

        fun onColorClick(color: Int)

        fun onToolClick(tool: Int)
    }

    fun setPaintViewListener(listener: PaintViewListener) {
        this.listener = listener
    }
}