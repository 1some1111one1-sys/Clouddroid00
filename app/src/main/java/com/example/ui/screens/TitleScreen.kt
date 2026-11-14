package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.DistrictId
import com.example.game.model.HudSnapshot
import com.example.game.render3d.GraphicsPipelineInfo
import com.example.game.world.CityWorldBuilder
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ConcreteSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSteel
import com.example.ui.theme.EmeraldCash
import com.example.ui.theme.MidnightAsphalt
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextSecondary

@Composable
fun TitleScreen(
    hud: HudSnapshot,
    onContinuePlay: () -> Unit,
    onNewCareer: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenControls: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightAsphalt)
    ) {
        // Full-bleed atmospheric backdrop
        Image(
            painter = painterResource(id = R.drawable.img_hero_banner),
            contentDescription = "Vice Metro city skyline artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MidnightAsphalt.copy(alpha = 0.78f),
                        0.48f to MidnightAsphalt.copy(alpha = 0.86f),
                        1f to MidnightAsphalt.copy(alpha = 0.96f)
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val isLandscapeWide = maxWidth >= 560.dp

            if (isLandscapeWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left column: Branding, hero banner card & sandbox features
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        TitleBrandHeader()
                        Spacer(Modifier.height(14.dp))
                        FeatureHighlightStrip()
                    }

                    // Right column: Save Profile + Play + Action Grid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        SaveAndActionPanel(
                            hud = hud,
                            onContinuePlay = onContinuePlay,
                            onOpenMissions = onOpenMissions,
                            onOpenMap = onOpenMap,
                            onOpenControls = onOpenControls,
                            onResetRequest = { confirmReset = true }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TitleBrandHeader()
                    SaveAndActionPanel(
                        hud = hud,
                        onContinuePlay = onContinuePlay,
                        onOpenMissions = onOpenMissions,
                        onOpenMap = onOpenMap,
                        onOpenControls = onOpenControls,
                        onResetRequest = { confirmReset = true }
                    )
                    FeatureHighlightStrip()
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = ConcreteSlate,
            title = { Text("Start New Career?", color = Color.White) },
            text = {
                Text(
                    "This resets your offline cash, weapons, contracts, and collectibles back to the beginning. Your custom control layout stays saved.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onNewCareer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral)
                ) {
                    Text("RESET SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TitleBrandHeader() {
    val context = LocalContext.current
    val gfxBadge = remember(context) { GraphicsPipelineInfo.pipelineBadge(context) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(50),
            color = ConcreteSlate.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "5 ISLANDS • ${DistrictId.entries.size} DISTRICTS • $gfxBadge",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }
        }

        Text(
            text = stringResource(id = R.string.app_name).uppercase(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 42.sp,
                letterSpacing = 2.sp
            ),
            color = Color.White
        )
        Text(
            text = "Cars, Planes, Ships, Metro Trains, 3D Physics & Dynamic Police Pursuits",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun SaveAndActionPanel(
    hud: HudSnapshot,
    onContinuePlay: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenControls: () -> Unit,
    onResetRequest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ConcreteSlate.copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LOCAL SAVE PROFILE", style = MaterialTheme.typography.labelLarge, color = NeonAmber)
                    Text("$${hud.cash}", style = MaterialTheme.typography.headlineMedium, color = EmeraldCash)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill("CONTRACTS", "${hud.completedMissions.size}/${CityWorldBuilder.missions.size}")
                    StatPill("DISTRICTS", "${hud.discoveredDistricts.size}/${DistrictId.entries.size}")
                    StatPill("PACKAGES", "${hud.collectedPackages.size}/32")
                    StatPill("RESPECT", "${hud.respectXp}")
                }
            }
        }

        Button(
            onClick = onContinuePlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("play_sandbox_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = MidnightAsphalt
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.play_continue),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenMissions,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("title_missions_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSlate),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ElevatedSteel.copy(alpha = 0.92f),
                    contentColor = NeonAmber
                )
            ) {
                Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("CONTRACTS", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onOpenMap,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("title_map_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSlate),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ElevatedSteel.copy(alpha = 0.92f),
                    contentColor = ElectricCyan
                )
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("CITY ATLAS", style = MaterialTheme.typography.labelLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenControls,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("title_controls_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSlate),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ElevatedSteel.copy(alpha = 0.92f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("CONTROLS", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onResetRequest,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("title_new_career_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ConcreteSlate.copy(alpha = 0.92f),
                    contentColor = SunsetCoral
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("NEW CAREER", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FeatureHighlightStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ConcreteSlate.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        FeatureBadge(Icons.Default.DirectionsCar, "Cars & Trains")
        FeatureBadge(Icons.Default.Flight, "Jets & Boats")
        FeatureBadge(Icons.Default.Shield, "5★ Wanted AI")
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun FeatureBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
