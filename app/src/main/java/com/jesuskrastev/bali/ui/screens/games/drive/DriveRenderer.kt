package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** World width visible at the player's car, in lanes. */
private const val VIEW_WIDTH = 5.6f

/** World x drawn at the horizontal centre of the screen. */
private const val X_CENTER = 1.65f

/** Height of the player's front bumper on screen, as a share of the canvas height. */
private const val ANCHOR = 0.78f

/** How much smaller things look at the top edge than at the car (0.62 → about 62 % bigger at the car). */
private const val PERSPECTIVE = 0.62f

/** Camera distance behind the car, in lanes: smaller means stronger perspective. */
private const val DEPTH = 14f

/**
 * Maps world positions to the canvas with a gentle perspective: rows further ahead are drawn
 * smaller and closer together, like a toy city seen from a slightly tilted camera. Straight lines
 * stay straight, so a world rectangle is drawn as a trapezoid with [quad].
 *
 * @param cameraY world y drawn at [ANCHOR] (the player's front bumper)
 * @param viewWidth world width visible at the car, in lanes; wider shows more of the town
 * @param centerX world x drawn at the horizontal centre of the screen
 */
class DriveProjection(
    val width: Float,
    val height: Float,
    val cameraY: Float,
    viewWidth: Float = VIEW_WIDTH,
    private val centerX: Float = X_CENTER,
) {
    val unit = width / viewWidth
    private val anchorY = height * ANCHOR
    private val horizon = -anchorY / PERSPECTIVE

    /** Size factor of row [y] relative to the car's row. */
    fun depth(y: Float): Float = DEPTH / (max(y - cameraY, -DEPTH * 0.8f) + DEPTH)

    /** Pixels per lane at row [y]. */
    fun scale(y: Float): Float = unit * depth(y)

    /** Screen x of world point ([x], [y]). */
    fun sx(x: Float, y: Float): Float = width / 2 + (x - centerX) * scale(y)

    /** Screen y of world row [y]. */
    fun sy(y: Float): Float = horizon + (anchorY - horizon) * depth(y)

    /** Screen y of the player's front bumper. */
    val anchorScreenY: Float get() = anchorY

    /** Furthest row visible at the top edge, plus a margin. */
    val farY: Float get() = cameraY + DEPTH * PERSPECTIVE + 1f

    /** Nearest row visible at the bottom edge, minus a margin. */
    val nearY: Float get() = cameraY - 2.5f - (height - anchorY) / unit
}

/** Palette of the toy city: soft, sunny and readable, with the app's orange on the player's car. */
object DrivePalette {
    val Grass = Color(0xFF9AD77F)
    val GrassStripe = Color(0xFF8CCB71)
    val Pavement = Color(0xFFF3EBDD)
    val PavementLine = Color(0xFFE2D7C3)
    val Kerb = Color(0xFFD9CDB8)
    val Road = Color(0xFF4B5567)
    val Parking = Color(0xFF5A6476)
    val Marking = Color(0xFFF8FAFC)
    val Shadow = Color(0x33000000)
    val Player = Color(0xFFFF6B35)
    val PlayerDark = Color(0xFFE2541F)
    val Glass = Color(0xFF1E293B)
    val Star = Color(0xFFFACC15)
    val StarEdge = Color(0xFFF59E0B)
    val Cone = Color(0xFFFF7A1A)
    val BrakeLight = Color(0xFFEF4444)
    val Amber = Color(0xFFF59E0B)
    val Green = Color(0xFF22C55E)
    val SignBlue = Color(0xFF2563EB)
    val SignRed = Color(0xFFDC2626)
    val Zone30 = Color(0x3DE11D48)
    val Shield = Color(0xFF38BDF8)
    val Magnet = Color(0xFFEF4444)
    val Slow = Color(0xFFA78BFA)
    val Siren = Color(0xFF2563EB)
    val Trunk = Color(0xFF7C5A3A)
    val TreeDark = Color(0xFF3E9A5B)
    val Tree = Color(0xFF52B86E)
    val TreeLight = Color(0xFF7ACF8C)
    val npc = listOf(
        Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFF8B5CF6),
        Color(0xFFF43F5E), Color(0xFFEAB308), Color(0xFFF1F5F9),
    )
    val roofs = listOf(Color(0xFFFB923C), Color(0xFFF87171), Color(0xFF60A5FA), Color(0xFFA78BFA), Color(0xFF2DD4BF))
    val shirts = listOf(Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF10B981), Color(0xFFF97316))
    val hair = listOf(Color(0xFF3F2A1D), Color(0xFF111827), Color(0xFFB45309), Color(0xFF78350F))
    val skin = Color(0xFFF2C29B)
}

/**
 * Pre-measured images and texts the renderer stamps on the scene.
 *
 * @param bali the mascot, drawn peeking out of the player's sunroof
 * @param stop "STOP" lettering for the sign and the road
 * @param finish "META" lettering for the finish arch
 * @param alert "!" over a pedestrian who is about to cross
 * @param thirty "30" for the zone 30 sign and road marking
 * @param double "×2" for the double-points bubble
 */
class DriveArt(
    val bali: ImageBitmap?,
    val stop: TextLayoutResult,
    val finish: TextLayoutResult,
    val alert: TextLayoutResult,
    val thirty: TextLayoutResult,
    val double: TextLayoutResult,
)

/** A short-lived bit of confetti or sparkle, in world coordinates; [z] is height above the road. */
class Particle(
    var x: Float, var y: Float, var z: Float,
    val vx: Float, val vy: Float, var vz: Float,
    var life: Float, val maxLife: Float, val color: Color, val size: Float, val round: Boolean,
)

/** An expanding ring around the car that marks a well-handled situation. */
class Ring(val x: Float, val y: Float, var age: Float, val color: Color)

/**
 * Purely visual effects layered on the simulation: particles, success rings, screen shake and the
 * red flash of a fault. Kept out of [DriveEngine] so the rules stay testable without them.
 */
class DriveFx {
    val particles = ArrayList<Particle>()
    val rings = ArrayList<Ring>()
    val skids = ArrayDeque<Offset>()
    var shake = 0f
    var flash = 0f
    var glow = 0f
    private val random = kotlin.random.Random(42)

    /**
     * Throws [count] bits of confetti from world point ([x], [y]).
     *
     * @param colors palette the bits are picked from
     * @param power initial speed
     */
    fun burst(x: Float, y: Float, colors: List<Color>, count: Int, power: Float = 2.2f) {
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = power * (0.4f + random.nextFloat() * 0.8f)
            val life = 0.6f + random.nextFloat() * 0.6f
            particles += Particle(
                x = x, y = y, z = 0.2f,
                vx = cos(angle) * speed, vy = sin(angle) * speed, vz = 1.5f + random.nextFloat() * 2.5f,
                life = life, maxLife = life,
                color = colors[random.nextInt(colors.size)],
                size = 0.05f + random.nextFloat() * 0.05f,
                round = random.nextBoolean(),
            )
        }
    }

    /** Adds the expanding success ring around ([x], [y]). */
    fun ring(x: Float, y: Float, color: Color) {
        rings += Ring(x, y, 0f, color)
    }

    /** Leaves a tyre mark at world point ([x], [y]) while braking hard. */
    fun skid(x: Float, y: Float) {
        skids.addLast(Offset(x, y))
        while (skids.size > 160) skids.removeFirst()
    }

    /** Ages every effect by [dt] seconds. */
    fun update(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vz -= 9f * dt
            p.z = max(0f, p.z + p.vz * dt)
        }
        rings.forEach { it.age += dt }
        rings.removeAll { it.age > 0.6f }
        shake = max(0f, shake - dt * 2.5f)
        flash = max(0f, flash - dt * 1.6f)
        glow = max(0f, glow - dt * 2f)
    }
}

