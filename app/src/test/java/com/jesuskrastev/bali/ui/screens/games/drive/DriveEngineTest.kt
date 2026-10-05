package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DriveEngineTest {

    @Test
    fun `countdown counts 2, 1 and go before the car moves`() {
        val engine = DriveEngine(seed = 1)

        val events = mutableListOf<DriveEvent>()
        repeat((COUNTDOWN_SECONDS / TEST_FRAME).toInt() + 2) {
            engine.update(TEST_FRAME, 0f, false)
            events += engine.drainEvents()
        }

        assertThat(events.filterIsInstance<DriveEvent.Countdown>().map { it.number }).containsExactly(2, 1, 0).inOrder()
        assertThat(engine.phase).isEqualTo(DrivePhase.DRIVING)
    }

    @Test
    fun `every route opens with roadworks to teach steering and a crosswalk to teach braking`() {
        (1L..20L).forEach { seed ->
            val kinds = DriveEngine(seed).situations.map { it.kind }
            assertThat(kinds.take(2)).containsExactly(SituationKind.ROADWORKS, SituationKind.CROSSWALK).inOrder()
            assertThat(kinds.zipWithNext().none { (a, b) -> a == b }).isTrue()
            assertThat(kinds.toSet()).containsExactlyElementsIn(SituationKind.entries)
        }
    }

    @Test
    fun `a careful driver finishes every route without a single fault`() {
        (1L..30L).forEach { seed ->
            val engine = DriveEngine(seed)

            val events = engine.drive(driver = ::examinerInput)

            val faults = events.filterIsInstance<DriveEvent.Faulted>()
            assertThat(faults.map { "seed $seed: ${it.kind} ${it.message}" }).isEmpty()
            assertThat(engine.phase).isEqualTo(DrivePhase.FINISHED)
            val summary = engine.summary()
            assertThat(summary.resolved).isEqualTo(summary.situations)
            assertThat(summary.rating).isEqualTo(3)
            assertThat(events.last()).isEqualTo(DriveEvent.Finished(perfect = true))
        }
    }

    @Test
    fun `a run lasts about a minute`() {
        (1L..10L).forEach { seed ->
            val engine = DriveEngine(seed)
            engine.drive(driver = ::examinerInput)
            assertThat(engine.drivingTime).isIn(Range.closed(35f, 75f))
        }
    }

    @Test
    fun `a driver who never steers nor brakes breaks rules but still reaches the finish`() {
        val engine = DriveEngine(seed = 3)

        val events = engine.drive { 0f to false }

        val faults = events.filterIsInstance<DriveEvent.Faulted>()
        assertThat(faults.first().message).isEqualTo(RULE_ROADWORKS)
        assertThat(faults.map { it.kind }).contains(SituationKind.CROSSWALK)
        assertThat(faults.map { it.kind }).contains(SituationKind.STOP)
        assertThat(engine.phase).isEqualTo(DrivePhase.FINISHED)
        assertThat(engine.summary().rating).isEqualTo(1)
    }

    @Test
    fun `running a STOP without stopping is a fault even with nobody coming`() {
        val engine = DriveEngine(seed = 5)
        val stop = engine.situations.filterIsInstance<StopSituation>().first()

        // Careful everywhere except at this STOP line.
        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            steer to (brake && !(e.nextSituation() === stop && !stop.stopped))
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).containsExactly(RULE_STOP)
    }

    @Test
    fun `stopping at the STOP and pulling away at once hits the crossing car`() {
        val engine = DriveEngine(seed = 5)
        val stop = engine.situations.filterIsInstance<StopSituation>().first()

        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            // Waits for nobody once stopped.
            steer to (brake && !(e.nextSituation() === stop && stop.stopped))
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).containsExactly(RULE_STOP_YIELD)
    }

    @Test
    fun `a fault resets the streak, freezes the run and resumes by itself`() {
        val engine = DriveEngine(seed = 2)
        while (engine.phase != DrivePhase.FAULT && engine.drivingTime < 120f) {
            engine.update(TEST_FRAME, 0f, false)
            engine.drainEvents()
        }
        assertThat(engine.combo).isEqualTo(0)
        assertThat(engine.car.speed).isEqualTo(0f)

        engine.resumeFromFault()
        assertThat(engine.phase).isEqualTo(DrivePhase.FAULT) // too early: the rule must be read first

        repeat((FAULT_PAUSE_SECONDS / TEST_FRAME).toInt() + 1) { engine.update(TEST_FRAME, 0f, false) }
        assertThat(engine.phase).isEqualTo(DrivePhase.DRIVING)
    }

    @Test
    fun `consecutive situations raise the multiplier`() {
        val engine = DriveEngine(seed = 8)

        val resolved = engine.drive(driver = ::examinerInput).filterIsInstance<DriveEvent.Resolved>()

        assertThat(resolved.map { it.multiplier }.take(6)).containsExactly(1, 2, 3, 4, 5, 5).inOrder()
        assertThat(resolved.first().points).isAtLeast(100)
    }

    @Test
    fun `collecting stars in a row raises the streak`() {
        val engine = DriveEngine(seed = 4)
        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            val star = e.stars.firstOrNull { !it.collected && !it.missed && it.y > e.car.y - 0.3f }
            val avoidingWorks = e.nextSituation() is RoadworksSituation && e.nextSituation()!!.y - e.car.y < 8f
            val steering = if (star != null && star.y - e.car.y < 2f && !avoidingWorks) star.x - e.car.targetX else steer
            steering to brake
        }

        val pickups = events.filterIsInstance<DriveEvent.Pickup>()
        assertThat(pickups.size).isAtLeast(10)
        assertThat(pickups.maxOf { it.streak }).isAtLeast(3)
    }

    @Test
    fun `beating the personal best is announced once`() {
        val engine = DriveEngine(seed = 6, bestScore = 300)

        val events = engine.drive(driver = ::examinerInput)

        assertThat(events.count { it == DriveEvent.NewRecord }).isEqualTo(1)
    }

    @Test
    fun `first-run hints announce each situation once`() {
        val engine = DriveEngine(seed = 6, showHints = true)

        val hints = engine.drive(driver = ::examinerInput).filterIsInstance<DriveEvent.Hint>()

        // The ambulance is announced by its siren on every run instead.
        val announced = engine.situations.map { it.kind }.filter { it != SituationKind.AMBULANCE }
        assertThat(hints.map { it.kind }).containsExactlyElementsIn(announced).inOrder()
    }

    @Test
    fun `every run starts with a power-up in the starting lane and has a few more`() {
        (1L..10L).forEach { seed ->
            val engine = DriveEngine(seed)
            assertThat(engine.powerUps.first().y).isLessThan(engine.situations.first().y)
            assertThat(engine.powerUps.first().x).isEqualTo(laneCenter(1))
            assertThat(engine.powerUps.size).isAtLeast(4)
        }
    }

    @Test
    fun `power-ups are collected and their timers run out`() {
        val engine = DriveEngine(seed = 3)
        val events = mutableListOf<DriveEvent>()
        while (events.none { it is DriveEvent.PowerUpCollected } && engine.drivingTime < 120f) {
            val (steer, brake) = examinerInput(engine)
            engine.update(TEST_FRAME, steer, brake)
            events += engine.drainEvents()
        }
        val kind = events.filterIsInstance<DriveEvent.PowerUpCollected>().single().kind
        if (kind == PowerUpKind.SHIELD) {
            assertThat(engine.shield).isTrue()
        } else {
            assertThat(engine.isActive(kind)).isTrue()
            repeat(((kind.seconds + 0.5f) / TEST_FRAME).toInt()) { engine.update(TEST_FRAME, 0f, false) }
            assertThat(engine.isActive(kind)).isFalse()
        }
    }

    @Test
    fun `the shield keeps the streak through one fault`() {
        val engine = DriveEngine(seed = 3)
        // Drive carefully until the shield is picked up somewhere on the route.
        val shieldAt = engine.powerUps.first { it.kind == PowerUpKind.SHIELD }
        engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            val goForIt = !shieldAt.collected && shieldAt.y - e.car.y in 0f..3f
            (if (goForIt) shieldAt.x - e.car.targetX else steer) to brake
        }.let { assertThat(shieldAt.collected).isTrue() }

        val fresh = DriveEngine(seed = 3)
        var comboBefore = 0
        val events = fresh.drive { e ->
            val (steer, brake) = examinerInput(e)
            val goForIt = !fresh.powerUps.first { it.kind == PowerUpKind.SHIELD }.collected &&
                fresh.powerUps.first { it.kind == PowerUpKind.SHIELD }.y - e.car.y in 0f..3f
            if (e.shield && e.combo > 0) comboBefore = e.combo
            // Once shielded with a streak, ignore the next red light / STOP / pedestrian.
            val reckless = e.shield && e.combo > 0
            (if (goForIt) fresh.powerUps.first { it.kind == PowerUpKind.SHIELD }.x - e.car.targetX else steer) to (brake && !reckless)
        }
        val saved = events.indexOfFirst { it == DriveEvent.ShieldSaved }
        assertThat(saved).isAtLeast(0)
        assertThat(fresh.lastFault).isNotNull()
        assertThat(comboBefore).isGreaterThan(0)
    }

    @Test
    fun `entering a zone 30 too fast is a fault`() {
        val engine = DriveEngine(seed = 5)
        val zone = engine.situations.filterIsInstance<Zone30Situation>().single()

        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            steer to (brake && e.nextSituation() !== zone)
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).containsExactly(RULE_ZONE_30)
    }

    @Test
    fun `staying in the lane of an ambulance is a fault and the siren is always announced`() {
        val engine = DriveEngine(seed = 5)
        val ambulance = engine.situations.filterIsInstance<AmbulanceSituation>().single()

        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            (if (e.nextSituation() === ambulance) 0f else steer) to brake
        }

        assertThat(events.count { it == DriveEvent.Siren }).isEqualTo(1)
        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).containsExactly(RULE_AMBULANCE)
    }

    @Test
    fun `squeezing past a scooter in its lane is a fault`() {
        val engine = DriveEngine(seed = 5)
        val scooter = engine.situations.filterIsInstance<ScooterSituation>().first()

        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            // Neither changes lane nor slows down behind it.
            if (scooter.visible && scooter.status == SituationStatus.UPCOMING) 0f to false else steer to brake
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).contains(RULE_SCOOTER)
    }

    @Test
    fun `an overtaken car stays solid so swerving back into it is a crash and never a drive-through`() {
        val engine = DriveEngine(seed = 5)
        val lead = engine.situations.filterIsInstance<LeadCarSituation>().first()

        val events = engine.drive { e ->
            val (steer, brake) = examinerInput(e)
            val car = lead.lead
            when {
                // Overtake on the left...
                e.nextSituation() === lead && car != null -> (laneCenter(if (laneOf(car.x) == 0) 1 else 0) - e.car.targetX) to false
                // ...then cut straight back into its lane while still alongside it.
                lead.status == SituationStatus.PASSED && car != null && car.y - e.car.y in -1.5f..0.5f ->
                    (car.x - e.car.targetX) to false
                else -> steer to brake
            }
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).contains(RULE_CRASH)
        val after = lead.lead
        if (after != null) assertThat(after.box().overlaps(engine.car.box())).isFalse()
    }

    @Test
    fun `rating is three stars when clean, two with one fault, one otherwise`() {
        val base = DriveSummary(1000, 9, 11, emptyList(), 10, 40, 50)
        assertThat(base.rating).isEqualTo(3)
        assertThat(base.copy(faults = listOf("a")).rating).isEqualTo(2)
        assertThat(base.copy(faults = listOf("a", "b")).rating).isEqualTo(1)
    }
}
