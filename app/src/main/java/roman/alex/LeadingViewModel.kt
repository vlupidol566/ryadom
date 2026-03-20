package roman.alex

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

data class LeadingUiState(
    val destinationText: String = "",
    val routeBuilt: Boolean = false,
    val searchQueryForMap: String? = null,
    val isVideoExpanded: Boolean = false,
    val remoteVideoTrack: VideoTrack? = null,
    val incomingHelpRequest: Pair<String, String>? = null,
    val isConnected: Boolean = false,
    val history: List<RouteHistory> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val currentChatMessage: String = "",
    val isChatVisible: Boolean = false,
    val followerLocation: Point? = null,
    val searchSuggestions: List<SuggestItem> = emptyList(),
    val favoritePlaces: List<FavoritePlace> = emptyList(),
    val isSearching: Boolean = false
)

class LeadingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LeadingUiState())
    val uiState: StateFlow<LeadingUiState> = _uiState.asStateFlow()

    private var webrtcClient: WebRtcClient? = null
    private val db = AppDatabase.getDatabase(application)
    private val routeHistoryDao = db.routeHistoryDao()
    private val chatMessageDao = db.chatMessageDao()
    private val favoritePlaceDao = db.favoritePlaceDao()
    
    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var suggestSession: SuggestSession? = null
    private var searchJob: Job? = null

    init {
        loadHistory()
        loadChatMessages()
        loadFavorites()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            routeHistoryDao.getRecentHistory().collectLatest { historyList ->
                _uiState.value = _uiState.value.copy(history = historyList)
            }
        }
    }

    private fun loadChatMessages() {
        viewModelScope.launch {
            chatMessageDao.getAllMessages().collectLatest { messages ->
                _uiState.value = _uiState.value.copy(chatMessages = messages)
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            favoritePlaceDao.getAllFavorites().collectLatest { favorites ->
                _uiState.value = _uiState.value.copy(favoritePlaces = favorites)
            }
        }
    }

    fun onDestinationChange(text: String) {
        _uiState.value = _uiState.value.copy(destinationText = text)

        // Отменяем предыдущий запрос
        searchJob?.cancel()

        if (text.length > 2) {
            // Debounce 400ms — запрос только после паузы ввода
            _uiState.value = _uiState.value.copy(isSearching = true)
            searchJob = viewModelScope.launch {
                delay(400)
                requestSuggest(text)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                searchSuggestions = emptyList(),
                isSearching = false
            )
        }
    }

    private fun requestSuggest(query: String) {
        suggestSession?.reset()
        val suggestOptions = SuggestOptions().setSuggestTypes(
            SuggestType.GEO.value or SuggestType.BIZ.value or SuggestType.TRANSIT.value
        )

        // Используем follower location (если есть) или дефолт Москва
        val center = _uiState.value.followerLocation ?: Point(55.751244, 37.618423) // Красная площадь
        val radius = 0.5 // ~50км в градусах
        val boundingBox = BoundingBox(
            Point(center.latitude - radius, center.longitude - radius),
            Point(center.latitude + radius, center.longitude + radius)
        )

        if (suggestSession == null) {
            searchManager.createSuggestSession().also { suggestSession = it }
        }

        suggestSession?.suggest(query, boundingBox, suggestOptions, object : SuggestSession.SuggestListener {
            override fun onResponse(response: SuggestResponse) {
                _uiState.value = _uiState.value.copy(
                    searchSuggestions = response.items,
                    isSearching = false
                )
                Log.d("Search", "Got ${response.items.size} suggestions for '$query'")
            }
            override fun onError(error: Error) {
                Log.e("Search", "Suggest error: $error")
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        })
    }

    fun onSuggestionClick(item: SuggestItem) {
        val text = item.displayText ?: item.title.text
        _uiState.value = _uiState.value.copy(
            destinationText = text,
            searchSuggestions = emptyList(),
            searchQueryForMap = text,
            isSearching = false
        )
        // Сохраняем в историю сразу при выборе
        viewModelScope.launch {
            routeHistoryDao.insert(RouteHistory(destination = text))
        }
    }

    fun onFavoriteClick(type: FavoriteType) {
        viewModelScope.launch {
            val place = favoritePlaceDao.getByType(type)
            if (place != null) {
                _uiState.value = _uiState.value.copy(
                    destinationText = place.address,
                    searchQueryForMap = place.address,
                    searchSuggestions = emptyList()
                )
            } else {
                // Если места нет, используем текст из поля как адрес для сохранения
                val currentText = _uiState.value.destinationText
                if (currentText.isNotBlank()) {
                    favoritePlaceDao.insert(FavoritePlace(
                        name = if (type == FavoriteType.HOME) "Дом" else "Работа",
                        address = currentText,
                        type = type
                    ))
                }
            }
        }
    }

    fun initWebRtc(context: android.content.Context) {
        if (webrtcClient != null) return
        webrtcClient = WebRtcClient(
            context = context.applicationContext,
            signalingUrl = SignalingConfig.SIGNALING_SERVER_URL,
            role = "leader",
            onRemoteStream = { track ->
                _uiState.value = _uiState.value.copy(remoteVideoTrack = track)
            },
            onConnectionStateChange = { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            },
            onHelpRequest = { fromPhone, note ->
                _uiState.value = _uiState.value.copy(incomingHelpRequest = fromPhone to note)
            },
            onChatMessage = { message ->
                viewModelScope.launch {
                    chatMessageDao.insert(ChatMessage(text = message, isFromMe = false))
                }
            },
            onLocationUpdate = { lat, lon ->
                _uiState.value = _uiState.value.copy(followerLocation = Point(lat, lon))
            }
        )
    }

    fun onBuildRoute() {
        val dest = _uiState.value.destinationText
        if (dest.isNotBlank()) {
            _uiState.value = _uiState.value.copy(searchQueryForMap = dest, routeBuilt = false)
            viewModelScope.launch {
                routeHistoryDao.insert(RouteHistory(destination = dest))
            }
        }
    }

    fun onRouteBuilt() {
        _uiState.value = _uiState.value.copy(routeBuilt = true)
    }

    fun toggleVideoExpanded() {
        _uiState.value = _uiState.value.copy(isVideoExpanded = !_uiState.value.isVideoExpanded)
    }

    fun dismissHelpRequest() {
        _uiState.value = _uiState.value.copy(incomingHelpRequest = null)
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

    override fun onCleared() {
        super.onCleared()
        webrtcClient?.release()
        suggestSession?.reset()
    }
}
