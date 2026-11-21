package com.kid.audioplayer.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PlaybackClient(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connection: ListenableFuture<MediaController>? = null
    private var analysis: Pair<String, Deferred<List<Long>>>? = null
    private val analysisLock = Mutex()
    internal var observedController: MediaController? = null
        private set
    private val _startedMediaId = MutableStateFlow<String?>(null)
    internal val startedMediaId = _startedMediaId.asStateFlow()
    internal var sentenceJumps: Pair<String, List<Long>>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val mediaId = player.currentMediaItem?.mediaId
            if (sentenceJumps?.first != mediaId) sentenceJumps = null
            _startedMediaId.value = when {
                player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE -> null
                player.playWhenReady -> mediaId
                else -> _startedMediaId.value?.takeIf { it == mediaId }
            }
        }
    }

    suspend fun controller(): MediaController {
        val future = connection ?: MediaController.Builder(
            context, SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().also { connection = it }
        return try {
            future.result().also { player ->
                if (observedController !== player) {
                    observedController?.removeListener(listener)
                    observedController = player
                    player.addListener(listener)
                }
            }
        } catch (error: Exception) {
            if (future.isDone && connection === future && error !is kotlinx.coroutines.CancellationException) {
                connection = null
                MediaController.releaseFuture(future)
            }
            throw error
        }
    }

    suspend fun sentences(mediaId: String, audio: File): List<Long> {
        val result = analysisLock.withLock {
            val current = analysis
            if (current != null && current.first == mediaId) return@withLock current.second
            current?.second?.cancelAndJoin()
            scope.async(Dispatchers.IO) { SilenceAnalyzer.analyze(audio) }.also {
                analysis = mediaId to it
            }
        }
        return result.await()
    }

    suspend fun release(bookId: String) = withContext(Dispatchers.Main.immediate) {
        analysisLock.withLock {
            val current = analysis
            if (current != null && current.first.startsWith("$bookId/")) {
                current.second.cancelAndJoin()
                analysis = null
            }
        }
        val future = connection ?: return@withContext
        val controller = future.result()
        val result = controller.sendCustomCommand(
            releaseBookCommand, Bundle().apply { putString(MEDIA_ID, bookId) },
        ).result()
        check(result.resultCode == SessionResult.RESULT_SUCCESS) { "无法释放书籍播放会话" }
    }

    private suspend fun <T> ListenableFuture<T>.result(): T = suspendCancellableCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (error: ExecutionException) {
                continuation.resumeWithException(error.cause ?: error)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, Runnable::run)
    }
}
