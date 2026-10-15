package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.example.ml.FaceTracker
import com.example.model.DetectedFaceInfo
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import com.example.model.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FaceSwap Video", appName)
  }

  @Test
  fun `test swap config single for all`() {
    val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val globalFace = ReplacementFace(id = "face_cyber", name = "Cyber", bitmap = dummyBitmap)
    val customFace = ReplacementFace(id = "face_custom", name = "Custom", bitmap = dummyBitmap)

    val config = SwapConfig(
      isSingleForAll = true,
      globalReplacement = globalFace,
      individualMappings = mapOf(1 to customFace)
    )

    assertEquals(globalFace, config.getReplacementForFace(0))
    assertEquals(globalFace, config.getReplacementForFace(1))
    assertEquals(globalFace, config.getReplacementForFace(2))
  }

  @Test
  fun `test swap config individual mode`() {
    val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val globalFace = ReplacementFace(id = "face_cyber", name = "Cyber", bitmap = dummyBitmap)
    val individualFace1 = ReplacementFace(id = "face_robot", name = "Robot", bitmap = dummyBitmap)
    val individualFace2 = ReplacementFace(id = "face_cat", name = "Cat", bitmap = dummyBitmap)

    val config = SwapConfig(
      isSingleForAll = false,
      globalReplacement = globalFace,
      individualMappings = mapOf(
        0 to individualFace1,
        1 to individualFace2
      )
    )

    assertEquals(individualFace1, config.getReplacementForFace(0))
    assertEquals(individualFace2, config.getReplacementForFace(1))
    assertEquals(globalFace, config.getReplacementForFace(99))
  }

  @Test
  fun `test landmark angle and anchor calculation`() {
    val bounds = Rect(100, 100, 300, 300)
    // Left eye at (160, 180), Right eye at (240, 180) -> horizontal (angle = 0 deg)
    val faceHorizontal = DetectedFaceInfo(
      id = 0,
      trackingId = 1,
      bounds = bounds,
      leftEye = PointF(160f, 180f),
      rightEye = PointF(240f, 180f)
    )

    assertEquals(0f, faceHorizontal.rollAngle, 0.5f)
    assertEquals(80f, faceHorizontal.eyeDistance, 0.5f)
    assertEquals(200f, faceHorizontal.anchorCenterX, 0.5f)

    // Tilted eyes: Left eye at (160, 160), Right eye at (240, 200) -> dy = 40, dx = 80 -> ~26.56 deg
    val faceTilted = DetectedFaceInfo(
      id = 1,
      trackingId = 2,
      bounds = bounds,
      leftEye = PointF(160f, 160f),
      rightEye = PointF(240f, 200f)
    )
    assertTrue(faceTilted.rollAngle > 20f && faceTilted.rollAngle < 30f)
  }

  @Test
  fun `test face tracker temporal smoothing prevents coordinate jumps`() {
    val tracker = FaceTracker(smoothingFactor = 0.40f)

    // Frame 1: Face at (200, 200)
    val face1 = DetectedFaceInfo(
      id = 0,
      trackingId = 42,
      bounds = Rect(100, 100, 300, 300) // center = 200, 200
    )
    val state1 = tracker.update(listOf(face1))
    assertEquals(1, state1.size)
    assertEquals(200f, state1[0].centerX, 0.01f)

    // Frame 2: Jitter to (220, 200) - EMA with 0.40 smoothing factor should move 200 * 0.6 + 220 * 0.4 = 208
    val face2 = DetectedFaceInfo(
      id = 0,
      trackingId = 42,
      bounds = Rect(120, 100, 320, 300) // center = 220, 200
    )
    val state2 = tracker.update(listOf(face2))
    assertEquals(1, state2.size)
    assertEquals(208f, state2[0].centerX, 0.5f)
  }

  @Test
  fun `test vertical 9 to 16 video detection`() {
    val verticalVideo = VideoInfo(
      uriString = "content://test/video.mp4",
      durationMs = 30000L,
      width = 1080,
      height = 1920,
      rotation = 0
    )
    assertTrue(verticalVideo.isVertical)
    assertTrue(verticalVideo.aspectRatio < 1.0f)

    val landscapeVideo = VideoInfo(
      uriString = "content://test/video2.mp4",
      durationMs = 120000L,
      width = 1920,
      height = 1080,
      rotation = 0
    )
    assertTrue(!landscapeVideo.isVertical)
    assertTrue(landscapeVideo.aspectRatio > 1.0f)
  }
}
