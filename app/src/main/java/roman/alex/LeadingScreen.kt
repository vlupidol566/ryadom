package roman.alex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField as ComposeBasicTextField
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
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
import com.yandex.mapkit.search.SuggestItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadingScreen(
    onBack: () -> Unit,
    onCallFollower: () -> Unit,
    onRouteUpdated: (distance: String, instruction: String) -> Unit,
    viewModel: LeadingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var routeMode by remember { mutableStateOf(RouteMode.PEDESTRIAN) }

    // iOS color tokens
    val bgColor = MaterialTheme.colorScheme.background
    val navBarColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                      else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val labelColor = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val connected = uiState.remoteVideoTrack != null

    LaunchedEffect(Unit) {
        viewModel.initWebRtc(context)
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            // iOS-style navigation bar
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
                        // Back button — iOS chevron style
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

                        // Title + subtitle центрированы
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Ведущий",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = labelColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (connected) Color(0xFF34C759) else secondaryLabel,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = if (connected) "Подключён" else "Ожидание...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (connected) Color(0xFF34C759) else secondaryLabel
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IosNavButton(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                badge = uiState.chatMessages.any { !it.isFromMe },
                                isDark = isDark,
                                onClick = { viewModel.toggleChatVisibility() }
                            )
                            IosNavButton(
                                icon = Icons.Rounded.Call,
                                isDark = isDark,
                                onClick = onCallFollower
                            )
                        }
                    }

                    // Thin separator
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ─── Видеоблок (28% высоты) ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                uiState.remoteVideoTrack?.let { track ->
                    WebRtcVideoView(videoTrack = track, modifier = Modifier.fillMaxSize())

                    // LIVE badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFFF3B30), CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                } ?: run {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.VideocamOff,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ожидание подключения",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ─── Карта (72% высоты) ────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().weight(0.72f)) {
                YandexMapView(
                    modifier = Modifier.fillMaxSize(),
                    searchQuery = uiState.searchQueryForMap,
                    followerLocation = uiState.followerLocation,
                    routeMode = routeMode,
                    onRouteUpdated = { dist, instr ->
                        onRouteUpdated(dist, instr)
                        viewModel.onRouteBuilt()
                    }
                )

                // ─── Панель управления поверх карты (снизу) ─────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Карточка поиска + режим маршрута
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.97f)
                                else Color.White.copy(alpha = 0.97f),
                        shadowElevation = 4.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {

                            // iOS-style Search Field
                            IosSearchField(
                                query = uiState.destinationText,
                                onQueryChange = { viewModel.onDestinationChange(it) },
                                onBuildClick = { viewModel.onBuildRoute() },
                                isDark = isDark,
                                isSearching = uiState.isSearching
                            )

                            // Подсказки поиска
                            if (uiState.searchSuggestions.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
                                )
                                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                                    items(uiState.searchSuggestions) { item ->
                                        SuggestRow(item, { viewModel.onSuggestionClick(it) }, labelColor)
                                    }
                                }
                            } else if (uiState.destinationText.isBlank() && uiState.history.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
                                )
                                uiState.history.take(3).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onDestinationChange(item.destination) }
                                            .padding(horizontal = 4.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.History,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = secondaryLabel
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            item.destination,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                            color = labelColor
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // iOS Segmented Control — режим маршрута
                            IosSegmentedControl(
                                options = listOf("🚶 Пешком", "🚌 Транспорт"),
                                selectedIndex = if (routeMode == RouteMode.PEDESTRIAN) 0 else 1,
                                onSelect = { idx ->
                                    routeMode = if (idx == 0) RouteMode.PEDESTRIAN else RouteMode.MASSTRANSIT
                                },
                                isDark = isDark
                            )

                            // Статус маршрута
                            if (uiState.routeBuilt) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF34C759)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "Маршрут построен",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF34C759)
                                    )
                                }
                            }
                        }
                    }
                }

                // Чат
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    AnimatedVisibility(
                        visible = uiState.isChatVisible,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
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
    }
}

// ─── iOS Search Field ──────────────────────────────────────────────────────────

@Composable
private fun IosSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onBuildClick: () -> Unit,
    isDark: Boolean,
    isSearching: Boolean = false
) {
    val fillColor = if (isDark) Color(0xFF3A3A3C) else Color(0x1A787880)
    val placeholderColor = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.3f) else Color(0x993C3C43)
    val labelColor = MaterialTheme.colorScheme.onSurface

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Search field с серым фоном — iOS-стиль
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = fillColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = placeholderColor
                    )
                }
                Spacer(Modifier.width(6.dp))
                BasicSearchTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Поиск адреса или места",
                    placeholderColor = placeholderColor,
                    textColor = labelColor
                )
            }
        }

        if (query.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onBuildClick,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(
                    "Найти",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun BasicSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    placeholderColor: Color,
    textColor: Color
) {
    ComposeBasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            color = textColor
        ),
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = placeholderColor
                    )
                }
                inner()
            }
        }
    )
}

// ─── iOS Segmented Control ─────────────────────────────────────────────────────

@Composable
fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isDark: Boolean
) {
    val trackColor = if (isDark) Color(0xFF3A3A3C) else Color(0x1A787880)
    val thumbColor = if (isDark) Color(0xFF636366) else Color.White

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = trackColor
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onSelect(index) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (selected) thumbColor else Color.Transparent,
                    shadowElevation = if (selected && !isDark) 2.dp else 0.dp
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        color = if (isDark) Color.White
                                else if (selected) Color.Black
                                else Color(0x993C3C43),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─── Переиспользуем из LeadingScreen ──────────────────────────────────────────

@Composable
fun SearchContainer(
    query: String,
    suggestions: List<SuggestItem>,
    history: List<RouteHistory>,
    onQueryChange: (String) -> Unit,
    onSuggestClick: (SuggestItem) -> Unit,
    onBuildClick: () -> Unit,
    onFavoriteClick: (FavoriteType) -> Unit,
    isDark: Boolean,
    contentColor: Color
) {
    // Используется только если вызывается из других мест. LeadingScreen использует IosSearchField напрямую.
}

@Composable
fun SuggestRow(item: SuggestItem, onClick: (SuggestItem) -> Unit, contentColor: Color) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            item.tags.contains("transit") -> Icons.Rounded.DirectionsBus
            item.tags.contains("biz") -> Icons.Rounded.Business
            else -> Icons.Rounded.Place
        }
        Icon(
            icon, null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                item.title.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Normal,
                color = contentColor
            )
            item.subtitle?.let {
                Text(
                    it.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = contentColor.copy(0.5f)
                )
            }
        }
    }
}

@Composable
fun FavoriteButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 13.sp)
    }
}

@Composable
fun RouteModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Оставлен для обратной совместимости — используем IosSegmentedControl
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun RouteStatusChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xFF34C759).copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = Color(0xFF34C759),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ─── Вспомогательные компоненты ────────────────────────────────────────────────

@Composable
private fun IosNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: Boolean = false,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(badge = {
            if (badge) Badge(containerColor = Color(0xFFFF3B30))
        }) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

