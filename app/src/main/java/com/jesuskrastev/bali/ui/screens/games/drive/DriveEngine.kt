package com.jesuskrastev.bali.ui.screens.games.drive

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// World units: one unit is one lane wide. x runs from the left kerb (0) to the right kerb of the
// traffic lanes ([LANES]); a parking lane and a pavement follow on the right. y grows forward.

/** Traffic lanes of the one-way avenue the player drives along. */
const val LANES = 3

/** Right edge of the parking lane that runs along the right kerb. */
const val PARKING_EDGE = 3.6f

/** Width of every car, in lanes. */
const val CAR_WIDTH = 0.56f

/** Length of every car, in lanes. */
const val CAR_LENGTH = 0.95f

/** Below this speed a car counts as fully stopped. */
const val STOPPED_SPEED = 0.08f

/** Seconds of "3, 2, 1" before the car moves. */
const val COUNTDOWN_SECONDS = 3f

/** A fault freezes the run this long before it resumes by itself. */
const val FAULT_PAUSE_SECONDS = 3.2f

/** A tap resumes a fault only after this long, so the explanation is never skipped by accident. */
const val FAULT_MIN_PAUSE_SECONDS = 0.8f

/** Multiplies world speed into the km/h shown on the speedometer (cruise peaks just under 50). */
const val KMH_PER_UNIT = 12.8f

private const val ACCELERATION = 2.4f
private const val BRAKING = 5.0f
private const val CRUISE_START = 3.0f
private const val CRUISE_END = 3.9f
private const val STEER_RESPONSE = 13f
private const val ACTIVE_AHEAD = 12f
private const val ACTIVE_BEHIND = 8f
private const val HINT_DISTANCE = 7.5f
private const val MAX_MULTIPLIER = 5
private const val SITUATION_POINTS = 100
private const val BONUS_POINTS = 50
private const val STAR_POINTS = 5
private const val PERFECT_RUN_POINTS = 300

/** Distance a car at [speed] needs to stop with the brake fully held. */
fun stoppingDistance(speed: Float): Float = speed * speed / (2f * BRAKING)

/** Centre x of [lane]. */
fun laneCenter(lane: Int): Float = lane + 0.5f

/** Lane under [x], clamped to the traffic lanes. */
fun laneOf(x: Float): Int = floor(x).toInt().coerceIn(0, LANES - 1)

/** Axis-aligned rectangle in world units, used for every collision. */
data class Box(val left: Float, val bottom: Float, val right: Float, val top: Float) {
    /** True when this box and [other] overlap. */
    fun overlaps(other: Box): Boolean =
        left < other.right && right > other.left && bottom < other.top && top > other.bottom
}

/** Situations a run is made of; each teaches one rule. */
enum class SituationKind(val icon: String, val hint: String) {
    ROADWORKS("🚧", "Obras: cambia de carril"),
    CROSSWALK("🚶", "Paso de peatones: cede el paso"),
    STOP("🛑", "STOP: para del todo y mira"),
    TRAFFIC_LIGHT("🚦", "Semáforo: en rojo, detente"),
    LEAD_CAR("🚙", "Coche delante: guarda distancia"),
    BALL("⚽", "Balón: frena, puede salir un niño"),
}

/** Phase of a run. */
enum class DrivePhase { COUNTDOWN, DRIVING, FAULT, FINISHED }

/** Where a situation stands for the player. */
enum class SituationStatus { UPCOMING, PASSED, FAILED }

/** Traffic-light colour. */
enum class LightColor { GREEN, AMBER, RED }

/** The player's car. [y] is its front bumper. */
class PlayerCar {
    var x = laneCenter(1)
    var targetX = laneCenter(1)
    var y = 0f
    var speed = 0f
    var cruise = CRUISE_START
    var braking = false

    /** Rear bumper. */
    val rear: Float get() = y - CAR_LENGTH

    /** Collision box. */
    fun box(): Box = Box(x - CAR_WIDTH / 2, rear, x + CAR_WIDTH / 2, y)
}

/**
 * A moving car that is not the player's.
 *
 * @param horizontal true when it drives along a cross street (along x) instead of the avenue
 */
class Vehicle(var x: Float, var y: Float, var speed: Float, val color: Int, val horizontal: Boolean = false) {
    var braking = false

