package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ControlConfigEntity
import com.example.data.ControlPreset
import com.example.data.GameRepository
import com.example.game.engine.GameEngine
import com.example.game.model.GameOverlayStatus
import com.example.game.model.HudSnapshot
import com.example.game.model.WeaponType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    TITLE,
    PLAYING,
    MAP,
    MISSIONS_ARMORY,
    CUSTOMIZE_CONTROLS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(AppDatabase.getInstance(application).saveDao())
    val engine = GameEngine()

    private val _currentScreen = MutableStateFlow(AppScreen.TITLE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _hudState = MutableStateFlow(HudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    private val _frameTick = MutableStateFlow(0L)
    val frameTick: StateFlow<Long> = _frameTick.asStateFlow()

    private val _armoryTabInitial = MutableStateFlow(0)
    val armoryTabInitial: StateFlow<Int> = _armoryTabInitial.asStateFlow()

    val controlConfig: StateFlow<ControlConfigEntity> = repository.controlConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ControlConfigEntity())

    private var hasInitializedSave = false
    private var hudSyncAccumulator = 0f
    private var autoSaveAccumulator = 0f

    init {
        engine.onOpenArmoryRequested = {
            _armoryTabInitial.value = 1
            engine.overlayStatus = GameOverlayStatus.PAUSED
            _currentScreen.value = AppScreen.MISSIONS_ARMORY
        }
        engine.onSaveRequested = {
            persistCurrentSave()
        }

        viewModelScope.launch {
            val saved = repository.loadPlayerSave()
            val ctrl = repository.loadControlConfig()
            engine.applySave(saved)
            engine.autoAimEnabled = ctrl.autoAimEnabled
            _hudState.value = engine.snapshotHud()
            hasInitializedSave = true
        }

        // Collect control config updates to keep autoAim in sync
        viewModelScope.launch {
            controlConfig.collect { cfg ->
                engine.autoAimEnabled = cfg.autoAimEnabled
            }
        }

        // Main Game Simulation Loop (respects 30fps Battery Saver vs 60fps Smooth)
        viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                val lowPower = controlConfig.value.lowPower30Fps
                val targetFrameMs = if (lowPower) 33L else 16L
                if (_currentScreen.value == AppScreen.PLAYING &&
                    engine.overlayStatus == GameOverlayStatus.NONE
                ) {
                    val nowNs = System.nanoTime()
                    val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                    lastNs = nowNs

                    engine.step(dt)
                    _frameTick.value = _frameTick.value + 1L

                    hudSyncAccumulator += dt
                    if (hudSyncAccumulator >= 0.08f) {
                        hudSyncAccumulator = 0f
                        _hudState.value = engine.snapshotHud()
                    }

                    autoSaveAccumulator += dt
                    if (autoSaveAccumulator >= 15f) {
                        autoSaveAccumulator = 0f
                        persistCurrentSave()
                    }
                } else {
                    lastNs = System.nanoTime()
                }
                delay(targetFrameMs)
            }
        }
    }

    fun startFreeRoam() {
        engine.overlayStatus = GameOverlayStatus.NONE
        _hudState.value = engine.snapshotHud()
        _currentScreen.value = AppScreen.PLAYING
    }

    fun startNewCareer() {
        viewModelScope.launch {
            val fresh = repository.resetCareer()
            engine.applySave(fresh)
            engine.abandonActiveMission()
            engine.overlayStatus = GameOverlayStatus.NONE
            _hudState.value = engine.snapshotHud()
            _currentScreen.value = AppScreen.PLAYING
        }
    }

    fun navigateTo(screen: AppScreen, missionsTab: Int = 0) {
        if (screen != AppScreen.PLAYING && _currentScreen.value == AppScreen.PLAYING) {
            persistCurrentSave()
        }
        if (screen == AppScreen.PLAYING) {
            engine.overlayStatus = GameOverlayStatus.NONE
        }
        _armoryTabInitial.value = missionsTab
        _hudState.value = engine.snapshotHud()
        _currentScreen.value = screen
    }

    fun togglePause() {
        engine.overlayStatus = if (engine.overlayStatus == GameOverlayStatus.PAUSED) {
            GameOverlayStatus.NONE
        } else {
            persistCurrentSave()
            GameOverlayStatus.PAUSED
        }
        _hudState.value = engine.snapshotHud()
    }

    fun resumeGame() {
        engine.overlayStatus = GameOverlayStatus.NONE
        _hudState.value = engine.snapshotHud()
        _currentScreen.value = AppScreen.PLAYING
    }

    fun respawnPlayer() {
        engine.respawnAfterKnockout()
        _hudState.value = engine.snapshotHud()
    }

    fun dismissMissionResult() {
        engine.overlayStatus = GameOverlayStatus.NONE
        _hudState.value = engine.snapshotHud()
    }

    fun setMoveVector(x: Float, y: Float) {
        engine.inputMoveX = x
        engine.inputMoveY = y
    }

    fun setFiring(firing: Boolean) {
        engine.inputFiring = firing
    }

    fun setBoosting(boosting: Boolean) {
        engine.inputBoosting = boosting
    }

    fun enterOrExitVehicle() {
        engine.toggleEnterOrExitVehicle()
        _hudState.value = engine.snapshotHud()
    }

    fun jump() {
        engine.jumpPlayer()
    }

    fun cycleWeapon() {
        engine.cycleWeapon()
        _hudState.value = engine.snapshotHud()
    }

    fun equipWeapon(weapon: WeaponType) {
        engine.equipWeapon(weapon)
        _hudState.value = engine.snapshotHud()
    }

    fun buyWeaponOrAmmo(weapon: WeaponType): Boolean {
        val ok = engine.purchaseWeaponOrAmmo(weapon)
        _hudState.value = engine.snapshotHud()
        return ok
    }

    fun buyArmor(): Boolean {
        val ok = engine.buyArmorVest()
        _hudState.value = engine.snapshotHud()
        return ok
    }

    fun launchMission(missionId: String) {
        engine.startMission(missionId)
        _hudState.value = engine.snapshotHud()
        _currentScreen.value = AppScreen.PLAYING
    }

    fun abandonMission() {
        engine.abandonActiveMission()
        _hudState.value = engine.snapshotHud()
    }

    fun setWaypoint(x: Float?, y: Float?) {
        engine.waypointX = x
        engine.waypointY = y
        _hudState.value = engine.snapshotHud()
    }

    fun saveControlConfig(config: ControlConfigEntity) {
        viewModelScope.launch {
            repository.saveControlConfig(config)
        }
    }

    fun applyControlPreset(preset: ControlPreset) {
        val current = controlConfig.value
        val updated = ControlConfigEntity.forPreset(preset).copy(
            buttonScale = current.buttonScale,
            buttonOpacity = current.buttonOpacity,
            autoAimEnabled = current.autoAimEnabled,
            lowPower30Fps = current.lowPower30Fps
        )
        saveControlConfig(updated)
    }

    fun persistCurrentSave() {
        if (!hasInitializedSave) return
        val snapshot = engine.exportSave()
        viewModelScope.launch {
            repository.savePlayerProgress(snapshot)
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistCurrentSave()
    }
}
