package roman.alex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(WebRtcClient.rootEglBase.eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(mirror)
                setEnableHardwareScaler(true)
            }
        },
        update = { view ->
            videoTrack.addSink(view)
        },
        modifier = modifier,
        onRelease = { view ->
            videoTrack.removeSink(view)
            view.release()
        }
    )
}
