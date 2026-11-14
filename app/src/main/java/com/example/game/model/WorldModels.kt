package com.example.game.model

import androidx.compose.ui.graphics.Color
import kotlin.math.PI

enum class DistrictId(
    val displayName: String,
    val subtitle: String,
    val accentColor: Color,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    AIRSTRIP(
        displayName = "Metro Int'l Airport",
        subtitle = "Mainland Runways • Hangars & Stunt Jets",
        accentColor = Color(0xFF38BDF8),
        minX = 0f, minY = 0f, maxX = 1600f, maxY = 1600f
    ),
    DOWNTOWN(
        displayName = "Downtown Core",
        subtitle = "Neon Towers • Metro Central & Plaza",
        accentColor = Color(0xFF00E5FF),
        minX = 1600f, minY = 0f, maxX = 3300f, maxY = 2400f
    ),
    METRO_PARK(
        displayName = "Emerald Park",
        subtitle = "Botanical Loop • Emerald Station",
        accentColor = Color(0xFF10B981),
        minX = 3300f, minY = 0f, maxX = 4600f, maxY = 2200f
    ),
    INDUSTRIAL(
        displayName = "Foundry Yards",
        subtitle = "Stunt Ramps • Underground Armory",
        accentColor = Color(0xFFFFB300),
        minX = 0f, minY = 1600f, maxX = 1600f, maxY = 3400f
    ),
    HARBOR(
        displayName = "Harbor Docks",
        subtitle = "Cargo Piers • Speedboats & Ships",
        accentColor = Color(0xFF60A5FA),
        minX = 0f, minY = 3400f, maxX = 3000f, maxY = 4800f
    ),
    SUNSET_BEACH(
        displayName = "Sunset Palms",
        subtitle = "Ocean Boardwalk • Convertibles & Bay",
        accentColor = Color(0xFFFF4D6D),
        minX = 3000f, minY = 2200f, maxX = 4600f, maxY = 4800f
    ),
    VENTURAS_STRIP(
        displayName = "Venturas Strip",
        subtitle = "East Island • Grand Resorts & Neon Blvd",
        accentColor = Color(0xFFA855F7),
        minX = 4900f, minY = 0f, maxX = 7500f, maxY = 4800f
    ),
    DESERT_AIRFIELD(
        displayName = "Verdant Airfield",
        subtitle = "East Desert • Canyon Ramps & Runway 2",
        accentColor = Color(0xFFF59E0B),
        minX = 7500f, minY = 0f, maxX = 9600f, maxY = 4800f
    ),
    BAY_CITY(
        displayName = "San Fierro Bay",
        subtitle = "South Island • Suspension Bridge & Towers",
        accentColor = Color(0xFF2DD4BF),
        minX = 0f, minY = 4800f, maxX = 5000f, maxY = 9600f
    ),
    NAVAL_AIRPORT(
        displayName = "Easter Naval Air Base",
        subtitle = "South Island • Naval Runway 3 & Fleet",
        accentColor = Color(0xFFFB7185),
        minX = 5000f, minY = 4800f, maxX = 9300f, maxY = 9600f
    ),
    PALOMINO_BAY(
        displayName = "Palomino Creek & Pines",
        subtitle = "East Island • Pine Woods & Regional Airport 4",
        accentColor = Color(0xFF4ADE80),
        minX = 9300f, minY = 4800f, maxX = 12800f, maxY = 9600f
    ),
    CHILIAD_PEAKS(
        displayName = "Mt. Chiliad & Red County",
        subtitle = "South Alpine Ridge • Mountain Peaks & Canyon",
        accentColor = Color(0xFFE879F9),
        minX = 0f, minY = 9600f, maxX = 12800f, maxY = 12800f
    )
}

data class WorldRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
) {
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + w
    val bottom: Float get() = y + h
    val centerX: Float get() = x + w * 0.5f
    val centerY: Float get() = y + h * 0.5f

    fun contains(px: Float, py: Float, padding: Float = 0f): Boolean {
        return px >= left - padding && px <= right + padding &&
            py >= top - padding && py <= bottom + padding
    }

    fun intersects(otherLeft: Float, otherTop: Float, otherRight: Float, otherBottom: Float): Boolean {
        return left < otherRight && right > otherLeft && top < otherBottom && bottom > otherTop
    }
}

