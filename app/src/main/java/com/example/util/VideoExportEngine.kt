package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ml.FaceDetectionHelper
import com.example.ml.FaceTracker
import com.example.model.SwapConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

object VideoExportEngine {

  suspend fun exportVideo(
    context: Context,
    sourceUri: Uri,
    swapConfig: SwapConfig,
    isCancelled: AtomicBoolean,
    onProgress: (progress: Float, currentFrame: Int, totalFrames: Int, currentSec: Long, totalSec: Long, status: String) -> Unit
  ): Result<String> = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    val audioExtractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var encoder: MediaCodec? = null
    var tempOutputFile: File? = null

    try {
      onProgress(0.01f, 0, 0, 0, 0, "Analiza pliku wideo...")
      retriever.setDataSource(context, sourceUri)

      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val durationMs = durationStr?.toLongOrNull() ?: 0L
      if (durationMs <= 0L) {
        return@withContext Result.failure(IllegalArgumentException("Nieprawidłowy czas trwania wideo ($durationMs ms)"))
      }

      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

      val rawWidth = widthStr?.toIntOrNull() ?: 1280
      val rawHeight = heightStr?.toIntOrNull() ?: 720
      val rotation = rotationStr?.toIntOrNull() ?: 0

      // Compute display dimensions accounting for rotation
      val isVertical = (rotation == 90 || rotation == 270)
      val exportWidthUnscaled = if (isVertical) rawHeight else rawWidth
      val exportHeightUnscaled = if (isVertical) rawWidth else rawHeight

      // Scale to max 1080p for encoding speed while keeping aspect ratio and even dimensions
      val maxDimension = 1920
      val scale = (maxDimension.toFloat() / maxOf(exportWidthUnscaled, exportHeightUnscaled).toFloat()).coerceAtMost(1.0f)
      var outWidth = (exportWidthUnscaled * scale).toInt()
      var outHeight = (exportHeightUnscaled * scale).toInt()
      // Codecs require even dimensions
      if (outWidth % 2 != 0) outWidth--
      if (outHeight % 2 != 0) outHeight--

      val fps = 24
      val frameIntervalUs = 1_000_000L / fps
      val totalFrames = ((durationMs * fps) / 1000).toInt().coerceAtLeast(1)
      val totalSec = durationMs / 1000

      // Setup output file
      val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
      tempOutputFile = File(outputDir, "faceswap_${System.currentTimeMillis()}.mp4")

      muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      // Audio track extraction
      audioExtractor.setDataSource(context, sourceUri, null)
      var audioTrackIndexInSource = -1
      var audioFormat: MediaFormat? = null
      for (i in 0 until audioExtractor.trackCount) {
        val format = audioExtractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
          audioTrackIndexInSource = i
          audioFormat = format
          break
        }
      }

