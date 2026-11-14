package com.example.game.render3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.GameEngine
import com.example.game.model.DynamicProp3D
import com.example.game.model.NpcRole
import com.example.game.model.PropType
import com.example.game.model.ServicePointType
import com.example.game.model.VehicleCategory
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleType
import com.example.game.model.WeaponType
import com.example.game.world.CityWorldBuilder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class WorldGLRenderer(
    private val engine: GameEngine
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var textures: ProceduralTextures
    private lateinit var cubeMesh: Mesh
    private lateinit var planeMesh: Mesh
    private lateinit var rampMesh: Mesh
    private lateinit var cylMesh: Mesh
    private lateinit var coneMesh: Mesh
    private lateinit var sphereMesh: Mesh
    private lateinit var wheelMesh: Mesh
    private lateinit var wheelSpokesMesh: Mesh
    private lateinit var carBodyMesh: Mesh
    private lateinit var carCabinMesh: Mesh
    private lateinit var carRoofFrameMesh: Mesh
    private lateinit var boatHullMesh: Mesh
    private lateinit var planeFuselageMesh: Mesh
    private lateinit var sweptWingMesh: Mesh
    private lateinit var palmFrondMesh: Mesh
    private lateinit var ringMesh: Mesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var aspectRatio = 16f / 9f
    private var startTimeNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GraphicsPipelineInfo.glRendererString = GLES30.glGetString(GLES30.GL_RENDERER).orEmpty()
        GLES30.glClearColor(0.62f, 0.78f, 0.90f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shader = ShaderProgram()
        textures = ProceduralTextures()
        cubeMesh = MeshBuilder.createUnitCube()
        planeMesh = MeshBuilder.createGroundPlane()
        rampMesh = MeshBuilder.createRampWedge()
        cylMesh = MeshBuilder.createCylinder(14)
        coneMesh = MeshBuilder.createCone(14)
        sphereMesh = MeshBuilder.createSphere(10, 14)
        wheelMesh = MeshBuilder.createWheelMesh(18)
        wheelSpokesMesh = MeshBuilder.createWheelSpokesMesh()
        carBodyMesh = MeshBuilder.createCarBodyMesh()
        carCabinMesh = MeshBuilder.createCarCabinMesh()
        carRoofFrameMesh = MeshBuilder.createCarRoofFrameMesh()
        boatHullMesh = MeshBuilder.createBoatHullMesh()
        planeFuselageMesh = MeshBuilder.createPlaneFuselageMesh(14)
        sweptWingMesh = MeshBuilder.createSweptWingMesh()
        palmFrondMesh = MeshBuilder.createPalmFrondMesh()
        ringMesh = MeshBuilder.createRing(24, 0.78f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, aspectRatio, 1.5f, 3000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        val timeSec = (System.nanoTime() - startTimeNs) / 1_000_000_000f

        // Compute dynamic Time-of-Day atmosphere from worldTimeMinutes (0..1440)
        val hour = (engine.worldTimeMinutes / 60f) % 24f
        val dayAngle = ((hour - 6f) / 12f) * PI.toFloat() // 0 at 6am, PI/2 at noon, PI at 6pm
        val sunElev = sin(dayAngle)
        val daylight = ((sunElev + 0.18f) / 1.18f).coerceIn(0f, 1f)
        val duskFactor = (1f - abs(sunElev) * 2.2f).coerceIn(0f, 1f)
        val nightFactor = (1f - daylight).coerceIn(0f, 1f)

        val sunX = cos(dayAngle) * 0.72f
        val sunY = sunElev.coerceAtLeast(0.18f)
        val sunZ = 0.38f

        val skyTopR = lerp(0.05f, 0.14f, daylight) + duskFactor * 0.10f
        val skyTopG = lerp(0.08f, 0.38f, daylight) + duskFactor * 0.04f
        val skyTopB = lerp(0.20f, 0.74f, daylight)
        val skyHorR = lerp(0.10f, 0.74f, daylight) + duskFactor * 0.24f
        val skyHorG = lerp(0.13f, 0.84f, daylight) - duskFactor * 0.18f
        val skyHorB = lerp(0.24f, 0.94f, daylight) - duskFactor * 0.32f
        val sunColR = lerp(0.55f, 1.0f, daylight) + duskFactor * 0.10f
        val sunColG = lerp(0.62f, 0.94f, daylight) - duskFactor * 0.24f
        val sunColB = lerp(0.85f, 0.80f, daylight) - duskFactor * 0.38f
        val ambientFactor = lerp(0.36f, 1.02f, daylight)

        GLES30.glUseProgram(shader.programId)
        GLES30.glUniform3f(shader.uFogColorLoc, skyHorR, skyHorG, skyHorB)
        GLES30.glUniform1f(shader.uFogNearLoc, 660f)
        GLES30.glUniform1f(shader.uFogFarLoc, 2100f)
        GLES30.glUniform3f(shader.uSunDirLoc, sunX, sunY, sunZ)
        GLES30.glUniform3f(shader.uSkyTopColorLoc, skyTopR, skyTopG, skyTopB)
        GLES30.glUniform3f(shader.uSkyHorizonColorLoc, skyHorR, skyHorG, skyHorB)
        GLES30.glUniform3f(shader.uSunColorLoc, sunColR, sunColG, sunColB)
        GLES30.glUniform1f(shader.uAmbientFactorLoc, ambientFactor)

        val player = engine.player
        val pCar = player.vehicleId?.let { id -> engine.vehicles.firstOrNull { it.id == id } }
        val targetY = if (pCar != null) pCar.altitude * 165f + pCar.zHeight else player.zHeight

        val yaw = engine.cameraYawRad
        val pitch = engine.cameraPitchRad
        val fwdX = cos(yaw)
        val fwdZ = sin(yaw)
        val rightX = -sin(yaw)
        val rightZ = cos(yaw)

        val camDist: Float
        val camHeight: Float
        val shoulderOffset: Float
        val lookAhead: Float

        if (pCar == null) {
            camDist = 35f
            camHeight = 16f + sin(pitch) * 19f
            shoulderOffset = 7.2f
            lookAhead = 72f
        } else if (pCar.type.category == VehicleCategory.AIR) {
            camDist = 94f
            camHeight = 26f + sin(pitch) * 32f
            shoulderOffset = 0f
            lookAhead = 95f
        } else {
            camDist = 66f
            camHeight = 22f + sin(pitch) * 26f
            shoulderOffset = 0f
            lookAhead = 68f
        }

        val px = player.x
        val pz = player.y
        val eyeX = px - fwdX * camDist + rightX * shoulderOffset
        val eyeY = (targetY + camHeight).coerceAtLeast(3.5f)
        val eyeZ = pz - fwdZ * camDist + rightZ * shoulderOffset

        val lookX = px + fwdX * lookAhead + rightX * (shoulderOffset * 0.4f)
        val lookY = targetY + (if (pCar == null) 12.2f else 6.8f) - sin(pitch) * 22f
        val lookZ = pz + fwdZ * lookAhead + rightZ * (shoulderOffset * 0.4f)

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES30.glUniform3f(shader.uCameraPosLoc, eyeX, eyeY, eyeZ)

        // 0. Atmospheric Sky Dome with gradient & sun disc
        GLES30.glDepthMask(false)
        GLES30.glUniform1i(shader.uIsSkyLoc, 1)
        drawMeshAt(
            sphereMesh, eyeX, eyeY - 120f, eyeZ,
            2200f, 2200f, 2200f,
            0f, 0f, 0f,
            1f, 1f, 1f, 1f,
            0, 1f, 0f, 16f
        )
        GLES30.glUniform1i(shader.uIsSkyLoc, 0)
        GLES30.glDepthMask(true)

        val cullDist = 1750f
        val world = engine.world

        // 1. Infinite Animated Ocean Water Sea (Surrounds all 3 islands & flows under bridges)
        drawMeshAt(
            planeMesh, px, -0.25f, pz,
            4200f, 1f, 4200f,
            0f, 0f, 0f,
            0.84f, 0.93f, 1.0f, 1f,
            textures.waterTexId, 0.04f, 0.58f, 48f,
            uvScaleX = 30f, uvScaleY = 30f,
            uvOffsetX = (px / 140f) + timeSec * 0.025f,
            uvOffsetY = (pz / 140f) + timeSec * 0.015f
        )

        // 2. 3 Island Landmasses + Concrete Coastal Seawalls
        for (island in world.islands) {
            if (px < island.left - cullDist || px > island.right + cullDist ||
                pz < island.top - cullDist || pz > island.bottom + cullDist
            ) continue
            // Seawall perimeter skirt descending into ocean
            drawBox(
                island.centerX, -0.35f, island.centerY,
                island.w + 18f, 0.9f, island.h + 18f,
                0f, 0.44f, 0.46f, 0.50f, 1f, textures.sidewalkTexId, 0f, 0.05f, 10f,
                uvScaleX = (island.w / 120f).coerceAtLeast(1f),
                uvScaleY = (island.h / 120f).coerceAtLeast(1f)
            )
            // Elevated urban ground slab
            drawBox(
                island.centerX, -0.02f, island.centerY,
                island.w, 0.46f, island.h,
                0f, 0.36f, 0.38f, 0.41f, 1f, textures.sidewalkTexId, 0f, 0.05f, 10f,
                uvScaleX = (island.w / 72f).coerceAtLeast(1f),
                uvScaleY = (island.h / 72f).coerceAtLeast(1f)
            )
        }

        // 2b. Bone County / Verdant Desert Terrain
        for (dz in world.desertZones) {
            if (px < dz.left - cullDist || px > dz.right + cullDist ||
                pz < dz.top - cullDist || pz > dz.bottom + cullDist
            ) continue
            drawMeshAt(
                planeMesh, dz.centerX, 0.24f, dz.centerY,
                dz.w, 1f, dz.h,
                0f, 0f, 0f,
                0.86f, 0.68f, 0.44f, 1f,
                textures.sandTexId, 0f, 0.03f, 8f,
                uvScaleX = (dz.w / 140f).coerceAtLeast(1f),
                uvScaleY = (dz.h / 140f).coerceAtLeast(1f)
            )
        }

        // 3. Sand Zones (Coastal Beaches across Islands 1, 2 & 3)
        for (sz in world.sandZones) {
            if (px < sz.left - cullDist || px > sz.right + cullDist ||
                pz < sz.top - cullDist || pz > sz.bottom + cullDist
            ) continue
            drawMeshAt(
                planeMesh, sz.centerX, 0.26f, sz.centerY,
                sz.w, 1f, sz.h,
                0f, 0f, 0f,
                1f, 0.96f, 0.90f, 1f,
                textures.sandTexId, 0f, 0.04f, 8f,
                uvScaleX = (sz.w / 120f).coerceAtLeast(1f),
                uvScaleY = (sz.h / 120f).coerceAtLeast(1f)
            )
        }

        // 4. Parks
        for (pk in world.parks) {
            if (!isNear(pk.centerX, pk.centerY, px, pz, cullDist + maxOf(pk.w, pk.h) * 0.5f)) continue
            drawMeshAt(
                planeMesh, pk.centerX, 0.27f, pk.centerY,
                pk.w, 1f, pk.h,
                0f, 0f, 0f,
                1f, 1f, 1f, 1f,
                textures.grassTexId, 0f, 0.05f, 8f,
                uvScaleX = (pk.w / 110f).coerceAtLeast(1f),
                uvScaleY = (pk.h / 110f).coerceAtLeast(1f)
            )
        }

        // 5. Piers over water
        for (pier in world.piers) {
            if (!isNear(pier.centerX, pier.centerY, px, pz, cullDist + maxOf(pier.w, pier.h))) continue
            drawBox(
                pier.centerX, 0.6f, pier.centerY, pier.w, 1.2f, pier.h,
                0f, 0.62f, 0.58f, 0.52f, 1f, textures.sidewalkTexId, 0f,
                uvScaleX = (pier.w / 60f).coerceAtLeast(1f),
                uvScaleY = (pier.h / 60f).coerceAtLeast(1f)
            )
        }

        // 5b. Inter-Island Bridges (Concrete Decks, Water Pilings, Guardrails & Suspension Towers)
        for ((bIdx, br) in world.bridges.withIndex()) {
            if (px < br.left - cullDist || px > br.right + cullDist ||
                pz < br.top - cullDist || pz > br.bottom + cullDist
            ) continue
            val isHoriz = br.w > br.h
            // Bridge concrete under-deck & curb skirts (roads draw cleanly on top at y=0.28)
            drawBox(
                br.centerX, -0.30f, br.centerY,
                br.w + 20f, 0.90f, br.h + 20f,
                0f, 0.42f, 0.45f, 0.50f, 1f, textures.sidewalkTexId, 0f, 0.10f, 16f,
                uvScaleX = (br.w / 80f).coerceAtLeast(1f),
                uvScaleY = (br.h / 80f).coerceAtLeast(1f)
            )
            // Guardrails along both sides of the bridge
            if (isHoriz) {
                drawBox(br.centerX, 2.2f, br.top - 4f, br.w, 3.6f, 4f, 0f, 0.70f, 0.74f, 0.78f, 1f, 0, 0f, 0.25f, 24f)
                drawBox(br.centerX, 2.2f, br.bottom + 4f, br.w, 3.6f, 4f, 0f, 0.70f, 0.74f, 0.78f, 1f, 0, 0f, 0.25f, 24f)
            } else {
                drawBox(br.left - 4f, 2.2f, br.centerY, 4f, 3.6f, br.h, 0f, 0.70f, 0.74f, 0.78f, 1f, 0, 0f, 0.25f, 24f)
                drawBox(br.right + 4f, 2.2f, br.centerY, 4f, 3.6f, br.h, 0f, 0.70f, 0.74f, 0.78f, 1f, 0, 0f, 0.25f, 24f)
            }
            // Support pilings into the ocean channel + Suspension Bridge Pylons on Golden Bay & East Bay bridges
            val towerColorR = if (bIdx == 2) 0.82f else 0.62f
            val towerColorG = if (bIdx == 2) 0.26f else 0.66f
            val towerColorB = if (bIdx == 2) 0.20f else 0.72f
            val spanSteps = 3
            for (s in 1 until spanSteps) {
                val frac = s.toFloat() / spanSteps
                val tx = if (isHoriz) br.left + br.w * frac else br.centerX
                val tz = if (isHoriz) br.centerY else br.top + br.h * frac
                val sideDx = if (isHoriz) 0f else br.w * 0.54f
                val sideDz = if (isHoriz) br.h * 0.54f else 0f
                // Twin pylons rising cleanly beside the bridge deck
                drawBox(tx - sideDx, 36f, tz - sideDz, 10f, 84f, 10f, 0f, towerColorR, towerColorG, towerColorB, 1f, 0, 0.02f, 0.35f, 32f)
                drawBox(tx + sideDx, 36f, tz + sideDz, 10f, 84f, 10f, 0f, towerColorR, towerColorG, towerColorB, 1f, 0, 0.02f, 0.35f, 32f)
                // Overhead crossbeam arch above roadway
                val beamW = if (isHoriz) 8f else br.w * 1.15f
                val beamD = if (isHoriz) br.h * 1.15f else 8f
                drawBox(tx, 74f, tz, beamW, 7f, beamD, 0f, towerColorR, towerColorG, towerColorB, 1f, 0, 0.05f, 0.4f, 32f)
                // Aviation warning beacon atop pylon
                drawMeshAt(sphereMesh, tx - sideDx, 80f, tz - sideDz, 3.2f, 3.2f, 3.2f, 0f, 0f, 0f, 1f, 0.3f, 0.2f, 1f, 0, 0.95f)
                drawMeshAt(sphereMesh, tx + sideDx, 80f, tz + sideDz, 3.2f, 3.2f, 3.2f, 0f, 0f, 0f, 1f, 0.3f, 0.2f, 1f, 0, 0.95f)
            }
        }

        // 6. 3 Airport Runways & Aprons
        for (rw in world.runwayZones) {
            if (!isNear(rw.centerX, rw.centerY, px, pz, cullDist + maxOf(rw.w, rw.h))) continue
            drawBox(
                rw.centerX, 0.24f, rw.centerY, rw.w, 0.30f, rw.h,
                0f, 0.95f, 0.95f, 0.98f, 1f, textures.runwayTexId, 0f, 0.14f, 22f,
                uvScaleX = (rw.w / 110f).coerceAtLeast(1f),
                uvScaleY = (rw.h / 110f).coerceAtLeast(1f)
            )
        }

        // 7. Roads + Textured Sidewalks + Curbs
        for (road in world.roads) {
            val r = road.rect
            if (!isNear(r.centerX, r.centerY, px, pz, cullDist + maxOf(r.w, r.h))) continue
            drawBox(
                r.centerX, 0.22f, r.centerY, r.w + 26f, 0.44f, r.h + 26f,
                0f, 0.92f, 0.90f, 0.86f, 1f, textures.sidewalkTexId, 0f, 0.05f, 12f,
                uvScaleX = ((r.w + 26f) / 72f).coerceAtLeast(1f),
                uvScaleY = ((r.h + 26f) / 72f).coerceAtLeast(1f)
            )
            val rotDeg = if (road.isHorizontal) 90f else 0f
            val roadLen = if (road.isHorizontal) r.w else r.h
            val roadWid = if (road.isHorizontal) r.h else r.w
            drawBox(
                r.centerX, 0.28f, r.centerY, roadWid, 0.42f, roadLen,
                rotDeg, 1f, 1f, 1f, 1f, textures.roadTexId, 0f, 0.16f, 26f,
                uvScaleX = 1f,
                uvScaleY = (roadLen / 96f).coerceAtLeast(1f)
            )
        }

        // 8. Metro Rail Loop & 8 3D Railway Stations
        val track = world.railTrack
        for (i in track.indices) {
            val (x1, y1) = track[i]
            val (x2, y2) = track[(i + 1) % track.size]
            val mx = (x1 + x2) * 0.5f
            val mz = (y1 + y2) * 0.5f
            val len = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
            if (!isNear(mx, mz, px, pz, cullDist + len)) continue
            val angDeg = (kotlin.math.atan2((x2 - x1).toDouble(), (y2 - y1).toDouble()) * 180.0 / PI).toFloat()
            drawBox(mx, 0.52f, mz, 22f, 0.7f, len, angDeg, 0.24f, 0.27f, 0.33f, 1f, 0, 0f, 0.2f, 24f)
            drawBox(mx, 0.92f, mz, 3.2f, 0.35f, len, angDeg, 0.72f, 0.78f, 0.85f, 1f, 0, 0.05f, 0.7f, 64f)
        }
        for (st in world.metroStations) {
            if (!isNear(st.x, st.y, px, pz, cullDist)) continue
            // Boarding platform + steel canopy pillars + modern overhanging station roof + cyan beacon trim
            drawBox(st.x, 1.2f, st.y, 44f, 1.8f, 68f, 0f, 0.58f, 0.62f, 0.68f, 1f, textures.sidewalkTexId, 0f, 0.15f, 16f, 1.5f, 2f)
            drawMeshAt(cylMesh, st.x - 16f, 9f, st.y - 24f, 2.2f, 16f, 2.2f, 0f, 0f, 0f, 0.25f, 0.30f, 0.38f, 1f, 0, 0f, 0.4f, 32f)
            drawMeshAt(cylMesh, st.x + 16f, 9f, st.y - 24f, 2.2f, 16f, 2.2f, 0f, 0f, 0f, 0.25f, 0.30f, 0.38f, 1f, 0, 0f, 0.4f, 32f)
            drawMeshAt(cylMesh, st.x - 16f, 9f, st.y + 24f, 2.2f, 16f, 2.2f, 0f, 0f, 0f, 0.25f, 0.30f, 0.38f, 1f, 0, 0f, 0.4f, 32f)
            drawMeshAt(cylMesh, st.x + 16f, 9f, st.y + 24f, 2.2f, 16f, 2.2f, 0f, 0f, 0f, 0.25f, 0.30f, 0.38f, 1f, 0, 0f, 0.4f, 32f)
            drawBox(st.x, 17.5f, st.y, 48f, 2.4f, 72f, 0f, 0.16f, 0.22f, 0.30f, 1f, 0, 0.04f, 0.6f, 48f)
            drawBox(st.x, 18.8f, st.y, 49f, 1.0f, 73f, 0f, 0f, 0.90f, 1f, 1f, 0, 0.72f)
        }

        // 9. Stunt Ramps
        for (ramp in world.stuntRamps) {
            val r = ramp.rect
            if (!isNear(r.centerX, r.centerY, px, pz, cullDist)) continue
            val deg = (ramp.launchAngleRad * 180f / PI).toFloat() + 90f
            drawMeshAt(
                rampMesh, r.centerX, 0.4f, r.centerY,
                r.w, 18f, r.h,
                deg, 0f, 0f,
                0.88f, 0.62f, 0.18f, 1f, 0, 0.06f, 0.22f, 24f
            )
        }

        // 10. Architectural Buildings (Stepped setbacks, reflective glass towers + masonry midrises)
        for (idx in world.buildings.indices) {
            val b = world.buildings[idx]
            val r = b.rect
            if (!isNear(r.centerX, r.centerY, px, pz, cullDist + 240f)) continue
            val h3d = b.height * 2.25f
            val isGlassTower = b.height > 58f || idx % 3 == 0
            val texId = if (isGlassTower) textures.buildingGlassTexId else textures.buildingTexId
            val spec = if (isGlassTower) 0.55f else 0.16f
            val shininess = if (isGlassTower) 48f else 18f
            val uvW = (r.w / 68f).coerceAtLeast(1f)
            val uvH = (h3d / 58f).coerceAtLeast(1f)

            // Main tower shaft
            drawBox(
                r.centerX, h3d * 0.5f, r.centerY, r.w, h3d, r.h,
                0f, 0.92f, 0.94f, 0.98f, 1f, texId, 0f, spec, shininess, uvW, uvH
            )
            // Ground-floor podium / storefront trim
            drawBox(
                r.centerX, 7f, r.centerY, r.w + 3f, 14f, r.h + 3f,
                0f, 0.24f, 0.27f, 0.32f, 1f, 0, 0f, 0.15f, 18f
            )
            // Roof cornice & stepped mechanical penthouse
            drawBox(
                r.centerX, h3d + 1.5f, r.centerY, r.w + 3.5f, 3f, r.h + 3.5f,
                0f, b.roofColor.red, b.roofColor.green, b.roofColor.blue, 1f, 0, 0.04f, 0.2f, 20f
            )
            if (b.height > 45f && !b.hasHelipad) {
                drawBox(
                    r.centerX, h3d + 7f, r.centerY, r.w * 0.58f, 8f, r.h * 0.58f,
                    0f, 0.32f, 0.36f, 0.42f, 1f, texId, 0f, spec * 0.6f, shininess, uvW * 0.6f, 0.8f
                )
            }
            if (b.hasHelipad) {
                drawMeshAt(
                    cylMesh, r.centerX, h3d + 3.2f, r.centerY, 44f, 1.0f, 44f,
                    0f, 0f, 0f, 0.22f, 0.25f, 0.30f, 1f, 0, 0f, 0.2f, 16f
                )
                drawMeshAt(
                    ringMesh, r.centerX, h3d + 3.9f, r.centerY, 36f, 36f, 1f,
                    0f, 90f, 0f, 1.0f, 0.78f, 0.08f, 1f, 0, 0.4f, 0.1f, 8f
                )
            }
        }

        // 10b. 3D Mountain Peaks & Red-Rock Canyon Mesas (Mt. Chiliad & Desert Ridge)
        for (peak in world.mountainPeaks) {
            if (!isNear(peak.x, peak.y, px, pz, cullDist + peak.radius)) continue
            if (peak.isDesertMesa) {
                drawMeshAt(
                    cylMesh, peak.x, peak.height * 0.32f, peak.y,
                    peak.radius * 1.45f, peak.height * 0.64f, peak.radius * 1.45f,
                    0f, 0f, 0f, 0.68f, 0.42f, 0.25f, 1f, 0, 0f, 0.06f, 10f
                )
                drawMeshAt(
                    cylMesh, peak.x + peak.radius * 0.08f, peak.height * 0.72f, peak.y - peak.radius * 0.06f,
                    peak.radius * 0.96f, peak.height * 0.36f, peak.radius * 0.96f,
                    0f, 0f, 0f, 0.78f, 0.50f, 0.30f, 1f, 0, 0f, 0.08f, 12f
                )
            } else {
                drawMeshAt(
                    coneMesh, peak.x, peak.height * 0.5f, peak.y,
                    peak.radius * 1.9f, peak.height, peak.radius * 1.9f,
                    0f, 0f, 0f, 0.28f, 0.36f, 0.29f, 1f, 0, 0f, 0.06f, 10f
                )
                drawMeshAt(
                    coneMesh, peak.x, peak.height * 0.62f, peak.y,
                    peak.radius * 1.25f, peak.height * 0.76f, peak.radius * 1.25f,
                    0f, 0f, 0f, 0.40f, 0.43f, 0.46f, 1f, 0, 0f, 0.08f, 14f
                )
                if (peak.height >= 260f) {
                    drawMeshAt(
                        coneMesh, peak.x, peak.height * 0.88f, peak.y,
                        peak.radius * 0.52f, peak.height * 0.26f, peak.radius * 0.52f,
                        0f, 0f, 0f, 0.94f, 0.96f, 0.99f, 1f, 0, 0.05f, 0.25f, 20f
                    )
                }
            }
        }

        // 11. Organic Multi-Segment Palm Trees, Alpine Pines, Cacti, Traffic Gantries & Streetlights
        for ((idx, tree) in world.palmTrees.withIndex()) {
            val (tx, ty) = tree
            if (!isNear(tx, ty, px, pz, cullDist)) continue
            drawShadowDisc(tx - sunX * 4f, 0.38f, ty - sunZ * 4f, 18f)
            drawRealisticPalmTree(tx, ty, idx)
        }
        for ((pIdx, pine) in world.pineTrees.withIndex()) {
            val (tx, ty) = pine
            if (!isNear(tx, ty, px, pz, cullDist)) continue
            drawShadowDisc(tx - sunX * 4f, 0.38f, ty - sunZ * 4f, 16f)
            drawPineTree3D(tx, ty, pIdx)
        }
        for (cactus in world.cacti) {
            val (cx, cy) = cactus
            if (!isNear(cx, cy, px, pz, cullDist)) continue
            drawCactus3D(cx, cy)
        }
        drawNearbyTrafficSignals(px, pz)
        drawNearbyStreetlights(px, pz)

        // 11b. 3D Tire Skid Marks on Asphalt
        for (sm in engine.skidMarks) {
            if (!isNear(sm.x1, sm.y1, px, pz, 900f)) continue
            val mx = (sm.x1 + sm.x2) * 0.5f
            val mz = (sm.y1 + sm.y2) * 0.5f
            val len = hypot((sm.x2 - sm.x1).toDouble(), (sm.y2 - sm.y1).toDouble()).toFloat().coerceAtLeast(3f)
            val angDeg = (kotlin.math.atan2((sm.x2 - sm.x1).toDouble(), (sm.y2 - sm.y1).toDouble()) * 180.0 / PI).toFloat()
            drawBox(mx, 0.51f, mz, 3.2f, 0.08f, len, angDeg, 0.06f, 0.06f, 0.08f, sm.alpha, 0, 0f, 0.05f, 8f)
        }

        // 12. Service Points, 3D Diner Pavilions, Hidden Packages & Mission Beacons / Sky Rings
        val pulse = 0.85f + 0.15f * sin(timeSec * 4f)
        for (sp in world.servicePoints) {
            if (!isNear(sp.x, sp.y, px, pz, cullDist)) continue
            if (sp.type == ServicePointType.DINER) {
                drawDinerPavilion3D(sp.x, sp.y)
            }
            if (sp.cooldownSec > 0f) continue
            val (cr, cg, cb) = when (sp.type) {
                ServicePointType.ARMORY -> Triple(0f, 0.9f, 1f)
                ServicePointType.SPRAY_SHOP -> Triple(1f, 0.7f, 0f)
                ServicePointType.HEALTH_PACK -> Triple(0.1f, 0.95f, 0.45f)
                ServicePointType.ARMOR_VEST -> Triple(0.2f, 0.75f, 1f)
                ServicePointType.BRIBE_STAR -> Triple(1f, 0.85f, 0.1f)
                ServicePointType.DINER -> Triple(1f, 0.48f, 0.14f)
            }
            drawMeshAt(cylMesh, sp.x, 8f, sp.y, 12f * pulse, 16f, 12f * pulse, timeSec * 60f, 0f, 0f, cr, cg, cb, 0.45f, 0, 0.8f)
        }
        for (pkg in world.hiddenPackages) {
            if (pkg.id in engine.collectedPackages || !isNear(pkg.x, pkg.y, px, pz, cullDist)) continue
            drawMeshAt(sphereMesh, pkg.x, 7.5f + sin(timeSec * 3f) * 2f, pkg.y, 8f, 8f, 8f, timeSec * 90f, 0f, 0f, 0f, 0.9f, 1f, 1f, 0, 0.8f, 0.7f, 48f)
        }

        val am = engine.activeMission
        if (am != null) {
            val isAerial = am.def.goalType == com.example.game.model.MissionGoalType.AERIAL_RINGS
            if (isAerial) {
                drawMeshAt(ringMesh, am.targetX, 90f, am.targetY, 70f, 70f, 8f, 0f, 0f, 0f, 1f, 0.7f, 0f, 0.85f, 0, 0.9f)
            } else {
                drawMeshAt(cylMesh, am.targetX, 45f, am.targetY, 26f * pulse, 90f, 26f * pulse, 0f, 0f, 0f, 1f, 0.7f, 0f, 0.4f, 0, 0.85f)
            }
        } else {
            for (m in CityWorldBuilder.missions) {
                if (!isNear(m.triggerX, m.triggerY, px, pz, cullDist)) continue
                val done = m.id in engine.completedMissions
                val cr = if (done) 0.1f else 1f
                val cg = if (done) 0.9f else 0.7f
                val cb = if (done) 0.45f else 0.9f
                drawMeshAt(cylMesh, m.triggerX, 24f, m.triggerY, 18f * pulse, 48f, 18f * pulse, 0f, 0f, 0f, cr, cg, cb, 0.35f, 0, 0.85f)
            }
        }

        // 13. Interactive Dynamic 3D World Props
        for (prop in engine.props) {
            if (!isNear(prop.x, prop.y, px, pz, cullDist)) continue
            drawShadowDisc(prop.x, 0.52f, prop.y, prop.type.radius * 2.2f)
            drawProp3D(prop)
        }

        // 14. Vehicles (Sculpted Cars with Steering Wheels, Brake Lights, Headlight Cones, Boats, Planes, Trains)
        for (v in engine.vehicles) {
            if (v.hp <= 0f) continue
            if (!isNear(v.x, v.y, px, pz, cullDist)) continue
            if (v.type.category == VehicleCategory.LAND && v.altitude < 0.3f) {
                drawShadowDisc(v.x - sunX * 3.5f, 0.54f, v.y - sunZ * 3.5f, v.type.length * 1.15f)
            }
            drawVehicle3D(v, timeSec, nightFactor)
        }

        // 15. 5-Star Police Chopper
        if (engine.chopperActive) {
            drawChopper3D(engine.chopperX, engine.chopperY, engine.chopperAngle, timeSec)
        }

        // 16. Articulated Non-Cubic NPCs & On-Foot Player
        for (npc in engine.npcs) {
            if (!isNear(npc.x, npc.y, px, pz, cullDist)) continue
            drawShadowDisc(npc.x - sunX * 2.2f, 0.52f, npc.y - sunZ * 2.2f, 11f)
            drawCharacter3D(
                x = npc.x, yHeight = npc.zHeight, z = npc.y,
                angleRad = npc.angleRad,
                shirtR = npc.shirtColor.red, shirtG = npc.shirtColor.green, shirtB = npc.shirtColor.blue,
                pantsR = npc.pantsColor.red, pantsG = npc.pantsColor.green, pantsB = npc.pantsColor.blue,
                usePalmTex = false,
                isOfficer = npc.role == NpcRole.OFFICER || npc.role == NpcRole.SWAT,
                hasCap = npc.hasCap,
                walkPhase = npc.walkPhase,
                aiming = npc.role != NpcRole.CIVILIAN && npc.behaviorState == com.example.game.model.NpcBehaviorState.COMBAT,
                weaponType = WeaponType.PISTOL
            )
        }
        if (player.vehicleId == null) {
            drawShadowDisc(player.x - sunX * 2.2f, 0.52f, player.y - sunZ * 2.2f, 12f)
            drawCharacter3D(
                x = player.x, yHeight = player.zHeight, z = player.y,
                angleRad = player.angleRad,
                shirtR = 1f, shirtG = 1f, shirtB = 1f,
                pantsR = 0.14f, pantsG = 0.18f, pantsB = 0.28f,
                usePalmTex = true,
                isOfficer = false,
                hasCap = false,
                walkPhase = player.walkCycle,
                aiming = engine.inputFiring || player.equippedWeapon != WeaponType.BATON,
                weaponType = player.equippedWeapon
            )
            // Muzzle flash burst at weapon barrel tip
            if (player.muzzleFlashTimerSec > 0f && player.equippedWeapon != WeaponType.BATON) {
                val fx = player.x + cos(player.angleRad) * 11.5f
                val fz = player.y + sin(player.angleRad) * 11.5f
                drawMeshAt(
                    sphereMesh, fx, player.zHeight + 11.2f, fz,
                    4.4f, 4.4f, 6.0f,
                    90f - (player.angleRad * 180f / PI).toFloat(), 0f, 0f,
                    1f, 0.88f, 0.35f, 0.95f,
                    0, 1.0f
                )
            }
        }

        // 17. Projectiles & Spherical 3D Smoke/Explosion/Water Particles
        for (p in engine.projectiles) {
            if (!isNear(p.x, p.y, px, pz, cullDist)) continue
            drawMeshAt(
                sphereMesh, p.x, 9f, p.y,
                2.6f, 2.2f, 5.5f,
                0f, 0f, 0f,
                p.color.red, p.color.green, p.color.blue, 1f,
                0, 0.95f
            )
        }
        for (pt in engine.particles) {
            if (!isNear(pt.x, pt.y, px, pz, cullDist)) continue
            val lifeFrac = (pt.life / pt.maxLife).coerceIn(0f, 1f)
            val puffSize = pt.radius * (2.2f - lifeFrac)
            drawMeshAt(
                sphereMesh, pt.x, pt.zHeight.coerceAtLeast(1f), pt.y,
                puffSize, puffSize, puffSize,
                0f, 0f, 0f,
                pt.color.red, pt.color.green, pt.color.blue, lifeFrac * 0.85f,
                0, 0.35f, 0.1f, 8f
            )
        }
    }

    private fun drawShadowDisc(x: Float, y: Float, z: Float, diameter: Float) {
        GLES30.glDepthMask(false)
        drawMeshAt(
            planeMesh, x, y, z,
            diameter, 1f, diameter,
            0f, 0f, 0f,
            1f, 1f, 1f, 0.72f,
            textures.shadowTexId, 0f, 0f, 8f
        )
        GLES30.glDepthMask(true)
    }

    private fun drawProp3D(p: DynamicProp3D) {
        val r = p.type.primaryColor.red
        val g = p.type.primaryColor.green
        val b = p.type.primaryColor.blue
        val ar = p.type.accentColor.red
        val ag = p.type.accentColor.green
        val ab = p.type.accentColor.blue
        val cy = p.zHeight + p.type.height * 0.5f

        when (p.type) {
            PropType.TRAFFIC_CONE -> {
                drawMeshAt(cubeMesh, p.x, p.zHeight + 0.8f, p.y, 7.2f, 1.4f, 7.2f, p.yawDeg, p.pitchDeg, p.rollDeg, r, g, b, 1f, 0, 0.04f, 0.25f, 24f)
                drawMeshAt(coneMesh, p.x, cy + 0.4f, p.y, 5.4f, p.type.height, 5.4f, p.yawDeg, p.pitchDeg, p.rollDeg, r, g, b, 1f, 0, 0.08f, 0.35f, 32f)
                drawMeshAt(cylMesh, p.x, cy + 0.5f, p.y, 3.8f, 2.0f, 3.8f, p.yawDeg, p.pitchDeg, p.rollDeg, ar, ag, ab, 1f, 0, 0.25f, 0.4f, 32f)
            }
            PropType.WOODEN_CRATE -> {
                drawMeshAt(cubeMesh, p.x, cy, p.y, 14f, p.type.height, 14f, p.yawDeg, p.pitchDeg, p.rollDeg, r, g, b, 1f, 0, 0.02f, 0.08f, 10f)
                drawMeshAt(cubeMesh, p.x, cy, p.y, 14.5f, 2.2f, 14.5f, p.yawDeg, p.pitchDeg, p.rollDeg, ar, ag, ab, 1f, 0, 0f, 0.05f, 8f)
            }
            PropType.HYDRANT -> {
                drawMeshAt(cylMesh, p.x, cy - 0.5f, p.y, 5.2f, p.type.height - 1.5f, 5.2f, p.yawDeg, p.pitchDeg, p.rollDeg, r, g, b, 1f, 0, 0.05f, 0.45f, 36f)
                drawMeshAt(sphereMesh, p.x, cy + 3.8f, p.y, 5.4f, 3.6f, 5.4f, p.yawDeg, p.pitchDeg, p.rollDeg, ar, ag, ab, 1f, 0, 0.05f, 0.6f, 48f)
                drawMeshAt(cylMesh, p.x, cy + 1.8f, p.y, 7.6f, 2.2f, 2.6f, p.yawDeg, p.pitchDeg, p.rollDeg, ar, ag, ab, 1f, 0, 0.05f, 0.6f, 48f)
            }
            PropType.HAZARD_BARREL -> {
                drawMeshAt(cylMesh, p.x, cy, p.y, 9.2f, p.type.height, 9.2f, p.yawDeg, p.pitchDeg, p.rollDeg, r, g, b, 1f, 0, 0.06f, 0.42f, 32f)
                drawMeshAt(cylMesh, p.x, cy, p.y, 9.6f, 2.6f, 9.6f, p.yawDeg, p.pitchDeg, p.rollDeg, ar, ag, ab, 1f, 0, 0.18f, 0.5f, 40f)
            }
        }
    }

    private fun drawCharacter3D(
        x: Float, yHeight: Float, z: Float,
        angleRad: Float,
        shirtR: Float, shirtG: Float, shirtB: Float,
        pantsR: Float, pantsG: Float, pantsB: Float,
        usePalmTex: Boolean,
        isOfficer: Boolean,
        hasCap: Boolean,
        walkPhase: Float,
        aiming: Boolean,
        weaponType: WeaponType
    ) {
        val yawDeg = 90f - (angleRad * 180f / PI).toFloat()
        val swing = sin(walkPhase) * 22f
        val sideX = -sin(angleRad)
        val sideZ = cos(angleRad)
        val fwdX = cos(angleRad)
        val fwdZ = sin(angleRad)

        // Articulated cylindrical legs & shoes
        drawMeshAt(cylMesh, x + sideX * 1.7f, yHeight + 3.6f, z + sideZ * 1.7f, 2.3f, 6.2f, 2.4f, yawDeg, swing, 0f, pantsR, pantsG, pantsB, 1f, 0, 0f, 0.08f, 12f)
        drawMeshAt(cylMesh, x - sideX * 1.7f, yHeight + 3.6f, z - sideZ * 1.7f, 2.3f, 6.2f, 2.4f, yawDeg, -swing, 0f, pantsR, pantsG, pantsB, 1f, 0, 0f, 0.08f, 12f)
        drawMeshAt(sphereMesh, x + sideX * 1.7f, yHeight + 0.9f, z + sideZ * 1.7f, 2.4f, 1.4f, 3.4f, yawDeg, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f, 0, 0f, 0.2f, 20f)
        drawMeshAt(sphereMesh, x - sideX * 1.7f, yHeight + 0.9f, z - sideZ * 1.7f, 2.4f, 1.4f, 3.4f, yawDeg, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f, 0, 0f, 0.2f, 20f)

        // Rounded torso (Signature Teal Palm Shirt for player; tactical vest & belt for officers)
        val tex = if (usePalmTex) textures.palmShirtTexId else 0
        drawMeshAt(cylMesh, x, yHeight + 9.4f, z, 6.0f, 6.4f, 4.0f, yawDeg, 0f, 0f, shirtR, shirtG, shirtB, 1f, tex, 0.04f, 0.12f, 14f)
        if (isOfficer) {
            drawMeshAt(cylMesh, x, yHeight + 6.5f, z, 6.2f, 1.1f, 4.2f, yawDeg, 0f, 0f, 0.08f, 0.09f, 0.12f, 1f, 0, 0f, 0.25f, 18f)
        }
        // Rounded shoulders
        drawMeshAt(sphereMesh, x + sideX * 2.8f, yHeight + 11.8f, z + sideZ * 2.8f, 2.4f, 2.2f, 2.4f, yawDeg, 0f, 0f, shirtR, shirtG, shirtB, 1f, tex, 0.04f, 0.12f, 14f)
        drawMeshAt(sphereMesh, x - sideX * 2.8f, yHeight + 11.8f, z - sideZ * 2.8f, 2.4f, 2.2f, 2.4f, yawDeg, 0f, 0f, shirtR, shirtG, shirtB, 1f, tex, 0.04f, 0.12f, 14f)

        // Neck + smooth spherical head + sculpted hair/cap
        drawMeshAt(cylMesh, x, yHeight + 12.8f, z, 1.8f, 1.4f, 1.8f, yawDeg, 0f, 0f, 0.85f, 0.67f, 0.54f, 1f, 0, 0f, 0.12f, 12f)
        drawMeshAt(sphereMesh, x, yHeight + 14.6f, z, 3.7f, 4.1f, 3.8f, yawDeg, 0f, 0f, 0.88f, 0.70f, 0.56f, 1f, 0, 0f, 0.16f, 16f)
        val hairR = if (isOfficer) 0.10f else 0.16f
        val hairG = if (isOfficer) 0.20f else 0.11f
        val hairB = if (isOfficer) 0.44f else 0.09f
        drawMeshAt(sphereMesh, x - fwdX * 0.25f, yHeight + 15.8f, z - fwdZ * 0.25f, 3.9f, 2.2f, 4.0f, yawDeg, 0f, 0f, hairR, hairG, hairB, 1f, 0, 0f, 0.22f, 20f)
        if (hasCap) {
            drawBox(x + fwdX * 1.4f, yHeight + 15.6f, z + fwdZ * 1.4f, 3.4f, 0.45f, 2.2f, yawDeg, hairR, hairG, hairB, 1f, 0, 0f, 0.25f, 16f)
        }

        // Arms & multi-part weapon
        if (aiming) {
            val armX = x + fwdX * 3.8f
            val armZ = z + fwdZ * 3.8f
            drawMeshAt(cylMesh, armX, yHeight + 10.8f, armZ, 1.8f, 5.0f, 1.8f, yawDeg, 90f, 0f, 0.86f, 0.68f, 0.54f, 1f, 0, 0f, 0.12f, 12f)
            val gunLen = when (weaponType) {
                WeaponType.PULSE_RIFLE, WeaponType.SHOTGUN -> 7.2f
                WeaponType.LAUNCHER -> 9.0f
                else -> 4.2f
            }
            val gunX = x + fwdX * 6.8f
            val gunZ = z + fwdZ * 6.8f
            drawMeshAt(cylMesh, gunX, yHeight + 11.1f, gunZ, 1.1f, gunLen, 1.1f, yawDeg, 90f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0, 0.05f, 0.55f, 48f)
        } else {
            drawMeshAt(cylMesh, x + sideX * 3.3f, yHeight + 9.4f, z + sideZ * 3.3f, 1.7f, 4.8f, 1.7f, yawDeg, -swing * 0.6f, 0f, 0.86f, 0.68f, 0.54f, 1f, 0, 0f, 0.12f, 12f)
            drawMeshAt(cylMesh, x - sideX * 3.3f, yHeight + 9.4f, z - sideZ * 3.3f, 1.7f, 4.8f, 1.7f, yawDeg, swing * 0.6f, 0f, 0.86f, 0.68f, 0.54f, 1f, 0, 0f, 0.12f, 12f)
        }
    }

    private fun drawVehicle3D(v: VehicleEntity, timeSec: Float, nightFactor: Float) {
        val yawDeg = 90f - (v.angleRad * 180f / PI).toFloat()
        val y = 3.2f + v.zHeight + v.altitude * 165f
        val r = v.customColor.red
        val g = v.customColor.green
        val b = v.customColor.blue
        val fwdX = cos(v.angleRad)
        val fwdZ = sin(v.angleRad)
        val rightX = -sin(v.angleRad)
        val rightZ = cos(v.angleRad)

        when (v.type.category) {
            VehicleCategory.AIR -> {
                val roll = (if (v.altitude > 0.1f) sin(timeSec * 1.5f) * 6f else 0f) + v.rollDeg
                // Streamlined tapered fuselage
                drawMeshAt(planeFuselageMesh, v.x, y + 4.8f, v.y, 13f, 11f, v.type.length * 1.05f, yawDeg, v.pitchDeg, roll, r, g, b, 1f, 0, 0.04f, 0.55f, 48f)
                // Tinted canopy dome
                drawMeshAt(sphereMesh, v.x + fwdX * 8f, y + 8.6f, v.y + fwdZ * 8f, 7.2f, 5.2f, 16f, yawDeg, v.pitchDeg, roll, 0.10f, 0.18f, 0.28f, 1f, 0, 0.05f, 0.85f, 72f)
                // Swept aerodynamic wings
                drawMeshAt(sweptWingMesh, v.x, y + 4.5f, v.y, v.type.width * 1.05f, 1.8f, 24f, yawDeg, v.pitchDeg, roll, v.type.accentColor.red, v.type.accentColor.green, v.type.accentColor.blue, 1f, 0, 0.04f, 0.45f, 40f)
                // Vertical tail fin
                val tailX = v.x - fwdX * (v.type.length * 0.42f)
                val tailZ = v.y - fwdZ * (v.type.length * 0.42f)
                drawMeshAt(sweptWingMesh, tailX, y + 10.5f, tailZ, 12f, 1.8f, 12f, yawDeg, v.pitchDeg, 90f + roll, v.type.accentColor.red, v.type.accentColor.green, v.type.accentColor.blue, 1f, 0, 0.05f, 0.4f, 36f)
                // Spinning propeller or jet afterburner
                val noseX = v.x + fwdX * (v.type.length * 0.53f)
                val noseZ = v.y + fwdZ * (v.type.length * 0.53f)
                if (v.type == VehicleType.PROP_PLANE) {
                    drawMeshAt(cubeMesh, noseX, y + 4.8f, noseZ, 15f, 1.6f, 0.8f, yawDeg, v.pitchDeg, timeSec * 1400f, 0.2f, 0.22f, 0.26f, 0.85f, 0, 0.1f, 0.6f, 32f)
                } else if (v.speed > 180f) {
                    drawMeshAt(sphereMesh, tailX - fwdX * 4f, y + 4.8f, tailZ - fwdZ * 4f, 4.5f, 4.5f, 11f, yawDeg, v.pitchDeg, roll, 0.2f, 0.85f, 1f, 0.9f, 0, 0.95f)
                }
            }
            VehicleCategory.WATER -> {
                val bobY = 2.0f + sin(timeSec * 2.4f + v.id) * 0.5f
                drawMeshAt(boatHullMesh, v.x, bobY, v.y, v.type.width * 0.92f, 6.2f, v.type.length, yawDeg, v.pitchDeg, v.rollDeg, r, g, b, 1f, 0, 0.04f, 0.65f, 52f)
                // Deck cabin & sloped windshield
                drawMeshAt(carCabinMesh, v.x - fwdX * 4f, bobY + 4.8f, v.y - fwdZ * 4f, v.type.width * 0.62f, 4.5f, v.type.length * 0.36f, yawDeg, v.pitchDeg, v.rollDeg, 0.12f, 0.20f, 0.30f, 1f, 0, 0.05f, 0.8f, 64f)
            }
            VehicleCategory.RAIL -> {
                drawMeshAt(carBodyMesh, v.x, 7.5f, v.y, 26f, 13f, v.type.length * 0.96f, yawDeg, 0f, 0f, r, g, b, 1f, 0, 0.05f, 0.55f, 48f)
                drawMeshAt(carCabinMesh, v.x, 14.5f, v.y, 23f, 4.8f, v.type.length * 0.92f, yawDeg, 0f, 0f, 0.10f, 0.18f, 0.26f, 1f, 0, 0.12f, 0.8f, 64f)
                drawBox(v.x, 9.5f, v.y, 26.4f, 1.8f, v.type.length * 0.90f, yawDeg, 0f, 0.9f, 1f, 1f, 0, 0.65f)
            }
            VehicleCategory.LAND -> {
                if (v.type == VehicleType.MOTORCYCLE) {
                    // Sportbike frame, sculpted fuel tank, front fork, handlebars, chrome muffler & 5-spoke wheels
                    val wheelSpin = (timeSec * v.speed * 11f) % 360f
                    drawMeshAt(carBodyMesh, v.x, y + 2.2f, v.y, 6.2f, 4.6f, 24f, yawDeg, v.pitchDeg, v.rollDeg, r, g, b, 1f, 0, 0.03f, 0.88f, 84f)
                    drawMeshAt(sphereMesh, v.x + fwdX * 3f, y + 5.0f, v.y + fwdZ * 3f, 5.2f, 3.6f, 8.5f, yawDeg, v.pitchDeg, v.rollDeg, r, g, b, 1f, 0, 0.03f, 0.92f, 92f)
                    drawMeshAt(cubeMesh, v.x - fwdX * 3.5f, y + 4.4f, v.y - fwdZ * 3.5f, 4.2f, 1.8f, 9.0f, yawDeg, v.pitchDeg, v.rollDeg, 0.12f, 0.12f, 0.14f, 1f, 0, 0f, 0.25f, 18f)
                    // Front fork + handlebars
                    drawMeshAt(cylMesh, v.x + fwdX * 8.5f, y + 4.5f, v.y + fwdZ * 8.5f, 1.4f, 8.5f, 1.4f, yawDeg, v.pitchDeg + 22f, v.rollDeg, 0.76f, 0.80f, 0.85f, 1f, 0, 0f, 0.85f, 64f)
                    drawMeshAt(cylMesh, v.x + fwdX * 6.5f, y + 7.5f, v.y + fwdZ * 6.5f, 1.1f, 9.5f, 1.1f, yawDeg + 90f, 0f, 90f, 0.18f, 0.20f, 0.22f, 1f, 0, 0f, 0.55f, 40f)
                    // Wheels & 5-spoke rims
                    for (wzOff in listOf(11.2f, -11.2f)) {
                        val wx = v.x + fwdX * wzOff
                        val wz = v.y + fwdZ * wzOff
                        drawMeshAt(wheelMesh, wx, y + 0.4f, wz, 2.8f, 7.6f, 7.6f, yawDeg, wheelSpin, v.rollDeg, 0.10f, 0.10f, 0.12f, 1f, 0, 0f, 0.22f, 16f)
                        drawMeshAt(wheelSpokesMesh, wx, y + 0.4f, wz, 3.0f, 5.0f, 5.0f, yawDeg, wheelSpin, v.rollDeg, 0.78f, 0.82f, 0.88f, 1f, 0, 0f, 0.85f, 72f)
                    }
                    drawMeshAt(sphereMesh, v.x + fwdX * 11.4f, y + 4.8f, v.y + fwdZ * 11.4f, 2.4f, 2.2f, 1.6f, yawDeg, 0f, 0f, 1f, 0.96f, 0.82f, 1f, 0, 0.95f)
                } else {
                    val isVan = v.type == VehicleType.ARMORED_VAN
                    val isPolice = v.type == VehicleType.POLICE_CRUISER
                    val isTaxi = v.type == VehicleType.TAXI
                    val isSports = v.type == VehicleType.SPORTS_CAR
                    val isMuscle = v.type == VehicleType.MUSCLE_CAR

                    val bodyW = v.type.width * 0.90f
                    val bodyL = v.type.length * 0.94f
                    val bodyH = if (isVan) 7.4f else if (isSports) 5.6f else 6.4f
                    val wheelRadius = if (isVan) 6.8f else 6.1f
                    val wheelWid = if (isSports) 3.4f else 2.8f
                    val axleX = bodyW * 0.485f
                    val axleZ = bodyL * 0.315f
                    val wheelSpin = (timeSec * v.speed * 10f) % 360f

                    // 4 Rotating 3D Tires + Front Wheel Steering + Recessed Wheel-Well Arches + Brake Discs + 5-Spoke Rims
                    val wheelOffsets = listOf(
                        Triple(axleX, axleZ, true), Triple(-axleX, axleZ, true),
                        Triple(axleX, -axleZ, false), Triple(-axleX, -axleZ, false)
                    )
                    for ((ox, oz, isFront) in wheelOffsets) {
                        val wx = v.x + rightX * ox + fwdX * oz
                        val wz = v.y + rightZ * ox + fwdZ * oz
                        val wYaw = yawDeg + if (isFront) v.steerAngleDeg else 0f
                        // Recessed wheel-well arch liner
                        drawBox(wx * 0.9999f, y + 0.6f, wz * 0.9999f, wheelWid + 0.8f, wheelRadius * 0.85f, wheelRadius * 1.18f, yawDeg, 0.05f, 0.06f, 0.08f, 1f, 0, 0f, 0.05f, 8f)
                        // Rubber tire sidewall & tread
                        drawMeshAt(wheelMesh, wx, y - 0.1f, wz, wheelWid, wheelRadius, wheelRadius, wYaw, wheelSpin, v.rollDeg, 0.10f, 0.11f, 0.13f, 1f, 0, 0f, 0.22f, 14f)
                        // Dark brake rotor disc
                        drawMeshAt(wheelMesh, wx, y - 0.1f, wz, wheelWid + 0.15f, wheelRadius * 0.62f, wheelRadius * 0.62f, wYaw, wheelSpin, v.rollDeg, 0.20f, 0.22f, 0.26f, 1f, 0, 0f, 0.45f, 36f)
                        // 5-Spoke metallic alloy rim
                        drawMeshAt(wheelSpokesMesh, wx, y - 0.1f, wz, wheelWid + 0.25f, wheelRadius * 0.60f, wheelRadius * 0.60f, wYaw, wheelSpin, v.rollDeg, 0.80f, 0.84f, 0.90f, 1f, 0, 0f, 0.88f, 76f)
                        // Center lug cap
                        drawMeshAt(wheelMesh, wx, y - 0.1f, wz, wheelWid + 0.32f, wheelRadius * 0.18f, wheelRadius * 0.18f, wYaw, wheelSpin, v.rollDeg, 0.65f, 0.70f, 0.76f, 1f, 0, 0f, 0.85f, 64f)
                    }

                    // Matte dark underbody chassis pan / rocker skirts
                    drawBox(
                        v.x, y + 0.3f, v.y,
                        bodyW * 0.92f, 1.4f, bodyL * 0.92f,
                        yawDeg, 0.10f, 0.11f, 0.13f, 1f, 0, 0f, 0.15f, 16f
                    )

                    // Smooth Clearcoat Sculpted Car Bodywork (Curved hood, flared fenders, high-gloss clearcoat!)
                    drawMeshAt(
                        carBodyMesh,
                        v.x, y + 2.3f, v.y,
                        bodyW, bodyH, bodyL,
                        yawDeg, v.pitchDeg, v.rollDeg,
                        r, g, b, 1f, 0, 0.02f, 0.88f, 92f
                    )

                    // Police patrol white doors/roof contrast OR Muscle car racing stripe
                    if (isPolice) {
                        drawBox(
                            v.x, y + 2.8f, v.y,
                            bodyW * 1.005f, 3.2f, bodyL * 0.38f,
                            yawDeg, 0.94f, 0.96f, 0.99f, 1f, 0, 0.02f, 0.82f, 80f
                        )
                    } else if (isMuscle) {
                        drawBox(
                            v.x + fwdX * (bodyL * 0.22f), y + 4.0f, v.y + fwdZ * (bodyL * 0.22f),
                            bodyW * 0.24f, 1.8f, bodyL * 0.42f,
                            yawDeg, 0.95f, 0.95f, 0.95f, 1f, 0, 0.02f, 0.75f, 72f
                        )
                    }

                    // Front & Rear Sculpted Bumpers + Front Radiator Grille + Dual Exhaust Tips
                    val frontZ = bodyL * 0.495f
                    val rearZ = -bodyL * 0.495f
                    val bumperCol = if (isPolice || isVan) 0.14f else 0.16f
                    drawBox(
                        v.x + fwdX * frontZ, y + 1.1f, v.y + fwdZ * frontZ,
                        bodyW * 0.94f, 1.6f, 2.2f,
                        yawDeg, bumperCol, bumperCol, bumperCol + 0.02f, 1f, 0, 0f, 0.45f, 32f
                    )
                    drawBox(
                        v.x + fwdX * rearZ, y + 1.2f, v.y + fwdZ * rearZ,
                        bodyW * 0.94f, 1.7f, 2.0f,
                        yawDeg, bumperCol, bumperCol, bumperCol + 0.02f, 1f, 0, 0f, 0.45f, 32f
                    )
                    // Front dark radiator grille + chrome slat
                    drawBox(
                        v.x + fwdX * (frontZ + 0.2f), y + 2.1f, v.y + fwdZ * (frontZ + 0.2f),
                        bodyW * 0.44f, 1.4f, 0.9f,
                        yawDeg, 0.08f, 0.09f, 0.11f, 1f, 0, 0f, 0.35f, 28f
                    )
                    drawBox(
                        v.x + fwdX * (frontZ + 0.4f), y + 2.1f, v.y + fwdZ * (frontZ + 0.4f),
                        bodyW * 0.40f, 0.35f, 0.6f,
                        yawDeg, 0.82f, 0.85f, 0.90f, 1f, 0, 0f, 0.90f, 80f
                    )
                    // Dual chrome rear exhaust pipes
                    for (side in listOf(-1f, 1f)) {
                        val ex = v.x + fwdX * (rearZ - 0.8f) + rightX * (side * bodyW * 0.28f)
                        val ez = v.y + fwdZ * (rearZ - 0.8f) + rightZ * (side * bodyW * 0.28f)
                        drawMeshAt(cylMesh, ex, y + 0.7f, ez, 1.3f, 2.4f, 1.3f, yawDeg, 90f, 0f, 0.76f, 0.80f, 0.86f, 1f, 0, 0f, 0.92f, 84f)
                    }

                    if (isVan) {
                        // Heavy Armored Van upper box + armored side windows + roof vents
                        drawBox(
                            v.x - fwdX * (bodyL * 0.05f), y + 7.8f, v.y - fwdZ * (bodyL * 0.05f),
                            bodyW * 0.94f, 6.2f, bodyL * 0.78f,
                            yawDeg, r, g, b, 1f, 0, 0.02f, 0.55f, 48f
                        )
                        drawMeshAt(
                            carCabinMesh,
                            v.x + fwdX * (bodyL * 0.18f), y + 7.4f, v.y + fwdZ * (bodyL * 0.18f),
                            bodyW * 0.88f, 4.4f, bodyL * 0.34f,
                            yawDeg, v.pitchDeg, v.rollDeg,
                            0.08f, 0.14f, 0.22f, 1f, 0, 0.04f, 0.95f, 96f
                        )
                    } else {
                        // Tinted glossy windshield/windows framed inside painted roof & A/B/C pillars!
                        val cabLen = bodyL * 0.50f
                        val cabZ = -bodyL * 0.06f
                        val cabY = y + (if (isSports) 6.5f else 7.2f)
                        val cabH = if (isSports) 3.8f else 4.5f
                        val cabX = v.x + fwdX * cabZ
                        val cabZZ = v.y + fwdZ * cabZ

                        drawMeshAt(
                            carCabinMesh,
                            cabX, cabY, cabZZ,
                            bodyW * 0.81f, cabH, cabLen,
                            yawDeg, v.pitchDeg, v.rollDeg,
                            0.07f, 0.12f, 0.18f, 1f, 0, 0.04f, 0.95f, 96f
                        )
                        val roofR = if (isPolice) 0.95f else r
                        val roofG = if (isPolice) 0.97f else g
                        val roofB = if (isPolice) 1.0f else b
                        drawMeshAt(
                            carRoofFrameMesh,
                            cabX, cabY, cabZZ,
                            bodyW * 0.82f, cabH, cabLen,
                            yawDeg, v.pitchDeg, v.rollDeg,
                            roofR, roofG, roofB, 1f, 0, 0.02f, 0.88f, 88f
                        )

                        // Side-view wing mirrors at A-pillar base
                        for (side in listOf(-1f, 1f)) {
                            val mx = v.x + fwdX * (bodyL * 0.13f) + rightX * (side * bodyW * 0.54f)
                            val mz = v.y + fwdZ * (bodyL * 0.13f) + rightZ * (side * bodyW * 0.54f)
                            drawBox(mx, y + 5.4f, mz, 2.2f, 1.3f, 1.6f, yawDeg, r, g, b, 1f, 0, 0.02f, 0.82f, 72f)
                        }
                    }

                    // Crisp Headlights (+Z front), Amber Corners, Active Brake Taillights (-Z rear) & License Plates
                    val lightX = bodyW * 0.33f
                    drawBox(v.x + fwdX * frontZ + rightX * lightX, y + 2.3f, v.y + fwdZ * frontZ + rightZ * lightX, 3.0f, 1.3f, 1.0f, yawDeg, 1f, 0.97f, 0.86f, 1f, 0, 0.95f)
                    drawBox(v.x + fwdX * frontZ - rightX * lightX, y + 2.3f, v.y + fwdZ * frontZ - rightZ * lightX, 3.0f, 1.3f, 1.0f, yawDeg, 1f, 0.97f, 0.86f, 1f, 0, 0.95f)
                    // Amber corner turn-signal markers
                    drawBox(v.x + fwdX * (frontZ - 0.8f) + rightX * (bodyW * 0.45f), y + 2.1f, v.y + fwdZ * (frontZ - 0.8f) + rightZ * (bodyW * 0.45f), 1.0f, 0.9f, 1.4f, yawDeg, 1f, 0.68f, 0.10f, 1f, 0, 0.75f)
                    drawBox(v.x + fwdX * (frontZ - 0.8f) - rightX * (bodyW * 0.45f), y + 2.1f, v.y + fwdZ * (frontZ - 0.8f) - rightZ * (bodyW * 0.45f), 1.0f, 0.9f, 1.4f, yawDeg, 1f, 0.68f, 0.10f, 1f, 0, 0.75f)

                    // Headlight road illumination pools (subtle in daytime, crisp at dusk/night)
                    val headAlpha = (0.10f + nightFactor * 0.34f).coerceIn(0f, 0.48f)
                    if (v.altitude < 0.1f && abs(v.speed) > 15f) {
                        val beamX = v.x + fwdX * (frontZ + 22f)
                        val beamZ = v.y + fwdZ * (frontZ + 22f)
                        GLES30.glDepthMask(false)
                        drawMeshAt(planeMesh, beamX, 0.56f, beamZ, bodyW * 1.6f, 1f, 40f, yawDeg, 0f, 0f, 1f, 0.96f, 0.82f, headAlpha, textures.shadowTexId, 0.9f, 0f, 8f)
                        GLES30.glDepthMask(true)
                    }

                    // Active Brake Glow vs Normal Running Taillights
                    val tailEmissive = if (v.isBraking) 1.0f else 0.62f
                    val tailSize = if (v.isBraking) 3.9f else 3.3f
                    drawBox(v.x + fwdX * rearZ + rightX * lightX, y + 2.8f, v.y + fwdZ * rearZ + rightZ * lightX, tailSize, 1.2f, 0.95f, yawDeg, 1.0f, if (v.isBraking) 0.18f else 0.10f, 0.12f, 1f, 0, tailEmissive)
                    drawBox(v.x + fwdX * rearZ - rightX * lightX, y + 2.8f, v.y + fwdZ * rearZ - rightZ * lightX, tailSize, 1.2f, 0.95f, yawDeg, 1.0f, if (v.isBraking) 0.18f else 0.10f, 0.12f, 1f, 0, tailEmissive)

                    // Front & Rear License Plates
                    drawBox(v.x + fwdX * (frontZ + 0.55f), y + 1.15f, v.y + fwdZ * (frontZ + 0.55f), 4.6f, 1.1f, 0.4f, yawDeg, 0.94f, 0.95f, 0.90f, 1f, 0, 0.08f)
                    drawBox(v.x + fwdX * (rearZ - 0.55f), y + 1.65f, v.y + fwdZ * (rearZ - 0.55f), 4.6f, 1.1f, 0.4f, yawDeg, 0.94f, 0.95f, 0.90f, 1f, 0, 0.08f)

                    // Type-specific authentic car trims
                    if (isSports) {
                        // GT Rear Wing Spoiler
                        val spX = v.x - fwdX * (bodyL * 0.43f)
                        val spZ = v.y - fwdZ * (bodyL * 0.43f)
                        drawBox(spX, y + 6.4f, spZ, bodyW * 0.90f, 0.7f, 3.8f, yawDeg, 0.10f, 0.11f, 0.13f, 1f, 0, 0f, 0.65f, 56f)
                        for (side in listOf(-1f, 1f)) {
                            drawBox(spX + rightX * (side * bodyW * 0.26f), y + 5.1f, spZ + rightZ * (side * bodyW * 0.26f), 0.9f, 2.2f, 2.4f, yawDeg, 0.10f, 0.11f, 0.13f, 1f, 0, 0f, 0.5f, 40f)
                        }
                    } else if (isMuscle) {
                        // Raised Chrome Engine Intake Scoop
                        drawBox(v.x + fwdX * (bodyL * 0.22f), y + 4.8f, v.y + fwdZ * (bodyL * 0.22f), 4.6f, 1.8f, 6.2f, yawDeg, 0.78f, 0.82f, 0.88f, 1f, 0, 0f, 0.92f, 84f)
                    } else if (isTaxi) {
                        // Illuminated Roof TAXI Sign
                        drawBox(v.x - fwdX * 1.5f, y + 10.2f, v.y - fwdZ * 1.5f, 5.4f, 1.8f, 2.8f, yawDeg, 1.0f, 0.86f, 0.20f, 1f, 0, 0.82f)
                    } else if (isPolice) {
                        // Steel Front Push-Bar + Flashing Red/Blue LED Lightbar
                        drawBox(v.x + fwdX * (frontZ + 1.4f), y + 2.0f, v.y + fwdZ * (frontZ + 1.4f), bodyW * 0.52f, 2.6f, 1.0f, yawDeg, 0.18f, 0.20f, 0.24f, 1f, 0, 0f, 0.55f, 40f)
                        val redOn = ((timeSec * 8f).toInt() % 2 == 0)
                        val lr = if (v.sirenOn && redOn) 1f else 0.18f
                        val lb = if (v.sirenOn && !redOn) 1f else 0.25f
                        drawMeshAt(cylMesh, v.x + rightX * 3.5f, y + 10.3f, v.y + rightZ * 3.5f, 5.8f, 1.5f, 3.4f, yawDeg, v.pitchDeg, v.rollDeg, lr, 0.14f, 0.18f, 1f, 0, 0.92f)
                        drawMeshAt(cylMesh, v.x - rightX * 3.5f, y + 10.3f, v.y - rightZ * 3.5f, 5.8f, 1.5f, 3.4f, yawDeg, v.pitchDeg, v.rollDeg, 0.14f, 0.35f, lb, 1f, 0, 0.92f)
                    }
                }
            }
        }
    }

    private fun drawDinerPavilion3D(x: Float, z: Float) {
        // Cozy 3D outdoor eating kiosk + neon sign strip + 2 patio tables with colorful umbrellas
        drawBox(x + 14f, 6.5f, z - 14f, 22f, 13f, 18f, 0f, 0.90f, 0.86f, 0.78f, 1f, 0, 0.04f, 0.22f, 20f)
        // Warm glowing order counter window
        drawBox(x + 14f, 5.2f, z - 4.5f, 16f, 4.5f, 1.2f, 0f, 1.0f, 0.78f, 0.34f, 1f, 0, 0.75f)
        // Coral-red diner awning & neon trim
        drawBox(x + 14f, 13.8f, z - 12f, 26f, 2.2f, 24f, 0f, 0.94f, 0.30f, 0.20f, 1f, 0, 0.10f, 0.35f, 28f)
        drawBox(x + 14f, 15.1f, z - 12f, 26.5f, 0.8f, 24.5f, 0f, 1.0f, 0.68f, 0.16f, 1f, 0, 0.78f)
        // Two outdoor patio tables with umbrellas
        for (tx in listOf(x - 10f, x + 4f)) {
            val tz = z + 14f
            drawMeshAt(cylMesh, tx, 2.2f, tz, 1.2f, 4.4f, 1.2f, 0f, 0f, 0f, 0.28f, 0.30f, 0.34f, 1f, 0, 0f, 0.4f, 24f)
            drawMeshAt(cylMesh, tx, 4.4f, tz, 7.2f, 0.8f, 7.2f, 0f, 0f, 0f, 0.92f, 0.88f, 0.80f, 1f, 0, 0f, 0.25f, 18f)
            drawMeshAt(cylMesh, tx, 8.2f, tz, 0.7f, 7.5f, 0.7f, 0f, 0f, 0f, 0.72f, 0.76f, 0.82f, 1f, 0, 0f, 0.6f, 32f)
            drawMeshAt(coneMesh, tx, 12.2f, tz, 11.5f, 3.2f, 11.5f, 0f, 0f, 0f, 0.98f, 0.56f, 0.18f, 1f, 0, 0.06f, 0.2f, 16f)
        }
    }

    private fun drawChopper3D(x: Float, z: Float, angleRad: Float, timeSec: Float) {
        val yawDeg = 90f - (angleRad * 180f / PI).toFloat()
        val y = 135f
        drawMeshAt(planeFuselageMesh, x, y, z, 20f, 18f, 48f, yawDeg, 0f, 0f, 0.12f, 0.16f, 0.24f, 1f, 0, 0.06f, 0.6f, 48f)
        drawMeshAt(sphereMesh, x + cos(angleRad) * 10f, y + 3f, z + sin(angleRad) * 10f, 14f, 11f, 18f, yawDeg, 0f, 0f, 0.14f, 0.28f, 0.42f, 1f, 0, 0.05f, 0.85f, 64f)
        drawMeshAt(cubeMesh, x, y + 10f, z, 86f, 1.2f, 4.0f, timeSec * 780f, 0f, 0f, 0.85f, 0.88f, 0.92f, 1f, 0, 0.15f, 0.5f, 32f)
    }

    private fun drawRealisticPalmTree(x: Float, z: Float, seed: Int) {
        val leanAngle = (seed % 7) * 0.9f
        val leanX = cos(leanAngle) * 3.5f
        val leanZ = sin(leanAngle) * 3.5f
        // 3-segment curved tapered trunk
        drawMeshAt(cylMesh, x, 9f, z, 4.6f, 18f, 4.6f, 0f, 3f, 4f, 0.46f, 0.33f, 0.20f, 1f, 0, 0f, 0.08f, 10f)
        drawMeshAt(cylMesh, x + leanX * 0.5f, 26f, z + leanZ * 0.5f, 3.8f, 18f, 3.8f, 0f, 5f, 6f, 0.48f, 0.35f, 0.22f, 1f, 0, 0f, 0.08f, 10f)
        drawMeshAt(cylMesh, x + leanX, 42f, z + leanZ, 3.1f, 16f, 3.1f, 0f, 6f, 7f, 0.50f, 0.37f, 0.24f, 1f, 0, 0f, 0.08f, 10f)

        val crownX = x + leanX * 1.3f
        val crownZ = z + leanZ * 1.3f
        // Coconut cluster
        drawMeshAt(sphereMesh, crownX + 1.4f, 48.5f, crownZ, 2.8f, 2.8f, 2.8f, 0f, 0f, 0f, 0.36f, 0.24f, 0.12f, 1f, 0, 0f, 0.1f, 12f)
        drawMeshAt(sphereMesh, crownX - 1.2f, 48.2f, crownZ + 1.2f, 2.6f, 2.6f, 2.6f, 0f, 0f, 0f, 0.38f, 0.25f, 0.13f, 1f, 0, 0f, 0.1f, 12f)

        // 8 Arched drooping tropical fronds
        for (i in 0 until 8) {
            val ang = i * 45f + (seed % 30)
            val pitch = if (i % 2 == 0) -6f else 8f
            drawMeshAt(palmFrondMesh, crownX, 49.5f, crownZ, 24f, 18f, 24f, ang, pitch, 0f, 0.16f, 0.56f, 0.20f, 1f, 0, 0f, 0.14f, 16f)
        }
    }

    private fun drawPineTree3D(x: Float, z: Float, seed: Int) {
        val scale = 0.88f + (seed % 4) * 0.12f
        drawMeshAt(cylMesh, x, 11f * scale, z, 3.4f, 22f * scale, 3.4f, 0f, 0f, 0f, 0.36f, 0.24f, 0.15f, 1f, 0, 0f, 0.06f, 10f)
        drawMeshAt(coneMesh, x, 24f * scale, z, 19f * scale, 20f * scale, 19f * scale, 0f, 0f, 0f, 0.11f, 0.36f, 0.18f, 1f, 0, 0f, 0.10f, 12f)
        drawMeshAt(coneMesh, x, 35f * scale, z, 15f * scale, 18f * scale, 15f * scale, 0f, 0f, 0f, 0.13f, 0.42f, 0.21f, 1f, 0, 0f, 0.10f, 12f)
        drawMeshAt(coneMesh, x, 45f * scale, z, 10f * scale, 14f * scale, 10f * scale, 0f, 0f, 0f, 0.16f, 0.48f, 0.24f, 1f, 0, 0f, 0.10f, 12f)
    }

    private fun drawCactus3D(x: Float, z: Float) {
        drawMeshAt(cylMesh, x, 8.5f, z, 2.8f, 17f, 2.8f, 0f, 0f, 0f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
        drawMeshAt(sphereMesh, x, 17f, z, 2.8f, 2.8f, 2.8f, 0f, 0f, 0f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
        drawMeshAt(cylMesh, x + 3.2f, 10.5f, z, 1.8f, 5.5f, 1.8f, 0f, 0f, 90f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
        drawMeshAt(cylMesh, x + 5.4f, 13.5f, z, 1.8f, 6.2f, 1.8f, 0f, 0f, 0f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
        drawMeshAt(cylMesh, x - 3.0f, 8.5f, z, 1.8f, 5.0f, 1.8f, 0f, 0f, 90f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
        drawMeshAt(cylMesh, x - 5.0f, 11.5f, z, 1.8f, 5.5f, 1.8f, 0f, 0f, 0f, 0.22f, 0.52f, 0.24f, 1f, 0, 0f, 0.12f, 14f)
    }

    private fun drawNearbyTrafficSignals(px: Float, pz: Float) {
        val isRedNS = engine.isRedTrafficLightPhase
        for (inter in engine.world.intersections) {
            val ix = inter.first
            val iz = inter.second
            if (!isNear(ix, iz, px, pz, 820f)) continue
            val poleX = ix + 60f
            val poleZ = iz + 60f
            drawMeshAt(cylMesh, poleX, 15f, poleZ, 1.8f, 30f, 1.8f, 0f, 0f, 0f, 0.24f, 0.27f, 0.32f, 1f, 0, 0f, 0.5f, 36f)
            drawMeshAt(cylMesh, poleX - 18f, 29f, poleZ, 1.3f, 36f, 1.3f, 0f, 0f, 90f, 0.24f, 0.27f, 0.32f, 1f, 0, 0f, 0.5f, 36f)
            drawBox(poleX - 18f, 27.5f, poleZ, 3.2f, 7.5f, 3.2f, 0f, 0.12f, 0.14f, 0.16f, 1f, 0, 0f, 0.2f, 18f)
            val nsR = if (isRedNS) 1.0f else 0.15f
            val nsG = if (isRedNS) 0.15f else 0.95f
            drawMeshAt(sphereMesh, poleX - 18f, 29.2f, poleZ, 2.2f, 2.2f, 3.6f, 0f, 0f, 0f, nsR, 0.16f, 0.16f, 1f, 0, if (isRedNS) 0.95f else 0.1f)
            drawMeshAt(sphereMesh, poleX - 18f, 25.8f, poleZ, 2.2f, 2.2f, 3.6f, 0f, 0f, 0f, 0.15f, nsG, 0.30f, 1f, 0, if (!isRedNS) 0.95f else 0.1f)
        }
    }

    private fun drawNearbyStreetlights(px: Float, pz: Float) {
        val world = engine.world
        for (road in world.roads) {
            if (road.isHorizontal) continue
            val r = road.rect
            val rx = r.centerX
            if (abs(rx - px) > 700f) continue
            var sy = r.top + 80f
            while (sy < r.bottom - 80f) {
                if (abs(sy - pz) < 700f && !world.isWater(rx + 58f, sy)) {
                    drawMeshAt(cylMesh, rx + 58f, 19f, sy, 2.0f, 38f, 2.0f, 0f, 0f, 0f, 0.22f, 0.25f, 0.30f, 1f, 0, 0f, 0.5f, 36f)
                    drawMeshAt(cylMesh, rx + 52f, 38f, sy, 1.4f, 12f, 1.4f, 0f, 0f, 90f, 0.22f, 0.25f, 0.30f, 1f, 0, 0f, 0.5f, 36f)
                    drawMeshAt(sphereMesh, rx + 47f, 37.5f, sy, 3.2f, 1.8f, 2.6f, 0f, 0f, 0f, 1.0f, 0.94f, 0.74f, 1f, 0, 0.90f)
                }
                sy += 360f
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun drawBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        yawDeg: Float,
        r: Float, g: Float, b: Float, a: Float,
        texId: Int, emissive: Float,
        specular: Float = 0.12f,
        shininess: Float = 16f,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f
    ) {
        drawMeshAt(
            cubeMesh, x, y, z, sx, sy, sz, yawDeg, 0f, 0f,
            r, g, b, a, texId, emissive, specular, shininess, uvScaleX, uvScaleY
        )
    }

    private fun drawMeshAt(
        mesh: Mesh,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        yawDeg: Float, pitchDeg: Float, rollDeg: Float,
        r: Float, g: Float, b: Float, a: Float,
        texId: Int, emissive: Float,
        specular: Float = 0.15f,
        shininess: Float = 20f,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f,
        uvOffsetX: Float = 0f,
        uvOffsetY: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (yawDeg != 0f) Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        if (pitchDeg != 0f) Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(shader.uColorLoc, r, g, b, a)
        GLES30.glUniform1f(shader.uEmissiveLoc, emissive)
        GLES30.glUniform1f(shader.uSpecularLoc, specular)
        GLES30.glUniform1f(shader.uShininessLoc, shininess)
        GLES30.glUniform2f(shader.uUvScaleLoc, uvScaleX, uvScaleY)
        GLES30.glUniform2f(shader.uUvOffsetLoc, uvOffsetX, uvOffsetY)

        if (texId != 0) {
            GLES30.glUniform1i(shader.uUseTextureLoc, 1)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
            GLES30.glUniform1i(shader.uTextureLoc, 0)
        } else {
            GLES30.glUniform1i(shader.uUseTextureLoc, 0)
        }
        mesh.draw()
    }

    private fun isNear(x: Float, z: Float, px: Float, pz: Float, maxDist: Float): Boolean {
        return abs(x - px) < maxDist && abs(z - pz) < maxDist
    }
}
