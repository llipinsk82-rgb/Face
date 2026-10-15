package com.example.model

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import kotlin.math.atan2
import kotlin.math.hypot

data class DetectedFaceInfo(
  val id: Int,
  val trackingId: Int? = null,
  val bounds: Rect,
  val eulerX: Float = 0f,
  val eulerY: Float = 0f,
  val eulerZ: Float = 0f,
  val leftEye: PointF? = null,
  val rightEye: PointF? = null,
  val nose: PointF? = null,
  val mouth: PointF? = null,
  val faceThumbnail: Bitmap? = null
) {
  /**
   * Calculates the roll angle in degrees.
   * If both eyes are detected, uses the eye line for high geometric precision.
   * Otherwise falls back to ML Kit's Euler Z angle.
   */
  val rollAngle: Float
    get() {
      return if (leftEye != null && rightEye != null) {
        val dx = (rightEye.x - leftEye.x).toDouble()
        val dy = (rightEye.y - leftEye.y).toDouble()
        Math.toDegrees(atan2(dy, dx)).toFloat()
      } else {
        -eulerZ
      }
    }

  /**
   * Distance between eyes if both landmarks exist, else estimated from bounds width.
   */
  val eyeDistance: Float
    get() {
      return if (leftEye != null && rightEye != null) {
        hypot(rightEye.x - leftEye.x, rightEye.y - leftEye.y)
      } else {
        bounds.width() * 0.38f
      }
    }

  /**
   * Center anchor point for face replacement.
   */
  val anchorCenterX: Float
    get() {
      return if (leftEye != null && rightEye != null) {
        (leftEye.x + rightEye.x) / 2f
      } else {
        bounds.exactCenterX()
      }
    }

  val anchorCenterY: Float
    get() {
      return if (leftEye != null && rightEye != null) {
        val eyeCenterY = (leftEye.y + rightEye.y) / 2f
        // Offset downwards towards nose bridge to center the face
        eyeCenterY + (eyeDistance * 0.45f)
      } else {
        bounds.exactCenterY()
      }
    }
}

data class ReplacementFace(
  val id: String,
  val name: String,
  val bitmap: Bitmap,
  val isPreset: Boolean = false,
  val description: String = ""
)

data class SwapConfig(
  val isSingleForAll: Boolean = true,
  val globalReplacement: ReplacementFace? = null,
  val individualMappings: Map<Int, ReplacementFace> = emptyMap(),
  val scale: Float = 1.15f,
  val xOffset: Float = 0.0f,
  val yOffset: Float = 0.0f,
  val rotationEnabled: Boolean = true,
  val featherBorder: Float = 18f,
  val alpha: Float = 1.0f,
  val isOvalMask: Boolean = true,
  val smoothingFactor: Float = 0.45f
) {
  fun getReplacementForFace(faceId: Int): ReplacementFace? {
    return if (isSingleForAll) {
      globalReplacement
    } else {
      individualMappings[faceId] ?: globalReplacement
    }
  }
}

enum class MediaMode {
  PHOTO,
  VIDEO
}

data class VideoInfo(
  val uriString: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val rotation: Int = 0,
  val frameRate: Float = 30f,
  val hasAudio: Boolean = false
) {
  val isVertical: Boolean
    get() {
      val rotatedWidth = if (rotation == 90 || rotation == 270) height else width
      val rotatedHeight = if (rotation == 90 || rotation == 270) width else height
      return rotatedHeight > rotatedWidth
    }

  val aspectRatio: Float
    get() {
      val rotatedWidth = if (rotation == 90 || rotation == 270) height else width
      val rotatedHeight = if (rotation == 90 || rotation == 270) width else height
      return if (rotatedHeight > 0) rotatedWidth.toFloat() / rotatedHeight.toFloat() else 16f / 9f
    }
}

data class ExportProgress(
  val isExporting: Boolean = false,
  val progress: Float = 0f,
  val currentFrame: Int = 0,
  val totalFrames: Int = 0,
  val currentSec: Long = 0L,
  val totalSec: Long = 0L,
  val statusText: String = "",
  val savedPath: String? = null,
  val errorMessage: String? = null
)
