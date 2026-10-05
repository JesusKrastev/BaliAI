package com.jesuskrastev.bali.ui.screens.games.drive

import kotlin.math.abs

/** Seconds per simulated frame in the tests (60 fps). */
const val TEST_FRAME = 1f / 60f

/** How far behind the car's front bumper the screen still shows the road (the camera is tilted). */
private const val VISIBLE_BEHIND = 1.5f

/**
 * Steering and brake of a careful driver who knows every rule: avoids closed lanes, stops for
 * pedestrians, at STOP lines and red lights, keeps its distance and brakes for a ball. If it
 * cannot finish a route without faults, a situation is unfair.
 *
 * @param engine the run being driven
 * @param overtakesScooters false for a patient driver who never changes lane for a scooter and
 *   waits behind it instead
 * @return lanes to steer by this frame and whether to brake
 */
fun examinerInput(engine: DriveEngine, overtakesScooters: Boolean = true): Pair<Float, Boolean> {
    val car = engine.car
    var targetX = car.targetX
    var brake = false
    val forbidden = mutableSetOf<Int>()
    for (situation in engine.situations) {
        if (situation.status != SituationStatus.UPCOMING) continue
        val distance = situation.y - car.y
        if (distance > 9f) break
        when (situation) {
            is RoadworksSituation -> forbidden += situation.blockedLanes
            is CrosswalkSituation -> if ((situation.walkerOnRoad || situation.walker.alert) &&
                distance > -0.1f && distance < stoppingDistance(car.speed) + 0.9f
            ) brake = true
            is StopSituation -> if (!situation.stopped) {
                if (distance < stoppingDistance(car.speed) + 0.4f) brake = true
            } else if ((situation.crossCar?.x ?: 99f) < 4.6f) {
                brake = true
            }
            is TrafficLightSituation -> if (situation.light != LightColor.GREEN &&
                distance > -0.05f && distance < stoppingDistance(car.speed) + 0.6f
            ) brake = true
            is LeadCarSituation -> situation.lead?.let { lead ->
                val gap = lead.y - CAR_LENGTH / 2 - car.y
                if (gap < stoppingDistance(car.speed) + 0.9f) brake = true
            }
            is BallSituation -> if (situation.ballVisible || situation.child.alert) brake = true
            is ScooterSituation -> if (situation.visible) {
                if (overtakesScooters) forbidden += laneOf(situation.riderX - 0.15f)
                // Stuck behind it (no free lane to overtake): follow at a safe distance.
                val behind = situation.safetyBox().let { it.left < car.x + CAR_WIDTH / 2 && it.right > car.x - CAR_WIDTH / 2 }
                val gap = situation.safetyBox().bottom - car.y
                if (behind && gap < stoppingDistance(car.speed) + 0.4f) brake = true
            }
            is AmbulanceSituation -> if (situation.ambulance != null && !situation.hasPassed) forbidden += situation.ambulanceLane
            is Zone30Situation -> {
                val limit = ZONE_30_SPEED * 0.95f
                val needed = (car.speed * car.speed - limit * limit) / (2f * 5f) + 0.5f
                if (car.speed > limit && distance > -0.2f && distance < needed) brake = true
            }
        }
    }
    // Lanes with something alongside right now (a car just overtaken, a scooter being passed).
    val alongside = mutableSetOf<Int>()
    for (situation in engine.situations) {
        // A patient driver waits behind a scooter however close it gets: it never dodges it.
        if (!overtakesScooters && situation is ScooterSituation) continue
        val boxes = situation.obstacles() + ((situation as? ScooterSituation)?.takeIf { it.visible }?.safetyBox()?.let(::listOf) ?: emptyList())
        boxes.filter { it.top > car.rear - 0.6f && it.bottom < car.y + 1.5f }
            .forEach { alongside += laneOf((it.left + it.right) / 2) }
    }
    forbidden += alongside
    if (laneOf(targetX) in forbidden) {
        (0 until LANES).filter { it !in forbidden }
            .minByOrNull { abs(laneCenter(it) - car.targetX) }
            ?.let { targetX = laneCenter(it) }
    }
    // Never cut across a lane with something alongside: wait until it is behind.
    val from = laneOf(car.x)
    val to = laneOf(targetX)
    if ((minOf(from, to)..maxOf(from, to)).any { it != from && it in alongside }) targetX = car.targetX
    if (!overtakesScooters && engine.situations.any { situation -> situation is ScooterSituation && situation.heads(targetX, car) }) {
        // Waiting behind a scooter is not dodging it: never swerve into the way it is turning off.
        targetX = car.targetX
    }
    return (targetX - car.targetX) to brake
}

