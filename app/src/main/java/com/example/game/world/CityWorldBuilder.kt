package com.example.game.world

import androidx.compose.ui.graphics.Color
import com.example.game.model.Building
import com.example.game.model.CityWorld
import com.example.game.model.DistrictId
import com.example.game.model.HiddenPackage
import com.example.game.model.MetroStation
import com.example.game.model.MissionDef
import com.example.game.model.MissionGoalType
import com.example.game.model.MountainPeak
import com.example.game.model.NpcBehaviorState
import com.example.game.model.NpcEntity
import com.example.game.model.NpcRole
import com.example.game.model.RoadSegment
import com.example.game.model.ServicePointType
import com.example.game.model.StuntRamp
import com.example.game.model.TrafficLane
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleType
import com.example.game.model.WorldRect
import com.example.game.model.WorldServicePoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

object CityWorldBuilder {

    val missions: List<MissionDef> = listOf(
        MissionDef(
            id = "m1_harbor_run",
            title = "Harbor Courier",
            district = DistrictId.DOWNTOWN,
            description = "Grab the Stallion V8 outside Downtown Plaza and deliver it to Harbor Terminal before rivals box you in.",
            goalType = MissionGoalType.DELIVER_VEHICLE,
            cashReward = 1200,
            xpReward = 250,
            triggerX = 2180f,
            triggerY = 2020f,
            timeLimitSec = 110f
        ),
        MissionDef(
            id = "m2_coastal_contraband",
            title = "Bay Runner",
            district = DistrictId.HARBOR,
            description = "Hop in a Marlin Jetboat at Harbor Pier and thread all 5 glowing ocean buoys across the South Channel.",
            goalType = MissionGoalType.WATER_CHECKPOINTS,
            cashReward = 1800,
            xpReward = 350,
            triggerX = 1480f,
            triggerY = 3860f,
            timeLimitSec = 120f
        ),
        MissionDef(
            id = "m3_skyline_escape",
            title = "Skyline Escape",
            district = DistrictId.AIRSTRIP,
            description = "Take off from Metro Int'l Airport in a plane or jet, fly through 4 high-altitude rings, and touch back down.",
            goalType = MissionGoalType.AERIAL_RINGS,
            cashReward = 2600,
            xpReward = 500,
            triggerX = 520f,
            triggerY = 720f,
            timeLimitSec = 135f
        ),
        MissionDef(
            id = "m4_railroad_heist",
            title = "Tri-Island Express",
            district = DistrictId.METRO_PARK,
            description = "Board the Metro Express train at any of the 8 island stations and pilot it through 3 consecutive stops.",
            goalType = MissionGoalType.TRAIN_STATIONS,
            cashReward = 2200,
            xpReward = 450,
            triggerX = 3740f,
            triggerY = 1480f,
            timeLimitSec = 160f
        ),
        MissionDef(
            id = "m5_precision_pursuit",
            title = "Syndicate Sweep",
            district = DistrictId.SUNSET_BEACH,
            description = "Track down and disable 3 rival getaway sedans racing across Sunset Palms.",
            goalType = MissionGoalType.TAKEDOWN_VEHICLES,
            cashReward = 2800,
            xpReward = 550,
            triggerX = 3560f,
            triggerY = 2620f,
            timeLimitSec = 150f
        ),
        MissionDef(
            id = "m6_foundry_showdown",
            title = "Foundry Showdown",
            district = DistrictId.INDUSTRIAL,
            description = "Clear out the Foundry Yards crew, then lose the Metro PD heat to lock down the sector.",
            goalType = MissionGoalType.CLEAR_AND_ESCAPE,
            cashReward = 3500,
            xpReward = 700,
            triggerX = 960f,
            triggerY = 2360f,
            timeLimitSec = 160f
        )
    )

