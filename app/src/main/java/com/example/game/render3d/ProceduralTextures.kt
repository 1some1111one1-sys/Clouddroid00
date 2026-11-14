package com.example.game.render3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.cos
import kotlin.math.sin

class ProceduralTextures {
    val roadTexId: Int
    val sidewalkTexId: Int
    val buildingTexId: Int
    val buildingGlassTexId: Int
    val palmShirtTexId: Int
    val waterTexId: Int
    val runwayTexId: Int
    val grassTexId: Int
    val sandTexId: Int
    val shadowTexId: Int

    init {
        roadTexId = uploadBitmap(createRoadBitmap())
        sidewalkTexId = uploadBitmap(createSidewalkBitmap())
        buildingTexId = uploadBitmap(createBuildingBitmap())
        buildingGlassTexId = uploadBitmap(createGlassTowerBitmap())
        palmShirtTexId = uploadBitmap(createPalmShirtBitmap())
        waterTexId = uploadBitmap(createWaterBitmap())
        runwayTexId = uploadBitmap(createRunwayBitmap())
        grassTexId = uploadBitmap(createTerrainBitmap(baseR = 38, baseG = 98, baseB = 46, varR = 14, varG = 26, varB = 12))
        sandTexId = uploadBitmap(createTerrainBitmap(baseR = 216, baseG = 186, baseB = 132, varR = 16, varG = 14, varB = 10))
        shadowTexId = uploadBitmap(createShadowBitmap(), clampEdge = true)
    }

