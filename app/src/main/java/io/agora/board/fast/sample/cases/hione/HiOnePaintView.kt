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

class HiOnePaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var gestureView: HiOneGestureView
    private lateinit var appliances: LinearLayout
    private lateinit var editor: View
    private lateinit var cloudService: View

    private var fastRoom: FastRoom? = null

    private var listener: PaintViewListener? = null

    private var textCount = 0
    private var lastTextId = "";

    init {
        initView(View.inflate(context, R.layout.layout_hi_one_paint, this))
    }

    private fun initView(root: View) {

    }

    fun updateMemberState(memberState: MemberState) {

    }

    interface PaintViewListener {

    }

    fun setPaintViewListener(listener: PaintViewListener) {
        this.listener = listener
    }
}