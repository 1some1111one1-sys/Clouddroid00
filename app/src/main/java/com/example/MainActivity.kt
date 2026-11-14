package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.screens.ControlCustomizerScreen
import com.example.ui.screens.GameHudScreen
import com.example.ui.screens.MissionsArmoryScreen
import com.example.ui.screens.TitleScreen
import com.example.ui.screens.WorldMapScreen
import com.example.ui.theme.ViceMetroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViceMetroTheme {
                val vm: GameViewModel = viewModel()
                val screen by vm.currentScreen.collectAsStateWithLifecycle()
                val hud by vm.hudState.collectAsStateWithLifecycle()
                val frameTick by vm.frameTick.collectAsStateWithLifecycle()
                val controlConfig by vm.controlConfig.collectAsStateWithLifecycle()
                val armoryTab by vm.armoryTabInitial.collectAsStateWithLifecycle()
                var returnScreen by remember { mutableStateOf(AppScreen.TITLE) }

                when (screen) {
                    AppScreen.TITLE -> {
                        TitleScreen(
                            hud = hud,
                            onContinuePlay = { vm.startFreeRoam() },
                            onNewCareer = { vm.startNewCareer() },
                            onOpenMap = {
                                returnScreen = AppScreen.TITLE
                                vm.navigateTo(AppScreen.MAP)
                            },
                            onOpenMissions = {
                                returnScreen = AppScreen.TITLE
                                vm.navigateTo(AppScreen.MISSIONS_ARMORY, 0)
                            },
                            onOpenControls = {
                                returnScreen = AppScreen.TITLE
                                vm.navigateTo(AppScreen.CUSTOMIZE_CONTROLS)
                            }
                        )
                    }
                    AppScreen.PLAYING -> {
                        GameHudScreen(
                            engine = vm.engine,
                            frameTick = frameTick,
                            hud = hud,
                            controlConfig = controlConfig,
                            onMoveVector = vm::setMoveVector,
                            onFiringChanged = vm::setFiring,
                            onBoostingChanged = vm::setBoosting,
                            onJump = vm::jump,
                            onEnterExitVehicle = vm::enterOrExitVehicle,
                            onCycleWeapon = vm::cycleWeapon,
                            onLaunchMission = vm::launchMission,
                            onAbandonMission = vm::abandonMission,
                            onPauseToggle = vm::togglePause,
                            onResume = vm::resumeGame,
                            onRespawn = vm::respawnPlayer,
                            onDismissResult = vm::dismissMissionResult,
                            onOpenMap = {
                                returnScreen = AppScreen.PLAYING
                                vm.navigateTo(AppScreen.MAP)
                            },
                            onOpenMissions = {
                                returnScreen = AppScreen.PLAYING
                                vm.navigateTo(AppScreen.MISSIONS_ARMORY, 0)
                            },
                            onOpenArmory = {
                                returnScreen = AppScreen.PLAYING
                                vm.navigateTo(AppScreen.MISSIONS_ARMORY, 1)
                            },
                            onOpenControls = {
                                returnScreen = AppScreen.PLAYING
                                vm.navigateTo(AppScreen.CUSTOMIZE_CONTROLS)
                            },
                            onExitToTitle = { vm.navigateTo(AppScreen.TITLE) }
                        )
                    }
                    AppScreen.MAP -> {
                        WorldMapScreen(
                            engine = vm.engine,
                            hud = hud,
                            onSetWaypoint = vm::setWaypoint,
                            onResumePlay = { vm.resumeGame() },
                            onBack = { vm.navigateTo(returnScreen) }
                        )
                    }
                    AppScreen.MISSIONS_ARMORY -> {
                        MissionsArmoryScreen(
                            initialTab = armoryTab,
                            engine = vm.engine,
                            hud = hud,
                            onLaunchMission = vm::launchMission,
                            onEquipWeapon = vm::equipWeapon,
                            onBuyWeaponOrAmmo = vm::buyWeaponOrAmmo,
                            onBuyArmor = vm::buyArmor,
                            onBack = { vm.navigateTo(returnScreen) }
                        )
                    }
                    AppScreen.CUSTOMIZE_CONTROLS -> {
                        ControlCustomizerScreen(
                            initialConfig = controlConfig,
                            onSaveConfig = vm::saveControlConfig,
                            onBack = { vm.navigateTo(returnScreen) }
                        )
                    }
                }
            }
        }
    }
}