/**
 * [examinerInput] that also steers into [target] once it is within [reach] world units ahead, so a
 * test can make the careful driver pick one particular power-up up on its way.
 *
 * @param engine the run being driven
 * @param target the power-up to collect
 * @param reach how far ahead of the car the detour starts
 * @return lanes to steer by this frame and whether to brake
 */
fun examinerFetching(engine: DriveEngine, target: PowerUp, reach: Float = 3f): Pair<Float, Boolean> {
    val (steer, brake) = examinerInput(engine)
    val ahead = target.y - engine.car.y
    return if (!target.collected && ahead in 0f..reach) (target.x - engine.car.targetX) to brake else steer to brake
}

/**
 * Runs this engine frame by frame with [driver] until [until] holds, the run is finished or
 * [maxSeconds] of simulated time pass. Every exit is bounded by that simulated time, which keeps
 * ticking through faults and after the finish line, so a test can never hang on a stuck run.
 *
 * @param maxSeconds simulated seconds after which the loop gives up
 * @param until extra stop condition, checked before each frame (the run's end always stops it)
 * @param driver steering and brake for the next frame
 * @return every event emitted on the way
 */
fun DriveEngine.drive(
    maxSeconds: Float = 240f,
    until: (DriveEngine) -> Boolean = { false },
    driver: (DriveEngine) -> Pair<Float, Boolean>,
): List<DriveEvent> {
    val events = mutableListOf<DriveEvent>()
    var elapsed = 0f
    while (phase != DrivePhase.FINISHED && !until(this) && elapsed < maxSeconds) {
        val (steer, brake) = driver(this)
        update(TEST_FRAME, steer, brake)
        events += drainEvents()
        elapsed += TEST_FRAME
    }
    return events
}

/**
 * A driver that waits for anything in its lane: [examinerInput] that also brakes while another
 * actor (a scooter, a leading car...) is ahead in its lane, so it only ever drives on once the way
 * is clear. A run it cannot finish would have an actor that never leaves.
 *
 * @param engine the run being driven
 * @return lanes to steer by this frame and whether to brake
 */
fun waitingInput(engine: DriveEngine): Pair<Float, Boolean> {
    val (steer, brake) = examinerInput(engine, overtakesScooters = false)
    val car = engine.car
    val blocked = engine.situations.any { situation ->
        situation !is RoadworksSituation && situation.status != SituationStatus.PASSED && situation.obstacles().any {
            it.right > car.x - 0.4f && it.left < car.x + 0.4f && it.bottom > car.y && it.bottom < car.y + 9f
        }
    }
    return steer to (brake || blocked)
}

/**
 * Pairs of actors that overlap right now, owned by different situations and at least partly on
 * screen: a car driving through a barrier, a scooter spawned inside an ambulance... Overlaps
 * off screen are not reported, since no player can see them.
 *
 * @return a description of each overlapping pair, empty when every actor is solid
 */
fun DriveEngine.visibleOverlaps(): List<String> {
    val actors = situations.flatMapIndexed { index, situation -> situation.obstacles().map { Triple(index, situation.kind, it) } }
    fun onScreen(box: Box) = box.top > car.y - VISIBLE_BEHIND && box.bottom < car.y + VISIBLE_AHEAD
    val overlaps = mutableListOf<String>()
    for (i in actors.indices) for (j in i + 1 until actors.size) {
        val (indexA, kindA, boxA) = actors[i]
        val (indexB, kindB, boxB) = actors[j]
        if (indexA != indexB && boxA.overlaps(boxB) && (onScreen(boxA) || onScreen(boxB))) {
            overlaps += "$kindA $boxA overlaps $kindB $boxB with the car at y=${car.y}"
        }
    }
    return overlaps
}

/** True when a car at [x] would end up in this scooter's safety zone, which it is not already in. */
private fun ScooterSituation.heads(x: Float, car: PlayerCar): Boolean {
    if (!visible) return false
    val zone = safetyBox()
    fun inZone(at: Float) = zone.left < at + CAR_WIDTH / 2 && zone.right > at - CAR_WIDTH / 2
    return zone.bottom < car.y + 2.5f && zone.top > car.rear - 1f && inZone(x) && !inZone(car.x)
}
