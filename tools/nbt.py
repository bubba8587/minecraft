#!/usr/bin/env python3
# dte:A3
"""Tiny NBT reader/writer (Java Edition, big-endian, gzipped files like level.dat). Standard library only.

Tags are kept typed so a file round-trips byte for byte: every value is a Tag(type_id, value), compounds are
dicts of name -> Tag (insertion order kept), lists are (element_type, [Tag...]).

  python tools/nbt.py FILE [PATH.TO.TAG]     print a file (or one tag) as indented text, for looking inside
"""
import gzip, struct, sys
from typing import NamedTuple

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)
_NUM = {BYTE: ">b", SHORT: ">h", INT: ">i", LONG: ">q", FLOAT: ">f", DOUBLE: ">d"}
_ARR = {BYTE_ARRAY: ">b", INT_ARRAY: ">i", LONG_ARRAY: ">q"}


class Tag(NamedTuple):
    type: int
    value: object


def string(s): return Tag(STRING, s)
def byte(b): return Tag(BYTE, int(b))
def int_(i): return Tag(INT, int(i))


class _Reader:
    def __init__(self, data):
        self.d, self.i = data, 0

    def take(self, n):
        b = self.d[self.i:self.i + n]
        self.i += n
        return b

    def unpack(self, fmt):
        return struct.unpack(fmt, self.take(struct.calcsize(fmt)))[0]

    def str(self):
        return self.take(self.unpack(">H")).decode("utf-8", "surrogatepass")

    def payload(self, t):
        if t in _NUM:
            return self.unpack(_NUM[t])
        if t in _ARR:
            n = self.unpack(">i")
            return list(struct.unpack(">%d%s" % (n, _ARR[t][1]), self.take(n * struct.calcsize(_ARR[t]))))
        if t == STRING:
            return self.str()
        if t == LIST:
            et, n = self.unpack(">b"), self.unpack(">i")
            return (et, [Tag(et, self.payload(et)) for _ in range(n)])
        if t == COMPOUND:
            out = {}
            while True:
                ct = self.unpack(">b")
                if ct == END:
                    return out
                name = self.str()
                out[name] = Tag(ct, self.payload(ct))
        raise ValueError(f"bad tag type {t}")


def _w_str(out, s):
    b = s.encode("utf-8", "surrogatepass")
    out += struct.pack(">H", len(b)) + b


def _w_payload(out, t, v):
    if t in _NUM:
        out += struct.pack(_NUM[t], v)
    elif t in _ARR:
        out += struct.pack(">i", len(v)) + struct.pack(">%d%s" % (len(v), _ARR[t][1]), *v)
    elif t == STRING:
        _w_str(out, v)
    elif t == LIST:
        et, items = v
        out += struct.pack(">bi", et if items or et else END, len(items))
        for it in items:
            _w_payload(out, et, it.value)
    elif t == COMPOUND:
        for name, tag in v.items():
            out += struct.pack(">b", tag.type)
            _w_str(out, name)
            _w_payload(out, tag.type, tag.value)
        out += b"\x00"
    else:
        raise ValueError(f"bad tag type {t}")


def loads(data):
    """Parse uncompressed NBT bytes; return (root_name, root Tag)."""
    r = _Reader(data)
    t = r.unpack(">b")
    name = r.str()
    return name, Tag(t, r.payload(t))


def dumps(name, tag):
    out = bytearray()
    out += struct.pack(">b", tag.type)
    _w_str(out, name)
    _w_payload(out, tag.type, tag.value)
    return bytes(out)


def read_file(path):
    with open(path, "rb") as f:
        data = f.read()
    return loads(gzip.decompress(data) if data[:2] == b"\x1f\x8b" else data)


def write_file(path, name, tag):
    # mtime=0 keeps the file identical for identical content (smaller git diffs).
    with open(path, "wb") as f:
        f.write(gzip.compress(dumps(name, tag), mtime=0))


def show(tag, indent=0, name=""):
    pad = "  " * indent
    label = f"{name}: " if name else ""
    if tag.type == COMPOUND:
        print(f"{pad}{label}{{")
        for k, v in tag.value.items():
            show(v, indent + 1, k)
        print(f"{pad}}}")
    elif tag.type == LIST:
        et, items = tag.value
        print(f"{pad}{label}[  # {len(items)}")
        for v in items:
            show(v, indent + 1)
        print(f"{pad}]")
    elif tag.type in _ARR:
        print(f"{pad}{label}<array of {len(tag.value)}>")
    else:
        print(f"{pad}{label}{tag.value!r}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    _, root = read_file(sys.argv[1])
    for part in (sys.argv[2].split(".") if len(sys.argv) > 2 else []):
        root = root.value[part]
    show(root)