/** Unit five-point star, reused by every star on the road. */
private val STAR_PATH: Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) 1f else 0.45f
        val angle = -PI / 2 + i * PI / 5
        val px = (cos(angle) * radius).toFloat()
        val py = (sin(angle) * radius).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** Unit regular octagon (STOP sign). */
private val OCTAGON_PATH: Path = Path().apply {
    for (i in 0 until 8) {
        val angle = PI / 8 + i * PI / 4
        val px = (cos(angle)).toFloat()
        val py = (sin(angle)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** Reused path for every projected quad, so a frame allocates no paths. */
private val QUAD_PATH = Path()

/**
 * Draws a whole frame of the run: ground, road markings, every actor sorted far to near, then
 * the effects on top.
 *
 * @param time seconds since the run started, for idle animations (stars bobbing, lights blinking)
 */
fun DrawScope.drawDriveWorld(engine: DriveEngine, fx: DriveFx, p: DriveProjection, art: DriveArt, time: Float) {
    drawGround(engine, p)
    drawMarkings(engine, p, art, time)
    drawSkids(fx, p)
    drawSprites(engine, p, art, time)
    drawSirenWarning(engine, p, time)
    drawEffects(fx, p)
}

/** Fills the world rectangle x0..x1 × y0..y1 as its projected trapezoid. */
private fun DrawScope.quad(p: DriveProjection, x0: Float, y0: Float, x1: Float, y1: Float, color: Color) {
    val near = max(y0, p.nearY)
    val far = min(y1, p.farY)
    if (far <= near) return
    QUAD_PATH.reset()
    QUAD_PATH.moveTo(p.sx(x0, near), p.sy(near))
    QUAD_PATH.lineTo(p.sx(x1, near), p.sy(near))
    QUAD_PATH.lineTo(p.sx(x1, far), p.sy(far))
    QUAD_PATH.lineTo(p.sx(x0, far), p.sy(far))
    QUAD_PATH.close()
    drawPath(QUAD_PATH, color)
}

/** Grass, pavements, parking lane and asphalt. */
private fun DrawScope.drawGround(engine: DriveEngine, p: DriveProjection) {
    drawRect(DrivePalette.Grass, Offset.Zero, Size(p.width, p.height))
    // Mown stripes scroll past and sell the speed.
    var band = floor(p.nearY / 2f) * 2f
    while (band < p.farY) {
        quad(p, -12f, band, 16f, band + 1f, DrivePalette.GrassStripe)
        band += 2f
    }
    quad(p, -0.6f, p.nearY, 0f, p.farY, DrivePalette.Pavement)
    quad(p, PARKING_EDGE, p.nearY, PARKING_EDGE + 0.6f, p.farY, DrivePalette.Pavement)
    var tile = floor(p.nearY / 0.7f) * 0.7f
    while (tile < p.farY) {
        quad(p, -0.6f, tile, 0f, tile + 0.03f, DrivePalette.PavementLine)
        quad(p, PARKING_EDGE, tile, PARKING_EDGE + 0.6f, tile + 0.03f, DrivePalette.PavementLine)
        tile += 0.7f
    }
    quad(p, LANES.toFloat(), p.nearY, PARKING_EDGE, p.farY, DrivePalette.Parking)
    quad(p, 0f, p.nearY, LANES.toFloat(), p.farY, DrivePalette.Road)
    for (situation in engine.situations) {
        if (situation is Zone30Situation) quad(p, 0f, situation.y, LANES.toFloat(), situation.layoutEnd, DrivePalette.Zone30)
    }
    quad(p, -0.08f, p.nearY, 0f, p.farY, DrivePalette.Kerb)
    quad(p, PARKING_EDGE, p.nearY, PARKING_EDGE + 0.08f, p.farY, DrivePalette.Kerb)
}

/** True when row [y] falls inside a junction, where lane markings and scenery stop. */
private fun DriveEngine.isJunction(y: Float, margin: Float = 0f): Boolean = situations.any {
    when (it) {
        is StopSituation -> y > it.crossStart - margin && y < it.crossEnd + margin
        is TrafficLightSituation -> y > it.crossStart - margin && y < it.crossEnd + margin
        else -> false
    }
}

/**
 * Lane lines, the cross streets of junctions (drawn over the avenue's edge lines), zebras, stop
 * lines, "STOP" and "30" lettering, closed-lane hatching, the 1.5 m zone around a scooter rider
 * and the start/finish checks.
 *
 * @param time seconds since the run started, for the pulse of the scooter's safety zone
 */
private fun DrawScope.drawMarkings(engine: DriveEngine, p: DriveProjection, art: DriveArt, time: Float) {
    val white = DrivePalette.Marking.copy(alpha = 0.9f)
    quad(p, 0.06f, p.nearY, 0.12f, p.farY, white)
    quad(p, 2.88f, p.nearY, 2.94f, p.farY, white)
    var dash = floor(p.nearY / 1.2f) * 1.2f
    while (dash < p.farY) {
        if (!engine.isJunction(dash, 0.6f)) {
            for (lane in 1 until LANES) quad(p, lane - 0.03f, dash, lane + 0.03f, dash + 0.55f, white)
        }
        if (!engine.isJunction(dash, 0.3f)) quad(p, LANES.toFloat(), dash, PARKING_EDGE, dash + 0.035f, white.copy(alpha = 0.55f))
        dash += 1.2f
    }

    for (situation in engine.situations) {
        val (start, end) = when (situation) {
            is StopSituation -> situation.crossStart to situation.crossEnd
            is TrafficLightSituation -> situation.crossStart to situation.crossEnd
            else -> continue
        }
        if (end < p.nearY || start > p.farY) continue
        quad(p, -12f, start - 0.08f, 16f, end + 0.08f, DrivePalette.Kerb)
        quad(p, -12f, start, 16f, end, DrivePalette.Road)
        var x = -12f
        while (x < 16f) {
            quad(p, x, (start + end) / 2 - 0.03f, x + 0.5f, (start + end) / 2 + 0.03f, DrivePalette.Marking.copy(alpha = 0.8f))
            x += 1f
        }
    }

    for (situation in engine.situations) {
        if (situation.layoutEnd < p.nearY - 2f || situation.layoutStart > p.farY + 2f) continue
        when (situation) {
            is CrosswalkSituation -> {
                quad(p, 0f, situation.y - 0.25f, PARKING_EDGE, situation.y + situation.zebraLength + 0.25f, DrivePalette.Road)
                var x = 0.1f
                while (x < PARKING_EDGE - 0.1f) {
                    quad(p, x, situation.y, x + 0.2f, situation.y + situation.zebraLength, white)
                    x += 0.38f
                }
            }
            is StopSituation -> {
                quad(p, 0f, situation.y - 0.08f, LANES.toFloat(), situation.y + 0.06f, white)
                drawRoadText(p, 1.5f, situation.y - 0.75f, 0.42f, DrivePalette.Marking, art.stop)
            }
            is TrafficLightSituation -> quad(p, 0f, situation.y - 0.08f, LANES.toFloat(), situation.y + 0.04f, white)
            is Zone30Situation -> {
                quad(p, 0f, situation.y, LANES.toFloat(), situation.y + 0.1f, white)
                for (lane in 0 until LANES) {
                    drawRoadRing(p, laneCenter(lane), situation.y + 0.8f)
                    drawRoadText(p, laneCenter(lane), situation.y + 0.8f, 0.5f, DrivePalette.Marking, art.thirty)
                }
            }
            is ScooterSituation -> if (situation.visible && situation.status == SituationStatus.UPCOMING) {
                drawSafetyZone(p, situation.safetyBox(), time)
            }
            is RoadworksSituation -> situation.blockedLanes.forEach { lane ->
                var stripe = situation.y + 0.2f
                while (stripe < situation.y + situation.length) {
                    quad(p, lane + 0.18f, stripe, lane + 0.82f, stripe + 0.08f, DrivePalette.Amber.copy(alpha = 0.75f))
                    stripe += 0.35f
                }
            }
            else -> Unit
        }
    }
    drawChecks(p, -0.6f)
    drawChecks(p, engine.finishY)
}

/** Paints [text] flat on the road, centred on ([x], [y]), [height] lanes tall. */
private fun DrawScope.drawRoadText(p: DriveProjection, x: Float, y: Float, height: Float, color: Color, text: TextLayoutResult) {
    if (y < p.nearY || y > p.farY) return
    val s = p.scale(y)
    val k = height * s / text.size.height
    withTransform({
        translate(p.sx(x, y), p.sy(y))
        scale(k * 1.25f, k * 0.8f, Offset.Zero)
    }) {
        drawText(text, color = color.copy(alpha = 0.92f), topLeft = Offset(-text.size.width / 2f, -text.size.height / 2f))
    }
}

/** Red ring painted flat on the road around the "30" at ([x], [y]), like the speed-limit paint on a real street. */
private fun DrawScope.drawRoadRing(p: DriveProjection, x: Float, y: Float) {
    if (y < p.nearY || y > p.farY) return
    val s = p.scale(y)
    withTransform({ translate(p.sx(x, y), p.sy(y)); scale(s, s, Offset.Zero) }) {
        drawOval(DrivePalette.SignRed.copy(alpha = 0.85f), Offset(-0.43f, -0.27f), Size(0.86f, 0.54f), style = Stroke(0.07f))
    }
}

/**
 * The 1.5 m a passing car must leave around a scooter rider, drawn on the asphalt as a softly
 * pulsing outlined zone, so the rule can be seen instead of guessed.
 *
 * @param zone the rider and the margin around them, in world units
 * @param time seconds since the run started
 */
private fun DrawScope.drawSafetyZone(p: DriveProjection, zone: Box, time: Float) {
    val pulse = 0.5f + 0.5f * sin(time * 5f)
    val edge = Color.White.copy(alpha = 0.3f + 0.2f * pulse)
    val thickness = 0.035f
    quad(p, zone.left, zone.bottom, zone.right, zone.top, Color.White.copy(alpha = 0.06f + 0.04f * pulse))
    quad(p, zone.left, zone.bottom, zone.right, zone.bottom + thickness, edge)
    quad(p, zone.left, zone.top - thickness, zone.right, zone.top, edge)
    quad(p, zone.left, zone.bottom, zone.left + thickness, zone.top, edge)
    quad(p, zone.right - thickness, zone.bottom, zone.right, zone.top, edge)
}

/**
 * Warns of an ambulance racing along a lane before it comes into view: the lane tints flashing
 * blue and red from halfway down the screen, with chevrons rising along it, stronger the closer
 * the ambulance gets. It sits above the corner widgets, which would otherwise hide it.
 *
 * @param time seconds since the run started, for the flashing
 */
private fun DrawScope.drawSirenWarning(engine: DriveEngine, p: DriveProjection, time: Float) {
    val situation = engine.situations.firstOrNull { it is AmbulanceSituation && it.ambulance != null && !it.hasPassed } as? AmbulanceSituation ?: return
    val ambulance = situation.ambulance ?: return
    val behind = p.cameraY - ambulance.y
    // It becomes visible about 1.9 behind the car's front bumper; before that, the glow stands in for it.
    if (behind < 1.9f || behind > 9f) return
    val strength = (1f - (behind - 1.9f) / 7.1f).coerceIn(0.3f, 1f)
    val color = if (sin(time * 14f) > 0f) DrivePalette.Siren else DrivePalette.SignRed
    val lane = situation.ambulanceLane
    val left = p.sx(lane.toFloat(), p.cameraY)
    val right = p.sx(lane + 1f, p.cameraY)
    val width = right - left
    val top = p.height * 0.55f
    drawRect(
        Brush.verticalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f * strength)), startY = top, endY = p.height),
        Offset(left, top),
        Size(width, p.height - top),
    )
    for (i in 0 until 3) {
        val rise = (time * 1.8f + i / 3f) % 1f
        val cy = p.height * (0.93f - 0.38f * rise)
        val half = width * 0.2f
        val chevron = Path().apply {
            moveTo(left + width / 2 - half, cy + half * 0.6f)
            lineTo(left + width / 2, cy - half * 0.4f)
            lineTo(left + width / 2 + half, cy + half * 0.6f)
        }
        drawPath(chevron, Color.White.copy(alpha = (0.5f + 0.5f * strength) * (1f - rise * 0.7f)), style = Stroke(width * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Black-and-white checks across the road at row [y]. */
private fun DrawScope.drawChecks(p: DriveProjection, y: Float) {
    val size = 0.25f
    for (row in 0 until 2) {
        var x = 0f
        var column = 0
        while (x < LANES) {
            val color = if ((row + column) % 2 == 0) Color(0xFF111827) else DrivePalette.Marking
            quad(p, x, y + row * size, x + size, y + (row + 1) * size, color)
            x += size
            column++
        }
    }
}

/** Dark tyre marks left by hard braking. */
private fun DrawScope.drawSkids(fx: DriveFx, p: DriveProjection) {
    for (mark in fx.skids) {
        if (mark.y < p.nearY || mark.y > p.farY) continue
        val s = p.scale(mark.y)
        drawCircle(Color(0x30000000), radius = 0.045f * s, center = Offset(p.sx(mark.x, mark.y), p.sy(mark.y)))
    }
}

/** Something drawn at world row [y]; sprites are painted far to near so nearer ones overlap. */
private class Sprite(val y: Float, val draw: DrawScope.() -> Unit)

/** Draws every actor and prop, sorted by depth. */
private fun DrawScope.drawSprites(engine: DriveEngine, p: DriveProjection, art: DriveArt, time: Float) {
    val sprites = ArrayList<Sprite>(96)
    addScenery(engine, p, sprites)

    for (parked in engine.parkedCars) {
        if (parked.y < p.nearY - 1f || parked.y > p.farY + 1f) continue
        sprites += Sprite(parked.y) {
            drawCarAt(p, (LANES + PARKING_EDGE) / 2, parked.y, DrivePalette.npc[parked.color % DrivePalette.npc.size], braking = false)
        }
    }
    for (star in engine.stars) {
        if (star.collected || star.y < p.nearY || star.y > p.farY) continue
        sprites += Sprite(star.y) { drawStar(p, star.x, star.y, time) }
    }
    for (powerUp in engine.powerUps) {
        if (powerUp.collected || powerUp.y < p.nearY || powerUp.y > p.farY) continue
        sprites += Sprite(powerUp.y) { drawPowerUp(p, powerUp, art, time) }
    }
    for (situation in engine.situations) {
        if (situation.layoutEnd < p.nearY - 6f || situation.layoutStart > p.farY + 2f) continue
        addSituationSprites(situation, p, art, time, sprites)
    }
    val car = engine.car
    sprites += Sprite(car.y - CAR_LENGTH / 2) {
        val yaw = ((car.targetX - car.x) * 22f).coerceIn(-18f, 18f)
        drawPowerAura(engine, p, time)
        drawCarAt(p, car.x, car.y - CAR_LENGTH / 2, DrivePalette.Player, car.braking, yaw = yaw, bali = art.bali)
    }
    drawSignBillboards(engine, p, art, time, sprites)

    sprites.sortByDescending { it.y }
    sprites.forEach { it.draw(this) }
}

/** Trees and little houses along both sides, placed deterministically from the row index. */
private fun DrawScope.addScenery(engine: DriveEngine, p: DriveProjection, sprites: MutableList<Sprite>) {
    var block = floor(p.nearY / 2f).toInt() - 1
    while (block * 2f < p.farY + 2f) {
        val y = block * 2f
        val hash = (block * 73856093) xor 0x5bd1e995
        if (!engine.isJunction(y, 0.9f)) {
            val jitter = ((hash ushr 3) and 7) / 20f
            sprites += Sprite(y + 0.3f) { drawTree(p, -1.05f - jitter, y + 0.3f, 0.36f + jitter / 3) }
            sprites += Sprite(y + 1.1f) { drawTree(p, PARKING_EDGE + 1.05f + jitter, y + 1.1f, 0.34f + jitter / 3) }
            val flowers = DrivePalette.roofs[(hash ushr 5 and 0xFF) % DrivePalette.roofs.size]
            if (hash and 1 == 0) sprites += Sprite(y + 1.3f) { drawBush(p, -0.95f, y + 1.3f, flowers) }
            if (hash and 2 == 0) sprites += Sprite(y + 0.2f) { drawBush(p, PARKING_EDGE + 0.95f, y + 0.2f, flowers) }
            if (hash and 4 == 0) sprites += Sprite(y + 0.9f) { drawHouse(p, -2.1f, y + 0.9f, DrivePalette.roofs[(hash ushr 9 and 0xFF) % DrivePalette.roofs.size]) }
            sprites += Sprite(y + 1.6f) { drawTree(p, -3.3f + jitter, y + 1.6f, 0.42f) }
            if (hash and 8 == 0) sprites += Sprite(y + 0.5f) { drawBush(p, -2.2f - jitter, y + 0.5f, flowers) }
        }
        block++
    }
}

/** Ground-level actors of one situation: cones, pedestrians, the ball, other cars. */
private fun addSituationSprites(situation: Situation, p: DriveProjection, art: DriveArt, time: Float, sprites: MutableList<Sprite>) {
    when (situation) {
        is RoadworksSituation -> situation.barriers().forEach { barrier ->
            sprites += Sprite(barrier.bottom) { drawBarrier(p, barrier.left, barrier.right, barrier.bottom, time) }
            var cone = barrier.bottom + 0.45f
            while (cone < barrier.top) {
                val y = cone
                sprites += Sprite(y) {
                    drawCone(p, barrier.left + 0.08f, y)
                    drawCone(p, barrier.right - 0.08f, y)
                }
                cone += 0.5f
            }
        }
        is CrosswalkSituation -> sprites += Sprite(situation.walker.y) { drawWalker(p, situation.walker, art) }
        is StopSituation -> situation.crossCar?.let { car ->
            sprites += Sprite(car.y) { drawCarAt(p, car.x, car.y, DrivePalette.npc[car.color % DrivePalette.npc.size], false, yaw = 90f) }
        }
        is TrafficLightSituation -> situation.crossCar?.let { car ->
            sprites += Sprite(car.y) { drawCarAt(p, car.x, car.y, DrivePalette.npc[car.color % DrivePalette.npc.size], false, yaw = -90f) }
        }
        is LeadCarSituation -> situation.lead?.let { car ->
            sprites += Sprite(car.y) {
                drawCarAt(p, car.x, car.y, DrivePalette.npc[car.color % DrivePalette.npc.size], car.braking, yaw = (car.drift * 10f).coerceIn(-16f, 16f))
            }
        }
        is ScooterSituation -> if (situation.visible) {
            sprites += Sprite(situation.riderY) { drawRider(p, situation, time) }
        }
        is AmbulanceSituation -> situation.ambulance?.let { ambulance ->
            sprites += Sprite(ambulance.y) { drawAmbulance(p, ambulance.x, ambulance.y, time) }
        }
        is Zone30Situation -> Unit
        is BallSituation -> {
            if (situation.ballVisible) sprites += Sprite(situation.y) { drawBall(p, situation.ballX, situation.y, situation.ballSpin) }
            if (situation.child.visible) sprites += Sprite(situation.child.y) { drawWalker(p, situation.child, art) }
        }
    }
}

/** Upright signs, traffic lights and the finish arch; drawn as billboards so they stay readable. */
private fun drawSignBillboards(engine: DriveEngine, p: DriveProjection, art: DriveArt, time: Float, sprites: MutableList<Sprite>) {
    for (situation in engine.situations) {
        if (situation.layoutEnd < p.nearY || situation.layoutStart > p.farY + 2f) continue
        when (situation) {
            is StopSituation -> sprites += Sprite(situation.y - 0.25f) { drawStopSign(p, PARKING_EDGE + 0.3f, situation.y - 0.25f, art) }
            is TrafficLightSituation -> {
                sprites += Sprite(situation.y - 0.2f) { drawTrafficLight(p, PARKING_EDGE + 0.3f, situation.y - 0.2f, situation.light) }
                sprites += Sprite(situation.y - 0.2f) { drawTrafficLight(p, -0.3f, situation.y - 0.2f, situation.light) }
            }
            is CrosswalkSituation -> sprites += Sprite(situation.y - 0.3f) { drawCrosswalkSign(p, PARKING_EDGE + 0.3f, situation.y - 0.3f) }
            is RoadworksSituation -> sprites += Sprite(situation.y - 2.4f) { drawWorksSign(p, PARKING_EDGE + 0.3f, situation.y - 2.4f) }
            is Zone30Situation -> {
                sprites += Sprite(situation.y - 0.3f) { drawZone30Sign(p, PARKING_EDGE + 0.3f, situation.y - 0.3f, art) }
                sprites += Sprite(situation.y - 0.3f) { drawZone30Sign(p, -0.3f, situation.y - 0.3f, art) }
            }
            else -> Unit
        }
    }
    if (engine.finishY in p.nearY..p.farY + 2f) {
        sprites += Sprite(engine.finishY) { drawFinishArch(p, engine.finishY, art, time) }
    }
}

/**
 * Runs [block] with the canvas moved to world point ([x], [y]) and scaled so one unit is one lane
 * there, optionally rotated by [rotation] degrees.
 */
private inline fun DrawScope.atWorld(p: DriveProjection, x: Float, y: Float, rotation: Float = 0f, crossinline block: DrawScope.() -> Unit) {
    val s = p.scale(y)
    withTransform({
        translate(p.sx(x, y), p.sy(y))
        if (rotation != 0f) rotate(rotation, Offset.Zero)
        scale(s, s, Offset.Zero)
    }) { block() }
}

/**
 * Draws a car seen from above, centred on world ([x], [y]), nose pointing forward.
 *
 * @param braking lights up the brake lights with a glow
 * @param yaw rotation in degrees; ±90 for cars on a cross street, a little for the player's steering
 * @param bali when given, the mascot peeks out of the sunroof (the player's car)
 */
private fun DrawScope.drawCarAt(
    p: DriveProjection, x: Float, y: Float, body: Color, braking: Boolean,
    yaw: Float = 0f, bali: ImageBitmap? = null,
) {
    if (y < p.nearY - 1f || y > p.farY + 1f) return
    val w = CAR_WIDTH
    val l = CAR_LENGTH
    atWorld(p, x, y, yaw) {
        drawRoundRect(DrivePalette.Shadow, Offset(-w / 2 + 0.05f, -l / 2 + 0.08f), Size(w, l), CornerRadius(0.16f))
        for (side in listOf(-1f, 1f)) for (axle in listOf(-0.28f, 0.27f)) {
            drawRoundRect(Color(0xFF1F2937), Offset(side * w / 2 - 0.05f, axle - 0.1f), Size(0.1f, 0.2f), CornerRadius(0.04f))
        }
        drawRoundRect(body, Offset(-w / 2, -l / 2), Size(w, l), CornerRadius(0.16f))
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.12f)), -w / 2, w / 2),
            Offset(-w / 2, -l / 2), Size(w, l), CornerRadius(0.16f),
        )
        drawRoundRect(DrivePalette.Glass, Offset(-w / 2 + 0.07f, -l / 2 + 0.17f), Size(w - 0.14f, 0.19f), CornerRadius(0.06f))
        drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(-w / 2 + 0.1f, -l / 2 + 0.19f), Size(0.12f, 0.06f), CornerRadius(0.03f))
        drawRoundRect(DrivePalette.Glass.copy(alpha = 0.9f), Offset(-w / 2 + 0.09f, l / 2 - 0.25f), Size(w - 0.18f, 0.11f), CornerRadius(0.05f))
        if (bali != null) {
            drawRoundRect(DrivePalette.PlayerDark, Offset(-0.19f, -0.13f), Size(0.38f, 0.36f), CornerRadius(0.1f))
            withTransform({ scale(0.01f, 0.01f, Offset.Zero) }) {
                drawImage(bali, dstOffset = IntOffset(-19, -15), dstSize = IntSize(38, 38))
            }
            drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(-0.035f, -l / 2 + 0.02f), Size(0.07f, 0.14f), CornerRadius(0.03f))
        } else {
            drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(-w / 2 + 0.09f, -0.06f), Size(w - 0.18f, 0.26f), CornerRadius(0.07f))
        }
        val head = Color(0xFFFFF7D6)
        drawCircle(head, 0.045f, Offset(-w / 2 + 0.1f, -l / 2 + 0.05f))
        drawCircle(head, 0.045f, Offset(w / 2 - 0.1f, -l / 2 + 0.05f))
        val brake = if (braking) DrivePalette.BrakeLight else Color(0xFF991B1B)
        if (braking) {
            drawCircle(DrivePalette.BrakeLight.copy(alpha = 0.3f), 0.12f, Offset(-w / 2 + 0.1f, l / 2 - 0.02f))
            drawCircle(DrivePalette.BrakeLight.copy(alpha = 0.3f), 0.12f, Offset(w / 2 - 0.1f, l / 2 - 0.02f))
        }
        drawRoundRect(brake, Offset(-w / 2 + 0.04f, l / 2 - 0.06f), Size(0.13f, 0.05f), CornerRadius(0.02f))
        drawRoundRect(brake, Offset(w / 2 - 0.17f, l / 2 - 0.06f), Size(0.13f, 0.05f), CornerRadius(0.02f))
    }
}

