package roman.alex

import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.runtime.Error

/**
 * Базовый фон экрана: теперь опирается на MaterialTheme,
 * а не на фиолетовый градиент, чтобы палитра темы была видна.
 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

/**
 * Возвращает основной цвет текста в зависимости от темы.
 */
@Composable
fun getAppContentColor(): Color {
    return if (isSystemInDarkTheme()) Color.White else Color(0xFF1A237E)
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    onRouteUpdated: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val mapStarted = remember { mutableStateOf(false) }

    // Стартовая точка: пытаемся взять текущую локацию, иначе центр Москвы
    val originPointState = remember {
        mutableStateOf(
            getLastKnownLocationPoint(context) ?: Point(55.751244, 37.618423)
        )
    }
    val destinationPointState = remember { mutableStateOf<Point?>(null) }
    val searchManager = remember { SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED) }
    val drivingRouter = remember { DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED) }
    val lastSearchQuery = remember { mutableStateOf<String?>(null) }
    val activeSearchSession = remember { mutableStateOf<Session?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef.value?.onStop()
            mapViewRef.value = null
            mapStarted.value = false
        }
    }

    AndroidView(
        factory = { ctx ->
            val mapView = MapView(activity ?: ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            mapViewRef.value = mapView

            val startPoint = originPointState.value
            mapView.mapWindow.map.move(
                CameraPosition(
                    startPoint,
                    11.0f, 0.0f, 0.0f
                )
            )

            // Базовый пин начальной точки
            val mapObjects = mapView.map.mapObjects
            mapObjects.clear()
            mapObjects.addPlacemark(startPoint)

            mapView
        },
        update = { mapView ->
            if (!mapStarted.value && mapView.width > 0 && mapView.height > 0) {
                mapView.onStart()
                mapStarted.value = true
            }

            val query = searchQuery?.takeIf { it.isNotBlank() }
            if (query != null && query != lastSearchQuery.value) {
                lastSearchQuery.value = query

                activeSearchSession.value?.cancel()
                activeSearchSession.value = searchManager.submit(
                    query,
                    com.yandex.mapkit.geometry.Geometry.fromPoint(mapView.mapWindow.map.cameraPosition.target),
                    SearchOptions(),
                    object : Session.SearchListener {
                        override fun onSearchResponse(response: Response) {
                            val first = response.collection.children.firstOrNull()?.obj
                            val point = first?.geometry?.firstOrNull()?.point
                            if (point != null) {
                                destinationPointState.value = point

                                val mapObjects = mapView.map.mapObjects
                                mapObjects.clear()

                                // Пин старта (текущее origin)
                                val origin = originPointState.value
                                mapObjects.addPlacemark(origin)

                                // Пин назначения
                                mapObjects.addPlacemark(point)

                                // Пытаемся построить реальный маршрут по дорогам
                                val drivingOptions = DrivingOptions()
                                val vehicleOptions = VehicleOptions()
                                val points = listOf(
                                    RequestPoint(
                                        origin,
                                        RequestPointType.WAYPOINT,
                                        null,
                                        null,
                                        null
                                    ),
                                    RequestPoint(
                                        point,
                                        RequestPointType.WAYPOINT,
                                        null,
                                        null,
                                        null
                                    )
                                )

                                drivingRouter.requestRoutes(
                                    points,
                                    drivingOptions,
                                    vehicleOptions,
                                    object : DrivingSession.DrivingRouteListener {
                                        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
                                            val route = routes.firstOrNull()
                                            if (route != null) {
                                                mapObjects.addPolyline(route.geometry)
                                                val distanceMeters = route.metadata.weight.distance.value
                                                val distanceText = when {
                                                    distanceMeters >= 1000 -> String.format("%.1f км", distanceMeters / 1000.0)
                                                    else -> "${distanceMeters.toInt()} м"
                                                }
                                                onRouteUpdated?.invoke(
                                                    distanceText,
                                                    "Следуйте по маршруту до точки назначения"
                                                )
                                            } else {
                                                // Фоллбек: прямая линия
                                                mapObjects.addPolyline(
                                                    Polyline(
                                                        listOf(origin, point)
                                                    )
                                                )
                                                onRouteUpdated?.invoke(
                                                    "—",
                                                    "Маршрут не найден, ориентируйтесь по карте"
                                                )
                                            }
                                        }

                                        override fun onDrivingRoutesError(error: Error) {
                                            Log.e("YandexMapView", "Driving route error: $error")
                                            // Фоллбек: прямая линия
                                            mapObjects.addPolyline(
                                                Polyline(
                                                    listOf(origin, point)
                                                )
                                            )
                                            onRouteUpdated?.invoke(
                                                "—",
                                                "Ошибка построения маршрута, попробуйте другой адрес"
                                            )
                                        }
                                    }
                                )

                                mapView.mapWindow.map.move(
                                    CameraPosition(
                                        point,
                                        15.0f,
                                        0.0f,
                                        0.0f
                                    ),
                                    com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1f),
                                    null
                                )
                            }
                        }

                        override fun onSearchError(error: Error) {
                            Log.e("YandexMapView", "Search error: $error")
                        }
                    }
                )
            }
        }
    )
}

fun getLastKnownLocationPoint(context: Context): Point? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        bestLocation?.let { Point(it.latitude, it.longitude) }
    } catch (e: SecurityException) {
        null
    }
}
