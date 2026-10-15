package com.example.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.DetectedFaceInfo
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object FaceDetectionHelper {

  private val detector: FaceDetector by lazy {
    val options = FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
      .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
      .enableTracking()
      .build()
    FaceDetection.getClient(options)
  }

  /**
   * Real on-device face detection using ML Kit.
   * If no face is found, returns an EMPTY list without generating fake fallback faces.
   */
  suspend fun detectFaces(bitmap: Bitmap): List<DetectedFaceInfo> = withContext(Dispatchers.Default) {
    try {
      val image = InputImage.fromBitmap(bitmap, 0)
      val faces = suspendCancellableCoroutine<List<Face>> { cont ->
        try {
          detector.process(image)
            .addOnSuccessListener { result ->
              if (cont.isActive) cont.resume(result)
            }
            .addOnFailureListener {
              if (cont.isActive) cont.resume(emptyList())
            }
        } catch (e: Throwable) {
          if (cont.isActive) cont.resume(emptyList())
        }
      }

      if (faces.isEmpty()) {
        return@withContext emptyList()
      }

      return@withContext faces.mapIndexed { index, face ->
        val bounds = face.boundingBox
        val clampedRect = Rect(
          bounds.left.coerceAtLeast(0),
          bounds.top.coerceAtLeast(0),
          bounds.right.coerceAtMost(bitmap.width),
          bounds.bottom.coerceAtMost(bitmap.height)
        )

        val thumb = if (clampedRect.width() > 10 && clampedRect.height() > 10) {
          try {
            Bitmap.createBitmap(
              bitmap,
              clampedRect.left,
              clampedRect.top,
              clampedRect.width(),
              clampedRect.height()
            )
          } catch (_: Exception) {
            null
          }
        } else null

        DetectedFaceInfo(
          id = index,
          trackingId = face.trackingId,
          bounds = clampedRect,
          eulerX = face.headEulerAngleX,
          eulerY = face.headEulerAngleY,
          eulerZ = face.headEulerAngleZ,
          leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position,
          rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position,
          nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position,
          mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position,
          faceThumbnail = thumb
        )
      }
    } catch (_: Exception) {
      return@withContext emptyList()
    }
  }

  /**
   * Renders face swap on a single image/frame using raw detected faces.
   */
  fun renderFaceSwap(
    original: Bitmap,
    faces: List<DetectedFaceInfo>,
    config: SwapConfig
  ): Bitmap {
    if (faces.isEmpty()) return original

    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    for (face in faces) {
      val replacement = config.getReplacementForFace(face.id) ?: continue
      drawFaceReplacement(
        canvas = canvas,
        replacement = replacement,
        targetCenterX = face.anchorCenterX + (config.xOffset * face.bounds.width()),
        targetCenterY = face.anchorCenterY + (config.yOffset * face.bounds.height()),
        targetWidth = face.bounds.width().toFloat() * config.scale,
        targetHeight = face.bounds.height().toFloat() * config.scale,
        angleDeg = if (config.rotationEnabled) face.rollAngle else 0f,
        config = config
      )
    }

    return result
  }

  /**
   * Renders face swap using temporal-smoothed face tracks (for video processing/playback).
   */
  fun renderFaceSwapSmoothed(
    original: Bitmap,
    smoothedTracks: List<SmoothedFaceState>,
    config: SwapConfig
  ): Bitmap {
    if (smoothedTracks.isEmpty()) return original

    val result = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    for (track in smoothedTracks) {
      val faceId = track.rawFaceInfo.id
      val replacement = config.getReplacementForFace(faceId) ?: continue

      val cx = track.centerX + (config.xOffset * track.width)
      val cy = track.centerY + (config.yOffset * track.height)
      val w = track.width * config.scale
      val h = track.height * config.scale
      val angle = if (config.rotationEnabled) track.angleDeg else 0f

      drawFaceReplacement(
        canvas = canvas,
        replacement = replacement,
        targetCenterX = cx,
        targetCenterY = cy,
        targetWidth = w,
        targetHeight = h,
        angleDeg = angle,
        config = config
      )
    }

    return result
  }

  private fun drawFaceReplacement(
    canvas: Canvas,
    replacement: ReplacementFace,
    targetCenterX: Float,
    targetCenterY: Float,
    targetWidth: Float,
    targetHeight: Float,
    angleDeg: Float,
    config: SwapConfig
  ) {
    if (targetWidth <= 5f || targetHeight <= 5f) return

    val srcBmp = replacement.bitmap
    val matrix = Matrix().apply {
      postTranslate(-srcBmp.width / 2f, -srcBmp.height / 2f)
      val scaleX = targetWidth / srcBmp.width.toFloat()
      val scaleY = targetHeight / srcBmp.height.toFloat()
      postScale(scaleX, scaleY)

      if (angleDeg != 0f) {
        postRotate(angleDeg)
      }
      postTranslate(targetCenterX, targetCenterY)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      alpha = (config.alpha.coerceIn(0f, 1f) * 255).toInt()
    }

    canvas.drawBitmap(srcBmp, matrix, paint)
  }

  /**
   * Automatically crops the face from a photo selected by the user, applying a soft oval feathered edge.
   */
  suspend fun extractFaceForReplacement(
    sourceBitmap: Bitmap,
    name: String
  ): ReplacementFace = withContext(Dispatchers.Default) {
    val detected = detectFaces(sourceBitmap)
    val primaryFace = detected.maxByOrNull { it.bounds.width() * it.bounds.height() }

    val cropped: Bitmap = if (primaryFace != null && primaryFace.bounds.width() > 30) {
      val b = primaryFace.bounds
      val padX = (b.width() * 0.18f).toInt()
      val padY = (b.height() * 0.20f).toInt()
      val left = (b.left - padX).coerceAtLeast(0)
      val top = (b.top - padY).coerceAtLeast(0)
      val right = (b.right + padX).coerceAtMost(sourceBitmap.width)
      val bottom = (b.bottom + padY).coerceAtMost(sourceBitmap.height)

      val w = right - left
      val h = bottom - top
      if (w > 20 && h > 20) {
        val raw = Bitmap.createBitmap(sourceBitmap, left, top, w, h)
        createFeatheredOvalBitmap(raw)
      } else {
        createFeatheredOvalBitmap(sourceBitmap)
      }
    } else {
      createFeatheredOvalBitmap(sourceBitmap)
    }

    ReplacementFace(
      id = "custom_${System.currentTimeMillis()}",
      name = name,
      bitmap = cropped,
      isPreset = false,
      description = "Własna twarz ze zdjęcia"
    )
  }

  fun createFeatheredOvalBitmap(src: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Base oval
    val rectF = RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())
    canvas.drawOval(rectF, paint)

    // Mask with source image
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(src, 0f, 0f, paint)

    return output
  }
}
