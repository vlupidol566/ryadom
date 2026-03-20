package roman.alex

/**
 * Адрес сервера сигналинга WebRTC.
 * Для локального теста: Mac в той же сети, что и устройства.
 */
object SignalingConfig {
    /** Удалённый сервер сигналинга */
    const val SIGNALING_SERVER_URL = "ws://176.99.158.181:9090"
    
    /** API ключ Яндекс Карт (рекомендуется выносить в BuildConfig) */
    const val YANDEX_MAPS_API_KEY = "a475bc49-93c8-4788-a542-c112213af8fb"
}