/** A power-up bubble: a glossy coloured orb with its icon, bobbing and glowing. */
private fun DrawScope.drawPowerUp(p: DriveProjection, powerUp: PowerUp, art: DriveArt, time: Float) {
    val bob = sin(time * 4f + powerUp.y)
    val color = powerUpColor(powerUp.kind)
    atWorld(p, powerUp.x, powerUp.y) {
        drawOval(DrivePalette.Shadow, Offset(-0.2f, 0.1f), Size(0.4f, 0.14f))
        withTransform({ translate(0f, -0.14f - bob * 0.04f) }) {
            drawCircle(color.copy(alpha = 0.25f + 0.1f * bob), 0.36f, Offset.Zero)
            drawCircle(Color.White, 0.25f, Offset.Zero)
            drawCircle(color, 0.21f, Offset.Zero)
            drawPowerUpIcon(powerUp.kind, art)
            drawCircle(Color.White.copy(alpha = 0.55f), 0.06f, Offset(-0.09f, -0.1f))
        }
    }
}

/** Colour that identifies a power-up everywhere (bubble, aura, HUD). */
fun powerUpColor(kind: PowerUpKind): Color = when (kind) {
    PowerUpKind.SHIELD -> DrivePalette.Shield
    PowerUpKind.MAGNET -> DrivePalette.Magnet
    PowerUpKind.DOUBLE -> DrivePalette.Star
    PowerUpKind.SLOW_MOTION -> DrivePalette.Slow
}

