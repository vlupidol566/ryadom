package roman.alex

import android.content.Context
import android.util.Log
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.VideoCapturer

private const val CAMERA_TAG = "CameraCapturerHelper"

/**
 * Создаёт VideoCapturer.
 *
 * 1. Сначала пробуем заднюю камеру (back facing), так как подопечному нужно показывать дорогу.
 * 2. Если не вышло — любую доступную.
 * 3. Если устройство не тянет Camera2, используем Camera1Enumerator.
 *
 * Подробно логируем выбор камеры в Logcat по тегу CAMERA_TAG.
 */
fun createDefaultVideoCapturer(context: Context): VideoCapturer? {
    // Выбор между Camera2 и Camera1 в зависимости от поддержки
    val useCamera2 = Camera2Enumerator.isSupported(context)
    Log.d(CAMERA_TAG, "createDefaultVideoCapturer: useCamera2=$useCamera2")

    val enumerator = if (useCamera2) {
        Camera2Enumerator(context)
    } else {
        Camera1Enumerator(false)
    }

    val deviceNames = enumerator.deviceNames
    Log.d(CAMERA_TAG, "Available cameras: ${deviceNames.joinToString()}")

    // 1. Задняя камера
    for (name in deviceNames) {
        if (enumerator.isBackFacing(name)) {
            Log.d(CAMERA_TAG, "Trying back camera: $name")
            val capturer = enumerator.createCapturer(name, null)
            if (capturer != null) {
                Log.d(CAMERA_TAG, "Using back camera: $name")
                return capturer
            } else {
                Log.w(CAMERA_TAG, "Failed to create capturer for back camera: $name")
            }
        }
    }

    // 2. Любая доступная (например, фронтальная, если задней нет)
    for (name in deviceNames) {
        Log.d(CAMERA_TAG, "Trying any camera: $name")
        val capturer = enumerator.createCapturer(name, null)
        if (capturer != null) {
            Log.d(CAMERA_TAG, "Using fallback camera: $name")
            return capturer
        } else {
            Log.w(CAMERA_TAG, "Failed to create capturer for camera: $name")
        }
    }

    Log.e(CAMERA_TAG, "No suitable camera found")
    return null
}
