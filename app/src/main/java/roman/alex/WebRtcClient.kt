package roman.alex

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.MediaStreamTrack

/**
 * Клиент WebRTC с сигналингом по WebSocket.
 */
class WebRtcClient(
    private val context: Context,
    private val signalingUrl: String,
    private val role: String,
    private val onRemoteStream: (VideoTrack) -> Unit,
    private val onConnectionStateChange: ((connected: Boolean) -> Unit)? = null,
    private val onLocalTrack: ((VideoTrack) -> Unit)? = null,
    private val onHelpRequest: ((fromPhone: String, note: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WebRtcClient"
        /** Глобальный контекст для всех видео-операций */
        val rootEglBase: EglBase by lazy { EglBase.create() }
    }

    private val signalingOutgoing = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val client = HttpClient { install(WebSockets) }

    private fun getOrCreatePeerConnectionFactory(): PeerConnectionFactory {
        if (peerConnectionFactory != null) return peerConnectionFactory!!
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .createPeerConnectionFactory()
        return peerConnectionFactory!!
    }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private val pcObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            val connected = state == PeerConnection.IceConnectionState.CONNECTED
            onConnectionStateChange?.let { scope.launch(Dispatchers.Main) { it(connected) } }
        }
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(candidate: IceCandidate) {
            sendSignaling(JSONObject().apply {
                put("type", "ice")
                put("candidate", JSONObject().apply {
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                })
            }.toString())
        }
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {
            stream?.videoTracks?.firstOrNull()?.let { track ->
                scope.launch(Dispatchers.Main) { onRemoteStream(track) }
            }
        }
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onTrack(transceiver: RtpTransceiver?) {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            receiver?.track()?.takeIf { it.kind() == "video" }?.let { track ->
                scope.launch(Dispatchers.Main) { onRemoteStream(track as VideoTrack) }
            }
        }
    }

    init {
        scope.launch { connectToSignaling() }
    }

    private suspend fun connectToSignaling() {
        try {
            client.webSocket(signalingUrl) {
                launch {
                    for (msg in signalingOutgoing) {
                        send(Frame.Text(msg))
                    }
                }
                sendSignaling(JSONObject().apply {
                    put("type", "role")
                    put("role", role)
                }.toString())

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleSignalingMessage(frame.readText())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signaling error: ${e.message}")
            onConnectionStateChange?.let { scope.launch(Dispatchers.Main) { it(false) } }
        }
    }

    private fun sendSignaling(msg: String) {
        scope.launch { signalingOutgoing.send(msg) }
    }

    private fun handleSignalingMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "offer" -> {
                    if (role != "leader") return
                    val sdp = json.optString("sdp")
                    if (sdp.isBlank()) return
                    createPeerConnectionIfNeeded(receiver = true)
                    val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetSuccess() {
                            peerConnection?.createAnswer(object : SdpObserver {
                                override fun onCreateSuccess(answer: SessionDescription?) {
                                    answer ?: return
                                    peerConnection?.setLocalDescription(object : SdpObserver {
                                        override fun onCreateSuccess(p0: SessionDescription?) {}
                                        override fun onCreateFailure(p0: String?) {}
                                        override fun onSetSuccess() {
                                            sendSignaling(JSONObject().apply {
                                                put("type", "answer")
                                                put("sdp", answer.description)
                                            }.toString())
                                        }
                                        override fun onSetFailure(p0: String?) {}
                                    }, answer)
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetSuccess() {}
                                override fun onSetFailure(p0: String?) {}
                            }, MediaConstraints())
                        }
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
                "answer" -> {
                    if (role != "follower") return
                    val sdp = json.optString("sdp")
                    if (sdp.isBlank()) return
                    val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetSuccess() {}
                        override fun onSetFailure(p0: String?) {}
                    }, desc)
                }
                "ice" -> {
                    val cand = json.optJSONObject("candidate") ?: return
                    val sdpMid = cand.optString("sdpMid")
                    val sdpMLineIndex = cand.optInt("sdpMLineIndex", 0)
                    val candidate = cand.optString("candidate")
                    if (candidate.isBlank()) return
                    peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
                }
                "help_request" -> {
                    if (role != "leader") return
                    val fromPhone = json.optString("fromPhone")
                    val note = json.optString("note")
                    if (fromPhone.isNotBlank()) {
                        onHelpRequest?.let { cb ->
                            scope.launch(Dispatchers.Main) {
                                cb(fromPhone, note)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSignaling: ${e.message}")
        }
    }

    private fun createPeerConnectionIfNeeded(receiver: Boolean = false) {
        if (peerConnection != null) return
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = getOrCreatePeerConnectionFactory().createPeerConnection(rtcConfig, pcObserver)
        if (receiver) {
            val init = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
            peerConnection?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, init)
        }
    }

    fun startCall(capturer: VideoCapturer) {
        if (role != "follower") return
        videoCapturer = capturer
        createPeerConnectionIfNeeded()
        val factory = getOrCreatePeerConnectionFactory()
        val videoSource = factory.createVideoSource(capturer.isScreencast)
        
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        capturer.startCapture(1280, 720, 30)
        
        val videoTrack = factory.createVideoTrack("video0", videoSource)
        onLocalTrack?.let { scope.launch(Dispatchers.Main) { it(videoTrack) } }
        peerConnection?.addTrack(videoTrack, listOf("stream"))
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(offer: SessionDescription?) {
                offer ?: return
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetSuccess() {
                        sendSignaling(JSONObject().apply {
                            put("type", "offer")
                            put("sdp", offer.description)
                        }.toString())
                    }
                    override fun onSetFailure(p0: String?) {}
                }, offer)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun release() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        peerConnection?.close()
        peerConnection = null
        
        scope.cancel()
    }
}
