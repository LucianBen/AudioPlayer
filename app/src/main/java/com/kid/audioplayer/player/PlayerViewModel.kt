package com.kid.audioplayer.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.kid.audioplayer.data.Book
import com.kid.audioplayer.data.BookRepository
import com.kid.audioplayer.data.BookUnit
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal val playbackSpeeds = listOf(0.5f, 0.8f, 1f, 1.5f, 2f)

data class PlayerState(
    val book: Book? = null,
    val unit: BookUnit? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val page: Int = 0,
    val playRequested: Boolean = false,
    val playbackSpeed: Float = 1f,
    val ready: Boolean = false,
    val bySentence: Boolean = false,
    val canUseSentences: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val analysisMessage: String? = null,
    val error: String? = null,
)

class PlayerViewModel(
    repository: BookRepository,
    private val playback: PlaybackClient,
    bookId: String,
    unitIndex: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    private val mediaId = "$bookId/$unitIndex"
    private var controller: MediaController? = null
    private var starts = emptyList<Long>()
    private var analyzed = false
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    init {
        viewModelScope.launch {
            try {
                repository.load()
                val book = requireNotNull(repository.books.value.firstOrNull { it.id == bookId }) { "书籍已不可用" }
                val unit = requireNotNull(book.units.getOrNull(unitIndex)) { "单元已不可用" }
                _state.update { it.copy(book = book, unit = unit, analysisMessage = "正在分析语句停顿…") }
                val audio = File(book.directory, unit.audio)
                val player = playback.controller()
                controller = player
                if (player.currentMediaItem?.mediaId != mediaId) {
                    player.pause()
                    player.setMediaItem(MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(Uri.fromFile(audio))
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle(unit.title)
                            .setAlbumTitle(book.title)
                            .setArtworkUri(Uri.fromFile(File(book.directory, book.cover)))
                            .build())
                        .build())
                    player.prepare()
                } else if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.addListener(listener)
                refresh()
                launch {
                    try {
                        starts = playback.sentences(mediaId, audio)
                        analyzed = true
                        _state.update { it.copy(analysisMessage = null) }
                        refresh()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        _state.update { it.copy(analysisMessage = "无法分析语句停顿，仍可按秒跳转") }
                    }
                }
                while (true) {
                    delay(50)
                    refresh()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "无法准备播放", ready = false, analysisMessage = null) }
            }
        }
    }

    fun seek(positionMs: Long) {
        val player = activePlayer() ?: return
        player.seekWithinAudio(positionMs)
        refresh()
    }

    fun selectPage(page: Int) {
        val time = _state.value.unit?.pages?.getOrNull(page)?.startMs ?: return
        seek(time)
    }

    fun playFromStart() {
        val player = activePlayer() ?: return
        player.seekTo(0L)
        player.play()
        refresh()
    }

    fun togglePlay() {
        val player = activePlayer() ?: return
        player.togglePlayback()
        refresh()
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = activePlayer() ?: return
        if (speed !in playbackSpeeds || !player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return
        player.setPlaybackSpeed(speed)
        refresh()
    }

    fun toggleMode() {
        val state = _state.value
        if (!state.canUseSentences || activePlayer() == null) return
        playback.sentenceJumps = if (state.bySentence) null else mediaId to starts
        refresh()
    }

    fun jump(direction: Int) {
        val player = activePlayer() ?: return
        val state = _state.value
        player.jumpTarget(direction, usableStarts(state.durationMs).takeIf { state.bySentence })?.let(::seek)
    }

    private fun activePlayer(): MediaController? =
        controller?.takeIf { it.isConnected && it.currentMediaItem?.mediaId == mediaId }

    private fun usableStarts(duration: Long): List<Long> =
        if (duration > 0) starts.takeWhile { it < duration } else emptyList()

    private fun refresh() {
        val player = activePlayer()
        if (player == null) {
            _state.update { it.copy(ready = false, canGoBack = false, canGoForward = false) }
            return
        }
        val unit = _state.value.unit ?: return
        val position = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        val ended = player.playbackState == Player.STATE_ENDED
        val usable = usableStarts(duration)
        val canUseSentences = usable.size >= 2
        val failure = player.playerError?.let { "音频播放失败（${it.errorCodeName}）" }
        _state.update { old ->
            val bySentence = playback.sentenceJumps?.first == mediaId && canUseSentences
            old.copy(
                positionMs = position,
                durationMs = duration,
                page = pictureIndex(unit.pages, position, ended),
                playRequested = player.playWhenReady && !ended,
                playbackSpeed = player.playbackParameters.speed,
                ready = duration > 0L && failure == null,
                bySentence = bySentence,
                canUseSentences = canUseSentences,
                canGoBack = if (bySentence) sentenceTarget(usable, position, -1) != null else position > 0,
                canGoForward = if (bySentence) sentenceTarget(usable, position, 1) != null else position < duration,
                error = failure,
                analysisMessage = if (analyzed && duration > 0 && !canUseSentences) {
                    "可用停顿不足，仍可按秒跳转"
                } else if (canUseSentences) null else old.analysisMessage,
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
    }
}
