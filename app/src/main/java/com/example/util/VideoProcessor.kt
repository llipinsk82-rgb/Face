package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
    try {
      retriever.setDataSource(context, uri)
      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

      val durationMs = durationStr?.toLongOrNull() ?: 0L
      val width = widthStr?.toIntOrNull() ?: 1280
      val height = heightStr?.toIntOrNull() ?: 720
      val rotation = rotationStr?.toIntOrNull() ?: 0

      VideoInfo(
        uriString = uri.toString(),
        durationMs = durationMs,
        width = width,
        height = height,
        rotation = rotation
      )
    } catch (e: Exception) {
      null
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
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
    } catch (e: Exception) {
      null
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }
  }

  /**
   * Generates a dynamic sequence of frames simulating a recorded video clip with 2 moving people,
   * allowing instant in-app video testing without requiring external files.
   */
  fun generateDemoVideoFrame(frameIndex: Int, totalFrames: Int = 120): Bitmap {
    val w = 960
    val h = 540
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dark sleek room background
    canvas.drawColor(Color.parseColor("#121824"))

    // Moving spotlight/ambient lights
    val progress = (frameIndex.toFloat() / totalFrames.toFloat()) * (Math.PI * 2)
    val lightX = (w / 2 + Math.sin(progress) * 200).toFloat()

    paint.color = Color.parseColor("#1F293D")
    canvas.drawCircle(lightX, h * 0.4f, 300f, paint)

    // Person 1 (Moving gently left/right)
    val p1X = (w * 0.35f + Math.sin(progress) * 40).toFloat()
    val p1Y = (h * 0.55f + Math.cos(progress * 2) * 15).toFloat()
    val tilt1 = (Math.sin(progress) * 8).toFloat()
    drawDemoActor(canvas, p1X, p1Y, tilt1, skin = "#FFCC80", hair = "#4E342E", shirt = "#00ACC1")

    // Person 2 (Bobbing slightly)
    val p2X = (w * 0.68f + Math.cos(progress) * 35).toFloat()
    val p2Y = (h * 0.58f + Math.sin(progress * 2) * 12).toFloat()
    val tilt2 = (-Math.cos(progress) * 6).toFloat()
    drawDemoActor(canvas, p2X, p2Y, tilt2, skin = "#FFE0B2", hair = "#C2185B", shirt = "#7E57C2")

    // Overlay timecode badge
    paint.color = Color.parseColor("#80000000")
    canvas.drawRoundRect(20f, 20f, 220f, 65f, 12f, 12f, paint)
    paint.color = Color.parseColor("#00E5FF")
    paint.textSize = 28f
    val secs = frameIndex / 24
    val frames = frameIndex % 24
    canvas.drawText("00:0%d:%02d".format(secs, frames), 40f, 52f, paint)

    return bmp
  }

  private fun drawDemoActor(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    tiltAngle: Float,
    skin: String,
    hair: String,
    shirt: String
  ) {
    canvas.save()
    canvas.rotate(tiltAngle, cx, cy)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shirt / Body
    paint.color = Color.parseColor(shirt)
    canvas.drawRoundRect(cx - 100f, cy + 80f, cx + 100f, cy + 220f, 30f, 30f, paint)

    // Neck
    paint.color = Color.parseColor(skin)
    canvas.drawRect(cx - 24f, cy + 50f, cx + 24f, cy + 85f, paint)

    // Head Oval
    canvas.drawOval(cx - 65f, cy - 65f, cx + 65f, cy + 75f, paint)

    // Hair
    paint.color = Color.parseColor(hair)
    canvas.drawArc(cx - 68f, cy - 75f, cx + 68f, cy + 10f, 180f, 180f, true, paint)

    // Eyes
    paint.color = Color.WHITE
    canvas.drawCircle(cx - 26f, cy + 4f, 11f, paint)
    canvas.drawCircle(cx + 26f, cy + 4f, 11f, paint)
    paint.color = Color.parseColor("#212121")
    canvas.drawCircle(cx - 26f, cy + 4f, 6f, paint)
    canvas.drawCircle(cx + 26f, cy + 4f, 6f, paint)

    // Nose
    paint.color = Color.parseColor("#8D6E63")
    paint.strokeWidth = 3f
    paint.style = Paint.Style.STROKE
    canvas.drawLine(cx, cy + 8f, cx - 4f, cy + 24f, paint)
    canvas.drawLine(cx - 4f, cy + 24f, cx + 5f, cy + 24f, paint)

    // Smile
    paint.style = Paint.Style.STROKE
    paint.color = Color.parseColor("#D81B60")
    paint.strokeWidth = 4f
    canvas.drawArc(cx - 20f, cy + 30f, cx + 20f, cy + 50f, 0f, 180f, false, paint)

    canvas.restore()
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
