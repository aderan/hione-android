package io.agora.board.fast.sample.cases.hione

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.herewhite.sdk.domain.PptPage
import com.herewhite.sdk.domain.Scene
import com.herewhite.sdk.domain.WindowParams
import io.agora.board.fast.FastRoom
import io.agora.board.fast.Fastboard
import io.agora.board.fast.FastboardView
import io.agora.board.fast.extension.FastResource
import io.agora.board.fast.internal.FastConvertor
import io.agora.board.fast.model.ControllerId
import io.agora.board.fast.model.FastRegion
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.sample.Constants
import io.agora.board.fast.sample.R
import io.agora.board.fast.sample.cases.MultiWhiteBoardHelper
import io.agora.board.fast.sample.misc.KeyboardHeightProvider
import io.agora.board.fast.sample.misc.Utils
import io.agora.board.fast.sample.misc.hideSoftInput
import io.agora.board.fast.sample.misc.showSoftInput
import kotlin.math.abs

open class HiOneActivity : AppCompatActivity() {
    private lateinit var fastboardView: FastboardView
    private lateinit var fastboard: Fastboard
    private lateinit var fastRoom: FastRoom

    // 主控制布局
    private lateinit var hiOneLayout: HiOneLayout

    // 文本输入布局
    private lateinit var textInputLayout: View
    private lateinit var textInput: EditText
    private lateinit var textInputDone: View

    // 云盘文件列表
    private lateinit var filesLayout: View
    private var keyboardHeightProvider: KeyboardHeightProvider? = null

