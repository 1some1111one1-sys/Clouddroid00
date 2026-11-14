package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class VehicleCategory {
    LAND,
    WATER,
    AIR,
    RAIL
}

enum class VehicleType(
    val displayName: String,
    val category: VehicleCategory,
    val width: Float,
    val length: Float,
    val maxSpeed: Float,
    val accel: Float,
    val turnRate: Float,
    val maxHp: Float,
    val primaryColor: Color,
    val accentColor: Color
) {
    SPORTS_CAR(
        displayName = "Vortex GT",
        category = VehicleCategory.LAND,
        width = 28f, length = 54f, maxSpeed = 480f, accel = 370f, turnRate = 3.2f, maxHp = 180f,
        primaryColor = Color(0xFFFF3D5A), accentColor = Color(0xFF1E293B)
    ),
    MUSCLE_CAR(
        displayName = "Stallion V8",
        category = VehicleCategory.LAND,
        width = 30f, length = 58f, maxSpeed = 435f, accel = 340f, turnRate = 2.8f, maxHp = 220f,
        primaryColor = Color(0xFFFFB300), accentColor = Color(0xFF111827)
    ),
    TAXI(
        displayName = "Metro Cab",
        category = VehicleCategory.LAND,
        width = 28f, length = 52f, maxSpeed = 365f, accel = 290f, turnRate = 2.9f, maxHp = 170f,
        primaryColor = Color(0xFFFACC15), accentColor = Color(0xFF0F172A)
    ),
    POLICE_CRUISER(
        displayName = "Metro PD Cruiser",
        category = VehicleCategory.LAND,
        width = 29f, length = 56f, maxSpeed = 450f, accel = 360f, turnRate = 3.1f, maxHp = 240f,
        primaryColor = Color(0xFFF8FAFC), accentColor = Color(0xFF0F172A)
    ),
    ARMORED_VAN(
        displayName = "Enforcer Van",
        category = VehicleCategory.LAND,
        width = 34f, length = 68f, maxSpeed = 320f, accel = 230f, turnRate = 2.1f, maxHp = 450f,
        primaryColor = Color(0xFF334155), accentColor = Color(0xFF0F172A)
    ),
    MOTORCYCLE(
        displayName = "Neon Moto",
        category = VehicleCategory.LAND,
        width = 16f, length = 38f, maxSpeed = 470f, accel = 420f, turnRate = 4.2f, maxHp = 110f,
        primaryColor = Color(0xFF00E5FF), accentColor = Color(0xFF0F172A)
    ),
    SPEEDBOAT(
        displayName = "Marlin Jetboat",
        category = VehicleCategory.WATER,
        width = 30f, length = 66f, maxSpeed = 410f, accel = 290f, turnRate = 2.5f, maxHp = 220f,
        primaryColor = Color(0xFFF1F5F9), accentColor = Color(0xFF00E5FF)
    ),
    CARGO_SHIP(
        displayName = "Harbor Freighter",
        category = VehicleCategory.WATER,
        width = 56f, length = 132f, maxSpeed = 240f, accel = 130f, turnRate = 1.2f, maxHp = 700f,
        primaryColor = Color(0xFF991B1B), accentColor = Color(0xFFE2E8F0)
    ),
    PROP_PLANE(
        displayName = "Bush Hopper",
        category = VehicleCategory.AIR,
        width = 74f, length = 62f, maxSpeed = 540f, accel = 260f, turnRate = 2.1f, maxHp = 260f,
        primaryColor = Color(0xFF38BDF8), accentColor = Color(0xFFF8FAFC)
    ),
    STUNT_JET(
        displayName = "Skyblade Jet",
        category = VehicleCategory.AIR,
        width = 82f, length = 72f, maxSpeed = 690f, accel = 380f, turnRate = 2.6f, maxHp = 320f,
        primaryColor = Color(0xFFE2E8F0), accentColor = Color(0xFFFF4D6D)
    ),
    METRO_TRAIN(
        displayName = "Metro Express",
        category = VehicleCategory.RAIL,
        width = 40f, length = 156f, maxSpeed = 380f, accel = 210f, turnRate = 1.0f, maxHp = 2000f,
        primaryColor = Color(0xFF1E293B), accentColor = Color(0xFF00E5FF)
    )
}

