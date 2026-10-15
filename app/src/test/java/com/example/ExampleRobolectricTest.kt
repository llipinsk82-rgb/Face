package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.example.model.DetectedFaceInfo
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import org.junit.Assert.assertEquals
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
  fun `test detected face info creation`() {
    val bounds = Rect(10, 20, 110, 150)
    val face = DetectedFaceInfo(
      id = 0,
      trackingId = 101,
      bounds = bounds,
      eulerZ = 12.5f
    )

    assertEquals(0, face.id)
    assertEquals(101, face.trackingId)
    assertEquals(100, face.bounds.width())
    assertEquals(130, face.bounds.height())
    assertEquals(12.5f, face.eulerZ, 0.001f)
  }
}
