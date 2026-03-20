package roman.alex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerScreen(
    onBack: () -> Unit,
    onCallLeading: () -> Unit,
    leaderName: String? = null,
    distanceToTurn: String,
    nextInstruction: String,
    viewModel: FollowerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    // iOS color tokens
    val bgColor = MaterialTheme.colorScheme.background
    val navBarColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                      else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val labelColor = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val cardColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            // iOS Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = navBarColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onBack)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Назад",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Подопечный",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = labelColor
                            )
                            if (leaderName != null) {
                                Text(
                                    leaderName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = secondaryLabel
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Индикатор соединения
                            IosConnectionPill(connected = uiState.webrtcConnected)

                            // Чат
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                                    )
                                    .clickable { viewModel.toggleChatVisibility() },
                                contentAlignment = Alignment.Center
                            ) {
                                BadgedBox(badge = {
                                    if (!uiState.isChatVisible && uiState.chatMessages.any { !it.isFromMe }) {
                                        Badge(containerColor = Color(0xFFFF3B30))
                                    }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = "Чат",
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = separatorColor)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // ─── Ошибка (если есть) ─────────────────────────────
                uiState.signalingError?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF3B30).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = Color(0xFFFF3B30),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ─── Главная карточка инструкции ─────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardColor,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = uiState.routeStatus,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Расстояние — главный элемент, максимально крупно
                        Text(
                            text = distanceToTurn,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 56.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Тонкий разделитель
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = separatorColor.copy(alpha = 0.5f)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Инструкция
                        Text(
                            text = nextInstruction,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp,
                                lineHeight = 28.sp
                            ),
                            color = labelColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ─── Три кнопки действий (88dp — для незрячих) ────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    A11yButton(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        label = "Повтор",
                        bg = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val text = "${uiState.routeStatus}. Через $distanceToTurn. $nextInstruction"
                            viewModel.speak(text)
                        }
                    )

                    A11yButton(
                        icon = Icons.Rounded.Call,
                        label = "Связь",
                        bg = Color(0xFF34C759),
                        onClick = onCallLeading
                    )

                    A11yButton(
                        icon = Icons.Rounded.Stop,
                        label = "Стоп",
                        bg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                        contentTint = if (isDark) Color.White else Color(0xFF3C3C43),
                        onClick = { viewModel.onStopRoute() }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ─── Видеозвонок ───────────────────────────────────────
                if (!uiState.isCalling) {
                    Button(
                        onClick = { viewModel.startVideoCall(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                            contentColor = if (isDark) Color.White else Color(0xFF3C3C43)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Начать видеозвонок",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopVideoCall() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30).copy(alpha = 0.12f),
                            contentColor = Color(0xFFFF3B30)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.webrtcConnected) "Завершить (Подключён)" else "Завершить звонок",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Превью своей камеры (угловое)
            uiState.localVideoTrack?.let { track ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                        .size(100.dp, 134.dp),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    tonalElevation = 0.dp,
                    color = Color.Black
                ) {
                    WebRtcVideoView(
                        videoTrack = track,
                        modifier = Modifier.fillMaxSize(),
                        mirror = true
                    )
                }
            }

            // Чат снизу
            AnimatedVisibility(
                visible = uiState.isChatVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ChatPanel(
                    messages = uiState.chatMessages,
                    currentMessage = uiState.currentChatMessage,
                    onMessageChange = { viewModel.onChatMessageChange(it) },
                    onSendMessage = { viewModel.sendChatMessage() },
                    onClose = { viewModel.toggleChatVisibility() },
                    contentColor = labelColor
                )
            }
        }
    }
}

// ─── Большая кнопка для слабовидящих (88dp) ───────────────────────────────────

@Composable
private fun A11yButton(
    icon: ImageVector,
    label: String,
    bg: Color,
    contentTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = bg,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(34.dp),
                    tint = contentTint
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Индикатор соединения (pill-стиль) ────────────────────────────────────────

@Composable
private fun IosConnectionPill(connected: Boolean) {
    val bg = if (connected) Color(0xFF34C759).copy(alpha = 0.12f)
             else Color(0xFF8E8E93).copy(alpha = 0.15f)
    val dotColor = if (connected) Color(0xFF34C759) else Color(0xFF8E8E93)
    val textColor = dotColor

    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (connected) "OK" else "...",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = textColor
            )
        }
    }
}
