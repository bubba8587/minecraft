# dte:B16
"""Writes solid-colour PLACEHOLDER textures (16x and their 32x twins) until people draw the real ones.
Never replace these with AI art (specs/textures.md). No PIL needed."""
import struct
import zlib
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
MOD = "bubbas_reworked_mobs"
TEXTURES = {
    "item/skeleton_sleeper": (220, 220, 205),
    "item/zombie_sleeper": (70, 120, 60),
}


def png(rgb, size):
    raw = b"".join(b"\x00" + bytes(rgb) * size for _ in range(size))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


for name, rgb in TEXTURES.items():
    for base, size in ((RES / f"assets/{MOD}/textures", 16), (RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures", 32)):
        out = base / f"{name}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(png(rgb, size))
        print(out)
