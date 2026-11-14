package com.example.game.engine

import androidx.compose.ui.graphics.Color
import com.example.data.PlayerSaveEntity
import com.example.game.model.ActiveMissionState
import com.example.game.model.CityWorld
import com.example.game.model.DistrictId
import com.example.game.model.FloatingPopup
import com.example.game.model.GameOverlayStatus
import com.example.game.model.HudSnapshot
import com.example.game.model.MissionDef
import com.example.game.model.MissionGoalType
import com.example.game.model.NpcEntity
import com.example.game.model.NpcRole
import com.example.game.model.ParticleEntity
import com.example.game.model.PlayerEntity
import com.example.game.model.ProjectileEntity
import com.example.game.model.ServicePointType
import com.example.game.model.SkidMark
import com.example.game.model.VehicleCategory
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleType
import com.example.game.model.DynamicProp3D
import com.example.game.model.PropType
import com.example.game.model.WeaponType
import com.example.game.physics.Physics3DEngine
import com.example.game.world.CityWorldBuilder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val world: CityWorld = CityWorldBuilder.buildWorld()
) {
    val physics = Physics3DEngine(world)
    val player = PlayerEntity()
    val vehicles = mutableListOf<VehicleEntity>()
    val npcs = mutableListOf<NpcEntity>()
    val props = mutableListOf<DynamicProp3D>()
    val projectiles = mutableListOf<ProjectileEntity>()
    val particles = mutableListOf<ParticleEntity>()
    val skidMarks = mutableListOf<SkidMark>()
    val floatingPopups = mutableListOf<FloatingPopup>()

    // Camera state (3D Third-Person Orbit + Chase)
    var cameraX: Float = 2160f
    var cameraY: Float = 2160f
    var cameraZoom: Float = 1.0f
    var cameraYawRad: Float = 0f
    var cameraPitchRad: Float = 0.22f
    var worldTimeMinutes: Float = 9 * 60 + 48f
    private var lastCameraDragEpochMs: Long = 0L

    fun rotateCamera(deltaYawRad: Float, deltaPitchRad: Float) {
        cameraYawRad = normalizeAngle(cameraYawRad + deltaYawRad)
        cameraPitchRad = (cameraPitchRad + deltaPitchRad).coerceIn(-0.12f, 0.72f)
        lastCameraDragEpochMs = System.currentTimeMillis()
    }

    // 5-Star Air Support Chopper state
    var chopperX: Float = 0f
    var chopperY: Float = 0f
    var chopperAngle: Float = 0f
    var chopperActive: Boolean = false

    // Progression & Discovery
    val discoveredDistricts = mutableSetOf(DistrictId.DOWNTOWN)
    val collectedPackages = mutableSetOf<String>()
    val completedMissions = mutableSetOf<String>()
    var bestStuntMeters: Float = 0f
    var totalDistanceMeters: Float = 0f
    var totalBustedCount: Int = 0

    // Waypoint & Banners
    var waypointX: Float? = null
    var waypointY: Float? = null
    private var districtBannerText: String? = null
    private var districtBannerTimer: Float = 0f
    private var stuntBannerText: String? = null
    private var stuntBannerTimer: Float = 0f
    private var currentStuntAirMeters: Float = 0f
    private var wasAirborneOnStunt: Boolean = false

    // Overlay & Mission state
    var overlayStatus: GameOverlayStatus = GameOverlayStatus.NONE
    var lastResultTitle: String = ""
    var lastResultRewardCash: Int = 0
    var activeMission: ActiveMissionState? = null
    private var missionCheckpoints: List<Pair<Float, Float>> = emptyList()
    private var missionStepIndex: Int = 0
    private var missionLastStationIdx: Int = -1
    private var missionStageClearDone: Boolean = false

    // Input state updated by Compose touch HUD
    var inputMoveX: Float = 0f
    var inputMoveY: Float = 0f
    var inputFiring: Boolean = false
    var inputBoosting: Boolean = false
    var autoAimEnabled: Boolean = true

    // Traffic intersection cycle
    private var trafficLightTimer: Float = 0f
    var isRedTrafficLightPhase: Boolean = false
    private var nextDynamicId: Int = 5000
    private val railLength = CityWorldBuilder.railTrackTotalLength(world.railTrack)
    private val rng = Random(System.currentTimeMillis())

    // Callbacks
    var onOpenArmoryRequested: (() -> Unit)? = null
    var onSaveRequested: (() -> Unit)? = null

    init {
        vehicles.addAll(CityWorldBuilder.spawnInitialVehicles(world))
        npcs.addAll(CityWorldBuilder.spawnInitialNpcs(world))
        props.addAll(physics.spawnInitialProps())
    }

    fun jumpPlayer() {
        physics.triggerPlayerJump(player)
    }

    fun applySave(save: PlayerSaveEntity) {
        player.cash = save.cash
        player.respectXp = save.respectXp
        player.health = save.health.coerceIn(10, 100).toFloat()
        player.armor = save.armor.coerceIn(0, 100).toFloat()
        player.x = save.playerX.coerceIn(100f, world.worldWidth - 100f)
        player.y = save.playerY.coerceIn(100f, world.worldHeight - 100f)
        player.vehicleId = null
        player.wantedLevel = 0
        player.heatScore = 0f

        player.ownedWeapons.clear()
        save.ownedWeaponsCsv.split(",").filter { it.isNotBlank() }.forEach { name ->
            runCatching { WeaponType.valueOf(name.trim()) }.getOrNull()?.let {
                player.ownedWeapons += it
            }
        }
        if (player.ownedWeapons.isEmpty()) {
            player.ownedWeapons += WeaponType.BATON
            player.ownedWeapons += WeaponType.PISTOL
        }
        player.equippedWeapon = runCatching { WeaponType.valueOf(save.equippedWeapon) }
            .getOrElse { WeaponType.PISTOL }
        if (player.equippedWeapon !in player.ownedWeapons) {
            player.equippedWeapon = player.ownedWeapons.first()
        }

        save.ammoCsv.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val wt = runCatching { WeaponType.valueOf(parts[0].trim()) }.getOrNull()
                val amt = parts[1].trim().toIntOrNull()
                if (wt != null && amt != null) {
                    player.ammoMap[wt] = amt
                }
            }
        }

        completedMissions.clear()
        save.completedMissionsCsv.split(",").filter { it.isNotBlank() }.forEach {
            completedMissions += it.trim()
        }

        discoveredDistricts.clear()
        save.discoveredDistrictsCsv.split(",").filter { it.isNotBlank() }.forEach { name ->
            runCatching { DistrictId.valueOf(name.trim()) }.getOrNull()?.let {
                discoveredDistricts += it
            }
        }
        if (discoveredDistricts.isEmpty()) discoveredDistricts += DistrictId.DOWNTOWN

        collectedPackages.clear()
        save.collectedPackagesCsv.split(",").filter { it.isNotBlank() }.forEach {
            collectedPackages += it.trim()
        }

        bestStuntMeters = save.bestStuntMeters
        totalDistanceMeters = save.totalDistanceMeters
        totalBustedCount = save.totalBustedCount
        cameraX = player.x
        cameraY = player.y
    }

    fun exportSave(): PlayerSaveEntity {
        val safePos = if (world.isWater(player.x, player.y)) {
            world.nearestRoadPoint(player.x, player.y)
        } else {
            player.x to player.y
        }
        return PlayerSaveEntity(
            id = 1,
            cash = player.cash,
            respectXp = player.respectXp,
            health = player.health.toInt().coerceIn(1, 100),
            armor = player.armor.toInt().coerceIn(0, 100),
            playerX = safePos.first,
            playerY = safePos.second,
            equippedWeapon = player.equippedWeapon.name,
            ownedWeaponsCsv = player.ownedWeapons.joinToString(",") { it.name },
            ammoCsv = WeaponType.entries.joinToString(",") { "${it.name}:${player.ammoMap[it] ?: 0}" },
            completedMissionsCsv = completedMissions.joinToString(","),
            discoveredDistrictsCsv = discoveredDistricts.joinToString(",") { it.name },
            collectedPackagesCsv = collectedPackages.joinToString(","),
            bestStuntMeters = bestStuntMeters,
            totalDistanceMeters = totalDistanceMeters,
            totalBustedCount = totalBustedCount
        )
    }

    fun step(dtRaw: Float) {
        if (overlayStatus != GameOverlayStatus.NONE) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val nowMs = System.currentTimeMillis()
        trafficLightTimer += dt
        isRedTrafficLightPhase = (trafficLightTimer.toInt() / 8) % 2 == 1
        worldTimeMinutes = (worldTimeMinutes + dt * 0.5f) % (24f * 60f)
        if (player.muzzleFlashTimerSec > 0f) {
            player.muzzleFlashTimerSec = (player.muzzleFlashTimerSec - dt).coerceAtLeast(0f)
        }

        // Decay banners & popups
        if (districtBannerTimer > 0f) {
            districtBannerTimer -= dt
            if (districtBannerTimer <= 0f) districtBannerText = null
        }
        if (stuntBannerTimer > 0f) {
            stuntBannerTimer -= dt
            if (stuntBannerTimer <= 0f) stuntBannerText = null
        }
        for (i in floatingPopups.lastIndex downTo 0) {
            floatingPopups[i].lifeSec -= dt
            if (floatingPopups[i].lifeSec <= 0f) floatingPopups.removeAt(i)
        }
        for (sp in world.servicePoints) {
            if (sp.cooldownSec > 0f) sp.cooldownSec = (sp.cooldownSec - dt).coerceAtLeast(0f)
        }

        updatePlayer(dt, nowMs)
        physics.stepPlayerGravityAndProps(player, props, vehicles, particles, dt)
        updateVehicles(dt)
        physics.resolveVehicleCollisions(
            vehicles = vehicles,
            props = props,
            particles = particles,
            dt = dt,
            onPoliceHit = { cruiser -> addCrimeHeat(0.9f, cruiser.x, cruiser.y) },
            onVehicleWrecked = { wrecked -> handleVehicleDestroyed(wrecked) },
            onBarrelDetonated = { bx, by -> triggerExplosion(bx, by, 130f, 155f) }
        )
        physics.stepProps(props, particles, dt)
        updateNpcs(dt, nowMs)
        updateProjectiles(dt)
        updateParticles(dt)
        updateWantedSystem(dt)
        updateWorldPickupsAndDistricts()
        updateActiveMission(dt)
        updateCamera(dt)
    }

    private fun updatePlayer(dt: Float, nowMs: Long) {
        val currentVehicle = player.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
        if (currentVehicle != null) {
            player.x = currentVehicle.x
            player.y = currentVehicle.y
            player.angleRad = currentVehicle.angleRad

            if (inputFiring) {
                if (currentVehicle.type == VehicleType.POLICE_CRUISER && nowMs - player.lastShotEpochMs > 350L) {
                    currentVehicle.sirenOn = !currentVehicle.sirenOn
                    player.lastShotEpochMs = nowMs
                } else {
                    firePlayerWeapon(nowMs, fromVehicle = true)
                }
            }
            return
        }

        // On-foot movement (relative to 3D cameraYawRad)
        val mag = hypot(inputMoveX.toDouble(), inputMoveY.toDouble()).toFloat().coerceIn(0f, 1f)
        if (mag > 0.08f) {
            val baseSpeed = if (inputBoosting) 225f else 150f
            val stickAngle = atan2(inputMoveX.toDouble(), (-inputMoveY).toDouble()).toFloat()
            val targetAngle = normalizeAngle(cameraYawRad + stickAngle)
            player.angleRad = if (inputFiring) cameraYawRad else targetAngle
            val nx = player.x + cos(targetAngle) * baseSpeed * mag * dt
            val ny = player.y + sin(targetAngle) * baseSpeed * mag * dt
            moveFootEntity(nx, ny, radius = 14f)
            player.walkCycle += dt * 10f * mag
            totalDistanceMeters += (baseSpeed * mag * dt) * 0.04f
        } else if (inputFiring) {
            player.angleRad = cameraYawRad
        }

        if (inputFiring) {
            firePlayerWeapon(nowMs, fromVehicle = false)
        }
    }

    private fun moveFootEntity(targetX: Float, targetY: Float, radius: Float) {
        var nextX = targetX.coerceIn(40f, world.worldWidth - 40f)
        var nextY = targetY.coerceIn(40f, world.worldHeight - 40f)

        // Prevent walking into deep ocean unless standing on pier
        if (world.isWater(nextX, nextY)) {
            if (!world.isWater(nextX, player.y)) {
                nextY = player.y
            } else if (!world.isWater(player.x, nextY)) {
                nextX = player.x
            } else {
                return
            }
        }

        // Slide cleanly along buildings
        for (b in world.buildings) {
            val r = b.rect
            if (r.contains(nextX, player.y, radius)) {
                nextX = player.x
            }
            if (r.contains(player.x, nextY, radius)) {
                nextY = player.y
            }
        }
        player.x = nextX
        player.y = nextY
    }

    fun toggleEnterOrExitVehicle() {
        val currentVid = player.vehicleId
        if (currentVid != null) {
            val v = vehicles.firstOrNull { it.id == currentVid }
            player.vehicleId = null
            if (v != null) {
                v.speed *= 0.45f
                if (v.type.category == VehicleCategory.AIR && v.altitude > 0.12f) {
                    // Parachute safe landing directly below
                    v.altitude = 0f
                    v.verticalVel = 0f
                    spawnFloatingPopup("PARACHUTE SAFE!", player.x, player.y - 30f, Color(0xFF38BDF8))
                }
                if (v.type.category == VehicleCategory.WATER) {
                    // Step onto nearest pier/land if close, otherwise hop to shoreline
                    val offsetAngles = listOf(-PI / 2, PI / 2, 0.0, PI)
                    var landed = false
                    for (a in offsetAngles) {
                        val tx = v.x + cos(v.angleRad + a).toFloat() * 52f
                        val ty = v.y + sin(v.angleRad + a).toFloat() * 52f
                        if (!world.isWater(tx, ty)) {
                            player.x = tx
                            player.y = ty
                            landed = true
                            break
                        }
                    }
                    if (!landed) {
                        val (rx, ry) = world.nearestRoadPoint(v.x, v.y)
                        player.x = rx
                        player.y = ry
                    }
                } else {
                    val sideX = v.x + cos(v.angleRad - PI / 2).toFloat() * (v.type.width + 18f)
                    val sideY = v.y + sin(v.angleRad - PI / 2).toFloat() * (v.type.width + 18f)
                    if (!isBlockedByBuilding(sideX, sideY, 12f) && !world.isWater(sideX, sideY)) {
                        player.x = sideX
                        player.y = sideY
                    } else {
                        player.x = v.x
                        player.y = v.y - 32f
                    }
                }
            }
            return
        }

        // Find nearest vehicle
        val nearby = findNearestEnterableVehicle() ?: return
        player.vehicleId = nearby.id
        nearby.isPlayerOwned = true

        // If hijacking an active traffic or police car, eject NPC & add heat!
        if (nearby.isTrafficControlled || nearby.isPoliceActive) {
            nearby.isTrafficControlled = false
            nearby.isPoliceActive = false
            npcs += NpcEntity(
                id = nextDynamicId++,
                role = if (nearby.type == VehicleType.POLICE_CRUISER) NpcRole.OFFICER else NpcRole.CIVILIAN,
                x = nearby.x + 28f,
                y = nearby.y + 28f,
                hp = 60f,
                panicTimerSec = 5f
            )
            addCrimeHeat(if (nearby.type == VehicleType.POLICE_CRUISER) 1.4f else 0.7f, nearby.x, nearby.y)
        }
        spawnFloatingPopup(nearby.type.displayName.uppercase(), nearby.x, nearby.y - 26f, Color(0xFF00E5FF))
    }

    fun findNearestEnterableVehicle(): VehicleEntity? {
        var best: VehicleEntity? = null
        var bestDist = Float.MAX_VALUE
        for (v in vehicles) {
            if (v.hp <= 0f) continue
            val maxReach = when (v.type.category) {
                VehicleCategory.RAIL -> 130f
                VehicleCategory.WATER -> 135f
                else -> 95f
            }
            val d = hypot((v.x - player.x).toDouble(), (v.y - player.y).toDouble()).toFloat()
            if (d <= maxReach && d < bestDist) {
                bestDist = d
                best = v
            }
        }
        return best
    }

    private fun updateVehicles(dt: Float) {
        val playerVehicleId = player.vehicleId
        for (i in vehicles.indices) {
            val v = vehicles[i]
            if (v.hp <= 0f) continue

            // Smoke trail when damaged
            if (v.hp < v.type.maxHp * 0.35f) {
                v.damagedSmokeTimer -= dt
                if (v.damagedSmokeTimer <= 0f) {
                    v.damagedSmokeTimer = 0.14f
                    particles += ParticleEntity(
                        x = v.x, y = v.y,
                        vx = rng.nextFloat() * 20f - 10f,
                        vy = -25f - rng.nextFloat() * 15f,
                        radius = 10f, maxLife = 0.7f, life = 0.7f,
                        color = Color(0xFF64748B)
                    )
                }
            }

            when {
                v.id == playerVehicleId -> updatePlayerDrivenVehicle(v, dt)
                v.type.category == VehicleCategory.RAIL -> updateAmbientTrain(v, dt)
                v.isPoliceActive -> updatePoliceVehicle(v, dt)
                v.isTrafficControlled -> updateTrafficVehicle(v, dt)
                v.isAmbientCruise -> updateAmbientVehicle(v, dt)
                v.missionTag == "TARGET_CAR" -> updateFleeingMissionCar(v, dt)
                else -> {
                    // Gently coast parked vehicles to stop
                    v.isBraking = abs(v.speed) > 10f
                    v.steerAngleDeg *= (1f - 6f * dt).coerceAtLeast(0f)
                    v.speed *= (1f - 3.5f * dt).coerceAtLeast(0f)
                    if (abs(v.speed) > 4f) {
                        v.x += cos(v.angleRad) * v.speed * dt
                        v.y += sin(v.angleRad) * v.speed * dt
                    }
                    if (v.altitude > 0f) {
                        v.altitude = (v.altitude - dt * 0.6f).coerceAtLeast(0f)
                    }
                }
            }
        }
    }

    private fun updatePlayerDrivenVehicle(v: VehicleEntity, dt: Float) {
        if (v.type.category == VehicleCategory.RAIL) {
            val mag = hypot(inputMoveX.toDouble(), inputMoveY.toDouble()).toFloat()
            val targetThrottle = if (mag > 0.1f) {
                val dirAngle = atan2(inputMoveY.toDouble(), inputMoveX.toDouble()).toFloat()
                val align = cos((dirAngle - v.angleRad).toDouble()).toFloat()
                if (align >= -0.2f) 1f else -0.6f
            } else 0f
            val boostMult = if (inputBoosting) 1.35f else 1f
            val desiredSpeed = targetThrottle * v.type.maxSpeed * boostMult
            v.isBraking = mag <= 0.1f && abs(v.speed) > 15f
            v.speed += (desiredSpeed - v.speed) * (2.5f * dt).coerceAtMost(1f)
            v.railTrackDistance = (v.railTrackDistance + v.speed * dt + railLength) % railLength
            val (tx, ty, tang) = CityWorldBuilder.sampleRailTrack(world.railTrack, v.railTrackDistance)
            v.x = tx
            v.y = ty
            v.angleRad = tang
            totalDistanceMeters += abs(v.speed * dt) * 0.04f
            return
        }

        val mag = hypot(inputMoveX.toDouble(), inputMoveY.toDouble()).toFloat().coerceIn(0f, 1f)
        val boostMult = if (inputBoosting) 1.32f else 1.0f

        if (mag > 0.08f) {
            val stickAngle = atan2(inputMoveX.toDouble(), (-inputMoveY).toDouble()).toFloat()
            val desiredAngle = normalizeAngle(cameraYawRad + stickAngle)
            val angleDiff = normalizeAngle(desiredAngle - v.angleRad)
            val turnAmount = v.type.turnRate * dt * (if (v.altitude > 0.15f) 0.85f else 1.0f)
            v.angleRad = normalizeAngle(v.angleRad + angleDiff.coerceIn(-turnAmount, turnAmount))

            val targetSteer = (angleDiff * 36f).coerceIn(-30f, 30f)
            v.steerAngleDeg += (targetSteer - v.steerAngleDeg) * (10f * dt).coerceAtMost(1f)
            if (v.type.category == VehicleCategory.LAND && v.altitude <= 0.03f) {
                val speedFactor = (abs(v.speed) / v.type.maxSpeed).coerceIn(0f, 1f)
                v.rollDeg += ((-angleDiff * 9f * speedFactor).coerceIn(-9f, 9f) - v.rollDeg) * (7f * dt).coerceAtMost(1f)
            }

            val targetSpeed = v.type.maxSpeed * mag * boostMult
            v.isBraking = targetSpeed < v.speed - 30f
            v.speed += (targetSpeed - v.speed) * (v.type.accel / v.type.maxSpeed * 2.2f * dt).coerceAtMost(1f)

            // Drift skid marks when turning sharp at high speed on land
            if (v.type.category == VehicleCategory.LAND && v.altitude <= 0.02f &&
                abs(angleDiff) > 0.55f && v.speed > 230f
            ) {
                skidMarks += SkidMark(
                    v.x, v.y,
                    v.x - cos(v.angleRad) * 14f,
                    v.y - sin(v.angleRad) * 14f
                )
                if (skidMarks.size > 160) skidMarks.removeAt(0)
            }
        } else {
            // Natural friction / coasting (planes maintain minimum cruise throttle unless slowing)
            v.steerAngleDeg += (0f - v.steerAngleDeg) * (8f * dt).coerceAtMost(1f)
            v.isBraking = v.type.category == VehicleCategory.LAND && abs(v.speed) > 25f
            val drag = if (v.type.category == VehicleCategory.AIR && v.altitude > 0.15f) 0.55f else 2.2f
            v.speed *= (1f - drag * dt).coerceAtLeast(0f)
        }

        // Boost exhaust sparks
        if (inputBoosting && v.speed > 120f && rng.nextFloat() < 0.5f) {
            particles += ParticleEntity(
                x = v.x - cos(v.angleRad) * (v.type.length * 0.5f),
                y = v.y - sin(v.angleRad) * (v.type.length * 0.5f),
                vx = -cos(v.angleRad) * 90f,
                vy = -sin(v.angleRad) * 90f,
                radius = 5f, maxLife = 0.25f, life = 0.25f,
                color = if (v.type.category == VehicleCategory.AIR) Color(0xFF38BDF8) else Color(0xFFFFB300)
            )
        }

        // Boat foam wake
        if (v.type.category == VehicleCategory.WATER && v.speed > 75f && rng.nextFloat() < 0.55f) {
            particles += ParticleEntity(
                x = v.x - cos(v.angleRad) * (v.type.length * 0.45f),
                y = v.y - sin(v.angleRad) * (v.type.length * 0.45f),
                vx = rng.nextFloat() * 20f - 10f,
                vy = rng.nextFloat() * 20f - 10f,
                radius = 8f, maxLife = 0.55f, life = 0.55f,
                color = Color(0xFFBAE6FD)
            )
        }

        // Aircraft takeoff & landing physics
        if (v.type.category == VehicleCategory.AIR) {
            if (v.speed > 240f) {
                v.altitude = (v.altitude + dt * 0.42f).coerceAtMost(1.0f)
            } else if (v.altitude > 0f) {
                v.altitude = (v.altitude - dt * 0.35f).coerceAtLeast(0f)
            }
        } else if (v.type.category == VehicleCategory.LAND) {
            // Check stunt ramps!
            if (v.altitude <= 0.01f && v.speed > 250f) {
                for (ramp in world.stuntRamps) {
                    if (ramp.rect.contains(v.x, v.y)) {
                        v.verticalVel = 0.9f
                        v.altitude = 0.05f
                        wasAirborneOnStunt = true
                        currentStuntAirMeters = 0f
                        break
                    }
                }
            }
            if (v.altitude > 0f || v.verticalVel > 0f) {
                v.verticalVel -= 1.45f * dt
                v.altitude += v.verticalVel * dt
                currentStuntAirMeters += v.speed * dt * 0.045f
                if (v.altitude <= 0f) {
                    v.altitude = 0f
                    v.verticalVel = 0f
                    if (wasAirborneOnStunt && currentStuntAirMeters > 4f) {
                        val bonus = (currentStuntAirMeters * 25).toInt().coerceAtLeast(100)
                        player.cash += bonus
                        player.respectXp += 40
                        if (currentStuntAirMeters > bestStuntMeters) {
                            bestStuntMeters = currentStuntAirMeters
                        }
                        stuntBannerText = "INSANE STUNT! ${currentStuntAirMeters.toInt()}m (+$$bonus)"
                        stuntBannerTimer = 3.2f
                        onSaveRequested?.invoke()
                    }
                    wasAirborneOnStunt = false
                }
            }
        }

        // Apply movement & collisions
        val prevX = v.x
        val prevY = v.y
        val nextX = (v.x + cos(v.angleRad) * v.speed * dt).coerceIn(40f, world.worldWidth - 40f)
        val nextY = (v.y + sin(v.angleRad) * v.speed * dt).coerceIn(40f, world.worldHeight - 40f)
        totalDistanceMeters += hypot((nextX - prevX).toDouble(), (nextY - prevY).toDouble()).toFloat() * 0.04f

        when (v.type.category) {
            VehicleCategory.AIR -> {
                if (v.altitude > 0.14f) {
                    // Flies over all buildings and water!
                    v.x = nextX
                    v.y = nextY
                } else {
                    if (!isBlockedByBuilding(nextX, nextY, 24f)) {
                        v.x = nextX
                        v.y = nextY
                    } else {
                        v.speed *= 0.4f
                    }
                }
            }
            VehicleCategory.WATER -> {
                if (world.isWater(nextX, nextY)) {
                    v.x = nextX
                    v.y = nextY
                } else {
                    v.speed *= 0.3f
                }
            }
            VehicleCategory.LAND -> {
                var blocked = false
                if (v.altitude < 0.18f && isBlockedByBuilding(nextX, nextY, v.type.width * 0.5f)) {
                    blocked = true
                }
                if (v.altitude < 0.05f && world.isWater(nextX, nextY)) {
                    blocked = true
                }
                if (!blocked) {
                    v.x = nextX
                    v.y = nextY
                } else {
                    // Slide or bounce softly
                    if (!isBlockedByBuilding(nextX, v.y, v.type.width * 0.5f) && !world.isWater(nextX, v.y)) {
                        v.x = nextX
                        v.speed *= 0.65f
                    } else if (!isBlockedByBuilding(v.x, nextY, v.type.width * 0.5f) && !world.isWater(v.x, nextY)) {
                        v.y = nextY
                        v.speed *= 0.65f
                    } else {
                        v.speed *= -0.25f
                    }
                }
                handleVehicleRamming(v)
            }
            VehicleCategory.RAIL -> Unit
        }
    }

    private fun handleVehicleRamming(playerCar: VehicleEntity) {
        if (playerCar.altitude > 0.15f || abs(playerCar.speed) < 90f) return
        // Ram NPCs (vehicle-to-vehicle collisions are handled by Physics3DEngine)
        for (i in npcs.lastIndex downTo 0) {
            val npc = npcs[i]
            val d = hypot((npc.x - playerCar.x).toDouble(), (npc.y - playerCar.y).toDouble()).toFloat()
            if (d < 26f) {
                npc.hp -= abs(playerCar.speed) * 0.45f
                npc.panicTimerSec = 5f
                npc.x += cos(playerCar.angleRad) * 20f
                npc.y += sin(playerCar.angleRad) * 20f
                npc.vz = 42f
                spawnExplosionSparks(npc.x, npc.y, 4, Color(0xFF38BDF8))
                addCrimeHeat(if (npc.role == NpcRole.OFFICER) 1.5f else 0.7f, npc.x, npc.y)
                if (npc.hp <= 0f) {
                    onNpcKnockedOut(i)
                }
            }
        }
    }

    private fun updateAmbientTrain(v: VehicleEntity, dt: Float) {
        if (v.railStopTimerSec > 0f) {
            v.railStopTimerSec -= dt
            return
        }
        v.speed = 195f
        v.railTrackDistance = (v.railTrackDistance + v.speed * dt) % railLength
        val (tx, ty, tang) = CityWorldBuilder.sampleRailTrack(world.railTrack, v.railTrackDistance)
        v.x = tx
        v.y = ty
        v.angleRad = tang

        // Brief station stop check
        for (st in world.metroStations) {
            if (hypot((v.x - st.x).toDouble(), (v.y - st.y).toDouble()) < 16f && rng.nextFloat() < 0.08f) {
                v.railStopTimerSec = 2.0f
                break
            }
        }
    }

    private fun updateAmbientVehicle(v: VehicleEntity, dt: Float) {
        if (v.type.category == VehicleCategory.AIR) {
            // Wide scenic airliner/jet flyover orbit across the 5-island skies
            v.angleRad = normalizeAngle(v.angleRad + 0.055f * dt)
            val nx = v.x + cos(v.angleRad) * v.speed * dt
            val ny = v.y + sin(v.angleRad) * v.speed * dt
            if (nx < 300f || nx > world.worldWidth - 300f || ny < 300f || ny > world.worldHeight - 300f) {
                val centerAng = atan2((world.worldHeight * 0.5f - v.y).toDouble(), (world.worldWidth * 0.5f - v.x).toDouble()).toFloat()
                v.angleRad = centerAng
            } else {
                v.x = nx
                v.y = ny
            }
            v.rollDeg = -11f
        } else if (v.type.category == VehicleCategory.WATER) {
            val nx = v.x + cos(v.angleRad) * v.speed * dt
            val ny = v.y + sin(v.angleRad) * v.speed * dt
            if (nx in 120f..(world.worldWidth - 120f) && ny in 120f..(world.worldHeight - 120f) &&
                world.isWater(nx + cos(v.angleRad) * 90f, ny + sin(v.angleRad) * 90f)
            ) {
                v.x = nx
                v.y = ny
            } else {
                v.angleRad = normalizeAngle(v.angleRad + 1.35f * dt)
            }
        }
    }

    private fun updateTrafficVehicle(v: VehicleEntity, dt: Float) {
        if (world.trafficLanes.isEmpty()) return
        val lane = world.trafficLanes[v.laneIndex % world.trafficLanes.size]
        val dx = lane.endX - lane.startX
        val dy = lane.endY - lane.startY
        val laneLen = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val dirX = dx / laneLen
        val dirY = dy / laneLen

        if (v.trafficPanicTimerSec > 0f) {
            v.trafficPanicTimerSec = (v.trafficPanicTimerSec - dt).coerceAtLeast(0f)
        }

        val isPanicked = v.trafficPanicTimerSec > 0f
        val isNS = abs(dirX) < 0.5f
        val lightRed = if (isNS) isRedTrafficLightPhase else !isRedTrafficLightPhase
        var desiredSpeed = if (isPanicked) v.desiredTrafficSpeed * 1.48f else v.desiredTrafficSpeed

        if (!isPanicked && lightRed && isNearIntersection(v.x, v.y)) {
            desiredSpeed = 0f
        }

        // Yield for active police sirens, player, vehicles ahead, or crossing pedestrians
        val aheadX = v.x + dirX * 76f
        val aheadY = v.y + dirY * 76f
        if (!isPanicked && hypot((aheadX - player.x).toDouble(), (aheadY - player.y).toDouble()) < 58f) {
            desiredSpeed = 0f
        } else {
            for (other in vehicles) {
                if (other.id == v.id || other.hp <= 0f || other.altitude > 0.15f) continue
                val dAhead = hypot((aheadX - other.x).toDouble(), (aheadY - other.y).toDouble()).toFloat()
                if (dAhead < 54f) {
                    desiredSpeed = 0f
                    break
                }
                if (other.isPoliceActive && other.sirenOn &&
                    hypot((v.x - other.x).toDouble(), (v.y - other.y).toDouble()) < 165f
                ) {
                    desiredSpeed = 0f
                    break
                }
            }
            if (desiredSpeed > 0f && !isPanicked) {
                for (npc in npcs) {
                    if (hypot((aheadX - npc.x).toDouble(), (aheadY - npc.y).toDouble()) < 34f) {
                        desiredSpeed = 0f
                        break
                    }
                }
            }
        }

        v.isBraking = desiredSpeed < v.speed - 12f || (v.speed < 8f && desiredSpeed <= 1f)
        v.steerAngleDeg += (0f - v.steerAngleDeg) * (8f * dt).coerceAtMost(1f)
        val accelRate = if (desiredSpeed < v.speed) 6.0f else 3.2f
        v.speed += (desiredSpeed - v.speed) * (accelRate * dt).coerceAtMost(1f)
        v.x += dirX * v.speed * dt
        v.y += dirY * v.speed * dt
        v.angleRad = lane.angleRad

        // Wrap cleanly at end of avenue
        val distToEnd = hypot((lane.endX - v.x).toDouble(), (lane.endY - v.y).toDouble()).toFloat()
        if (distToEnd < 40f) {
            v.x = lane.startX
            v.y = lane.startY
        }
    }

    private fun isNearIntersection(x: Float, y: Float): Boolean {
        for (i in world.intersections.indices) {
            val inter = world.intersections[i]
            if (abs(x - inter.first) < 88f && abs(y - inter.second) < 88f) return true
        }
        return false
    }

    private fun updatePoliceVehicle(v: VehicleEntity, dt: Float) {
        if (player.wantedLevel <= 0) {
            v.sirenOn = false
            v.speed *= 0.9f
            return
        }
        v.sirenOn = true
        val dx = player.x - v.x
        val dy = player.y - v.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val desiredAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val diff = normalizeAngle(desiredAngle - v.angleRad)
        v.angleRad = normalizeAngle(v.angleRad + diff.coerceIn(-v.type.turnRate * dt, v.type.turnRate * dt))

        val targetSpeed = when {
            dist > 110f -> v.type.maxSpeed * (0.65f + player.wantedLevel * 0.06f)
            else -> 120f
        }
        v.speed += (targetSpeed - v.speed) * (3.0f * dt).coerceAtMost(1f)

        val nx = v.x + cos(v.angleRad) * v.speed * dt
        val ny = v.y + sin(v.angleRad) * v.speed * dt
        if (!isBlockedByBuilding(nx, ny, 18f) && !world.isWater(nx, ny)) {
            v.x = nx
            v.y = ny
        } else {
            v.angleRad += 1.2f * dt
        }

        // Busted / collision check
        if (dist < 42f && player.vehicleId == null) {
            damagePlayer(8f * dt * 10f)
        } else if (dist < 52f && player.vehicleId != null) {
            val pCar = vehicles.firstOrNull { it.id == player.vehicleId }
            if (pCar != null && pCar.altitude < 0.1f) {
                pCar.hp -= 12f * dt
            }
        }
    }

    private fun updateFleeingMissionCar(v: VehicleEntity, dt: Float) {
        val dx = v.x - player.x
        val dy = v.y - player.y
        val escapeAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val diff = normalizeAngle(escapeAngle - v.angleRad)
        v.angleRad = normalizeAngle(v.angleRad + diff.coerceIn(-2.2f * dt, 2.2f * dt))
        v.speed = v.type.maxSpeed * 0.62f
        val nx = (v.x + cos(v.angleRad) * v.speed * dt).coerceIn(180f, world.worldWidth - 180f)
        val ny = (v.y + sin(v.angleRad) * v.speed * dt).coerceIn(240f, world.worldHeight - 180f)
        if (!isBlockedByBuilding(nx, ny, 20f) && !world.isWater(nx, ny)) {
            v.x = nx
            v.y = ny
        } else {
            v.angleRad += 1.5f * dt
        }
    }

    private fun updateNpcs(dt: Float, nowMs: Long) {
        for (i in npcs.lastIndex downTo 0) {
            val npc = npcs[i]
            physics.stepNpcGravity(npc, props, vehicles, dt)
            val distToPlayer = hypot((player.x - npc.x).toDouble(), (player.y - npc.y).toDouble()).toFloat()

            when (npc.role) {
                NpcRole.CIVILIAN -> {
                    if (npc.panicTimerSec > 0f) {
                        npc.panicTimerSec -= dt
                        npc.behaviorState = com.example.game.model.NpcBehaviorState.FLEEING
                        val fleeAngle = atan2((npc.y - player.y).toDouble(), (npc.x - player.x).toDouble()).toFloat()
                        npc.angleRad = fleeAngle
                        val nx = npc.x + cos(fleeAngle) * 148f * dt
                        val ny = npc.y + sin(fleeAngle) * 148f * dt
                        if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                            npc.x = nx
                            npc.y = ny
                        }
                        npc.walkPhase += dt * 11f
                    } else {
                        // Check for fast oncoming vehicles so pedestrians sidestep realistically
                        var dangerCar: VehicleEntity? = null
                        for (v in vehicles) {
                            if (v.hp <= 0f || v.type.category != VehicleCategory.LAND || v.altitude > 0.1f) continue
                            if (abs(v.speed) < 50f) continue
                            val dCar = hypot((v.x - npc.x).toDouble(), (v.y - npc.y).toDouble()).toFloat()
                            if (dCar < 115f) {
                                val toNpcX = npc.x - v.x
                                val toNpcY = npc.y - v.y
                                val dot = toNpcX * cos(v.angleRad) + toNpcY * sin(v.angleRad)
                                if (dot > 0f) {
                                    dangerCar = v
                                    break
                                }
                            }
                        }
                        if (dangerCar != null) {
                            npc.behaviorState = com.example.game.model.NpcBehaviorState.DODGING_TRAFFIC
                            val sideSign = if ((npc.x - dangerCar.x) * (-sin(dangerCar.angleRad)) +
                                (npc.y - dangerCar.y) * cos(dangerCar.angleRad) >= 0f
                            ) 1f else -1f
                            val dodgeAngle = dangerCar.angleRad + sideSign * (PI.toFloat() * 0.5f)
                            npc.angleRad = dodgeAngle
                            val nx = npc.x + cos(dodgeAngle) * 120f * dt
                            val ny = npc.y + sin(dodgeAngle) * 120f * dt
                            if (!isBlockedByBuilding(nx, ny, 9f) && !world.isWater(nx, ny)) {
                                npc.x = nx
                                npc.y = ny
                            }
                            npc.walkPhase += dt * 9.5f
                        } else {
                            npc.stateTimerSec -= dt
                            if (npc.stateTimerSec <= 0f) {
                                if (npc.behaviorState == com.example.game.model.NpcBehaviorState.LOITERING) {
                                    npc.behaviorState = com.example.game.model.NpcBehaviorState.WALKING
                                    npc.stateTimerSec = 5f + rng.nextFloat() * 7f
                                } else {
                                    npc.behaviorState = com.example.game.model.NpcBehaviorState.LOITERING
                                    npc.stateTimerSec = 2f + rng.nextFloat() * 4f
                                }
                            }
                            if (npc.behaviorState == com.example.game.model.NpcBehaviorState.LOITERING) {
                                // Subtle idle look around while standing outside shops/diners
                                npc.angleRad = normalizeAngle(npc.angleRad + sin(nowMs * 0.001 + npc.id).toFloat() * 0.25f * dt)
                            } else if (world.sidewalkWalkNodes.isNotEmpty()) {
                                val (tx, ty) = world.sidewalkWalkNodes[npc.targetNodeIdx % world.sidewalkWalkNodes.size]
                                val d = hypot((tx - npc.x).toDouble(), (ty - npc.y).toDouble()).toFloat()
                                if (d < 24f || d > 950f) {
                                    var picked = (npc.targetNodeIdx + 1) % world.sidewalkWalkNodes.size
                                    for (step in 1..12) {
                                        val candIdx = (npc.targetNodeIdx + step * 2) % world.sidewalkWalkNodes.size
                                        val cand = world.sidewalkWalkNodes[candIdx]
                                        val cd = hypot((cand.first - npc.x).toDouble(), (cand.second - npc.y).toDouble()).toFloat()
                                        if (cd in 35f..880f && !world.isWater(cand.first, cand.second)) {
                                            picked = candIdx
                                            break
                                        }
                                    }
                                    npc.targetNodeIdx = picked
                                } else {
                                    val ang = atan2((ty - npc.y).toDouble(), (tx - npc.x).toDouble()).toFloat()
                                    npc.angleRad = ang
                                    val nx = npc.x + cos(ang) * 52f * dt
                                    val ny = npc.y + sin(ang) * 52f * dt
                                    if (!isBlockedByBuilding(nx, ny, 8f) && !world.isWater(nx, ny)) {
                                        npc.x = nx
                                        npc.y = ny
                                    } else {
                                        npc.targetNodeIdx = (npc.targetNodeIdx + 2) % world.sidewalkWalkNodes.size
                                    }
                                }
                                npc.walkPhase += dt * 6f
                            }
                        }
                    }
                }
                NpcRole.OFFICER, NpcRole.SWAT -> {
                    if (player.wantedLevel > 0 && distToPlayer < 650f) {
                        npc.behaviorState = com.example.game.model.NpcBehaviorState.COMBAT
                        val ang = atan2((player.y - npc.y).toDouble(), (player.x - npc.x).toDouble()).toFloat()
                        npc.angleRad = ang
                        val speed = if (npc.role == NpcRole.SWAT) 128f else 110f
                        if (distToPlayer > 145f) {
                            val nx = npc.x + cos(ang) * speed * dt
                            val ny = npc.y + sin(ang) * speed * dt
                            if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                                npc.x = nx
                                npc.y = ny
                            }
                        } else {
                            // Tactical combat strafe around target
                            npc.stateTimerSec -= dt
                            if (npc.stateTimerSec <= 0f) {
                                npc.strafeDir = -npc.strafeDir
                                npc.stateTimerSec = 1.6f + rng.nextFloat() * 1.4f
                            }
                            val strafeAng = ang + npc.strafeDir * (PI.toFloat() * 0.5f)
                            val nx = npc.x + cos(strafeAng) * 65f * dt
                            val ny = npc.y + sin(strafeAng) * 65f * dt
                            if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                                npc.x = nx
                                npc.y = ny
                            }
                        }
                        npc.walkPhase += dt * 8f
                        if (distToPlayer < 340f && nowMs - npc.lastAttackEpochMs > 900L) {
                            npc.lastAttackEpochMs = nowMs
                            projectiles += ProjectileEntity(
                                x = npc.x, y = npc.y,
                                vx = cos(ang) * 700f,
                                vy = sin(ang) * 700f,
                                damage = if (npc.role == NpcRole.SWAT) 11f else 7f,
                                remainingDist = 370f,
                                isFromPlayer = false,
                                explosiveRadius = 0f,
                                knockback = 30f,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    } else {
                        // Calm foot beat patrol
                        npc.behaviorState = com.example.game.model.NpcBehaviorState.WALKING
                        npc.stateTimerSec -= dt
                        if (npc.stateTimerSec <= 0f) {
                            npc.angleRad = normalizeAngle(npc.angleRad + (rng.nextFloat() - 0.5f) * 2.2f)
                            npc.stateTimerSec = 3f + rng.nextFloat() * 4f
                        }
                        val nx = npc.x + cos(npc.angleRad) * 38f * dt
                        val ny = npc.y + sin(npc.angleRad) * 38f * dt
                        if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                            npc.x = nx
                            npc.y = ny
                        } else {
                            npc.angleRad = normalizeAngle(npc.angleRad + 2.4f)
                        }
                        npc.walkPhase += dt * 4.5f
                    }
                }
                NpcRole.RIVAL -> {
                    if (distToPlayer < 400f) {
                        npc.behaviorState = com.example.game.model.NpcBehaviorState.COMBAT
                        val ang = atan2((player.y - npc.y).toDouble(), (player.x - npc.x).toDouble()).toFloat()
                        npc.angleRad = ang
                        if (distToPlayer > 135f) {
                            val nx = npc.x + cos(ang) * 102f * dt
                            val ny = npc.y + sin(ang) * 102f * dt
                            if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                                npc.x = nx
                                npc.y = ny
                            }
                        } else {
                            npc.stateTimerSec -= dt
                            if (npc.stateTimerSec <= 0f) {
                                npc.strafeDir = -npc.strafeDir
                                npc.stateTimerSec = 1.4f + rng.nextFloat() * 1.4f
                            }
                            val strafeAng = ang + npc.strafeDir * (PI.toFloat() * 0.5f)
                            val nx = npc.x + cos(strafeAng) * 62f * dt
                            val ny = npc.y + sin(strafeAng) * 62f * dt
                            if (!isBlockedByBuilding(nx, ny, 10f) && !world.isWater(nx, ny)) {
                                npc.x = nx
                                npc.y = ny
                            }
                        }
                        npc.walkPhase += dt * 7.5f
                        if (nowMs - npc.lastAttackEpochMs > 1000L) {
                            npc.lastAttackEpochMs = nowMs
                            projectiles += ProjectileEntity(
                                x = npc.x, y = npc.y,
                                vx = cos(ang) * 650f,
                                vy = sin(ang) * 650f,
                                damage = 8f,
                                remainingDist = 350f,
                                isFromPlayer = false,
                                explosiveRadius = 0f,
                                knockback = 30f,
                                color = Color(0xFFFF4D6D)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun firePlayerWeapon(nowMs: Long, fromVehicle: Boolean) {
        val weapon = if (fromVehicle && player.equippedWeapon == WeaponType.BATON) {
            WeaponType.PISTOL
        } else {
            player.equippedWeapon
        }
        if (nowMs - player.lastShotEpochMs < weapon.fireRateMs) return

        if (weapon != WeaponType.BATON) {
            val ammo = player.ammoMap[weapon] ?: 0
            if (ammo <= 0) {
                player.equippedWeapon = WeaponType.BATON
                spawnFloatingPopup("OUT OF AMMO!", player.x, player.y - 30f, Color(0xFFFFB300))
                return
            }
            player.ammoMap[weapon] = ammo - 1
        }
        player.lastShotEpochMs = nowMs

        // Determine aim angle (aims straight down 3D camera reticle, with optional smart auto-aim)
        var aimAngle = if (fromVehicle) player.angleRad else cameraYawRad
        if (autoAimEnabled) {
            var bestTargetDist = weapon.range + 40f
            for (npc in npcs) {
                val priority = npc.role != NpcRole.CIVILIAN || npc.missionTag != null
                if (!priority) continue
                val d = hypot((npc.x - player.x).toDouble(), (npc.y - player.y).toDouble()).toFloat()
                if (d < bestTargetDist) {
                    bestTargetDist = d
                    aimAngle = atan2((npc.y - player.y).toDouble(), (npc.x - player.x).toDouble()).toFloat()
                }
            }
            if (bestTargetDist > weapon.range + 20f) {
                for (v in vehicles) {
                    if (v.id == player.vehicleId || v.hp <= 0f) continue
                    if (v.missionTag == "TARGET_CAR" || v.isPoliceActive) {
                        val d = hypot((v.x - player.x).toDouble(), (v.y - player.y).toDouble()).toFloat()
                        if (d < bestTargetDist) {
                            bestTargetDist = d
                            aimAngle = atan2((v.y - player.y).toDouble(), (v.x - player.x).toDouble()).toFloat()
                        }
                    }
                }
            }
        }
        player.angleRad = aimAngle

        // Panic nearby civilians & traffic drivers
        for (npc in npcs) {
            if (npc.role == NpcRole.CIVILIAN &&
                hypot((npc.x - player.x).toDouble(), (npc.y - player.y).toDouble()) < 260f
            ) {
                npc.panicTimerSec = 5f
            }
        }
        if (weapon != WeaponType.BATON) {
            player.muzzleFlashTimerSec = 0.07f
            for (v in vehicles) {
                if (v.isTrafficControlled &&
                    hypot((v.x - player.x).toDouble(), (v.y - player.y).toDouble()) < 360f
                ) {
                    v.trafficPanicTimerSec = 4.5f
                }
            }
        }

        if (weapon == WeaponType.BATON) {
            // Melee arc sweep
            spawnExplosionSparks(
                player.x + cos(aimAngle) * 28f,
                player.y + sin(aimAngle) * 28f,
                4, weapon.accentColor
            )
            for (i in npcs.lastIndex downTo 0) {
                val npc = npcs[i]
                val d = hypot((npc.x - player.x).toDouble(), (npc.y - player.y).toDouble()).toFloat()
                if (d <= weapon.range) {
                    npc.hp -= weapon.damage
                    npc.x += cos(aimAngle) * 18f
                    npc.y += sin(aimAngle) * 18f
                    addCrimeHeat(if (npc.role == NpcRole.OFFICER) 1.2f else 0.4f, npc.x, npc.y)
                    if (npc.hp <= 0f) onNpcKnockedOut(i)
                }
            }
            return
        }

        // Spawn projectile pellets
        repeat(weapon.pellets) {
            val spread = (rng.nextFloat() - 0.5f) * weapon.spreadRad
            val angle = aimAngle + spread
            projectiles += ProjectileEntity(
                x = player.x + cos(angle) * 22f,
                y = player.y + sin(angle) * 22f,
                vx = cos(angle) * weapon.projectileSpeed,
                vy = sin(angle) * weapon.projectileSpeed,
                damage = weapon.damage,
                remainingDist = weapon.range,
                isFromPlayer = true,
                explosiveRadius = weapon.explosiveRadius,
                knockback = weapon.knockback,
                color = weapon.accentColor
            )
        }
        addCrimeHeat(0.18f, player.x, player.y)
    }

    private fun updateProjectiles(dt: Float) {
        for (i in projectiles.lastIndex downTo 0) {
            val p = projectiles[i]
            val stepX = p.vx * dt
            val stepY = p.vy * dt
            p.x += stepX
            p.y += stepY
            p.remainingDist -= hypot(stepX.toDouble(), stepY.toDouble()).toFloat()

            var hit = p.remainingDist <= 0f || isBlockedByBuilding(p.x, p.y, 2f)

            if (!hit && p.isFromPlayer) {
                // Check NPC hits
                for (nIdx in npcs.lastIndex downTo 0) {
                    val npc = npcs[nIdx]
                    if (hypot((npc.x - p.x).toDouble(), (npc.y - p.y).toDouble()) < 18f) {
                        hit = true
                        if (p.explosiveRadius <= 0f) {
                            npc.hp -= p.damage
                            npc.panicTimerSec = 5f
                            spawnExplosionSparks(p.x, p.y, 4, p.color)
                            addCrimeHeat(if (npc.role == NpcRole.OFFICER) 1.0f else 0.45f, npc.x, npc.y)
                            if (npc.hp <= 0f) onNpcKnockedOut(nIdx)
                        }
                        break
                    }
                }
                // Check Vehicle hits
                if (!hit) {
                    for (v in vehicles) {
                        if (v.id == player.vehicleId || v.hp <= 0f) continue
                        if (hypot((v.x - p.x).toDouble(), (v.y - p.y).toDouble()) < v.type.width * 0.85f) {
                            hit = true
                            if (p.explosiveRadius <= 0f) {
                                v.hp -= p.damage
                                spawnExplosionSparks(p.x, p.y, 4, Color(0xFFFFB300))
                                if (v.type == VehicleType.POLICE_CRUISER || v.isPoliceActive) {
                                    addCrimeHeat(0.6f, v.x, v.y)
                                }
                                if (v.hp <= 0f) handleVehicleDestroyed(v)
                            }
                            break
                        }
                    }
                }
                // Check Dynamic World Props hits
                if (!hit) {
                    for (prIdx in props.lastIndex downTo 0) {
                        val pr = props[prIdx]
                        if (hypot((pr.x - p.x).toDouble(), (pr.y - p.y).toDouble()) < pr.type.radius + 5f) {
                            hit = true
                            val pDirX = p.vx / hypot(p.vx.toDouble(), p.vy.toDouble()).toFloat().coerceAtLeast(1f)
                            val pDirY = p.vy / hypot(p.vx.toDouble(), p.vy.toDouble()).toFloat().coerceAtLeast(1f)
                            pr.vx += pDirX * p.knockback * 0.9f
                            pr.vy += pDirY * p.knockback * 0.9f
                            pr.vz += 35f
                            pr.isGrounded = false
                            pr.angVel += 280f
                            pr.hp -= p.damage
                            spawnExplosionSparks(p.x, p.y, 4, Color(0xFFFFB300))
                            if (pr.type == PropType.HYDRANT && !pr.triggered) {
                                pr.triggered = true
                                pr.geyserTimerSec = 7f
                            }
                            if (pr.type == PropType.HAZARD_BARREL && pr.hp <= 0f) {
                                val bx = pr.x
                                val by = pr.y
                                props.removeAt(prIdx)
                                triggerExplosion(bx, by, 130f, 155f)
                            }
                            break
                        }
                    }
                }
            } else if (!hit && !p.isFromPlayer) {
                val hitDist = if (player.vehicleId != null) 28f else 15f
                if (hypot((player.x - p.x).toDouble(), (player.y - p.y).toDouble()) < hitDist) {
                    hit = true
                    val pCar = player.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
                    if (pCar != null) {
                        pCar.hp -= p.damage * 0.65f
                    } else {
                        damagePlayer(p.damage)
                    }
                    spawnExplosionSparks(p.x, p.y, 3, Color(0xFFFF4D6D))
                }
            }

            if (hit) {
                if (p.explosiveRadius > 0f) {
                    triggerExplosion(p.x, p.y, p.explosiveRadius, p.damage)
                }
                projectiles.removeAt(i)
            }
        }
    }

    private fun triggerExplosion(ex: Float, ey: Float, radius: Float, baseDamage: Float) {
        spawnExplosionSparks(ex, ey, 16, Color(0xFFFF6D00))
        addCrimeHeat(1.8f, ex, ey)
        physics.applyExplosionImpulse(ex, ey, radius, 380f, props, vehicles, player)
        for (nIdx in npcs.lastIndex downTo 0) {
            val npc = npcs[nIdx]
            val d = hypot((npc.x - ex).toDouble(), (npc.y - ey).toDouble()).toFloat()
            if (d < radius) {
                npc.hp -= baseDamage * (1f - d / radius)
                npc.vz = 55f
                if (npc.hp <= 0f) onNpcKnockedOut(nIdx)
            }
        }
        for (v in vehicles) {
            if (v.hp <= 0f || v.type.category == VehicleCategory.RAIL) continue
            val d = hypot((v.x - ex).toDouble(), (v.y - ey).toDouble()).toFloat()
            if (d < radius) {
                v.hp -= baseDamage * (1f - d / radius)
                if (v.hp <= 0f) handleVehicleDestroyed(v)
            }
        }
    }

    private fun handleVehicleDestroyed(v: VehicleEntity) {
        spawnExplosionSparks(v.x, v.y, 14, Color(0xFFFFB300))
        player.cash += 120
        player.respectXp += 35
        spawnFloatingPopup("+$120 WRECK", v.x, v.y - 20f, Color(0xFF00E676))
        if (v.id == player.vehicleId) {
            player.vehicleId = null
            damagePlayer(30f)
        }
    }

    private fun onNpcKnockedOut(index: Int) {
        val npc = npcs[index]
        val bonus = when (npc.role) {
            NpcRole.RIVAL -> 95
            NpcRole.OFFICER, NpcRole.SWAT -> 60
            NpcRole.CIVILIAN -> 35
        }
        player.cash += bonus
        player.respectXp += 20
        spawnFloatingPopup("+$bonus", npc.x, npc.y - 16f, Color(0xFF00E676))
        npcs.removeAt(index)
    }

    private fun damagePlayer(amount: Float) {
        var remaining = amount
        if (player.armor > 0f) {
            val absorbed = remaining.coerceAtMost(player.armor)
            player.armor -= absorbed
            remaining -= absorbed * 0.75f
        }
        player.health -= remaining
        if (player.health <= 0f) {
            player.health = 0f
            player.vehicleId = null
            if (player.wantedLevel > 0) {
                totalBustedCount++
                overlayStatus = GameOverlayStatus.BUSTED
                lastResultTitle = "BUSTED BY METRO PD"
            } else {
                overlayStatus = GameOverlayStatus.KNOCKED_OUT
                lastResultTitle = "KNOCKED OUT"
            }
            if (activeMission != null) {
                activeMission = null
            }
            onSaveRequested?.invoke()
        }
    }

    fun respawnAfterKnockout() {
        val fine = (player.cash * 0.08f).toInt().coerceIn(0, 250)
        player.cash = (player.cash - fine).coerceAtLeast(0)
        player.health = 100f
        player.armor = 25f
        player.wantedLevel = 0
        player.heatScore = 0f
        val (respawnX, respawnY) = when {
            player.y >= 9600f -> 5600f to 11320f
            player.x >= 9300f && player.y >= 4800f -> 10540f to 6700f
            player.x > 4600f && player.y < 4800f -> 6160f to 2220f
            player.y >= 4800f -> 2380f to 7480f
            else -> 2160f to 2160f
        }
        player.x = respawnX
        player.y = respawnY
        player.vehicleId = null
        // Clear active police
        vehicles.removeAll { it.isPoliceActive }
        npcs.removeAll { it.role == NpcRole.OFFICER || it.role == NpcRole.SWAT }
        overlayStatus = GameOverlayStatus.NONE
        onSaveRequested?.invoke()
    }

    private fun addCrimeHeat(amount: Float, cx: Float, cy: Float) {
        // Check if any witness or police is within 700 units
        player.heatScore = (player.heatScore + amount).coerceAtMost(12f)
        val newLevel = when {
            player.heatScore >= 9.5f -> 5
            player.heatScore >= 6.8f -> 4
            player.heatScore >= 4.2f -> 3
            player.heatScore >= 2.1f -> 2
            player.heatScore >= 0.85f -> 1
            else -> 0
        }
        if (newLevel > player.wantedLevel) {
            player.wantedLevel = newLevel
            spawnFloatingPopup("${newLevel}★ WANTED!", cx, cy - 28f, Color(0xFFFF2A54))
        }
        if (player.wantedLevel > 0) {
            player.wantedCooldownSec = 10f
        }
    }

    private fun updateWantedSystem(dt: Float) {
        if (player.wantedLevel <= 0) {
            chopperActive = false
            return
        }

        // Maintain active police responders scaled by wanted level
        val desiredCruisers = when (player.wantedLevel) {
            1 -> 0
            2 -> 2
            3 -> 3
            4 -> 4
            else -> 5
        }
        val activePoliceCars = vehicles.count { it.isPoliceActive && it.hp > 0f }
        if (activePoliceCars < desiredCruisers) {
            val ang = rng.nextFloat() * CityWorld.TWO_PI
            val sx = (player.x + cos(ang) * 580f).coerceIn(180f, world.worldWidth - 180f)
            val sy = (player.y + sin(ang) * 580f).coerceIn(240f, world.worldHeight - 180f)
            if (!world.isWater(sx, sy) && !isBlockedByBuilding(sx, sy, 26f)) {
                val vType = if (player.wantedLevel >= 4 && rng.nextFloat() < 0.45f) {
                    VehicleType.ARMORED_VAN
                } else {
                    VehicleType.POLICE_CRUISER
                }
                vehicles += VehicleEntity(
                    id = nextDynamicId++,
                    type = vType,
                    x = sx, y = sy,
                    angleRad = ang + PI.toFloat(),
                    speed = 240f,
                    isPoliceActive = true,
                    sirenOn = true
                )
            }
        }

        val desiredOfficers = 1 + player.wantedLevel
        val activeFootPolice = npcs.count { it.role == NpcRole.OFFICER || it.role == NpcRole.SWAT }
        if (activeFootPolice < desiredOfficers) {
            val ang = rng.nextFloat() * CityWorld.TWO_PI
            val sx = (player.x + cos(ang) * 380f).coerceIn(160f, world.worldWidth - 160f)
            val sy = (player.y + sin(ang) * 380f).coerceIn(220f, world.worldHeight - 160f)
            if (!world.isWater(sx, sy) && !isBlockedByBuilding(sx, sy, 14f)) {
                npcs += NpcEntity(
                    id = nextDynamicId++,
                    role = if (player.wantedLevel >= 4) NpcRole.SWAT else NpcRole.OFFICER,
                    x = sx, y = sy,
                    hp = if (player.wantedLevel >= 4) 120f else 80f,
                    shirtColor = if (player.wantedLevel >= 4) Color(0xFF0F172A) else Color(0xFF2563EB)
                )
            }
        }

        // 5-Star Chopper Pursuit
        chopperActive = player.wantedLevel >= 5
        if (chopperActive) {
            val targetAng = atan2((player.y - chopperY).toDouble(), (player.x - chopperX).toDouble()).toFloat()
            if (hypot((player.x - chopperX).toDouble(), (player.y - chopperY).toDouble()) > 1200f) {
                chopperX = player.x - 450f
                chopperY = player.y - 450f
            }
            chopperAngle = targetAng
            chopperX += cos(targetAng) * 260f * dt
            chopperY += sin(targetAng) * 260f * dt
        }

        // Check if any police sees player within 460 units
        val policeClose = vehicles.any {
            it.isPoliceActive && it.hp > 0f &&
                hypot((it.x - player.x).toDouble(), (it.y - player.y).toDouble()) < 480f
        } || npcs.any {
            (it.role == NpcRole.OFFICER || it.role == NpcRole.SWAT) &&
                hypot((it.x - player.x).toDouble(), (it.y - player.y).toDouble()) < 420f
        }

        if (policeClose) {
            player.wantedCooldownSec = 9.5f
        } else {
            player.wantedCooldownSec -= dt
            if (player.wantedCooldownSec <= 0f) {
                player.wantedLevel = 0
                player.heatScore = 0f
                vehicles.removeAll { it.isPoliceActive }
                spawnFloatingPopup("HEAT EVADED!", player.x, player.y - 28f, Color(0xFF00E5FF))
            }
        }
    }

    private fun updateWorldPickupsAndDistricts() {
        // District discovery
        val dist = world.districtAt(player.x, player.y)
        if (dist !in discoveredDistricts) {
            discoveredDistricts += dist
            player.respectXp += 100
            player.cash += 250
            districtBannerText = "DISCOVERED: ${dist.displayName.uppercase()} (+$250)"
            districtBannerTimer = 3.5f
            onSaveRequested?.invoke()
        }

        // Hidden Packages
        for (pkg in world.hiddenPackages) {
            if (pkg.id !in collectedPackages &&
                hypot((pkg.x - player.x).toDouble(), (pkg.y - player.y).toDouble()) < 34f
            ) {
                collectedPackages += pkg.id
                player.cash += 500
                player.respectXp += 150
                spawnFloatingPopup("PACKAGE ${collectedPackages.size}/${world.hiddenPackages.size} (+$500)", pkg.x, pkg.y - 24f, Color(0xFF00E5FF))
                onSaveRequested?.invoke()
            }
        }

        // Service Points
        for (sp in world.servicePoints) {
            if (sp.cooldownSec > 0f) continue
            val d = hypot((sp.x - player.x).toDouble(), (sp.y - player.y).toDouble()).toFloat()
            val reach = when (sp.type) {
                ServicePointType.SPRAY_SHOP -> 56f
                ServicePointType.DINER -> 48f
                else -> 34f
            }
            if (d < reach) {
                when (sp.type) {
                    ServicePointType.HEALTH_PACK -> {
                        if (player.health < 100f) {
                            player.health = 100f
                            sp.cooldownSec = 25f
                            spawnFloatingPopup("FULL HEALTH", sp.x, sp.y - 20f, Color(0xFF00E676))
                        }
                    }
                    ServicePointType.ARMOR_VEST -> {
                        if (player.armor < 100f) {
                            player.armor = 100f
                            sp.cooldownSec = 30f
                            spawnFloatingPopup("ARMOR +100", sp.x, sp.y - 20f, Color(0xFF38BDF8))
                        }
                    }
                    ServicePointType.BRIBE_STAR -> {
                        if (player.wantedLevel > 0) {
                            player.wantedLevel = (player.wantedLevel - 1).coerceAtLeast(0)
                            player.heatScore = (player.heatScore - 2.5f).coerceAtLeast(0f)
                            sp.cooldownSec = 30f
                            spawnFloatingPopup("BRIBE -1★", sp.x, sp.y - 20f, Color(0xFFFFB300))
                        }
                    }
                    ServicePointType.SPRAY_SHOP -> {
                        val pCar = player.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
                        if (player.wantedLevel > 0 || (pCar != null && pCar.hp < pCar.type.maxHp)) {
                            val fee = if (player.cash >= 150) 150 else 0
                            player.cash -= fee
                            player.wantedLevel = 0
                            player.heatScore = 0f
                            vehicles.removeAll { it.isPoliceActive }
                            if (pCar != null) {
                                pCar.hp = pCar.type.maxHp
                                val colors = listOf(
                                    Color(0xFF00E5FF), Color(0xFFFF4D6D), Color(0xFFFFB300),
                                    Color(0xFF10B981), Color(0xFFA855F7)
                                )
                                pCar.customColor = colors[rng.nextInt(colors.size)]
                            }
                            sp.cooldownSec = 10f
                            spawnFloatingPopup("SPRAY & FIX! HEAT CLEARED", sp.x, sp.y - 24f, Color(0xFF00E5FF))
                        }
                    }
                    ServicePointType.ARMORY -> {
                        sp.cooldownSec = 6f
                        onOpenArmoryRequested?.invoke()
                    }
                    ServicePointType.DINER -> {
                        if (player.health < 100f || player.armor < 100f) {
                            val mealFee = if (player.cash >= 15) 15 else 0
                            player.cash -= mealFee
                            player.health = 100f
                            player.armor = (player.armor + 20f).coerceAtMost(100f)
                            sp.cooldownSec = 14f
                            spawnFloatingPopup(
                                "${sp.label.uppercase()}! FULL HP +20 ARMOR",
                                sp.x,
                                sp.y - 24f,
                                Color(0xFFFB923C)
                            )
                            onSaveRequested?.invoke()
                        }
                    }
                }
            }
        }
    }

    fun startMission(missionId: String) {
        val def = CityWorldBuilder.missions.firstOrNull { it.id == missionId } ?: return
        vehicles.removeAll { it.missionTag != null }
        npcs.removeAll { it.missionTag != null }
        missionStepIndex = 0
        missionLastStationIdx = -1
        missionStageClearDone = false
        player.vehicleId = null
        overlayStatus = GameOverlayStatus.NONE

        when (def.goalType) {
            MissionGoalType.DELIVER_VEHICLE -> {
                player.x = def.triggerX
                player.y = def.triggerY + 40f
                val courier = VehicleEntity(
                    id = nextDynamicId++,
                    type = VehicleType.MUSCLE_CAR,
                    x = def.triggerX + 55f,
                    y = def.triggerY,
                    angleRad = 0f,
                    customColor = Color(0xFFFFB300),
                    missionTag = "COURIER"
                )
                vehicles += courier
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Drive the Gold Stallion V8 to Harbor Terminal",
                    targetX = 1480f, targetY = 3620f,
                    progressCurrent = 0, progressTotal = 1,
                    timeRemainingSec = def.timeLimitSec
                )
            }
            MissionGoalType.WATER_CHECKPOINTS -> {
                player.x = 1485f
                player.y = 4020f
                vehicles += VehicleEntity(
                    id = nextDynamicId++,
                    type = VehicleType.SPEEDBOAT,
                    x = 1580f, y = 4060f,
                    angleRad = (PI / 2).toFloat(),
                    customColor = Color(0xFF00E5FF)
                )
                missionCheckpoints = listOf(
                    1600f to 4320f,
                    2350f to 4420f,
                    3100f to 4300f,
                    3850f to 4250f,
                    4420f to 3450f
                )
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Board a Jetboat & hit Bay Buoy 1/5",
                    targetX = missionCheckpoints[0].first, targetY = missionCheckpoints[0].second,
                    progressCurrent = 0, progressTotal = missionCheckpoints.size,
                    timeRemainingSec = def.timeLimitSec
                )
            }
            MissionGoalType.AERIAL_RINGS -> {
                player.x = 460f
                player.y = 560f
                vehicles += VehicleEntity(
                    id = nextDynamicId++,
                    type = VehicleType.STUNT_JET,
                    x = 380f, y = 490f,
                    angleRad = 0f
                )
                missionCheckpoints = listOf(
                    1350f to 490f,
                    2380f to 850f,
                    2700f to 1950f,
                    1600f to 1200f,
                    720f to 490f
                )
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Take off & fly through Aerial Ring 1/4, then land at Runway",
                    targetX = missionCheckpoints[0].first, targetY = missionCheckpoints[0].second,
                    progressCurrent = 0, progressTotal = missionCheckpoints.size,
                    timeRemainingSec = def.timeLimitSec
                )
            }
            MissionGoalType.TRAIN_STATIONS -> {
                val firstTrain = vehicles.firstOrNull { it.type == VehicleType.METRO_TRAIN }
                if (firstTrain != null) {
                    player.x = firstTrain.x
                    player.y = firstTrain.y + 45f
                }
                val st = world.metroStations[0]
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Board Metro Express & pass 3 Metro Stations",
                    targetX = st.x, targetY = st.y,
                    progressCurrent = 0, progressTotal = 3,
                    timeRemainingSec = def.timeLimitSec
                )
            }
            MissionGoalType.TAKEDOWN_VEHICLES -> {
                player.x = def.triggerX
                player.y = def.triggerY
                vehicles += VehicleEntity(
                    id = nextDynamicId++,
                    type = VehicleType.SPORTS_CAR,
                    x = def.triggerX + 60f, y = def.triggerY,
                    angleRad = 0f
                )
                val targetSpots = listOf(3120f to 2220f, 3450f to 2860f, 2950f to 3300f)
                targetSpots.forEach { (tx, ty) ->
                    vehicles += VehicleEntity(
                        id = nextDynamicId++,
                        type = VehicleType.MUSCLE_CAR,
                        x = tx, y = ty,
                        angleRad = 0f,
                        hp = 120f,
                        customColor = Color(0xFFFF2A54),
                        missionTag = "TARGET_CAR"
                    )
                }
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Disable all 3 Syndicate getaway cars",
                    targetX = targetSpots[0].first, targetY = targetSpots[0].second,
                    progressCurrent = 0, progressTotal = 3,
                    timeRemainingSec = def.timeLimitSec
                )
            }
            MissionGoalType.CLEAR_AND_ESCAPE -> {
                player.x = def.triggerX - 90f
                player.y = def.triggerY
                val crewSpots = listOf(
                    960f to 2280f, 1020f to 2340f, 920f to 2400f,
                    1040f to 2440f, 980f to 2500f
                )
                crewSpots.forEach { (rx, ry) ->
                    npcs += NpcEntity(
                        id = nextDynamicId++,
                        role = NpcRole.RIVAL,
                        x = rx, y = ry,
                        hp = 75f,
                        shirtColor = Color(0xFFFF2A54),
                        missionTag = "FOUNDRY_CREW"
                    )
                }
                activeMission = ActiveMissionState(
                    def = def,
                    objectiveText = "Defeat Foundry Yard Rivals (0/5)",
                    targetX = 980f, targetY = 2380f,
                    progressCurrent = 0, progressTotal = 5,
                    timeRemainingSec = def.timeLimitSec
                )
            }
        }
        cameraX = player.x
        cameraY = player.y
    }

    private fun updateActiveMission(dt: Float) {
        val m = activeMission ?: return
        val newTime = m.timeRemainingSec - dt
        if (newTime <= 0f) {
            activeMission = null
            overlayStatus = GameOverlayStatus.MISSION_FAILED
            lastResultTitle = "${m.def.title.uppercase()} FAILED"
            lastResultRewardCash = 0
            return
        }

        when (m.def.goalType) {
            MissionGoalType.DELIVER_VEHICLE -> {
                val courier = vehicles.firstOrNull { it.missionTag == "COURIER" }
                if (courier == null || courier.hp <= 0f) {
                    activeMission = null
                    overlayStatus = GameOverlayStatus.MISSION_FAILED
                    lastResultTitle = "COURIER CAR WRECKED"
                    return
                }
                val inCourier = player.vehicleId == courier.id
                val distToDest = hypot((courier.x - 1480f).toDouble(), (courier.y - 3620f).toDouble()).toFloat()
                if (inCourier && distToDest < 85f) {
                    completeMission(m.def)
                } else {
                    activeMission = m.copy(
                        objectiveText = if (inCourier) "Deliver Stallion V8 to Harbor Terminal" else "Get into the Gold Stallion V8",
                        targetX = if (inCourier) 1480f else courier.x,
                        targetY = if (inCourier) 3620f else courier.y,
                        timeRemainingSec = newTime
                    )
                }
            }
            MissionGoalType.WATER_CHECKPOINTS -> {
                val target = missionCheckpoints[missionStepIndex]
                val pCar = player.vehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
                val inBoat = pCar?.type?.category == VehicleCategory.WATER
                val d = hypot((player.x - target.first).toDouble(), (player.y - target.second).toDouble()).toFloat()
                if (inBoat && d < 80f) {
                    missionStepIndex++
                    if (missionStepIndex >= missionCheckpoints.size) {
                        completeMission(m.def)
                    } else {
                        val nextPt = missionCheckpoints[missionStepIndex]
                        spawnFloatingPopup("BUOY ${missionStepIndex}/${missionCheckpoints.size}", target.first, target.second, Color(0xFF00E5FF))
                        activeMission = m.copy(
                            objectiveText = "Hit Bay Buoy ${missionStepIndex + 1}/${missionCheckpoints.size}",
                            targetX = nextPt.first, targetY = nextPt.second,
                            progressCurrent = missionStepIndex,
                            timeRemainingSec = newTime
                        )
                    }
                } else {
                    activeMission = m.copy(
                        objectiveText = if (inBoat) "Hit Bay Buoy ${missionStepIndex + 1}/${missionCheckpoints.size}" else "Board a Jetboat at the Pier",
                        timeRemainingSec = newTime
                    )
                }
            }
            MissionGoalType.AERIAL_RINGS -> {
                val target = missionCheckpoints[missionStepIndex]
                val pCar = player.vehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
                val inAir = pCar?.type?.category == VehicleCategory.AIR
                val d = hypot((player.x - target.first).toDouble(), (player.y - target.second).toDouble()).toFloat()
                val isFinalLanding = missionStepIndex == missionCheckpoints.lastIndex
                val passed = if (isFinalLanding) {
                    inAir && d < 120f && (pCar?.altitude ?: 1f) < 0.25f
                } else {
                    inAir && d < 110f && (pCar?.altitude ?: 0f) > 0.15f
                }
                if (passed) {
                    missionStepIndex++
                    if (missionStepIndex >= missionCheckpoints.size) {
                        completeMission(m.def)
                    } else {
                        val nextPt = missionCheckpoints[missionStepIndex]
                        spawnFloatingPopup("RING CLEARED!", target.first, target.second, Color(0xFF38BDF8))
                        activeMission = m.copy(
                            objectiveText = if (missionStepIndex == missionCheckpoints.lastIndex) {
                                "Land back on Metro Runway!"
                            } else {
                                "Fly through Ring ${missionStepIndex + 1}/4"
                            },
                            targetX = nextPt.first, targetY = nextPt.second,
                            progressCurrent = missionStepIndex,
                            timeRemainingSec = newTime
                        )
                    }
                } else {
                    activeMission = m.copy(timeRemainingSec = newTime)
                }
            }
            MissionGoalType.TRAIN_STATIONS -> {
                val pCar = player.vehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
                val onTrain = pCar?.type == VehicleType.METRO_TRAIN
                if (onTrain) {
                    world.metroStations.forEachIndexed { idx, st ->
                        if (idx != missionLastStationIdx &&
                            hypot((player.x - st.x).toDouble(), (player.y - st.y).toDouble()) < 85f
                        ) {
                            missionLastStationIdx = idx
                            missionStepIndex++
                            spawnFloatingPopup("${st.name.uppercase()} (${missionStepIndex}/3)", st.x, st.y - 30f, Color(0xFF00E5FF))
                        }
                    }
                }
                if (missionStepIndex >= 3) {
                    completeMission(m.def)
                } else {
                    val nextSt = world.metroStations[(missionLastStationIdx + 1) % world.metroStations.size]
                    val nearestTrain = vehicles.firstOrNull { it.type == VehicleType.METRO_TRAIN }
                    activeMission = m.copy(
                        objectiveText = if (onTrain) "Pilot Metro Express past ${nextSt.name} ($missionStepIndex/3)" else "Board the Metro Express Train",
                        targetX = if (onTrain) nextSt.x else (nearestTrain?.x ?: nextSt.x),
                        targetY = if (onTrain) nextSt.y else (nearestTrain?.y ?: nextSt.y),
                        progressCurrent = missionStepIndex,
                        timeRemainingSec = newTime
                    )
                }
            }
            MissionGoalType.TAKEDOWN_VEHICLES -> {
                val remainingCars = vehicles.filter { it.missionTag == "TARGET_CAR" && it.hp > 0f }
                val done = 3 - remainingCars.size
                if (remainingCars.isEmpty()) {
                    completeMission(m.def)
                } else {
                    val focus = remainingCars.first()
                    activeMission = m.copy(
                        objectiveText = "Disable Syndicate Cars ($done/3)",
                        targetX = focus.x, targetY = focus.y,
                        progressCurrent = done,
                        timeRemainingSec = newTime
                    )
                }
            }
            MissionGoalType.CLEAR_AND_ESCAPE -> {
                val remainingCrew = npcs.filter { it.missionTag == "FOUNDRY_CREW" }
                if (!missionStageClearDone) {
                    val done = 5 - remainingCrew.size
                    if (remainingCrew.isEmpty()) {
                        missionStageClearDone = true
                        addCrimeHeat(4.5f, player.x, player.y)
                        activeMission = m.copy(
                            objectiveText = "Lose the 3★ Metro PD Heat!",
                            progressCurrent = 5,
                            timeRemainingSec = newTime
                        )
                    } else {
                        val focus = remainingCrew.first()
                        activeMission = m.copy(
                            objectiveText = "Defeat Foundry Rivals ($done/5)",
                            targetX = focus.x, targetY = focus.y,
                            progressCurrent = done,
                            timeRemainingSec = newTime
                        )
                    }
                } else {
                    if (player.wantedLevel == 0) {
                        completeMission(m.def)
                    } else {
                        activeMission = m.copy(
                            objectiveText = "Evade Metro PD until Wanted Stars clear!",
                            targetX = 2820f, targetY = 2150f,
                            timeRemainingSec = newTime
                        )
                    }
                }
            }
        }
    }

    private fun completeMission(def: MissionDef) {
        activeMission = null
        completedMissions += def.id
        player.cash += def.cashReward
        player.respectXp += def.xpReward
        overlayStatus = GameOverlayStatus.MISSION_PASSED
        lastResultTitle = def.title.uppercase()
        lastResultRewardCash = def.cashReward
        onSaveRequested?.invoke()
    }

    fun abandonActiveMission() {
        activeMission = null
        vehicles.removeAll { it.missionTag != null }
        npcs.removeAll { it.missionTag != null }
    }

    fun findNearbyMissionTrigger(): MissionDef? {
        if (activeMission != null) return null
        return CityWorldBuilder.missions.firstOrNull { m ->
            hypot((m.triggerX - player.x).toDouble(), (m.triggerY - player.y).toDouble()) < 64f
        }
    }

    fun cycleWeapon() {
        val list = WeaponType.entries.filter { it in player.ownedWeapons }
        if (list.isEmpty()) return
        val idx = list.indexOf(player.equippedWeapon)
        player.equippedWeapon = list[(idx + 1) % list.size]
    }

    fun equipWeapon(weapon: WeaponType) {
        if (weapon in player.ownedWeapons) {
            player.equippedWeapon = weapon
        }
    }

    fun purchaseWeaponOrAmmo(weapon: WeaponType): Boolean {
        if (weapon !in player.ownedWeapons) {
            if (player.cash < weapon.unlockCost) return false
            player.cash -= weapon.unlockCost
            player.ownedWeapons += weapon
            player.ammoMap[weapon] = (player.ammoMap[weapon] ?: 0) + weapon.ammoPackAmount * 2
            player.equippedWeapon = weapon
            onSaveRequested?.invoke()
            return true
        } else if (weapon.ammoPackCost > 0) {
            if (player.cash < weapon.ammoPackCost) return false
            player.cash -= weapon.ammoPackCost
            player.ammoMap[weapon] = (player.ammoMap[weapon] ?: 0) + weapon.ammoPackAmount
            player.equippedWeapon = weapon
            onSaveRequested?.invoke()
            return true
        }
        return false
    }

    fun buyArmorVest(): Boolean {
        if (player.cash < 200 || player.armor >= 100f) return false
        player.cash -= 200
        player.armor = 100f
        onSaveRequested?.invoke()
        return true
    }

    private fun updateParticles(dt: Float) {
        for (i in particles.lastIndex downTo 0) {
            val pt = particles[i]
            pt.x += pt.vx * dt
            pt.y += pt.vy * dt
            pt.vz += Physics3DEngine.GRAVITY * 0.55f * dt
            pt.zHeight = (pt.zHeight + pt.vz * dt).coerceAtLeast(0.5f)
            pt.life -= dt
            if (pt.life <= 0f) particles.removeAt(i)
        }
    }

    private fun spawnExplosionSparks(x: Float, y: Float, count: Int, color: Color) {
        repeat(count) {
            val ang = rng.nextFloat() * CityWorld.TWO_PI
            val spd = 40f + rng.nextFloat() * 140f
            particles += ParticleEntity(
                x = x, y = y,
                vx = cos(ang) * spd,
                vy = sin(ang) * spd,
                radius = 5f + rng.nextFloat() * 6f,
                maxLife = 0.45f,
                life = 0.45f,
                color = color
            )
        }
    }

    private fun spawnFloatingPopup(text: String, x: Float, y: Float, color: Color) {
        floatingPopups += FloatingPopup(text = text, x = x, y = y, color = color)
    }

    private fun updateCamera(dt: Float) {
        val pCar = player.vehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
        val leadDist = if (pCar != null) (pCar.speed * 0.22f).coerceIn(0f, 110f) else 20f
        val targetCamX = player.x + cos(player.angleRad) * leadDist
        val targetCamY = player.y + sin(player.angleRad) * leadDist
        val smooth = (7.5f * dt).coerceAtMost(1f)
        cameraX += (targetCamX - cameraX) * smooth
        cameraY += (targetCamY - cameraY) * smooth

        // Smooth 3D chase camera behind vehicle heading when player isn't manually orbiting
        if (pCar != null && System.currentTimeMillis() - lastCameraDragEpochMs > 900L && abs(pCar.speed) > 40f) {
            val yawDiff = normalizeAngle(pCar.angleRad - cameraYawRad)
            cameraYawRad = normalizeAngle(cameraYawRad + yawDiff * (2.6f * dt).coerceAtMost(1f))
        }

        val targetZoom = when {
            pCar == null -> 1.05f
            pCar.type.category == VehicleCategory.AIR -> 1.0f - pCar.altitude * 0.36f
            else -> 0.94f - (abs(pCar.speed) / 600f) * 0.14f
        }
        cameraZoom += (targetZoom - cameraZoom) * (4f * dt).coerceAtMost(1f)
    }

    private fun isBlockedByBuilding(x: Float, y: Float, padding: Float): Boolean {
        for (i in world.buildings.indices) {
            if (world.buildings[i].rect.contains(x, y, padding)) return true
        }
        return false
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI) r -= CityWorld.TWO_PI
        while (r < -PI) r += CityWorld.TWO_PI
        return r
    }

    fun snapshotHud(): HudSnapshot {
        val pCar = player.vehicleId?.let { vid -> vehicles.firstOrNull { it.id == vid } }
        val nearCar = if (pCar == null) findNearestEnterableVehicle() else null
        val nearMission = findNearbyMissionTrigger()
        return HudSnapshot(
            cash = player.cash,
            respectXp = player.respectXp,
            health = player.health.toInt().coerceIn(0, 100),
            armor = player.armor.toInt().coerceIn(0, 100),
            wantedLevel = player.wantedLevel,
            wantedFlashing = player.wantedLevel > 0 && player.wantedCooldownSec < 6.5f,
            currentDistrict = world.districtAt(player.x, player.y),
            districtBannerText = districtBannerText,
            stuntBannerText = stuntBannerText,
            equippedWeapon = player.equippedWeapon,
            equippedAmmo = player.ammoMap[player.equippedWeapon] ?: 0,
            ownedWeapons = player.ownedWeapons.toSet(),
            inVehicleName = pCar?.type?.displayName,
            inVehicleCategory = pCar?.type?.category,
            vehicleSpeedKmh = if (pCar != null) (abs(pCar.speed) * 0.36f).toInt() else 0,
            vehicleAltitudeMeters = if (pCar != null) (pCar.altitude * 180f).toInt() else 0,
            vehicleHpPercent = if (pCar != null) ((pCar.hp / pCar.type.maxHp) * 100f).toInt().coerceIn(0, 100) else 100,
            nearbyVehicleName = nearCar?.type?.displayName,
            nearbyMissionTitle = nearMission?.title,
            nearbyMissionId = nearMission?.id,
            activeMission = activeMission,
            waypointX = waypointX,
            waypointY = waypointY,
            discoveredDistricts = discoveredDistricts.toSet(),
            collectedPackages = collectedPackages.toSet(),
            completedMissions = completedMissions.toSet(),
            bestStuntMeters = bestStuntMeters,
            totalDistanceMeters = totalDistanceMeters,
            overlayStatus = overlayStatus,
            lastResultTitle = lastResultTitle,
            lastResultRewardCash = lastResultRewardCash,
            playerX = player.x,
            playerY = player.y,
            playerAngleRad = player.angleRad,
            cameraYawRad = cameraYawRad,
            clockText = String.format(
                "%02d:%02d",
                (worldTimeMinutes.toInt() / 60) % 24,
                worldTimeMinutes.toInt() % 60
            ),
            isFiring = inputFiring
        )
    }
}
