package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.ReplacementFace

object SampleAssets {

  fun getPresetFaces(context: Context): List<ReplacementFace> {
    return listOf(
      ReplacementFace(
        id = "preset_cyber",
        name = "Cyberpunk",
        bitmap = drawCyberpunkFace(),
        isPreset = true,
        description = "Futurystyczny neonowy hełm z wizjerem"
      ),
      ReplacementFace(
        id = "preset_mystery",
        name = "Maska Teatralna",
        bitmap = drawMysteryMask(),
        isPreset = true,
        description = "Tajemnicza stylizowana maska"
      ),
      ReplacementFace(
        id = "preset_robot",
        name = "Złoty Android",
        bitmap = drawRobotFace(),
        isPreset = true,
        description = "Metaliczna złota twarz robota"
      ),
      ReplacementFace(
        id = "preset_sunglasses",
        name = "Retro Cool",
        bitmap = drawSunglassesFace(),
        isPreset = true,
        description = "Lustrzane okulary i uśmiech"
      ),
      ReplacementFace(
        id = "preset_cat",
        name = "Kotek Anime",
        bitmap = drawCatMask(),
        isPreset = true,
        description = "Urocza stylizowana maska kotka"
      ),
      ReplacementFace(
        id = "preset_emoji",
        name = "Uśmieszek",
        bitmap = drawEmojiFace(),
        isPreset = true,
        description = "Klasyczna radosna żółta buźka"
      )
    )
  }

  private fun drawCyberpunkFace(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Base face oval
    paint.shader = LinearGradient(
      0f, 0f, size.toFloat(), size.toFloat(),
      Color.parseColor("#1A237E"), Color.parseColor("#0D47A1"), Shader.TileMode.CLAMP
    )
    canvas.drawOval(RectF(60f, 40f, 340f, 360f), paint)
    paint.shader = null

    // Glowing cyan visor
    paint.color = Color.parseColor("#00E5FF")
    paint.style = Paint.Style.FILL
    val visorPath = Path().apply {
      moveTo(80f, 150f)
      lineTo(320f, 150f)
      lineTo(300f, 210f)
      lineTo(100f, 210f)
      close()
    }
    canvas.drawPath(visorPath, paint)

    // Visor highlight
    paint.color = Color.WHITE
    paint.strokeWidth = 4f
    paint.style = Paint.Style.STROKE
    canvas.drawLine(110f, 165f, 290f, 165f, paint)

    // Cyber jaw lines
    paint.color = Color.parseColor("#7C4DFF")
    paint.strokeWidth = 6f
    canvas.drawLine(150f, 270f, 250f, 270f, paint)
    canvas.drawLine(140f, 300f, 260f, 300f, paint)

    return bmp
  }

  private fun drawMysteryMask(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Mask oval (Ivory white)
    paint.color = Color.parseColor("#F5F5F0")
    paint.style = Paint.Style.FILL
    canvas.drawOval(RectF(60f, 30f, 340f, 370f), paint)

    // Shading border
    paint.color = Color.parseColor("#CCCCCC")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawOval(RectF(63f, 33f, 337f, 367f), paint)

    // Eyes
    paint.style = Paint.Style.FILL
    paint.color = Color.BLACK
    canvas.drawOval(RectF(110f, 140f, 165f, 175f), paint)
    canvas.drawOval(RectF(235f, 140f, 290f, 175f), paint)

    // Dramatic arched eyebrows
    paint.strokeWidth = 8f
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    val browL = Path().apply {
      moveTo(95f, 130f)
      quadTo(135f, 105f, 175f, 125f)
    }
    val browR = Path().apply {
      moveTo(225f, 125f)
      quadTo(265f, 105f, 305f, 130f)
    }
    canvas.drawPath(browL, paint)
    canvas.drawPath(browR, paint)

    // Rosy cheeks
    paint.style = Paint.Style.FILL
    paint.color = Color.parseColor("#FF8A80")
    paint.alpha = 180
    canvas.drawCircle(115f, 220f, 24f, paint)
    canvas.drawCircle(285f, 220f, 24f, paint)
    paint.alpha = 255

    // Stylized mustache & smirk
    paint.color = Color.BLACK
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 7f
    val stache = Path().apply {
      moveTo(130f, 255f)
      quadTo(200f, 230f, 270f, 255f)
    }
    canvas.drawPath(stache, paint)

    // Smile
    val smile = Path().apply {
      moveTo(145f, 285f)
      quadTo(200f, 325f, 255f, 285f)
    }
    canvas.drawPath(smile, paint)

    // Goatee
    paint.style = Paint.Style.FILL
    val goatee = Path().apply {
      moveTo(190f, 320f)
      lineTo(210f, 320f)
      lineTo(200f, 345f)
      close()
    }
    canvas.drawPath(goatee, paint)

    return bmp
  }

