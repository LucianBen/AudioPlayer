package com.kid.audioplayer.player

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kid.audioplayer.R
import com.kid.audioplayer.throttledClick
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(model: PlayerViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text(state.unit?.title ?: "播放器", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = throttledClick(onBack)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            } },
            actions = {
                TextButton(throttledClick(model::toggleMode), enabled = state.canUseSentences && state.ready) {
                    Text(if (state.bySentence) "按句跳转" else "按秒跳转")
                }
            })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            val book = state.book
            val unit = state.unit
            if (book != null && unit != null) {
                BookPages(book, unit, state.page, state.ready, Modifier.weight(1f), model::selectPage)
                Controls(model, state)
            }
            state.error?.let { Text(it, Modifier.padding(vertical = 8.dp)) }
        }
    }
}

@Composable
private fun Controls(model: PlayerViewModel, state: PlayerState) {
    var thumb by remember { mutableStateOf<Float?>(null) }
    val progress = thumb ?: if (state.durationMs > 0) {
        (state.positionMs.toDouble() / state.durationMs).toFloat().coerceIn(0f, 1f)
    } else 0f
    Slider(
        value = progress,
        onValueChange = { thumb = it },
        onValueChangeFinished = {
            thumb?.let { model.seek((it.toDouble() * state.durationMs).roundToLong()) }
            thumb = null
        },
        enabled = state.ready,
        modifier = Modifier.semantics { contentDescription = "播放进度" },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(DateUtils.formatElapsedTime((thumb?.let { it.toDouble() * state.durationMs }?.toLong() ?: state.positionMs) / 1000))
        Text(DateUtils.formatElapsedTime(state.durationMs / 1000))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(throttledClick(model::playFromStart), modifier = Modifier.weight(1f), enabled = state.ready) {
            Icon(painterResource(R.drawable.ic_playback_restart), "从头播放", Modifier.size(playbackIconSize))
        }
        TransportButtons(
            playing = state.playRequested,
            canPlay = state.ready,
            canGoBack = state.ready && state.canGoBack,
            canGoForward = state.ready && state.canGoForward,
            onTogglePlay = model::togglePlay,
            onJump = model::jump,
        )
        PlaybackSpeedMenu(
            speed = state.playbackSpeed,
            enabled = state.ready,
            onSelect = model::setPlaybackSpeed,
            modifier = Modifier.weight(1f),
        )
    }
    state.analysisMessage?.let { Text(it, Modifier.padding(bottom = 10.dp)) }
}

@Composable
private fun PlaybackSpeedMenu(speed: Float, enabled: Boolean, onSelect: (Float) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val label = "x${speed.toString().removeSuffix(".0")}"
    Box(modifier, contentAlignment = Alignment.Center) {
        TextButton(
            onClick = throttledClick { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.semantics { contentDescription = "播放速度 $label" },
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            playbackSpeeds.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (option == 2f) "x2" else "x$option",
                            color = if (option == speed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = throttledClick {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
