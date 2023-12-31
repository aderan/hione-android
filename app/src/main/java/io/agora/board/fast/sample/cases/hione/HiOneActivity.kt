package io.agora.board.fast.sample.cases.hione

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.herewhite.sdk.domain.AnimationMode
import com.herewhite.sdk.domain.GlobalState
import com.herewhite.sdk.domain.PptPage
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.Scene
import com.herewhite.sdk.domain.WhiteDisplayerState
import com.herewhite.sdk.domain.WindowParams
import io.agora.board.fast.FastException
import io.agora.board.fast.FastRoom
import io.agora.board.fast.FastRoomListener
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
import io.agora.board.fast.sample.cases.MultiWhiteBoardHelper.BoardItemStatus
import io.agora.board.fast.sample.misc.KeyboardHeightProvider
import io.agora.board.fast.sample.misc.Utils
import io.agora.board.fast.sample.misc.hideSoftInput
import io.agora.board.fast.sample.misc.showSoftInput
import kotlin.math.abs
import kotlin.random.Random

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
    private val globalStateUid = Random(System.currentTimeMillis()).nextInt(1000) + 10000
    private var fastRoomListener :FastRoomListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hi_one)

        initView()
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

        // 切页滑动监听
        hiOneLayout.setHiOneSwapListener(object: HiOneLayout.HiOneSwipeListener{

            override fun onLeftSwipe() {
                // 翻到下一页
                val helper = multiWhiteBoardHelper ?: return
                val currWhiteBoard = helper.whiteBoardList.find { it.status == BoardItemStatus.active } ?: return
                val targetPageIndex = currWhiteBoard.activityPage + 1
                if(targetPageIndex < currWhiteBoard.totalPage){
                    multiWhiteBoardHelper?.switchWhiteBoard(currWhiteBoard.name, targetPageIndex){
                        updateGlobalState()
                        fastRoom.room.scalePptToFit(AnimationMode.Immediately)
                        runOnUiThread {
                            initCloudLayout()
                        }
                    }
                }
            }

            override fun onRightSwipe() {
                // 翻到上一页
                val helper = multiWhiteBoardHelper ?: return
                val currWhiteBoard = helper.whiteBoardList.find { it.status == BoardItemStatus.active } ?: return
                val targetPageIndex = currWhiteBoard.activityPage - 1
                if(targetPageIndex >= 0){
                    multiWhiteBoardHelper?.switchWhiteBoard(currWhiteBoard.name, targetPageIndex){
                        updateGlobalState()
                        fastRoom.room.scalePptToFit(AnimationMode.Immediately)
                        runOnUiThread {
                            initCloudLayout()
                        }
                    }
                }
            }
        })

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

        findViewById<Button>(R.id.btnOpenWhiteBoard).setOnClickListener {
            setupFastboard()
        }

        findViewById<Button>(R.id.btnCloseWhiteBoard).setOnClickListener {
            val fastboardViewContainer = findViewById<FrameLayout>(R.id.fastboard_view_container)
            fastboardViewContainer.removeAllViews()
            fastRoom.removeListener(fastRoomListener)
            fastRoom.destroy()
        }
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
        val fastboardViewContainer = findViewById<FrameLayout>(R.id.fastboard_view_container)
        fastboardView = FastboardView(this)
        fastboardViewContainer.removeAllViews()
        fastboardViewContainer.addView(fastboardView)
        fastboard = fastboardView.fastboard

        val roomOptions = FastRoomOptions(
            Constants.SAMPLE_APP_ID,
            Constants.SAMPLE_ROOM_UUID,
            Constants.SAMPLE_ROOM_TOKEN,
            globalStateUid.toString(),
            FastRegion.CN_HZ
        )
        // window params
        val roomParams = roomOptions.roomParams.apply {
            windowParams = WindowParams()
                .setContainerSizeRatio(3f / 4)
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
        Log.e("HiOneActivity", "FastRoom Join >> start")
        fastRoom.join { room ->
            Log.e("HiOneActivity", "FastRoom Join >> End")
            // hiOneLayout.attachRoom(room)
        }
        WhiteDisplayerState.setCustomGlobalStateClass(GlobalInfo::class.java)

        fastRoomListener = object : FastRoomListener {

            override fun onRoomReadyChanged(fastRoom: FastRoom?) {
                super.onRoomReadyChanged(fastRoom)
                Log.d("HiOneActivity", "onRoomReadyChanged >> isReady = ${fastRoom?.isReady}")

                Log.d(
                    "HiOneActivity",
                    "onRoomReadyChanged >> state = ${fastRoom?.room?.globalState}"
                )
                if (fastRoom?.isReady == true) {
                    hiOneLayout.attachRoom(fastRoom)

                    // 新
                    multiWhiteBoardHelper?.addWhiteBoard(
                        "blackBoard" + java.util.Random().nextInt(1000),
                        arrayOf(Scene("1"), Scene("2"), Scene("3"), Scene("4"), Scene("5"))
                    )
                    multiWhiteBoardHelper?.switchWhiteBoard("blackBoard111", 0) {
                        //updateGlobalState()
                    }
                }
            }

            override fun onRoomStateChanged(state: RoomState?) {
                super.onRoomStateChanged(state)
                Log.d("HiOneActivity", "onRoomStateChanged >> state = $state")
                // 监听global state变化
                val globalInfo = state?.globalState as? GlobalInfo
                Log.d("HiOneActivity", "GlobalInfo onRoomStateChanged >> globalInfo= $globalInfo")
                if (globalInfo != null && globalInfo.lastEditUid != globalStateUid) {
                    multiWhiteBoardHelper?.whiteBoardList = globalInfo.roomList
                    runOnUiThread {
                        initCloudLayout()
                    }
                }
            }

            override fun onFastError(error: FastException?) {
                super.onFastError(error)
            }
        }
        fastRoom.addListener(fastRoomListener)



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
        fastRoom.room.globalState = null
        fastRoom.destroy()
    }

    private fun getUserId(): String? {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun initCloudLayout() {
        filesLayout = findViewById(R.id.files_layout)

        // 停止共享
        val stopShare: (name: String) -> Unit = { name ->
            multiWhiteBoardHelper?.destroyWhiteBoard(name){
                updateGlobalState()
            }

        }
        // 切页
        val switchPage: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.switchWhiteBoard(name, index){
                updateGlobalState()
                fastRoom.room.scalePptToFit(AnimationMode.Immediately)
            }
        }
        // 共享空白白板
        val startShareWhiteBoard: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.addWhiteBoard(
                name,
                arrayOf(Scene("1"), Scene("2"), Scene("3"), Scene("4"), Scene("5"))
            )
            multiWhiteBoardHelper?.switchWhiteBoard(name, index){
                updateGlobalState()
            }
        }
        // 共享图片
        val startShareImage: (name: String, index: Int) -> Unit = { name, index ->
            val imageUrl =
                "https://flat-storage.oss-accelerate.aliyuncs.com/cloud-storage/2022-02/15/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda.png"
            val width = 512.0
            val height = 512.0
            multiWhiteBoardHelper?.addWhiteBoard(name, arrayOf(Scene("1", PptPage(imageUrl, width, height))))
            multiWhiteBoardHelper?.switchWhiteBoard(name, index){
                updateGlobalState()
            }
        }
        // 共享PPT
        val pptScenes = FastConvertor.convertScenes(Utils.getDocPages("8da4cdc71a9845d385a5b58ddfa10b7e"))
        val startSharePpt: (name: String, index: Int) -> Unit = { name, index ->
            multiWhiteBoardHelper?.addWhiteBoard(name, pptScenes)
            multiWhiteBoardHelper?.switchWhiteBoard(name, index){
                updateGlobalState()
                fastRoom.room.scalePptToFit(AnimationMode.Immediately)
            }

        }
        setupControllerItem(R.id.insert_whiteboard_1, "whiteboard_1", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_whiteboard_2, "whiteboard_2", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_whiteboard_3, "whiteboard_3", 5, startShare = startShareWhiteBoard, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_image, "Test.png", 0, startShare = startShareImage, stopShare = stopShare, switchPage = switchPage)
        setupControllerItem(R.id.insert_ppt, "Test.ppt", pptScenes.size, startShare = startSharePpt, stopShare = stopShare, switchPage = switchPage)

        // 设置GlobalState状态
        findViewById<TextView>(R.id.tvSetGlobalState).setOnClickListener {
            updateGlobalState()
        }

        // 获取GlobalState状态
        findViewById<TextView>(R.id.tvGetGlobalState).setOnClickListener {
            val globalState = fastRoom.room.globalState
            val globalInfo = globalState as? GlobalInfo
            Toast.makeText(this@HiOneActivity, "globalInfo = $globalInfo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGlobalState() {
        val globalInfo = GlobalInfo(
            globalStateUid,
            multiWhiteBoardHelper?.whiteBoardList ?: ArrayList()
        )
        Log.d("HiOneActivity", "GlobalInfo updateGlobalState >> globalInfo= $globalInfo")
        fastRoom.room.globalState = globalInfo
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

        val whiteboard = multiWhiteBoardHelper?.whiteBoardList?.find { it.name == name }
        var index = whiteboard?.activityPage ?: 0

        tvPage.isVisible = pageCount > 0
        ivPre.isVisible = pageCount > 0
        ivNext.isVisible = pageCount > 0
        tvName.text = name
        tvPage.text = "${index + 1}/$pageCount"
        if(whiteboard == null){
            tvShare.setText(R.string.share_start)
            tvShare.setTextColor(resources.getColor(R.color.blue))
        }else{
            tvShare.setText(R.string.share_stop)
            tvShare.setTextColor(resources.getColor(R.color.red))
        }

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
            // 选中时切换
            switchPage.invoke(name, index)
        }
    }

    class GlobalInfo(
        val lastEditUid: Int,
        val roomList: List<MultiWhiteBoardHelper.BoardListItem>? = null
    ) : GlobalState()

}