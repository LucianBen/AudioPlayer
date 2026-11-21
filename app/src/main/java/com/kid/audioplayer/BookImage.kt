package com.kid.audioplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookImage(file: File, label: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val height = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        var failed by remember(file, width, height) { mutableStateOf(false) }
        val bitmap by produceState<Bitmap?>(null, file, width, height) {
            value = null
            value = withContext(Dispatchers.IO) { decode(file, width, height) }
            failed = value == null
        }
        bitmap?.let {
            Image(it.asImageBitmap(), label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } ?: if (failed) {
            Text("无法读取图片")
        } else {
            Unit
        }
    }
}

private fun decode(file: File, width: Int, height: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / sample / 2 >= width || bounds.outHeight / sample / 2 >= height) {
        sample *= 2
    }
    return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
}
