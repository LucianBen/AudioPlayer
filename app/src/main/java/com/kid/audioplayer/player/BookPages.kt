package com.kid.audioplayer.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.kid.audioplayer.BookImage
import com.kid.audioplayer.data.Book
import com.kid.audioplayer.data.BookUnit
import java.io.File
import kotlin.math.abs

@Composable
internal fun BookPages(
    book: Book,
    unit: BookUnit,
    selected: Int,
    ready: Boolean,
    modifier: Modifier,
    onPageSelected: (Int) -> Unit,
) {
    val page = unit.pages.getOrNull(selected) ?: return
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val selectPage by rememberUpdatedState(onPageSelected)
    key(book.directory, unit, selected) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier.fillMaxWidth().clipToBounds()
                .semantics {
                    if (ready) {
                        customActions = buildList {
                            if (selected > 0) add(CustomAccessibilityAction("上一页") {
                                selectPage(selected - 1)
                                true
                            })
                            if (selected < unit.pages.lastIndex) add(CustomAccessibilityAction("下一页") {
                                selectPage(selected + 1)
                                true
                            })
                        }
                    }
                }
                .pointerInput(windowWidth, ready) {
                    awaitEachGesture {
                        val first = awaitFirstDown()
                        var canSwipe = ready && scale == 1f
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) break
                            if (event.changes.count { it.pressed || it.previousPressed } > 1) {
                                canSwipe = false
                            }
                            val fingersMoving = event.changes.count { it.pressed && it.previousPressed }
                            if (fingersMoving >= 2 || (fingersMoving == 1 && scale > 1f)) {
                                val newScale = if (fingersMoving >= 2) {
                                    (scale * event.calculateZoom()).coerceIn(1f, 4f)
                                } else scale
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val anchor = event.calculateCentroid(useCurrent = false) - center
                                val moved = anchor + event.calculatePan() + (offset - anchor) * (newScale / scale)
                                val limit = center * (newScale - 1f)
                                offset = Offset(
                                    moved.x.coerceIn(-limit.x, limit.x),
                                    moved.y.coerceIn(-limit.y, limit.y),
                                )
                                scale = newScale
                                event.changes.forEach { it.consume() }
                            } else if (canSwipe) {
                                val finger = event.changes.firstOrNull { it.id == first.id }
                                if (finger != null && !finger.pressed) {
                                    swipePageTarget(
                                        finger.position - first.position, windowWidth, selected, unit.pages.lastIndex,
                                    )?.let(selectPage)
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            BookImage(
                File(book.directory, page.image),
                "第 ${selected + 1} 页，共 ${unit.pages.size} 页",
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            )
        }
    }
}

internal fun swipePageTarget(displacement: Offset, windowWidth: Int, page: Int, lastPage: Int): Int? {
    val horizontal = abs(displacement.x)
    if (windowWidth <= 0 || horizontal <= windowWidth * 0.35f || horizontal <= abs(displacement.y)) return null
    val target = page + if (displacement.x < 0) 1 else -1
    return target.takeIf { it in 0..lastPage }
}
