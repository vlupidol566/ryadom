package roman.alex

/**
 * Адрес сервера сигналинга WebRTC.
 * Для локального теста: Mac в той же сети, что и устройства.
 */
object SignalingConfig {
    /** Удалённый сервер сигналинга */
    const val SIGNALING_SERVER_URL = "ws://176.99.158.181:9090"
}
