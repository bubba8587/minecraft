# dte:B5,B6
"""Writes the solid-colour 16x16 placeholder textures for refmod (internal mod, never published). No PIL needed."""
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/refmod/textures"
TEXTURES = {
    "block/signal_lamp": (90, 70, 30),
    "block/signal_lamp_on": (255, 220, 80),
    "block/counter_block": (110, 110, 130),
    "item/tally_stick": (150, 110, 60),
    "item/flint_hatchet": (60, 60, 70),
    "item/pebble": (140, 140, 140),
}


def png(rgb, size=16):
    raw = b"".join(b"\x00" + bytes(rgb) * size for _ in range(size))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


for name, rgb in TEXTURES.items():
    out = ROOT / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(png(rgb))
    print(out)