    /** Collision box; [x], [y] are its centre. */
    fun box(): Box = if (horizontal) {
        Box(x - CAR_LENGTH / 2, y - CAR_WIDTH / 2, x + CAR_LENGTH / 2, y + CAR_WIDTH / 2)
    } else {
        Box(x - CAR_WIDTH / 2, y - CAR_LENGTH / 2, x + CAR_WIDTH / 2, y + CAR_LENGTH / 2)
    }
}

/** A person on foot; [stride] drives the walking animation. */
class Walker(var x: Float, var y: Float, val style: Int, val isChild: Boolean = false) {
    var vx = 0f
    var stride = 0f
    var visible = true
    var alert = false

    /** Collision box. */
    fun box(): Box {
        val half = if (isChild) 0.13f else 0.16f
        return Box(x - half, y - half, x + half, y + half)
    }

    /** Moves along x at [vx] for [dt] seconds. */
    fun step(dt: Float) {
        x += vx * dt
        if (vx != 0f) stride += dt * 9f
    }
}

/** A collectible star; stars give points and keep the eyes moving between situations. */
class Star(val x: Float, val y: Float) {
    var collected = false
    var missed = false
}

/** A car parked in the right-hand parking lane (scenery). */
data class ParkedCar(val y: Float, val color: Int)

/** One-shot things that happened in a frame, for the screen to turn into sound, haptics and popups. */
sealed interface DriveEvent {
    /** The countdown reached [number]; 0 is "go". */
    data class Countdown(val number: Int) : DriveEvent

    /** A situation should be announced to a first-time player. */
    data class Hint(val kind: SituationKind) : DriveEvent

    /** A situation was handled well. */
    data class Resolved(
        val kind: SituationKind,
        val points: Int,
        val multiplier: Int,
        val praise: String,
        val bonusLabel: String?,
    ) : DriveEvent

    /** A rule was broken; [message] is the one-sentence rule. */
    data class Faulted(val kind: SituationKind, val message: String) : DriveEvent

    /** A star was collected; [streak] counts consecutive stars. */
    data class Pickup(val points: Int, val streak: Int, val x: Float, val y: Float) : DriveEvent

    /** The score just went past the personal best. */
    data object NewRecord : DriveEvent

    /** The finish line was crossed. */
    data class Finished(val perfect: Boolean) : DriveEvent
}

/**
 * Outcome of a finished run.
 *
 * @param score total points
 * @param resolved situations handled without a fault
 * @param situations situations in the route
 * @param faults rules broken, in order, as their one-sentence explanation
 * @param starsCollected stars picked up
 * @param starsTotal stars on the route
 * @param durationSeconds driving time, countdown excluded
 */
data class DriveSummary(
    val score: Int,
    val resolved: Int,
    val situations: Int,
    val faults: List<String>,
    val starsCollected: Int,
    val starsTotal: Int,
    val durationSeconds: Int,
) {
    /** 3 stars for a clean run, 2 for one fault, 1 otherwise. */
    val rating: Int get() = when (faults.size) {
        0 -> 3
        1 -> 2
        else -> 1
    }
}

/**
 * Simulates one Bali Drive run: the player's car on a one-way avenue, the route of situations
 * that each test one rule, the stars between them, and the score.
 *
 * It is plain Kotlin with no Compose or Android types, so the rules can be unit tested; the
 * screen calls [update] once per frame and draws whatever state it finds.
 *
 * @param seed picks the order of situations and the star patterns
 * @param bestScore personal best, to announce [DriveEvent.NewRecord] once it is beaten
 * @param showHints whether to emit [DriveEvent.Hint] as each situation approaches
 */
class DriveEngine(seed: Long, private val bestScore: Int = 0, private val showHints: Boolean = false) {
    private val random = Random(seed)

    val car = PlayerCar()
    val situations: List<Situation> = buildRoute()
    val finishY: Float = situations.last().layoutEnd + 6f
    val stars: List<Star> = buildStars()
    val parkedCars: List<ParkedCar> = buildParkedCars()

    var phase = DrivePhase.COUNTDOWN
        private set
    var countdown = COUNTDOWN_SECONDS
        private set
    var drivingTime = 0f
        private set
    var faultTime = 0f
        private set
    var score = 0
        private set
    var combo = 0
        private set
    var lastFault: DriveEvent.Faulted? = null
        private set

    private var resolvedCount = 0
    private val faultMessages = mutableListOf<String>()
    private var pickupStreak = 0
    private var recordAnnounced = false
    private val pendingEvents = mutableListOf<DriveEvent>()

