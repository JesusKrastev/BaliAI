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

        val fault = events.filterIsInstance<DriveEvent.Faulted>().single()
        assertThat(fault.kind).isEqualTo(SituationKind.STOP)
        assertThat(fault.message).isEqualTo(RULE_STOP)
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
        while (engine.phase != DrivePhase.FAULT) {
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

        assertThat(hints.map { it.kind }).containsExactlyElementsIn(engine.situations.map { it.kind }).inOrder()
    }

    @Test
    fun `rating is three stars when clean, two with one fault, one otherwise`() {
        val base = DriveSummary(1000, 9, 11, emptyList(), 10, 40, 50)
        assertThat(base.rating).isEqualTo(3)
        assertThat(base.copy(faults = listOf("a")).rating).isEqualTo(2)
        assertThat(base.copy(faults = listOf("a", "b")).rating).isEqualTo(1)
    }
}
