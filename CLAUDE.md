# Правила для работы с проектом

## Стек
- Android + Kotlin + Jetpack Compose + Material3
- Yandex MapKit 4.10.1-full
- WebRTC (org.webrtc)
- Firebase FCM
- Room (локальная БД)
- Node.js signaling server (WS :9090, REST :3001)

---

## ОБЯЗАТЕЛЬНО перед написанием кода под любой SDK

### Правило №1 — Верификация классов до кода

Перед тем как использовать классы из стороннего SDK (Yandex MapKit, WebRTC и т.д.) —
**сначала проверить реальные имена классов**, а потом писать код.

Никогда не угадывать имена пакетов и классов по аналогии или документации.

**Способ 1 — grep по gradle кэшу (самый быстрый):**
```bash
find ~/.gradle/caches -name "*.jar" | xargs -I{} sh -c 'jar tf "{}" 2>/dev/null | grep -i "KEYWORD"'
```

**Способ 2 — DEX анализ скомпилированного APK:**
```bash
# Распаковать APK
unzip -o app/build/outputs/apk/debug/app-debug.apk -d /tmp/apk_extract

# Найти реальные классы
for f in /tmp/apk_extract/classes*.dex; do
  grep -oa "com/yandex/mapkit/[a-zA-Z0-9/_]*" "$f" 2>/dev/null | sort -u | grep -i "KEYWORD"
done
```

**Способ 3 — grep по существующим импортам в проекте:**
```bash
grep -r "import com.yandex" app/src/main/java/ | sort -u
```

---

## Известная структура Yandex MapKit 4.10.1

### Инициализация (MainActivity.onCreate)
```kotlin
MapKitFactory.setApiKey(...)
MapKitFactory.initialize(this)
// TransportFactory — отдельный initialize НЕ нужен, работает через MapKitFactory
```

### Маршрутизация (пешеход + транспорт)
```kotlin
// Пакеты:
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.Route as MasstransitRoute
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.transport.masstransit.Session as MasstransitSession
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.mapkit.transport.masstransit.TransitOptions

// Создание роутеров:
val pedestrianRouter = TransportFactory.getInstance().createPedestrianRouter()
val masstransitRouter = TransportFactory.getInstance().createMasstransitRouter()

// RequestPoint — 5 параметров (не 4):
RequestPoint(point, RequestPointType.WAYPOINT, null, null, null)

// Сигнатуры requestRoutes — 4 параметра (points, options, RouteOptions, listener):
pedestrianRouter.requestRoutes(points, TimeOptions(), RouteOptions(), listener)
masstransitRouter.requestRoutes(points, TransitOptions(), RouteOptions(), listener)

// TransitOptions конструкторы (НЕ принимает List):
TransitOptions()                        // без фильтров
TransitOptions(filterFlags: Int, TimeOptions())  // с фильтром типов транспорта

// Листенер — один для обоих роутеров:
object : MasstransitSession.RouteListener {
    override fun onMasstransitRoutes(routes: MutableList<out MasstransitRoute>) { ... }
    override fun onMasstransitRoutesError(error: Error) { ... }
}

// Типы данных в Weight:
// weight.time.value — Double (секунды)
// weight.walkingDistance.value — Double (метры)
// LocalizedValue находится в com.yandex.mapkit.LocalizedValue
```

### Стилизация полилиний (PolylineMapObject)
```kotlin
// setStrokeWidth / setOutlineColor / setOutlineWidth — deprecated в Java.
// setStrokeColor — НЕ deprecated.
// Подавлять предупреждения через @Suppress("DEPRECATION") на блок apply:
@Suppress("DEPRECATION")
mapObjects.addPolyline(geometry).apply {
    setStrokeColor(android.graphics.Color.parseColor("#2196F3"))
    setStrokeWidth(5f)
    setOutlineColor(android.graphics.Color.parseColor("#0D47A1"))
    setOutlineWidth(1f)
}
// Альтернатива для одного цвета без outline — setStrokeColors(listOf(color))
```

### Карта (MapView)
```kotlin
// Правильно:
mapView.mapWindow.map          // НЕ mapView.map (deprecated)
mapObjects.addPlacemark().also { it.geometry = point }  // НЕ addPlacemark(point) (deprecated)

// Запуск карты — через OnAttachStateChangeListener, не через update{}:
addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) {
        onStart()
        mapWindow.map.move(CameraPosition(...))
    }
    override fun onViewDetachedFromWindow(v: View) {}
})
```

### Иконки Material (Compose)
```kotlin
// Правильно (не deprecated):
Icons.AutoMirrored.Rounded.ArrowBack
Icons.AutoMirrored.Rounded.VolumeUp
Icons.AutoMirrored.Rounded.Send
```

---

## Архитектура приложения

Приложение "Рядом" — сопровождение слабовидящих/незрячих.

**Роли:**
- **Ведущий (Leading)** — зрячий сопровождающий. Видит карту, строит маршрут, смотрит видео с камеры подопечного.
- **Подопечный (Follower)** — незрячий/слабовидящий. Получает голосовые инструкции, большие touch-таргеты (88dp), высококонтрастный UI, совместимость с TalkBack.

**Идеология UI:**
- Follower screen: accessibility-first, минимум анимаций, иконки 88dp, текст 52sp
- Leading screen: информативность, карта + видео + управление маршрутом
- Единый design token: тёмная тема `Color(0xFF1C2333)`, светлая — белые карточки с тенью 3dp

**Навигация (NavHost):**
`role_selection` → `leading` | `follower` | `contacts` | `profile` | `user_search`