    /** Score multiplier earned by the current streak of handled situations. */
    val multiplier: Int get() = combo.coerceIn(1, MAX_MULTIPLIER)

    /** Share of the route already driven, 0–1. */
    val progress: Float get() = (car.y / finishY).coerceIn(0f, 1f)

    /** Speed shown on the speedometer. */
    val speedKmh: Int get() = (car.speed * KMH_PER_UNIT).toInt()

    /** Returns and forgets the events since the last call. */
    fun drainEvents(): List<DriveEvent> = pendingEvents.toList().also { pendingEvents.clear() }

    /**
     * Advances the run by [dt] seconds.
     *
     * @param dt seconds since the last frame (the caller caps it, so a paused app does not jump)
     * @param steer lanes to move the car sideways by, from the player's drag since the last frame
     * @param brake whether the player is holding the brake
     */
    fun update(dt: Float, steer: Float, brake: Boolean) {
        when (phase) {
            DrivePhase.COUNTDOWN -> updateCountdown(dt)
            DrivePhase.DRIVING -> drive(dt, steer, brake)
            DrivePhase.FAULT -> {
                faultTime += dt
                if (faultTime >= FAULT_PAUSE_SECONDS) resume()
            }
            DrivePhase.FINISHED -> coastPastFinish(dt)
        }
    }

    /** Resumes after a fault when the player taps, once the explanation has been on screen briefly. */
    fun resumeFromFault() {
        if (phase == DrivePhase.FAULT && faultTime >= FAULT_MIN_PAUSE_SECONDS) resume()
    }

    /** The finished run's figures. */
    fun summary(): DriveSummary = DriveSummary(
        score = score,
        resolved = resolvedCount,
        situations = situations.size,
        faults = faultMessages.toList(),
        starsCollected = stars.count { it.collected },
        starsTotal = stars.size,
        durationSeconds = drivingTime.toInt().coerceAtLeast(1),
    )

    /** Situation that still lies ahead and is closest, for the progress bar and the tests. */
    fun nextSituation(): Situation? = situations.firstOrNull { it.status == SituationStatus.UPCOMING }

    /** Ticks the "3, 2, 1, go" countdown. */
    private fun updateCountdown(dt: Float) {
        val before = ceil(countdown).toInt()
        countdown -= dt
        val after = ceil(countdown).toInt()
        if (after != before) pendingEvents += DriveEvent.Countdown(after.coerceAtLeast(0))
        if (countdown <= 0f) {
            countdown = 0f
            phase = DrivePhase.DRIVING
        }
    }

    /** One frame of driving: car physics, situations, stars, record and finish line. */
    private fun drive(dt: Float, steer: Float, brake: Boolean) {
        drivingTime += dt
        moveCar(dt, steer, brake)

        for (situation in situations) {
            val distance = situation.y - car.y
            // A pending situation is always updated: the car ahead travels with the player.
            if (distance > ACTIVE_AHEAD) continue
            if (situation.status != SituationStatus.UPCOMING && car.rear - situation.layoutEnd > ACTIVE_BEHIND) continue
            if (showHints && !situation.hinted && distance < HINT_DISTANCE) {
                situation.hinted = true
                pendingEvents += DriveEvent.Hint(situation.kind)
            }
            val checkRules = situation.status == SituationStatus.UPCOMING
            val fault = situation.update(dt, car, checkRules)
            if (checkRules && fault != null) {
                fault(situation, fault)
                return
            }
            if (checkRules && situation.isCleared(car)) resolve(situation)
        }

        collectStars()
        if (!recordAnnounced && bestScore > 0 && score > bestScore) {
            recordAnnounced = true
            pendingEvents += DriveEvent.NewRecord
        }
        if (car.rear > finishY) finish()
    }

    /** Steers towards the dragged position and accelerates towards cruise speed or brakes. */
    private fun moveCar(dt: Float, steer: Float, brake: Boolean) {
        car.cruise = CRUISE_START + (CRUISE_END - CRUISE_START) * progress
        val minX = CAR_WIDTH / 2 + 0.04f
        val maxX = LANES - CAR_WIDTH / 2 - 0.04f
        car.targetX = (car.targetX + steer).coerceIn(minX, maxX)
        car.x += (car.targetX - car.x) * min(1f, STEER_RESPONSE * dt)
        car.braking = brake
        car.speed = when {
            brake -> max(0f, car.speed - BRAKING * dt)
            car.speed < car.cruise -> min(car.cruise, car.speed + ACCELERATION * dt)
            else -> max(car.cruise, car.speed - ACCELERATION * dt)
        }
        car.y += car.speed * dt
    }

