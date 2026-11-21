package com.kid.audioplayer.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kid.audioplayer.BookImage
import com.kid.audioplayer.data.BookRepository
import com.kid.audioplayer.throttledClick
import java.io.File
import kotlinx.coroutines.delay

private data class MiniPlayerState(
    val progress: Float,
    val playing: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

@Composable
internal fun MiniPlayerBar(
    playback: PlaybackClient,
    repository: BookRepository,
    onOpen: (String) -> Unit,
) {
    val startedMediaId by playback.startedMediaId.collectAsStateWithLifecycle()
    val mediaId = startedMediaId ?: return
    val books by repository.books.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.id == mediaId.substringBefore('/') } ?: return
    val controller = playback.observedController ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun isCurrent() = controller.isConnected && playback.startedMediaId.value == mediaId &&
        controller.currentMediaItem?.mediaId == mediaId

    fun sentenceStarts(): List<Long>? = playback.sentenceJumps?.takeIf { it.first == mediaId }
        ?.second?.takeWhile { it < controller.duration }?.takeIf { it.size >= 2 }

    fun snapshot(): MiniPlayerState? {
        if (!isCurrent() || controller.duration <= 0L) return null
        val position = controller.currentPosition
        val duration = controller.duration
        val starts = sentenceStarts()
        return MiniPlayerState(
            progress = (position.toDouble() / duration).toFloat().coerceIn(0f, 1f),
            playing = controller.playWhenReady,
            canGoBack = if (starts == null) position > 0 else sentenceTarget(starts, position, -1) != null,
            canGoForward = if (starts == null) position < duration else sentenceTarget(starts, position, 1) != null,
        )
    }

    val state by produceState(snapshot(), controller, mediaId, lifecycle) {
        value = snapshot()
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = snapshot()
                delay(200)
            }
        }
    }
    val current = state ?: return

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookImage(
                File(book.directory, book.cover),
                "《${book.title}》封面",
                Modifier.size(width = 64.dp, height = 80.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "打开播放页面",
                        onClick = throttledClick { if (isCurrent()) onOpen(mediaId) },
                    ),
            )
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { current.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TransportButtons(
                        playing = current.playing,
                        canPlay = true,
                        canGoBack = current.canGoBack,
                        canGoForward = current.canGoForward,
                        onTogglePlay = { if (isCurrent()) controller.togglePlayback() },
                        onJump = { direction ->
                            if (isCurrent()) {
                                controller.jumpTarget(direction, sentenceStarts())?.let(controller::seekWithinAudio)
                            }
                        },
                    )
                }
            }
        }
    }
}