/** White icon of a power-up, about 0.24 lanes across, centred on the origin. */
private fun DrawScope.drawPowerUpIcon(kind: PowerUpKind, art: DriveArt) {
    val ink = Color.White
    when (kind) {
        PowerUpKind.SHIELD -> {
            val shield = Path().apply {
                moveTo(0f, -0.12f); lineTo(0.1f, -0.08f); lineTo(0.09f, 0.03f)
                quadraticTo(0.06f, 0.1f, 0f, 0.13f); quadraticTo(-0.06f, 0.1f, -0.09f, 0.03f)
                lineTo(-0.1f, -0.08f); close()
            }
            drawPath(shield, ink)
        }
        PowerUpKind.MAGNET -> {
            drawArc(ink, 0f, 180f, false, Offset(-0.09f, -0.06f), Size(0.18f, 0.18f), style = Stroke(0.06f))
            drawRect(ink, Offset(-0.12f, -0.1f), Size(0.06f, 0.08f))
            drawRect(ink, Offset(0.06f, -0.1f), Size(0.06f, 0.08f))
        }
        PowerUpKind.DOUBLE -> {
            val t = art.double
            val q = 0.2f / t.size.height
            withTransform({ scale(q, q, Offset.Zero) }) {
                drawText(t, color = Color(0xFF7C2D12), topLeft = Offset(-t.size.width / 2f, -t.size.height / 2f))
            }
        }
        PowerUpKind.SLOW_MOTION -> {
            val glass = Path().apply {
                moveTo(-0.08f, -0.11f); lineTo(0.08f, -0.11f); lineTo(0f, 0f); lineTo(0.08f, 0.11f)
                lineTo(-0.08f, 0.11f); lineTo(0f, 0f); close()
            }
            drawPath(glass, ink)
        }
    }
}

