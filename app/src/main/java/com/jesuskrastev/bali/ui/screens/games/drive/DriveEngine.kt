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
private const val SLOW_MOTION_FACTOR = 0.55f
private const val MAGNET_RANGE = 3.5f

/** Centre x of the parking lane, where a car that pulls over ends up. */
const val PARKED_X = (LANES + PARKING_EDGE) / 2

/** How far ahead of the car the screen shows the road; anything further is off the top edge. */
const val VISIBLE_AHEAD = 8.7f

/** Speed limit of a zone 30, in world units per second (30 km/h). */
const val ZONE_30_SPEED = 30f / KMH_PER_UNIT

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
    SCOOTER("🛴", "Patinete: adelanta dejando 1,5 m"),
    AMBULANCE("🚑", "¡Sirena! Apártate de su carril"),
    ZONE_30("🐢", "Zona 30: frena antes de entrar"),
}

/**
 * Bubbles on the road that change the run for a while.
 *
 * @param seconds how long it lasts; 0 for the shield, which waits for the next fault
 */
enum class PowerUpKind(val label: String, val seconds: Float) {
    SHIELD("ESCUDO", 0f),
    MAGNET("IMÁN", 7f),
    DOUBLE("PUNTOS ×2", 8f),
    SLOW_MOTION("CÁMARA LENTA", 5f),
}

/** A power-up bubble waiting on the road. */
class PowerUp(val x: Float, val y: Float, val kind: PowerUpKind) {
    var collected = false
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

    /** Sideways speed in lanes per second (positive is to the right), so the car can be drawn turning. */
    var drift = 0f

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
class Star(var x: Float, var y: Float) {
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

    /** A power-up bubble was collected. */
    data class PowerUpCollected(val kind: PowerUpKind, val x: Float, val y: Float) : DriveEvent

    /** The shield took a fault: the streak survives. */
    data object ShieldSaved : DriveEvent

    /** An ambulance with its siren on appeared behind the player. */
    data object Siren : DriveEvent
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
 * @param faultKinds situations of [faults], in the same order
 */
data class DriveSummary(
    val score: Int,
    val resolved: Int,
    val situations: Int,
    val faults: List<String>,
    val starsCollected: Int,
    val starsTotal: Int,
    val durationSeconds: Int,
    val faultKinds: List<SituationKind> = emptyList(),
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
 * @param route fixed order of situations, or null for the usual shuffled route (tests use it to
 *   try every combination of neighbouring situations)
 */
class DriveEngine(
    seed: Long,
    private val bestScore: Int = 0,
    private val showHints: Boolean = false,
    private val route: List<SituationKind>? = null,
) {
    private val random = Random(seed)

    val car = PlayerCar()
    val situations: List<Situation> = buildRoute()
    val finishY: Float = situations.last().layoutEnd + 6f
    val powerUps: List<PowerUp> = buildPowerUps()
    val stars: List<Star> = buildStars().filter { star ->
        powerUps.none { abs(it.y - star.y) < 0.7f && abs(it.x - star.x) < 0.6f }
    }
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

    /** True when the shield absorbed the last fault, so the streak survived it. */
    var lastFaultShielded = false
        private set

    /** True while a shield is waiting to absorb the next fault. */
    var shield = false
        private set

    private var resolvedCount = 0
    private val faultMessages = mutableListOf<String>()
    private val faultKinds = mutableListOf<SituationKind>()
    private val powerUpTimers = mutableMapOf<PowerUpKind, Float>()
    private var pickupStreak = 0
    private var recordAnnounced = false
    private val pendingEvents = mutableListOf<DriveEvent>()

    /** Score multiplier earned by the current streak of handled situations. */
    val multiplier: Int get() = combo.coerceIn(1, MAX_MULTIPLIER)

    /** Share of the route already driven, 0–1. */
    val progress: Float get() = (car.y / finishY).coerceIn(0f, 1f)

    /** Speed shown on the speedometer. */
    val speedKmh: Int get() = (car.speed * KMH_PER_UNIT).toInt()

    /** Speed limit in force around the car, in km/h, or null outside a zone 30. */
    val speedLimitKmh: Int?
        get() = situations.filterIsInstance<Zone30Situation>()
            .firstOrNull { car.y > it.y - 2f && car.rear < it.layoutEnd }?.let { 30 }

    /**
     * Seconds left of a timed power-up.
     *
     * @return 0 when [kind] is not active
     */
    fun remaining(kind: PowerUpKind): Float = powerUpTimers[kind] ?: 0f

    /** True while a timed power-up [kind] is running. */
    fun isActive(kind: PowerUpKind): Boolean = remaining(kind) > 0f

    /** Points multiplier of the "×2" power-up. */
    private val pointsFactor: Int get() = if (isActive(PowerUpKind.DOUBLE)) 2 else 1

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
            DrivePhase.DRIVING -> {
                tickPowerUps(dt)
                drive(if (isActive(PowerUpKind.SLOW_MOTION)) dt * SLOW_MOTION_FACTOR else dt, steer, brake)
            }
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
        faultKinds = faultKinds.toList(),
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
            // The siren is announced on every run instead: the ambulance comes from behind.
            if (showHints && !situation.hinted && distance < HINT_DISTANCE && situation !is AmbulanceSituation) {
                situation.hinted = true
                pendingEvents += DriveEvent.Hint(situation.kind)
            }
            val checkRules = situation.status == SituationStatus.UPCOMING
            val fault = situation.update(dt, car, checkRules)
            if (situation is AmbulanceSituation && situation.ambulance != null && !situation.hinted) {
                situation.hinted = true
                pendingEvents += DriveEvent.Siren
            }
            if (checkRules && fault != null) {
                fault(situation, fault)
                return
            }
            if (checkRules && situation.isCleared(car)) resolve(situation)
        }

        if (checkCrashes()) return
        collectStars(dt)
        collectPowerUps()
        if (!recordAnnounced && bestScore > 0 && score > bestScore) {
            recordAnnounced = true
            pendingEvents += DriveEvent.NewRecord
        }
        if (car.rear > finishY) finish()
    }

