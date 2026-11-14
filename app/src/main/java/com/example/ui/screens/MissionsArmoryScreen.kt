package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.engine.GameEngine
import com.example.game.model.DistrictId
import com.example.game.model.HudSnapshot
import com.example.game.model.WeaponType
import com.example.game.world.CityWorldBuilder
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ConcreteSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSteel
import com.example.ui.theme.EmeraldCash
import com.example.ui.theme.MidnightAsphalt
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.TextSecondary

@Composable
fun MissionsArmoryScreen(
    initialTab: Int,
    engine: GameEngine,
    hud: HudSnapshot,
    onLaunchMission: (String) -> Unit,
    onEquipWeapon: (WeaponType) -> Unit,
    onBuyWeaponOrAmmo: (WeaponType) -> Boolean,
    onBuyArmor: () -> Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = listOf("CONTRACTS", "ARMORY", "EXPLORATION")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightAsphalt)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("SYNDICATE HUB", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Surface(
                color = ConcreteSlate,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldCash.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "$${hud.cash}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldCash,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ConcreteSlate,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricCyan
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == index) ElectricCyan else TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> ContractsTab(hud = hud, onLaunchMission = onLaunchMission)
            1 -> ArmoryTab(
                engine = engine,
                hud = hud,
                onEquipWeapon = onEquipWeapon,
                onBuyWeaponOrAmmo = onBuyWeaponOrAmmo,
                onBuyArmor = onBuyArmor
            )
            2 -> ExplorationStatsTab(hud = hud, totalPackages = engine.world.hiddenPackages.size)
        }
    }
}

@Composable
private fun ContractsTab(
    hud: HudSnapshot,
    onLaunchMission: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(CityWorldBuilder.missions) { mission ->
            val passed = mission.id in hud.completedMissions
            Card(
                colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
                border = BorderStroke(1.dp, if (passed) EmeraldCash.copy(alpha = 0.5f) else BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mission.district.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = mission.district.accentColor
                            )
                            Text(
                                text = mission.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        if (passed) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Passed", tint = EmeraldCash)
                        }
                    }

                    Text(
                        text = mission.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAYOUT: $${mission.cashReward} • +${mission.xpReward} XP",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonAmber
                        )
                        Button(
                            onClick = { onLaunchMission(mission.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = MidnightAsphalt
                            ),
                            modifier = Modifier.testTag("launch_${mission.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (passed) "REPLAY" else "LAUNCH", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmoryTab(
    engine: GameEngine,
    hud: HudSnapshot,
    onEquipWeapon: (WeaponType) -> Unit,
    onBuyWeaponOrAmmo: (WeaponType) -> Boolean,
    onBuyArmor: () -> Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedSteel),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Tactical Kevlar Vest", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Current Armor: ${hud.armor}/100", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Button(
                        onClick = { onBuyArmor() },
                        enabled = hud.cash >= 200 && hud.armor < 100,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt)
                    ) {
                        Text("REFILL $200", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        items(WeaponType.entries) { weapon ->
            val owned = weapon in hud.ownedWeapons
            val equipped = weapon == hud.equippedWeapon
            val ammo = engine.player.ammoMap[weapon] ?: 0
            Card(
                colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
                border = BorderStroke(1.dp, if (equipped) weapon.accentColor else BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = weapon.accentColor)
                            Column {
                                Text(weapon.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    text = "DMG ${weapon.damage.toInt()} • RNG ${weapon.range.toInt()}m" +
                                        if (weapon != WeaponType.BATON) " • AMMO $ammo" else " • MELEE",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (equipped) {
                            Surface(color = weapon.accentColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "EQUIPPED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = weapon.accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!owned) {
                            Button(
                                onClick = { onBuyWeaponOrAmmo(weapon) },
                                enabled = hud.cash >= weapon.unlockCost,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = MidnightAsphalt)
                            ) {
                                Text("UNLOCK $${weapon.unlockCost}", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onEquipWeapon(weapon) },
                                enabled = !equipped,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (equipped) "EQUIPPED" else "EQUIP", color = Color.White)
                            }
                            if (weapon.ammoPackCost > 0) {
                                Button(
                                    onClick = { onBuyWeaponOrAmmo(weapon) },
                                    enabled = hud.cash >= weapon.ammoPackCost,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = MidnightAsphalt)
                                ) {
                                    Text("+${weapon.ammoPackAmount} AMMO ($${weapon.ammoPackCost})", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorationStatsTab(hud: HudSnapshot, totalPackages: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
                border = BorderStroke(1.dp, BorderSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("CITY EXPLORATION RECORD", style = MaterialTheme.typography.labelLarge, color = NeonAmber)
                    StatLine("Districts Discovered", "${hud.discoveredDistricts.size} / ${DistrictId.entries.size}")
                    StatLine("Hidden Packages Collected", "${hud.collectedPackages.size} / $totalPackages")
                    StatLine("Contracts Completed", "${hud.completedMissions.size} / ${CityWorldBuilder.missions.size}")
                    StatLine("Best Ramp Stunt Distance", "${hud.bestStuntMeters.toInt()} m")
                    StatLine("Total Distance Traveled", "${hud.totalDistanceMeters.toInt()} m")
                    StatLine("Total Respect XP", "${hud.respectXp} XP")
                }
            }
        }

        items(DistrictId.entries) { d ->
            val found = d in hud.discoveredDistricts
            Card(
                colors = CardDefaults.cardColors(containerColor = ConcreteSlate),
                border = BorderStroke(1.dp, if (found) d.accentColor.copy(alpha = 0.6f) else BorderSlate),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (found) d.accentColor else TextSecondary
                        )
                        Column {
                            Text(d.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(d.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    Text(
                        text = if (found) "EXPLORED" else "UNDISCOVERED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (found) EmeraldCash else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}
