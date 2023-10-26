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
import androidx.core.view.isVisible
import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.Appliance
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.model.FastAppliance
import io.agora.board.fast.sample.R
import io.agora.board.fast.sample.cases.hione.HiOneLayout.Item.Companion.HAND_INDEX
import io.agora.board.fast.sample.cases.hione.HiOneLayout.Item.Companion.LASER_INDEX
import io.agora.board.fast.sample.cases.hione.HiOneLayout.Item.Companion.PENCIL_INDEX
import io.agora.board.fast.sample.misc.toColorArray
import org.json.JSONObject

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

    // 手势检测工具类
    private var gestureHelper: HiOneGestureHelper? = null

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

    private var swipeListener: HiOneSwipeListener? = null

    private var textCount = 0

    private var lastTextId: String? = null

    private var lastPaint = FastAppliance.PENCIL

    data class Item(
        @DrawableRes val icon: Int,
        @StringRes val text: Int,
        val onClick: () -> Unit = {}
    ) {
        companion object {
            const val HAND_INDEX = 0
            const val LASER_INDEX = 1
            const val PENCIL_INDEX = 2
        }
    }

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

            getCameraState { x, y -> insertText(x.toInt(), y.toInt()) }
        }),
        Item(R.drawable.fast_ic_tool_undo, R.string.tool_undo, onClick = { fastRoom?.undo() }),
        Item(R.drawable.fast_ic_tool_redo, R.string.tool_redo, onClick = { fastRoom?.redo() }),
        Item(
            R.drawable.fast_ic_tool_clear,
            R.string.tool_clear,
            onClick = { fastRoom?.cleanScene() }),
    )

    private fun getCameraState(callback: (x: Double, y: Double) -> Unit) {
        val jsCode = "(function(a){return a ? a.camera : {}})(window.manager.focusedView);"
        fastRoom?.fastboardView?.run {
            val whiteboardView = findViewById<WhiteboardView>(R.id.fast_whiteboard_view)
            whiteboardView?.evaluateJavascript(jsCode) {
                val cameraObj = JSONObject(it)
                val centerX = cameraObj.optDouble("centerX", 0.0)
                val centerY = cameraObj.optDouble("centerY", 0.0)
                callback(centerX, centerY)
            }
        }
    }

    private fun insertText(centerX: Int, centerY: Int) {
        val defaultText = context.getString(R.string.tool_text)
        val offset = defaultTextOffset * (textCount++ % 5)
        fastRoom?.room?.insertText(
            centerX + offset,
            centerY + offset,
            defaultText,
            object : Promise<String> {
                override fun then(id: String) {
                    lastTextId = id
                    listener?.onTextInsert(context.getString(R.string.tool_text))
                }

                override fun catchEx(t: SDKError) {

                }
            })
    }

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

        editor = root.findViewById(R.id.editor)
        editor.setOnClickListener {
            updateEditorMode(!editor.isSelected)
            hidePaintView()
        }

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
        fastRoom?.room?.disableDeviceInputs(!selected)
        gestureHelper?.setEnable(!selected)
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
        this.gestureHelper = HiOneGestureHelper(getWhiteboardView()!!)
        this.gestureHelper?.setGestureListener(object : HiOneGestureHelper.GestureListener {
            override fun onLeftSwipe() {
                swipeListener?.onLeftSwipe()
            }

            override fun onRightSwipe() {
                swipeListener?.onRightSwipe()
            }
        })
        updateEditorMode(false)
    }

    private fun getWhiteboardView(): WhiteboardView? {
        return fastRoom?.fastboardView?.findViewById(R.id.fast_whiteboard_view)
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
            Appliance.HAND -> appliances.getChildAt(HAND_INDEX).isSelected = true
            Appliance.LASER_POINTER -> appliances.getChildAt(LASER_INDEX).isSelected = true
            in HiOnePaintView.tools -> appliances.getChildAt(PENCIL_INDEX).isSelected = true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fastRoom?.removeListener(roomListener)
    }

    interface HiOneLayoutListener {
        fun onCloudStorageClick()

        fun onTextInsert(text: String)

    }

    interface HiOneSwipeListener {
        fun onLeftSwipe()

        fun onRightSwipe()
    }

    fun setHiOneLayoutListener(listener: HiOneLayoutListener) {
        this.listener = listener
    }

    fun setHiOneSwapListener(listener: HiOneSwipeListener) {
        this.swipeListener = listener
    }
}