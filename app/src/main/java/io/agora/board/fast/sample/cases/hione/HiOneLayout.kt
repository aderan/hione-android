package io.agora.board.fast.sample.cases.hione

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.herewhite.sdk.domain.Appliance
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.WindowDocsEvent
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.model.FastAppliance
import io.agora.board.fast.sample.R
import io.agora.board.fast.sample.misc.toColorArray

class HiOneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val defaultTextSize = 15.0
        const val defaultTextOffset = 30

        val defaultStrokeWidth = HiOnePaintView.strokeWidths[0]
        val defaultTextColor = intArrayOf(0xff, 0x00, 0)
        val defaultColor = HiOnePaintView.colors[0].toColorArray()
    }

    // 手势检测控件
    private lateinit var gestureView: HiOneGestureView

    // 底部教具
    private lateinit var appliances: LinearLayout

    // 绘制教具容器
    private lateinit var paintLayout: ViewGroup

    // 绘制教具选择面板
    private lateinit var paintView: HiOnePaintView

    // 编辑模式按钮
    private lateinit var editor: View

    // 云服务按钮
    private lateinit var cloudService: View

    private var fastRoom: FastRoom? = null

    private var listener: HiOneLayoutListener? = null

    private var textCount = 0

    private var lastTextId: String? = null

    private var lastPaint = FastAppliance.PENCIL

    private val items = mutableListOf(
        Item(R.drawable.fast_ic_tool_hand_selector, R.string.tool_drag, onClick = {
            fastRoom?.setAppliance(FastAppliance.HAND)
        }),
        Item(R.drawable.fast_ic_tool_raser, R.string.tool_laser, onClick = {
            fastRoom?.setAppliance(FastAppliance.LASER_POINTER)
        }),
        Item(R.drawable.fast_ic_tool_pencil_selector, R.string.tool_paint, onClick = {
            fastRoom?.setAppliance(lastPaint)
            showPaintView()
        }),
        Item(R.drawable.fast_ic_tool_text_selector, R.string.tool_text, onClick = {
            fastRoom?.setAppliance(FastAppliance.SELECTOR)

            val defaultText = context.getString(R.string.tool_text)
            val offset = defaultTextOffset * (textCount++ % 5)
            fastRoom?.room?.insertText(offset, offset, defaultText, object : Promise<String> {
                override fun then(id: String) {
                    lastTextId = id;
                    listener?.onTextInsert(defaultText)
                }

                override fun catchEx(t: SDKError) {

                }
            })
        }),
        Item(R.drawable.fast_ic_tool_undo, R.string.tool_undo, onClick = { fastRoom?.undo() }),
        Item(R.drawable.fast_ic_tool_redo, R.string.tool_redo, onClick = { fastRoom?.redo() }),
        Item(
            R.drawable.fast_ic_tool_clear,
            R.string.tool_clear,
            onClick = { fastRoom?.cleanScene() }),
    )

    init {
        initView(View.inflate(context, R.layout.layout_hi_one, this))
    }

    private fun initView(root: View) {
        appliances = root.findViewById(R.id.appliances_layout)
        for (item in items) {
            val view = View.inflate(context, R.layout.layout_hi_one_appliance, null)
            view.findViewById<ImageView>(R.id.icon).setImageResource(item.icon)
            view.findViewById<TextView>(R.id.text).text = context.getString(item.text)
            view.setOnClickListener { item.onClick() }
            appliances.addView(view, generateLayoutParams())
        }

        gestureView = root.findViewById(R.id.gesture_view)
        gestureView.setGestureListener(object : HiOneGestureView.GestureListener {
            override fun onLeftSwipe() {
                // Right to Left Swipe
                fastRoom?.room?.dispatchDocsEvent(WindowDocsEvent.NextPage, null)
            }

            override fun onRightSwipe() {
                // Left to Right Swipe
                fastRoom?.room?.dispatchDocsEvent(WindowDocsEvent.PrevPage, null)
            }
        })

        editor = root.findViewById(R.id.editor)
        editor.setOnClickListener {
            updateEditorMode(!editor.isSelected)
        }
        updateEditorMode(true)

        cloudService = root.findViewById(R.id.cloud_service)
        cloudService.setOnClickListener {
            listener?.onCloudStorageClick()
        }

        paintLayout = root.findViewById(R.id.paint_layout)
        paintLayout.setOnClickListener {
            paintLayout.isVisible = false
        }

        paintView = root.findViewById(R.id.paint_view)
        paintView.setPaintViewListener(object : HiOnePaintView.PaintViewListener {
            override fun onStrokeClick(width: Double) {
                fastRoom?.room?.memberState = MemberState().apply {
                    strokeWidth = width
                }

                hidePaintView()
            }

            override fun onColorClick(color: Int) {
                fastRoom?.room?.memberState = MemberState().apply {
                    strokeColor = color.toColorArray()
                }

                hidePaintView()
            }

            override fun onApplianceClick(appliance: String) {
                lastPaint = FastAppliance.of(appliance, lastPaint.shapeType)
                fastRoom?.setAppliance(lastPaint)

                hidePaintView()
            }
        })
    }

    private fun showPaintView() {
        paintLayout.visibility = VISIBLE

        val anchor = appliances.getChildAt(2)
        val lp = paintView.layoutParams as LayoutParams
        lp.bottomMargin = appliances.height + dp2px(4)
        lp.leftMargin = anchor.left + (anchor.width - paintView.width) / 2
        paintView.layoutParams = lp
    }

    private fun hidePaintView() {
        paintLayout.visibility = INVISIBLE
    }

    private fun dp2px(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateEditorMode(selected: Boolean) {
        editor.isSelected = selected
        appliances.isVisible = selected
        gestureView.isVisible = !selected
    }

    private fun generateLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 1f
        }
    }

    private val roomListener = object : FastRoomListener {
        override fun onRoomStateChanged(state: RoomState?) {
            state?.memberState?.run {
                updateMemberState(this)
            }
        }
    }

    fun attachRoom(fastRoom: FastRoom) {
        fastRoom.addListener(roomListener)
        updateMemberState(fastRoom.room.memberState)
        // 设置默认参数
        fastRoom.room.memberState = MemberState().apply {
            textColor = defaultTextColor
            textSize = defaultTextSize
            strokeColor = defaultColor
            strokeWidth = defaultStrokeWidth
        }
        this.fastRoom = fastRoom
    }

    fun updateText(text: String) {
        if (lastTextId != null) {
            fastRoom?.room?.updateText(lastTextId, text)
        }
    }

    private fun updateMemberState(memberState: MemberState) {
        paintView.updateMemberState(memberState)

        appliances.forEach { it.isSelected = false }
        when (memberState.currentApplianceName) {
            Appliance.HAND -> appliances.getChildAt(0).isSelected = true
            Appliance.LASER_POINTER -> appliances.getChildAt(1).isSelected = true
            in HiOnePaintView.tools -> appliances.getChildAt(2).isSelected = true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fastRoom?.removeListener(roomListener)
    }

    data class Item(
        @DrawableRes val icon: Int,
        @StringRes val text: Int,
        val onClick: () -> Unit = {}
    )

    interface HiOneLayoutListener {
        fun onCloudStorageClick()

        fun onTextInsert(text: String)
    }

    fun setHiOneLayoutListener(listener: HiOneLayoutListener) {
        this.listener = listener
    }
}