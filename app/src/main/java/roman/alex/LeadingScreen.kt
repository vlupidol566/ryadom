package roman.alex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadingScreen(
    onBack: () -> Unit,
    onCallFollower: () -> Unit,
    onRouteUpdated: (distance: String, instruction: String) -> Unit
) {
    var destinationText by remember { mutableStateOf("") }
    var routeBuilt by remember { mutableStateOf(false) }
    var searchQueryForMap by remember { mutableStateOf<String?>(null) }
    var isVideoExpanded by remember { mutableStateOf(false) }
    var incomingHelpRequest by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isDark = isSystemInDarkTheme()
    val contentColor = MaterialTheme.colorScheme.onBackground

    val context = LocalContext.current
    var webrtcClient by remember { mutableStateOf<WebRtcClient?>(null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }

    LaunchedEffect(Unit) {
        webrtcClient = WebRtcClient(
            context = context.applicationContext,
            signalingUrl = SignalingConfig.SIGNALING_SERVER_URL,
            role = "leader",
            onRemoteStream = { track ->
                Log.d("LeadingScreen", "Received remote VideoTrack ${track.id()}")
                remoteVideoTrack = track
            },
            onConnectionStateChange = { connected ->
                Log.d("LeadingScreen", "WebRTC connection state (leader): connected=$connected")
            },
            onHelpRequest = { fromPhone, note ->
                incomingHelpRequest = fromPhone to note
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webrtcClient?.release()
            webrtcClient = null
            remoteVideoTrack = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { 
                    Text(
                        "Ведущий",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 18.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = contentColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCallFollower,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Позвонить",
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = contentColor
                )
            )
        }
    ) { padding ->
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Карта всегда на фоне, поверх — видео ведомого и панель поиска/маршрута
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // 1. Всегда отображаем карту (как в Яндекс.Картах)
                    YandexMapView(
                        modifier = Modifier.fillMaxSize(),
                        searchQuery = searchQueryForMap,
                        onRouteUpdated = { distance, instruction ->
                            onRouteUpdated(distance, instruction)
                            routeBuilt = true
                        }
                    )

                    // 2. Видео подопечного поверх карты (картинка-в-картинке / фулл-скрин по тапу)
                    remoteVideoTrack?.let { track ->
                        if (isVideoExpanded) {
                            // Полноэкранный режим
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.98f))
                                    .clickable { isVideoExpanded = false },
                                color = Color.Black,
                            ) {
                                WebRtcVideoView(
                                    videoTrack = track,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // Маленькое окошко в углу
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(width = 140.dp, height = 200.dp)
                                    .clickable { isVideoExpanded = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black,
                                border = BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.5f)
                                )
                            ) {
                                WebRtcVideoView(
                                    videoTrack = track,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } ?: run {
                        Text(
                            text = "Видео подопечного появится,\nкогда он начнёт видеозвонок",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = contentColor,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp)
                        )
                    }

                    // 3. Панель поиска и маршрута поверх карты (всё на одном экране)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 72.dp)
                    ) {
                            GlassRoutePanel(
                                destinationText = destinationText,
                                routeBuilt = routeBuilt,
                                onDestinationChange = { destinationText = it },
                                onBuildRoute = {
                                    searchQueryForMap = destinationText
                                },
                                isDark = isDark,
                                contentColor = contentColor
                            )
                    }
                }

                incomingHelpRequest?.let { (phone, note) ->
                    AlertDialog(
                        onDismissRequest = { incomingHelpRequest = null },
                        title = {
                            Text("Запрос помощи")
                        },
                        text = {
                            Text(
                                "Подопечный $phone просит помощи.\n$note"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                incomingHelpRequest = null
                                // Переходим в сценарий связи с подопечным
                                onCallFollower()
                            }) {
                                Text("Принять")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { incomingHelpRequest = null }) {
                                Text("Отклонить")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassMapContainer(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    routeBuilt: Boolean,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
            )
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            YandexMapView(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GlassRoutePanel(
    destinationText: String,
    routeBuilt: Boolean,
    onDestinationChange: (String) -> Unit,
    onBuildRoute: () -> Unit,
    isDark: Boolean,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = if (isDark) 0.18f else 0.9f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Основная строка поиска, визуально ближе к Яндекс.Картам
            OutlinedTextField(
                value = destinationText,
                onValueChange = onDestinationChange,
                placeholder = { Text("Куда поедем?", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = contentColor
                    )
                },
                trailingIcon = {
                    Row {
                        if (destinationText.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Очистить",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onDestinationChange("") }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            )

            // Простейшие подсказки как в поиске Яндекс.Карт
            val suggestions = listOf(
                "Дом",
                "Работа",
                "Магазин у дома",
                "Аптека рядом",
                "Красная площадь",
                "ЖД вокзал",
                "Аэропорт"
            )
            val filtered = suggestions.filter {
                destinationText.isNotBlank() &&
                    it.contains(destinationText, ignoreCase = true)
            }

            if (filtered.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = if (isDark) 0.25f else 0.95f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filtered.take(5).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDestinationChange(item) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item,
                                    color = contentColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBuildRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (destinationText.isBlank()) "Построить маршрут"
                        else "Построить маршрут до \"$destinationText\"",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (destinationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (routeBuilt) {
                        "Маршрут построен до: \"$destinationText\""
                    } else {
                        "Введите адрес, выберите подсказку или нажмите «Построить маршрут»"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
fun QuickRouteButton(
    label: String,
    icon: ImageVector,
    isDark: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (isDark) 0.1f else 0.6f),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = contentColor, fontWeight = FontWeight.Medium)
        }
    }
}
