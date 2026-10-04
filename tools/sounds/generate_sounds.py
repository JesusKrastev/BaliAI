"""Generates the answer-feedback sound effects shipped in app/src/main/res/raw.

The sounds are synthesised here from plain sine/triangle waves, so they are original work: no
sample, library or third-party recording is involved and nothing has to be attributed. They are
released as CC0 1.0 (public domain) together with this script.

    sfx_correct.ogg          three quick bell notes rising (C6 -> E6 -> G6) with a sparkle on top:
                             a glassy "ting-ting-ting" that rewards a right answer
    sfx_wrong.ogg            two falling notes (A3 -> F3), soft and low, never harsh
    sfx_lesson_complete.ogg  a C major arpeggio (C5 E5 G5) landing on a held C6 chord: the
                             result screen of a test, mini-game or exam worth celebrating
    sfx_chest_open.ogg       a wooden creak of the lid, then a magic shimmer climbing a pentatonic
                             scale and ringing out on a high chord: a surprise chest opening
    sfx_soft_finish.ogg      two quiet, round notes rising a fourth (G4 -> C5): the result screen
                             of a failed exam or a low score, a calm "done" with no fanfare
    sfx_pickup.ogg           a tiny two-note "pling" (E6 -> B6): a star collected in Bali Drive.
                             The app replays it at a rising playback rate for consecutive stars,
                             so a streak climbs in pitch

Usage (needs numpy and ffmpeg on the PATH):

    python tools/sounds/generate_sounds.py

Output is mono OGG Vorbis, a few KB each. Re-run it after changing a parameter and commit the
resulting files; the app only reads the .ogg files. The encoder runs in bit-exact mode, so a
re-run without changes rewrites byte-identical files and leaves git clean.
"""

import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

SAMPLE_RATE = 44_100
PEAK = 0.55  # linear peak after normalisation; leaves headroom so the sounds are never loud
OUT_DIR = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "res" / "raw"


def note(freq, length, harmonics, decay, attack=0.006):
    """Returns one plucked note.

    Args:
        freq: fundamental frequency in Hz.
        length: duration in seconds.
        harmonics: list of (multiple, amplitude) pairs added on top of the fundamental.
        decay: exponential decay time constant in seconds (smaller = shorter note).
        attack: linear fade-in in seconds, so the note never starts with a click.
    """
    t = np.arange(int(length * SAMPLE_RATE)) / SAMPLE_RATE
    wave_ = np.zeros_like(t)
    for multiple, amplitude in harmonics:
        wave_ += amplitude * np.sin(2 * np.pi * freq * multiple * t)
    envelope = np.exp(-t / decay) * np.minimum(1.0, t / attack)
    fade_out = np.minimum(1.0, (length - t) / 0.02)  # last 20 ms to zero, no click at the end
    return wave_ * envelope * fade_out


def mix(notes_at, total_length):
    """Sums [(start_seconds, samples), ...] into one buffer of total_length seconds."""
    out = np.zeros(int(total_length * SAMPLE_RATE))
    for start, samples in notes_at:
        begin = int(start * SAMPLE_RATE)
        out[begin:begin + len(samples)] += samples[: len(out) - begin]
    return out


def bell(freq, length, decay, attack=0.003):
    """Glassy bell note: inharmonic upper partials ring and die faster than the fundamental."""
    partials = [(1.0, 1.0, 1.0), (2.76, 0.38, 0.55), (5.40, 0.16, 0.30), (8.93, 0.06, 0.18)]
    t = np.arange(int(length * SAMPLE_RATE)) / SAMPLE_RATE
    out = np.zeros_like(t)
    for multiple, amplitude, speed in partials:
        out += amplitude * np.sin(2 * np.pi * freq * multiple * t) * np.exp(-t / (decay * speed))
    out *= np.minimum(1.0, t / attack) * np.minimum(1.0, (length - t) / 0.02)
    return out


def correct():
    """Glassy rising "ting-ting-ting": C6, E6, G6 fifty-five milliseconds apart, the last one
    ringing longest with a quiet G7 sparkle over it. Higher and quicker than lesson_complete()."""
    return mix(
        [
            (0.000, bell(1046.50, 0.30, decay=0.10)),
            (0.055, bell(1318.51, 0.32, decay=0.11)),
            (0.110, bell(1567.98, 0.55, decay=0.20)),
            (0.110, 0.22 * bell(3135.96, 0.40, decay=0.10)),
        ],
        total_length=0.66,
    )


def wrong():
    """Soft falling pair: A3 then F3. Odd harmonics only (triangle-like) keep it round, not buzzy."""
    round_ = [(1, 1.0), (3, 0.11), (5, 0.04)]
    return mix(
        [
            (0.00, note(220.00, 0.22, round_, decay=0.10)),
            (0.13, note(174.61, 0.34, round_, decay=0.14)),
        ],
        total_length=0.48,
    )


