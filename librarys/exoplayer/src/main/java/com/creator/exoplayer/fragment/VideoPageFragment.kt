package com.creator.exoplayer.fragment

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.creator.common.activity.BaseActivity
import com.creator.common.Constants
import com.creator.common.bean.FileBean
import com.creator.common.bean.VideoItemBean
import com.creator.common.bean.VideoPlayerParams
import com.creator.common.enums.Enums
import com.creator.common.fragment.BaseFragment
import com.creator.common.utils.AppPermissionUtil
import com.creator.common.utils.LogUtil
import com.creator.common.utils.ScreenUtil
import com.creator.common.utils.ToastUtil
import com.creator.exoplayer.databinding.FragmentVideoPageBinding
import com.creator.nanohttpd.server.VideoNanoHttpDServer
import com.google.android.exoplayer2.Player
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI


class VideoPageFragment : BaseFragment<FragmentVideoPageBinding>() {

    private val TAG = "VideoPageFragment"
    private lateinit var localFilesRadioButton: RadioButton
    private lateinit var httpRadioButton: RadioButton
    private lateinit var screenCastingRadioButton: RadioButton
    private var localFileUri: String? = null
    private var videoPlayerParams: VideoPlayerParams = VideoPlayerParams.getInstance()
    lateinit var player: VideoPlayerFragment
    var videoNanoHttpDServer: VideoNanoHttpDServer? = null

    private val isServer = videoPlayerParams.playerRole == Enums.PlayerRole.Server

    private var isSeekTo = false

    //监听事件
    val listener = object : Player.Listener {
        //播放状态变化监听
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            videoPlayerParams.currentVideoItemBean.playerState = playbackState
            myWebSocket?.send()
            when (playbackState) {
                // 处于缓冲状态
                Player.STATE_BUFFERING -> {
                }
                // 已准备好播放
                Player.STATE_READY -> {

                }
                // 播放已结束
                Player.STATE_ENDED -> {

                }
                // 播放器处于空闲状态
                Player.STATE_IDLE -> {

                }

            }

        }