    /** Steers towards the dragged position and accelerates towards cruise speed or brakes. */
    private fun moveCar(dt: Float, steer: Float, brake: Boolean) {
        car.cruise = CRUISE_START + (CRUISE_END - CRUISE_START) * progress
        // Inside a zone 30 the car holds the limit by itself: the test is slowing down before it.
        val inZone30 = situations.any { it is Zone30Situation && car.y > it.y && car.rear < it.layoutEnd }
        if (inZone30) car.cruise = min(car.cruise, ZONE_30_SPEED * 0.97f)
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
        val points = (SITUATION_POINTS + if (bonus != null) BONUS_POINTS else 0) * multiplier * pointsFactor
        score += points
        pendingEvents += DriveEvent.Resolved(situation.kind, points, multiplier, praise(), bonus)
    }

    /** Freezes the run on a broken rule; the streak is lost but the run goes on. */
    private fun fault(situation: Situation, message: String) {
        situation.status = SituationStatus.FAILED
        situation.clearAfterFault(car)
        freeze(situation.kind, message)
    }

    /** Stops the car, spends the shield or the streak, records the rule and freezes the run. */
    private fun freeze(kind: SituationKind, message: String) {
        car.speed = 0f
        car.braking = false
        lastFaultShielded = shield
        if (shield) {
            shield = false
            pendingEvents += DriveEvent.ShieldSaved
        } else {
            combo = 0
        }
        faultMessages += message
        faultKinds += kind
        phase = DrivePhase.FAULT
        faultTime = 0f
        val event = DriveEvent.Faulted(kind, message)
        lastFault = event
        pendingEvents += event
    }

    /**
     * Keeps every actor solid after its situation is settled: a car already overtaken, a scooter
     * left behind, a crossing car... Hitting one is a crash fault, and the car is moved out of it
     * so it can never drive through.
     *
     * @return true when a crash froze the run this frame
     */
    private fun checkCrashes(): Boolean {
        val box = car.box()
        for (situation in situations) {
            if (situation.status == SituationStatus.UPCOMING) continue
            if (situation.layoutStart - car.y > ACTIVE_AHEAD || car.rear - situation.layoutEnd > ACTIVE_BEHIND + 6f) continue
            if (situation.crashed) continue
            val hit = situation.obstacles().firstOrNull { it.overlaps(box) } ?: continue
            situation.crashed = true
            separate(hit)
            freeze(situation.kind, RULE_CRASH)
            return true
        }
        return false
    }

