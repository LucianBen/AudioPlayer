package com.kid.audioplayer.player

import androidx.media3.common.Player
import com.kid.audioplayer.data.BookPage
import kotlin.math.log10

internal fun pictureIndex(pages: List<BookPage>, positionMs: Long, ended: Boolean): Int {
    if (ended) return pages.lastIndex
    return pages.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
}

internal fun sentenceTarget(starts: List<Long>, positionMs: Long, direction: Int): Long? {
    val current = starts.indexOfLast { it <= positionMs }.coerceAtLeast(0)
    return starts.getOrNull(current + direction)
}

internal fun Player.seekWithinAudio(positionMs: Long) {
    val length = duration
    if (length <= 0L) return
    if (playbackState == Player.STATE_ENDED) pause()
    seekTo(positionMs.coerceIn(0L, length))
}

internal fun Player.togglePlayback() {
    if (playWhenReady && playbackState != Player.STATE_ENDED) {
        pause()
    } else {
        if (playbackState == Player.STATE_ENDED) seekTo(0L)
        play()
    }
}

internal fun Player.jumpTarget(direction: Int, starts: List<Long>?): Long? =
    if (starts == null) currentPosition + direction * 10_000L
    else sentenceTarget(starts, currentPosition, direction)

internal fun sentenceStarts(levels: List<Double>): List<Long> {
    if (levels.isEmpty()) return emptyList()
    val sorted = levels.sorted()
    val threshold = (sorted[sorted.lastIndex / 10] + 10).coerceIn(-62.0, -32.0)
    val starts = mutableListOf(0L)
    var silence = 0
    var voice = 0
    var silenceBeforeVoice = 0
    for ((frame, level) in levels.withIndex()) {
        if (level <= threshold) {
            silence++
            voice = 0
        } else {
            if (voice == 0) silenceBeforeVoice = silence
            voice++
            silence = 0
            if (silenceBeforeVoice >= 20 && voice == 5) {
                val start = ((frame - voice + 1) * 20L - 80L).coerceAtLeast(0L)
                if (start > starts.last()) starts.add(start)
            }
        }
    }
    return starts
}

internal fun decibels(sumSquares: Double, count: Int): Double =
    10 * log10((sumSquares / count).coerceAtLeast(1e-10))