enum class WeaponType(
    val displayName: String,
    val shortName: String,
    val unlockCost: Int,
    val ammoPackCost: Int,
    val ammoPackAmount: Int,
    val damage: Float,
    val fireRateMs: Long,
    val range: Float,
    val spreadRad: Float,
    val pellets: Int,
    val projectileSpeed: Float,
    val explosiveRadius: Float,
    val knockback: Float,
    val accentColor: Color
) {
    BATON(
        displayName = "Stun Baton",
        shortName = "BATON",
        unlockCost = 0,
        ammoPackCost = 0,
        ammoPackAmount = 0,
        damage = 34f,
        fireRateMs = 360L,
        range = 62f,
        spreadRad = 0f,
        pellets = 1,
        projectileSpeed = 0f,
        explosiveRadius = 0f,
        knockback = 180f,
        accentColor = Color(0xFF38BDF8)
    ),
    PISTOL(
        displayName = "9mm Sidearm",
        shortName = "9MM",
        unlockCost = 0,
        ammoPackCost = 60,
        ammoPackAmount = 40,
        damage = 32f,
        fireRateMs = 260L,
        range = 420f,
        spreadRad = 0.04f,
        pellets = 1,
        projectileSpeed = 880f,
        explosiveRadius = 0f,
        knockback = 90f,
        accentColor = Color(0xFFE2E8F0)
    ),
    SMG(
        displayName = "Viper SMG",
        shortName = "SMG",
        unlockCost = 750,
        ammoPackCost = 140,
        ammoPackAmount = 90,
        damage = 19f,
        fireRateMs = 95L,
        range = 390f,
        spreadRad = 0.12f,
        pellets = 1,
        projectileSpeed = 900f,
        explosiveRadius = 0f,
        knockback = 55f,
        accentColor = Color(0xFFFFB300)
    ),
    SHOTGUN(
        displayName = "Breacher 12G",
        shortName = "12G",
        unlockCost = 1200,
        ammoPackCost = 160,
        ammoPackAmount = 24,
        damage = 22f,
        fireRateMs = 620L,
        range = 250f,
        spreadRad = 0.34f,
        pellets = 5,
        projectileSpeed = 820f,
        explosiveRadius = 0f,
        knockback = 210f,
        accentColor = Color(0xFFFF4D6D)
    ),
    PULSE_RIFLE(
        displayName = "Carbide Rifle",
        shortName = "RIFLE",
        unlockCost = 2200,
        ammoPackCost = 240,
        ammoPackAmount = 60,
        damage = 38f,
        fireRateMs = 150L,
        range = 560f,
        spreadRad = 0.03f,
        pellets = 1,
        projectileSpeed = 1120f,
        explosiveRadius = 0f,
        knockback = 110f,
        accentColor = Color(0xFF00E5FF)
    ),
    LAUNCHER(
        displayName = "Apex Launcher",
        shortName = "RPG",
        unlockCost = 4500,
        ammoPackCost = 500,
        ammoPackAmount = 6,
        damage = 165f,
        fireRateMs = 1150L,
        range = 600f,
        spreadRad = 0.02f,
        pellets = 1,
        projectileSpeed = 620f,
        explosiveRadius = 125f,
        knockback = 420f,
        accentColor = Color(0xFFFB7185)
    )
}

enum class NpcRole {
    CIVILIAN,
    RIVAL,
    OFFICER,
    SWAT
}

enum class PropType(
    val displayName: String,
    val radius: Float,
    val height: Float,
    val mass: Float,
    val restitution: Float,
    val primaryColor: Color,
    val accentColor: Color
) {
    TRAFFIC_CONE(
        displayName = "Traffic Cone",
        radius = 4.2f, height = 8.5f, mass = 12f, restitution = 0.45f,
        primaryColor = Color(0xFFF97316), accentColor = Color(0xFFF8FAFC)
    ),
    WOODEN_CRATE(
        displayName = "Supply Crate",
        radius = 8.5f, height = 14f, mass = 65f, restitution = 0.22f,
        primaryColor = Color(0xFFB45309), accentColor = Color(0xFF78350F)
    ),
    HYDRANT(
        displayName = "Fire Hydrant",
        radius = 4.8f, height = 9.5f, mass = 45f, restitution = 0.35f,
        primaryColor = Color(0xFFEF4444), accentColor = Color(0xFFE2E8F0)
    ),
    HAZARD_BARREL(
        displayName = "Fuel Drum",
        radius = 6.5f, height = 12.5f, mass = 50f, restitution = 0.32f,
        primaryColor = Color(0xFFDC2626), accentColor = Color(0xFFFACC15)
    )
}