    private var multiWhiteBoardHelper: MultiWhiteBoardHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hi_one)

        initView()
        setupFastboard()
    }

    private fun initView() {
        hiOneLayout = findViewById(R.id.hi_one_layout)
        hiOneLayout.setHiOneLayoutListener(object : HiOneLayout.HiOneLayoutListener {
            override fun onCloudStorageClick() {
                filesLayout.isVisible = !filesLayout.isVisible
            }

            override fun onTextInsert(text: String) {
                enterTextInput()
                textInput.setText(text)
                textInput.setSelection(text.length)
            }
        })

        textInputLayout = findViewById(R.id.text_input_layout)
        textInput = findViewById(R.id.text_input)
        textInputDone = findViewById(R.id.text_input_done)

        textInputLayout.setOnClickListener {
            exitTextInput()
        }

        textInput.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                hiOneLayout.updateText(if (text.isNullOrEmpty()) " " else text.toString())
            }
        )
        textInputDone.setOnClickListener {
            exitTextInput()
        }

        initCloudLayout()

        keyboardHeightProvider = KeyboardHeightProvider(this)
            .setHeightListener(object : KeyboardHeightProvider.HeightListener {
                private var originBottomMargin: Int? = null
                override fun onHeightChanged(height: Int) {
                    if (originBottomMargin == null && textInputLayout.isVisible) {
                        originBottomMargin =
                            (textInputLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin
                    }
                    if (originBottomMargin != null) {
                        val lp = textInputLayout.layoutParams as FrameLayout.LayoutParams
                        lp.bottomMargin = height + originBottomMargin!!
                        textInputLayout.postDelayed({
                            textInputLayout.layoutParams = lp
                        }, 100)
                    }
                }
            })
    }

    private fun enterTextInput() {
        textInputLayout.visibility = View.VISIBLE
        textInput.requestFocus()
        textInput.showSoftInput()
    }

    private fun exitTextInput() {
        textInputLayout.visibility = View.INVISIBLE
        textInput.hideSoftInput()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            keyboardHeightProvider?.start()
        } else {
            keyboardHeightProvider?.stop()
        }
    }

    private fun setupFastboard() {
        fastboardView = findViewById(R.id.fastboard_view)
        fastboard = fastboardView.fastboard


        val roomOptions = FastRoomOptions(
            Constants.SAMPLE_APP_ID,
            Constants.SAMPLE_ROOM_UUID,
            Constants.SAMPLE_ROOM_TOKEN,
            getUserId(),
            FastRegion.CN_HZ
        )
        // window params
        val roomParams = roomOptions.roomParams.apply {
            windowParams = WindowParams()
                .setContainerSizeRatio(9f / 16)
                .setChessboard(false)
                .setFullscreen(true)
        }
        roomOptions.roomParams = roomParams

        fastRoom = fastboard.createFastRoom(roomOptions)
        multiWhiteBoardHelper = MultiWhiteBoardHelper(fastRoom)

        //set whiteboard and FastboardView background
        fastRoom.setResource(object : FastResource() {
            override fun getBackgroundColor(darkMode: Boolean): Int {
                return Color.BLACK
            }
        })
        fastRoom.join { room ->
            hiOneLayout.attachRoom(room)
        }

        // hide all interactive controllers
        hideController()
    }

    private fun hideController() {
        fastboardView.uiSettings.hideRoomController(
            ControllerId.RedoUndo,
            ControllerId.PageIndicator,
            ControllerId.ToolBox
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fastRoom.destroy()
    }

    private fun getUserId(): String? {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun initCloudLayout() {
        filesLayout = findViewById(R.id.files_layout)

        // 停止共享
        val stopShare: (name: String) -> Unit = { name ->
            multiWhiteBoardHelper?.destroyWhiteBoard(name)
        }
        // 切页
        val switchPage: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.switchWhiteBoard(name, index)
        }
        // 共享空白白板
        val startShareWhiteBoard: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.addWhiteBoard(
                name,
                arrayOf(Scene("1"), Scene("2"), Scene("3"), Scene("4"), Scene("5"))
            )
            multiWhiteBoardHelper?.switchWhiteBoard(name, index)
        }
        // 共享图片
        val startShareImage: (name: String, index: Int) -> Unit = { name, index ->
            val imageUrl =
                "https://flat-storage.oss-accelerate.aliyuncs.com/cloud-storage/2022-02/15/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda.png"
            val width = 512.0
            val height = 512.0
            multiWhiteBoardHelper?.addWhiteBoard(name, arrayOf(Scene("1", PptPage(imageUrl, width, height))))
            multiWhiteBoardHelper?.switchWhiteBoard(name, index)
        }
        // 共享PPT
        val pptScenes = FastConvertor.convertScenes(Utils.getDocPages("8da4cdc71a9845d385a5b58ddfa10b7e"))
        val startSharePpt: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.addWhiteBoard(name, pptScenes)
            multiWhiteBoardHelper?.switchWhiteBoard(name, index)
        }
        setupControllerItem(R.id.insert_whiteboard_1, "whiteboard_1", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_whiteboard_2, "whiteboard_2", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_whiteboard_3, "whiteboard_3", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_image, "Test.png", 0, startShare = startShareImage, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_ppt, "Test.ppt", pptScenes.size, startShare = startSharePpt, stopShare = stopShare, switchPage = switchPage)
    }

    private fun setupControllerItem(
        id: Int,
        name: String,
        pageCount: Int,
        startShare: (name: String, index: Int) -> Unit,
        stopShare: (name: String) -> Unit,
        switchPage: (name: String, index: Int) -> Unit
    ) {
        val controllerView = findViewById<View>(id)
        val tvName = controllerView.findViewById<TextView>(R.id.tvName)
        val tvPage = controllerView.findViewById<TextView>(R.id.tvPage)
        val ivPre = controllerView.findViewById<ImageView>(R.id.ivPrev)
        val ivNext = controllerView.findViewById<ImageView>(R.id.ivNext)
        val tvShare = controllerView.findViewById<TextView>(R.id.tvShare)
        var index = 0

        tvPage.isVisible = pageCount > 0
        ivPre.isVisible = pageCount > 0
        ivNext.isVisible = pageCount > 0
        tvName.text = name
        tvPage.text = "${index + 1}/$pageCount"
        tvShare.setText(R.string.share_start)
        tvShare.setTextColor(resources.getColor(R.color.blue))
        tvShare.setOnClickListener {
            if (tvShare.text == getString(R.string.share_start)) {
                startShare.invoke(name, index)
                tvShare.setText(R.string.share_stop)
                tvShare.setTextColor(resources.getColor(R.color.red))
            } else {
                stopShare.invoke(name)
                tvShare.setText(R.string.share_start)
                tvShare.setTextColor(resources.getColor(R.color.blue))
            }
        }
        ivPre.setOnClickListener {
            index = abs(--index + pageCount * 1000) % pageCount
            tvPage.text = "${index + 1}/$pageCount"
            switchPage.invoke(name, index)
        }
        ivNext.setOnClickListener {
            index = ++index % pageCount
            tvPage.text = "${index + 1}/$pageCount"
            switchPage.invoke(name, index)
        }
        controllerView.setOnClickListener {
            switchPage.invoke(name, index)
        }
    }

}