    /** Scores a situation handled without breaking its rule. */
    private fun resolve(situation: Situation) {
        situation.status = SituationStatus.PASSED
        resolvedCount++
        combo++
        val bonus = situation.bonus()
        val points = (SITUATION_POINTS + if (bonus != null) BONUS_POINTS else 0) * multiplier
        score += points
        pendingEvents += DriveEvent.Resolved(situation.kind, points, multiplier, praise(), bonus)
    }

    /** Freezes the run on a broken rule; the streak is lost but the run goes on. */
    private fun fault(situation: Situation, message: String) {
        situation.status = SituationStatus.FAILED
        situation.clearAfterFault(car)
        car.speed = 0f
        car.braking = false
        combo = 0
        faultMessages += message
        phase = DrivePhase.FAULT
        faultTime = 0f
        val event = DriveEvent.Faulted(situation.kind, message)
        lastFault = event
        pendingEvents += event
    }

    /** Leaves the fault freeze and drives on from a standstill. */
    private fun resume() {
        phase = DrivePhase.DRIVING
        faultTime = 0f
    }

    /** Picks up the stars under the car and breaks the streak on any star left behind. */
    private fun collectStars() {
        val box = car.box()
        for (star in stars) {
            if (star.collected || star.missed) continue
            if (star.y > car.y + 0.3f) break
            val reach = Box(star.x - 0.17f, star.y - 0.17f, star.x + 0.17f, star.y + 0.17f)
            if (box.overlaps(reach)) {
                star.collected = true
                pickupStreak++
                val points = STAR_POINTS * multiplier
                score += points
                pendingEvents += DriveEvent.Pickup(points, pickupStreak, star.x, star.y)
            } else if (star.y < car.rear - 0.2f) {
                star.missed = true
                pickupStreak = 0
            }
        }
    }

    /** Crosses the finish line and closes the run. */
    private fun finish() {
        // Only a car ahead can still be pending here (it was still driving off): the player kept
        // their distance all the way to the line, so it counts as handled.
        situations.filter { it.status == SituationStatus.UPCOMING }.forEach(::resolve)
        val perfect = faultMessages.isEmpty()
        if (perfect) score += PERFECT_RUN_POINTS
        phase = DrivePhase.FINISHED
        car.braking = false
        pendingEvents += DriveEvent.Finished(perfect)
    }

    /** After the finish line the car rolls gently to a stop. */
    private fun coastPastFinish(dt: Float) {
        car.speed = max(0f, car.speed - 1.6f * dt)
        car.braking = car.speed > 0f
        car.y += car.speed * dt
        car.x += (car.targetX - car.x) * min(1f, STEER_RESPONSE * dt)
        situations.forEach { it.update(dt, car, checkRules = false) }
    }

    /** Words of praise that grow with the streak. */
    private fun praise(): String {
        val tiers = listOf(
            listOf("¡Bien visto!", "¡Eso es!", "¡Muy bien!"),
            listOf("¡Qué control!", "¡Así se conduce!", "¡Fino!"),
            listOf("¡Conducción impecable!", "¡Crack del volante!", "¡De libro!"),
            listOf("¡Imparable!", "¡Nivel examinador!", "¡Leyenda del asfalto!"),
        )
        val tier = tiers[((combo - 1) / 2).coerceIn(0, tiers.lastIndex)]
        return tier[random.nextInt(tier.size)]
    }

