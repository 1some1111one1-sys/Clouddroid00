package com.example.game.render3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Mesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / STRIDE_FLOATS
    private val vaoId: Int
    private val vboId: Int

    init {
        val buffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buffer.position(0)

        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        vaoId = vaos[0]
        vboId = vbos[0]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

        val strideBytes = STRIDE_FLOATS * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
    }

    companion object {
        const val STRIDE_FLOATS = 8 // x, y, z, nx, ny, nz, u, v
    }
}

object MeshBuilder {

    private fun addTri(
        list: MutableList<Float>,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        uA: Float = 0f, vA: Float = 0f,
        uB: Float = 1f, vB: Float = 0f,
        uC: Float = 1f, vC: Float = 1f
    ) {
        val ux = bx - ax; val uy = by - ay; val uz = bz - az
        val vx = cx - ax; val vy = cy - ay; val vz = cz - az
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = hypot(hypot(nx.toDouble(), ny.toDouble()), nz.toDouble()).toFloat().coerceAtLeast(1e-5f)
        nx /= len; ny /= len; nz /= len
        list.add(ax); list.add(ay); list.add(az); list.add(nx); list.add(ny); list.add(nz); list.add(uA); list.add(vA)
        list.add(bx); list.add(by); list.add(bz); list.add(nx); list.add(ny); list.add(nz); list.add(uB); list.add(vB)
        list.add(cx); list.add(cy); list.add(cz); list.add(nx); list.add(ny); list.add(nz); list.add(uC); list.add(vC)
    }

    private fun addQuad(
        list: MutableList<Float>,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float
    ) {
        addTri(list, ax, ay, az, bx, by, bz, cx, cy, cz, 0f, 1f, 1f, 1f, 1f, 0f)
        addTri(list, ax, ay, az, cx, cy, cz, dx, dy, dz, 0f, 1f, 1f, 0f, 0f, 0f)
    }

    fun createUnitCube(uvScaleX: Float = 1f, uvScaleY: Float = 1f): Mesh {
        val verts = floatArrayOf(
            // Front (+Z)
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, uvScaleY,
            0.5f, -0.5f, 0.5f, 0f, 0f, 1f, uvScaleX, uvScaleY,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f, uvScaleX, 0f,
            -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, uvScaleY,
            0.5f, 0.5f, 0.5f, 0f, 0f, 1f, uvScaleX, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0f, 0f,
            // Back (-Z)
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, uvScaleY,
            -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, uvScaleX, uvScaleY,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, uvScaleX, 0f,
            0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, uvScaleY,
            -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, uvScaleX, 0f,
            0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0f, 0f,
            // Left (-X)
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, uvScaleY,
            -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, uvScaleX, uvScaleY,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, uvScaleX, 0f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, uvScaleY,
            -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, uvScaleX, 0f,
            -0.5f, 0.5f, -0.5f, -1f, 0f, 0f, 0f, 0f,
            // Right (+X)
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, uvScaleY,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f, uvScaleX, uvScaleY,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f, uvScaleX, 0f,
            0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, uvScaleY,
            0.5f, 0.5f, -0.5f, 1f, 0f, 0f, uvScaleX, 0f,
            0.5f, 0.5f, 0.5f, 1f, 0f, 0f, 0f, 0f,
            // Top (+Y)
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, uvScaleY,
            0.5f, 0.5f, 0.5f, 0f, 1f, 0f, uvScaleX, uvScaleY,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f, uvScaleX, 0f,
            -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, uvScaleY,
            0.5f, 0.5f, -0.5f, 0f, 1f, 0f, uvScaleX, 0f,
            -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0f, 0f,
            // Bottom (-Y)
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f,
            0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 1f, 1f,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 1f, 0f,
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f,
            0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 1f, 0f,
            -0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 0f, 0f
        )
        return Mesh(verts)
    }