        //进度条变化监听
        override fun onPositionDiscontinuity(reason: Int) {
            super.onPositionDiscontinuity(reason)
            LogUtil.d(TAG, "进度条变化")
            if (!isSeekTo) {
                val currentPosition = player.getCurrentPosition()
                videoPlayerParams.currentVideoItemBean.currentPosition = currentPosition
                LogUtil.d(TAG, "当前时间:::$currentPosition")
                //发送当前变更位置
                myWebSocket?.send()
            }
            isSeekTo = false
        }
    }


    /**
     * 初始化对象
     */
     override fun init() {
        player = VideoPlayerFragment.newInstance()
        // 使用 FragmentManager 启动 Fragment
        childFragmentManager.beginTransaction().replace(binding.playerView.id, player).commit()
        //初始化websocket
        if (myWebSocket == null) {
            myWebSocket = MyWebSocket()
        }
        //本地文件RadioButton
        localFilesRadioButton = binding.localFilesRadioButton
        //Http RadioButton
        httpRadioButton = binding.httpRadioButton
        //投屏RadioButton
        screenCastingRadioButton = binding.screenCastingRadioButton
        if (!isServer) {
            //客户端
            binding.ipText.text = videoPlayerParams.serverIp
            binding.btnOpenDrawer.visibility = View.GONE
        } else {
            var i = 1
            //服务端
            if (videoPlayerParams.myPublicIps.size>0){
                binding.ipText.text = "  你的可用公网ip地址为:\n"
                videoPlayerParams.myPublicIps.forEach {
                    binding.ipText.text =
                        binding.ipText.text.toString() + "  " + i++.toString() + ": " + it + "  \n"
                }
            }else{
                binding.ipText.text = "  你没有可用的公网ip地址,无法在非同一网络下进行连接\n"
            }

            if (videoPlayerParams.myPrivateIps.size>0){
                binding.ipText.text =
                    binding.ipText.text.toString() + "  你的可用内网ip地址为:\n"
                i = 1
                videoPlayerParams.myPrivateIps.forEach {
                    binding.ipText.text =
                        binding.ipText.text.toString() + "  " + i++.toString() + ": " + it + "  \n"
                }
            }else{
                binding.ipText.text =
                    binding.ipText.text.toString() + "  你没有可用的内网ip地址\n"
            }
        }
        addListener()

        screenCastingRadioButton.visibility = View.GONE
    }

    override fun addListener() {
        //播放按钮
        binding.startPlayer.setOnClickListener {
            val videoItemBean = VideoItemBean()
            videoPlayerParams.videoItemBeanList.add(videoItemBean)
            videoItemBean.ip = videoPlayerParams.myIp
            when (binding.playbackRadioGroup.checkedRadioButtonId) {
                localFilesRadioButton.id -> {
                    if (localFileUri != null) {
                        videoItemBean.setLocalUri(localFileUri)
                        videoItemBean.playbackSource = Enums.PlaybackSource.LOCAL_FILES
                        startNano()
                    } else {
                        ToastUtil.show(context, "请先选择视频文件")
                        return@setOnClickListener
                    }

                }

                httpRadioButton.id -> {
                    val httpUri = binding.httpEditText.text.toString()
                    if (httpUri.isNotEmpty()) {
                        videoItemBean.uri = httpUri
                        videoItemBean.playbackSource = Enums.PlaybackSource.HTTP
                        closeNano()
                    } else {
                        ToastUtil.show(context, "请先输入视频http地址")
                        return@setOnClickListener
                    }

                }

                screenCastingRadioButton.id -> {
                }

                else -> {
                }
            }

            startPlay() {
                addPlayerListener()
            }

        }
        //选择文件按钮
        binding.chooseFileBtn.setOnClickListener {
            //        PermissionUtils.requestFilePermissions((Activity) context);
            AppPermissionUtil.requestPermissions(
                context,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                object : AppPermissionUtil.OnPermissionListener {
                    override fun onPermissionGranted() {
                        // 启动文件选择器
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "video/*"
                        startActivityForResult(intent, Enums.FileRequestCode.VIDEO.ordinal)
                    }

                    override fun onPermissionDenied() {
                        ToastUtil.show(context, "需要文件权限才能选择文件")
                    }
                })
        }
        //侧边栏
        binding.btnOpenDrawer.setOnClickListener {
            val drawerLayout = binding.drawerLayout
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LogUtil.d(TAG, "onOrientationChanged:::$newConfig")
        setPlayerFull(newConfig)
    }

    /**
     * 判断是否需要设置播放器全屏
     */
    fun setPlayerFull(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setFullScreen(true)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setFullScreen(false)
        }
    }

    /**
     * 设置播放器是否全屏
     */
    private fun setFullScreen(fullScreen: Boolean) {
        val layoutParams = binding.playerView.layoutParams
        if (fullScreen) {
            layoutParams.height = LayoutParams.MATCH_PARENT

            // 隐藏状态栏和导航栏，并启用沉浸式模式
            entryFullscreen()
        } else {
            layoutParams.height = ScreenUtil.dip2px(context, 200f)
            entryImmersiveMode()
        }
        binding.playerView.layoutParams = layoutParams
    }

    fun removePlayerListener() {
        player.removeListener(listener)
    }
    /**
     * 进入沉浸模式
     */
    fun entryImmersiveMode() {
        (activity as BaseActivity<*>).entryImmersiveMode()
    }
    fun entryFullscreen() {
        (activity as BaseActivity<*>).entryFullscreen()
    }
    fun addPlayerListener() {
        player.addListener(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
//        removePlayerListener()

    }


    fun seekTo(l: Long) {
        isSeekTo = true
        player.seekTo(l)
    }

    /**
     * 开启Nano
     */
    fun startNano() {
        if (videoNanoHttpDServer == null) {
            videoNanoHttpDServer = VideoNanoHttpDServer(
                uri = videoPlayerParams.currentVideoUri,
                context = context
            )
            videoNanoHttpDServer?.start()
            ToastUtil.show(context, "NanoHttpD已启动")
        } else {
            videoNanoHttpDServer?.setVideoUri(videoPlayerParams.currentVideoUri)
        }
    }

    fun closeNano() {
        if (videoNanoHttpDServer != null) {
            videoNanoHttpDServer?.stop()
            videoNanoHttpDServer = null
            ToastUtil.show(context, "已关闭NanoHttpD")
        }

    }

    /**
     * 开始播放
     */
    fun startPlay(block: (() -> Unit)? = null) {
//        if (videoPlayerParams.currentVideoItemBean.playerState==Player.STATE_READY){
        player.startPlay(videoPlayerParams.currentVideoUri, block)
        if (videoPlayerParams.currentVideoItemBean.currentPosition != null) {
            player.seekTo(videoPlayerParams.currentVideoItemBean.currentPosition)
        }
//        }else{
//            player.pause()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Enums.FileRequestCode.VIDEO.ordinal && resultCode == AppCompatActivity.RESULT_OK) {
            // 处理选择的视频文件
            val videoAddress = data?.data
            localFileUri = videoAddress.toString()
            val fileBean = FileBean(context, localFileUri)
            binding.chooseFilePathText.text = fileBean.fileName
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        myWebSocket?.close()
        closeNano()
    }

    inner class MyWebSocket {

        private var websockets = ArrayList<WebSocket>()
        private lateinit var exoPlayerWebSocketServer: ExoPlayerWebSocketServer
        private lateinit var exoPlayerWebSocketClient: ExoPlayerWebSocketClient

        init {
            //根据播放器角色创建对应的websocket类
            when (VideoPlayerParams.getInstance().playerRole) {
                Enums.PlayerRole.Server -> {
                    exoPlayerWebSocketServer = ExoPlayerWebSocketServer()
                    exoPlayerWebSocketServer.start()
                }

                Enums.PlayerRole.Client -> {
                    exoPlayerWebSocketClient = ExoPlayerWebSocketClient()
                    exoPlayerWebSocketClient.connect()
                }
            }
        }

        inner class ExoPlayerWebSocketClient constructor(uri: String = videoPlayerParams.webSocketServerIp) :
            org.java_websocket.client.WebSocketClient(URI(uri)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                websockets.add(connection)
                ToastUtil.show(context, "连接成功")
            }

            override fun onMessage(message: String?) {
                LogUtil.d(TAG, message.toString())
                updateVideo(message)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                LogUtil.e(TAG, "onClose$reason")
                websockets.clear()
                if (remote) {
                    ToastUtil.show(context, "对方已断开连接")

                    activity?.finish()
                } else {
                    ToastUtil.show(context, "重新连接")
                    connect()
                }

            }

            override fun onError(ex: java.lang.Exception?) {
                LogUtil.e(TAG, "onError:::" + ex?.message, ex)
            }
        }


        inner class ExoPlayerWebSocketServer constructor(port: Int = Constants.WebSocket.PORT) :
            WebSocketServer(InetSocketAddress("::", port)) {
            override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
                websockets.add(conn)
                send()
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                if (remote) {
                    ToastUtil.show(context, "${conn.toString()}已断开连接")
                    websockets.remove(conn)
                } else {
                    ToastUtil.show(context, "${conn.toString()}已断开连接")

                    activity?.finish()
                }
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                LogUtil.d(TAG, "onMessage:::$message")
                updateVideo(message)
            }

            override fun onError(conn: WebSocket?, ex: java.lang.Exception?) {
                LogUtil.e(TAG, "onError:::" + ex?.message, ex)
            }

            override fun onStart() {
                LogUtil.d(TAG, "onStart")
                ToastUtil.show(requireContext(), "WebSocket服务启动成功")
            }

            override fun start() {
                try {
                    super.start()
                } catch (e: Exception) {
                    ToastUtil.show(context, "websocket服务端口被占用,无法启动成功")
                    activity?.finish()
                }
            }
        }

        fun close() {
            kotlin.runCatching {
                //根据播放器角色创建对应的websocket类
                when (VideoPlayerParams.getInstance().playerRole) {
                    Enums.PlayerRole.Server -> {
                        exoPlayerWebSocketServer.stop()
                    }

                    Enums.PlayerRole.Client -> {
                        exoPlayerWebSocketClient.close()
                    }
                }
                myWebSocket = null
                ToastUtil.show(context, "已关闭websocket")
            }
        }

        /**
         * 通过websocket接收到的消息更新视频数据
         */
        fun updateVideo(message: String?) {
            videoPlayerParams = VideoPlayerParams.getInstance().toClass(message)
            LogUtil.d(TAG, videoPlayerParams.toString())
            if (videoPlayerParams != null) {
                startPlay()
            }
        }

        fun send(videoPlayerParams: VideoPlayerParams) {

            websockets.forEach { websocket ->
                try {
                    websocket.send(videoPlayerParams.toString())
                } catch (e: Exception) {
                    LogUtil.e(TAG, "send失败：${videoPlayerParams.toString()}\n" + e.message, e)
                }
            }
        }

        fun send() {
            send(videoPlayerParams)
        }
    }

    companion object {
        private var myWebSocket: MyWebSocket? = null

        @JvmStatic
        fun newInstance() =
            VideoPageFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}