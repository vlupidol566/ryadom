package roman.alex

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import java.util.Locale
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerScreen(
    onBack: () -> Unit,
    onCallLeading: () -> Unit,
    leaderName: String? = null,
    distanceToTurn: String,
    nextInstruction: String
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = getAppContentColor()
    var routeStatus by remember { mutableStateOf("Ожидание маршрута") }

    val context = LocalContext.current
    val tts = remember { TextToSpeech(context) { } }

    LaunchedEffect(tts) {
        if (tts.engines.isNotEmpty()) {
            tts.language = Locale.getDefault()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    var webrtcClient by remember { mutableStateOf<WebRtcClient?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var webrtcConnected by remember { mutableStateOf(false) }
    var signalingError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webrtcClient?.release()
            webrtcClient = null
            localVideoTrack = null
        }
    }

    fun startVideoCall() {
        Log.d("FollowerScreen", "startVideoCall tapped")
        signalingError = null
        val capturer = createDefaultVideoCapturer(context) ?: run {
            val msg = "Камера недоступна: не удалось выбрать подходящее устройство"
            Log.e("FollowerScreen", msg)
            signalingError = msg
            return
        }
        try {
            val client = WebRtcClient(
                context = context.applicationContext,
                signalingUrl = SignalingConfig.SIGNALING_SERVER_URL,
                role = "follower",
                onRemoteStream = {},
                onConnectionStateChange = { connected ->
                    Log.d("FollowerScreen", "WebRTC connection state: connected=$connected")
                    webrtcConnected = connected
                },
                onLocalTrack = { track ->
                    Log.d("FollowerScreen", "Received local video track: ${track.id()}")
                    localVideoTrack = track
                }
            )
            webrtcClient = client
            Log.d("FollowerScreen", "Starting capture 1280x720@30")
            client.startCall(capturer)
        } catch (e: Exception) {
            val msg = "Ошибка запуска видеозвонка: ${e.message}"
            Log.e("FollowerScreen", msg, e)
            signalingError = msg
            webrtcClient?.release()
            webrtcClient = null
            localVideoTrack = null
        }
    }

    fun endVideoCall() {
        webrtcClient?.release()
        webrtcClient = null
        localVideoTrack = null
        webrtcConnected = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { 
                    Column {
                        Text(
                            "Режим подопечного",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                fontSize = 16.sp
                            )
                        )
                        if (leaderName != null) {
                            Text(
                                "Ведущий: $leaderName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
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
                        onClick = onCallLeading,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                    ) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Позвонить ведущему",
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor
                )
            )
        }
    ) { padding ->
        AppBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ошибка сигналинга / камеры
                    signalingError?.let { msg ->
                        Text(
                            text = msg,
                            color = Color(0xFFE53935),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 1. Glass Status Card
                    GlassStatusCard(
                        status = routeStatus,
                        distance = distanceToTurn,
                        instruction = nextInstruction,
                        isDark = isDark,
                        contentColor = contentColor
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // 2. Glass Buttons
                    GlassActionButtons(
                        onRepeatInstruction = {
                            val text = buildString {
                                append(routeStatus)
                                append(". ")
                                append("Через ")
                                append(distanceToTurn)
                                append(". ")
                                append(nextInstruction)
                            }
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "route_instruction")
                        },
                        onCallLeader = onCallLeading,
                        onStopRoute = {
                            routeStatus = "Остановка маршрута"
                        },
                        isDark = isDark,
                        contentColor = contentColor
                    )

                    // 3. Кнопка видеозвонка
                    Spacer(modifier = Modifier.height(20.dp))
                    if (webrtcClient == null) {
                        GlassIconButton(
                            icon = Icons.Rounded.Videocam,
                            label = "Видеозвонок",
                            onClick = { startVideoCall() },
                            scale = 1f,
                            isDark = isDark,
                            contentColor = contentColor
                        )
                    } else {
                        GlassIconButton(
                            icon = Icons.Rounded.CallEnd,
                            label = if (webrtcConnected) "Завершить (подключено)" else "Завершить",
                            onClick = { endVideoCall() },
                            scale = 1f,
                            isDark = isDark,
                            contentColor = contentColor
                        )
                    }
                }

                // Локальное превью камеры в углу во время звонка
                localVideoTrack?.let { track ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(120.dp, 160.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        WebRtcVideoView(
                            videoTrack = track,
                            modifier = Modifier.fillMaxSize(),
                            mirror = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassStatusCard(
    status: String,
    distance: String,
    instruction: String,
    isDark: Boolean,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .width(300.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = distance,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2196F3),
                    fontSize = 32.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlassActionButtons(
    onRepeatInstruction: () -> Unit,
    onCallLeader: () -> Unit,
    onStopRoute: () -> Unit,
    isDark: Boolean,
    contentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_btn_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.Rounded.VolumeUp,
            label = "Повтор",
            onClick = onRepeatInstruction,
            scale = pulse,
            isDark = isDark,
            contentColor = contentColor
        )

        GlassIconButton(
            icon = Icons.Rounded.Call,
            label = "Связь",
            onClick = onCallLeader,
            scale = pulse,
            isDark = isDark,
            contentColor = contentColor
        )

        GlassIconButton(
            icon = Icons.Rounded.Stop,
            label = "Стоп",
            onClick = onStopRoute,
            scale = pulse,
            isDark = isDark,
            contentColor = contentColor
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    scale: Float,
    isDark: Boolean,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = Color.White.copy(alpha = if (isDark) 0.15f else 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = contentColor
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        )
    }
}