    fun createGroundPlane(uvX: Float = 1f, uvY: Float = 1f): Mesh {
        val verts = floatArrayOf(
            -0.5f, 0f, 0.5f, 0f, 1f, 0f, 0f, uvY,
            0.5f, 0f, 0.5f, 0f, 1f, 0f, uvX, uvY,
            0.5f, 0f, -0.5f, 0f, 1f, 0f, uvX, 0f,
            -0.5f, 0f, 0.5f, 0f, 1f, 0f, 0f, uvY,
            0.5f, 0f, -0.5f, 0f, 1f, 0f, uvX, 0f,
            -0.5f, 0f, -0.5f, 0f, 1f, 0f, 0f, 0f
        )
        return Mesh(verts)
    }

    fun createRampWedge(): Mesh {
        val ny = 0.707f
        val nz = -0.707f
        val verts = floatArrayOf(
            -0.5f, 0f, -0.5f, 0f, ny, nz, 0f, 1f,
            0.5f, 0f, -0.5f, 0f, ny, nz, 1f, 1f,
            0.5f, 1f, 0.5f, 0f, ny, nz, 1f, 0f,
            -0.5f, 0f, -0.5f, 0f, ny, nz, 0f, 1f,
            0.5f, 1f, 0.5f, 0f, ny, nz, 1f, 0f,
            -0.5f, 1f, 0.5f, 0f, ny, nz, 0f, 0f,
            -0.5f, 0f, 0.5f, 0f, 0f, 1f, 0f, 1f,
            0.5f, 0f, 0.5f, 0f, 0f, 1f, 1f, 1f,
            0.5f, 1f, 0.5f, 0f, 0f, 1f, 1f, 0f,
            -0.5f, 0f, 0.5f, 0f, 0f, 1f, 0f, 1f,
            0.5f, 1f, 0.5f, 0f, 0f, 1f, 1f, 0f,
            -0.5f, 1f, 0.5f, 0f, 0f, 1f, 0f, 0f,
            -0.5f, 0f, -0.5f, -1f, 0f, 0f, 0f, 1f,
            -0.5f, 1f, 0.5f, -1f, 0f, 0f, 1f, 0f,
            -0.5f, 0f, 0.5f, -1f, 0f, 0f, 1f, 1f,
            0.5f, 0f, -0.5f, 1f, 0f, 0f, 0f, 1f,
            0.5f, 0f, 0.5f, 1f, 0f, 0f, 1f, 1f,
            0.5f, 1f, 0.5f, 1f, 0f, 0f, 1f, 0f
        )
        return Mesh(verts)
    }

    /** Smooth UV sphere of radius 0.5 centered at (0,0,0). */
    fun createSphere(stacks: Int = 10, slices: Int = 14): Mesh {
        val list = mutableListOf<Float>()
        fun push(theta: Float, phi: Float) {
            val nx = sin(theta) * cos(phi)
            val ny = cos(theta)
            val nz = sin(theta) * sin(phi)
            list.add(nx * 0.5f); list.add(ny * 0.5f); list.add(nz * 0.5f)
            list.add(nx); list.add(ny); list.add(nz)
            list.add(phi / (2f * PI.toFloat())); list.add(theta / PI.toFloat())
        }
        for (i in 0 until stacks) {
            val t0 = (i.toFloat() / stacks) * PI.toFloat()
            val t1 = ((i + 1).toFloat() / stacks) * PI.toFloat()
            for (j in 0 until slices) {
                val p0 = (j.toFloat() / slices) * 2f * PI.toFloat()
                val p1 = ((j + 1).toFloat() / slices) * 2f * PI.toFloat()
                push(t0, p0); push(t1, p0); push(t1, p1)
                push(t0, p0); push(t1, p1); push(t0, p1)
            }
        }
        return Mesh(list.toFloatArray())
    }