class DynamicProp3D(
    val id: Int,
    val type: PropType,
    var x: Float,
    var y: Float,
    var zHeight: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var pitchDeg: Float = 0f,
    var yawDeg: Float = 0f,
    var rollDeg: Float = 0f,
    var angVel: Float = 0f,
    var isGrounded: Boolean = true,
    var triggered: Boolean = false,
    var geyserTimerSec: Float = 0f,
    var hp: Float = 60f
)

class PlayerEntity(
    var x: Float = 2160f,
    var y: Float = 2160f,
    var zHeight: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var isGrounded: Boolean = true,
    var coyoteTimeSec: Float = 0f,
    var angleRad: Float = 0f,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var armor: Float = 50f,
    var maxArmor: Float = 100f,
    var cash: Int = 1500,
    var respectXp: Int = 0,
    var wantedLevel: Int = 0,
    var heatScore: Float = 0f,
    var wantedCooldownSec: Float = 0f,
    var policeSightedTimer: Float = 0f,
    var equippedWeapon: WeaponType = WeaponType.PISTOL,
    val ownedWeapons: MutableSet<WeaponType> = mutableSetOf(WeaponType.BATON, WeaponType.PISTOL),
    val ammoMap: MutableMap<WeaponType, Int> = mutableMapOf(
        WeaponType.BATON to 999,
        WeaponType.PISTOL to 80,
        WeaponType.SMG to 0,
        WeaponType.SHOTGUN to 0,
        WeaponType.PULSE_RIFLE to 0,
        WeaponType.LAUNCHER to 0
    ),
    var vehicleId: Int? = null,
    var lastShotEpochMs: Long = 0L,
    var walkCycle: Float = 0f,
    var muzzleFlashTimerSec: Float = 0f
)

class VehicleEntity(
    val id: Int,
    val type: VehicleType,
    var x: Float,
    var y: Float,
    var angleRad: Float,
    var speed: Float = 0f,
    var zHeight: Float = 0f,
    var vz: Float = 0f,
    var knockVx: Float = 0f,
    var knockVy: Float = 0f,
    var pitchDeg: Float = 0f,
    var rollDeg: Float = 0f,
    var steerAngleDeg: Float = 0f,
    var isBraking: Boolean = false,
    var hp: Float = type.maxHp,
    var altitude: Float = 0f, // 0f ground, up to 1f high altitude for planes/stunts
    var verticalVel: Float = 0f,
    var isPlayerOwned: Boolean = false,
    var isTrafficControlled: Boolean = false,
    var isPoliceActive: Boolean = false,
    var isAmbientCruise: Boolean = false,
    var sirenOn: Boolean = false,
    var laneIndex: Int = 0,
    var trafficPanicTimerSec: Float = 0f,
    var desiredTrafficSpeed: Float = 140f,
    var railTrackDistance: Float = 0f,
    var railStopTimerSec: Float = 0f,
    var customColor: Color = type.primaryColor,
    var missionTag: String? = null,
    var damagedSmokeTimer: Float = 0f
) {
    val mass: Float
        get() = when (type) {
            VehicleType.MOTORCYCLE -> 320f
            VehicleType.SPORTS_CAR -> 1150f
            VehicleType.TAXI -> 1350f
            VehicleType.MUSCLE_CAR -> 1500f
            VehicleType.POLICE_CRUISER -> 1550f
            VehicleType.ARMORED_VAN -> 3400f
            VehicleType.SPEEDBOAT -> 1400f
            VehicleType.CARGO_SHIP -> 18000f
            VehicleType.PROP_PLANE -> 1250f
            VehicleType.STUNT_JET -> 2200f
            VehicleType.METRO_TRAIN -> 32000f
        }
}