    private fun uploadBitmap(bmp: Bitmap, clampEdge: Boolean = false): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        val wrap = if (clampEdge) GLES30.GL_CLAMP_TO_EDGE else GLES30.GL_REPEAT
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bmp.recycle()
        return id
    }

    private fun createRoadBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(46, 50, 56))
        val p = Paint()

        // Fine asphalt mineral grain
        for (i in 0 until 2400) {
            val shade = when (i % 4) {
                0 -> 58
                1 -> 38
                2 -> 50
                else -> 64
            }
            p.color = Color.rgb(shade, shade + 2, shade + 5)
            val x = ((i * 73 + (i / 17) * 19) % size).toFloat()
            val y = ((i * 41 + (i / 13) * 31) % size).toFloat()
            c.drawRect(x, y, x + 2f, y + 2f, p)
        }

        // Subtle dark tire wear paths in each lane
        p.color = Color.argb(38, 18, 20, 24)
        c.drawRect(44f, 0f, 94f, size.toFloat(), p)
        c.drawRect(162f, 0f, 212f, size.toFloat(), p)

        // Crisp double yellow centerlines
        p.color = Color.rgb(238, 192, 48)
        c.drawRect(122f, 0f, 125.5f, size.toFloat(), p)
        c.drawRect(130.5f, 0f, 134f, size.toFloat(), p)

        // Shoulder white edge lines + subtle gutter shadow
        p.color = Color.argb(175, 224, 230, 238)
        c.drawRect(8f, 0f, 12f, size.toFloat(), p)
        c.drawRect(244f, 0f, 248f, size.toFloat(), p)
        p.color = Color.argb(65, 16, 18, 22)
        c.drawRect(0f, 0f, 8f, size.toFloat(), p)
        c.drawRect(248f, 0f, 256f, size.toFloat(), p)
        return bmp
    }

    private fun createSidewalkBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(154, 150, 142))
        val p = Paint()
        for (i in 0 until 900) {
            val s = 140 + (i * 17 % 26)
            p.color = Color.rgb(s, s - 3, s - 8)
            val x = ((i * 67) % size).toFloat()
            val y = ((i * 53) % size).toFloat()
            c.drawRect(x, y, x + 3f, y + 3f, p)
        }
        p.color = Color.argb(95, 92, 88, 82)
        p.strokeWidth = 2f
        for (g in 0..3) {
            val pos = g * 64f
            c.drawLine(pos, 0f, pos, size.toFloat(), p)
            c.drawLine(0f, pos, size.toFloat(), pos, p)
        }
        return bmp
    }

    private fun createBuildingBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(168, 174, 182))
        val p = Paint()

        // Subtle concrete vertical pilaster bands
        p.color = Color.rgb(144, 150, 160)
        for (col in 0 until 4) {
            c.drawRect(col * 64f, 0f, col * 64f + 8f, size.toFloat(), p)
        }

        // Horizontal floor spandrels
        for (row in 0 until 4) {
            val y = row * 64f
            p.color = Color.rgb(112, 120, 130)
            c.drawRect(0f, y, size.toFloat(), y + 10f, p)
            p.color = Color.rgb(188, 194, 202)
            c.drawRect(0f, y + 10f, size.toFloat(), y + 13f, p)
        }

        // Recessed sky-reflective windows with warm lit interiors
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val x = col * 64f + 12f
                val y = row * 64f + 16f
                val w = 40f
                val h = 40f
                val lit = (row * 3 + col * 5) % 7 == 0
                p.shader = LinearGradient(
                    x, y, x, y + h,
                    if (lit) Color.rgb(245, 224, 168) else Color.rgb(74, 114, 146),
                    if (lit) Color.rgb(176, 154, 112) else Color.rgb(28, 46, 68),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(x, y, x + w, y + h, p)
                p.shader = null

                // Mullion crossbar
                p.color = Color.rgb(88, 96, 108)
                c.drawRect(x + w * 0.48f, y, x + w * 0.54f, y + h, p)
                c.drawRect(x, y + h * 0.52f, x + w, y + h * 0.58f, p)
            }
        }
        return bmp
    }

    private fun createGlassTowerBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint()
        p.shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            Color.rgb(92, 148, 188),
            Color.rgb(36, 72, 108),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        p.shader = null

        // Steel curtain wall grid
        p.color = Color.rgb(40, 56, 74)
        for (i in 0 until 8) {
            val pos = i * 32f
            c.drawRect(pos, 0f, pos + 3f, size.toFloat(), p)
            c.drawRect(0f, pos, size.toFloat(), pos + 4f, p)
        }
        // Highlighted reflection streaks
        p.color = Color.argb(65, 210, 238, 255)
        for (i in 0 until 8) {
            if (i % 3 == 0) {
                c.drawRect(i * 32f + 4f, 4f, i * 32f + 28f, size.toFloat(), p)
            }
        }
        return bmp
    }

    private fun createPalmShirtBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(), Color.rgb(0, 184, 200), Color.rgb(0, 142, 162), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
        val p = Paint().apply {
            color = Color.rgb(0, 96, 114)
            strokeWidth = 3.5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        val centers = listOf(28f to 34f, 92f to 78f, 30f to 98f, 94f to 26f)
        for ((cx, cy) in centers) {
            c.drawLine(cx, cy, cx + 2f, cy + 18f, p)
            for (f in 0..5) {
                val a = -2.6f + f * 0.85f
                c.drawLine(cx, cy, cx + cos(a) * 14f, cy + sin(a) * 10f, p)
            }
        }
        return bmp
    }

    private fun createWaterBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(18, 92, 140))
        val p = Paint().apply { isAntiAlias = true }
        for (i in 0 until 48) {
            val x = ((i * 59) % size).toFloat()
            val y = ((i * 37) % size).toFloat()
            p.color = if (i % 3 == 0) Color.argb(100, 120, 210, 245) else Color.argb(65, 34, 140, 192)
            c.drawOval(x - 24f, y - 5f, x + 24f, y + 5f, p)
        }
        return bmp
    }

    private fun createRunwayBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(42, 46, 52))
        val p = Paint()
        for (i in 0 until 400) {
            val s = 36 + (i % 18)
            p.color = Color.rgb(s, s + 2, s + 6)
            c.drawRect(((i * 37) % size).toFloat(), ((i * 53) % size).toFloat(), ((i * 37) % size + 2).toFloat(), ((i * 53) % size + 2).toFloat(), p)
        }
        p.color = Color.rgb(236, 240, 244)
        c.drawRect(58f, 14f, 70f, 84f, p)
        return bmp
    }

    private fun createTerrainBitmap(baseR: Int, baseG: Int, baseB: Int, varR: Int, varG: Int, varB: Int): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(baseR, baseG, baseB))
        val p = Paint()
        for (i in 0 until 900) {
            val t = ((i * 29) % 11) - 5
            val r = (baseR + (t * varR) / 5).coerceIn(0, 255)
            val g = (baseG + (t * varG) / 5).coerceIn(0, 255)
            val b = (baseB + (t * varB) / 5).coerceIn(0, 255)
            p.color = Color.rgb(r, g, b)
            val x = ((i * 71) % size).toFloat()
            val y = ((i * 43) % size).toFloat()
            c.drawRect(x, y, x + 4f, y + 4f, p)
        }
        return bmp
    }

    private fun createShadowBitmap(): Bitmap {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                size * 0.5f, size * 0.5f, size * 0.48f,
                intArrayOf(Color.argb(150, 8, 10, 16), Color.argb(70, 8, 10, 16), Color.argb(0, 8, 10, 16)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(size * 0.5f, size * 0.5f, size * 0.48f, p)
        return bmp
    }
}