  private fun drawRobotFace(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Gold metallic face gradient
    paint.shader = LinearGradient(
      60f, 40f, 340f, 360f,
      Color.parseColor("#FFE082"), Color.parseColor("#FFB300"), Shader.TileMode.CLAMP
    )
    canvas.drawRoundRect(RectF(70f, 50f, 330f, 350f), 60f, 60f, paint)
    paint.shader = null

    // Glowing optics (Eyes)
    paint.color = Color.parseColor("#00E676")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(140f, 160f, 25f, paint)
    canvas.drawCircle(260f, 160f, 25f, paint)
    paint.color = Color.WHITE
    canvas.drawCircle(140f, 160f, 10f, paint)
    canvas.drawCircle(260f, 160f, 10f, paint)

    // Speaker grill mouth
    paint.color = Color.parseColor("#37474F")
    for (i in 0..4) {
      val y = 250f + i * 14f
      canvas.drawRoundRect(RectF(150f, y, 250f, y + 6f), 3f, 3f, paint)
    }

    return bmp
  }

  private fun drawSunglassesFace(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Warm skin tone oval
    paint.color = Color.parseColor("#FFCC80")
    paint.style = Paint.Style.FILL
    canvas.drawOval(RectF(70f, 40f, 330f, 360f), paint)

    // Cool dark sunglasses
    paint.color = Color.parseColor("#212121")
    val leftLens = RectF(100f, 130f, 185f, 190f)
    val rightLens = RectF(215f, 130f, 300f, 190f)
    canvas.drawRoundRect(leftLens, 20f, 20f, paint)
    canvas.drawRoundRect(rightLens, 20f, 20f, paint)

    // Frame bridge
    paint.strokeWidth = 10f
    paint.style = Paint.Style.STROKE
    canvas.drawLine(185f, 155f, 215f, 155f, paint)

    // Lens reflection glare
    paint.color = Color.parseColor("#80FFFFFF")
    paint.strokeWidth = 4f
    canvas.drawLine(115f, 175f, 155f, 140f, paint)
    canvas.drawLine(230f, 175f, 270f, 140f, paint)

    // Confident smile
    paint.color = Color.parseColor("#BF360C")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 8f
    paint.strokeCap = Paint.Cap.ROUND
    val smilePath = Path().apply {
      moveTo(140f, 260f)
      quadTo(200f, 305f, 260f, 260f)
    }
    canvas.drawPath(smilePath, paint)

    return bmp
  }

  private fun drawCatMask(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Pink / White Cat face
    paint.color = Color.WHITE
    canvas.drawOval(RectF(60f, 80f, 340f, 360f), paint)

    // Ears
    paint.color = Color.WHITE
    val earL = Path().apply {
      moveTo(70f, 130f)
      lineTo(120f, 30f)
      lineTo(170f, 95f)
      close()
    }
    val earR = Path().apply {
      moveTo(330f, 130f)
      lineTo(280f, 30f)
      lineTo(230f, 95f)
      close()
    }
    canvas.drawPath(earL, paint)
    canvas.drawPath(earR, paint)

    // Inner pink ears
    paint.color = Color.parseColor("#FF80AB")
    val inEarL = Path().apply {
      moveTo(85f, 120f)
      lineTo(120f, 55f)
      lineTo(155f, 95f)
      close()
    }
    val inEarR = Path().apply {
      moveTo(315f, 120f)
      lineTo(280f, 55f)
      lineTo(245f, 95f)
      close()
    }
    canvas.drawPath(inEarL, paint)
    canvas.drawPath(inEarR, paint)

    // Big anime eyes
    paint.color = Color.parseColor("#69F0AE")
    canvas.drawOval(RectF(110f, 170f, 170f, 230f), paint)
    canvas.drawOval(RectF(230f, 170f, 290f, 230f), paint)
    paint.color = Color.BLACK
    canvas.drawCircle(140f, 200f, 20f, paint)
    canvas.drawCircle(260f, 200f, 20f, paint)
    paint.color = Color.WHITE
    canvas.drawCircle(146f, 194f, 8f, paint)
    canvas.drawCircle(266f, 194f, 8f, paint)

    // Pink nose
    paint.color = Color.parseColor("#FF4081")
    val nose = Path().apply {
      moveTo(190f, 240f)
      lineTo(210f, 240f)
      lineTo(200f, 252f)
      close()
    }
    canvas.drawPath(nose, paint)

    // Whiskers
    paint.color = Color.DKGRAY
    paint.strokeWidth = 4f
    paint.style = Paint.Style.STROKE
    canvas.drawLine(70f, 230f, 140f, 240f, paint)
    canvas.drawLine(70f, 255f, 140f, 255f, paint)
    canvas.drawLine(330f, 230f, 260f, 240f, paint)
    canvas.drawLine(330f, 255f, 260f, 255f, paint)

    return bmp
  }