/** Glow around the player's car for the active power-ups: shield bubble, magnet field, golden ×2. */
private fun DrawScope.drawPowerAura(engine: DriveEngine, p: DriveProjection, time: Float) {
    val car = engine.car
    atWorld(p, car.x, car.y - CAR_LENGTH / 2) {
        if (engine.isActive(PowerUpKind.DOUBLE)) {
            drawCircle(DrivePalette.Star.copy(alpha = 0.3f + 0.1f * sin(time * 8f)), 0.7f, Offset.Zero)
        }
        if (engine.isActive(PowerUpKind.MAGNET)) {
            val wave = (time * 1.6f) % 1f
            drawCircle(DrivePalette.Magnet.copy(alpha = 0.5f * (1f - wave)), 0.5f + wave * 1.2f, Offset.Zero, style = Stroke(0.05f))
        }
        if (engine.shield) {
            drawCircle(DrivePalette.Shield.copy(alpha = 0.18f), 0.72f, Offset.Zero)
            drawCircle(DrivePalette.Shield.copy(alpha = 0.75f), 0.72f, Offset.Zero, style = Stroke(0.05f))
        }
    }
}

/** Someone on an e-scooter (or a bike) seen from above, with a helmet and a slight wobble. */
private fun DrawScope.drawRider(p: DriveProjection, situation: ScooterSituation, time: Float) {
    val tilt = sin(situation.wobble) * 4f
    val shirt = DrivePalette.shirts[situation.style % DrivePalette.shirts.size]
    atWorld(p, situation.riderX, situation.riderY, tilt) {
        withTransform({ scale(1.5f, 1.5f, Offset.Zero) }) {
            drawOval(DrivePalette.Shadow, Offset(-0.1f, -0.22f), Size(0.24f, 0.5f))
            if (situation.bike) {
                drawOval(Color(0xFF111827), Offset(-0.03f, -0.26f), Size(0.06f, 0.16f))
                drawOval(Color(0xFF111827), Offset(-0.03f, 0.1f), Size(0.06f, 0.16f))
                drawLine(Color(0xFF0EA5E9), Offset(0f, -0.15f), Offset(0f, 0.15f), strokeWidth = 0.035f)
            } else {
                drawRoundRect(Color(0xFF1F2937), Offset(-0.045f, -0.22f), Size(0.09f, 0.44f), CornerRadius(0.04f))
            }
            drawLine(Color(0xFF374151), Offset(-0.12f, -0.17f), Offset(0.12f, -0.17f), strokeWidth = 0.03f)
            drawCircle(DrivePalette.skin, 0.03f, Offset(-0.11f, -0.15f))
            drawCircle(DrivePalette.skin, 0.03f, Offset(0.11f, -0.15f))
            drawOval(shirt, Offset(-0.1f, -0.1f), Size(0.2f, 0.18f))
            drawCircle(DrivePalette.BrakeLight, 0.07f, Offset(0f, -0.02f))
            drawCircle(Color.White.copy(alpha = 0.5f), 0.025f, Offset(-0.02f, -0.04f))
        }
    }
}