      // Configure H.264 Video Encoder
      val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outWidth, outHeight).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        setInteger(MediaFormat.KEY_BIT_RATE, (outWidth * outHeight * 2.5f).toInt().coerceIn(2_000_000, 8_000_000))
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
      }

      encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
      encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      // MediaMuxer tracks setup
      var videoMuxerTrack = -1
      var audioMuxerTrack = -1
      var muxerStarted = false

      val bufferInfo = MediaCodec.BufferInfo()
      val faceTracker = FaceTracker(smoothingFactor = swapConfig.smoothingFactor)

      // Reusable buffer for YUV conversion
      val yuvBuffer = ByteArray(outWidth * outHeight * 3 / 2)
      val scaledBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
      val scaledCanvas = Canvas(scaledBitmap)
      val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

      onProgress(0.05f, 0, totalFrames, 0, totalSec, "Przetwarzanie klatek wideo...")

      // Sequential frame extraction, face detection, face replacement, and encoding
      for (frameIndex in 0 until totalFrames) {
        if (isCancelled.get()) {
          return@withContext Result.failure(Exception("Eksport został anulowany przez użytkownika"))
        }

        val timeUs = frameIndex * frameIntervalUs
        val currentSec = (timeUs / 1_000_000L).coerceAtMost(totalSec)

        // Extract raw frame from source
        val rawFrame = try {
          retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
          null
        }

        val processedFrame: Bitmap = if (rawFrame != null) {
          // Detect faces on this frame
          val detected = FaceDetectionHelper.detectFaces(rawFrame)
          val smoothed = faceTracker.update(detected)

          // Render face swap onto frame
          val swapped = FaceDetectionHelper.renderFaceSwapSmoothed(rawFrame, smoothed, swapConfig)

          // Scale to target encoder resolution
          scaledCanvas.drawColor(Color.BLACK)
          val srcRect = android.graphics.Rect(0, 0, swapped.width, swapped.height)
          val dstRect = android.graphics.Rect(0, 0, outWidth, outHeight)
          scaledCanvas.drawBitmap(swapped, srcRect, dstRect, drawPaint)

          if (swapped != rawFrame) swapped.recycle()
          rawFrame.recycle()
          scaledBitmap
        } else {
          // Black fallback frame if seek failed
          scaledCanvas.drawColor(Color.BLACK)
          scaledBitmap
        }

        // Convert processed frame to NV12/YUV420SemiPlanar
        encodeBitmapToYuv420SP(processedFrame, yuvBuffer, outWidth, outHeight)

        // Feed into MediaCodec input buffer
        val inputBufferIndex = encoder.dequeueInputBuffer(20_000L)
        if (inputBufferIndex >= 0) {
          val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
          inputBuffer?.clear()
          inputBuffer?.put(yuvBuffer)
          encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, timeUs, 0)
        }

        // Drain encoded outputs from MediaCodec
        var encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
        while (encoderStatus >= 0 || encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            val newFormat = encoder.outputFormat
            videoMuxerTrack = muxer.addTrack(newFormat)
            if (audioFormat != null && audioTrackIndexInSource >= 0) {
              audioMuxerTrack = muxer.addTrack(audioFormat)
            }
            muxer.start()
            muxerStarted = true
          } else if (encoderStatus >= 0 && muxerStarted) {
            val encodedData = encoder.getOutputBuffer(encoderStatus)
            if (encodedData != null && bufferInfo.size > 0) {
              encodedData.position(bufferInfo.offset)
              encodedData.limit(bufferInfo.offset + bufferInfo.size)
              muxer.writeSampleData(videoMuxerTrack, encodedData, bufferInfo)
            }
            encoder.releaseOutputBuffer(encoderStatus, false)
          }
          encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0L)
        }

        // Report progress
        val progress = 0.05f + (0.80f * (frameIndex + 1).toFloat() / totalFrames.toFloat())
        onProgress(progress, frameIndex + 1, totalFrames, currentSec, totalSec, "Klatka ${frameIndex + 1} / $totalFrames (${currentSec}s / ${totalSec}s)")
      }

      // Signal End of Stream to Encoder
      val eosBufferIndex = encoder.dequeueInputBuffer(20_000L)
      if (eosBufferIndex >= 0) {
        encoder.queueInputBuffer(eosBufferIndex, 0, 0, durationMs * 1000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      }

      // Drain remaining EOS output buffers
      var draining = true
      while (draining && !isCancelled.get()) {
        val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 20_000L)
        if (outIndex >= 0) {
          if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            draining = false
          }
          if (muxerStarted && bufferInfo.size > 0) {
            val encodedData = encoder.getOutputBuffer(outIndex)
            if (encodedData != null) {
              encodedData.position(bufferInfo.offset)
              encodedData.limit(bufferInfo.offset + bufferInfo.size)
              muxer.writeSampleData(videoMuxerTrack, encodedData, bufferInfo)
            }
          }
          encoder.releaseOutputBuffer(outIndex, false)
        } else {
          draining = false
        }
      }

      scaledBitmap.recycle()

      // Transmux original audio track
      if (muxerStarted && audioMuxerTrack >= 0 && audioTrackIndexInSource >= 0) {
        onProgress(0.90f, totalFrames, totalFrames, totalSec, totalSec, "Miksowanie oryginalnej ścieżki audio...")
        audioExtractor.selectTrack(audioTrackIndexInSource)
        val audioBufferSize = 64 * 1024
        val audioBuf = ByteBuffer.allocate(audioBufferSize)
        val audioInfo = MediaCodec.BufferInfo()

        while (!isCancelled.get()) {
          audioInfo.offset = 0
          audioInfo.size = audioExtractor.readSampleData(audioBuf, 0)
          if (audioInfo.size < 0) break

          audioInfo.presentationTimeUs = audioExtractor.sampleTime
          audioInfo.flags = audioExtractor.sampleFlags
          muxer.writeSampleData(audioMuxerTrack, audioBuf, audioInfo)
          audioExtractor.advance()
        }
      }

      onProgress(0.96f, totalFrames, totalFrames, totalSec, totalSec, "Zapisywanie pliku MP4 do pamięci urządzenia...")

      // Finalize Muxer and Codecs
      try {
        encoder.stop()
        encoder.release()
        encoder = null
      } catch (_: Exception) {}

      try {
        if (muxerStarted) {
          muxer.stop()
        }
        muxer.release()
        muxer = null
      } catch (_: Exception) {}

      // Save to public gallery / MediaStore if possible
      val finalPath = saveToPublicGallery(context, tempOutputFile) ?: tempOutputFile.absolutePath

      onProgress(1.0f, totalFrames, totalFrames, totalSec, totalSec, "Zakończono pomyślnie!")
      Result.success(finalPath)
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      try { retriever.release() } catch (_: Exception) {}
      try { audioExtractor.release() } catch (_: Exception) {}
      try { encoder?.release() } catch (_: Exception) {}
      try { muxer?.release() } catch (_: Exception) {}
    }
  }

  /**
   * Fast bitmap ARGB to NV12 (YUV420SemiPlanar) converter.
   */
  private fun encodeBitmapToYuv420SP(bitmap: Bitmap, yuv420sp: ByteArray, width: Int, height: Int) {
    val frameSize = width * height
    val argb = IntArray(frameSize)
    bitmap.getPixels(argb, 0, width, 0, 0, width, height)

    var yIndex = 0
    var uvIndex = frameSize

    var index = 0
    for (j in 0 until height) {
      for (i in 0 until width) {
        val pixel = argb[index++]
        val R = (pixel shr 16) and 0xff
        val G = (pixel shr 8) and 0xff
        val B = pixel and 0xff

        // ITU-R BT.601 conversion formula
        val Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
        yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()

        if (j % 2 == 0 && i % 2 == 0) {
          val U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
          val V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128
          yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
          yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
        }
      }
    }
  }

  private fun saveToPublicGallery(context: Context, sourceFile: File): String? {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, "FaceSwap_${System.currentTimeMillis()}.mp4")
          put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
          put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FaceSwap")
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
          FileInputStream(sourceFile).use { input ->
            input.copyTo(out)
          }
        }

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        return uri.toString()
      } else {
        // Fallback for older Android
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "FaceSwap").apply { mkdirs() }
        val dest = File(publicDir, "FaceSwap_${System.currentTimeMillis()}.mp4")
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
      }
    } catch (_: Exception) {
      return null
    }
  }
}