    /**
     * Lays out the route: the first two situations teach the two controls (roadworks: steer;
     * crosswalk: brake), then a shuffle of the rest with no kind twice in a row.
     */
    private fun buildRoute(): List<Situation> {
        val pool = listOf(
            SituationKind.STOP, SituationKind.TRAFFIC_LIGHT, SituationKind.LEAD_CAR,
            SituationKind.BALL, SituationKind.ROADWORKS, SituationKind.CROSSWALK,
            SituationKind.TRAFFIC_LIGHT, SituationKind.LEAD_CAR, SituationKind.STOP,
        )
        val opening = listOf(SituationKind.ROADWORKS, SituationKind.CROSSWALK)
        var kinds = opening + pool.shuffled(random)
        var attempts = 0
        while (kinds.zipWithNext().any { (a, b) -> a == b } && attempts++ < 100) {
            kinds = opening + pool.shuffled(random)
        }
        var y = 10f
        var lightCount = 0
        return kinds.map { kind ->
            val situation = when (kind) {
                SituationKind.ROADWORKS -> RoadworksSituation(y, wide = y > 20f && random.nextBoolean())
                SituationKind.CROSSWALK -> CrosswalkSituation(y, fromLeft = random.nextBoolean(), style = random.nextInt(4))
                SituationKind.STOP -> StopSituation(y, crossColor = random.nextInt(NPC_COLORS))
                SituationKind.TRAFFIC_LIGHT -> TrafficLightSituation(
                    y,
                    startsRed = lightCount++ % 2 == 1,
                    crossColor = random.nextInt(NPC_COLORS),
                )
                SituationKind.LEAD_CAR -> LeadCarSituation(y, color = random.nextInt(NPC_COLORS))
                SituationKind.BALL -> BallSituation(y, style = random.nextInt(4))
            }
            y = situation.layoutEnd + 6.5f + random.nextFloat() * 1.5f
            situation
        }
    }

    /** Places star patterns in the free stretches between situations. */
    private fun buildStars(): List<Star> {
        val result = mutableListOf<Star>()
        var from = 2.5f
        for (situation in situations + null) {
            val to = (situation?.layoutStart ?: finishY) - 2.2f
            var y = from
            while (to - y > 3.2f) {
                val pattern = random.nextInt(3)
                val lane = random.nextInt(LANES)
                when (pattern) {
                    0 -> repeat(4) { result += Star(laneCenter(lane), y + it * 0.75f) }
                    1 -> listOf(0, 1, 2, 1, 0).let { lanes -> if (random.nextBoolean()) lanes else lanes.map { 2 - it } }
                        .forEachIndexed { i, l -> result += Star(laneCenter(l), y + i * 0.75f) }
                    else -> {
                        val other = (lane + 1 + random.nextInt(LANES - 1)) % LANES
                        repeat(3) { result += Star(laneCenter(lane), y + it * 0.75f) }
                        repeat(3) { result += Star(laneCenter(other), y + 2.6f + it * 0.75f) }
                    }
                }
                y += 5.5f
            }
            from = (situation?.layoutEnd ?: finishY) + 1.6f
        }
        return result.sortedBy { it.y }
    }

    /** Fills the parking lane with parked cars, leaving crossings and junctions clear. */
    private fun buildParkedCars(): List<ParkedCar> {
        val result = mutableListOf<ParkedCar>()
        var y = -3f
        while (y < finishY + 12f) {
            val blocked = situations.any { y > it.parkingClearStart - 0.6f && y < it.parkingClearEnd + 0.6f }
            if (!blocked && random.nextFloat() < 0.55f) result += ParkedCar(y, random.nextInt(NPC_COLORS))
            y += 1.25f
        }
        situations.filterIsInstance<BallSituation>().forEach {
            result += ParkedCar(it.y - 0.75f, random.nextInt(NPC_COLORS))
            result += ParkedCar(it.y + 0.75f, random.nextInt(NPC_COLORS))
        }
        // Never two cars in the same bay.
        val spaced = mutableListOf<ParkedCar>()
        result.sortedBy { it.y }.forEach { if (spaced.isEmpty() || it.y - spaced.last().y >= CAR_LENGTH + 0.2f) spaced += it }
        return spaced
    }
}

/** Number of body colours non-player cars pick from (the renderer owns the palette). */
const val NPC_COLORS = 6

/**
 * One rule to respect at a point of the route.
 *
 * @param y world position of the situation's key line (stop line, zebra start, works start...)
 */
sealed class Situation(val kind: SituationKind, val y: Float) {
    var status = SituationStatus.UPCOMING
    var hinted = false

    /** Where the situation's road markings start, for laying out the route. */
    open val layoutStart: Float get() = y

    /** Where the situation's road markings end, for laying out the route. */
    abstract val layoutEnd: Float

    /** Stretch of parking lane kept free of parked cars. */
    open val parkingClearStart: Float get() = layoutStart
    open val parkingClearEnd: Float get() = layoutEnd

    /**
     * Moves this situation's actors and checks its rule.
     *
     * @param car the player's car
     * @param checkRules false once the situation is settled: actors keep moving, nothing is judged
     * @return the broken rule's one-sentence explanation, or null
     */
    abstract fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String?