/** A white ambulance with a red stripe and a flashing blue light bar. */
private fun DrawScope.drawAmbulance(p: DriveProjection, x: Float, y: Float, time: Float) {
    if (y < p.nearY - 1f || y > p.farY + 1f) return
    drawCarAt(p, x, y, Color(0xFFF8FAFC), braking = false)
    val flash = sin(time * 18f) > 0f
    atWorld(p, x, y) {
        drawRect(DrivePalette.SignRed, Offset(-CAR_WIDTH / 2, 0.02f), Size(CAR_WIDTH, 0.06f))
        drawRect(DrivePalette.SignRed, Offset(-0.03f, -0.1f), Size(0.06f, 0.2f))
        drawRect(DrivePalette.SignRed, Offset(-0.1f, -0.03f), Size(0.2f, 0.06f))
        val left = Offset(-0.12f, -0.2f)
        val right = Offset(0.12f, -0.2f)
        drawCircle(DrivePalette.Siren.copy(alpha = if (flash) 0.45f else 0.1f), 0.32f, left)
        drawCircle(DrivePalette.SignRed.copy(alpha = if (flash) 0.1f else 0.45f), 0.32f, right)
        drawRoundRect(if (flash) DrivePalette.Siren else Color(0xFF1E3A8A), Offset(-0.18f, -0.24f), Size(0.17f, 0.08f), CornerRadius(0.03f))
        drawRoundRect(if (flash) Color(0xFF7F1D1D) else DrivePalette.SignRed, Offset(0.01f, -0.24f), Size(0.17f, 0.08f), CornerRadius(0.03f))
    }
}

/** Round "30" speed-limit sign on a pole. */
private fun DrawScope.drawZone30Sign(p: DriveProjection, x: Float, y: Float, art: DriveArt) {
    val top = drawPole(p, x, y, 0.9f)
    val s = p.scale(y)
    withTransform({ translate(top.x, top.y); scale(s, s, Offset.Zero) }) {
        drawCircle(DrivePalette.SignRed, 0.24f, Offset.Zero)
        drawCircle(Color.White, 0.18f, Offset.Zero)
        val t = art.thirty
        val q = 0.2f / t.size.height
        withTransform({ scale(q, q, Offset.Zero) }) {
            drawText(t, color = Color(0xFF111827), topLeft = Offset(-t.size.width / 2f, -t.size.height / 2f))
        }
    }
}

/** A bobbing, glinting star with its shadow. */
private fun DrawScope.drawStar(p: DriveProjection, x: Float, y: Float, time: Float) {
    val bob = sin(time * 5f + y * 1.7f)
    atWorld(p, x, y) {
        drawOval(DrivePalette.Shadow, Offset(-0.13f, 0.06f), Size(0.26f, 0.1f))
        withTransform({
            translate(0f, -0.08f - bob * 0.03f)
            rotate(bob * 10f, Offset.Zero)
            scale(0.21f * (1f + bob * 0.06f), 0.21f * (1f + bob * 0.06f), Offset.Zero)
        }) {
            drawPath(STAR_PATH, DrivePalette.StarEdge)
            withTransform({ scale(0.8f, 0.8f, Offset.Zero) }) { drawPath(STAR_PATH, DrivePalette.Star) }
            drawCircle(Color.White.copy(alpha = 0.85f), 0.14f, Offset(-0.18f, -0.22f))
        }
    }
}

