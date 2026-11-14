package com.example

import com.example.game.engine.GameEngine
import com.example.game.model.VehicleCategory
import com.example.game.model.VehicleType
import com.example.game.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun `world spawns cars boats planes and trains`() {
        val engine = GameEngine()
        val categories = engine.vehicles.map { it.type.category }.toSet()
        assertTrue(VehicleCategory.LAND in categories)
        assertTrue(VehicleCategory.WATER in categories)
        assertTrue(VehicleCategory.AIR in categories)
        assertTrue(VehicleCategory.RAIL in categories)
    }

    @Test
    fun `player can enter car drive and launch jet airborne`() {
        val engine = GameEngine()
        // Move near starter Sports Car (2240, 2140)
        engine.player.x = 2230f
        engine.player.y = 2140f
        engine.toggleEnterOrExitVehicle()
        assertNotNull(engine.player.vehicleId)

        // Exit car and move near Stunt Jet on runway (640, 490)
        engine.toggleEnterOrExitVehicle()
        val jet = engine.vehicles.first { it.type == VehicleType.STUNT_JET }
        engine.player.x = jet.x
        engine.player.y = jet.y
        engine.toggleEnterOrExitVehicle()
        assertEquals(jet.id, engine.player.vehicleId)

        // Accelerate along runway for 2 seconds -> plane lifts off
        engine.inputMoveX = 1f
        engine.inputMoveY = 0f
        engine.inputBoosting = true
        repeat(120) { engine.step(0.016f) }
        assertTrue("Expected jet altitude > 0.15f, was ${jet.altitude}", jet.altitude > 0.15f)
    }

    @Test
    fun `player can purchase weapon and launch mission`() {
        val engine = GameEngine()
        engine.player.cash = 3000
        assertTrue(engine.purchaseWeaponOrAmmo(WeaponType.SMG))
        assertTrue(WeaponType.SMG in engine.player.ownedWeapons)
        assertEquals(WeaponType.SMG, engine.player.equippedWeapon)

        engine.startMission("m1_harbor_run")
        assertNotNull(engine.activeMission)
        assertEquals("m1_harbor_run", engine.activeMission?.def?.id)
    }

    @Test
    fun `metro train moves along rail loop`() {
        val engine = GameEngine()
        val train = engine.vehicles.first { it.type == VehicleType.METRO_TRAIN }
        val startX = train.x
        val startY = train.y
        repeat(60) { engine.step(0.016f) }
        assertTrue(train.x != startX || train.y != startY)
    }

    @Test
    fun `player jump applies 3d velocity and gravity lands back on ground`() {
        val engine = GameEngine()
        assertTrue(engine.player.isGrounded)
        engine.jumpPlayer()
        assertTrue(!engine.player.isGrounded)

        var peakZ = 0f
        repeat(15) {
            engine.step(0.016f)
            if (engine.player.zHeight > peakZ) peakZ = engine.player.zHeight
        }
        assertTrue("Expected player to rise above 5f, peak was $peakZ", peakZ > 5f)

        repeat(80) { engine.step(0.016f) }
        assertTrue("Expected player to land back on ground", engine.player.isGrounded)
        assertTrue("Expected resting zHeight <= 2f, was ${engine.player.zHeight}", engine.player.zHeight <= 2f)
    }

    @Test
    fun `vehicle collisions apply mass-weighted impulses and damage`() {
        val engine = GameEngine()
        val sports = engine.vehicles.first { it.type == VehicleType.SPORTS_CAR }
        val heavy = engine.vehicles.first { it.type == VehicleType.ARMORED_VAN }
        sports.x = 2200f
        sports.y = 2200f
        sports.angleRad = 0f
        sports.speed = 420f
        sports.isTrafficControlled = false

        heavy.x = 2220f
        heavy.y = 2200f
        heavy.angleRad = 0f
        heavy.speed = 0f
        heavy.isTrafficControlled = false

        val sportsStartHp = sports.hp
        val heavyStartHp = heavy.hp
        repeat(6) { engine.step(0.016f) }

        assertTrue("Expected vehicles to exchange impulse velocity", sports.knockVx != 0f || heavy.knockVx != 0f)
        assertTrue("Expected collision damage on vehicles", sports.hp < sportsStartHp || heavy.hp < heavyStartHp)
    }

    @Test
    fun `vehicle collision triggers hydrant water geyser and launches prop`() {
        val engine = GameEngine()
        val hydrant = engine.props.first { it.type == com.example.game.model.PropType.HYDRANT }
        val car = engine.vehicles.first { it.type == VehicleType.MUSCLE_CAR }
        car.x = hydrant.x - 10f
        car.y = hydrant.y
        car.angleRad = 0f
        car.speed = 180f

        repeat(8) { engine.step(0.016f) }

        assertTrue("Expected hydrant geyser to trigger", hydrant.triggered)
        assertTrue("Expected hydrant to emit water particles", engine.particles.isNotEmpty())
        assertTrue("Expected hydrant to launch horizontally or vertically", hydrant.vx != 0f || hydrant.vz != 0f)
    }

    @Test
    fun `expanded 12800x12800 map has 5 islands, 4 airports, 12 railway stations, 7 bridges, and dense traffic and NPCs`() {
        val engine = GameEngine()
        val world = engine.world

        assertEquals(12800f, world.worldWidth)
        assertEquals(12800f, world.worldHeight)
        assertEquals(5, world.islands.size)
        assertTrue("Expected at least 7 inter-island bridges", world.bridges.size >= 7)
        assertTrue("Expected 4 airports (8 runway zones)", world.runwayZones.size >= 8)
        assertTrue("Expected 12 railway stations", world.metroStations.size >= 12)
        assertTrue("Expected mountain peaks across Chiliad & desert", world.mountainPeaks.size >= 8)
        assertTrue("Expected dense NPC population (>= 300)", engine.npcs.size >= 300)
        assertTrue("Expected expanded vehicle fleet (>= 120)", engine.vehicles.size >= 120)
        val diners = world.servicePoints.filter { it.type == com.example.game.model.ServicePointType.DINER }
        assertTrue("Expected eating places across all 5 islands (>= 14)", diners.size >= 14)

        // Bridge midpoints must be drivable land (not water), while open ocean beside them IS water
        val eastBayBridge = world.bridges[0]
        assertTrue(!world.isWater(eastBayBridge.centerX, eastBayBridge.centerY))
        assertTrue(world.isWater(eastBayBridge.centerX, eastBayBridge.centerY + 400f))
    }

    @Test
    fun `eating at diner restores full health and boosts armor`() {
        val engine = GameEngine()
        val diner = engine.world.servicePoints.first { it.type == com.example.game.model.ServicePointType.DINER }
        engine.player.health = 45f
        engine.player.armor = 10f
        engine.player.cash = 100
        engine.player.x = diner.x
        engine.player.y = diner.y

        engine.step(0.016f)

        assertEquals(100f, engine.player.health)
        assertEquals(30f, engine.player.armor)
        assertEquals(85, engine.player.cash)
    }

    @Test
    fun `pedestrians dodge fast oncoming cars and traffic vehicles brake for obstacles`() {
        val engine = GameEngine()
        val trafficCar = engine.vehicles.first { it.isTrafficControlled }
        val lane = engine.world.trafficLanes[trafficCar.laneIndex]
        val dx = lane.endX - lane.startX
        val dy = lane.endY - lane.startY
        val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val dirX = dx / len
        val dirY = dy / len

        // Place civilian directly ahead of fast traffic car -> civilian should enter DODGING_TRAFFIC & car brakes
        val civ = engine.npcs.first { it.role == com.example.game.model.NpcRole.CIVILIAN }
        civ.panicTimerSec = 0f
        civ.x = trafficCar.x + dirX * 55f
        civ.y = trafficCar.y + dirY * 55f
        trafficCar.speed = 160f

        repeat(4) { engine.step(0.016f) }

        assertEquals(com.example.game.model.NpcBehaviorState.DODGING_TRAFFIC, civ.behaviorState)
        assertTrue("Expected traffic car to apply brakes for pedestrian ahead", trafficCar.isBraking)
    }
}