    /** Tapered cylinder/cone along Y from -0.5 to +0.5. */
    fun createTaperedCylinder(bottomRadius: Float = 0.5f, topRadius: Float = 0.5f, segments: Int = 14): Mesh {
        val list = mutableListOf<Float>()
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float = 0.5f, v: Float = 0.5f) {
            list.add(x); list.add(y); list.add(z)
            list.add(nx); list.add(ny); list.add(nz)
            list.add(u); list.add(v)
        }
        val step = (2.0 * PI / segments).toFloat()
        val slope = (bottomRadius - topRadius)
        val normLen = hypot(1.0, slope.toDouble()).toFloat()
        val nySide = slope / normLen
        val rScale = 1f / normLen

        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments

            push(c0 * bottomRadius, -0.5f, s0 * bottomRadius, c0 * rScale, nySide, s0 * rScale, u0, 1f)
            push(c1 * bottomRadius, -0.5f, s1 * bottomRadius, c1 * rScale, nySide, s1 * rScale, u1, 1f)
            push(c1 * topRadius, 0.5f, s1 * topRadius, c1 * rScale, nySide, s1 * rScale, u1, 0f)

            push(c0 * bottomRadius, -0.5f, s0 * bottomRadius, c0 * rScale, nySide, s0 * rScale, u0, 1f)
            push(c1 * topRadius, 0.5f, s1 * topRadius, c1 * rScale, nySide, s1 * rScale, u1, 0f)
            push(c0 * topRadius, 0.5f, s0 * topRadius, c0 * rScale, nySide, s0 * rScale, u0, 0f)