/** A tree seen from above: shadow, trunk peeking out and three layers of crown. */
private fun DrawScope.drawTree(p: DriveProjection, x: Float, y: Float, radius: Float) {
    atWorld(p, x, y) {
        drawCircle(DrivePalette.Shadow, radius, Offset(0.08f, 0.1f))
        drawCircle(DrivePalette.TreeDark, radius, Offset.Zero)
        drawCircle(DrivePalette.Tree, radius * 0.78f, Offset(-radius * 0.12f, -radius * 0.12f))
        drawCircle(DrivePalette.TreeLight, radius * 0.35f, Offset(-radius * 0.3f, -radius * 0.32f))
    }
}

/** A round bush dotted with flowers of [flower] colour. */
private fun DrawScope.drawBush(p: DriveProjection, x: Float, y: Float, flower: Color) {
    atWorld(p, x, y) {
        drawCircle(DrivePalette.Shadow, 0.2f, Offset(0.05f, 0.06f))
        drawCircle(DrivePalette.Tree, 0.2f, Offset.Zero)
        drawCircle(DrivePalette.TreeLight, 0.09f, Offset(-0.06f, -0.07f))
        for (i in 0 until 5) {
            val a = i * 2 * PI.toFloat() / 5 + 0.4f
            drawCircle(Color.White, 0.035f, Offset(cos(a) * 0.12f, sin(a) * 0.12f))
            drawCircle(flower, 0.022f, Offset(cos(a) * 0.12f, sin(a) * 0.12f))
        }
    }
}

/** A small house seen from above: walls hidden under a two-tone roof with a ridge. */
private fun DrawScope.drawHouse(p: DriveProjection, x: Float, y: Float, roof: Color) {
    atWorld(p, x, y) {
        drawRoundRect(DrivePalette.Shadow, Offset(-0.62f, -0.5f), Size(1.34f, 1.1f), CornerRadius(0.1f))
        drawRoundRect(roof, Offset(-0.7f, -0.6f), Size(1.4f, 1.1f), CornerRadius(0.1f))
        drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(0f, -0.6f), Size(0.7f, 1.1f), CornerRadius(0.1f))
        drawRect(Color.White.copy(alpha = 0.35f), Offset(-0.03f, -0.6f), Size(0.06f, 1.1f))
        drawRoundRect(Color(0xFFE5E7EB), Offset(-0.5f, -0.45f), Size(0.18f, 0.18f), CornerRadius(0.03f))
    }
}

/** A traffic cone seen from above. */
private fun DrawScope.drawCone(p: DriveProjection, x: Float, y: Float) {
    atWorld(p, x, y) {
        drawCircle(DrivePalette.Shadow, 0.11f, Offset(0.03f, 0.04f))
        drawCircle(DrivePalette.Cone, 0.1f, Offset.Zero)
        drawCircle(Color.White, 0.06f, Offset.Zero)
        drawCircle(DrivePalette.Cone, 0.035f, Offset.Zero)
    }
}

/** Red-and-white barrier across a closed lane, with a blinking amber lamp. */
private fun DrawScope.drawBarrier(p: DriveProjection, left: Float, right: Float, y: Float, time: Float) {
    val width = right - left
    atWorld(p, left, y) {
        drawRect(DrivePalette.Shadow, Offset(0.04f, 0.05f), Size(width, 0.16f))
        drawRect(Color.White, Offset(0f, -0.04f), Size(width, 0.16f))
        var x = 0f
        var red = true
        while (x < width) {
            if (red) drawRect(DrivePalette.SignRed, Offset(x, -0.04f), Size(min(0.14f, width - x), 0.16f))
            x += 0.14f
            red = !red
        }
        val on = sin(time * 9f) > 0f
        drawCircle(if (on) DrivePalette.Amber else Color(0xFF92400E), 0.06f, Offset(width / 2, 0.04f))
        if (on) drawCircle(DrivePalette.Amber.copy(alpha = 0.35f), 0.16f, Offset(width / 2, 0.04f))
    }
}

/** A pedestrian (or child) seen from above, swinging arms while walking, with a "!" when about to cross. */
private fun DrawScope.drawWalker(p: DriveProjection, walker: Walker, art: DriveArt) {
    if (!walker.visible) return
    val k = if (walker.isChild) 1.25f else 1.5f
    val swing = sin(walker.stride) * 0.07f
    val shirt = DrivePalette.shirts[walker.style % DrivePalette.shirts.size]
    atWorld(p, walker.x, walker.y) {
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawOval(DrivePalette.Shadow, Offset(-0.14f, -0.08f), Size(0.32f, 0.24f))
            drawCircle(DrivePalette.skin, 0.045f, Offset(swing, -0.15f))
            drawCircle(DrivePalette.skin, 0.045f, Offset(-swing, 0.15f))
            drawOval(shirt, Offset(-0.1f, -0.16f), Size(0.2f, 0.32f))
            drawCircle(DrivePalette.skin, 0.085f, Offset.Zero)
            drawCircle(DrivePalette.hair[walker.style % DrivePalette.hair.size], 0.07f, Offset(-0.02f, 0f))
        }
        if (walker.alert) {
            drawCircle(Color.White, 0.15f, Offset(0f, -0.5f))
            drawCircle(DrivePalette.Star, 0.13f, Offset(0f, -0.5f))
            val t = art.alert
            val q = 0.2f / t.size.height
            withTransform({ translate(0f, -0.5f); scale(q, q, Offset.Zero) }) {
                drawText(t, color = Color(0xFF111827), topLeft = Offset(-t.size.width / 2f, -t.size.height / 2f))
            }
        }
    }
}

/** A football rolling across the road. */
private fun DrawScope.drawBall(p: DriveProjection, x: Float, y: Float, spin: Float) {
    atWorld(p, x, y) {
        withTransform({ scale(1.6f, 1.6f, Offset.Zero) }) { drawFootball(spin) }
    }
}

/** A football of radius 0.1 at the origin, rotated by [spin] degrees. */
private fun DrawScope.drawFootball(spin: Float) {
    run {
        drawCircle(DrivePalette.Shadow, 0.1f, Offset(0.03f, 0.04f))
        drawCircle(Color.White, 0.1f, Offset.Zero)
        withTransform({ rotate(spin, Offset.Zero) }) {
            drawCircle(Color(0xFF111827), 0.035f, Offset.Zero)
            for (i in 0 until 5) {
                val a = i * 2 * PI.toFloat() / 5
                drawCircle(Color(0xFF111827), 0.022f, Offset(cos(a) * 0.075f, sin(a) * 0.075f))
            }
        }
    }
}

/** Grey pole rising [height] lanes from world ([x], [y]); returns the screen top of the pole. */
private fun DrawScope.drawPole(p: DriveProjection, x: Float, y: Float, height: Float): Offset {
    val s = p.scale(y)
    val base = Offset(p.sx(x, y), p.sy(y))
    val top = Offset(base.x, base.y - height * s)
    drawOval(DrivePalette.Shadow, Offset(base.x - 0.08f * s, base.y - 0.03f * s), Size(0.2f * s, 0.07f * s))
    drawLine(Color(0xFF6B7280), base, top, strokeWidth = 0.05f * s)
    return top
}

