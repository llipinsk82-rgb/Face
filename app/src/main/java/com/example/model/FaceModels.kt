package com.example.model

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect

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
)

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
  val featherBorder: Float = 14f,
  val alpha: Float = 1.0f,
  val isOvalMask: Boolean = true
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
  val rotation: Int = 0
)

data class ProcessingProgress(
  val isProcessing: Boolean = false,
  val progress: Float = 0f,
  val statusText: String = "",
  val processedFrames: Int = 0,
  val totalFrames: Int = 0,
  val savedPath: String? = null
)