    /** True once the player has driven past without a fault. */
    open fun isCleared(car: PlayerCar): Boolean = car.rear > layoutEnd

    /** Label of the extra points earned by handling it especially well, or null. */
    open fun bonus(): String? = null

    /** Makes the scene safe to drive on after a fault (moves actors out of the way, etc.). */
    abstract fun clearAfterFault(car: PlayerCar)
}

/** Roadworks closing the player's lane (or two lanes when [wide]): steer into a free one. */
class RoadworksSituation(y: Float, val wide: Boolean) : Situation(SituationKind.ROADWORKS, y) {
    val length = 2.4f
    var blockedLanes: Set<Int> = emptySet()
        private set
    private var early: Boolean? = null

    override val layoutEnd: Float get() = y + length

    /** Collision boxes of the closed lanes. */
    fun barriers(): List<Box> = blockedLanes.map { Box(it + 0.06f, y, it + 0.94f, y + length) }

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        if (blockedLanes.isEmpty()) {
            // Decided just before it comes into view: the works always close the player's lane.
            val lane = laneOf(car.targetX)
            blockedLanes = if (!wide) setOf(lane) else when (lane) {
                0 -> setOf(0, 1)
                2 -> setOf(1, 2)
                else -> if (y.toInt() % 2 == 0) setOf(0, 1) else setOf(1, 2)
            }
        }
        if (!checkRules) return null
        if (early == null && car.y > y - 2f) {
            early = barriers().none { it.left < car.x + CAR_WIDTH / 2 && it.right > car.x - CAR_WIDTH / 2 }
        }
        return if (barriers().any { it.overlaps(car.box()) }) RULE_ROADWORKS else null
    }

    override fun bonus(): String? = if (early == true) "¡Con antelación!" else null

    override fun clearAfterFault(car: PlayerCar) {
        car.y = y - 0.35f
        val free = (0 until LANES).filter { it !in blockedLanes }.minBy { abs(laneCenter(it) - car.x) }
        car.x = laneCenter(free)
        car.targetX = car.x
    }
}

/** A pedestrian crossing the zebra: stop before it and let them cross. */
class CrosswalkSituation(y: Float, val fromLeft: Boolean, style: Int) : Situation(SituationKind.CROSSWALK, y) {
    val zebraLength = 0.8f
    val walker = Walker(if (fromLeft) -0.32f else PARKING_EDGE + 0.3f, y + zebraLength / 2, style)
    private val endX = if (fromLeft) PARKING_EDGE + 0.3f else -0.32f
    private var triggered = false
    private var waitTime = 0f
    private var stoppedAtLine = false

    override val layoutEnd: Float get() = y + zebraLength
    override val parkingClearStart: Float get() = y - 1.2f

    /** Whether the pedestrian is on the carriageway right now. */
    val walkerOnRoad: Boolean get() = walker.x > -0.04f && walker.x < PARKING_EDGE + 0.02f

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        val distance = y - car.y
        if (!triggered && distance <= max(2.6f, stoppingDistance(car.speed) + 2.4f)) {
            triggered = true
            walker.alert = true
        }
        if (triggered && walker.vx == 0f && abs(walker.x - endX) > 0.01f) {
            waitTime += dt
            if (waitTime > 0.35f) walker.vx = if (fromLeft) 1.7f else -1.7f
        }
        walker.step(dt)
        if ((fromLeft && walker.x >= endX) || (!fromLeft && walker.x <= endX)) {
            walker.x = endX
            walker.vx = 0f
            walker.alert = false
        }
        if (!checkRules) return null
        if (car.speed < STOPPED_SPEED && distance in 0f..1.6f) stoppedAtLine = true
        val onZebra = car.y > y - 0.02f && car.rear < y + zebraLength
        return if ((walkerOnRoad && onZebra) || walker.box().overlaps(car.box())) RULE_CROSSWALK else null
    }

    override fun bonus(): String? = if (stoppedAtLine) "¡Parada perfecta!" else null

    override fun clearAfterFault(car: PlayerCar) {
        walker.x = endX
        walker.vx = 0f
        walker.alert = false
        triggered = true
    }
}

/** A STOP junction: stop completely before the line, then let the crossing car go by. */
class StopSituation(y: Float, private val crossColor: Int) : Situation(SituationKind.STOP, y) {
    val crossStart = y + 0.15f
    val crossEnd = y + 1.55f
    var crossCar: Vehicle? = null
        private set
    var stopped = false
        private set
    private var stoppedNearLine = false

