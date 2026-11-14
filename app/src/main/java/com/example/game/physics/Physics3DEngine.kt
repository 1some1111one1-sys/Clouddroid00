package com.example.game.physics

import androidx.compose.ui.graphics.Color
import com.example.game.model.CityWorld
import com.example.game.model.DynamicProp3D
import com.example.game.model.NpcEntity
import com.example.game.model.ParticleEntity
import com.example.game.model.PlayerEntity
import com.example.game.model.PropType
import com.example.game.model.VehicleCategory
import com.example.game.model.VehicleEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic 3D rigid-body & impulse physics engine compatible with Compose & OpenGL ES 3.0.
 * Handles:
 *  - Character 3D gravity, jumping, coyote time, ramp slopes, and prop/vehicle platforming
 *  - Mass-weighted 3D vehicle-to-vehicle impulse collisions, positional separation, and suspension tilt
 *  - Interactive 3D dynamic props (traffic cones, supply crates, fire hydrants, hazard barrels)
 *  - Building AABB bounce & radial explosion shockwaves
 */
class Physics3DEngine(
    private val world: CityWorld
) {
    companion object {
        const val GRAVITY = -165f          // Vertical acceleration (world-height units / s^2)
        const val PLAYER_JUMP_SPEED = 78f  // Crisp upward jump velocity
        const val MAX_FALL_SPEED = -260f
        const val VEHICLE_RESTITUTION = 0.42f
        const val PROP_FRICTION = 3.8f
    }

    private val rng = Random(2026)

    fun spawnInitialProps(): MutableList<DynamicProp3D> {
        val list = mutableListOf<DynamicProp3D>()
        var id = 1

        // Downtown avenue cones & fire hydrants near safehouse & main streets
        val downtownCones = listOf(
            2290f to 2175f, 2310f to 2175f, 2330f to 2175f,
            2340f to 2040f, 2340f to 2065f,
            1795f to 1640f, 1795f to 1665f, 2435f to 1640f
        )
        for ((cx, cy) in downtownCones) {
            list += DynamicProp3D(id++, PropType.TRAFFIC_CONE, cx, cy, zHeight = 0.44f)
        }

        // Fire hydrants along Downtown sidewalks
        val hydrants = listOf(
            2140f to 2172f, 2330f to 2272f, 1792f to 2165f,
            2432f to 1525f, 2965f to 2165f, 3605f to 2805f
        )
        for ((hx, hy) in hydrants) {
            list += DynamicProp3D(id++, PropType.HYDRANT, hx, hy, zHeight = 0.44f)
        }

        // Supply crates & hazard barrels at Foundry Yards & Harbor Piers
        val crateSpots = listOf(
            2080f to 2190f, 2098f to 2190f, 2089f to 2190f, // jumpable crates near spawn!
            940f to 2320f, 960f to 2320f, 950f to 2340f,
            690f to 3960f, 710f to 3960f, 1480f to 3980f, 1500f to 3980f
        )
        crateSpots.forEachIndexed { idx, (cx, cy) ->
            val stackedY = if (idx == 2) 14.5f else 0.44f
            list += DynamicProp3D(id++, PropType.WOODEN_CRATE, cx, cy, zHeight = stackedY)
        }

        val barrelSpots = listOf(
            2265f to 2255f, 990f to 2290f, 1010f to 2290f,
            1240f to 2490f, 665f to 4020f, 1445f to 4030f,
            // Island 2: Venturas & Verdant Desert Airfield
            6120f to 2200f, 7800f to 780f, 7825f to 780f, 8950f to 2880f,
            // Island 3: San Fierro Bay & Easter Naval Air Base
            2420f to 6650f, 5600f to 6300f, 5625f to 6300f, 7250f to 9200f
        )
        for ((bx, by) in barrelSpots) {
            list += DynamicProp3D(id++, PropType.HAZARD_BARREL, bx, by, zHeight = 0.44f)
        }

        // Extra cones, crates & hydrants across Island 2 & Island 3
        val outerIslandCones = listOf(
            6140f to 1620f, 6160f to 1620f, 7740f to 520f,
            2360f to 5560f, 2380f to 5560f, 5480f to 6020f
        )
        for ((cx, cy) in outerIslandCones) {
            list += DynamicProp3D(id++, PropType.TRAFFIC_CONE, cx, cy, zHeight = 0.44f)
        }
        val outerHydrants = listOf(
            6210f to 2260f, 6850f to 1620f, 2430f to 7520f, 3860f to 7520f
        )
        for ((hx, hy) in outerHydrants) {
            list += DynamicProp3D(id++, PropType.HYDRANT, hx, hy, zHeight = 0.44f)
        }
        val outerCrates = listOf(
            7860f to 490f, 7880f to 490f, 5650f to 5960f, 5670f to 5960f, 7220f to 9240f,
            // Island 4: Palomino Creek & Island 5: Mt. Chiliad / Red County
            10500f to 6720f, 10520f to 6720f, 5620f to 11260f, 5640f to 11260f, 9080f to 10420f
        )
        for ((cx, cy) in outerCrates) {
            list += DynamicProp3D(id++, PropType.WOODEN_CRATE, cx, cy, zHeight = 0.44f)
        }
        val extraBarrelsAndCones = listOf(
            PropType.HAZARD_BARREL to (10440f to 5380f),
            PropType.HAZARD_BARREL to (11380f to 8740f),
            PropType.HAZARD_BARREL to (5580f to 10240f),
            PropType.HAZARD_BARREL to (9140f to 11280f),
            PropType.TRAFFIC_CONE to (10480f to 6650f),
            PropType.TRAFFIC_CONE to (10510f to 6650f),
            PropType.TRAFFIC_CONE to (3840f to 10360f),
            PropType.HYDRANT to (10520f to 7560f),
            PropType.HYDRANT to (7520f to 11300f)
        )
        for ((type, pos) in extraBarrelsAndCones) {
            list += DynamicProp3D(id++, type, pos.first, pos.second, zHeight = 0.44f)
        }

        return list
    }

    /**
     * Computes the solid 3D surface elevation directly beneath (x, y) considering
     * sidewalks, piers, sloped stunt ramps, and standable crates/vehicles.
     */
    fun sampleGroundElevation(
        x: Float,
        y: Float,
        currentZ: Float,
        props: List<DynamicProp3D>,
        vehicles: List<VehicleEntity>,
        ignoreVehicleId: Int? = null
    ): Float {
        var floorY = 0f

        // Island landmasses slightly above ocean surface
        for (i in world.islands.indices) {
            if (world.islands[i].contains(x, y)) {
                floorY = maxOf(floorY, 0.28f)
            }
        }

        // Elevated Inter-Island Bridges over ocean channels
        for (i in world.bridges.indices) {
            if (world.bridges[i].contains(x, y)) {
                floorY = maxOf(floorY, 3.2f)
            }
        }

        // Piers over water
        for (i in world.piers.indices) {
            if (world.piers[i].contains(x, y)) {
                floorY = maxOf(floorY, 1.2f)
            }
        }

        // Sidewalks border roads slightly above road bed
        for (i in world.roads.indices) {
            val r = world.roads[i].rect
            if (r.contains(x, y, padding = 13f) && !r.contains(x, y, padding = 0f)) {
                floorY = maxOf(floorY, 0.44f)
            } else if (r.contains(x, y)) {
                floorY = maxOf(floorY, 0.48f)
            }
        }

        // Sloped 3D Stunt Ramps
        for (i in world.stuntRamps.indices) {
            val ramp = world.stuntRamps[i]
            val r = ramp.rect
            if (r.contains(x, y)) {
                val u = ((x - r.left) / r.w).coerceIn(0f, 1f)
                val v = ((y - r.top) / r.h).coerceIn(0f, 1f)
                // Match MeshBuilder ramp slope direction
                val dirX = cos(ramp.launchAngleRad)
                val dirY = sin(ramp.launchAngleRad)
                val t = when {
                    abs(dirX) > abs(dirY) -> if (dirX > 0f) u else 1f - u
                    else -> if (dirY > 0f) v else 1f - v
                }
                floorY = maxOf(floorY, 0.4f + t * 18f)
            }
        }

        // Standable dynamic crates
        for (i in props.indices) {
            val p = props[i]
            if (p.type == PropType.WOODEN_CRATE) {
                val d = hypot((x - p.x).toDouble(), (y - p.y).toDouble()).toFloat()
                val topY = p.zHeight + p.type.height
                if (d < p.type.radius + 4f && currentZ >= topY - 6f) {
                    floorY = maxOf(floorY, topY)
                }
            }
        }

        // Standable vehicle roofs
        for (i in vehicles.indices) {
            val v = vehicles[i]
            if (v.id == ignoreVehicleId || v.hp <= 0f || v.altitude > 0.1f) continue
            val d = hypot((x - v.x).toDouble(), (y - v.y).toDouble()).toFloat()
            val roofY = if (v.type.category == VehicleCategory.RAIL) 17f else 10.5f
            if (d < v.type.width * 0.7f && currentZ >= roofY - 4f) {
                floorY = maxOf(floorY, roofY)
            }
        }

        return floorY
    }

    fun triggerPlayerJump(player: PlayerEntity): Boolean {
        if (player.vehicleId != null) return false
        if (player.isGrounded || player.coyoteTimeSec > 0f) {
            player.vz = PLAYER_JUMP_SPEED
            player.isGrounded = false
            player.coyoteTimeSec = 0f
            return true
        }
        return false
    }

    fun stepPlayerGravityAndProps(
        player: PlayerEntity,
        props: MutableList<DynamicProp3D>,
        vehicles: List<VehicleEntity>,
        particles: MutableList<ParticleEntity>,
        dt: Float
    ) {
        if (player.vehicleId != null) {
            player.zHeight = 0f
            player.vz = 0f
            player.isGrounded = true
            return
        }

        val groundY = sampleGroundElevation(player.x, player.y, player.zHeight, props, vehicles)
        if (!player.isGrounded) {
            player.vz = (player.vz + GRAVITY * dt).coerceAtLeast(MAX_FALL_SPEED)
            player.zHeight += player.vz * dt
            if (player.zHeight <= groundY) {
                val impactSpeed = -player.vz
                player.zHeight = groundY
                player.vz = 0f
                player.isGrounded = true
                player.coyoteTimeSec = 0.12f
                if (impactSpeed > 65f) {
                    // Landing dust puff
                    repeat(4) {
                        val a = rng.nextFloat() * 6.28f
                        particles += ParticleEntity(
                            x = player.x, y = player.y,
                            vx = cos(a) * 28f, vy = sin(a) * 28f,
                            radius = 3.5f, maxLife = 0.3f, life = 0.3f,
                            color = Color(0xFFD6D3D1),
                            zHeight = player.zHeight + 1f, vz = 10f
                        )
                    }
                }
            }
        } else {
            if (player.zHeight > groundY + 1.2f) {
                player.isGrounded = false
                player.coyoteTimeSec = 0.12f
            } else {
                player.zHeight = groundY
                player.coyoteTimeSec = 0.12f
            }
        }
        if (!player.isGrounded && player.coyoteTimeSec > 0f) {
            player.coyoteTimeSec = (player.coyoteTimeSec - dt).coerceAtLeast(0f)
        }

        // Player pushing / colliding with dynamic world props
        for (i in props.indices) {
            val p = props[i]
            val dx = p.x - player.x
            val dy = p.y - player.y
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val minDist = 8.5f + p.type.radius
            if (dist < minDist && dist > 0.01f && abs(player.zHeight - p.zHeight) < p.type.height) {
                val nx = dx / dist
                val ny = dy / dist
                val overlap = minDist - dist
                if (p.type == PropType.TRAFFIC_CONE || p.type == PropType.HAZARD_BARREL) {
                    p.x += nx * overlap
                    p.y += ny * overlap
                    p.vx += nx * 75f
                    p.vy += ny * 75f
                    p.vz = maxOf(p.vz, 22f)
                    p.isGrounded = false
                    p.angVel += 180f
                } else {
                    // Heavier crates & hydrants push back slightly
                    p.x += nx * overlap * 0.35f
                    p.y += ny * overlap * 0.35f
                    player.x -= nx * overlap * 0.65f
                    player.y -= ny * overlap * 0.65f
                    p.vx += nx * 24f
                    p.vy += ny * 24f
                }
            }
        }
    }

    fun stepNpcGravity(npc: NpcEntity, props: List<DynamicProp3D>, vehicles: List<VehicleEntity>, dt: Float) {
        val groundY = sampleGroundElevation(npc.x, npc.y, npc.zHeight, props, vehicles)
        if (npc.zHeight > groundY || npc.vz > 0f) {
            npc.vz = (npc.vz + GRAVITY * dt).coerceAtLeast(MAX_FALL_SPEED)
            npc.zHeight += npc.vz * dt
            if (npc.zHeight <= groundY) {
                npc.zHeight = groundY
                npc.vz = 0f
            }
        } else {
            npc.zHeight = groundY
        }
    }

    /**
     * Resolves 3D vehicle-to-vehicle elastic/inelastic collisions using mass-weighted impulses,
     * plus vehicle-to-prop impacts and suspension pitch/roll dynamics.
     */
    fun resolveVehicleCollisions(
        vehicles: MutableList<VehicleEntity>,
        props: MutableList<DynamicProp3D>,
        particles: MutableList<ParticleEntity>,
        dt: Float,
        onPoliceHit: (VehicleEntity) -> Unit,
        onVehicleWrecked: (VehicleEntity) -> Unit,
        onBarrelDetonated: (Float, Float) -> Unit
    ) {
        // Decay knockback velocities & update suspension tilt
        for (v in vehicles) {
            if (v.hp <= 0f) continue
            if (abs(v.knockVx) > 1f || abs(v.knockVy) > 1f) {
                val nx = v.x + v.knockVx * dt
                val ny = v.y + v.knockVy * dt
                if (!isBlockedByBuilding(nx, ny, v.type.width * 0.45f)) {
                    v.x = nx
                    v.y = ny
                }
                v.knockVx *= (1f - 5.5f * dt).coerceAtLeast(0f)
                v.knockVy *= (1f - 5.5f * dt).coerceAtLeast(0f)
            }
            // Suspension pitch from altitude velocity
            val targetPitch = if (v.type.category == VehicleCategory.AIR || v.altitude > 0.02f) {
                (v.verticalVel * 28f).coerceIn(-28f, 28f)
            } else {
                0f
            }
            v.pitchDeg += (targetPitch - v.pitchDeg) * (8f * dt).coerceAtMost(1f)
            v.rollDeg *= (1f - 6f * dt).coerceAtMost(1f)
        }

        // Pairwise 3D vehicle collisions (only ground/water vehicles at similar heights)
        for (i in 0 until vehicles.size) {
            val a = vehicles[i]
            if (a.hp <= 0f || a.type.category == VehicleCategory.RAIL) continue
            for (j in i + 1 until vehicles.size) {
                val b = vehicles[j]
                if (b.hp <= 0f || b.type.category == VehicleCategory.RAIL) continue
                if (abs(a.altitude - b.altitude) > 0.18f) continue

                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val minDist = (a.type.width + b.type.width) * 0.85f
                if (dist < minDist && dist > 0.01f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = minDist - dist

                    val invMassA = 1f / a.mass
                    val invMassB = 1f / b.mass
                    val invSum = invMassA + invMassB

                    // Positional separation weighted by inverse mass
                    a.x -= nx * overlap * (invMassA / invSum)
                    a.y -= ny * overlap * (invMassA / invSum)
                    b.x += nx * overlap * (invMassB / invSum)
                    b.y += ny * overlap * (invMassB / invSum)

                    val avx = cos(a.angleRad) * a.speed + a.knockVx
                    val avy = sin(a.angleRad) * a.speed + a.knockVy
                    val bvx = cos(b.angleRad) * b.speed + b.knockVx
                    val bvy = sin(b.angleRad) * b.speed + b.knockVy

                    val relVx = bvx - avx
                    val relVy = bvy - avy
                    val velAlongNormal = relVx * nx + relVy * ny

                    if (velAlongNormal < 0f) {
                        val jImpulse = -(1f + VEHICLE_RESTITUTION) * velAlongNormal / invSum
                        val impX = jImpulse * nx
                        val impY = jImpulse * ny

                        a.knockVx -= impX * invMassA
                        a.knockVy -= impY * invMassA
                        b.knockVx += impX * invMassB
                        b.knockVy += impY * invMassB

                        a.speed *= 0.78f
                        b.speed *= 0.78f
                        a.rollDeg = (rng.nextFloat() - 0.5f) * 14f
                        b.rollDeg = (rng.nextFloat() - 0.5f) * 14f

                        val impactMag = abs(velAlongNormal)
                        if (impactMag > 95f) {
                            val dmg = (impactMag - 80f) * 0.16f
                            a.hp -= dmg * (b.mass / (a.mass + b.mass)) * 1.4f
                            b.hp -= dmg * (a.mass / (a.mass + b.mass)) * 1.4f
                            spawnImpactSparks((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, particles)
                            if (b.isPoliceActive || b.type.name == "POLICE_CRUISER") onPoliceHit(b)
                            if (a.isPoliceActive || a.type.name == "POLICE_CRUISER") onPoliceHit(a)
                            if (a.hp <= 0f) onVehicleWrecked(a)
                            if (b.hp <= 0f) onVehicleWrecked(b)
                        }
                    }
                }
            }
        }

        // Vehicle-to-DynamicProp3D collisions
        for (v in vehicles) {
            if (v.hp <= 0f || v.altitude > 0.14f) continue
            val vSpeed = abs(v.speed)
            if (vSpeed < 20f && abs(v.knockVx) < 20f) continue
            val vDirX = cos(v.angleRad)
            val vDirY = sin(v.angleRad)

            for (pIdx in props.lastIndex downTo 0) {
                val p = props[pIdx]
                val dx = p.x - v.x
                val dy = p.y - v.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val hitDist = v.type.width * 0.65f + p.type.radius
                if (dist < hitDist && dist > 0.01f && p.zHeight < 14f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val push = (hitDist - dist) + 2f
                    p.x += nx * push
                    p.y += ny * push

                    val launchSpeed = (vSpeed * 0.95f).coerceAtLeast(110f)
                    p.vx = (nx * 0.65f + vDirX * 0.55f) * launchSpeed
                    p.vy = (ny * 0.65f + vDirY * 0.55f) * launchSpeed
                    p.vz = (28f + vSpeed * 0.22f).coerceAtMost(135f)
                    p.isGrounded = false
                    p.angVel = (rng.nextFloat() - 0.5f) * 520f

                    // Slow heavy crates down slightly
                    if (p.type == PropType.WOODEN_CRATE) {
                        v.speed *= 0.88f
                    }
                    // Fire hydrant water geyser!
                    if (p.type == PropType.HYDRANT && !p.triggered) {
                        p.triggered = true
                        p.geyserTimerSec = 6.5f
                    }
                    // Hazard barrel high-speed detonation!
                    if (p.type == PropType.HAZARD_BARREL && vSpeed > 190f && !p.triggered) {
                        p.triggered = true
                        val bx = p.x
                        val by = p.y
                        props.removeAt(pIdx)
                        onBarrelDetonated(bx, by)
                    }
                }
            }
        }
    }

    fun stepProps(
        props: MutableList<DynamicProp3D>,
        particles: MutableList<ParticleEntity>,
        dt: Float
    ) {
        for (i in props.indices) {
            val p = props[i]

            // Active hydrant water fountain
            if (p.type == PropType.HYDRANT && p.geyserTimerSec > 0f) {
                p.geyserTimerSec -= dt
                if (rng.nextFloat() < 0.7f) {
                    particles += ParticleEntity(
                        x = p.x + rng.nextFloat() * 4f - 2f,
                        y = p.y + rng.nextFloat() * 4f - 2f,
                        vx = rng.nextFloat() * 24f - 12f,
                        vy = rng.nextFloat() * 24f - 12f,
                        radius = 3.5f, maxLife = 0.65f, life = 0.65f,
                        color = Color(0xFFBAE6FD),
                        zHeight = p.zHeight + 4f,
                        vz = 105f + rng.nextFloat() * 25f
                    )
                }
            }

            // Horizontal integration + building bounce
            if (abs(p.vx) > 0.5f || abs(p.vy) > 0.5f) {
                val nx = (p.x + p.vx * dt).coerceIn(40f, world.worldWidth - 40f)
                val ny = (p.y + p.vy * dt).coerceIn(40f, world.worldHeight - 40f)
                if (!isBlockedByBuilding(nx, p.y, p.type.radius)) {
                    p.x = nx
                } else {
                    p.vx *= -p.type.restitution
                }
                if (!isBlockedByBuilding(p.x, ny, p.type.radius)) {
                    p.y = ny
                } else {
                    p.vy *= -p.type.restitution
                }
                val drag = if (p.isGrounded) PROP_FRICTION else 0.45f
                p.vx *= (1f - drag * dt).coerceAtLeast(0f)
                p.vy *= (1f - drag * dt).coerceAtLeast(0f)
            }

            // Vertical gravity & bounce
            val groundY = sampleGroundElevationForProp(p)
            if (!p.isGrounded || p.zHeight > groundY + 0.1f || p.vz > 0f) {
                p.vz = (p.vz + GRAVITY * dt).coerceAtLeast(MAX_FALL_SPEED)
                p.zHeight += p.vz * dt
                p.pitchDeg = (p.pitchDeg + p.angVel * dt) % 360f
                p.rollDeg = (p.rollDeg + p.angVel * 0.6f * dt) % 360f
                if (p.zHeight <= groundY) {
                    p.zHeight = groundY
                    if (abs(p.vz) > 24f) {
                        p.vz = -p.vz * p.type.restitution
                        p.isGrounded = false
                    } else {
                        p.vz = 0f
                        p.isGrounded = true
                        p.angVel *= 0.25f
                        // Knocked over cones/hydrants stay tilted on ground
                        if (p.triggered || p.type == PropType.TRAFFIC_CONE) {
                            p.pitchDeg = if (abs(p.pitchDeg) > 15f) 78f else p.pitchDeg
                        } else {
                            p.pitchDeg = 0f
                            p.rollDeg = 0f
                        }
                    }
                }
            }
        }
    }

    fun applyExplosionImpulse(
        ex: Float,
        ey: Float,
        radius: Float,
        force: Float,
        props: MutableList<DynamicProp3D>,
        vehicles: List<VehicleEntity>,
        player: PlayerEntity
    ) {
        for (p in props) {
            val dx = p.x - ex
            val dy = p.y - ey
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (d < radius && d > 0.01f) {
                val falloff = 1f - (d / radius)
                val push = (force * falloff) / (p.type.mass * 0.04f).coerceAtLeast(1f)
                p.vx += (dx / d) * push
                p.vy += (dy / d) * push
                p.vz += 95f * falloff
                p.isGrounded = false
                p.angVel = (rng.nextFloat() - 0.5f) * 600f
            }
        }

        for (v in vehicles) {
            if (v.hp <= 0f || v.type.category == VehicleCategory.RAIL) continue
            val dx = v.x - ex
            val dy = v.y - ey
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (d < radius && d > 0.01f) {
                val falloff = 1f - (d / radius)
                val push = (force * falloff) / (v.mass * 0.002f).coerceAtLeast(1f)
                v.knockVx += (dx / d) * push
                v.knockVy += (dy / d) * push
                if (v.type.category == VehicleCategory.LAND) {
                    v.verticalVel = maxOf(v.verticalVel, 0.45f * falloff)
                    v.altitude = maxOf(v.altitude, 0.03f)
                }
            }
        }

        if (player.vehicleId == null) {
            val dx = player.x - ex
            val dy = player.y - ey
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (d < radius && d > 0.01f) {
                val falloff = 1f - (d / radius)
                player.vz = maxOf(player.vz, 62f * falloff)
                player.isGrounded = false
            }
        }
    }

    private fun sampleGroundElevationForProp(p: DynamicProp3D): Float {
        var floorY = 0f
        for (i in world.islands.indices) {
            if (world.islands[i].contains(p.x, p.y)) floorY = maxOf(floorY, 0.28f)
        }
        for (i in world.bridges.indices) {
            if (world.bridges[i].contains(p.x, p.y)) floorY = maxOf(floorY, 3.2f)
        }
        for (i in world.piers.indices) {
            if (world.piers[i].contains(p.x, p.y)) floorY = maxOf(floorY, 1.2f)
        }
        for (i in world.roads.indices) {
            val r = world.roads[i].rect
            if (r.contains(p.x, p.y, padding = 13f)) floorY = maxOf(floorY, 0.44f)
        }
        return floorY
    }

    private fun spawnImpactSparks(x: Float, y: Float, particles: MutableList<ParticleEntity>) {
        repeat(5) {
            val a = rng.nextFloat() * 6.28f
            val spd = 40f + rng.nextFloat() * 90f
            particles += ParticleEntity(
                x = x, y = y,
                vx = cos(a) * spd, vy = sin(a) * spd,
                radius = 3.5f, maxLife = 0.35f, life = 0.35f,
                color = Color(0xFFFFB300),
                zHeight = 6f, vz = 35f + rng.nextFloat() * 35f
            )
        }
    }

    private fun isBlockedByBuilding(x: Float, y: Float, padding: Float): Boolean {
        for (i in world.buildings.indices) {
            if (world.buildings[i].rect.contains(x, y, padding)) return true
        }
        return false
    }
}