            if (topRadius > 0.01f) {
                push(0f, 0.5f, 0f, 0f, 1f, 0f)
                push(c1 * topRadius, 0.5f, s1 * topRadius, 0f, 1f, 0f)
                push(c0 * topRadius, 0.5f, s0 * topRadius, 0f, 1f, 0f)
            }
            if (bottomRadius > 0.01f) {
                push(0f, -0.5f, 0f, 0f, -1f, 0f)
                push(c0 * bottomRadius, -0.5f, s0 * bottomRadius, 0f, -1f, 0f)
                push(c1 * bottomRadius, -0.5f, s1 * bottomRadius, 0f, -1f, 0f)
            }
        }
        return Mesh(list.toFloatArray())
    }

    /** Wheel cylinder aligned along X axis (axle) for direct car mounting. */
    fun createWheelMesh(segments: Int = 18): Mesh {
        val list = mutableListOf<Float>()
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
            list.add(x); list.add(y); list.add(z)
            list.add(nx); list.add(ny); list.add(nz)
            list.add(0.5f); list.add(0.5f)
        }
        val step = (2.0 * PI / segments).toFloat()
        val r = 0.5f
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            // Tire tread
            push(-0.5f, c0 * r, s0 * r, 0f, c0, s0)
            push(0.5f, c0 * r, s0 * r, 0f, c0, s0)
            push(0.5f, c1 * r, s1 * r, 0f, c1, s1)
            push(-0.5f, c0 * r, s0 * r, 0f, c0, s0)
            push(0.5f, c1 * r, s1 * r, 0f, c1, s1)
            push(-0.5f, c1 * r, s1 * r, 0f, c1, s1)
            // Left & right wheel faces
            push(0.5f, 0f, 0f, 1f, 0f, 0f)
            push(0.5f, c1 * r, s1 * r, 1f, 0f, 0f)
            push(0.5f, c0 * r, s0 * r, 1f, 0f, 0f)

            push(-0.5f, 0f, 0f, -1f, 0f, 0f)
            push(-0.5f, c0 * r, s0 * r, -1f, 0f, 0f)
            push(-0.5f, c1 * r, s1 * r, -1f, 0f, 0f)
        }
        return Mesh(list.toFloatArray())
    }

    private fun addSmoothTri(
        list: MutableList<Float>,
        ax: Float, ay: Float, az: Float, nax: Float, nay: Float, naz: Float,
        bx: Float, by: Float, bz: Float, nbx: Float, nby: Float, nbz: Float,
        cx: Float, cy: Float, cz: Float, ncx: Float, ncy: Float, ncz: Float
    ) {
        list.add(ax); list.add(ay); list.add(az); list.add(nax); list.add(nay); list.add(naz); list.add(0.5f); list.add(0.5f)
        list.add(bx); list.add(by); list.add(bz); list.add(nbx); list.add(nby); list.add(nbz); list.add(0.5f); list.add(0.5f)
        list.add(cx); list.add(cy); list.add(cz); list.add(ncx); list.add(ncy); list.add(ncz); list.add(0.5f); list.add(0.5f)
    }

    /**
     * Multi-station sculpted automotive body with smooth crown curvature, tapered aerodynamic nose (+Z),
     * flared front/rear fenders, tucked rocker panels, and sculpted rear decklid (-Z).
     */
    fun createCarBodyMesh(): Mesh {
        val list = mutableListOf<Float>()
        // Cross-section stations along Z from rear (-0.50) to front (+0.50):
        // Each station defines: z, halfWidthBottom, halfWidthBelt, halfWidthCrown, yBottom, yBelt, yCrown
        data class Station(
            val z: Float,
            val wBot: Float, val wBelt: Float, val wTop: Float,
            val yBot: Float, val yBelt: Float, val yTop: Float
        )
        val stations = listOf(
            Station(-0.50f, 0.41f, 0.45f, 0.36f, -0.36f, 0.24f, 0.32f), // Rear bumper / tail
            Station(-0.43f, 0.45f, 0.49f, 0.42f, -0.46f, 0.34f, 0.42f), // Rear quarter / trunk
            Station(-0.26f, 0.48f, 0.51f, 0.44f, -0.48f, 0.40f, 0.47f), // Rear fender flare
            Station(0.00f,  0.46f, 0.49f, 0.43f, -0.50f, 0.38f, 0.44f), // Door waist
            Station(0.24f,  0.48f, 0.51f, 0.43f, -0.48f, 0.36f, 0.43f), // Front fender / cowl
            Station(0.42f,  0.45f, 0.47f, 0.38f, -0.45f, 0.14f, 0.22f), // Sloped hood
            Station(0.50f,  0.39f, 0.42f, 0.33f, -0.34f, -0.02f, 0.06f) // Front bumper / splitter
        )

        // Ring vertices per station (8 points around cross section, starting bottom-left clockwise looking +Z)
        fun ringVerts(s: Station): List<FloatArray> {
            return listOf(
                floatArrayOf(-s.wBot, s.yBot, s.z,  -0.45f, -0.89f, 0f),  // 0 bottom left
                floatArrayOf(-s.wBelt, s.yBelt, s.z, -0.94f, 0.34f, 0f),  // 1 left shoulder
                floatArrayOf(-s.wTop, s.yTop, s.z,  -0.36f, 0.93f, 0f),   // 2 left crown
                floatArrayOf(0f, s.yTop + 0.04f, s.z, 0f, 1f, 0f),        // 3 center crown
                floatArrayOf(s.wTop, s.yTop, s.z,   0.36f, 0.93f, 0f),    // 4 right crown
                floatArrayOf(s.wBelt, s.yBelt, s.z, 0.94f, 0.34f, 0f),    // 5 right shoulder
                floatArrayOf(s.wBot, s.yBot, s.z,   0.45f, -0.89f, 0f)    // 6 bottom right
            )
        }

        val rings = stations.map { ringVerts(it) }
        for (i in 0 until rings.size - 1) {
            val r0 = rings[i]
            val r1 = rings[i + 1]
            val count = r0.size
            for (k in 0 until count) {
                val kn = (k + 1) % count
                val a = r0[k]; val b = r0[kn]
                val c = r1[kn]; val d = r1[k]
                addSmoothTri(
                    list,
                    a[0], a[1], a[2], a[3], a[4], a[5],
                    d[0], d[1], d[2], d[3], d[4], d[5],
                    c[0], c[1], c[2], c[3], c[4], c[5]
                )
                addSmoothTri(
                    list,
                    a[0], a[1], a[2], a[3], a[4], a[5],
                    c[0], c[1], c[2], c[3], c[4], c[5],
                    b[0], b[1], b[2], b[3], b[4], b[5]
                )
            }
        }

        // Rear cap (-Z) and Front nose cap (+Z)
        val rear = rings.first()
        for (k in 1 until rear.size - 1) {
            val a = rear[0]; val b = rear[k]; val c = rear[k + 1]
            addTri(list, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2])
        }
        val front = rings.last()
        for (k in 1 until front.size - 1) {
            val a = front[0]; val b = front[k + 1]; val c = front[k]
            addTri(list, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2])
        }
        return Mesh(list.toFloatArray())
    }

    /** Sloped automotive greenhouse glass cabin (front +Z sloped windshield, rear -Z fastback glass). */
    fun createCarCabinMesh(): Mesh {
        val list = mutableListOf<Float>()
        val bx = 0.49f; val tx = 0.37f
        val by = -0.50f; val ty = 0.48f
        // Front (+Z) sloped windshield
        addQuad(list, -bx, by, 0.50f, bx, by, 0.50f, tx, ty, 0.17f, -tx, ty, 0.17f)
        // Rear (-Z) sloped fastback
        addQuad(list, bx, by, -0.50f, -bx, by, -0.50f, -tx, ty, -0.27f, tx, ty, -0.27f)
        // Left (-X) angled side windows
        addQuad(list, -bx, by, -0.50f, -bx, by, 0.50f, -tx, ty, 0.17f, -tx, ty, -0.27f)
        // Right (+X) angled side windows
        addQuad(list, bx, by, 0.50f, bx, by, -0.50f, tx, ty, -0.27f, tx, ty, 0.17f)
        // Roof top
        addQuad(list, -tx, ty, 0.17f, tx, ty, 0.17f, tx, ty, -0.27f, -tx, ty, -0.27f)
        return Mesh(list.toFloatArray())
    }

    /** Painted roof panel + A/B/C window pillars framing the tinted glass greenhouse. */
    fun createCarRoofFrameMesh(): Mesh {
        val list = mutableListOf<Float>()
        val bx = 0.505f; val tx = 0.385f
        val by = -0.50f; val ty = 0.515f
        // Painted roof cap slightly raised above glass roof
        addQuad(list, -tx, ty, 0.18f, tx, ty, 0.18f, tx, ty, -0.28f, -tx, ty, -0.28f)
        addQuad(list, -tx, ty - 0.06f, 0.18f, tx, ty - 0.06f, 0.18f, tx, ty, 0.18f, -tx, ty, 0.18f)
        addQuad(list, tx, ty - 0.06f, -0.28f, -tx, ty - 0.06f, -0.28f, -tx, ty, -0.28f, tx, ty, -0.28f)
        // A-pillars (front corners)
        val pw = 0.055f
        addQuad(list, -bx, by, 0.505f, -bx + pw, by, 0.505f, -tx + pw, ty, 0.18f, -tx, ty, 0.18f)
        addQuad(list, bx - pw, by, 0.505f, bx, by, 0.505f, tx, ty, 0.18f, tx - pw, ty, 0.18f)
        // B-pillars (center vertical door posts)
        addQuad(list, -bx, by, -0.02f, -bx, by, 0.06f, -tx, ty, 0.04f, -tx, ty, -0.04f)
        addQuad(list, bx, by, 0.06f, bx, by, -0.02f, tx, ty, -0.04f, tx, ty, 0.04f)
        // C-pillars (rear sail panels)
        addQuad(list, -bx, by, -0.505f, -bx, by, -0.40f, -tx, ty, -0.21f, -tx, ty, -0.28f)
        addQuad(list, bx, by, -0.40f, bx, by, -0.505f, tx, ty, -0.28f, tx, ty, -0.21f)
        return Mesh(list.toFloatArray())
    }

    /** 5-spoke metallic alloy wheel rim aligned along X axle. */
    fun createWheelSpokesMesh(): Mesh {
        val list = mutableListOf<Float>()
        val spokeCount = 5
        val rInner = 0.14f
        val rOuter = 0.46f
        val halfThick = 0.52f
        val halfW = 0.07f
        for (i in 0 until spokeCount) {
            val ang = (i.toFloat() / spokeCount) * 2f * PI.toFloat()
            val c = cos(ang); val s = sin(ang)
            val px = -s * halfW; val pz = c * halfW
            val y0 = c * rInner; val z0 = s * rInner
            val y1 = c * rOuter; val z1 = s * rOuter
            // Outer (+X) and Inner (-X) spoke faces
            addQuad(
                list,
                halfThick, y0 + px, z0 + pz,
                halfThick, y0 - px, z0 - pz,
                halfThick, y1 - px, z1 - pz,
                halfThick, y1 + px, z1 + pz
            )
            addQuad(
                list,
                -halfThick, y0 - px, z0 - pz,
                -halfThick, y0 + px, z0 + pz,
                -halfThick, y1 + px, z1 + pz,
                -halfThick, y1 - px, z1 - pz
            )
        }
        return Mesh(list.toFloatArray())
    }

    /** Pointed V-hull speedboat / ship body (+Z bow). */
    fun createBoatHullMesh(): Mesh {
        val list = mutableListOf<Float>()
        // Bow keel & deck tip at (0, ..., 0.55)
        val bowTipX = 0f; val bowDeckY = 0.45f; val bowKeelY = -0.20f; val bowZ = 0.54f
        val midW = 0.50f; val midZ = 0.08f
        val sternW = 0.44f; val sternZ = -0.50f

        // Deck top
        addTri(list, bowTipX, bowDeckY, bowZ, -midW, 0.38f, midZ, midW, 0.38f, midZ)
        addQuad(list, -midW, 0.38f, midZ, -sternW, 0.34f, sternZ, sternW, 0.34f, sternZ, midW, 0.38f, midZ)
        // Bow sides
        addQuad(list, bowTipX, bowKeelY, bowZ, bowTipX, bowDeckY, bowZ, -midW, 0.38f, midZ, -midW * 0.6f, -0.50f, midZ)
        addQuad(list, bowTipX, bowDeckY, bowZ, bowTipX, bowKeelY, bowZ, midW * 0.6f, -0.50f, midZ, midW, 0.38f, midZ)
        // Aft hull sides
        addQuad(list, -midW * 0.6f, -0.50f, midZ, -midW, 0.38f, midZ, -sternW, 0.34f, sternZ, -sternW * 0.72f, -0.45f, sternZ)
        addQuad(list, midW, 0.38f, midZ, midW * 0.6f, -0.50f, midZ, sternW * 0.72f, -0.45f, sternZ, sternW, 0.34f, sternZ)
        // Transom
        addQuad(list, -sternW * 0.72f, -0.45f, sternZ, -sternW, 0.34f, sternZ, sternW, 0.34f, sternZ, sternW * 0.72f, -0.45f, sternZ)
        return Mesh(list.toFloatArray())
    }

    /** Streamlined fuselage along Z (+Z nose, -Z tail). */
    fun createPlaneFuselageMesh(segments: Int = 14): Mesh {
        val list = mutableListOf<Float>()
        val rings = listOf(
            -0.50f to 0.14f,
            -0.25f to 0.36f,
            0.18f to 0.48f,
            0.40f to 0.34f,
            0.52f to 0.05f
        )
        val step = (2.0 * PI / segments).toFloat()
        for (rIdx in 0 until rings.size - 1) {
            val (z0, rad0) = rings[rIdx]
            val (z1, rad1) = rings[rIdx + 1]
            for (s in 0 until segments) {
                val a0 = s * step
                val a1 = (s + 1) * step
                val c0 = cos(a0); val s0 = sin(a0)
                val c1 = cos(a1); val s1 = sin(a1)
                addQuad(
                    list,
                    c0 * rad0, s0 * rad0, z0,
                    c1 * rad0, s1 * rad0, z0,
                    c1 * rad1, s1 * rad1, z1,
                    c0 * rad1, s0 * rad1, z1
                )
            }
        }
        return Mesh(list.toFloatArray())
    }

    /** Swept trapezoidal wings spanning X in [-0.5..0.5], swept backward (-Z) at tips. */
    fun createSweptWingMesh(): Mesh {
        val list = mutableListOf<Float>()
        val h = 0.50f
        // Center chord Z: [-0.35..0.45], tip chord Z: [-0.50..-0.05]
        addQuad(list, -0.50f, h, -0.08f, 0f, h, 0.45f, 0f, h, -0.35f, -0.50f, h, -0.50f)
        addQuad(list, 0f, h, 0.45f, 0.50f, h, -0.08f, 0.50f, h, -0.50f, 0f, h, -0.35f)
        addQuad(list, -0.50f, -h, -0.50f, 0f, -h, -0.35f, 0f, -h, 0.45f, -0.50f, -h, -0.08f)
        addQuad(list, 0f, -h, -0.35f, 0.50f, -h, -0.50f, 0.50f, -h, -0.08f, 0f, -h, 0.45f)
        // Leading & trailing edges
        addQuad(list, -0.50f, -h, -0.08f, 0f, -h, 0.45f, 0f, h, 0.45f, -0.50f, h, -0.08f)
        addQuad(list, 0f, -h, 0.45f, 0.50f, -h, -0.08f, 0.50f, h, -0.08f, 0f, h, 0.45f)
        addQuad(list, 0f, -h, -0.35f, -0.50f, -h, -0.50f, -0.50f, h, -0.50f, 0f, h, -0.35f)
        addQuad(list, 0.50f, -h, -0.50f, 0f, -h, -0.35f, 0f, h, -0.35f, 0.50f, h, -0.50f)
        return Mesh(list.toFloatArray())
    }

    /** Curved drooping tropical palm frond extending from origin along +Z (0..1) and arching down. */
    fun createPalmFrondMesh(): Mesh {
        val list = mutableListOf<Float>()
        val segs = 5
        for (i in 0 until segs) {
            val t0 = i.toFloat() / segs
            val t1 = (i + 1).toFloat() / segs
            val z0 = t0; val z1 = t1
            val y0 = 0.14f * sin(t0 * 2.2f) - 0.38f * t0 * t0
            val y1 = 0.14f * sin(t1 * 2.2f) - 0.38f * t1 * t1
            val w0 = 0.11f * (1f - t0 * 0.8f) + 0.04f
            val w1 = 0.11f * (1f - t1 * 0.8f) + 0.02f
            // V-shaped leaf blade (left + right half angled slightly down for natural volume)
            addQuad(list, 0f, y0 + 0.02f, z0, -w0, y0 - 0.03f, z0, -w1, y1 - 0.03f, z1, 0f, y1 + 0.02f, z1)
            addQuad(list, 0f, y0 + 0.02f, z0, 0f, y1 + 0.02f, z1, w1, y1 - 0.03f, z1, w0, y0 - 0.03f, z0)
        }
        return Mesh(list.toFloatArray())
    }

    fun createCylinder(segments: Int = 14): Mesh = createTaperedCylinder(0.5f, 0.5f, segments)

    fun createCone(segments: Int = 14): Mesh = createTaperedCylinder(0.5f, 0.01f, segments)

    fun createRing(segments: Int = 24, innerRadius: Float = 0.78f): Mesh {
        val list = mutableListOf<Float>()
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
            list.add(x); list.add(y); list.add(z)
            list.add(nx); list.add(ny); list.add(nz)
            list.add(0.5f); list.add(0.5f)
        }
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = (i + 1) * step
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            push(c0 * innerRadius, s0 * innerRadius, 0f, 0f, 0f, 1f)
            push(c0, s0, 0f, 0f, 0f, 1f)
            push(c1, s1, 0f, 0f, 0f, 1f)
            push(c0 * innerRadius, s0 * innerRadius, 0f, 0f, 0f, 1f)
            push(c1, s1, 0f, 0f, 0f, 1f)
            push(c1 * innerRadius, s1 * innerRadius, 0f, 0f, 0f, 1f)
        }
        return Mesh(list.toFloatArray())
    }
}
