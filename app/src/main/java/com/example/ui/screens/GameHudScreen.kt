package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControlConfigEntity
import com.example.data.ControlPreset
import com.example.game.engine.GameEngine
import com.example.game.model.GameOverlayStatus
import com.example.game.model.HudSnapshot
import com.example.game.model.VehicleCategory
import com.example.game.model.WeaponType
import com.example.game.render.MinimapRenderer.renderMinimap
import com.example.game.render3d.WorldGLSurfaceView
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ConcreteSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSteel
import com.example.ui.theme.EmeraldCash
import com.example.ui.theme.MidnightAsphalt
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WantedCrimson
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameHudScreen(
    engine: GameEngine,
    frameTick: Long,
    hud: HudSnapshot,
    controlConfig: ControlConfigEntity,
    onMoveVector: (Float, Float) -> Unit,
    onFiringChanged: (Boolean) -> Unit,
    onBoostingChanged: (Boolean) -> Unit,
    onJump: () -> Unit,
    onEnterExitVehicle: () -> Unit,
    onCycleWeapon: () -> Unit,
    onLaunchMission: (String) -> Unit,
    onAbandonMission: () -> Unit,
    onPauseToggle: () -> Unit,
    onResume: () -> Unit,
    onRespawn: () -> Unit,
    onDismissResult: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenArmory: () -> Unit,
    onOpenControls: () -> Unit,
    onExitToTitle: () -> Unit
) {
    BackHandler {
        if (hud.overlayStatus == GameOverlayStatus.NONE) {
            onPauseToggle()
        } else {
            onResume()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Real-Time 3D OpenGL ES 3.0 Open-World Viewport (drag to orbit/aim camera!)
        WorldGLSurfaceView(
            engine = engine,
            modifier = Modifier
                .fillMaxSize()
                .testTag("world_3d_viewport")
        )

        // 2. Third-Person 3D Aiming Crosshair Reticle (when on foot)
        if (hud.inVehicleName == null && hud.overlayStatus == GameOverlayStatus.NONE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.48f
                val ringRadius = if (hud.isFiring) 24f else 18f
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = if (hud.isFiring) Color(0xFFFFB300) else Color.White,
                    radius = 3.2f,
                    center = Offset(cx, cy)
                )
            }
        }

        // 3. Top Safe-Area Vice-Style HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top-Left: Pink-Ringed Radar Minimap + District & Speedometer
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(CircleShape)
                        .clickable { onOpenMap() }
                        .testTag("minimap_button")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        @Suppress("UNUSED_EXPRESSION")
                        frameTick
                        renderMinimap(engine)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(
                        color = MidnightAsphalt.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, hud.currentDistrict.accentColor.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = hud.currentDistrict.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = hud.currentDistrict.accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudIconAction(
                            icon = Icons.Default.Map,
                            desc = "Open Metro Atlas",
                            tag = "hud_map_button",
                            tint = ElectricCyan,
                            onClick = onOpenMap
                        )
                        HudIconAction(
                            icon = Icons.Default.ShoppingBag,
                            desc = "Open Contracts and Armory",
                            tag = "hud_armory_button",
                            tint = NeonAmber,
                            onClick = onOpenArmory
                        )
                        HudIconAction(
                            icon = Icons.Default.Pause,
                            desc = "Pause Game",
                            tag = "hud_pause_button",
                            tint = Color.White,
                            onClick = onPauseToggle
                        )
                    }

                    if (hud.inVehicleName != null) {
                        Surface(
                            color = MidnightAsphalt.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(
                                    text = "${hud.vehicleSpeedKmh} KM/H",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = if (hud.inVehicleCategory == VehicleCategory.AIR) {
                                        "ALT ${hud.vehicleAltitudeMeters}m • ${hud.inVehicleName}"
                                    } else {
                                        "${hud.inVehicleName} • ${hud.vehicleHpPercent}%"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }
            }

            // Top-Right: Authentic Vice HUD (Clock, 8-digit Cash, Heart Health, Shield Armor, Weapon Box, Wanted Stars)
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Clock (cyan)
                    Text(
                        text = hud.clockText,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF7DD3FC)
                    )
                    // 8-Digit Cash (emerald)
                    Text(
                        text = String.format("$%08d", hud.cash.coerceAtLeast(0)),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = EmeraldCash
                    )
                    // Heart Health + Armor Shield
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hud.armor > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Armor",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "${hud.armor}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Health",
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format("%03d", hud.health),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF472B6)
                            )
                        }
                    }

                    // 5 Wanted Stars (glowing cyan/crimson)
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        val flashAlt = hud.wantedFlashing && ((frameTick / 12L) % 2L == 1L)
                        for (s in 1..5) {
                            val active = s <= hud.wantedLevel
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Wanted Star $s",
                                tint = when {
                                    !active -> Color.White.copy(alpha = 0.25f)
                                    flashAlt -> Color(0xFFF472B6)
                                    else -> Color(0xFF38BDF8)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Right Weapon Frame (clickable to cycle weapons!)
                Surface(
                    onClick = onCycleWeapon,
                    shape = RoundedCornerShape(12.dp),
                    color = MidnightAsphalt.copy(alpha = 0.72f),
                    border = BorderStroke(2.dp, Color(0xFFF472B6).copy(alpha = 0.85f)),
                    modifier = Modifier
                        .size(width = 68.dp, height = 74.dp)
                        .testTag("hud_weapon_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = "Cycle weapon",
                            tint = hud.equippedWeapon.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = hud.equippedWeapon.shortName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = if (hud.equippedWeapon == WeaponType.BATON) "MELEE" else "${hud.equippedAmmo}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ElectricCyan
                        )
                    }
                }
            }

            // Center-Top Mission & Discovery Toast Banners (compact top=4.dp so landscape center stays clear)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val am = hud.activeMission
                if (am != null) {
                    Surface(
                        color = MidnightAsphalt.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${am.def.title.uppercase()} • ${am.timeRemainingSec.toInt()}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber
                                )
                                Text(
                                    text = am.objectiveText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = onAbandonMission, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Abandon Mission", tint = SunsetCoral)
                            }
                        }
                    }
                } else if (hud.nearbyMissionId != null && hud.nearbyMissionTitle != null) {
                    Button(
                        onClick = { onLaunchMission(hud.nearbyMissionId) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = MidnightAsphalt),
                        modifier = Modifier.testTag("start_nearby_mission_button")
                    ) {
                        Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("START: ${hud.nearbyMissionTitle.uppercase()}", style = MaterialTheme.typography.labelLarge)
                    }
                }

                AnimatedVisibility(visible = hud.districtBannerText != null) {
                    Surface(color = ElectricCyan.copy(alpha = 0.92f), shape = RoundedCornerShape(50)) {
                        Text(
                            text = hud.districtBannerText.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MidnightAsphalt,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = hud.stuntBannerText != null) {
                    Surface(color = NeonAmber, shape = RoundedCornerShape(50)) {
                        Text(
                            text = hud.stuntBannerText.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MidnightAsphalt,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 4. Customizable Touch Controls Layer
        if (hud.overlayStatus == GameOverlayStatus.NONE) {
            CustomizableTouchOverlay(
                config = controlConfig,
                hud = hud,
                onMoveVector = onMoveVector,
                onFiringChanged = onFiringChanged,
                onBoostingChanged = onBoostingChanged,
                onJump = onJump,
                onEnterExitVehicle = onEnterExitVehicle,
                onCycleWeapon = onCycleWeapon
            )
        }

        // 5. Pause / Busted / Knocked Out / Mission Result Overlays
        if (hud.overlayStatus != GameOverlayStatus.NONE) {
            GameOverlayModal(
                status = hud.overlayStatus,
                hud = hud,
                onResume = onResume,
                onRespawn = onRespawn,
                onDismissResult = onDismissResult,
                onOpenMissions = onOpenMissions,
                onOpenControls = onOpenControls,
                onExitToTitle = onExitToTitle
            )
        }
    }
}

@Composable
private fun HudIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tag: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MidnightAsphalt.copy(alpha = 0.80f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderSlate),
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
private fun CustomizableTouchOverlay(
    config: ControlConfigEntity,
    hud: HudSnapshot,
    onMoveVector: (Float, Float) -> Unit,
    onFiringChanged: (Boolean) -> Unit,
    onBoostingChanged: (Boolean) -> Unit,
    onJump: () -> Unit,
    onEnterExitVehicle: () -> Unit,
    onCycleWeapon: () -> Unit
) {
    val density = LocalDensity.current
    val scale = config.buttonScale.coerceIn(0.75f, 1.4f)
    val opacity = config.buttonOpacity.coerceIn(0.35f, 1.0f)
    val isClassicDpad = config.preset == ControlPreset.CLASSIC_DPAD.name

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .alpha(opacity)
    ) {
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }

        // Movement Pad
        val joySizeDp = (132f * scale).dp
        val joyPx = with(density) { joySizeDp.toPx() }
        val jx = (config.joystickX * screenW - joyPx * 0.5f).coerceIn(12f, screenW - joyPx - 12f)
        val jy = (config.joystickY * screenH - joyPx * 0.5f).coerceIn(12f, screenH - joyPx - 12f)

        Box(
            modifier = Modifier
                .offset { IntOffset(jx.roundToInt(), jy.roundToInt()) }
                .size(joySizeDp)
        ) {
            if (isClassicDpad) {
                ClassicDpadControl(onMoveVector = onMoveVector)
            } else {
                AnalogJoystickControl(onMoveVector = onMoveVector)
            }
        }

        // Fire / Attack Button
        val fireSizeDp = (74f * scale).dp
        val firePx = with(density) { fireSizeDp.toPx() }
        val fx = (config.fireX * screenW - firePx * 0.5f).coerceIn(8f, screenW - firePx - 8f)
        val fy = (config.fireY * screenH - firePx * 0.5f).coerceIn(8f, screenH - firePx - 8f)

        HoldActionButton(
            modifier = Modifier
                .offset { IntOffset(fx.roundToInt(), fy.roundToInt()) }
                .size(fireSizeDp)
                .testTag("fire_button"),
            color = SunsetCoral,
            icon = Icons.Default.GpsFixed,
            label = if (hud.inVehicleName != null) "FIRE" else hud.equippedWeapon.shortName,
            onPressedChanged = onFiringChanged
        )

        // Boost / Sprint Button
        val boostSizeDp = (60f * scale).dp
        val boostPx = with(density) { boostSizeDp.toPx() }
        val bx = (config.boostX * screenW - boostPx * 0.5f).coerceIn(8f, screenW - boostPx - 8f)
        val by = (config.boostY * screenH - boostPx * 0.5f).coerceIn(8f, screenH - boostPx - 8f)

        HoldActionButton(
            modifier = Modifier
                .offset { IntOffset(bx.roundToInt(), by.roundToInt()) }
                .size(boostSizeDp)
                .testTag("boost_button"),
            color = NeonAmber,
            icon = Icons.Default.Bolt,
            label = if (hud.inVehicleName != null) "NITRO" else "RUN",
            onPressedChanged = onBoostingChanged
        )

        // Enter / Exit Vehicle Button
        val actSizeDp = (62f * scale).dp
        val actPx = with(density) { actSizeDp.toPx() }
        val ax = (config.actionX * screenW - actPx * 0.5f).coerceIn(8f, screenW - actPx - 8f)
        val ay = (config.actionY * screenH - actPx * 0.5f).coerceIn(8f, screenH - actPx - 8f)
        val canEnterOrExit = hud.inVehicleName != null || hud.nearbyVehicleName != null

        Surface(
            onClick = onEnterExitVehicle,
            shape = CircleShape,
            color = if (canEnterOrExit) ElectricCyan.copy(alpha = 0.9f) else ConcreteSlate.copy(alpha = 0.7f),
            border = BorderStroke(2.dp, if (canEnterOrExit) Color.White else BorderSlate),
            modifier = Modifier
                .offset { IntOffset(ax.roundToInt(), ay.roundToInt()) }
                .size(actSizeDp)
                .testTag("enter_exit_button")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Enter or exit vehicle",
                    tint = if (canEnterOrExit) MidnightAsphalt else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (hud.inVehicleName != null) "EXIT" else if (hud.nearbyVehicleName != null) "ENTER" else "RIDE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (canEnterOrExit) MidnightAsphalt else TextSecondary
                )
            }
        }

        // Jump Button (on-foot 3D gravity jump, placed cleanly above Action button for both right & left-handed presets)
        if (hud.inVehicleName == null) {
            val jumpSizeDp = (54f * scale).coerceAtLeast(48f).dp
            val jumpPx = with(density) { jumpSizeDp.toPx() }
            val gapPx = with(density) { 10.dp.toPx() }
            val jbx = (ax + (actPx - jumpPx) * 0.5f).coerceIn(8f, screenW - jumpPx - 8f)
            val jby = (ay - jumpPx - gapPx).coerceIn(8f, screenH - jumpPx - 8f)

            Surface(
                onClick = onJump,
                shape = CircleShape,
                color = ElectricCyan.copy(alpha = 0.85f),
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier
                    .offset { IntOffset(jbx.roundToInt(), jby.roundToInt()) }
                    .size(jumpSizeDp)
                    .testTag("jump_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Jump",
                        tint = MidnightAsphalt,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "JUMP",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MidnightAsphalt
                    )
                }
            }
        }

        // Weapon Cycle Pill
        val weapW = (84f * scale).dp
        val weapH = (48f * scale).dp
        val weapPxW = with(density) { weapW.toPx() }
        val weapPxH = with(density) { weapH.toPx() }
        val wx = (config.weaponX * screenW - weapPxW * 0.5f).coerceIn(8f, screenW - weapPxW - 8f)
        val wy = (config.weaponY * screenH - weapPxH * 0.5f).coerceIn(8f, screenH - weapPxH - 8f)

        Surface(
            onClick = onCycleWeapon,
            shape = RoundedCornerShape(14.dp),
            color = ConcreteSlate.copy(alpha = 0.88f),
            border = BorderStroke(1.5.dp, hud.equippedWeapon.accentColor),
            modifier = Modifier
                .offset { IntOffset(wx.roundToInt(), wy.roundToInt()) }
                .size(width = weapW, height = weapH)
                .testTag("weapon_cycle_button")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hud.equippedWeapon.shortName,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = hud.equippedWeapon.accentColor
                )
                Text(
                    text = if (hud.equippedWeapon == WeaponType.BATON) "MELEE" else "${hud.equippedAmmo}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AnalogJoystickControl(onMoveVector: (Float, Float) -> Unit) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var maxRadius by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(MidnightAsphalt.copy(alpha = 0.52f))
            .border(2.dp, ElectricCyan.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                maxRadius = (size.width * 0.36f).coerceAtLeast(10f)
                detectDragGestures(
                    onDragStart = { start ->
                        val center = Offset(size.width * 0.5f, size.height * 0.5f)
                        val delta = start - center
                        val dist = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                        val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                        knobOffset = clamped
                        onMoveVector(clamped.x / maxRadius, clamped.y / maxRadius)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMoveVector(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMoveVector(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        val dist = hypot(next.x.toDouble(), next.y.toDouble()).toFloat()
                        val clamped = if (dist > maxRadius) next * (maxRadius / dist) else next
                        knobOffset = clamped
                        onMoveVector(clamped.x / maxRadius, clamped.y / maxRadius)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(ElectricCyan.copy(alpha = 0.8f))
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun ClassicDpadControl(onMoveVector: (Float, Float) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DpadDirButton(
            modifier = Modifier.align(Alignment.TopCenter),
            icon = Icons.Default.KeyboardArrowUp,
            desc = "Move Forward",
            onHold = { pressed -> onMoveVector(0f, if (pressed) -1f else 0f) }
        )
        DpadDirButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            icon = Icons.Default.KeyboardArrowDown,
            desc = "Move Backward",
            onHold = { pressed -> onMoveVector(0f, if (pressed) 1f else 0f) }
        )
        DpadDirButton(
            modifier = Modifier.align(Alignment.CenterStart),
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            desc = "Move Left",
            onHold = { pressed -> onMoveVector(if (pressed) -1f else 0f, 0f) }
        )
        DpadDirButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            desc = "Move Right",
            onHold = { pressed -> onMoveVector(if (pressed) 1f else 0f, 0f) }
        )
    }
}

@Composable
private fun DpadDirButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onHold: (Boolean) -> Unit
) {
    var active by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) ElectricCyan else MidnightAsphalt.copy(alpha = 0.72f))
            .border(1.5.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        active = true
                        onHold(true)
                        tryAwaitRelease()
                        active = false
                        onHold(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (active) MidnightAsphalt else Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun HoldActionButton(
    modifier: Modifier,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onPressedChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (pressed) color else color.copy(alpha = 0.35f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressedChanged(true)
                        tryAwaitRelease()
                        pressed = false
                        onPressedChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.White
            )
        }
    }
}

@Composable
private fun GameOverlayModal(
    status: GameOverlayStatus,
    hud: HudSnapshot,
    onResume: () -> Unit,
    onRespawn: () -> Unit,
    onDismissResult: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenControls: () -> Unit,
    onExitToTitle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
            border = BorderStroke(1.5.dp, BorderSlate),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (status) {
                    GameOverlayStatus.PAUSED -> {
                        Text("GAME PAUSED", style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
                        Text(
                            "District: ${hud.currentDistrict.displayName} • Cash: $${hud.cash}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = onResume,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("RESUME 3D SANDBOX", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(onClick = onOpenMissions, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SportsMma, contentDescription = null, tint = NeonAmber)
                            Spacer(Modifier.width(6.dp))
                            Text("CONTRACTS & ARMORY", color = NeonAmber)
                        }
                        OutlinedButton(onClick = onOpenControls, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("CUSTOMIZE CONTROLS", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onExitToTitle,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.6f))
                        ) {
                            Text("SAVE & MAIN MENU", color = SunsetCoral)
                        }
                    }
                    GameOverlayStatus.BUSTED, GameOverlayStatus.KNOCKED_OUT -> {
                        Text(hud.lastResultTitle, style = MaterialTheme.typography.headlineLarge, color = WantedCrimson)
                        Text(
                            text = if (status == GameOverlayStatus.BUSTED) {
                                "Metro PD impounded your ride and processed a small precinct release fee."
                            } else {
                                "Vice Metro Medics patched you up at Downtown Clinic."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = onRespawn,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt)
                        ) {
                            Text("RESPAWN AT DOWNTOWN", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    GameOverlayStatus.MISSION_PASSED -> {
                        Text("CONTRACT PASSED!", style = MaterialTheme.typography.headlineLarge, color = EmeraldCash)
                        Text(hud.lastResultTitle, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Reward: +$${hud.lastResultRewardCash}", style = MaterialTheme.typography.headlineMedium, color = NeonAmber)
                        Button(
                            onClick = onDismissResult,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash, contentColor = MidnightAsphalt)
                        ) {
                            Text("CONTINUE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    GameOverlayStatus.MISSION_FAILED -> {
                        Text("CONTRACT FAILED", style = MaterialTheme.typography.headlineLarge, color = SunsetCoral)
                        Text(hud.lastResultTitle, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Button(
                            onClick = onDismissResult,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt)
                        ) {
                            Text("BACK TO SANDBOX", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    GameOverlayStatus.NONE -> Unit
                }
            }
        }
    }
}
