package io.agora.board.fast.sample.cases.hione

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.WindowAppSyncAttrs
import com.herewhite.sdk.domain.WindowParams
import io.agora.board.fast.FastRoom
import io.agora.board.fast.Fastboard
import io.agora.board.fast.FastboardView
import io.agora.board.fast.extension.FastResource
import io.agora.board.fast.model.ControllerId
import io.agora.board.fast.model.DocPage
import io.agora.board.fast.model.FastRegion
import io.agora.board.fast.model.FastRoomOptions
import io.agora.board.fast.sample.Constants
import io.agora.board.fast.sample.R
import io.agora.board.fast.sample.misc.KeyboardHeightProvider
import io.agora.board.fast.sample.misc.Utils
import io.agora.board.fast.sample.misc.hideSoftInput
import io.agora.board.fast.sample.misc.showSoftInput

open class HiOneActivity : AppCompatActivity() {
    private lateinit var fastboardView: FastboardView
    private lateinit var fastboard: Fastboard
    private lateinit var fastRoom: FastRoom

    private lateinit var hiOneLayout: HiOneLayout
    private lateinit var textInputLayout: View
    private lateinit var textInput: EditText
    private lateinit var textInputDone: View
    private lateinit var filesLayout: View
    private var keyboardHeightProvider: KeyboardHeightProvider? = null

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

        filesLayout = findViewById(R.id.files_layout)
        findViewById<View>(R.id.insert_pptx).setOnClickListener {
            val uuid = "dc01ee126edc4ce7be8da3f7361a2f70"
            val prefix =
                "https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/demo/dynamicConvert"
            val title = "开始使用 Flat"
            fastRoom.insertPptx(uuid, prefix, title, null)

            filesLayout.isVisible = false
        }

        findViewById<View>(R.id.insert_static).setOnClickListener {
            val pages = Utils.getDocPages("8da4cdc71a9845d385a5b58ddfa10b7e")
            val title = "开始使用 Flat"
            fastRoom.insertStaticDoc(pages, title, null)

            filesLayout.isVisible = false
        }

        findViewById<View>(R.id.insert_image).setOnClickListener {
            val imageUrl =
                "https://flat-storage.oss-accelerate.aliyuncs.com/cloud-storage/2022-02/15/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda/ebe8320a-a90e-4e03-ad3a-a5dc06ae6eda.png"
            val width = 512.0
            val height = 512.0

            val pages = listOf(DocPage(imageUrl, width, height)).toTypedArray()
            fastRoom.insertStaticDoc(pages, "单图片", null)

            filesLayout.isVisible = false
        }

        findViewById<View>(R.id.close_all).setOnClickListener {
            fastRoom.room.queryAllApps(object : Promise<Map<String, WindowAppSyncAttrs>> {
                override fun then(apps: Map<String, WindowAppSyncAttrs>) {
                    apps.keys.forEach { appId ->
                        fastRoom.room.closeApp(appId, null)
                    }
                }

                override fun catchEx(t: SDKError?) {

                }
            })
        }

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
}