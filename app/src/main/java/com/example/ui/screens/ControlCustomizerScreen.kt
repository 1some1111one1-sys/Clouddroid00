package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControlConfigEntity
import com.example.data.ControlPreset
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ConcreteSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSteel
import com.example.ui.theme.EmeraldCash
import com.example.ui.theme.MidnightAsphalt
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ControlCustomizerScreen(
    initialConfig: ControlConfigEntity,
    onSaveConfig: (ControlConfigEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var draft by remember(initialConfig) { mutableStateOf(initialConfig) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightAsphalt)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header bar
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
                    Text("CUSTOMIZE CONTROLS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Drag buttons below or pick a layout preset", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { draft = ControlConfigEntity.forPreset(ControlPreset.TWIN_STICK) },
                    modifier = Modifier.testTag("reset_controls_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = SunsetCoral)
                    Spacer(Modifier.width(4.dp))
                    Text("RESET", color = SunsetCoral)
                }
                Button(
                    onClick = {
                        onSaveConfig(draft)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash, contentColor = MidnightAsphalt),
                    modifier = Modifier.testTag("save_controls_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SAVE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Landscape-first side-by-side Interactive Preview + Settings Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0E1624))
                            .border(1.5.dp, BorderSlate, RoundedCornerShape(16.dp))
                    ) {
                        val density = LocalDensity.current
                        val boxW = with(density) { maxWidth.toPx() }
                        val boxH = with(density) { maxHeight.toPx() }
                        val scale = draft.buttonScale.coerceIn(0.75f, 1.4f)

                        Text(
                            text = "DRAG ANY CONTROL TO REPOSITION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        )

                        DraggableControlPreview(
                            normX = draft.joystickX,
                            normY = draft.joystickY,
                            boxW = boxW,
                            boxH = boxH,
                            sizeDp = (76f * scale).dp,
                            opacity = draft.buttonOpacity,
                            color = ElectricCyan,
                            label = if (draft.preset == ControlPreset.CLASSIC_DPAD.name) "D-PAD" else "MOVE",
                            onMoved = { nx, ny -> draft = draft.copy(joystickX = nx, joystickY = ny) }
                        )
                        DraggableControlPreview(
                            normX = draft.fireX,
                            normY = draft.fireY,
                            boxW = boxW,
                            boxH = boxH,
                            sizeDp = (58f * scale).dp,
                            opacity = draft.buttonOpacity,
                            color = SunsetCoral,
                            label = "FIRE",
                            icon = Icons.Default.GpsFixed,
                            onMoved = { nx, ny -> draft = draft.copy(fireX = nx, fireY = ny) }
                        )
                        DraggableControlPreview(
                            normX = draft.actionX,
                            normY = draft.actionY,
                            boxW = boxW,
                            boxH = boxH,
                            sizeDp = (50f * scale).dp,
                            opacity = draft.buttonOpacity,
                            color = ElectricCyan,
                            label = "RIDE",
                            icon = Icons.Default.DirectionsCar,
                            onMoved = { nx, ny -> draft = draft.copy(actionX = nx, actionY = ny) }
                        )
                        DraggableControlPreview(
                            normX = draft.boostX,
                            normY = draft.boostY,
                            boxW = boxW,
                            boxH = boxH,
                            sizeDp = (50f * scale).dp,
                            opacity = draft.buttonOpacity,
                            color = NeonAmber,
                            label = "NITRO",
                            icon = Icons.Default.Bolt,
                            onMoved = { nx, ny -> draft = draft.copy(boostX = nx, boostY = ny) }
                        )
                        DraggableControlPreview(
                            normX = draft.weaponX,
                            normY = draft.weaponY,
                            boxW = boxW,
                            boxH = boxH,
                            sizeDp = (50f * scale).dp,
                            opacity = draft.buttonOpacity,
                            color = Color(0xFFA855F7),
                            label = "GUN",
                            onMoved = { nx, ny -> draft = draft.copy(weaponX = nx, weaponY = ny) }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
                        border = BorderStroke(1.dp, BorderSlate),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                Text("CONTROL PRESET", style = MaterialTheme.typography.labelLarge, color = NeonAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlPreset.entries.forEach { preset ->
                        val selected = draft.preset == preset.name
                        FilterChip(
                            selected = selected,
                            onClick = {
                                draft = ControlConfigEntity.forPreset(preset).copy(
                                    buttonScale = draft.buttonScale,
                                    buttonOpacity = draft.buttonOpacity,
                                    autoAimEnabled = draft.autoAimEnabled,
                                    lowPower30Fps = draft.lowPower30Fps
                                )
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = MidnightAsphalt,
                                containerColor = ElevatedSteel,
                                labelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Size & Opacity sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Button Size: ${(draft.buttonScale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = draft.buttonScale,
                            onValueChange = { draft = draft.copy(buttonScale = it) },
                            valueRange = 0.75f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "HUD Opacity: ${(draft.buttonOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Slider(
                            value = draft.buttonOpacity,
                            onValueChange = { draft = draft.copy(buttonOpacity = it) },
                            valueRange = 0.4f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber)
                        )
                    }
                }

                // Assist & Budget Phone Performance Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Combat Auto-Aim", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Automatically locks onto nearby hostiles when firing", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(
                        checked = draft.autoAimEnabled,
                        onCheckedChange = { draft = draft.copy(autoAimEnabled = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MidnightAsphalt, checkedTrackColor = ElectricCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("30 FPS Battery Saver (Budget Phones)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Caps simulation at 30 Hz for cooler thermals & extra battery", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(
                        checked = draft.lowPower30Fps,
                        onCheckedChange = { draft = draft.copy(lowPower30Fps = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MidnightAsphalt, checkedTrackColor = NeonAmber)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DraggableControlPreview(
    normX: Float,
    normY: Float,
    boxW: Float,
    boxH: Float,
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    color: Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onMoved: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { sizeDp.toPx() }
    val pxX = (normX * boxW - sizePx * 0.5f).coerceIn(6f, (boxW - sizePx - 6f).coerceAtLeast(6f))
    val pxY = (normY * boxH - sizePx * 0.5f).coerceIn(6f, (boxH - sizePx - 6f).coerceAtLeast(6f))

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.28f),
        border = BorderStroke(2.dp, color),
        modifier = Modifier
            .offset { IntOffset(pxX.roundToInt(), pxY.roundToInt()) }
            .size(sizeDp)
            .alpha(opacity)
            .pointerInput(boxW, boxH) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val nextCenterX = ((normX * boxW) + dragAmount.x).coerceIn(sizePx * 0.5f, boxW - sizePx * 0.5f)
                    val nextCenterY = ((normY * boxH) + dragAmount.y).coerceIn(sizePx * 0.5f, boxH - sizePx * 0.5f)
                    onMoved((nextCenterX / boxW).coerceIn(0.06f, 0.94f), (nextCenterY / boxH).coerceIn(0.15f, 0.92f))
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White)
        }
    }
}
