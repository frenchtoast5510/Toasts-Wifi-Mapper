package com.frenchtoast.wifimapper.platform.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.frenchtoast.wifimapper.domain.math.SignalMath
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object FloorPlanImageManager {

  private var cachedSampleBitmap: ImageBitmap? = null

  fun getBuiltInSampleFloorPlan(): ImageBitmap {
    cachedSampleBitmap?.let { return it }
    val width = 1600
    val height = 1100
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.drawColor(Color.parseColor("#0E1522"))

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#162133")
      strokeWidth = 1f
    }
    for (x in 0..width step 80) {
      canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
    }
    for (y in 0..height step 80) {
      canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    val roomFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#141E30")
      style = Paint.Style.FILL
    }
    val hallFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#18253A")
      style = Paint.Style.FILL
    }
    val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#6E89AD")
      style = Paint.Style.STROKE
      strokeWidth = 8f
      strokeCap = Paint.Cap.ROUND
    }
    val innerWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#475F80")
      style = Paint.Style.STROKE
      strokeWidth = 4.5f
    }
    val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#1C293F")
      style = Paint.Style.FILL
    }
    val furnitureStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#2E4263")
      style = Paint.Style.STROKE
      strokeWidth = 2f
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#A7B9D0")
      textSize = 24f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      letterSpacing = 0.06f
    }
    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#647C9E")
      textSize = 18f
      typeface = Typeface.MONOSPACE
    }
    val doorArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#56CCF2")
      alpha = 110
      style = Paint.Style.STROKE
      strokeWidth = 2f
      pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    for (room in SignalMath.defaultDemoRooms) {
      val rect = RectF(
        room.xMin * width,
        room.yMin * height,
        room.xMax * width,
        room.yMax * height,
      )
      canvas.drawRect(rect, if (room.id == "hallway") hallFillPaint else roomFillPaint)
    }

    drawRoundedFurniture(canvas, 140f, 160f, 340f, 230f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 180f, 265f, 300f, 330f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 120f, 390f, 230f, 510f, furniturePaint, furnitureStrokePaint)

    drawRoundedFurniture(canvas, 810f, 120f, 1180f, 195f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 870f, 250f, 1120f, 355f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 1250f, 140f, 1440f, 360f, furniturePaint, furnitureStrokePaint)

    drawRoundedFurniture(canvas, 130f, 620f, 340f, 690f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 130f, 690f, 200f, 840f, furniturePaint, furnitureStrokePaint)

    drawRoundedFurniture(canvas, 1150f, 590f, 1420f, 840f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 1080f, 610f, 1135f, 675f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 1435f, 610f, 1490f, 675f, furniturePaint, furnitureStrokePaint)

    drawRoundedFurniture(canvas, 610f, 750f, 790f, 955f, furniturePaint, furnitureStrokePaint)
    drawRoundedFurniture(canvas, 845f, 740f, 995f, 975f, furniturePaint, furnitureStrokePaint)

    for (room in SignalMath.defaultDemoRooms) {
      val rect = RectF(
        room.xMin * width,
        room.yMin * height,
        room.xMax * width,
        room.yMax * height,
      )
      canvas.drawRect(rect, innerWallPaint)
    }

    val outer = RectF(
      0.06f * width,
      0.08f * height,
      0.94f * width,
      0.92f * height,
    )
    canvas.drawRect(outer, wallPaint)

    val gapPaint = Paint().apply {
      color = Color.parseColor("#18253A")
      strokeWidth = 9f
    }
    canvas.drawLine(0.37f * width, 0.52f * height, 0.42f * width, 0.52f * height, gapPaint)
    canvas.drawArc(0.37f * width, 0.46f * height, 0.43f * width, 0.52f * height, 0f, 90f, false, doorArcPaint)
    canvas.drawLine(0.49f * width, 0.44f * height, 0.56f * width, 0.44f * height, gapPaint)
    canvas.drawLine(0.36f * width, 0.55f * height, 0.36f * width, 0.62f * height, gapPaint)
    canvas.drawLine(0.64f * width, 0.50f * height, 0.64f * width, 0.58f * height, gapPaint)
    canvas.drawLine(0.48f * width, 0.64f * height, 0.55f * width, 0.64f * height, gapPaint)

    val roomLabels = listOf(
      Triple("LIVING ROOM", 0.10f * width, 0.13f * height),
      Triple("KITCHEN & DINING", 0.46f * width, 0.13f * height),
      Triple("CENTRAL HALLWAY", 0.40f * width, 0.55f * height),
      Triple("HOME OFFICE", 0.10f * width, 0.90f * height),
      Triple("GUEST & BATH", 0.40f * width, 0.90f * height),
      Triple("PRIMARY BEDROOM", 0.68f * width, 0.90f * height),
    )
    for ((title, lx, ly) in roomLabels) {
      canvas.drawText(title, lx, ly, labelPaint)
    }

    canvas.drawText("1,680 sq ft · Craftsman Main Floor · Scale 1:50", 96f, 54f, dimPaint)
    canvas.drawText("N ↑", width - 140f, 54f, labelPaint)

    val img = bmp.asImageBitmap()
    cachedSampleBitmap = img
    return img
  }

  private fun drawRoundedFurniture(
    canvas: Canvas,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    fill: Paint,
    stroke: Paint,
  ) {
    val rect = RectF(left, top, right, bottom)
    canvas.drawRoundRect(rect, 14f, 14f, fill)
    canvas.drawRoundRect(rect, 14f, 14f, stroke)
  }

  fun importFloorPlanToAppStorage(context: Context, sourceUri: Uri): String? {
    return runCatching {
      val dir = File(context.filesDir, "floorplans").apply { mkdirs() }
      val dest = File(dir, "floorplan_${System.currentTimeMillis()}.jpg")

      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, boundsOpts)
      }
      val maxDim = 2048
      var sampleSize = 1
      val longest = max(boundsOpts.outWidth, boundsOpts.outHeight)
      while (longest / sampleSize > maxDim) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOpts)
      } ?: return null

      FileOutputStream(dest).use { out ->
        decoded.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      decoded.recycle()
      dest.absolutePath
    }.getOrNull()
  }

  fun loadImageBitmapFromPath(pathOrUri: String?): ImageBitmap? {
    if (pathOrUri.isNullOrBlank()) return null
    val file = File(pathOrUri)
    if (!file.exists()) return null
    return runCatching {
      BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()
  }
}
