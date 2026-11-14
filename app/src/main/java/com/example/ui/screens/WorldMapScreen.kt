package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.model.DistrictId
import com.example.game.model.HudSnapshot
import com.example.game.model.ServicePointType
import com.example.game.model.VehicleCategory
import com.example.game.world.CityWorldBuilder
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ConcreteSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldCash
import com.example.ui.theme.MidnightAsphalt
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.hypot

@Composable
fun WorldMapScreen(
    engine: GameEngine,
    hud: HudSnapshot,
    onSetWaypoint: (Float?, Float?) -> Unit,
    onResumePlay: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val worldSize = engine.world.worldWidth
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gps_ring"
    )

    fun clampPan(px: Float, py: Float, z: Float, viewSize: IntSize): Pair<Float, Float> {
        val baseScale = minOf(viewSize.width, viewSize.height).coerceAtLeast(1) / worldSize
        val halfWorldPx = worldSize * 0.5f * baseScale * z
        val maxPanX = (halfWorldPx + viewSize.width * 0.15f).coerceAtLeast(120f)
        val maxPanY = (halfWorldPx + viewSize.height * 0.15f).coerceAtLeast(120f)
        return px.coerceIn(-maxPanX, maxPanX) to py.coerceIn(-maxPanY, maxPanY)
    }

    fun zoomAroundScreenPoint(targetZoom: Float, focusX: Float, focusY: Float) {
        val clampedZ = targetZoom.coerceIn(0.8f, 18.0f)
        if (clampedZ == zoom || canvasSize.width <= 1) return
        val baseScale = minOf(canvasSize.width, canvasSize.height) / worldSize
        val oldScale = baseScale * zoom
        val newScale = baseScale * clampedZ
        val oldOriginX = canvasSize.width * 0.5f + panX - worldSize * 0.5f * oldScale
        val oldOriginY = canvasSize.height * 0.5f + panY - worldSize * 0.5f * oldScale
        val worldFocusX = (focusX - oldOriginX) / oldScale
        val worldFocusY = (focusY - oldOriginY) / oldScale
        val rawPanX = focusX - canvasSize.width * 0.5f - (worldFocusX - worldSize * 0.5f) * newScale
        val rawPanY = focusY - canvasSize.height * 0.5f - (worldFocusY - worldSize * 0.5f) * newScale
        val (cx, cy) = clampPan(rawPanX, rawPanY, clampedZ, canvasSize)
        zoom = clampedZ
        panX = cx
        panY = cy
    }

    fun centerOnWorldPoint(wx: Float, wy: Float, targetZoom: Float) {
        val clampedZ = targetZoom.coerceIn(0.8f, 18.0f)
        val baseScale = minOf(canvasSize.width, canvasSize.height).coerceAtLeast(1) / worldSize
        val effScale = baseScale * clampedZ
        val rawPanX = -(wx - worldSize * 0.5f) * effScale
        val rawPanY = -(wy - worldSize * 0.5f) * effScale
        val (cx, cy) = clampPan(rawPanX, rawPanY, clampedZ, canvasSize)
        zoom = clampedZ
        panX = cx
        panY = cy
    }

    val waypointDistMeters = remember(hud.waypointX, hud.waypointY, hud.playerX, hud.playerY) {
        val wx = hud.waypointX
        val wy = hud.waypointY
        if (wx != null && wy != null) {
            hypot((wx - hud.playerX).toDouble(), (wy - hud.playerY).toDouble()).toInt()
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightAsphalt)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Satellite Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("SATELLITE ATLAS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = String.format("%.1fx", zoom),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${hud.currentDistrict.displayName} • Pinch/Double-tap to zoom • Tap to set GPS pin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hud.waypointX != null) {
                    TextButton(onClick = { onSetWaypoint(null, null) }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = SunsetCoral)
                        Spacer(Modifier.width(4.dp))
                        Text("CLEAR GPS", color = SunsetCoral)
                    }
                }
                Button(
                    onClick = onResumePlay,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt),
                    modifier = Modifier.testTag("map_play_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PLAY", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Interactive Satellite Map Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF041C32))
                .border(1.5.dp, BorderSlate, RoundedCornerShape(16.dp))
                .onSizeChanged { canvasSize = it }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(0.8f, 18.0f)
                            val baseScale = minOf(size.width, size.height).coerceAtLeast(1) / worldSize
                            val oldScale = baseScale * zoom
                            val newScale = baseScale * newZoom
                            val oldOriginX = size.width * 0.5f + panX - worldSize * 0.5f * oldScale
                            val oldOriginY = size.height * 0.5f + panY - worldSize * 0.5f * oldScale
                            val worldAtCentroidX = (centroid.x - oldOriginX) / oldScale
                            val worldAtCentroidY = (centroid.y - oldOriginY) / oldScale
                            val nextPanX = centroid.x + pan.x - size.width * 0.5f - (worldAtCentroidX - worldSize * 0.5f) * newScale
                            val nextPanY = centroid.y + pan.y - size.height * 0.5f - (worldAtCentroidY - worldSize * 0.5f) * newScale
                            val (cx, cy) = clampPan(nextPanX, nextPanY, newZoom, IntSize(size.width, size.height))
                            zoom = newZoom
                            panX = cx
                            panY = cy
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val nextZoom = if (zoom >= 10f) 1.0f else (zoom * 2.4f).coerceAtMost(18.0f)
                                if (nextZoom <= 1.05f) {
                                    zoom = 1.0f
                                    panX = 0f
                                    panY = 0f
                                } else {
                                    zoomAroundScreenPoint(nextZoom, tapOffset.x, tapOffset.y)
                                }
                            },
                            onTap = { tapOffset ->
                                val baseScale = minOf(size.width, size.height) / worldSize
                                val effScale = baseScale * zoom
                                val originX = size.width * 0.5f + panX - (worldSize * 0.5f * effScale)
                                val originY = size.height * 0.5f + panY - (worldSize * 0.5f * effScale)
                                val wx = ((tapOffset.x - originX) / effScale).coerceIn(80f, worldSize - 80f)
                                val wy = ((tapOffset.y - originY) / effScale).coerceIn(80f, worldSize - 80f)
                                onSetWaypoint(wx, wy)
                            }
                        )
                    }
            ) {
                val world = engine.world
                val baseScale = minOf(size.width, size.height) / world.worldWidth
                val effScale = baseScale * zoom
                val originX = size.width * 0.5f + panX - (world.worldWidth * 0.5f * effScale)
                val originY = size.height * 0.5f + panY - (world.worldHeight * 0.5f * effScale)

                // Inverse scale helper so icons & strokes stay sharp across 0.8x -> 18x zoom
                val markerScale = (1f / zoom.coerceAtLeast(1f)).coerceIn(0.14f, 1.0f)

                withTransform({
                    translate(originX, originY)
                    scale(effScale, effScale, Offset.Zero)
                }) {
                    // 1. Deep Ocean + Coastal Bathymetry Shelf Halos
                    drawRect(color = Color(0xFF06233E), size = Size(world.worldWidth, world.worldHeight))
                    for (island in world.islands) {
                        drawRect(
                            color = Color(0xFF0B3A5E),
                            topLeft = Offset(island.x - 160f, island.y - 160f),
                            size = Size(island.w + 320f, island.h + 320f)
                        )
                        drawRect(
                            color = Color(0xFF13557D),
                            topLeft = Offset(island.x - 55f, island.y - 55f),
                            size = Size(island.w + 110f, island.h + 110f)
                        )
                    }

                    // Subtle satellite coordinate grid lines
                    val gridStep = if (zoom >= 4f) 640f else 1600f
                    var gx = gridStep
                    while (gx < world.worldWidth) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, world.worldHeight),
                            strokeWidth = 10f * markerScale
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, gx),
                            end = Offset(world.worldWidth, gx),
                            strokeWidth = 10f * markerScale
                        )
                        gx += gridStep
                    }

                    // 2. Satellite Landmasses, Deserts, Beaches & Parks
                    for (island in world.islands) {
                        drawRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(island.x - 12f, island.y - 12f),
                            size = Size(island.w + 24f, island.h + 24f)
                        )
                        drawRect(
                            color = Color(0xFF2B382C),
                            topLeft = Offset(island.x, island.y),
                            size = Size(island.w, island.h)
                        )
                    }
                    for (dz in world.desertZones) {
                        drawRect(color = Color(0xFF916E49), topLeft = Offset(dz.x, dz.y), size = Size(dz.w, dz.h))
                    }
                    for (sz in world.sandZones) {
                        drawRect(color = Color(0xFFD6B580), topLeft = Offset(sz.x, sz.y), size = Size(sz.w, sz.h))
                    }
                    for (pk in world.parks) {
                        drawRect(color = Color(0xFF235D3A), topLeft = Offset(pk.x, pk.y), size = Size(pk.w, pk.h))
                    }

                    // 3. Topographic Satellite Mountain Peaks & Canyon Mesas
                    for (peak in world.mountainPeaks) {
                        if (peak.isDesertMesa) {
                            drawCircle(color = Color(0xFF6B4226), radius = peak.radius * 0.95f, center = Offset(peak.x + 28f, peak.y + 28f))
                            drawCircle(color = Color(0xFF8C5632), radius = peak.radius * 0.92f, center = Offset(peak.x, peak.y))
                            drawCircle(color = Color(0xFFA86A3D), radius = peak.radius * 0.58f, center = Offset(peak.x, peak.y))
                        } else {
                            drawCircle(color = Color(0xFF1F2E21), radius = peak.radius * 1.02f, center = Offset(peak.x + 36f, peak.y + 36f))
                            drawCircle(color = Color(0xFF334935), radius = peak.radius, center = Offset(peak.x, peak.y))
                            drawCircle(color = Color(0xFF4C574E), radius = peak.radius * 0.68f, center = Offset(peak.x, peak.y))
                            drawCircle(color = Color(0xFF69706B), radius = peak.radius * 0.42f, center = Offset(peak.x, peak.y))
                            if (peak.height >= 260f) {
                                drawCircle(color = Color(0xFFE5ECF4), radius = peak.radius * 0.22f, center = Offset(peak.x, peak.y))
                            }
                        }
                    }

                    // 4. Piers, Inter-Island Bridges & Airport Runways with Asphalt Markings
                    for (pier in world.piers) {
                        drawRect(color = Color(0xFF78716C), topLeft = Offset(pier.x, pier.y), size = Size(pier.w, pier.h))
                    }
                    for (br in world.bridges) {
                        drawRect(color = Color(0xFF64748B), topLeft = Offset(br.x - 6f, br.y - 6f), size = Size(br.w + 12f, br.h + 12f))
                        drawRect(color = Color(0xFF94A3B8), topLeft = Offset(br.x, br.y), size = Size(br.w, br.h))
                    }
                    for (rw in world.runwayZones) {
                        drawRect(color = Color(0xFF334155), topLeft = Offset(rw.x, rw.y), size = Size(rw.w, rw.h))
                        if (zoom >= 1.6f) {
                            drawLine(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.78f),
                                start = Offset(rw.x + 30f, rw.centerY),
                                end = Offset(rw.right - 30f, rw.centerY),
                                strokeWidth = 8f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 30f), 0f)
                            )
                        }
                    }

                    // 5. Asphalt Road Network + Sidewalk Borders + Lane Centerlines (at zoom >= 2.2x)
                    for (road in world.roads) {
                        val r = road.rect
                        drawRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(r.x - 10f, r.y - 10f),
                            size = Size(r.w + 20f, r.h + 20f)
                        )
                        drawRect(color = Color(0xFF262D38), topLeft = Offset(r.x, r.y), size = Size(r.w, r.h))
                        if (zoom >= 2.2f) {
                            if (road.isHorizontal) {
                                drawLine(
                                    color = Color(0xFFFACC15).copy(alpha = 0.75f),
                                    start = Offset(r.left, r.centerY),
                                    end = Offset(r.right, r.centerY),
                                    strokeWidth = 4f
                                )
                            } else {
                                drawLine(
                                    color = Color(0xFFFACC15).copy(alpha = 0.75f),
                                    start = Offset(r.centerX, r.top),
                                    end = Offset(r.centerX, r.bottom),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }

                    // 6. Satellite Foliage (Trees visible at street/neighborhood zoom)
                    if (zoom >= 1.8f) {
                        for ((tx, ty) in world.palmTrees) {
                            drawCircle(color = Color(0xFF14532D), radius = 14f, center = Offset(tx + 5f, ty + 5f))
                            drawCircle(color = Color(0xFF228B4E), radius = 13f, center = Offset(tx, ty))
                        }
                        for ((tx, ty) in world.pineTrees) {
                            drawCircle(color = Color(0xFF0B2F1D), radius = 15f, center = Offset(tx + 5f, ty + 5f))
                            drawCircle(color = Color(0xFF165B38), radius = 14f, center = Offset(tx, ty))
                        }
                    }

                    // 7. Buildings with Sun Drop-Shadows, Roof Colors & Helipad Rings
                    for (b in world.buildings) {
                        val r = b.rect
                        drawRect(
                            color = Color.Black.copy(alpha = 0.34f),
                            topLeft = Offset(r.x + 18f, r.y + 18f),
                            size = Size(r.w, r.h)
                        )
                        drawRect(
                            color = Color(0xFF364152),
                            topLeft = Offset(r.x, r.y),
                            size = Size(r.w, r.h)
                        )
                        if (zoom >= 2.0f) {
                            drawRect(
                                color = b.roofColor.copy(alpha = 0.78f),
                                topLeft = Offset(r.x + 8f, r.y + 8f),
                                size = Size((r.w - 16f).coerceAtLeast(4f), (r.h - 16f).coerceAtLeast(4f))
                            )
                        }
                        if (b.hasHelipad) {
                            drawCircle(
                                color = Color(0xFFFACC15),
                                radius = minOf(r.w, r.h) * 0.25f,
                                center = Offset(r.centerX, r.centerY),
                                style = Stroke(width = 6f)
                            )
                        }
                    }

                    // 8. 5-Island Metro Rail Loop & 12 Metro Stations
                    val track = world.railTrack
                    for (i in track.indices) {
                        val (x1, y1) = track[i]
                        val (x2, y2) = track[(i + 1) % track.size]
                        drawLine(
                            color = ElectricCyan.copy(alpha = 0.85f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = (28f * markerScale).coerceAtLeast(6f)
                        )
                    }
                    for (st in world.metroStations) {
                        val sSize = (84f * markerScale).coerceAtLeast(24f)
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(st.x - sSize * 0.5f, st.y - sSize * 0.5f),
                            size = Size(sSize, sSize)
                        )
                        drawRect(
                            color = ElectricCyan,
                            topLeft = Offset(st.x - sSize * 0.36f, st.y - sSize * 0.36f),
                            size = Size(sSize * 0.72f, sSize * 0.72f)
                        )
                    }

                    // 9. Live Moving Vehicles & Trains visible on Satellite Feed when zoomed in!
                    if (zoom >= 2.0f) {
                        for (v in engine.vehicles) {
                            if (v.hp <= 0f) continue
                            val vCol = when {
                                v.isPoliceActive -> Color(0xFF38BDF8)
                                v.type.category == VehicleCategory.RAIL -> ElectricCyan
                                v.type.category == VehicleCategory.AIR -> Color.White
                                else -> v.customColor
                            }
                            val vRad = when (v.type.category) {
                                VehicleCategory.RAIL -> 22f
                                VehicleCategory.AIR -> 20f
                                else -> 12f
                            } * markerScale.coerceAtLeast(0.45f)
                            drawCircle(color = vCol, radius = vRad, center = Offset(v.x, v.y))
                        }
                    }

                    // 10. District Borders & Labels (scale cleanly at low/medium zoom)
                    if (zoom < 5.5f) {
                        for (d in DistrictId.entries) {
                            val discovered = d in hud.discoveredDistricts
                            drawRect(
                                color = d.accentColor.copy(alpha = if (discovered) 0.28f else 0.14f),
                                topLeft = Offset(d.minX, d.minY),
                                size = Size(d.maxX - d.minX, d.maxY - d.minY),
                                style = Stroke(width = 24f * markerScale)
                            )
                            val label = textMeasurer.measure(
                                text = d.displayName.uppercase(),
                                style = TextStyle(
                                    color = if (discovered) Color.White else TextSecondary,
                                    fontSize = (210f * markerScale).coerceIn(48f, 220f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            drawText(
                                textLayoutResult = label,
                                topLeft = Offset(d.minX + 100f, d.minY + 100f)
                            )
                        }
                    }

                    // 11. Armories, Spray Shops, Diners & Mission Markers
                    for (sp in world.servicePoints) {
                        if (sp.type == ServicePointType.ARMORY ||
                            sp.type == ServicePointType.SPRAY_SHOP ||
                            sp.type == ServicePointType.DINER
                        ) {
                            val col = when (sp.type) {
                                ServicePointType.ARMORY -> ElectricCyan
                                ServicePointType.SPRAY_SHOP -> NeonAmber
                                else -> Color(0xFFFB923C)
                            }
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.65f),
                                radius = (68f * markerScale).coerceAtLeast(18f),
                                center = Offset(sp.x, sp.y)
                            )
                            drawCircle(
                                color = col,
                                radius = (52f * markerScale).coerceAtLeast(14f),
                                center = Offset(sp.x, sp.y)
                            )
                        }
                    }

                    for (m in CityWorldBuilder.missions) {
                        val done = m.id in hud.completedMissions
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.7f),
                            radius = (86f * markerScale).coerceAtLeast(22f),
                            center = Offset(m.triggerX, m.triggerY)
                        )
                        drawCircle(
                            color = if (done) EmeraldCash else NeonAmber,
                            radius = (66f * markerScale).coerceAtLeast(17f),
                            center = Offset(m.triggerX, m.triggerY)
                        )
                    }

                    // 12. GPS Waypoint Pin & Route Vector Line from Player
                    val wx = hud.waypointX
                    val wy = hud.waypointY
                    if (wx != null && wy != null) {
                        drawLine(
                            color = SunsetCoral.copy(alpha = 0.75f),
                            start = Offset(hud.playerX, hud.playerY),
                            end = Offset(wx, wy),
                            strokeWidth = (24f * markerScale).coerceAtLeast(6f),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(80f * markerScale, 55f * markerScale),
                                0f
                            )
                        )
                        drawCircle(
                            color = SunsetCoral,
                            radius = (110f * markerScale).coerceAtLeast(24f),
                            center = Offset(wx, wy),
                            style = Stroke(width = (26f * markerScale).coerceAtLeast(6f))
                        )
                        drawCircle(
                            color = SunsetCoral,
                            radius = (46f * markerScale).coerceAtLeast(11f),
                            center = Offset(wx, wy)
                        )
                    }

                    // 13. Pulsing Directional Player Location Indicator ("YOU")
                    val pCenter = Offset(hud.playerX, hud.playerY)
                    val baseRing = (165f * markerScale).coerceAtLeast(30f)
                    val expandRadius = baseRing * (0.7f + pulseProgress * 1.9f)
                    val pulseAlpha = (1f - pulseProgress) * 0.75f

                    // Expanding Radar Ping
                    drawCircle(
                        color = ElectricCyan.copy(alpha = pulseAlpha),
                        radius = expandRadius,
                        center = pCenter,
                        style = Stroke(width = (22f * markerScale).coerceAtLeast(4f))
                    )
                    // Translucent Heading Field-of-View Cone
                    val headingDeg = (hud.playerAngleRad * 180f / PI).toFloat()
                    val coneSize = (360f * markerScale).coerceAtLeast(65f)
                    drawArc(
                        color = ElectricCyan.copy(alpha = 0.28f),
                        startAngle = headingDeg - 26f,
                        sweepAngle = 52f,
                        useCenter = true,
                        topLeft = Offset(pCenter.x - coneSize, pCenter.y - coneSize),
                        size = Size(coneSize * 2f, coneSize * 2f)
                    )
                    // Outer White Halo + Directional Navigation Arrowhead
                    val arrowLen = (128f * markerScale).coerceAtLeast(22f)
                    val arrowWing = (82f * markerScale).coerceAtLeast(14f)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.7f),
                        radius = arrowLen * 0.92f,
                        center = pCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = arrowLen * 0.76f,
                        center = pCenter
                    )
                    withTransform({
                        rotate(degrees = headingDeg, pivot = pCenter)
                    }) {
                        val gpsArrow = Path().apply {
                            moveTo(pCenter.x + arrowLen, pCenter.y)
                            lineTo(pCenter.x - arrowWing * 0.65f, pCenter.y - arrowWing * 0.72f)
                            lineTo(pCenter.x - arrowWing * 0.25f, pCenter.y)
                            lineTo(pCenter.x - arrowWing * 0.65f, pCenter.y + arrowWing * 0.72f)
                            close()
                        }
                        drawPath(gpsArrow, color = ElectricCyan)
                    }
                }
            }

            // Top-Left Satellite Telemetry Pill (Live Coordinates + Scale + Waypoint Distance)
            Surface(
                color = MidnightAsphalt.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GPS: ${hud.playerX.toInt()}E, ${hud.playerY.toInt()}S",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    if (waypointDistMeters != null) {
                        Text(
                            text = "PIN DIST: ${waypointDistMeters}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetCoral
                        )
                    }
                }
            }

            // Right-Side Vertical Satellite Zoom & Player-Locator Controls (48dp+ touch targets)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapControlCircleButton(
                    icon = Icons.Default.Add,
                    desc = "Zoom In",
                    tag = "map_zoom_in_button",
                    tint = Color.White,
                    onClick = {
                        zoomAroundScreenPoint(
                            zoom * 1.65f,
                            canvasSize.width * 0.5f,
                            canvasSize.height * 0.5f
                        )
                    }
                )
                MapControlCircleButton(
                    icon = Icons.Default.Remove,
                    desc = "Zoom Out",
                    tag = "map_zoom_out_button",
                    tint = Color.White,
                    onClick = {
                        zoomAroundScreenPoint(
                            zoom / 1.65f,
                            canvasSize.width * 0.5f,
                            canvasSize.height * 0.5f
                        )
                    }
                )
                Spacer(Modifier.height(4.dp))
                MapControlCircleButton(
                    icon = Icons.Default.MyLocation,
                    desc = "Center on Player Location",
                    tag = "map_center_player_button",
                    tint = ElectricCyan,
                    onClick = {
                        val targetZ = zoom.coerceAtLeast(4.5f)
                        centerOnWorldPoint(hud.playerX, hud.playerY, targetZ)
                    }
                )
                MapControlCircleButton(
                    icon = Icons.Default.Public,
                    desc = "Full Archipelago Overview",
                    tag = "map_overview_button",
                    tint = NeonAmber,
                    onClick = {
                        zoom = 1.0f
                        panX = 0f
                        panY = 0f
                    }
                )
            }
        }

        // Legend Row
        Surface(
            color = ConcreteSlate,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MapLegendDot(ElectricCyan, "You / Armory")
                MapLegendDot(NeonAmber, "Contracts / Spray")
                MapLegendDot(Color(0xFFFB923C), "Diners")
                MapLegendDot(EmeraldCash, "Passed")
                MapLegendDot(SunsetCoral, "GPS Pin")
            }
        }
    }
}

@Composable
private fun MapControlCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tag: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MidnightAsphalt.copy(alpha = 0.88f),
        border = BorderStroke(1.5.dp, BorderSlate),
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
