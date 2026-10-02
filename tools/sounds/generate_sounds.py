"""Generates the answer-feedback sound effects shipped in app/src/main/res/raw.

The sounds are synthesised here from plain sine/triangle waves, so they are original work: no
sample, library or third-party recording is involved and nothing has to be attributed. They are
released as CC0 1.0 (public domain) together with this script.

    sfx_correct.ogg  two rising notes (C6 -> G6), a short bright chime
    sfx_wrong.ogg    two falling notes (A3 -> F3), soft and low, never harsh

Usage (needs numpy and ffmpeg on the PATH):

    python tools/sounds/generate_sounds.py

Output is mono OGG Vorbis, a few KB each. Re-run it after changing a parameter and commit the
resulting files; the app only reads the .ogg files.
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


def write_ogg(samples, name):
    """Normalises [samples] to PEAK and encodes it as mono OGG Vorbis at OUT_DIR/<name>.ogg."""
    samples = samples / np.max(np.abs(samples)) * PEAK
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
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav_path),
             "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", str(out_path)],
            check=True,
        )
    print(f"{out_path.name}: {out_path.stat().st_size} bytes, {len(samples) / SAMPLE_RATE:.2f} s")


if __name__ == "__main__":
    write_ogg(correct(), "sfx_correct")
    write_ogg(wrong(), "sfx_wrong")
