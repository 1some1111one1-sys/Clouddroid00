package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.game.engine.GameEngine
import com.example.game.model.ServicePointType
import com.example.game.world.CityWorldBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object MinimapRenderer {

    fun DrawScope.renderMinimap(engine: GameEngine) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = minOf(cx, cy)
        val scale = 0.115f

        val clipCircle = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        }

        // Rotate radar map opposite cameraYawRad so "up" on radar is always forward in 3D view!
        val camYawDeg = (-engine.cameraYawRad * 180f / PI).toFloat() - 90f

        clipPath(clipCircle) {
            // Ocean water background around the 3-island archipelago
            drawRect(color = Color(0xFF0E4772))

            val px = engine.player.x
            val py = engine.player.y

            rotate(camYawDeg, Offset(cx, cy)) {
                // 3 Island landmasses
                for (island in engine.world.islands) {
                    val left = cx + (island.x - px) * scale
                    val top = cy + (island.y - py) * scale
                    drawRect(
                        color = Color(0xFF1A222E),
                        topLeft = Offset(left, top),
                        size = Size(island.w * scale, island.h * scale)
                    )
                }

                // Desert expanses
                for (dz in engine.world.desertZones) {
                    val left = cx + (dz.x - px) * scale
                    val top = cy + (dz.y - py) * scale
                    drawRect(
                        color = Color(0xFF7D6141),
                        topLeft = Offset(left, top),
                        size = Size(dz.w * scale, dz.h * scale)
                    )
                }

                // Coastal sand beaches
                for (sz in engine.world.sandZones) {
                    val left = cx + (sz.x - px) * scale
                    val top = cy + (sz.y - py) * scale
                    drawRect(
                        color = Color(0xFFC29B61),
                        topLeft = Offset(left, top),
                        size = Size(sz.w * scale, sz.h * scale)
                    )
                }

                // Parks
                for (pk in engine.world.parks) {
                    val left = cx + (pk.x - px) * scale
                    val top = cy + (pk.y - py) * scale
                    drawRect(
                        color = Color(0xFF215E3C),
                        topLeft = Offset(left, top),
                        size = Size(pk.w * scale, pk.h * scale)
                    )
                }

                // Mountain Peaks & Canyon Mesas
                for (peak in engine.world.mountainPeaks) {
                    val sx = cx + (peak.x - px) * scale
                    val sy = cy + (peak.y - py) * scale
                    drawCircle(
                        color = if (peak.isDesertMesa) Color(0xFF8C5632) else Color(0xFF47554B),
                        radius = peak.radius * scale,
                        center = Offset(sx, sy)
                    )
                }

                // Airports & Runways
                for (rw in engine.world.runwayZones) {
                    val left = cx + (rw.x - px) * scale
                    val top = cy + (rw.y - py) * scale
                    drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(left, top),
                        size = Size(rw.w * scale, rw.h * scale)
                    )
                }

                // Piers & Inter-Island Bridges
                for (pier in engine.world.piers) {
                    val left = cx + (pier.x - px) * scale
                    val top = cy + (pier.y - py) * scale
                    drawRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(left, top),
                        size = Size(pier.w * scale, pier.h * scale)
                    )
                }
                for (br in engine.world.bridges) {
                    val left = cx + (br.x - px) * scale
                    val top = cy + (br.y - py) * scale
                    drawRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(left, top),
                        size = Size(br.w * scale, br.h * scale)
                    )
                }

                // Roads (crisp light-gray streets on dark radar)
                for (road in engine.world.roads) {
                    val r = road.rect
                    val left = cx + (r.x - px) * scale
                    val top = cy + (r.y - py) * scale
                    drawRect(
                        color = Color(0xFFD8DCE4),
                        topLeft = Offset(left, top),
                        size = Size(r.w * scale, r.h * scale)
                    )
                }

                // Rail track & Metro Stations
                val track = engine.world.railTrack
                for (i in track.indices) {
                    val (x1, y1) = track[i]
                    val (x2, y2) = track[(i + 1) % track.size]
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(cx + (x1 - px) * scale, cy + (y1 - py) * scale),
                        end = Offset(cx + (x2 - px) * scale, cy + (y2 - py) * scale),
                        strokeWidth = 1.8f
                    )
                }
                for (st in engine.world.metroStations) {
                    val sx = cx + (st.x - px) * scale
                    val sy = cy + (st.y - py) * scale
                    if (hypot((sx - cx).toDouble(), (sy - cy).toDouble()) < radius - 4f) {
                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(sx - 3f, sy - 3f),
                            size = Size(6f, 6f)
                        )
                    }
                }

                // Service points (Armory, Spray Shops & Eating Places)
                for (sp in engine.world.servicePoints) {
                    if (sp.type == ServicePointType.ARMORY ||
                        sp.type == ServicePointType.SPRAY_SHOP ||
                        sp.type == ServicePointType.DINER
                    ) {
                        val sx = cx + (sp.x - px) * scale
                        val sy = cy + (sp.y - py) * scale
                        if (hypot((sx - cx).toDouble(), (sy - cy).toDouble()) < radius - 4f) {
                            val col = when (sp.type) {
                                ServicePointType.ARMORY -> Color(0xFF00E5FF)
                                ServicePointType.SPRAY_SHOP -> Color(0xFFFFB300)
                                else -> Color(0xFFFB923C)
                            }
                            drawCircle(color = col, radius = 3.5f, center = Offset(sx, sy))
                        }
                    }
                }

                // Police blips
                for (v in engine.vehicles) {
                    if (!v.isPoliceActive || v.hp <= 0f) continue
                    val bx = cx + (v.x - px) * scale
                    val by = cy + (v.y - py) * scale
                    if (hypot((bx - cx).toDouble(), (by - cy).toDouble()) < radius - 4f) {
                        drawCircle(color = Color(0xFFFF2A54), radius = 3.8f, center = Offset(bx, by))
                    }
                }

                // Mission markers
                val am = engine.activeMission
                if (am != null) {
                    val mx = cx + (am.targetX - px) * scale
                    val my = cy + (am.targetY - py) * scale
                    drawCircle(color = Color(0xFFFFB300), radius = 5.5f, center = Offset(mx, my))
                } else {
                    for (m in CityWorldBuilder.missions) {
                        val mx = cx + (m.triggerX - px) * scale
                        val my = cy + (m.triggerY - py) * scale
                        if (hypot((mx - cx).toDouble(), (my - cy).toDouble()) < radius - 5f) {
                            drawCircle(color = Color(0xFFFFB300), radius = 4f, center = Offset(mx, my))
                        }
                    }
                }

                // Waypoint
                val wx = engine.waypointX
                val wy = engine.waypointY
                if (wx != null && wy != null) {
                    val mx = cx + (wx - px) * scale
                    val my = cy + (wy - py) * scale
                    drawCircle(color = Color(0xFFFF4D6D), radius = 5f, center = Offset(mx, my))
                }
            }

            // Player directional triangle at radar center
            val playerRelDeg = ((engine.player.angleRad - engine.cameraYawRad) * 180f / PI).toFloat() - 90f
            rotate(playerRelDeg, Offset(cx, cy)) {
                val tri = Path().apply {
                    moveTo(cx + 8f, cy)
                    lineTo(cx - 5f, cy - 5f)
                    lineTo(cx - 5f, cy + 5f)
                    close()
                }
                drawPath(tri, color = Color.White)
            }
        }

        // Iconic Vice neon-pink outer radar bezel + rotating North indicator dot
        drawCircle(
            color = Color(0xFFF472B6),
            radius = radius - 2f,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )
        val northRad = (camYawDeg - 90f) * (PI / 180f).toFloat()
        val nx = cx + cos(northRad) * (radius - 3f)
        val ny = cy + sin(northRad) * (radius - 3f)
        drawCircle(color = Color.Black, radius = 6.5f, center = Offset(nx, ny))
        drawCircle(color = Color(0xFFF472B6), radius = 4.5f, center = Offset(nx, ny))
    }
}
