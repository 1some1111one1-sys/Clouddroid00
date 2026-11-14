package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ControlPreset(val label: String, val description: String) {
    TWIN_STICK("Twin-Stick Arcade", "Left analog pad + right action cluster"),
    LEFT_HANDED("Left-Handed", "Right analog pad + left action cluster"),
    CLASSIC_DPAD("Classic 4-Way", "Precision D-Pad buttons + right action cluster")
}

@Entity(tableName = "player_save")
data class PlayerSaveEntity(
    @PrimaryKey val id: Int = 1,
    val cash: Int = 1500,
    val respectXp: Int = 0,
    val health: Int = 100,
    val armor: Int = 50,
    val playerX: Float = 2160f,
    val playerY: Float = 2160f,
    val equippedWeapon: String = "PISTOL",
    val ownedWeaponsCsv: String = "BATON,PISTOL",
    val ammoCsv: String = "BATON:999,PISTOL:80,SMG:0,SHOTGUN:0,PULSE_RIFLE:0,LAUNCHER:0",
    val completedMissionsCsv: String = "",
    val discoveredDistrictsCsv: String = "DOWNTOWN",
    val collectedPackagesCsv: String = "",
    val bestStuntMeters: Float = 0f,
    val totalDistanceMeters: Float = 0f,
    val totalBustedCount: Int = 0,
    val lastSavedEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "control_config")
data class ControlConfigEntity(
    @PrimaryKey val id: Int = 1,
    val preset: String = ControlPreset.TWIN_STICK.name,
    val buttonScale: Float = 1.0f,
    val buttonOpacity: Float = 0.85f,
    val autoAimEnabled: Boolean = true,
    val lowPower30Fps: Boolean = false,
    // Normalized screen coordinates [0f..1f]
    val joystickX: Float = 0.14f,
    val joystickY: Float = 0.76f,
    val fireX: Float = 0.88f,
    val fireY: Float = 0.78f,
    val actionX: Float = 0.77f,
    val actionY: Float = 0.65f,
    val boostX: Float = 0.89f,
    val boostY: Float = 0.56f,
    val weaponX: Float = 0.75f,
    val weaponY: Float = 0.84f
) {
    companion object {
        fun forPreset(preset: ControlPreset): ControlConfigEntity {
            return when (preset) {
                ControlPreset.TWIN_STICK -> ControlConfigEntity(
                    preset = preset.name,
                    joystickX = 0.14f,
                    joystickY = 0.76f,
                    fireX = 0.88f,
                    fireY = 0.78f,
                    actionX = 0.76f,
                    actionY = 0.66f,
                    boostX = 0.89f,
                    boostY = 0.56f,
                    weaponX = 0.75f,
                    weaponY = 0.85f
                )
                ControlPreset.LEFT_HANDED -> ControlConfigEntity(
                    preset = preset.name,
                    joystickX = 0.86f,
                    joystickY = 0.76f,
                    fireX = 0.12f,
                    fireY = 0.78f,
                    actionX = 0.24f,
                    actionY = 0.66f,
                    boostX = 0.11f,
                    boostY = 0.56f,
                    weaponX = 0.25f,
                    weaponY = 0.85f
                )
                ControlPreset.CLASSIC_DPAD -> ControlConfigEntity(
                    preset = preset.name,
                    joystickX = 0.15f,
                    joystickY = 0.74f,
                    fireX = 0.88f,
                    fireY = 0.77f,
                    actionX = 0.76f,
                    actionY = 0.65f,
                    boostX = 0.89f,
                    boostY = 0.54f,
                    weaponX = 0.75f,
                    weaponY = 0.84f
                )
            }
        }
    }
}
