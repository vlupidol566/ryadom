package roman.alex

import android.app.Activity
import android.content.Context
import android.location.Location
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.search.*
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.Route as MasstransitRoute
import com.yandex.mapkit.transport.masstransit.Session as MasstransitSession
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.mapkit.transport.masstransit.TransitOptions
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    // iOS systemBackground — чистый однотонный фон без градиента
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
fun getAppContentColor(): Color {
    return MaterialTheme.colorScheme.onBackground
}

enum class RouteMode { PEDESTRIAN, MASSTRANSIT }

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    followerLocation: Point? = null,
    routeMode: RouteMode = RouteMode.PEDESTRIAN,
    onRouteUpdated: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val mapStarted = remember { mutableStateOf(false) }
    val followerPlacemarkRef = remember { mutableStateOf<PlacemarkMapObject?>(null) }

    val originPointState = remember {
        mutableStateOf(Point(55.751244, 37.618423))
    }

    LaunchedEffect(Unit) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val newPoint = Point(it.latitude, it.longitude)
                        originPointState.value = newPoint
                        mapViewRef.value?.mapWindow?.map?.move(CameraPosition(newPoint, 15.0f, 0.0f, 0.0f))
                    }
                }
        } catch (e: SecurityException) {
            Log.e("YandexMapView", "Location permission denied", e)
        }
    }

    val searchManager = remember { SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED) }
    val lastSearchQuery = remember { mutableStateOf<String?>(null) }
    val activeSearchSession = remember { mutableStateOf<com.yandex.mapkit.search.Session?>(null) }
    val activeRouteSession = remember { mutableStateOf<MasstransitSession?>(null) }
    val activeMasstransitSession = remember { mutableStateOf<MasstransitSession?>(null) }
    val pedestrianRouter = remember { TransportFactory.getInstance().createPedestrianRouter() }
    val masstransitRouter = remember { TransportFactory.getInstance().createMasstransitRouter() }

    DisposableEffect(Unit) {
        onDispose {
            // Отменяем все активные сессии перед уничтожением
            activeSearchSession.value?.cancel()
            activeRouteSession.value?.cancel()
            activeMasstransitSession.value?.cancel()
            mapViewRef.value?.onStop()
            mapViewRef.value = null
            mapStarted.value = false
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(activity ?: ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                mapViewRef.value = this

                // Используем OnAttachStateChangeListener вместо проверки width > 0 в update{}.
                // update{} вызывается ДО layout-прохода, поэтому width == 0 при первом вызове,
                // и onStart() никогда не срабатывает если состояние не меняется после этого.
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        if (!mapStarted.value) {
                            onStart()
                            mapStarted.value = true
                            // Камера и метка текущей позиции — после старта рендера
                            mapWindow.map.move(
                                CameraPosition(originPointState.value, 15.0f, 0.0f, 0.0f)
                            )
                        }
                    }
                    override fun onViewDetachedFromWindow(v: View) {
                        // останов обрабатывается DisposableEffect выше
                    }
                })
            }
        },
        update = { mapView ->
            // ⚠️ Карта ещё не стартовала — выходим, иначе краш на cameraPosition / mapObjects
            if (!mapStarted.value) return@AndroidView

            // Плейсмарк подопечного
            if (followerLocation != null) {
                val existing = followerPlacemarkRef.value
                if (existing == null) {
                    val pm = mapView.mapWindow.map.mapObjects.addPlacemark().also {
                        it.geometry = followerLocation
                    }
                    pm.setIcon(
                        ImageProvider.fromResource(context, android.R.drawable.ic_menu_mylocation)
                    )
                    followerPlacemarkRef.value = pm
                } else {
                    existing.geometry = followerLocation
                }
            }

            // Поиск по запросу
            val query = searchQuery?.takeIf { it.isNotBlank() } ?: return@AndroidView
            if (query == lastSearchQuery.value) return@AndroidView

            lastSearchQuery.value = query

            // Отменяем предыдущие сессии
            activeSearchSession.value?.cancel()
            activeRouteSession.value?.cancel()
            activeMasstransitSession.value?.cancel()

            // Снимок текущего origin + режима для безопасного захвата в замыкание
            val origin = originPointState.value
            val currentMode = routeMode

            activeSearchSession.value = searchManager.submit(
                query,
                com.yandex.mapkit.geometry.Geometry.fromPoint(
                    mapView.mapWindow.map.cameraPosition.target
                ),
                SearchOptions(),
                object : com.yandex.mapkit.search.Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        val point = response.collection.children
                            .firstOrNull()?.obj
                            ?.geometry?.firstOrNull()?.point
                            ?: run {
                                Log.w("YandexMapView", "No point found for '$query'")
                                return
                            }

                        // Всё ниже — на главном потоке (MapKit гарантирует)
                        val mapObjects = mapView.mapWindow.map.mapObjects
                        mapObjects.clear()
                        followerPlacemarkRef.value = null

                        // Маркеры старт / финиш
                        mapObjects.addPlacemark().also { it.geometry = origin }
                        mapObjects.addPlacemark().also { it.geometry = point }

                        // Плавная анимация камеры
                        mapView.mapWindow.map.move(
                            CameraPosition(point, 14.0f, 0.0f, 0.0f),
                            com.yandex.mapkit.Animation(
                                com.yandex.mapkit.Animation.Type.SMOOTH, 1f
                            ),
                            null
                        )

                        val requestPoints = listOf(
                            RequestPoint(origin, RequestPointType.WAYPOINT, null, null, null),
                            RequestPoint(point, RequestPointType.WAYPOINT, null, null, null)
                        )

                        // Рисуем полилинию + вызываем колбэк с временем и расстоянием
                        fun onRouteReady(geometry: Polyline, timeSeconds: Double, distanceMeters: Double) {
                            @Suppress("DEPRECATION")
                            mapObjects.addPolyline(geometry).apply {
                                setStrokeColor(android.graphics.Color.parseColor("#007AFF"))
                                setStrokeWidth(5f)
                                setOutlineColor(android.graphics.Color.parseColor("#0051A8"))
                                setOutlineWidth(1f)
                            }
                            val minutes = (timeSeconds / 60).toInt().coerceAtLeast(1)
                            val distText = if (distanceMeters >= 1000)
                                String.format("%.1f км", distanceMeters / 1000.0)
                            else "${distanceMeters.toInt()} м"
                            onRouteUpdated?.invoke(
                                "$minutes мин",
                                "Маршрут построен — $distText, ~$minutes мин."
                            )
                        }

                        // Fallback: прямая + приблизительное время
                        fun drawFallback() {
                            @Suppress("DEPRECATION")
                            mapObjects.addPolyline(Polyline(listOf(origin, point))).apply {
                                setStrokeColor(android.graphics.Color.parseColor("#8E8E93"))
                                setStrokeWidth(3f)
                            }
                            val distKm = haversineDistanceKm(origin, point)
                            val approxMin = (distKm / 4.0 * 60).toInt().coerceAtLeast(1)
                            onRouteUpdated?.invoke(
                                "~$approxMin мин",
                                "Прямой путь — ${String.format("%.1f км", distKm)}, ~$approxMin мин пешком."
                            )
                        }

                        val routeListener = object : MasstransitSession.RouteListener {
                            override fun onMasstransitRoutes(routes: MutableList<out MasstransitRoute>) {
                                val route = routes.firstOrNull()
                                if (route != null) {
                                    onRouteReady(
                                        route.geometry,
                                        route.metadata.weight.time.value,
                                        route.metadata.weight.walkingDistance.value
                                    )
                                } else {
                                    Log.w("YandexMapView", "No routes returned, using fallback")
                                    drawFallback()
                                }
                            }
                            override fun onMasstransitRoutesError(error: Error) {
                                Log.e("YandexMapView", "Route error: $error")
                                drawFallback()
                            }
                        }

                        if (currentMode == RouteMode.PEDESTRIAN) {
                            activeRouteSession.value = pedestrianRouter.requestRoutes(
                                requestPoints, TimeOptions(), RouteOptions(), routeListener
                            )
                        } else {
                            activeMasstransitSession.value = masstransitRouter.requestRoutes(
                                requestPoints, TransitOptions(), RouteOptions(), routeListener
                            )
                        }
                    }

                    override fun onSearchError(error: Error) {
                        Log.e("YandexMapView", "Search error for '$query': $error")
                    }
                }
            )
        },
        modifier = modifier
    )
}

