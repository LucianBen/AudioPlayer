package com.kid.audioplayer.player

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kid.audioplayer.R
import com.kid.audioplayer.throttledClick

internal val playbackIconSize = 28.dp

@Composable
internal fun RowScope.TransportButtons(
    playing: Boolean,
    canPlay: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onTogglePlay: () -> Unit,
    onJump: (Int) -> Unit,
) {
    IconButton(throttledClick { onJump(-1) }, modifier = Modifier.weight(1f), enabled = canGoBack) {
        Icon(painterResource(R.drawable.ic_playback_rewind), "后退", Modifier.size(playbackIconSize))
    }
    IconButton(throttledClick(onTogglePlay), modifier = Modifier.weight(1f), enabled = canPlay) {
        if (playing) {
            Icon(painterResource(R.drawable.ic_playback_pause), "暂停", Modifier.size(32.dp))
        } else {
            Icon(Icons.Filled.PlayArrow, "播放", Modifier.size(32.dp))
        }
    }
    IconButton(throttledClick { onJump(1) }, modifier = Modifier.weight(1f), enabled = canGoForward) {
        Icon(painterResource(R.drawable.ic_playback_forward), "快进", Modifier.size(playbackIconSize))
    }
}