    /** Moves the car just out of [obstacle]: behind it when it is ahead, beside it otherwise. */
    private fun separate(obstacle: Box) {
        val obstacleCenterY = (obstacle.bottom + obstacle.top) / 2
        val carCenterY = car.y - CAR_LENGTH / 2
        if (obstacleCenterY >= carCenterY && car.y - obstacle.bottom < 0.5f) {
            car.y = obstacle.bottom - 0.05f
        } else {
            val pushRight = car.x >= (obstacle.left + obstacle.right) / 2
            val minX = CAR_WIDTH / 2 + 0.04f
            val maxX = LANES - CAR_WIDTH / 2 - 0.04f
            car.x = (if (pushRight) obstacle.right + CAR_WIDTH / 2 + 0.05f else obstacle.left - CAR_WIDTH / 2 - 0.05f).coerceIn(minX, maxX)
            car.targetX = car.x
            if (car.box().overlaps(obstacle)) car.y = obstacle.bottom - 0.05f
        }
    }

    /** Leaves the fault freeze and drives on from a standstill. */
    private fun resume() {
        phase = DrivePhase.DRIVING
        faultTime = 0f
    }

    /** Counts down the timed power-ups in real time (slow motion does not stretch itself). */
    private fun tickPowerUps(dt: Float) {
        powerUpTimers.keys.toList().forEach { kind ->
            val left = (powerUpTimers[kind] ?: 0f) - dt
            if (left <= 0f) powerUpTimers.remove(kind) else powerUpTimers[kind] = left
        }
    }

    /** Picks up the power-up bubbles under the car. */
    private fun collectPowerUps() {
        val box = car.box()
        for (powerUp in powerUps) {
            if (powerUp.collected || powerUp.y > car.y + 0.5f || powerUp.y < car.rear - 0.5f) continue
            if (box.overlaps(Box(powerUp.x - 0.22f, powerUp.y - 0.22f, powerUp.x + 0.22f, powerUp.y + 0.22f))) {
                powerUp.collected = true
                if (powerUp.kind == PowerUpKind.SHIELD) shield = true else powerUpTimers[powerUp.kind] = powerUp.kind.seconds
                pendingEvents += DriveEvent.PowerUpCollected(powerUp.kind, powerUp.x, powerUp.y)
            }
        }
    }