/** Octagonal STOP sign on a pole. */
private fun DrawScope.drawStopSign(p: DriveProjection, x: Float, y: Float, art: DriveArt) {
    val top = drawPole(p, x, y, 0.95f)
    val s = p.scale(y)
    withTransform({ translate(top.x, top.y); scale(s, s, Offset.Zero) }) {
        withTransform({ scale(0.3f, 0.3f, Offset.Zero) }) { drawPath(OCTAGON_PATH, Color.White) }
        withTransform({ scale(0.26f, 0.26f, Offset.Zero) }) { drawPath(OCTAGON_PATH, DrivePalette.SignRed) }
        val t = art.stop
        val q = 0.4f / t.size.width
        withTransform({ scale(q, q, Offset.Zero) }) {
            drawText(t, color = Color.White, topLeft = Offset(-t.size.width / 2f, -t.size.height / 2f))
        }
    }
}

/** Traffic light on a pole with the lit lamp glowing. */
private fun DrawScope.drawTrafficLight(p: DriveProjection, x: Float, y: Float, light: LightColor) {
    val top = drawPole(p, x, y, 0.75f)
    val s = p.scale(y)
    withTransform({ translate(top.x, top.y); scale(s, s, Offset.Zero) }) {
        drawRoundRect(Color(0xFF111827), Offset(-0.17f, -0.62f), Size(0.34f, 0.84f), CornerRadius(0.1f))
        val lamps = listOf(LightColor.RED to DrivePalette.BrakeLight, LightColor.AMBER to DrivePalette.Amber, LightColor.GREEN to DrivePalette.Green)
        lamps.forEachIndexed { index, (color, tint) ->
            val center = Offset(0f, -0.47f + index * 0.27f)
            if (color == light) {
                drawCircle(tint.copy(alpha = 0.35f), 0.22f, center)
                drawCircle(tint, 0.1f, center)
                drawCircle(Color.White.copy(alpha = 0.6f), 0.035f, center + Offset(-0.03f, -0.03f))
            } else {
                drawCircle(tint.copy(alpha = 0.18f), 0.1f, center)
            }
        }
    }
}

/** Blue pedestrian-crossing sign on a pole. */
private fun DrawScope.drawCrosswalkSign(p: DriveProjection, x: Float, y: Float) {
    val top = drawPole(p, x, y, 0.85f)
    val s = p.scale(y)
    withTransform({ translate(top.x, top.y); scale(s, s, Offset.Zero) }) {
        drawRoundRect(Color.White, Offset(-0.2f, -0.2f), Size(0.4f, 0.4f), CornerRadius(0.05f))
        drawRoundRect(DrivePalette.SignBlue, Offset(-0.17f, -0.17f), Size(0.34f, 0.34f), CornerRadius(0.04f))
        val triangle = Path().apply { moveTo(0f, -0.13f); lineTo(0.14f, 0.12f); lineTo(-0.14f, 0.12f); close() }
        drawPath(triangle, Color.White)
        drawCircle(Color(0xFF111827), 0.022f, Offset(0f, -0.03f))
        drawLine(Color(0xFF111827), Offset(0f, -0.01f), Offset(0f, 0.06f), strokeWidth = 0.025f)
        drawLine(Color(0xFF111827), Offset(0f, 0.06f), Offset(-0.035f, 0.1f), strokeWidth = 0.02f)
        drawLine(Color(0xFF111827), Offset(0f, 0.06f), Offset(0.035f, 0.1f), strokeWidth = 0.02f)
    }
}

/** Triangular roadworks warning sign on a pole. */
private fun DrawScope.drawWorksSign(p: DriveProjection, x: Float, y: Float) {
    val top = drawPole(p, x, y, 0.85f)
    val s = p.scale(y)
    withTransform({ translate(top.x, top.y); scale(s, s, Offset.Zero) }) {
        val outer = Path().apply { moveTo(0f, -0.24f); lineTo(0.24f, 0.17f); lineTo(-0.24f, 0.17f); close() }
        val inner = Path().apply { moveTo(0f, -0.15f); lineTo(0.16f, 0.12f); lineTo(-0.16f, 0.12f); close() }
        drawPath(outer, DrivePalette.SignRed)
        drawPath(inner, Color(0xFFFDE68A))
        drawCircle(Color(0xFF111827), 0.022f, Offset(0.02f, -0.04f))
        drawLine(Color(0xFF111827), Offset(0.01f, -0.02f), Offset(-0.03f, 0.06f), strokeWidth = 0.025f)
        drawLine(Color(0xFF111827), Offset(-0.07f, 0.09f), Offset(0.08f, 0.09f), strokeWidth = 0.03f)
    }
}

/** Finish arch spanning the avenue with a chequered "META" banner. */
private fun DrawScope.drawFinishArch(p: DriveProjection, y: Float, art: DriveArt, time: Float) {
    val left = drawPole(p, -0.3f, y, 1.25f)
    val right = drawPole(p, PARKING_EDGE + 0.3f, y, 1.25f)
    val s = p.scale(y)
    val height = 0.42f * s
    val bannerTop = left.y - height * 0.2f
    drawRoundRect(Color(0xFF111827), Offset(left.x, bannerTop), Size(right.x - left.x, height), CornerRadius(0.08f * s))
    val cell = height / 2
    var x = left.x
    var column = 0
    while (x < right.x - 0.5f) {
        for (row in 0 until 2) {
            if ((row + column) % 2 == 0) {
                drawRect(Color.White, Offset(x, bannerTop + row * cell), Size(min(cell, right.x - x), cell))
            }
        }
        x += cell
        column++
    }
    val t = art.finish
    val q = height * 0.8f / t.size.height
    val pulse = 1f + sin(time * 6f) * 0.04f
    withTransform({
        translate((left.x + right.x) / 2, bannerTop + height / 2)
        scale(q * pulse, q * pulse, Offset.Zero)
    }) {
        drawRoundRect(DrivePalette.Player, Offset(-t.size.width * 0.62f, -t.size.height * 0.55f), Size(t.size.width * 1.24f, t.size.height * 1.1f), CornerRadius(t.size.height * 0.3f))
        drawText(t, color = Color.White, topLeft = Offset(-t.size.width / 2f, -t.size.height / 2f))
    }
}

/** Confetti, success rings and nothing else: the screen-space flash is drawn by the screen. */
private fun DrawScope.drawEffects(fx: DriveFx, p: DriveProjection) {
    for (ring in fx.rings) {
        val progress = ring.age / 0.6f
        val s = p.scale(ring.y)
        drawCircle(
            ring.color.copy(alpha = (1f - progress) * 0.8f),
            radius = (0.4f + progress * 1.1f) * s,
            center = Offset(p.sx(ring.x, ring.y), p.sy(ring.y)),
            style = Stroke(width = 0.08f * s * (1f - progress) + 1f),
        )
    }
    for (particle in fx.particles) {
        val s = p.scale(particle.y)
        val center = Offset(p.sx(particle.x, particle.y), p.sy(particle.y) - particle.z * s)
        val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
        val size = particle.size * s
        if (particle.round) {
            drawCircle(particle.color.copy(alpha = alpha), size, center)
        } else {
            withTransform({ rotate(particle.life * 540f, center) }) {
                drawRect(particle.color.copy(alpha = alpha), center - Offset(size, size / 2), Size(size * 2, size))
            }
        }
    }
}