    override val layoutEnd: Float get() = crossEnd
    override val parkingClearStart: Float get() = y - 1.4f

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        crossCar?.let { it.x += it.speed * dt }
        if (!checkRules) return null
        if (!stopped && car.speed < STOPPED_SPEED && car.y in (y - 1.8f)..(y + 0.05f)) {
            stopped = true
            stoppedNearLine = car.y > y - 0.9f
            // Someone is coming along the cross street: the player has to look and wait for it.
            crossCar = Vehicle(-2.4f, crossStart + 0.36f, 4.2f, crossColor, horizontal = true)
        }
        if (!stopped && car.y > y + 0.1f) return RULE_STOP
        if (crossCar?.box()?.overlaps(car.box()) == true) return RULE_STOP_YIELD
        return null
    }

    override fun bonus(): String? = if (stoppedNearLine) "¡Parada en la línea!" else null

    override fun clearAfterFault(car: PlayerCar) {
        stopped = true
        crossCar?.x = 8f
    }
}

/** A traffic light that turns amber and red as the player approaches: stop on red, go on green. */
class TrafficLightSituation(y: Float, startsRed: Boolean, private val crossColor: Int) :
    Situation(SituationKind.TRAFFIC_LIGHT, y) {
    val crossStart = y + 0.15f
    val crossEnd = y + 1.55f
    var light = if (startsRed) LightColor.RED else LightColor.GREEN
        private set
    var crossCar: Vehicle? = null
        private set
    private var timer = 0f
    private var stoppedFor = 0f
    private var cycled = startsRed
    private var stoppedNearLine = false
    private var sawRed = startsRed

    override val layoutEnd: Float get() = crossEnd
    override val parkingClearStart: Float get() = y - 1.4f

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        timer += dt
        crossCar?.let { it.x += it.speed * dt }
        val distance = y - car.y
        if (light == LightColor.RED && crossCar == null && distance < 6f) {
            crossCar = Vehicle(7f, crossEnd - 0.36f, -4.2f, crossColor, horizontal = true)
        }
        when (light) {
            LightColor.GREEN -> if (!cycled && distance <= stoppingDistance(car.speed) + car.speed + 0.6f) {
                light = LightColor.AMBER
                cycled = true
                timer = 0f
            }
            LightColor.AMBER -> if (timer >= 1f) {
                light = LightColor.RED
                sawRed = true
                timer = 0f
            }
            LightColor.RED -> {
                if (car.speed < STOPPED_SPEED && car.y < y + 0.05f) {
                    stoppedFor += dt
                    if (distance < 0.9f) stoppedNearLine = true
                }
                if (stoppedFor >= 1.1f) {
                    light = LightColor.GREEN
                    timer = 0f
                }
            }
        }
        if (!checkRules) return null
        if (light == LightColor.RED && car.y > y + 0.1f) return RULE_RED_LIGHT
        if (crossCar?.box()?.overlaps(car.box()) == true) return RULE_RED_LIGHT
        return null
    }

    override fun bonus(): String? = if (sawRed && stoppedNearLine) "¡Parada en la línea!" else null

    override fun clearAfterFault(car: PlayerCar) {
        light = LightColor.GREEN
        crossCar?.x = -8f
    }
}

/** A slower car ahead that brakes hard: keep enough distance to stop behind it (or change lane). */
class LeadCarSituation(y: Float, private val color: Int) : Situation(SituationKind.LEAD_CAR, y) {
    var lead: Vehicle? = null
        private set
    private var stage = 0 // 0 waiting, 1 cruising, 2 braking, 3 holding, 4 leaving
    private var timer = 0f
    private var minGap = Float.MAX_VALUE
    private var overtook = false
    private var done = false