    /**
     * Picks up the stars under the car and breaks the streak on any star left behind. With the
     * magnet on, the stars ahead fly towards the car first.
     */
    private fun collectStars(dt: Float) {
        val box = car.box()
        val magnet = isActive(PowerUpKind.MAGNET)
        val reachAhead = if (magnet) MAGNET_RANGE else 0.3f
        for (star in stars) {
            if (star.collected || star.missed) continue
            if (star.y > car.y + reachAhead) break
            if (magnet && star.y > car.rear - 0.2f) {
                val pull = min(1f, dt * 7f)
                star.x += (car.x - star.x) * pull
                star.y += (car.y - CAR_LENGTH / 2 - star.y) * pull
            }
            val reach = Box(star.x - 0.17f, star.y - 0.17f, star.x + 0.17f, star.y + 0.17f)
            if (box.overlaps(reach)) {
                star.collected = true
                pickupStreak++
                val points = STAR_POINTS * multiplier * pointsFactor
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
        powerUpTimers.clear()
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
     * Picks the order of a run: the first two situations teach the two controls (roadworks: steer;
     * crosswalk: brake), then one of each kind in a shuffle with no kind twice in a row.
     *
     * @return the kinds of the route, in driving order
     */
    private fun shuffledKinds(): List<SituationKind> {
        // One of each after the opening: about a minute of driving.
        val pool = listOf(
            SituationKind.STOP, SituationKind.TRAFFIC_LIGHT, SituationKind.LEAD_CAR,
            SituationKind.BALL, SituationKind.ROADWORKS, SituationKind.CROSSWALK,
            SituationKind.SCOOTER, SituationKind.AMBULANCE, SituationKind.ZONE_30,
        )
        val opening = listOf(SituationKind.ROADWORKS, SituationKind.CROSSWALK)
        var kinds = opening + pool.shuffled(random)
        var attempts = 0
        while (kinds.zipWithNext().any { (a, b) -> a == b } && attempts++ < 100) {
            kinds = opening + pool.shuffled(random)
        }
        return kinds
    }

    /**
     * Lays out the route with the gaps between situations and builds each one.
     *
     * @return the situations, in driving order
     */
    private fun buildRoute(): List<Situation> {
        val kinds = route ?: shuffledKinds()
        var free = 10f
        var lightCount = 0
        return kinds.map { kind ->
            // Situations whose actor appears ahead of their y (the slow car, the scooter) need that
            // stretch free too, or it would show up while the previous one is still on screen.
            val y = free + when (kind) {
                SituationKind.LEAD_CAR -> 6.6f
                SituationKind.SCOOTER -> 8.8f
                else -> 0f
            }
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
                SituationKind.SCOOTER -> ScooterSituation(y, bike = random.nextBoolean(), style = random.nextInt(4))
                SituationKind.AMBULANCE -> AmbulanceSituation(y)
                SituationKind.ZONE_30 -> Zone30Situation(y)
            }
            free = situation.layoutEnd + 6.2f + random.nextFloat() * 1.4f
            situation
        }
    }

    /**
     * Places power-up bubbles: one on the first stretch, so every run starts with a treat, then
     * one after every third situation, cycling through the kinds in a shuffled order.
     */
    private fun buildPowerUps(): List<PowerUp> {
        val kinds = PowerUpKind.entries.shuffled(random)
        val result = mutableListOf(PowerUp(laneCenter(1), 5f, kinds[0]))
        situations.forEachIndexed { index, situation ->
            if (index % 3 == 1) {
                result += PowerUp(laneCenter(random.nextInt(LANES)), situation.layoutEnd + 2.4f, kinds[result.size % kinds.size])
            }
        }
        return result
    }

    /** Places star patterns in the free stretches between situations. */
    private fun buildStars(): List<Star> {
        val result = mutableListOf<Star>()
        var from = 2.5f
        for (situation in situations + null) {
            val to = (situation?.layoutStart ?: finishY) - 1.4f
            var y = from
            while (to - y > 2.3f) {
                // The longest pattern that fits what is left of the gap.
                val room = to - y
                val pattern = random.nextInt(if (room > 4.2f) 3 else if (room > 3.1f) 2 else 1)
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
            from = (situation?.layoutEnd ?: finishY) + 1f
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

    /** Set after the car crashed into one of its actors, which then stop being solid. */
    var crashed = false

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

    /** Solid things the car must not drive through once the situation is settled. */
    open fun obstacles(): List<Box> = emptyList()
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

    override fun obstacles(): List<Box> = barriers()

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

    override fun obstacles(): List<Box> = if (walkerOnRoad) listOf(walker.box()) else emptyList()

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

    override fun obstacles(): List<Box> = listOfNotNull(crossCar?.box())

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

    override fun obstacles(): List<Box> = listOfNotNull(crossCar?.box())

    override fun clearAfterFault(car: PlayerCar) {
        light = LightColor.GREEN
        crossCar?.x = -8f
    }
}

/**
 * A slower car ahead that brakes hard: keep enough distance to stop behind it (or change lane).
 * Once it has been stopped a moment it pulls over to the right and parks like any other car, so it
 * never drives on into the next situation.
 */
class LeadCarSituation(y: Float, private val color: Int) : Situation(SituationKind.LEAD_CAR, y) {
    var lead: Vehicle? = null
        private set
    private var stage = 0 // 0 waiting, 1 cruising, 2 braking, 3 holding, 4 pulling over, 5 parked
    private var timer = 0f
    private var minGap = Float.MAX_VALUE
    private var overtook = false
    private var done = false

    override val layoutStart: Float get() = y - 6.6f
    override val layoutEnd: Float get() = y + 10f

    // Where it parks, the bays are left empty.
    override val parkingClearStart: Float get() = y + 4.8f
    override val parkingClearEnd: Float get() = layoutEnd

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
            4 -> pullOver(lead, dt)
        }
        lead.y += lead.speed * dt
        if (!checkRules) return null
        val sameLane = abs(lead.x - car.x) < CAR_WIDTH
        if (sameLane && gap > -CAR_LENGTH) minGap = min(minGap, gap)
        if (car.rear > lead.y + CAR_LENGTH / 2) overtook = true
        return if (lead.box().overlaps(car.box())) RULE_LEAD_CAR else null
    }

    /**
     * Creeps forward and sideways into the parking lane; once there the car is parked for good.
     *
     * @param lead the car in front
     * @param dt seconds since the last frame
     */
    private fun pullOver(lead: Vehicle, dt: Float) {
        lead.speed = if (lead.y < y + 8.2f) PULL_OVER_CREEP else 0f
        lead.drift = PULL_OVER_DRIFT
        lead.x = min(PARKED_X, lead.x + PULL_OVER_DRIFT * dt)
        if (lead.x >= PARKED_X) {
            stage = 5
            lead.speed = 0f
            lead.drift = 0f
            done = true
        }
    }

    override fun isCleared(car: PlayerCar): Boolean = done || overtook

    override fun bonus(): String? = if (!overtook && minGap >= 0.7f) "¡Buena distancia!" else null

    override fun obstacles(): List<Box> = listOfNotNull(lead?.box())

    override fun clearAfterFault(car: PlayerCar) {
        val lead = lead ?: return
        lead.y = max(lead.y, car.y + 1.6f + CAR_LENGTH / 2)
        lead.braking = false
        stage = 4
        done = true
    }

    private companion object {
        /** Forward speed while pulling over, in world units per second. */
        const val PULL_OVER_CREEP = 0.9f

        /** Sideways speed while pulling over, in lanes per second. */
        const val PULL_OVER_DRIFT = 1.6f
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

    override fun obstacles(): List<Box> = if (child.visible && child.x > -0.05f) listOf(child.box()) else emptyList()

    override fun clearAfterFault(car: PlayerCar) {
        child.x = -0.4f
        child.vx = 0f
        child.alert = false
        judged = true
        ballVisible = false
    }
}

/**
 * A scooter (or bike) riding slowly in the player's lane: overtake it by changing lane, leaving
 * at least 1.5 m (about 0.4 lanes) of space, or hang back behind it and it turns up onto the
 * pavement by itself.
 */
class ScooterSituation(y: Float, val bike: Boolean, val style: Int) : Situation(SituationKind.SCOOTER, y) {
    var riderX = 0f
        private set
    var riderY = y
        private set
    var visible = false
        private set
    var wobble = 0f
        private set
    private var spawned = false
    private var timer = 0f
    private var riderSpeed = 1.5f
    private var gone = false
    private var overtaken = false
    private var early: Boolean? = null
    private var followedFor = 0f
    private var turningOff = false

    override val layoutStart: Float get() = y - 8.8f
    override val layoutEnd: Float get() = y + 10f

    // The rider may cross the parking lane on its way to the pavement.
    override val parkingClearStart: Float get() = y - 1f
    override val parkingClearEnd: Float get() = layoutEnd

    /** The rider and the 1.5 m around them that a passing car must keep clear. */
    fun safetyBox(): Box = Box(riderX - 0.11f - 0.4f, riderY - 0.25f - 0.35f, riderX + 0.11f + 0.4f, riderY + 0.3f)

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        if (!spawned) {
            if (car.y < y - 8.8f) return null
            spawned = true
            visible = true
            riderX = laneCenter(laneOf(car.targetX)) + 0.15f
            riderY = car.y + 8.8f
        }
        if (gone) return null
        timer += dt
        wobble += dt * 5f
        riderY += riderSpeed * dt
        followBehaviour(dt, car)
        // Leaves over the top edge if not overtaken, off the bottom edge once left behind, or off
        // the road to the right: it never rides into the next situation.
        if ((riderY - car.y > 9.3f && timer > 2f) || riderY < car.y - 2.4f || riderX > OFF_ROAD_X || riderY > layoutEnd) {
            gone = true
            visible = false
        }
        if (!checkRules) return null
        if (early == null && riderY - car.y < 2.5f) {
            val box = safetyBox()
            early = box.left >= car.x + CAR_WIDTH / 2 || box.right <= car.x - CAR_WIDTH / 2
        }
        if (car.rear > riderY + 0.3f) overtaken = true
        return if (safetyBox().overlaps(car.box())) RULE_SCOOTER else null
    }

    /**
     * Turns the rider off to the right once a car has been hanging back right behind it for a
     * moment, instead of letting it ride on at walking pace for ever.
     *
     * @param dt seconds since the last frame
     * @param car the player's car
     */
    private fun followBehaviour(dt: Float, car: PlayerCar) {
        val directlyBehind = abs(car.x - riderX) < 0.7f && riderY - 0.6f - car.y > 0.2f
        followedFor = if (directlyBehind && car.speed < riderSpeed + 0.7f) followedFor + dt else 0f
        if (followedFor > TURN_OFF_AFTER) turningOff = true
        if (turningOff) riderX += TURN_OFF_RATE * dt
    }

    override fun isCleared(car: PlayerCar): Boolean = gone || overtaken

    override fun bonus(): String? = if (overtaken && early == true) "¡Adelantamiento de libro!" else null

    override fun obstacles(): List<Box> =
        if (visible) listOf(Box(riderX - 0.11f, riderY - 0.25f, riderX + 0.11f, riderY + 0.25f)) else emptyList()

    override fun clearAfterFault(car: PlayerCar) {
        overtaken = true
        // The car stopped dead; the rider speeds off ahead, faster than the car, and turns off.
        riderY = max(riderY, car.y + 1.2f)
        riderSpeed = 5f
        turningOff = true
    }

    private companion object {
        /** Seconds a car must hang back behind the rider before it turns off. */
        const val TURN_OFF_AFTER = 0.8f

        /** Sideways speed while turning off, in lanes per second. */
        const val TURN_OFF_RATE = 2.2f

        /** Beyond this x the rider is off the screen, up on the pavement, and is removed. */
        const val OFF_ROAD_X = PARKING_EDGE + 2.4f
    }
}

/**
 * An ambulance with its siren on, closing in from behind in the player's lane: move out of it. It
 * is quick and, once by, floors it, so it is over the horizon before the next situation.
 */
class AmbulanceSituation(y: Float) : Situation(SituationKind.AMBULANCE, y) {
    var ambulance: Vehicle? = null
        private set
    private var lane = 1
    private var spawned = false
    private var timer = 0f
    private var movedAt = -1f
    private var passed = false

    override val layoutEnd: Float get() = y + ZONE_LENGTH

    // It drives along the traffic lanes only.
    override val parkingClearStart: Float get() = Float.MAX_VALUE
    override val parkingClearEnd: Float get() = -Float.MAX_VALUE

    /** True once the ambulance is ahead of the car. */
    val hasPassed: Boolean get() = passed

    /** Lane the ambulance drives along. */
    val ambulanceLane: Int get() = lane

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        if (!spawned) {
            if (car.y < y) return null
            spawned = true
            lane = laneOf(car.targetX)
            // Spawned off the bottom edge, so it is only ever seen driving in.
            ambulance = Vehicle(laneCenter(lane), car.y - SPAWN_BEHIND, max(car.speed, 2f) + CLOSING_SPEED, color = -1)
        }
        val ambulance = ambulance ?: return null
        timer += dt
        val target = car.speed + if (passed) FLEEING_SPEED else CLOSING_SPEED
        ambulance.speed += (target - ambulance.speed).coerceIn(-ACCELERATION_LIMIT * dt, ACCELERATION_LIMIT * dt)
        ambulance.y += ambulance.speed * dt
        // Gone over the top edge (or the end of its stretch of road) before it reaches what comes next.
        val rear = ambulance.y - CAR_LENGTH / 2
        if (rear - car.y > VISIBLE_AHEAD + 0.2f || rear > layoutEnd) this.ambulance = null
        if (!checkRules) return null
        val inLane = car.x + CAR_WIDTH / 2 > lane + 0.05f && car.x - CAR_WIDTH / 2 < lane + 0.95f
        if (!inLane && movedAt < 0f) movedAt = timer
        if (ambulance.y - CAR_LENGTH / 2 > car.y + 0.4f) passed = true
        // Long box: the ambulance also must not be held up right behind the car.
        val reach = Box(ambulance.x - CAR_WIDTH / 2, ambulance.y - 0.6f, ambulance.x + CAR_WIDTH / 2, ambulance.y + 0.6f + 0.5f)
        return if (!passed && reach.overlaps(car.box())) RULE_AMBULANCE else null
    }

    override fun isCleared(car: PlayerCar): Boolean = passed

    override fun bonus(): String? = if (movedAt in 0f..1.4f) "¡Paso abierto!" else null

    override fun obstacles(): List<Box> = listOfNotNull(ambulance?.box())

    override fun clearAfterFault(car: PlayerCar) {
        ambulance?.let { it.y = car.y + 2f }
        passed = true
    }

    private companion object {
        /** Long enough for the ambulance to leave through the top edge before the zone ends. */
        const val ZONE_LENGTH = 21f

        /** How far behind the car it spawns: off the bottom edge, so it is only seen driving in. */
        const val SPAWN_BEHIND = 7.6f

        /** Speed over the car's while closing in from behind. */
        const val CLOSING_SPEED = 4.5f

        /** Speed over the car's once it is by. */
        const val FLEEING_SPEED = 7.5f

        /** Most its speed may change per second, so the change of pace looks like driving. */
        const val ACCELERATION_LIMIT = 12f
    }
}

/** A zone 30: be at 30 km/h or less when entering; inside, the car keeps to the limit. */
class Zone30Situation(y: Float) : Situation(SituationKind.ZONE_30, y) {
    val length = 5f
    private var entrySpeed = -1f

    override val layoutEnd: Float get() = y + length

    override fun update(dt: Float, car: PlayerCar, checkRules: Boolean): String? {
        if (!checkRules || entrySpeed >= 0f || car.y < y) return null
        entrySpeed = car.speed
        return if (car.speed > ZONE_30_SPEED * 1.08f) RULE_ZONE_30 else null
    }

    override fun bonus(): String? = if (entrySpeed in ZONE_30_SPEED * 0.75f..ZONE_30_SPEED * 1.08f) "¡Velocidad perfecta!" else null

    override fun clearAfterFault(car: PlayerCar) = Unit
}

const val RULE_ROADWORKS = "Si las obras cortan tu carril, cámbiate a uno libre con antelación."
const val RULE_CROSSWALK = "En un paso de peatones, cede el paso a quien cruza o va a cruzar."
const val RULE_STOP = "En un STOP hay que detenerse por completo antes de la línea, aunque no venga nadie."
const val RULE_STOP_YIELD = "Tras parar en el STOP, cede el paso a los vehículos que circulan por la vía."
const val RULE_RED_LIGHT = "Con el semáforo en rojo, detente antes de la línea de detención."
const val RULE_LEAD_CAR = "Guarda la distancia de seguridad: si el de delante frena, tienes que poder parar."
const val RULE_BALL = "Si un balón sale a la calzada, frena: detrás puede venir un niño."
const val RULE_CRASH = "¡Choque! Antes de cambiar de carril o acercarte, comprueba que hay espacio."
const val RULE_SCOOTER = "Para adelantar a un patinete o una bici, deja al menos 1,5 m: cámbiate de carril."
const val RULE_AMBULANCE = "Si oyes la sirena de una ambulancia, apártate de su carril para dejarle paso."
const val RULE_ZONE_30 = "En una zona 30 hay que entrar ya a 30 km/h o menos: frena antes de la señal."
