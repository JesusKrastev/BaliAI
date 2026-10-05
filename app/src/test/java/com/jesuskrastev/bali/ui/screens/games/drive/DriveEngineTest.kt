package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
    fun `every kind of power-up can be collected on the way and the timed ones run out`() {
        PowerUpKind.entries.forEach { kind ->
            val engine = DriveEngine(seed = 3)
            val target = engine.powerUps.first { it.kind == kind }

            val events = engine.drive(until = { target.collected }) { examinerFetching(it, target) }

            assertThat(events.filterIsInstance<DriveEvent.PowerUpCollected>().map { it.kind }).contains(kind)
            if (kind == PowerUpKind.SHIELD) {
                assertThat(engine.shield).isTrue()
            } else {
                assertThat(engine.isActive(kind)).isTrue()
                engine.drive(maxSeconds = kind.seconds + 4 * FAULT_PAUSE_SECONDS, until = { !it.isActive(kind) }, driver = ::examinerInput)
                assertThat(engine.isActive(kind)).isFalse()
            }
        }
    }

    @Test
    fun `the shield keeps the streak through one fault and only one`() {
        // A seed whose shield is not the opening treat, so there is a streak to protect when it is picked up.
        val seed = (1L..50L).first { s -> DriveEngine(s).powerUps.indexOfFirst { it.kind == PowerUpKind.SHIELD } > 0 }
        val engine = DriveEngine(seed)
        val shield = engine.powerUps.first { it.kind == PowerUpKind.SHIELD }
        engine.drive(until = { it.shield }) { examinerFetching(it, shield) }
        assertThat(engine.shield).isTrue()
        assertThat(engine.combo).isGreaterThan(0)

        // Now drive carelessly: no steering, no braking, until the first rule is broken.
        var streakBeforeFault = 0
        val events = engine.drive(until = { it.lastFault != null }) { e ->
            streakBeforeFault = e.combo
            0f to false
        }

        assertThat(events).contains(DriveEvent.ShieldSaved)
        assertThat(engine.lastFaultShielded).isTrue()
        assertThat(engine.shield).isFalse()
        assertThat(engine.combo).isAtLeast(streakBeforeFault)

        // The shield is spent: the next fault breaks the streak.
        val firstFault = engine.lastFault
        engine.drive(until = { it.lastFault !== firstFault }) { 0f to false }

        assertThat(engine.lastFault).isNotSameInstanceAs(firstFault)
        assertThat(engine.lastFaultShielded).isFalse()
        assertThat(engine.combo).isEqualTo(0)
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
    fun `a barrier that already cost a fault stays solid, so steering back into it is a crash and never a drive-through`() {
        val engine = DriveEngine(seed = 5, route = listOf(SituationKind.ROADWORKS))
        val works = engine.situations.single() as RoadworksSituation
        var faults = 0

        val events = engine.drive { e ->
            // Drives straight into the works (a fault), then, once moved out of the way, straight back in.
            if (e.phase == DrivePhase.DRIVING) faults = e.summary().faults.size
            val blocked = works.blockedLanes.minOrNull()?.let { laneCenter(it) }
            (if (faults >= 1 && blocked != null) blocked - e.car.targetX else 0f) to false
        }

        assertThat(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }.take(2))
            .containsExactly(RULE_ROADWORKS, RULE_CRASH).inOrder()
        assertThat(works.barriers().none { it.overlaps(engine.car.box()) }).isTrue()
    }

    @Test
    fun `any three situations in a row can be driven without a fault and nothing crosses anything`() {
        val kinds = SituationKind.entries
        var seed = 0L
        kinds.forEach { a -> kinds.forEach { b -> kinds.forEach { c ->
            val route = listOf(a, b, c)
            val engine = DriveEngine(seed++, route = route)
            val overlaps = mutableListOf<String>()

            val events = engine.drive(maxSeconds = 120f) { e ->
                overlaps += e.visibleOverlaps()
                examinerInput(e)
            }

            assertWithMessage("route $route").that(engine.phase).isEqualTo(DrivePhase.FINISHED)
            assertWithMessage("route $route").that(events.filterIsInstance<DriveEvent.Faulted>().map { it.message }).isEmpty()
            assertWithMessage("route $route").that(overlaps.distinct()).isEmpty()
        } } }
    }

    @Test
    fun `no actor drives through another one on screen, whoever is driving`() {
        val drivers = mapOf<String, (DriveEngine) -> Pair<Float, Boolean>>(
            "examiner" to { examinerInput(it) },
            "patient" to { examinerInput(it, overtakesScooters = false) },
            "waiting" to ::waitingInput,
            "careless" to { 0f to false },
        )
        drivers.forEach { (name, driver) ->
            (1L..40L).forEach { seed ->
                val engine = DriveEngine(seed)
                val overlaps = mutableListOf<String>()

                engine.drive { e ->
                    overlaps += e.visibleOverlaps()
                    driver(e)
                }

                assertWithMessage("$name, seed $seed").that(overlaps.distinct()).isEmpty()
            }
        }
    }

    @Test
    fun `no actor drives on past the end of its own situation into the next one`() {
        listOf<(DriveEngine) -> Pair<Float, Boolean>>({ examinerInput(it) }, { examinerInput(it, false) }, { 0f to false }).forEach { driver ->
            (1L..40L).forEach { seed ->
                val engine = DriveEngine(seed)
                var furthest = 0f

                engine.drive { e ->
                    e.situations.forEach { situation ->
                        situation.obstacles().forEach { furthest = maxOf(furthest, it.top - situation.layoutEnd) }
                    }
                    driver(e)
                }

                // A car or a rider may stick out a little past the marked end, never a whole car.
                assertWithMessage("seed $seed").that(furthest).isAtMost(1f)
            }
        }
    }

    @Test
    fun `whoever drives, a run always reaches the finish`() {
        val drivers = mapOf<String, (DriveEngine) -> Pair<Float, Boolean>>(
            "waiting" to ::waitingInput,
            "careless" to { 0f to false },
        )
        drivers.forEach { (name, driver) ->
            (1L..40L).forEach { seed ->
                val engine = DriveEngine(seed)
                engine.drive(maxSeconds = 300f, driver = driver)
                assertWithMessage("$name, seed $seed").that(engine.phase).isEqualTo(DrivePhase.FINISHED)
            }
        }
    }

    @Test
    fun `the car in front pulls over and parks in the parking lane instead of driving on`() {
        (1L..10L).forEach { seed ->
            val engine = DriveEngine(seed, route = listOf(SituationKind.LEAD_CAR, SituationKind.ROADWORKS))
            val situation = engine.situations.first() as LeadCarSituation

            val events = engine.drive(until = { situation.lead?.x == PARKED_X }) { examinerInput(it) }

            val lead = situation.lead
            assertWithMessage("seed $seed").that(events.filterIsInstance<DriveEvent.Faulted>()).isEmpty()
            assertWithMessage("seed $seed").that(lead).isNotNull()
            // Parked beside the traffic lanes, like every other parked car.
            assertWithMessage("seed $seed").that(lead!!.x).isEqualTo(PARKED_X)
            assertWithMessage("seed $seed").that(lead.speed).isEqualTo(0f)
            assertWithMessage("seed $seed").that(lead.box().left).isAtLeast(LANES.toFloat())
        }
    }

    @Test
    fun `a driver who hangs back behind a scooter sees it turn up onto the pavement`() {
        (1L..10L).forEach { seed ->
            val engine = DriveEngine(seed, route = listOf(SituationKind.SCOOTER, SituationKind.ROADWORKS))
            val scooter = engine.situations.first() as ScooterSituation
            var furthestRight = 0f

            val events = engine.drive { e ->
                if (scooter.visible) furthestRight = maxOf(furthestRight, scooter.riderX)
                examinerInput(e, overtakesScooters = false)
            }

            assertWithMessage("seed $seed").that(events.filterIsInstance<DriveEvent.Faulted>()).isEmpty()
            assertWithMessage("seed $seed").that(furthestRight).isGreaterThan(PARKING_EDGE)
            assertWithMessage("seed $seed").that(scooter.visible).isFalse()
        }
    }

    @Test
    fun `rating is three stars when clean, two with one fault, one otherwise`() {
        val base = DriveSummary(1000, 9, 11, emptyList(), 10, 40, 50)
        assertThat(base.rating).isEqualTo(3)
        assertThat(base.copy(faults = listOf("a")).rating).isEqualTo(2)
        assertThat(base.copy(faults = listOf("a", "b")).rating).isEqualTo(1)
    }
}
