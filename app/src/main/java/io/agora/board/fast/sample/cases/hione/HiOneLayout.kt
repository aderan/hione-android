package io.agora.board.fast.sample.cases.hione

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.WindowDocsEvent
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
import io.agora.board.fast.model.FastAppliance
import io.agora.board.fast.sample.R

class HiOneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val TEXT_SIZE = 15.0
        val TEXT_COLOR = intArrayOf(0xff, 0x00, 0)
        const val defaultText = "文本"
    }

    private lateinit var gestureView: HiOneGestureView
    private lateinit var appliances: LinearLayout
    private lateinit var paintLayout: View
    private lateinit var paintView: HiOnePaintView

    private lateinit var editor: View
    private lateinit var cloudService: View

    private var fastRoom: FastRoom? = null

    private var listener: HiOneLayoutListener? = null

    private var textCount = 0
    private var lastTextId = "";

    private var lastPaint = FastAppliance.PENCIL

    private val items = mutableListOf(
        Item(FastAppliance.HAND, R.drawable.fast_ic_tool_hand_selector, "拖拽"),
        Item(FastAppliance.LASER_POINTER, R.drawable.fast_ic_tool_raser, "激光"),
        Item(null, R.drawable.fast_ic_tool_pencil_selector, "画笔", onClick = {
            fastRoom?.setAppliance(lastPaint)
            paintLayout.isVisible = true
        }),
        Item(null, R.drawable.fast_ic_tool_text_selector, "文本", onClick = {
            fastRoom?.setAppliance(FastAppliance.SELECTOR)

            val offset = TEXT_SIZE.toInt() * 2 * (textCount++ % 5)
            fastRoom?.room?.insertText(offset, offset, defaultText, object : Promise<String> {
                override fun then(id: String) {
                    lastTextId = id;
                    listener?.onTextInsert(defaultText)
                }

                override fun catchEx(t: SDKError?) {

                }
            })
        }),
        Item(null, R.drawable.fast_ic_tool_undo, "撤销", onClick = { fastRoom?.undo() }),
        Item(null, R.drawable.fast_ic_tool_redo, "重做", onClick = { fastRoom?.redo() }),
        Item(null, R.drawable.fast_ic_tool_clear, "清除", onClick = { fastRoom?.cleanScene() }),
    )

    init {
        initView(View.inflate(context, R.layout.layout_hi_one, this))
    }

    private fun initView(root: View) {
        appliances = root.findViewById(R.id.appliances_layout)
        for (item in items) {
            val view = View.inflate(context, R.layout.layout_hi_one_appliance, null)
            view.findViewById<ImageView>(R.id.icon).setImageResource(item.icon)
            view.findViewById<TextView>(R.id.text).text = item.text
            view.setOnClickListener {
                if (item.appliance != null) {
                    fastRoom?.setAppliance(item.appliance)
                } else {
                    item.onClick()
                }
            }
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
            super.onRoomStateChanged(state)

            state?.memberState?.run {
                updateMemberState(this)
            }
        }
    }

    fun attachRoom(fastRoom: FastRoom) {
        this.fastRoom = fastRoom
        fastRoom.addListener(roomListener)
        updateMemberState(fastRoom.room.memberState)

        fastRoom.room.memberState = MemberState().apply {
            textColor = TEXT_COLOR
            textSize = TEXT_SIZE
        }
    }

    fun updateText(text: String, end: Boolean = false) {
        fastRoom?.room?.updateText(lastTextId, text)
        if (end) {

        }
    }

    private fun updateMemberState(memberState: MemberState) {
        val appliance = FastAppliance.of(memberState.currentApplianceName, memberState.shapeType)
        items.forEachIndexed { index, item ->
            appliances.getChildAt(index).isSelected = item.appliance == appliance
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fastRoom?.removeListener(roomListener)
    }

    data class Item(
        val appliance: FastAppliance?,
        @DrawableRes val icon: Int,
        val text: String,
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