def lesson_complete():
    """Cheerful C major arpeggio, C5 E5 G5, landing on C6 with a quiet E6 and G6 ringing above it.

    Same bright timbre as correct(), one octave lower to start, so it sounds like the chime
    grown into a short fanfare rather than a different instrument.
    """
    bright = [(1, 1.0), (2, 0.30), (3, 0.10)]
    landing = 0.21
    return mix(
        [
            (0.00, note(523.25, 0.24, bright, decay=0.09)),
            (0.07, note(659.25, 0.24, bright, decay=0.09)),
            (0.14, note(783.99, 0.26, bright, decay=0.10)),
            (landing, note(1046.50, 0.85, bright, decay=0.30)),
            (landing, 0.35 * note(1318.51, 0.80, bright, decay=0.26)),
            (landing, 0.25 * note(1567.98, 0.75, bright, decay=0.22)),
        ],
        total_length=1.10,
    )


def chest_open():
    """Surprise chest: a wooden creak of the lid, a soft thump as it gives, then a shimmer climbing
    a C major pentatonic (C5 D5 E5 G5 A5 C6 E6) that lands on a ringing high chord.

    Total length is about 1.5 s, close to the lid animation (frames 1-37), so the shimmer arrives
    as the glow rises out of the chest.
    """
    t = np.arange(int(0.50 * SAMPLE_RATE)) / SAMPLE_RATE
    rng = np.random.default_rng(7)  # fixed seed: the file is reproducible
    # Stick-slip creak: a sawtooth gliding up while a ~26 Hz pulse chops it, plus a little noise.
    freq = 82.0 + 70.0 * (t / t[-1]) + 6.0 * np.sin(2 * np.pi * 9.0 * t)
    phase = np.cumsum(freq) / SAMPLE_RATE
    saw = 2.0 * (phase % 1.0) - 1.0
    chop = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 26.0 * t)) * np.sin(2 * np.pi * 3.0 * t)
    raw = saw * chop + 0.25 * rng.standard_normal(len(t))
    kernel = np.ones(14) / 14  # crude low-pass: keeps it woody, not buzzy
    creak = np.convolve(raw, kernel, mode="same")
    creak *= np.minimum(1.0, t / 0.05) * np.minimum(1.0, (t[-1] - t) / 0.12)
    creak *= 0.55

    thump = note(70.0, 0.20, [(1, 1.0), (2, 0.25)], decay=0.05)

    scale = [523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1318.51]
    shimmer = [
        (0.52 + 0.075 * i, 0.8 * bell(f, 0.40, decay=0.12)) for i, f in enumerate(scale)
    ]
    landing = 0.52 + 0.075 * len(scale)
    shimmer += [
        (landing, bell(1046.50, 0.95, decay=0.34)),
        (landing, 0.6 * bell(1567.98, 0.90, decay=0.30)),
        (landing, 0.4 * bell(2093.00, 0.85, decay=0.26)),
    ]
    return mix([(0.0, creak), (0.46, 0.9 * thump)] + shimmer, total_length=1.55)


def soft_finish():
    """Calm close: G4 then C5, round and quiet. A rising fourth resolves, so it sounds finished
    without sounding like a reward (no arpeggio) or a verdict (no falling notes, unlike wrong())."""
    round_ = [(1, 1.0), (2, 0.08), (3, 0.06)]
    return mix(
        [
            (0.00, note(392.00, 0.34, round_, decay=0.14, attack=0.015)),
            (0.16, note(523.25, 0.62, round_, decay=0.24, attack=0.015)),
        ],
        total_length=0.80,
    )


def pickup():
    """Tiny bright "pling": E6 then B6 thirty milliseconds apart, short enough to fire several
    times a second. Bali Drive raises its playback rate step by step during a star streak."""
    return mix(
        [
            (0.000, bell(1318.51, 0.12, decay=0.05)),
            (0.030, bell(1975.53, 0.20, decay=0.08)),
        ],
        total_length=0.24,
    )


def write_ogg(samples, name, peak=PEAK):
    """Normalises [samples] to [peak] and encodes it as mono OGG Vorbis at OUT_DIR/<name>.ogg."""
    samples = samples / np.max(np.abs(samples)) * peak
    pcm = (samples * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav_path), "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(SAMPLE_RATE)
            wav.writeframes(pcm.tobytes())
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        out_path = OUT_DIR / f"{name}.ogg"
        subprocess.run(
            # bitexact: fixed Ogg stream serial and no encoder tag, so output is reproducible
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav_path),
             "-ac", "1", "-c:a", "libvorbis", "-q:a", "4",
             "-fflags", "+bitexact", "-flags:a", "+bitexact", str(out_path)],
            check=True,
        )
    print(f"{out_path.name}: {out_path.stat().st_size} bytes, {len(samples) / SAMPLE_RATE:.2f} s")


if __name__ == "__main__":
    write_ogg(correct(), "sfx_correct")
    write_ogg(wrong(), "sfx_wrong")
    write_ogg(lesson_complete(), "sfx_lesson_complete")
    write_ogg(chest_open(), "sfx_chest_open")
    # Quieter than the others: it closes a result that is not being celebrated.
    write_ogg(soft_finish(), "sfx_soft_finish", peak=0.40)
    # Quieter too: it can fire several times a second during a star streak.
    write_ogg(pickup(), "sfx_pickup", peak=0.38)
