package com.kid.audioplayer

import android.app.Application
import com.kid.audioplayer.data.BookRepository
import com.kid.audioplayer.player.PlaybackClient

class AudioPlayerApp : Application() {
    val books by lazy { BookRepository(this) }
    val playback by lazy { PlaybackClient(this) }
}