enum class NpcBehaviorState {
    WALKING,
    LOITERING,
    DODGING_TRAFFIC,
    FLEEING,
    COMBAT
}

class NpcEntity(
    val id: Int,
    var role: NpcRole,
    var x: Float,
    var y: Float,
    var zHeight: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var angleRad: Float = 0f,
    var hp: Float = 60f,
    var targetNodeIdx: Int = 0,
    var panicTimerSec: Float = 0f,
    var lastAttackEpochMs: Long = 0L,
    var shirtColor: Color = Color(0xFF64748B),
    var pantsColor: Color = Color(0xFF1E293B),
    var behaviorState: NpcBehaviorState = NpcBehaviorState.WALKING,
    var stateTimerSec: Float = 0f,
    var strafeDir: Float = 1f,
    var hasCap: Boolean = false,
    var missionTag: String? = null,
    var walkPhase: Float = 0f
)

class ProjectileEntity(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var damage: Float,
    var remainingDist: Float,
    var isFromPlayer: Boolean,
    var explosiveRadius: Float,
    var knockback: Float,
    var color: Color
)

class ParticleEntity(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var maxLife: Float,
    var life: Float,
    var color: Color,
    var zHeight: Float = 6f,
    var vz: Float = 18f
)

data class SkidMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val alpha: Float = 0.45f
)

data class FloatingPopup(
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    var lifeSec: Float = 1.4f
)

enum class MissionGoalType {
    DELIVER_VEHICLE,
    WATER_CHECKPOINTS,
    AERIAL_RINGS,
    TRAIN_STATIONS,
    TAKEDOWN_VEHICLES,
    CLEAR_AND_ESCAPE
}

data class MissionDef(
    val id: String,
    val title: String,
    val district: DistrictId,
    val description: String,
    val goalType: MissionGoalType,
    val cashReward: Int,
    val xpReward: Int,
    val triggerX: Float,
    val triggerY: Float,
    val timeLimitSec: Float
)

data class ActiveMissionState(
    val def: MissionDef,
    val objectiveText: String,
    val targetX: Float,
    val targetY: Float,
    val progressCurrent: Int,
    val progressTotal: Int,
    val timeRemainingSec: Float
)

enum class GameOverlayStatus {
    NONE,
    PAUSED,
    BUSTED,
    KNOCKED_OUT,
    MISSION_PASSED,
    MISSION_FAILED
}

data class HudSnapshot(
    val cash: Int = 1500,
    val respectXp: Int = 0,
    val health: Int = 100,
    val armor: Int = 50,
    val wantedLevel: Int = 0,
    val wantedFlashing: Boolean = false,
    val currentDistrict: DistrictId = DistrictId.DOWNTOWN,
    val districtBannerText: String? = null,
    val stuntBannerText: String? = null,
    val equippedWeapon: WeaponType = WeaponType.PISTOL,
    val equippedAmmo: Int = 80,
    val ownedWeapons: Set<WeaponType> = setOf(WeaponType.BATON, WeaponType.PISTOL),
    val inVehicleName: String? = null,
    val inVehicleCategory: VehicleCategory? = null,
    val vehicleSpeedKmh: Int = 0,
    val vehicleAltitudeMeters: Int = 0,
    val vehicleHpPercent: Int = 100,
    val nearbyVehicleName: String? = null,
    val nearbyMissionTitle: String? = null,
    val nearbyMissionId: String? = null,
    val activeMission: ActiveMissionState? = null,
    val waypointX: Float? = null,
    val waypointY: Float? = null,
    val discoveredDistricts: Set<DistrictId> = setOf(DistrictId.DOWNTOWN),
    val collectedPackages: Set<String> = emptySet(),
    val completedMissions: Set<String> = emptySet(),
    val bestStuntMeters: Float = 0f,
    val totalDistanceMeters: Float = 0f,
    val overlayStatus: GameOverlayStatus = GameOverlayStatus.NONE,
    val lastResultTitle: String = "",
    val lastResultRewardCash: Int = 0,
    val playerX: Float = 2160f,
    val playerY: Float = 2160f,
    val playerAngleRad: Float = 0f,
    val cameraYawRad: Float = 0f,
    val clockText: String = "09:48",
    val isFiring: Boolean = false
)
