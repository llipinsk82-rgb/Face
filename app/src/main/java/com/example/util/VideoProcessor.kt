package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoProcessor {

  suspend fun extractVideoInfo(context: Context, uri: Uri): VideoInfo? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    val extractor = MediaExtractor()
    try {
      retriever.setDataSource(context, uri)
      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
      val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

      val durationMs = durationStr?.toLongOrNull() ?: 0L
      val width = widthStr?.toIntOrNull() ?: 1280
      val height = heightStr?.toIntOrNull() ?: 720
      val rotation = rotationStr?.toIntOrNull() ?: 0
      val frameRate = frameRateStr?.toFloatOrNull() ?: 30f

      var hasAudio = false
      try {
        extractor.setDataSource(context, uri, null)
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
          if (mime.startsWith("audio/")) {
            hasAudio = true
            break
          }
        }
      } catch (_: Exception) {}

      VideoInfo(
        uriString = uri.toString(),
        durationMs = durationMs,
        width = width,
        height = height,
        rotation = rotation,
        frameRate = frameRate,
        hasAudio = hasAudio
      )
    } catch (_: Exception) {
      null
    } finally {
      try { retriever.release() } catch (_: Exception) {}
      try { extractor.release() } catch (_: Exception) {}
    }
  }

  suspend fun extractFrameAtTime(
    context: Context,
    uri: Uri,
    timeUs: Long
  ): Bitmap? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (_: Exception) {
      null
    } finally {
      try { retriever.release() } catch (_: Exception) {}
    }
  }

  suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "faceswap_exports").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    file.absolutePath
  }
}
