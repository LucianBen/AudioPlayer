package com.kid.audioplayer.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kid.audioplayer.MainActivity

internal const val OPEN_MEDIA = "com.kid.audioplayer.OPEN_MEDIA"
internal const val MEDIA_ID = "media_id"
internal val releaseBookCommand = SessionCommand("com.kid.audioplayer.RELEASE_BOOK", Bundle.EMPTY)

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private lateinit var session: MediaSession
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        player = createPlayer()
        session = MediaSession.Builder(this, player).setCallback(object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val result = super.onConnect(session, controller)
                if (controller.packageName != packageName) return result
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                    .setAvailableSessionCommands(result.availableSessionCommands.buildUpon().add(releaseBookCommand).build())
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                if (customCommand != releaseBookCommand || controller.packageName != packageName) {
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
                val id = args.getString(MEDIA_ID)
                if (id != null && player.currentMediaItem?.mediaId?.startsWith("$id/") == true) {
                    val previous = player
                    val replacement = createPlayer()
                    session.setPlayer(replacement)
                    player = replacement
                    previous.release()
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }).build()
    }

    private fun createPlayer(): ExoPlayer {
        val extractors = DefaultExtractorsFactory().setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val created = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractors))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        created.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) return
                val intent = Intent(this@PlaybackService, MainActivity::class.java)
                    .setAction(OPEN_MEDIA)
                    .putExtra(MEDIA_ID, mediaItem.mediaId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                session.setSessionActivity(PendingIntent.getActivity(
                    this@PlaybackService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ))
            }
        })
        return created
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }
}