    override val layoutStart: Float get() = y - 6.6f
    override val layoutEnd: Float get() = y + 8f
    override val parkingClearStart: Float get() = Float.MAX_VALUE
    override val parkingClearEnd: Float get() = -Float.MAX_VALUE

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        if (stage == 0) {
            if (car.y < y - 6.6f) return null
            lead = Vehicle(laneCenter(laneOf(car.targetX)), car.y + 6.6f + CAR_LENGTH / 2, car.cruise * 0.55f, color)
            stage = 1
        }
        val lead = lead ?: return null
        timer += dt
        val gap = lead.y - CAR_LENGTH / 2 - car.y
        when (stage) {
            1 -> if (gap <= 3.0f || timer > 2.6f) { stage = 2; timer = 0f }
            2 -> {
                lead.braking = true
                lead.speed = max(0f, lead.speed - 5.5f * dt)
                if (lead.speed == 0f) { stage = 3; timer = 0f }
            }
            3 -> if (timer > 1.3f) { stage = 4; lead.braking = false }
            4 -> lead.speed = min(5.5f, lead.speed + 3f * dt)
        }
        lead.y += lead.speed * dt
        if (stage == 4 && gap > 8.5f) {
            // Gone over the horizon before it reaches the next situation.
            this.lead = null
            done = true
            return null
        }
        if (!checkRules) return null
        val sameLane = abs(lead.x - car.x) < CAR_WIDTH
        if (sameLane && gap > -CAR_LENGTH) minGap = min(minGap, gap)
        if (car.rear > lead.y + CAR_LENGTH / 2) overtook = true
        if (stage == 4 && gap > 7f) done = true
        return if (lead.box().overlaps(car.box())) RULE_LEAD_CAR else null
    }

    override fun isCleared(car: PlayerCar): Boolean = done || overtook

    override fun bonus(): String? = if (!overtook && minGap >= 0.7f) "¡Buena distancia!" else null

    override fun clearAfterFault(car: PlayerCar) {
        val lead = lead ?: return
        lead.y = max(lead.y, car.y + 1.6f + CAR_LENGTH / 2)
        lead.speed = 4f
        lead.braking = false
        stage = 4
        done = true
    }
}

/** A ball rolls out between parked cars and a child runs after it: slow down before it happens. */
class BallSituation(y: Float, style: Int) : Situation(SituationKind.BALL, y) {
    var ballX = 3.3f
        private set
    var ballSpin = 0f
        private set
    var ballVisible = false
        private set
    val child = Walker(3.4f, y + 0.05f, style, isChild = true).apply { visible = false }
    private var triggered = false
    private var timer = 0f
    private var triggerSpeed = 0f
    private var minSpeed = Float.MAX_VALUE
    private var judged = false

    override val layoutEnd: Float get() = y + 0.5f
    override val parkingClearStart: Float get() = y - 1.5f
    override val parkingClearEnd: Float get() = y + 1.5f

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        val distance = y - car.y
        if (!triggered && distance <= max(2.8f, stoppingDistance(car.speed) + 2.4f)) {
            triggered = true
            ballVisible = true
            triggerSpeed = car.speed
        }
        if (triggered) {
            timer += dt
            if (ballX > -1.5f) {
                ballX -= 2.6f * dt
                ballSpin += dt * 720f
            } else {
                ballVisible = false
            }
            if (timer > 0.55f && child.x > -0.4f) {
                child.visible = true
                child.alert = true
                child.vx = -1.6f
            }
            child.step(dt)
            if (child.x <= -0.4f) {
                child.vx = 0f
                child.alert = false
            }
            minSpeed = min(minSpeed, car.speed)
        }
        if (!checkRules) return null
        if (child.visible && child.box().overlaps(car.box())) return RULE_BALL
        if (!judged && car.rear > y + 0.4f) {
            judged = true
            // Only judged when the player was moving at a real speed when the ball appeared.
            if (triggerSpeed > 1.2f && minSpeed > triggerSpeed * 0.55f) return RULE_BALL
        }
        return null
    }

    override fun bonus(): String? = if (minSpeed < STOPPED_SPEED) "¡Gran anticipación!" else null

    override fun clearAfterFault(car: PlayerCar) {
        child.x = -0.4f
        child.vx = 0f
        child.alert = false
        judged = true
        ballVisible = false
    }
}

const val RULE_ROADWORKS = "Si las obras cortan tu carril, cámbiate a uno libre con antelación."
const val RULE_CROSSWALK = "En un paso de peatones, cede el paso a quien cruza o va a cruzar."
const val RULE_STOP = "En un STOP hay que detenerse por completo antes de la línea, aunque no venga nadie."
const val RULE_STOP_YIELD = "Tras parar en el STOP, cede el paso a los vehículos que circulan por la vía."
const val RULE_RED_LIGHT = "Con el semáforo en rojo, detente antes de la línea de detención."
const val RULE_LEAD_CAR = "Guarda la distancia de seguridad: si el de delante frena, tienes que poder parar."
const val RULE_BALL = "Si un balón sale a la calzada, frena: detrás puede venir un niño."
