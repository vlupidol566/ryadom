package roman.alex

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import java.util.Locale

data class FollowerUiState(
    val routeStatus: String = "Ожидание маршрута",
    val localVideoTrack: VideoTrack? = null,
    val webrtcConnected: Boolean = false,
    val signalingError: String? = null,
    val isCalling: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val currentChatMessage: String = "",
    val isChatVisible: Boolean = false
)

class FollowerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FollowerUiState())
    val uiState: StateFlow<FollowerUiState> = _uiState.asStateFlow()

    private var webrtcClient: WebRtcClient? = null
    private var tts: TextToSpeech? = null
    private val db = AppDatabase.getDatabase(application)
    private val chatMessageDao = db.chatMessageDao()
    private var locationJob: Job? = null

    init {
        loadChatMessages()
    }

    private fun loadChatMessages() {
        viewModelScope.launch {
            chatMessageDao.getAllMessages().collectLatest { messages ->
                _uiState.value = _uiState.value.copy(chatMessages = messages)
            }
        }
    }

    fun initTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "route_instruction")
    }

    fun startVideoCall(context: Context) {
        _uiState.value = _uiState.value.copy(signalingError = null, isCalling = true)
        
        val capturer = createDefaultVideoCapturer(context) ?: run {
            val msg = "Камера недоступна"
            Log.e("FollowerViewModel", msg)
            _uiState.value = _uiState.value.copy(signalingError = msg, isCalling = false)
            return
        }

        try {
            webrtcClient = WebRtcClient(
                context = context.applicationContext,
                signalingUrl = SignalingConfig.SIGNALING_SERVER_URL,
                role = "follower",
                onRemoteStream = {},
                onConnectionStateChange = { connected ->
                    _uiState.value = _uiState.value.copy(webrtcConnected = connected)
                    if (connected) startLocationSharing(context) else stopLocationSharing()
                },
                onLocalTrack = { track ->
                    _uiState.value = _uiState.value.copy(localVideoTrack = track)
                },
                onChatMessage = { message ->
                    viewModelScope.launch {
                        chatMessageDao.insert(ChatMessage(text = message, isFromMe = false))
                    }
                }
            )
            webrtcClient?.startCall(capturer)
        } catch (e: Exception) {
            val msg = "Ошибка запуска: ${e.message}"
            _uiState.value = _uiState.value.copy(signalingError = msg, isCalling = false)
            stopVideoCall()
        }
    }

    private fun startLocationSharing(context: Context) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            while (isActive) {
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnSuccessListener { location ->
                        location?.let {
                            webrtcClient?.sendLocation(it.latitude, it.longitude)
                            Log.d("FollowerViewModel", "Location sent: ${it.latitude}, ${it.longitude}")
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("FollowerViewModel", "Location permission missing", e)
                }
                delay(5000) // Отправка каждые 5 секунд
            }
        }
    }

    private fun stopLocationSharing() {
        locationJob?.cancel()
        locationJob = null
    }

    fun stopVideoCall() {
        stopLocationSharing()
        webrtcClient?.release()
        webrtcClient = null
        _uiState.value = _uiState.value.copy(
            localVideoTrack = null,
            webrtcConnected = false,
            isCalling = false
        )
    }

    fun onChatMessageChange(text: String) {
        _uiState.value = _uiState.value.copy(currentChatMessage = text)
    }

    fun sendChatMessage() {
        val text = _uiState.value.currentChatMessage
        if (text.isNotBlank()) {
            webrtcClient?.sendChatMessage(text)
            viewModelScope.launch {
                chatMessageDao.insert(ChatMessage(text = text, isFromMe = true))
                _uiState.value = _uiState.value.copy(currentChatMessage = "")
            }
        }
    }

    fun toggleChatVisibility() {
        _uiState.value = _uiState.value.copy(isChatVisible = !_uiState.value.isChatVisible)
    }

    fun onStopRoute() {
        _uiState.value = _uiState.value.copy(routeStatus = "Остановка маршрута")
    }

    override fun onCleared() {
        super.onCleared()
        stopVideoCall()
        tts?.stop()
        tts?.shutdown()
    }
}
