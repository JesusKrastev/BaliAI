package com.jesuskrastev.bali.ui.screens.games.drive

import kotlin.math.abs

/** Seconds per simulated frame in the tests (60 fps). */
const val TEST_FRAME = 1f / 60f

/**
 * Steering and brake of a careful driver who knows every rule: avoids closed lanes, stops for
 * pedestrians, at STOP lines and red lights, keeps its distance and brakes for a ball. If it
 * cannot finish a route without faults, a situation is unfair.
 *
 * @return lanes to steer by this frame and whether to brake
 */
fun examinerInput(engine: DriveEngine): Pair<Float, Boolean> {
    val car = engine.car
    var targetX = car.targetX
    var brake = false
    for (situation in engine.situations) {
        if (situation.status != SituationStatus.UPCOMING) continue
        val distance = situation.y - car.y
        if (distance > 9f) break
        when (situation) {
            is RoadworksSituation -> if (situation.blockedLanes.isNotEmpty()) {
                val free = (0 until LANES).filter { it !in situation.blockedLanes }
                    .minBy { abs(laneCenter(it) - car.targetX) }
                targetX = laneCenter(free)
            }
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
        }
    }
    return (targetX - car.targetX) to brake
}

/**
 * Runs this engine frame by frame with [driver] until the run is finished or [maxSeconds] pass.
 *
 * @return every event emitted on the way
 */
fun DriveEngine.drive(maxSeconds: Float = 240f, driver: (DriveEngine) -> Pair<Float, Boolean>): List<DriveEvent> {
    val events = mutableListOf<DriveEvent>()
    var elapsed = 0f
    while (phase != DrivePhase.FINISHED && elapsed < maxSeconds) {
        val (steer, brake) = driver(this)
        update(TEST_FRAME, steer, brake)
        events += drainEvents()
        elapsed += TEST_FRAME
    }
    return events
}
