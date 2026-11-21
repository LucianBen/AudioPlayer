package com.kid.audioplayer.player

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object SilenceAnalyzer {
    suspend fun analyze(file: File): List<Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            val track = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == "audio/mpeg"
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType("audio/mpeg")
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                return decode(extractor, codec)
            } finally {
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private suspend fun decode(extractor: MediaExtractor, codec: MediaCodec): List<Long> {
        val frames = mutableListOf<Double>()
        val info = MediaCodec.BufferInfo()
        var inputEnded = false
        var outputEnded = false
        var channels = 0
        var sampleRate = 0
        var encoding = AudioFormat.ENCODING_PCM_16BIT
        var sampleCount = 0
        var sampleIndex = 0L
        var power = 0.0
        var lastProgress = android.os.SystemClock.elapsedRealtime()
        while (!outputEnded) {
            currentCoroutineContext().ensureActive()
            if (!inputEnded) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val buffer = checkNotNull(codec.getInputBuffer(index))
                    val size = extractor.readSampleData(buffer, 0)
                    inputEnded = size < 0
                    if (inputEnded) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                    lastProgress = android.os.SystemClock.elapsedRealtime()
                }
            }

            when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    check(sampleIndex == 0L || (rate == sampleRate && channelCount == channels)) {
                        "音频采样格式发生变化"
                    }
                    sampleRate = rate
                    channels = channelCount
                    encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    check(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                        "音频 PCM 格式不可用"
                    }
                }
                else -> if (index >= 0) {
                    try {
                        if (info.size > 0) {
                            check(channels > 0 && sampleRate > 0)
                            val buffer = checkNotNull(codec.getOutputBuffer(index)).order(ByteOrder.nativeOrder())
                            buffer.limit(info.offset + info.size)
                            buffer.position(info.offset)
                            val sampleBytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            val frameBytes = channels * sampleBytes
                            while (buffer.remaining() >= frameBytes) {
                                repeat(channels) {
                                    val sample = if (sampleBytes == 4) buffer.float.toDouble() else buffer.short / 32768.0
                                    power += sample * sample
                                    sampleCount++
                                }
                                sampleIndex++
                                if (sampleIndex * 50 / sampleRate > frames.size) {
                                    frames.add(decibels(power, sampleCount))
                                    power = 0.0
                                    sampleCount = 0
                                }
                            }
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    } finally {
                        codec.releaseOutputBuffer(index, false)
                    }
                    lastProgress = android.os.SystemClock.elapsedRealtime()
                }
            }
            check(android.os.SystemClock.elapsedRealtime() - lastProgress < 5_000) { "音频解码超时" }
        }
        return sentenceStarts(frames)
    }
}
