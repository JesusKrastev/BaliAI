"""Generates the answer-feedback sound effects shipped in app/src/main/res/raw.

The sounds are synthesised here from plain sine/triangle waves, so they are original work: no
sample, library or third-party recording is involved and nothing has to be attributed. They are
released as CC0 1.0 (public domain) together with this script.

    sfx_correct.ogg          two rising notes (C6 -> G6), a short bright chime
    sfx_wrong.ogg            two falling notes (A3 -> F3), soft and low, never harsh
    sfx_lesson_complete.ogg  a C major arpeggio (C5 E5 G5) landing on a held C6 chord: the
                             result screen of a test, mini-game or exam worth celebrating
    sfx_soft_finish.ogg      two quiet, round notes rising a fourth (G4 -> C5): the result screen
                             of a failed exam or a low score, a calm "done" with no fanfare

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


def correct():
    """Bright rising chime: C6 then G6 (a perfect fifth up) with soft upper partials."""
    bright = [(1, 1.0), (2, 0.30), (3, 0.10)]
    return mix(
        [
            (0.00, note(1046.50, 0.30, bright, decay=0.10)),
            (0.09, note(1567.98, 0.46, bright, decay=0.16)),
        ],
        total_length=0.55,
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
    # Quieter than the others: it closes a result that is not being celebrated.
    write_ogg(soft_finish(), "sfx_soft_finish", peak=0.40)