    fun buildWorld(): CityWorld {
        val roads = mutableListOf<RoadSegment>()
        val buildings = mutableListOf<Building>()
        val parks = mutableListOf<WorldRect>()
        val waterZones = mutableListOf<WorldRect>()
        val piers = mutableListOf<WorldRect>()
        val sandZones = mutableListOf<WorldRect>()
        val desertZones = mutableListOf<WorldRect>()
        val runwayZones = mutableListOf<WorldRect>()
        val palmTrees = mutableListOf<Pair<Float, Float>>()
        val pineTrees = mutableListOf<Pair<Float, Float>>()
        val cacti = mutableListOf<Pair<Float, Float>>()
        val mountainPeaks = mutableListOf<MountainPeak>()

        // 5 San Andreas-inspired Islands & Regions surrounded by navigable ocean channels
        val islands = listOf(
            // Island 1: Los Vice Mainland (Northwest - Downtown, Metro Airport, Foundry, Harbor, Sunset Palms)
            WorldRect(160f, 160f, 4060f, 3760f),
            // Island 2: Venturas Resort & Verdant Desert Airfield (Northeast)
            WorldRect(5160f, 220f, 4240f, 4280f),
            // Island 3: San Fierro Bay & Easter Naval Air Base (Mid-South)
            WorldRect(1300f, 5260f, 7200f, 4020f),
            // Island 4: Palomino Creek & Pines Regional Airport (East Coast)
            WorldRect(9620f, 4980f, 2920f, 4260f),
            // Island 5: Mt. Chiliad & Red County Highlands (Far South Ridge)
            WorldRect(1540f, 9940f, 10760f, 2580f)
        )

        // 7 Major Suspension & Causeway Bridges linking all 5 islands cleanly over water
        val bridges = listOf(
            // 0: East Bay Highway Bridge (Island 1 <-> Island 2 at y=1580)
            WorldRect(4180f, 1528f, 1020f, 104f),
            // 1: Venturas Causeway (Island 1 <-> Island 2 at y=2860)
            WorldRect(4180f, 2808f, 1020f, 104f),
            // 2: Golden Bay Suspension Bridge (Island 1 <-> Island 3 at x=2380)
            WorldRect(2328f, 3860f, 104f, 1450f),
            // 3: Easter Causeway Bridge (Island 2 <-> Island 3 at x=6800)
            WorldRect(6748f, 4440f, 104f, 860f),
            // 4: Palomino West Suspension Bridge (Island 3 <-> Island 4 at y=6680)
            WorldRect(8440f, 6628f, 1240f, 104f),
            // 5: Chiliad West Highway Bridge (Island 3 <-> Island 5 at x=3820)
            WorldRect(3768f, 9220f, 104f, 780f),
            // 6: Red County East Bridge (Island 4 <-> Island 5 at x=10480)
            WorldRect(10428f, 9180f, 104f, 820f)
        )

        // Ocean zones (12800 x 12800 archipelago)
        waterZones += WorldRect(0f, 0f, 12800f, 12800f)

        // Sandy coastlines & Desert expanses
        // Island 1 Sunset Beach
        sandZones += WorldRect(3820f, 2000f, 400f, 1920f)
        sandZones += WorldRect(2750f, 3760f, 1470f, 160f)
        // Island 2 Venturas East Coast & South Beach
        sandZones += WorldRect(5160f, 3920f, 2200f, 580f)
        // Island 3 San Fierro West Coast & Naval South Shore
        sandZones += WorldRect(1300f, 5260f, 420f, 3900f)
        sandZones += WorldRect(1720f, 8920f, 6780f, 360f)
        // Island 4 Palomino Beach Shore
        sandZones += WorldRect(9620f, 8780f, 2920f, 460f)
        // Island 5 Chiliad South Coastline
        sandZones += WorldRect(1540f, 12200f, 10760f, 320f)

        // Bone County Desert & Red County Canyon Mesas
        desertZones += WorldRect(7400f, 220f, 2000f, 4280f)
        desertZones += WorldRect(7800f, 9940f, 4500f, 2260f)

        // Harbor Piers & Naval Docks extending into ocean channels
        piers += WorldRect(640f, 3840f, 120f, 360f)
        piers += WorldRect(1420f, 3840f, 130f, 400f)
        piers += WorldRect(2040f, 3840f, 110f, 340f)
        // Island 2 Casino Marina Pier
        piers += WorldRect(5600f, 4420f, 140f, 360f)
        // Island 3 Naval Carrier Pier & Bay Ferry Pier
        piers += WorldRect(2000f, 5020f, 130f, 310f)
        piers += WorldRect(7200f, 9160f, 180f, 360f)
        // Island 4 Palomino Creek Marina Pier
        piers += WorldRect(11400f, 9120f, 140f, 380f)

        // 4 FULL AIRPORTS across the 5 Islands!
        // Airport 1: Metro Int'l Airport (Island 1 NW)
        runwayZones += WorldRect(180f, 380f, 1280f, 220f)
        runwayZones += WorldRect(180f, 640f, 600f, 110f)
        // Airport 2: Verdant Desert Airfield (Island 2 East)
        runwayZones += WorldRect(7750f, 520f, 1420f, 240f)
        runwayZones += WorldRect(7900f, 810f, 700f, 120f)
        // Airport 3: Easter Basin Naval Air Base (Island 3 East)
        runwayZones += WorldRect(5460f, 6020f, 1800f, 250f)
        runwayZones += WorldRect(5600f, 6340f, 840f, 130f)
        // Airport 4: Palomino Regional Airfield (Island 4 North)
        runwayZones += WorldRect(10100f, 5260f, 1850f, 220f)
        runwayZones += WorldRect(10250f, 5540f, 780f, 120f)

        val roadW = 104f
        val intersections = mutableListOf<Pair<Float, Float>>()

        // --- ISLAND 1 ROADS ---
        val xRoadCenters1 = listOf(460f, 1080f, 1740f, 2380f, 3020f, 3660f)
        val yRoadCenters1 = listOf(960f, 1580f, 2220f, 2860f, 3500f)

        for (ry in yRoadCenters1) {
            val endX = if (ry == 1580f || ry == 2860f) 5220f else if (ry > 1800f) 3840f else 4180f
            roads += RoadSegment(
                rect = WorldRect(160f, ry - roadW * 0.5f, endX - 160f, roadW),
                isHorizontal = true,
                isAvenue = (ry == 1580f || ry == 2220f || ry == 2860f)
            )
        }
        for (rx in xRoadCenters1) {
            val startY = if (rx < 1500f) 900f else 220f
            val endY = if (rx == 2380f) 5320f else 3860f
            roads += RoadSegment(
                rect = WorldRect(rx - roadW * 0.5f, startY, roadW, endY - startY),
                isHorizontal = false,
                isAvenue = (rx == 1740f || rx == 2380f)
            )
        }
        for (rx in xRoadCenters1) for (ry in yRoadCenters1) {
            if (rx >= 1080f && ry >= 960f) intersections += rx to ry
        }

        // --- ISLAND 2 ROADS ---
        val xRoadCenters2 = listOf(5520f, 6160f, 6800f, 7440f, 8180f, 8920f)
        val yRoadCenters2 = listOf(960f, 1580f, 2220f, 2860f, 3500f, 4140f)

        for (ry in yRoadCenters2) {
            val startX = if (ry == 1580f || ry == 2860f) 5160f else 5220f
            val endX = if (ry < 1200f) 9200f else 8980f
            roads += RoadSegment(
                rect = WorldRect(startX, ry - roadW * 0.5f, endX - startX, roadW),
                isHorizontal = true,
                isAvenue = (ry == 1580f || ry == 2860f)
            )
        }
        for (rx in xRoadCenters2) {
            val startY = if (rx > 7600f) 960f else 340f
            val endY = if (rx == 6800f) 5320f else 4340f
            roads += RoadSegment(
                rect = WorldRect(rx - roadW * 0.5f, startY, roadW, endY - startY),
                isHorizontal = false,
                isAvenue = (rx == 6160f || rx == 6800f)
            )
        }
        for (rx in xRoadCenters2) for (ry in yRoadCenters2) {
            if (ry >= 1580f && rx <= 8180f) intersections += rx to ry
        }

        // --- ISLAND 3 ROADS ---
        val xRoadCenters3 = listOf(1860f, 2380f, 3080f, 3820f, 4580f, 5360f, 6800f, 7740f)
        val yRoadCenters3 = listOf(5520f, 6680f, 7480f, 8280f, 8960f)

        for (ry in yRoadCenters3) {
            val endX = if (ry == 6680f) 9720f else if (ry > 8400f) 8300f else 8200f
            roads += RoadSegment(
                rect = WorldRect(1600f, ry - roadW * 0.5f, endX - 1600f, roadW),
                isHorizontal = true,
                isAvenue = (ry == 5520f || ry == 6680f || ry == 7480f)
            )
        }
        for (rx in xRoadCenters3) {
            val startY = if (rx > 5400f && rx != 6800f) 6680f else 5300f
            val endY = if (rx == 3820f) 10020f else 9080f
            roads += RoadSegment(
                rect = WorldRect(rx - roadW * 0.5f, startY, roadW, endY - startY),
                isHorizontal = false,
                isAvenue = (rx == 2380f || rx == 3820f || rx == 6800f)
            )
        }
        for (rx in xRoadCenters3) for (ry in yRoadCenters3) {
            if (rx in 2380f..6800f && ry in 6680f..8280f) intersections += rx to ry
        }

        // --- ISLAND 4 ROADS (Palomino Creek & Pines) ---
        val xRoadCenters4 = listOf(10480f, 11380f, 12180f)
        val yRoadCenters4 = listOf(5860f, 6680f, 7560f, 8540f)

        for (ry in yRoadCenters4) {
            val startX = if (ry == 6680f) 9620f else 9740f
            roads += RoadSegment(
                rect = WorldRect(startX, ry - roadW * 0.5f, 12360f - startX, roadW),
                isHorizontal = true,
                isAvenue = (ry == 6680f || ry == 7560f)
            )
        }
        for (rx in xRoadCenters4) {
            val endY = if (rx == 10480f) 10040f else 9060f
            roads += RoadSegment(
                rect = WorldRect(rx - roadW * 0.5f, 5780f, roadW, endY - 5780f),
                isHorizontal = false,
                isAvenue = (rx == 10480f)
            )
        }
        for (rx in xRoadCenters4) for (ry in yRoadCenters4) intersections += rx to ry

        // --- ISLAND 5 ROADS (Mt. Chiliad & Red County Highlands) ---
        val xRoadCenters5 = listOf(2240f, 3820f, 5600f, 7500f, 9120f, 10480f, 11800f)
        val yRoadCenters5 = listOf(10380f, 11320f, 12040f)

        for (ry in yRoadCenters5) {
            roads += RoadSegment(
                rect = WorldRect(1840f, ry - roadW * 0.5f, 10200f, roadW),
                isHorizontal = true,
                isAvenue = (ry == 10380f || ry == 11320f)
            )
        }
        for (rx in xRoadCenters5) {
            roads += RoadSegment(
                rect = WorldRect(rx - roadW * 0.5f, 9980f, roadW, 2240f),
                isHorizontal = false,
                isAvenue = (rx == 3820f || rx == 10480f)
            )
        }
        for (rx in xRoadCenters5) for (ry in yRoadCenters5) {
            if (ry == 10380f || ry == 11320f) intersections += rx to ry
        }

        // Parks & Botanical Forests across all 5 Islands
        parks += WorldRect(3120f, 300f, 480f, 580f)
        parks += WorldRect(3740f, 300f, 440f, 580f)
        parks += WorldRect(3120f, 1040f, 480f, 460f)
        parks += WorldRect(5580f, 1040f, 500f, 460f)
        parks += WorldRect(3150f, 5600f, 600f, 960f)
        parks += WorldRect(9820f, 7680f, 540f, 900f)
        parks += WorldRect(4200f, 10540f, 1200f, 680f)

        // Organic Biomes: Tropical Palms, Alpine Pines, and Desert Cacti
        val rng = Random(4207)
        for (i in 0 until 28) palmTrees += (3840f + rng.nextFloat() * 320f) to (2060f + rng.nextFloat() * 1680f)
        for (i in 0 until 22) palmTrees += (3160f + rng.nextFloat() * 960f) to (340f + rng.nextFloat() * 500f)
        for (i in 0 until 32) palmTrees += (6160f + (if (i % 2 == 0) -66f else 66f)) to (420f + i * 125f)
        for (i in 0 until 20) palmTrees += (5240f + rng.nextFloat() * 1900f) to (3980f + rng.nextFloat() * 340f)
        for (i in 0 until 26) palmTrees += (1380f + rng.nextFloat() * 280f) to (5400f + rng.nextFloat() * 3400f)
        for (i in 0 until 20) palmTrees += (3200f + rng.nextFloat() * 500f) to (5660f + rng.nextFloat() * 820f)
        for (i in 0 until 22) palmTrees += (9750f + rng.nextFloat() * 2500f) to (8820f + rng.nextFloat() * 320f)

        // Pine trees on Palomino Island & Mt. Chiliad Foothills
        for (i in 0 until 45) pineTrees += (9800f + rng.nextFloat() * 2550f) to (5980f + rng.nextFloat() * 2650f)
        for (i in 0 until 60) pineTrees += (1850f + rng.nextFloat() * 5600f) to (10120f + rng.nextFloat() * 1950f)

        // Saguaro Cacti in Bone County & Red County Desert
        for (i in 0 until 42) cacti += (7550f + rng.nextFloat() * 1700f) to (1100f + rng.nextFloat() * 3100f)
        for (i in 0 until 35) cacti += (7950f + rng.nextFloat() * 4100f) to (10120f + rng.nextFloat() * 1950f)

        // 3D Mountain Peaks & Red-Rock Canyon Mesas
        mountainPeaks += MountainPeak(2980f, 10860f, radius = 380f, height = 260f, isDesertMesa = false)
        mountainPeaks += MountainPeak(3340f, 11060f, radius = 260f, height = 185f, isDesertMesa = false)
        mountainPeaks += MountainPeak(4850f, 10840f, radius = 430f, height = 310f, isDesertMesa = false)
        mountainPeaks += MountainPeak(6620f, 10880f, radius = 360f, height = 240f, isDesertMesa = false)
        mountainPeaks += MountainPeak(8560f, 10860f, radius = 320f, height = 205f, isDesertMesa = true)
        mountainPeaks += MountainPeak(9820f, 10820f, radius = 340f, height = 215f, isDesertMesa = true)
        mountainPeaks += MountainPeak(11180f, 10860f, radius = 300f, height = 190f, isDesertMesa = true)
        mountainPeaks += MountainPeak(8550f, 3840f, radius = 250f, height = 145f, isDesertMesa = true)
        mountainPeaks += MountainPeak(9020f, 3360f, radius = 220f, height = 125f, isDesertMesa = true)
        mountainPeaks += MountainPeak(11820f, 7080f, radius = 270f, height = 165f, isDesertMesa = false)

        var bId = 1
        fun addBuilding(
            x: Float, y: Float, w: Float, h: Float,
            height: Float,
            wallColor: Color,
            roofColor: Color,
            trimColor: Color,
            label: String? = null,
            helipad: Boolean = false
        ) {
            buildings += Building(
                id = bId++,
                rect = WorldRect(x, y, w, h),
                height = height,
                wallColor = wallColor,
                roofColor = roofColor,
                trimColor = trimColor,
                landmarkLabel = label,
                hasHelipad = helipad
            )
        }

        // Airport 1: Metro Int'l Hangars & ATC Tower
        addBuilding(220f, 180f, 260f, 150f, 28f, Color(0xFF1E293B), Color(0xFF334155), Color(0xFF38BDF8), "HANGAR A")
        addBuilding(560f, 180f, 260f, 150f, 28f, Color(0xFF1E293B), Color(0xFF334155), Color(0xFF38BDF8), "HANGAR B")
        addBuilding(920f, 180f, 140f, 140f, 68f, Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF00E5FF), "METRO ATC", helipad = true)