data class Building(
    val id: Int,
    val rect: WorldRect,
    val height: Float,
    val wallColor: Color,
    val roofColor: Color,
    val trimColor: Color,
    val landmarkLabel: String? = null,
    val hasHelipad: Boolean = false
)

data class MountainPeak(
    val x: Float,
    val y: Float,
    val radius: Float,
    val height: Float,
    val isDesertMesa: Boolean = false
)

data class RoadSegment(
    val rect: WorldRect,
    val isHorizontal: Boolean,
    val isAvenue: Boolean = false
)

data class StuntRamp(
    val id: Int,
    val name: String,
    val rect: WorldRect,
    val launchAngleRad: Float
)

data class HiddenPackage(
    val id: String,
    val x: Float,
    val y: Float,
    val hint: String
)

enum class ServicePointType {
    ARMORY,
    SPRAY_SHOP,
    HEALTH_PACK,
    ARMOR_VEST,
    BRIBE_STAR,
    DINER
}

data class WorldServicePoint(
    val id: String,
    val type: ServicePointType,
    val x: Float,
    val y: Float,
    val label: String,
    var cooldownSec: Float = 0f
)

data class MetroStation(
    val name: String,
    val x: Float,
    val y: Float,
    val trackIndex: Int
)

data class TrafficLane(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val angleRad: Float
)

data class CityWorld(
    val worldWidth: Float = 12800f,
    val worldHeight: Float = 12800f,
    val islands: List<WorldRect>,
    val bridges: List<WorldRect>,
    val buildings: List<Building>,
    val roads: List<RoadSegment>,
    val parks: List<WorldRect>,
    val waterZones: List<WorldRect>,
    val piers: List<WorldRect>,
    val sandZones: List<WorldRect>,
    val desertZones: List<WorldRect>,
    val runwayZones: List<WorldRect>,
    val railTrack: List<Pair<Float, Float>>,
    val metroStations: List<MetroStation>,
    val stuntRamps: List<StuntRamp>,
    val hiddenPackages: List<HiddenPackage>,
    val servicePoints: List<WorldServicePoint>,
    val trafficLanes: List<TrafficLane>,
    val sidewalkWalkNodes: List<Pair<Float, Float>>,
    val palmTrees: List<Pair<Float, Float>>,
    val pineTrees: List<Pair<Float, Float>> = emptyList(),
    val cacti: List<Pair<Float, Float>> = emptyList(),
    val mountainPeaks: List<MountainPeak> = emptyList(),
    val intersections: List<Pair<Float, Float>> = emptyList()
) {
    fun districtAt(x: Float, y: Float): DistrictId {
        return DistrictId.entries.firstOrNull { d ->
            x >= d.minX && x < d.maxX && y >= d.minY && y < d.maxY
        } ?: DistrictId.DOWNTOWN
    }

    fun isOnPier(x: Float, y: Float): Boolean {
        for (i in piers.indices) {
            if (piers[i].contains(x, y)) return true
        }
        return false
    }

    fun isOnBridge(x: Float, y: Float): Boolean {
        for (i in bridges.indices) {
            if (bridges[i].contains(x, y)) return true
        }
        return false
    }

    fun isWater(x: Float, y: Float): Boolean {
        if (isOnPier(x, y) || isOnBridge(x, y)) return false
        for (i in islands.indices) {
            if (islands[i].contains(x, y)) return false
        }
        return true
    }

    fun isRunway(x: Float, y: Float): Boolean {
        for (i in runwayZones.indices) {
            if (runwayZones[i].contains(x, y, padding = 10f)) return true
        }
        return false
    }

    fun nearestRoadPoint(x: Float, y: Float): Pair<Float, Float> {
        var bestX = x
        var bestY = y
        var bestDistSq = Float.MAX_VALUE
        for (i in roads.indices) {
            val r = roads[i].rect
            val cx = x.coerceIn(r.left + 20f, r.right - 20f)
            val cy = y.coerceIn(r.top + 20f, r.bottom - 20f)
            val dx = cx - x
            val dy = cy - y
            val d2 = dx * dx + dy * dy
            if (d2 < bestDistSq) {
                bestDistSq = d2
                bestX = cx
                bestY = cy
            }
        }
        return bestX to bestY
    }

    companion object {
        const val TWO_PI = (PI * 2.0).toFloat()
    }
}