  private fun drawEmojiFace(): Bitmap {
    val size = 400
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Bright yellow circle
    paint.shader = LinearGradient(
      200f, 40f, 200f, 360f,
      Color.parseColor("#FFEB3B"), Color.parseColor("#FBC02D"), Shader.TileMode.CLAMP
    )
    canvas.drawCircle(200f, 200f, 150f, paint)
    paint.shader = null

    // Happy curved eyes
    paint.color = Color.parseColor("#4E342E")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 12f
    paint.strokeCap = Paint.Cap.ROUND
    val eyeL = Path().apply {
      moveTo(120f, 160f)
      quadTo(145f, 130f, 170f, 160f)
    }
    val eyeR = Path().apply {
      moveTo(230f, 160f)
      quadTo(255f, 130f, 280f, 160f)
    }
    canvas.drawPath(eyeL, paint)
    canvas.drawPath(eyeR, paint)

    // Wide open happy smile
    paint.style = Paint.Style.FILL
    paint.color = Color.parseColor("#4E342E")
    val smileBox = RectF(130f, 180f, 270f, 290f)
    canvas.drawArc(smileBox, 0f, 180f, true, paint)

    // Tongue
    paint.color = Color.parseColor("#FF5252")
    canvas.drawCircle(200f, 270f, 28f, paint)

    return bmp
  }

  /**
   * Generates a high quality group photo bitmap with 3 distinct clear stylized faces,
   * so the app is immediately fully usable and testable even without uploading photos.
   */
  fun generateSampleGroupPhoto(): Bitmap {
    val width = 1200
    val height = 800
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Background gradient (warm studio indoor)
    paint.shader = LinearGradient(
      0f, 0f, 0f, height.toFloat(),
      Color.parseColor("#263238"), Color.parseColor("#102027"), Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // Background decorative elements
    paint.color = Color.parseColor("#37474F")
    paint.strokeWidth = 2f
    paint.style = Paint.Style.STROKE
    canvas.drawCircle(300f, 250f, 180f, paint)
    canvas.drawCircle(900f, 250f, 180f, paint)

    // Person 1 (Left) - Center x = 280, y = 380
    drawStylizedPerson(canvas, cx = 280f, cy = 380f, skinColor = "#FFCC80", hairColor = "#5D4037", shirtColor = "#0288D1")

    // Person 2 (Center) - Center x = 600, y = 350
    drawStylizedPerson(canvas, cx = 600f, cy = 350f, skinColor = "#FFE0B2", hairColor = "#FBC02D", shirtColor = "#7B1FA2")

    // Person 3 (Right) - Center x = 920, y = 390
    drawStylizedPerson(canvas, cx = 920f, cy = 390f, skinColor = "#D7CCC8", hairColor = "#212121", shirtColor = "#388E3C")

    return bmp
  }

  private fun drawStylizedPerson(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    skinColor: String,
    hairColor: String,
    shirtColor: String
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Torso / Shirt
    paint.color = Color.parseColor(shirtColor)
    paint.style = Paint.Style.FILL
    val torsoPath = Path().apply {
      moveTo(cx - 150f, cy + 240f)
      lineTo(cx - 80f, cy + 120f)
      lineTo(cx + 80f, cy + 120f)
      lineTo(cx + 150f, cy + 240f)
      close()
    }
    canvas.drawPath(torsoPath, paint)

    // Neck
    paint.color = Color.parseColor(skinColor)
    canvas.drawRect(cx - 35f, cy + 80f, cx + 35f, cy + 130f, paint)

    // Head / Face Oval (approx 180w x 220h)
    canvas.drawOval(RectF(cx - 90f, cy - 90f, cx + 90f, cy + 110f), paint)

    // Hair
    paint.color = Color.parseColor(hairColor)
    val hairPath = Path().apply {
      moveTo(cx - 95f, cy - 30f)
      quadTo(cx - 95f, cy - 120f, cx, cy - 120f)
      quadTo(cx + 95f, cy - 120f, cx + 95f, cy - 30f)
      lineTo(cx + 70f, cy - 40f)
      quadTo(cx, cy - 70f, cx - 70f, cy - 40f)
      close()
    }
    canvas.drawPath(hairPath, paint)

    // Eyes
    paint.color = Color.WHITE
    canvas.drawCircle(cx - 36f, cy + 5f, 15f, paint)
    canvas.drawCircle(cx + 36f, cy + 5f, 15f, paint)
    paint.color = Color.parseColor("#212121")
    canvas.drawCircle(cx - 36f, cy + 5f, 8f, paint)
    canvas.drawCircle(cx + 36f, cy + 5f, 8f, paint)

    // Eyebrows
    paint.strokeWidth = 5f
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    canvas.drawLine(cx - 52f, cy - 18f, cx - 20f, cy - 18f, paint)
    canvas.drawLine(cx + 20f, cy - 18f, cx + 52f, cy - 18f, paint)

    // Nose
    paint.color = Color.parseColor("#A1887F")
    paint.strokeWidth = 4f
    canvas.drawLine(cx, cy + 10f, cx - 5f, cy + 32f, paint)
    canvas.drawLine(cx - 5f, cy + 32f, cx + 6f, cy + 32f, paint)

    // Smile
    paint.color = Color.parseColor("#C2185B")
    paint.strokeWidth = 6f
    paint.style = Paint.Style.STROKE
    val smile = Path().apply {
      moveTo(cx - 30f, cy + 58f)
      quadTo(cx, cy + 82f, cx + 30f, cy + 58f)
    }
    canvas.drawPath(smile, paint)
  }
}