        // Airport 2: Verdant Desert Airfield Hangars & Control Tower
        addBuilding(7820f, 280f, 280f, 170f, 26f, Color(0xFF3F3832), Color(0xFF57534E), Color(0xFFF59E0B), "DESERT HANGAR 1")
        addBuilding(8200f, 280f, 280f, 170f, 26f, Color(0xFF3F3832), Color(0xFF57534E), Color(0xFFF59E0B), "DESERT HANGAR 2")
        addBuilding(8580f, 280f, 140f, 140f, 60f, Color(0xFF292524), Color(0xFF44403C), Color(0xFFFBBF24), "DESERT ATC", helipad = true)

        // Airport 3: Easter Basin Naval Air Base Hangars & Command Tower
        addBuilding(5540f, 5760f, 320f, 180f, 30f, Color(0xFF1E293B), Color(0xFF334155), Color(0xFFFB7185), "NAVAL HANGAR 1")
        addBuilding(5980f, 5760f, 320f, 180f, 30f, Color(0xFF1E293B), Color(0xFF334155), Color(0xFFFB7185), "NAVAL HANGAR 2")
        addBuilding(6420f, 5760f, 150f, 150f, 72f, Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF00E5FF), "NAVAL ATC", helipad = true)

        // Airport 4: Palomino Regional Airfield Hangars & Control Tower
        addBuilding(10180f, 5020f, 290f, 160f, 26f, Color(0xFF1E293B), Color(0xFF334155), Color(0xFF4ADE80), "PALOMINO HANGAR")
        addBuilding(10580f, 5020f, 140f, 140f, 58f, Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF4ADE80), "PALOMINO ATC", helipad = true)

        val roofColors = listOf(Color(0xFF1A2332), Color(0xFF1E293B), Color(0xFF232B3E), Color(0xFF16202E))
        val wallColors = listOf(Color(0xFF0F172A), Color(0xFF111827), Color(0xFF151E2E))
        val trims = listOf(Color(0xFF00E5FF), Color(0xFFFFB300), Color(0xFFFF4D6D), Color(0xFF38BDF8))

        // Island 1: Downtown Core Skyscrapers
        val downtownBlocks = listOf(
            Triple(1830f, 310f, "METRO BANK" to true),
            Triple(2090f, 310f, "APEX TOWER" to false),
            Triple(1830f, 600f, null to false),
            Triple(2090f, 600f, "POLICE HQ" to true),
            Triple(2470f, 310f, "SKYLINE PLAZA" to false),
            Triple(2730f, 310f, null to false),
            Triple(2470f, 600f, null to false),
            Triple(2730f, 600f, "VICE MEDIA" to false),
            Triple(1830f, 1050f, "ARCADE" to false),
            Triple(2090f, 1050f, null to false),
            Triple(1830f, 1280f, null to false),
            Triple(2090f, 1280f, "HELIPORT" to true),
            Triple(2470f, 1050f, "HOTEL" to false),
            Triple(2730f, 1050f, null to false),
            Triple(2470f, 1280f, null to false),
            Triple(2730f, 1280f, null to false),
            Triple(1830f, 1670f, "SAFEHOUSE" to false),
            Triple(2090f, 1670f, null to false),
            Triple(2470f, 1670f, "ARMORY" to false),
            Triple(2730f, 1670f, null to false),
            Triple(2470f, 1920f, null to false),
            Triple(2730f, 1920f, "SPRAY SHOP" to false)
        )
        downtownBlocks.forEachIndexed { idx, (bx, by, info) ->
            addBuilding(
                bx, by, 200f, 190f, 36f + (idx % 4) * 16f,
                wallColors[idx % wallColors.size],
                roofColors[idx % roofColors.size],
                trims[idx % trims.size],
                info.first, info.second
            )
        }

        // Island 1: Mid-South & Sunset Palms
        val midBlocks = listOf(
            Pair(1830f, 2320f), Pair(2090f, 2320f), Pair(2470f, 2320f), Pair(2730f, 2320f),
            Pair(1830f, 2940f), Pair(2090f, 2940f), Pair(2470f, 2940f), Pair(2730f, 2940f),
            Pair(3110f, 1670f), Pair(3370f, 1670f), Pair(3110f, 2320f), Pair(3370f, 2320f),
            Pair(3110f, 2940f), Pair(3370f, 2940f)
        )
        midBlocks.forEachIndexed { idx, (bx, by) ->
            addBuilding(
                bx, by, 195f, 210f, 28f + (idx % 3) * 11f,
                wallColors[idx % wallColors.size],
                roofColors[(idx + 1) % roofColors.size],
                trims[(idx + 1) % trims.size]
            )
        }

        // Island 1: Foundry Industrial Warehouses
        val industrialBuildings = listOf(
            Triple(540f, 1680f, "FOUNDRY"), Triple(800f, 1680f, "CRANE BAY"),
            Triple(1180f, 1680f, "MOD GARAGE"), Triple(540f, 2320f, "WAREHOUSE 1"),
            Triple(1180f, 2320f, "BLACK MARKET"), Triple(540f, 2940f, "FREIGHT DEPOT"),
            Triple(1180f, 2940f, "HARBOR CUSTOMS")
        )
        industrialBuildings.forEach { (bx, by, label) ->
            addBuilding(bx, by, 210f, 190f, 24f, Color(0xFF1F242D), Color(0xFF2B313C), Color(0xFFFFB300), label)
        }

        // Island 2: Venturas Casino Strip Resorts & Desert Compounds
        val venturasBlocks = listOf(
            Triple(5620f, 1670f, "EMERALD CASINO" to true),
            Triple(5880f, 1670f, "FOUR DRAGONS" to false),
            Triple(6260f, 1670f, "CALIGULA PALACE" to true),
            Triple(6520f, 1670f, null to false),
            Triple(5620f, 2320f, "ROYAL RESORT" to false),
            Triple(6260f, 2320f, "STARFISH HOTEL" to true),
            Triple(6900f, 1670f, "VENTURAS PD" to true),
            Triple(6900f, 2320f, "NEON ARENA" to false),
            Triple(5620f, 2940f, "STRIP MALL" to false),
            Triple(6260f, 2940f, "LUXOR TOWER" to true),
            Triple(6900f, 2940f, null to false),
            Triple(7560f, 1670f, "DESERT REFINERY" to false),
            Triple(7560f, 2320f, "CANYON GARAGE" to false),
            Triple(8280f, 1670f, "AREA DEPOT" to false),
            Triple(8280f, 2320f, "OUTPOST 51" to true)
        )
        venturasBlocks.forEachIndexed { idx, (bx, by, info) ->
            val h = if (idx < 11) 46f + (idx % 4) * 18f else 26f
            addBuilding(
                bx, by, 210f, 195f, h,
                wallColors[idx % wallColors.size],
                roofColors[(idx + 2) % roofColors.size],
                Color(0xFFA855F7),
                info.first, info.second
            )
        }

        // Island 3: San Fierro Bay Financial Towers & Naval Warehouses
        val bayCityBlocks = listOf(
            Triple(1960f, 5620f, "GOLDEN BAY HQ" to true),
            Triple(2480f, 5620f, "FERRY PLAZA" to false),
            Triple(1960f, 6780f, "TRANSAMERICA SPIRE" to true),
            Triple(2480f, 6780f, "FIERRO BANK" to false),
            Triple(3920f, 5620f, "CIVIC CENTER" to false),
            Triple(3920f, 6780f, "CHINATOWN ARCH" to false),
            Triple(1960f, 7580f, "TECH LABS" to false),
            Triple(2480f, 7580f, "SF HOSPITAL" to true),
            Triple(3920f, 7580f, "HASHBURY LOFTS" to false),
            Triple(4680f, 6780f, "SUBWAY DEPOT" to false),
            Triple(4680f, 7580f, "BAY ARMORY" to false),
            Triple(5480f, 6780f, "NAVAL BARRACKS" to false),
            Triple(5480f, 7580f, "DRY DOCK HQ" to false),
            Triple(6900f, 6780f, "CARRIER SUPPLY" to false),
            Triple(6900f, 7580f, "FLEET COMMAND" to true)
        )
        bayCityBlocks.forEachIndexed { idx, (bx, by, info) ->
            val h = if (idx < 10) 48f + (idx % 4) * 20f else 28f
            addBuilding(
                bx, by, 215f, 200f, h,
                wallColors[idx % wallColors.size],
                roofColors[idx % roofColors.size],
                Color(0xFF2DD4BF),
                info.first, info.second
            )
        }

        // Island 4: Palomino Creek Town & Pine Lodges
        val palominoBlocks = listOf(
            Triple(10620f, 6020f, "PALOMINO BANK" to false),
            Triple(10620f, 6820f, "CREEK LODGE" to true),
            Triple(11520f, 6020f, "TIMBER MILL" to false),
            Triple(11520f, 7720f, "MARINA RESORT" to true),
            Triple(10620f, 7720f, "PINES PD" to false)
        )
        palominoBlocks.forEachIndexed { idx, (bx, by, info) ->
            addBuilding(
                bx, by, 220f, 195f, 26f + (idx % 3) * 14f,
                Color(0xFF1F2937), Color(0xFF334155), Color(0xFF4ADE80),
                info.first, info.second
            )
        }

        // Island 5: Mt. Chiliad Observatory & Red County Outposts
        val chiliadBlocks = listOf(
            Triple(3940f, 11460f, "CHILIAD BASE CAMP" to true),
            Triple(5740f, 11460f, "ALPINE OBSERVATORY" to true),
            Triple(7640f, 11460f, "RED COUNTY TRUCK STOP" to false),
            Triple(9240f, 11460f, "CANYON QUARRY HQ" to false),
            Triple(10620f, 11460f, "BADLANDS DEPOT" to true)
        )
        chiliadBlocks.forEachIndexed { idx, (bx, by, info) ->
            addBuilding(
                bx, by, 230f, 200f, 28f + (idx % 2) * 16f,
                Color(0xFF292524), Color(0xFF44403C), Color(0xFFE879F9),
                info.first, info.second
            )
        }

        // Grand 5-Island Metro Railway Loop (12 Stations across all 5 islands!)
        val railTrack = listOf(
            620f to 1500f,
            2380f to 1500f,
            3740f to 1500f,
            5520f to 1500f,
            7920f to 1500f,
            7920f to 3500f,
            6800f to 4400f,
            6800f to 5520f,
            6800f to 6680f,
            10480f to 6680f,
            10480f to 10380f,
            5600f to 10380f,
            3820f to 10380f,
            3820f to 7480f,
            2380f to 7480f,
            2380f to 5520f,
            2380f to 3620f,
            1480f to 3620f,
            620f to 3620f
        )

        val metroStations = listOf(
            MetroStation("Airstrip Gate", 620f, 1500f, 0),
            MetroStation("Downtown Central", 2380f, 1500f, 1),
            MetroStation("Emerald Park", 3740f, 1500f, 2),
            MetroStation("Venturas Strip", 5520f, 1500f, 3),
            MetroStation("Verdant Airfield", 7920f, 1500f, 4),
            MetroStation("Easter Naval Base", 6800f, 6680f, 8),
            MetroStation("Palomino Pines", 10480f, 6680f, 9),
            MetroStation("Red County East", 10480f, 10380f, 10),
            MetroStation("Mt. Chiliad South", 3820f, 10380f, 12),
            MetroStation("San Fierro Civic", 3820f, 7480f, 13),
            MetroStation("San Fierro Bay", 2380f, 7480f, 14),
            MetroStation("Harbor Terminal", 1480f, 3620f, 17)
        )

        // 10 Stunt Ramps across all 5 Islands
        val stuntRamps = listOf(
            StuntRamp(1, "Foundry Overpass Jump", WorldRect(920f, 2060f, 80f, 120f), (-PI / 2).toFloat()),
            StuntRamp(2, "Harbor Pier Launch", WorldRect(1445f, 3720f, 80f, 110f), (PI / 2).toFloat()),
            StuntRamp(3, "Downtown Alley Boost", WorldRect(2340f, 1920f, 80f, 110f), (-PI / 2).toFloat()),
            StuntRamp(4, "Sunset Dune Leap", WorldRect(3860f, 2520f, 110f, 75f), 0f),
            StuntRamp(5, "Venturas Strip Mega Ramp", WorldRect(6120f, 3260f, 85f, 125f), (-PI / 2).toFloat()),
            StuntRamp(6, "Bone County Canyon Jump", WorldRect(8450f, 2820f, 125f, 85f), 0f),
            StuntRamp(7, "Golden Bay Bridge Approach", WorldRect(2340f, 6100f, 85f, 125f), (-PI / 2).toFloat()),
            StuntRamp(8, "Naval Carrier Deck Launch", WorldRect(7240f, 9020f, 90f, 120f), (PI / 2).toFloat()),
            StuntRamp(9, "Palomino Lake Jump", WorldRect(11340f, 8720f, 90f, 120f), (PI / 2).toFloat()),
            StuntRamp(10, "Chiliad Alpine Ridge Launch", WorldRect(5560f, 10200f, 130f, 90f), 0f)
        )

        // 32 Hidden Collectible Packages across all 5 Islands & 12 Districts
        val hiddenPackages = listOf(
            HiddenPackage("pkg_01", 260f, 340f, "Metro Airport Hangar Apron"),
            HiddenPackage("pkg_02", 1380f, 200f, "Metro Airport East Tower"),
            HiddenPackage("pkg_03", 2060f, 520f, "Downtown Plaza Alley"),
            HiddenPackage("pkg_04", 2720f, 840f, "Vice Media Rooftop Court"),
            HiddenPackage("pkg_05", 2290f, 1880f, "Safehouse Backlot"),
            HiddenPackage("pkg_06", 3340f, 560f, "Emerald Park Fountain"),
            HiddenPackage("pkg_07", 3980f, 580f, "Emerald Botanical Corner"),
            HiddenPackage("pkg_08", 760f, 1940f, "Foundry Crane Yard"),
            HiddenPackage("pkg_09", 1080f, 2640f, "Industrial Rail Siding"),
            HiddenPackage("pkg_10", 695f, 4150f, "Harbor West Pier Tip"),
            HiddenPackage("pkg_11", 2090f, 4120f, "Harbor East Pier Tip"),
            HiddenPackage("pkg_12", 4080f, 2320f, "Sunset North Dunes"),
            HiddenPackage("pkg_13", 4090f, 3460f, "Sunset Pier Cabana"),
            HiddenPackage("pkg_14", 3360f, 3420f, "South Boardwalk Alley"),
            HiddenPackage("pkg_15", 5640f, 4720f, "Venturas Casino Marina Pier"),
            HiddenPackage("pkg_16", 6220f, 1980f, "Venturas Strip Central Plaza"),
            HiddenPackage("pkg_17", 7950f, 460f, "Verdant Airfield Hangar"),
            HiddenPackage("pkg_18", 8950f, 2900f, "Bone County Desert Outpost"),
            HiddenPackage("pkg_19", 2060f, 5060f, "San Fierro Ferry Pier"),
            HiddenPackage("pkg_20", 2420f, 6480f, "Golden Bay Tower Promenade"),
            HiddenPackage("pkg_21", 3480f, 6080f, "San Fierro Civic Park"),
            HiddenPackage("pkg_22", 5760f, 5940f, "Naval Air Base Hangar"),
            HiddenPackage("pkg_23", 7290f, 9450f, "Naval Carrier Pier Tip"),
            HiddenPackage("pkg_24", 4700f, 1580f, "East Bay Bridge Midpoint"),
            HiddenPackage("pkg_25", 9060f, 6680f, "Palomino Suspension Span"),
            HiddenPackage("pkg_26", 10320f, 5180f, "Palomino Regional Hangar"),
            HiddenPackage("pkg_27", 11460f, 9460f, "Palomino Creek Pier Tip"),
            HiddenPackage("pkg_28", 12180f, 7080f, "Palomino Ridge Overlook"),
            HiddenPackage("pkg_29", 3820f, 9600f, "Chiliad West Bridge Span"),
            HiddenPackage("pkg_30", 4850f, 11260f, "Mt. Chiliad Pine Switchback"),
            HiddenPackage("pkg_31", 8560f, 11260f, "Red County Canyon Saddle"),
            HiddenPackage("pkg_32", 11800f, 12000f, "Badlands Southeast Outpost")
        )

        // Service Points across all 5 Islands (14 Eating Places + Armories + Spray Shops + Pickups)
        val servicePoints = mutableListOf(
            // Island 1
            WorldServicePoint("arm_downtown", ServicePointType.ARMORY, 2570f, 1900f, "Downtown Armory"),
            WorldServicePoint("arm_industrial", ServicePointType.ARMORY, 1285f, 2550f, "Foundry Black Market"),
            WorldServicePoint("spray_downtown", ServicePointType.SPRAY_SHOP, 2820f, 2150f, "Pay 'N' Spray"),
            WorldServicePoint("spray_foundry", ServicePointType.SPRAY_SHOP, 1285f, 1920f, "Foundry Mod & Spray"),
            WorldServicePoint("spray_sunset", ServicePointType.SPRAY_SHOP, 3480f, 3190f, "Sunset Paint Shop"),
            WorldServicePoint("hp_1", ServicePointType.HEALTH_PACK, 2120f, 2140f, "Medkit"),
            WorldServicePoint("hp_2", ServicePointType.HEALTH_PACK, 3360f, 640f, "Park Medkit"),
            WorldServicePoint("hp_3", ServicePointType.HEALTH_PACK, 1485f, 3880f, "Harbor Medkit"),
            WorldServicePoint("armr_1", ServicePointType.ARMOR_VEST, 2190f, 820f, "Tactical Vest"),
            WorldServicePoint("armr_2", ServicePointType.ARMOR_VEST, 960f, 2460f, "Foundry Vest"),
            WorldServicePoint("bribe_1", ServicePointType.BRIBE_STAR, 1620f, 1280f, "Police Bribe"),
            WorldServicePoint("bribe_2", ServicePointType.BRIBE_STAR, 3020f, 2540f, "Police Bribe"),
            // Island 2
            WorldServicePoint("arm_venturas", ServicePointType.ARMORY, 6180f, 2240f, "Venturas Armory"),
            WorldServicePoint("spray_venturas", ServicePointType.SPRAY_SHOP, 6820f, 2880f, "Venturas Spray & Fix"),
            WorldServicePoint("hp_venturas", ServicePointType.HEALTH_PACK, 6160f, 1580f, "Strip Medkit"),
            WorldServicePoint("armr_desert", ServicePointType.ARMOR_VEST, 8180f, 1580f, "Desert Tactical Vest"),
            WorldServicePoint("bribe_venturas", ServicePointType.BRIBE_STAR, 7440f, 2860f, "Desert Police Bribe"),
            // Island 3
            WorldServicePoint("arm_bay", ServicePointType.ARMORY, 4580f, 7480f, "San Fierro Armory"),
            WorldServicePoint("spray_bay", ServicePointType.SPRAY_SHOP, 3080f, 7480f, "Bay Mod & Spray"),
            WorldServicePoint("hp_bay", ServicePointType.HEALTH_PACK, 2380f, 7480f, "SF Clinic Medkit"),
            WorldServicePoint("armr_naval", ServicePointType.ARMOR_VEST, 6800f, 6680f, "Naval Heavy Vest"),
            WorldServicePoint("bribe_bay", ServicePointType.BRIBE_STAR, 5360f, 8280f, "Bay Police Bribe"),
            // Island 4 & 5
            WorldServicePoint("arm_palomino", ServicePointType.ARMORY, 10480f, 7560f, "Palomino Armory"),
            WorldServicePoint("spray_palomino", ServicePointType.SPRAY_SHOP, 11380f, 7560f, "Pines Paint & Fix"),
            WorldServicePoint("hp_chiliad", ServicePointType.HEALTH_PACK, 3820f, 11320f, "Chiliad Ranger Medkit"),
            WorldServicePoint("armr_chiliad", ServicePointType.ARMOR_VEST, 7500f, 11320f, "Canyon Tactical Vest"),
            WorldServicePoint("bribe_chiliad", ServicePointType.BRIBE_STAR, 9120f, 10380f, "Red County Bribe"),
            // 14 Eating Places across all 5 Islands!
            WorldServicePoint("diner_downtown", ServicePointType.DINER, 2240f, 1940f, "Cluckin' Wing"),
            WorldServicePoint("diner_pizza", ServicePointType.DINER, 3060f, 1520f, "Well Stacked Pizza"),
            WorldServicePoint("diner_burger", ServicePointType.DINER, 3620f, 2240f, "Burger Shot"),
            WorldServicePoint("diner_harbor", ServicePointType.DINER, 1460f, 3540f, "Pier 69 Seafood Grill"),
            WorldServicePoint("diner_foundry", ServicePointType.DINER, 1140f, 2240f, "Foundry Taco Truck"),
            WorldServicePoint("diner_venturas_1", ServicePointType.DINER, 6120f, 1940f, "Caligula Steakhouse"),
            WorldServicePoint("diner_venturas_2", ServicePointType.DINER, 5660f, 2820f, "Starlight Neon Diner"),
            WorldServicePoint("diner_desert", ServicePointType.DINER, 8160f, 2200f, "Lil' Probe Roadhouse"),
            WorldServicePoint("diner_bay", ServicePointType.DINER, 2420f, 6640f, "San Fierro Noodle Bar"),
            WorldServicePoint("diner_naval", ServicePointType.DINER, 6760f, 7440f, "Easter Mess & Cafe"),
            WorldServicePoint("diner_palomino_1", ServicePointType.DINER, 10540f, 6620f, "Palomino Bait & Grill"),
            WorldServicePoint("diner_palomino_2", ServicePointType.DINER, 11340f, 8460f, "Pinewood Coffee"),
            WorldServicePoint("diner_chiliad_1", ServicePointType.DINER, 5540f, 11260f, "Chiliad Summit Lodge"),
            WorldServicePoint("diner_chiliad_2", ServicePointType.DINER, 9060f, 11260f, "Red County BBQ")
        )

        // Traffic Lanes across all 5 Islands & 7 Bridges
        val trafficLanes = mutableListOf<TrafficLane>()
        for (ry in yRoadCenters1) {
            val endX = if (ry == 1580f || ry == 2860f) 5100f else if (ry > 1800f) 3740f else 4100f
            trafficLanes += TrafficLane(220f, ry + 24f, endX, ry + 24f, 0f)
            trafficLanes += TrafficLane(endX, ry - 24f, 220f, ry - 24f, PI.toFloat())
        }
        for (rx in xRoadCenters1) {
            val startY = if (rx < 1500f) 940f else 260f
            val endY = if (rx == 2380f) 5220f else 3800f
            trafficLanes += TrafficLane(rx - 24f, startY, rx - 24f, endY, (PI / 2).toFloat())
            trafficLanes += TrafficLane(rx + 24f, endY, rx + 24f, startY, (-PI / 2).toFloat())
        }
        for (ry in yRoadCenters2) {
            val endX = if (ry < 1200f) 9120f else 8900f
            trafficLanes += TrafficLane(5260f, ry + 24f, endX, ry + 24f, 0f)
            trafficLanes += TrafficLane(endX, ry - 24f, 5260f, ry - 24f, PI.toFloat())
        }
        for (rx in xRoadCenters2) {
            val startY = if (rx > 7600f) 1000f else 380f
            val endY = if (rx == 6800f) 5220f else 4280f
            trafficLanes += TrafficLane(rx - 24f, startY, rx - 24f, endY, (PI / 2).toFloat())
            trafficLanes += TrafficLane(rx + 24f, endY, rx + 24f, startY, (-PI / 2).toFloat())
        }
        for (ry in yRoadCenters3) {
            val endX = if (ry == 6680f) 9660f else 8140f
            trafficLanes += TrafficLane(1660f, ry + 24f, endX, ry + 24f, 0f)
            trafficLanes += TrafficLane(endX, ry - 24f, 1660f, ry - 24f, PI.toFloat())
        }
        for (rx in xRoadCenters3) {
            val startY = if (rx > 5400f && rx != 6800f) 6720f else 5340f
            val endY = if (rx == 3820f) 9960f else 9020f
            trafficLanes += TrafficLane(rx - 24f, startY, rx - 24f, endY, (PI / 2).toFloat())
            trafficLanes += TrafficLane(rx + 24f, endY, rx + 24f, startY, (-PI / 2).toFloat())
        }
        for (ry in yRoadCenters4) {
            trafficLanes += TrafficLane(9760f, ry + 24f, 12300f, ry + 24f, 0f)
            trafficLanes += TrafficLane(12300f, ry - 24f, 9760f, ry - 24f, PI.toFloat())
        }
        for (rx in xRoadCenters4) {
            val endY = if (rx == 10480f) 9960f else 9000f
            trafficLanes += TrafficLane(rx - 24f, 5820f, rx - 24f, endY, (PI / 2).toFloat())
            trafficLanes += TrafficLane(rx + 24f, endY, rx + 24f, 5820f, (-PI / 2).toFloat())
        }
        for (ry in yRoadCenters5) {
            trafficLanes += TrafficLane(1900f, ry + 24f, 11980f, ry + 24f, 0f)
            trafficLanes += TrafficLane(11980f, ry - 24f, 1900f, ry - 24f, PI.toFloat())
        }
        for (rx in xRoadCenters5) {
            trafficLanes += TrafficLane(rx - 24f, 10020f, rx - 24f, 12160f, (PI / 2).toFloat())
            trafficLanes += TrafficLane(rx + 24f, 12160f, rx + 24f, 10020f, (-PI / 2).toFloat())
        }

        // Sidewalk walk nodes across all 5 Islands
        val sidewalkNodes = mutableListOf<Pair<Float, Float>>()
        for (rx in xRoadCenters1) for (ry in yRoadCenters1) {
            sidewalkNodes += (rx - 64f) to (ry - 64f)
            sidewalkNodes += (rx + 64f) to (ry + 64f)
        }
        for (rx in xRoadCenters2) for (ry in yRoadCenters2) {
            sidewalkNodes += (rx - 64f) to (ry - 64f)
            sidewalkNodes += (rx + 64f) to (ry + 64f)
        }
        for (rx in xRoadCenters3) for (ry in yRoadCenters3) {
            sidewalkNodes += (rx - 64f) to (ry - 64f)
            sidewalkNodes += (rx + 64f) to (ry + 64f)
        }
        for (rx in xRoadCenters4) for (ry in yRoadCenters4) {
            sidewalkNodes += (rx - 64f) to (ry - 64f)
            sidewalkNodes += (rx + 64f) to (ry + 64f)
        }
        for (rx in xRoadCenters5) for (ry in yRoadCenters5) {
            sidewalkNodes += (rx - 64f) to (ry - 64f)
            sidewalkNodes += (rx + 64f) to (ry + 64f)
        }

        return CityWorld(
            worldWidth = 12800f,
            worldHeight = 12800f,
            islands = islands,
            bridges = bridges,
            buildings = buildings,
            roads = roads,
            parks = parks,
            waterZones = waterZones,
            piers = piers,
            sandZones = sandZones,
            desertZones = desertZones,
            runwayZones = runwayZones,
            railTrack = railTrack,
            metroStations = metroStations,
            stuntRamps = stuntRamps,
            hiddenPackages = hiddenPackages,
            servicePoints = servicePoints,
            trafficLanes = trafficLanes,
            sidewalkWalkNodes = sidewalkNodes,
            palmTrees = palmTrees,
            pineTrees = pineTrees,
            cacti = cacti,
            mountainPeaks = mountainPeaks,
            intersections = intersections
        )
    }

    fun spawnInitialVehicles(world: CityWorld): MutableList<VehicleEntity> {
        val list = mutableListOf<VehicleEntity>()
        var nextId = 1

        // Player starter vehicles near Downtown Safehouse (2160, 2160)
        list += VehicleEntity(nextId++, VehicleType.SPORTS_CAR, 2240f, 2140f, 0f)
        list += VehicleEntity(nextId++, VehicleType.MOTORCYCLE, 2110f, 2140f, -0.4f)
        list += VehicleEntity(nextId++, VehicleType.MUSCLE_CAR, 2220f, 2000f, 0f)

        // Airport 1: Metro Int'l Airport
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 380f, 490f, 0f)
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 640f, 490f, 0f)
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 460f, 695f, 0f)

        // Airport 2: Verdant Desert Airfield (Island 2)
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 7940f, 640f, 0f)
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 8160f, 860f, 0f)

        // Airport 3: Easter Basin Naval Air Base (Island 3)
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 5740f, 6140f, 0f)
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 5980f, 6400f, 0f)

        // Airport 4: Palomino Regional Airfield (Island 4)
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 10340f, 5370f, 0f)
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 10680f, 5600f, 0f)

        // Ambient High-Altitude Airliners / Jets cruising across the skies!
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 1800f, 2600f, 0.35f, speed = 290f, altitude = 0.78f, isAmbientCruise = true)
        list += VehicleEntity(nextId++, VehicleType.PROP_PLANE, 7200f, 4600f, 2.5f, speed = 240f, altitude = 0.72f, isAmbientCruise = true)
        list += VehicleEntity(nextId++, VehicleType.STUNT_JET, 9400f, 8600f, -1.8f, speed = 310f, altitude = 0.85f, isAmbientCruise = true)

        // Boats & Cargo Ships (Docked + Ambient Ocean Channel Cruisers)
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 795f, 4050f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 1585f, 4050f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.CARGO_SHIP, 1340f, 4300f, 0f, speed = 75f, isAmbientCruise = true)
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 2185f, 4040f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 4520f, 2400f, (PI / 2).toFloat(), speed = 135f, isAmbientCruise = true)
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 5780f, 4660f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 2170f, 5120f, (-PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.CARGO_SHIP, 7560f, 9400f, 0f, speed = 70f, isAmbientCruise = true)
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 11560f, 9320f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.SPEEDBOAT, 8920f, 6200f, (PI / 2).toFloat(), speed = 140f, isAmbientCruise = true)

        // 5 Active Metro Trains spaced across the 5-Island Rail Loop
        val railTotal = railTrackTotalLength(world.railTrack)
        for (tIdx in 0 until 5) {
            val frac = tIdx / 5f
            val (tx, ty, tang) = sampleRailTrack(world.railTrack, railTotal * frac)
            list += VehicleEntity(
                id = nextId++,
                type = VehicleType.METRO_TRAIN,
                x = tx, y = ty, angleRad = tang,
                speed = 205f,
                railTrackDistance = railTotal * frac
            )
        }

        // Parked vehicles across all 5 Islands
        list += VehicleEntity(nextId++, VehicleType.POLICE_CRUISER, 2120f, 900f, 0f)
        list += VehicleEntity(nextId++, VehicleType.SPORTS_CAR, 3720f, 2580f, (PI / 2).toFloat())
        list += VehicleEntity(nextId++, VehicleType.MUSCLE_CAR, 1020f, 2260f, 0f)
        list += VehicleEntity(nextId++, VehicleType.ARMORED_VAN, 920f, 2440f, 0f)
        list += VehicleEntity(nextId++, VehicleType.SPORTS_CAR, 6160f, 2150f, 0f)
        list += VehicleEntity(nextId++, VehicleType.POLICE_CRUISER, 6860f, 1620f, 0f)
        list += VehicleEntity(nextId++, VehicleType.MOTORCYCLE, 8180f, 1520f, 0f)
        list += VehicleEntity(nextId++, VehicleType.SPORTS_CAR, 2440f, 6680f, 0f)
        list += VehicleEntity(nextId++, VehicleType.ARMORED_VAN, 6760f, 6700f, 0f)
        list += VehicleEntity(nextId++, VehicleType.MUSCLE_CAR, 10540f, 6740f, 0f)
        list += VehicleEntity(nextId++, VehicleType.MOTORCYCLE, 5600f, 10440f, 0f)
        list += VehicleEntity(nextId++, VehicleType.SPORTS_CAR, 3880f, 11380f, 0f)

        // Dense Dynamic Road Traffic (Multiple staggered vehicles per lane with diverse speeds & colors!)
        val trafficTypes = listOf(
            VehicleType.TAXI,
            VehicleType.MUSCLE_CAR,
            VehicleType.SPORTS_CAR,
            VehicleType.MOTORCYCLE,
            VehicleType.TAXI,
            VehicleType.POLICE_CRUISER,
            VehicleType.SPORTS_CAR,
            VehicleType.ARMORED_VAN
        )
        val sedanPaintPalette = listOf(
            Color(0xFFE11D48), Color(0xFF0284C7), Color(0xFFF59E0B),
            Color(0xFF10B981), Color(0xFFE2E8F0), Color(0xFF6366F1),
            Color(0xFF1E293B), Color(0xFFFB7185), Color(0xFF14B8A6)
        )
        world.trafficLanes.forEachIndexed { laneIdx, lane ->
            val len = hypot((lane.endX - lane.startX).toDouble(), (lane.endY - lane.startY).toDouble()).toFloat()
            val carSlots = if (len > 3600f) listOf(0.16f, 0.48f, 0.78f) else listOf(0.24f, 0.68f)
            carSlots.forEachIndexed { slotIdx, tBase ->
                if ((laneIdx + slotIdx) % 3 != 2 || len > 3200f) {
                    val t = (tBase + (laneIdx % 5) * 0.02f).coerceIn(0.08f, 0.90f)
                    val vx = lane.startX + (lane.endX - lane.startX) * t
                    val vy = lane.startY + (lane.endY - lane.startY) * t
                    val type = trafficTypes[(laneIdx + slotIdx * 3) % trafficTypes.size]
                    val col = when (type) {
                        VehicleType.TAXI, VehicleType.POLICE_CRUISER, VehicleType.ARMORED_VAN -> type.primaryColor
                        else -> sedanPaintPalette[(laneIdx * 5 + slotIdx) % sedanPaintPalette.size]
                    }
                    val cruiseSpd = when (type) {
                        VehicleType.SPORTS_CAR, VehicleType.MOTORCYCLE -> 165f + (laneIdx % 3) * 18f
                        VehicleType.ARMORED_VAN -> 115f
                        else -> 135f + (laneIdx % 3) * 14f
                    }
                    list += VehicleEntity(
                        id = nextId++,
                        type = type,
                        x = vx,
                        y = vy,
                        angleRad = lane.angleRad,
                        speed = cruiseSpd,
                        isTrafficControlled = true,
                        laneIndex = laneIdx,
                        desiredTrafficSpeed = cruiseSpd,
                        customColor = col
                    )
                }
            }
        }
        return list
    }

    fun spawnInitialNpcs(world: CityWorld): MutableList<NpcEntity> {
        val npcs = mutableListOf<NpcEntity>()
        val rng = Random(9182)
        val shirtColors = listOf(
            Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFFFBBF24),
            Color(0xFF34D399), Color(0xFFA78BFA), Color(0xFFE2E8F0),
            Color(0xFFFB923C), Color(0xFF2DD4BF), Color(0xFFEF4444), Color(0xFF94A3B8)
        )
        val pantsPalette = listOf(
            Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569),
            Color(0xFF3F3F46), Color(0xFF292524), Color(0xFF1E3A8A)
        )
        var nextId = 1

        // 210 Strolling Street Civilians distributed evenly across all 5 Islands' sidewalks
        for (i in 0 until 210) {
            val nodeIdx = (i * 2) % world.sidewalkWalkNodes.size
            val (nx, ny) = world.sidewalkWalkNodes[nodeIdx]
            val startLoiter = (i % 6 == 0)
            npcs += NpcEntity(
                id = nextId++,
                role = NpcRole.CIVILIAN,
                x = nx + rng.nextFloat() * 28f - 14f,
                y = ny + rng.nextFloat() * 28f - 14f,
                angleRad = rng.nextFloat() * 6.28f,
                hp = 50f,
                targetNodeIdx = (nodeIdx + 1) % world.sidewalkWalkNodes.size,
                shirtColor = shirtColors[i % shirtColors.size],
                pantsColor = pantsPalette[i % pantsPalette.size],
                behaviorState = if (startLoiter) NpcBehaviorState.LOITERING else NpcBehaviorState.WALKING,
                stateTimerSec = if (startLoiter) 2f + rng.nextFloat() * 4f else 4f + rng.nextFloat() * 8f,
                hasCap = (i % 4 == 0)
            )
        }

        // Diner & Eating Place Patrons (3 patrons socializing around each of all 14 eating spots!)
        val diners = world.servicePoints.filter { it.type == ServicePointType.DINER }
        for ((dIdx, diner) in diners.withIndex()) {
            repeat(3) { p ->
                val ox = (p - 1) * 26f + (rng.nextFloat() * 10f - 5f)
                val oy = if (p == 1) 24f else -22f
                val px = diner.x + ox
                val py = diner.y + oy
                val nearestIdx = findClosestWalkNodeIndex(world.sidewalkWalkNodes, px, py)
                npcs += NpcEntity(
                    id = nextId++,
                    role = NpcRole.CIVILIAN,
                    x = px,
                    y = py,
                    angleRad = rng.nextFloat() * 6.28f,
                    hp = 55f,
                    targetNodeIdx = nearestIdx,
                    shirtColor = shirtColors[(dIdx + p) % shirtColors.size],
                    pantsColor = pantsPalette[(dIdx + p) % pantsPalette.size],
                    behaviorState = NpcBehaviorState.LOITERING,
                    stateTimerSec = 5f + rng.nextFloat() * 6f,
                    hasCap = (p % 2 == 0)
                )
            }
        }

        // Metro Station Commuters (2 commuters per station across all 12 stations)
        for ((sIdx, st) in world.metroStations.withIndex()) {
            repeat(2) { c ->
                val sx = st.x + (if (c == 0) -32f else 32f)
                val sy = st.y + (if (c == 0) 20f else -20f)
                val nearestIdx = findClosestWalkNodeIndex(world.sidewalkWalkNodes, sx, sy)
                npcs += NpcEntity(
                    id = nextId++,
                    role = NpcRole.CIVILIAN,
                    x = sx,
                    y = sy,
                    angleRad = rng.nextFloat() * 6.28f,
                    hp = 50f,
                    targetNodeIdx = nearestIdx,
                    shirtColor = shirtColors[(sIdx + c + 2) % shirtColors.size],
                    pantsColor = pantsPalette[(sIdx + c) % pantsPalette.size],
                    behaviorState = NpcBehaviorState.LOITERING,
                    stateTimerSec = 4f + rng.nextFloat() * 6f
                )
            }
        }

        // 24 Beat Officers patrolling all 5 Islands' hubs, bridges & 4 airports
        val officerSpots = listOf(
            2120f to 930f, 1740f to 1560f, 980f to 880f, 3020f to 2200f,
            4180f to 1580f, 2380f to 3860f,
            6820f to 1640f, 6160f to 2840f, 8180f to 1020f, 5520f to 1520f,
            7900f to 820f, 6760f to 4440f,
            2380f to 6680f, 3820f to 7480f, 6800f to 6680f, 5740f to 6240f,
            1960f to 5560f, 6800f to 7480f,
            10480f to 6680f, 11380f to 7560f, 10200f to 5300f,
            3820f to 10380f, 7500f to 11320f, 10480f to 10380f
        )
        for ((ox, oy) in officerSpots) {
            npcs += NpcEntity(
                id = nextId++,
                role = NpcRole.OFFICER,
                x = ox,
                y = oy,
                hp = 85f,
                shirtColor = Color(0xFF2563EB),
                pantsColor = Color(0xFF0F172A),
                hasCap = true
            )
        }

        // 28 Rival Crew Members across Foundry Yards, Harbor Backlots, Desert Outposts, Naval Docks & Red County
        val rivalSpots = listOf(
            900f to 2400f, 1010f to 2440f, 820f to 2160f, 1320f to 3180f, 680f to 3120f,
            1160f to 2920f, 560f to 1720f,
            7540f to 2280f, 8320f to 2380f, 6880f to 3560f, 7560f to 1680f, 8280f to 1680f,
            8820f to 2860f, 5660f to 3520f,
            4680f to 7520f, 5520f to 7620f, 6920f to 7620f, 3960f to 7560f, 4680f to 6780f,
            6900f to 6780f, 7740f to 8280f,
            11520f to 6060f, 11520f to 7760f, 9240f to 11460f, 10620f to 11460f,
            7640f to 11460f, 5740f to 11460f, 11800f to 11320f
        )
        for ((idx, spot) in rivalSpots.withIndex()) {
            npcs += NpcEntity(
                id = nextId++,
                role = NpcRole.RIVAL,
                x = spot.first,
                y = spot.second,
                hp = 90f,
                shirtColor = Color(0xFFEF4444),
                pantsColor = Color(0xFF18181B),
                strafeDir = if (idx % 2 == 0) 1f else -1f
            )
        }

        return npcs
    }

    private fun findClosestWalkNodeIndex(nodes: List<Pair<Float, Float>>, x: Float, y: Float): Int {
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE
        for (i in nodes.indices) {
            val dx = nodes[i].first - x
            val dy = nodes[i].second - y
            val d2 = dx * dx + dy * dy
            if (d2 < bestDistSq) {
                bestDistSq = d2
                bestIdx = i
            }
        }
        return bestIdx
    }

    fun railTrackTotalLength(track: List<Pair<Float, Float>>): Float {
        var total = 0f
        for (i in track.indices) {
            val (x1, y1) = track[i]
            val (x2, y2) = track[(i + 1) % track.size]
            total += kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        }
        return total
    }

    fun sampleRailTrack(track: List<Pair<Float, Float>>, dist: Float): Triple<Float, Float, Float> {
        val total = railTrackTotalLength(track)
        var d = ((dist % total) + total) % total
        for (i in track.indices) {
            val (x1, y1) = track[i]
            val (x2, y2) = track[(i + 1) % track.size]
            val segLen = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
            if (d <= segLen || i == track.lastIndex) {
                val t = (d / segLen).coerceIn(0f, 1f)
                val x = x1 + (x2 - x1) * t
                val y = y1 + (y2 - y1) * t
                val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()).toFloat()
                return Triple(x, y, angle)
            }
            d -= segLen
        }
        return Triple(track[0].first, track[0].second, 0f)
    }
}
