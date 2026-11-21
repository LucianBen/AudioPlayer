package com.kid.audioplayer

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal class ClickThrottle(private val uptimeMillis: () -> Long = SystemClock::uptimeMillis) {
    private var lastAcceptedAt: Long? = null

    fun run(action: () -> Unit) {
        val now = uptimeMillis()
        val last = lastAcceptedAt
        if (last != null && now - last < 300L) return

        lastAcceptedAt = now
        action()
    }
}

internal val LocalClickThrottle = staticCompositionLocalOf<ClickThrottle> {
    error("ClickThrottle must be provided by the activity")
}

@Composable
internal fun throttledClick(action: () -> Unit): () -> Unit {
    val throttle = LocalClickThrottle.current
    return { throttle.run(action) }
}