// Приближённое расстояние между двумя точками (км) по формуле гаверсинусов.
private fun haversineDistanceKm(a: Point, b: Point): Double {
    val r = 6371.0 // радиус Земли, км
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)

    val sinDLat = kotlin.math.sin(dLat / 2)
    val sinDLon = kotlin.math.sin(dLon / 2)
    val h = sinDLat * sinDLat + sinDLon * sinDLon * kotlin.math.cos(lat1) * kotlin.math.cos(lat2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
    return r * c
}

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClose: () -> Unit,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(20.dp).navigationBarsPadding().imePadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Чат", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
            }
            LazyColumn(modifier = Modifier.weight(1f), reverseLayout = true, contentPadding = PaddingValues(vertical = 8.dp)) {
                items(messages.reversed()) { message -> ChatMessageItem(message) }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(12.dp))
                FilledIconButton(onClick = onSendMessage, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val containerColor = if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (message.isFromMe) 18.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 18.dp
            )
        ) {
            Text(text = message.text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = textColor, fontSize = 15.sp)
        }
    }
}

/**
 * Стандартная кнопка-иконка в стиле стекла.
 * Используется на LeadingScreen, RoleSelectionScreen.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    scale: Float = 1f,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp), tint = contentColor)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

/**
 * Большая кнопка-иконка для экрана подопечного (слабовидящие / TalkBack).
 * Touch target — 88dp, иконка — 36dp, подпись — 14sp.
 */
@Composable
fun AccessibilityActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = Color.White,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .size(88.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            border = BorderStroke(2.dp, contentColor.copy(alpha = 0.18f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(36.dp),
                    tint = contentColor
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = contentColor.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Карточка с навигационной инструкцией — FollowerScreen (Apple HIG стиль).
 * Крупный шрифт, высокий контраст, full-width.
 */
@Composable
fun InstructionCard(
    status: String,
    distance: String,
    instruction: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
    val labelColor = MaterialTheme.colorScheme.onBackground

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Расстояние — 56sp Bold, акцентный синий
            Text(
                text = distance,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = separatorColor.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(12.dp))

            // Инструкция — 20sp, нейтральный цвет
            Text(
                text = instruction,
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
}
