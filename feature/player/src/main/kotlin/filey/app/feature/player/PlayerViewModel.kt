package filey.app.feature.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlayerUiState(
    val fileName: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val position: Long = 0L
)

class PlayerViewModel : ViewModel() {

    private var player: ExoPlayer? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initPlayer(context: Context, path: String) {
        if (player != null) return

        val file = File(path)
        _uiState.value = _uiState.value.copy(fileName = file.name)

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(path))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _uiState.value = _uiState.value.copy(duration = duration)
                    }
                }
            })
        }
    }

    fun getPlayer(): ExoPlayer? = player

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekForward() {
        player?.let { it.seekTo(it.currentPosition + 10000) }
    }

    fun seekBack() {
        player?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) }
    }

    fun updatePosition() {
        player?.let {
            _uiState.value = _uiState.value.copy(position = it.currentPosition)
        }
    }

    override fun onCleared() {
        player?.release()
        player = null
        super.onCleared()
    }
}
