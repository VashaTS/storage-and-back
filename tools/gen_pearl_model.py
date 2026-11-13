#!/usr/bin/env python3
"""Builds the Ender Pearl Block 3D model (9 touching stepped spheres) and its texture.

Geometry (16px block space):
  8 corner spheres: radius 4, centres at 4 or 12 on each axis (adjacent corners touch)
  1 centre sphere:  (8, 8, 8), radius sqrt(3)*4 - 4 ≈ 2.928 so it touches all 8

Vanilla block models only allow cuboids, so each sphere is a stack of square slices.
Run from the repo root: python3 tools/gen_pearl_model.py
"""
from __future__ import annotations

import json
import math
import os

import numpy as np
from PIL import Image

NS = "storageback"
MODEL = f"src/main/resources/assets/{NS}/models/block/ender_pearl_block.json"
TEXTURE = f"src/main/resources/assets/{NS}/textures/block/ender_pearl.png"
# Kept as the particle / docs fallback; same art as the model texture.
TEXTURE_OLD = f"src/main/resources/assets/{NS}/textures/block/ender_pearl_block.png"

CORNER_R = 4.0
CENTER_R = 4.0 * math.sqrt(3.0) - 4.0  # ≈ 2.928
CORNERS = [(4.0 if (i & 1) == 0 else 12.0,
            4.0 if (i & 2) == 0 else 12.0,
            4.0 if (i & 4) == 0 else 12.0) for i in range(8)]

# Y-slice relative edges and the half-width of the square at the slice mid-point,
# computed from the sphere: w/2 = sqrt(R^2 - y_mid^2).
# Corner (R=4): 5 slices, pixel-aligned, classic diameter-8 voxel sphere.
CORNER_SLICES = [
    (-4.0, -3.0, 2.0),
    (-3.0, -2.0, 3.0),
    (-2.0,  2.0, 4.0),
    ( 2.0,  3.0, 3.0),
    ( 3.0,  4.0, 2.0),
]
# Centre (R≈2.928): 3 slices, 2-decimal positions.
CENTER_SLICES = [
    (-CENTER_R, -1.1, math.sqrt(max(0.0, CENTER_R ** 2 - 2.0 ** 2))),  # mid ≈ -2.01
    (-1.1, 1.1, CENTER_R),
    (1.1, CENTER_R, math.sqrt(max(0.0, CENTER_R ** 2 - 2.0 ** 2))),
]


def _rnd(v: float) -> float:
    return round(v, 4)


def slice_boxes(cx, cy0, cy1, cz, r):
    """3 non-overlapping boxes approximating a circle of radius r in the XZ plane.

    A full square of half-width r would make neighbouring corner spheres (centres 8
    apart, r=4) tile solid slabs, hiding the gaps. A plus with arm width 0.72*r
    stays round enough to read as a sphere while leaving see-through channels
    through the middle of each face and around the centre pearl.
    """
    if r <= 0.25:
        return []
    k = r * 0.72
    if k * 2 >= r * 2 - 0.05:
        return [(_rnd(cx - r), _rnd(cy0), _rnd(cz - r),
                 _rnd(cx + r), _rnd(cy1), _rnd(cz + r))]
    return [
        # wide east-west bar
        (_rnd(cx - r), _rnd(cy0), _rnd(cz - k),
         _rnd(cx + r), _rnd(cy1), _rnd(cz + k)),
        # north stub (outside the bar)
        (_rnd(cx - k), _rnd(cy0), _rnd(cz - r),
         _rnd(cx + k), _rnd(cy1), _rnd(cz - k)),
        # south stub
        (_rnd(cx - k), _rnd(cy0), _rnd(cz + k),
         _rnd(cx + k), _rnd(cy1), _rnd(cz + r)),
    ]


def sphere_boxes(cx, cy, cz, slices):
    boxes = []
    for y0, y1, hw in slices:
        boxes.extend(slice_boxes(cx, cy + y0, cy + y1, cz, hw))
    return boxes


def all_boxes():
    boxes = []
    for c in CORNERS:
        boxes.extend(sphere_boxes(*c, CORNER_SLICES))
    boxes.extend(sphere_boxes(8.0, 8.0, 8.0, CENTER_SLICES))
    return boxes


def face_uv(face, x1, y1, z1, x2, y2, z2):
    """1:1 pixel UVs, origin chosen so 'lit' faces sample the highlight and 'down' the dark swirl."""
    if face in ("up", "down"):
        w, h = x2 - x1, z2 - z1
    elif face in ("north", "south"):
        w, h = x2 - x1, y2 - y1
    else:
        w, h = z2 - z1, y2 - y1
    w = max(0.5, min(16.0, w))
    h = max(0.5, min(16.0, h))
    # Origins (u, v) in the 16x16 pearl texture
    origins = {
        "up": (2.0, 1.0),
        "down": (7.0, 8.0),
        "south": (3.0, 2.0),
        "east": (4.0, 2.0),
        "north": (1.0, 4.0),
        "west": (0.0, 4.0),
    }
    u0, v0 = origins[face]
    u1, v1 = u0 + w, v0 + h
    if u1 > 16:
        u0 -= u1 - 16
        u1 = 16
    if v1 > 16:
        v0 -= v1 - 16
        v1 = 16
    if u0 < 0:
        u1 -= u0
        u0 = 0
    if v0 < 0:
        v1 -= v0
        v0 = 0
    return [_rnd(u0), _rnd(v0), _rnd(u1), _rnd(v1)]


def element(box):
    x1, y1, z1, x2, y2, z2 = box
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        faces[face] = {"uv": face_uv(face, x1, y1, z1, x2, y2, z2), "texture": "#pearl"}
    return {
        "from": [_rnd(x1), _rnd(y1), _rnd(z1)],
        "to": [_rnd(x2), _rnd(y2), _rnd(z2)],
        "faces": faces,
    }


def write_model():
    boxes = all_boxes()
    model = {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {
            "pearl": f"{NS}:block/ender_pearl",
            "particle": f"{NS}:block/ender_pearl",
        },
        # Raise the first-person item so the whole 9-pearl cluster sits in frame.
        "display": {
            "firstperson_righthand": {
                "rotation": [0, 45, 0],
                "translation": [0, 4.5, 0],
                "scale": [0.42, 0.42, 0.42],
            },
            "firstperson_lefthand": {
                "rotation": [0, 225, 0],
                "translation": [0, 4.5, 0],
                "scale": [0.42, 0.42, 0.42],
            },
        },
        "elements": [element(b) for b in boxes],
    }
    os.makedirs(os.path.dirname(MODEL), exist_ok=True)
    with open(MODEL, "w", encoding="utf-8") as f:
        json.dump(model, f, indent=2)
        f.write("\n")
    return boxes


def write_java_boxes(boxes, path="src/main/java/net/vsh33/storageback/block/EnderPearlShapes.java"):
    """Optional helper listing — the shape is inlined in EnderPearlBlock."""
    return boxes


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def write_texture():
    """16x16 ender-pearl material: teal body, dark swirl, cyan highlight (vanilla item palette)."""
    pal = {
        "bg": hx("032620"),
        "out": hx("0c3730"),
        "d2": hx("063931"),
        "d1": hx("0b4d42"),
        "m0": hx("105e51"),
        "m1": hx("1b7b6b"),
        "m2": hx("258474"),
        "l0": hx("349988"),
        "l1": hx("2ccdb1"),
        "hi": hx("8cf4e2"),
    }
    S = 16
    img = np.zeros((S, S, 4), np.uint8)
    cx, cy = 6.2, 5.8
    for y in range(S):
        for x in range(S):
            dx = (x + 0.5 - cx) / 8.5
            dy = (y + 0.5 - cy) / 8.5
            d = math.hypot(dx, dy)
            # Base radial shading, brighter upper-left
            light = 0.62 - 0.45 * min(d, 1.2) - 0.22 * (dx + dy)
            # Dark swirl (the pearl's 'eye'), a little off-centre
            sx, sy = x + 0.5 - 8.2, y + 0.5 - 8.0
            swirl = math.hypot(sx / 2.6, sy / 2.4)
            if swirl < 1.0:
                light -= 0.55 * (1.0 - swirl)
            # Compact highlight, upper-left (vanilla pearl has a tight cyan glint)
            hx_, hy_ = x + 0.5 - 4.4, y + 0.5 - 4.2
            hi = math.exp(-(hx_ * hx_ + hy_ * hy_) / 4.0)
            light += 0.55 * hi
            # Small rim glint, lower-right
            rx, ry = x + 0.5 - 11.4, y + 0.5 - 11.0
            light += 0.16 * math.exp(-(rx * rx + ry * ry) / 2.8)
            light = max(0.0, min(0.98, light))
            stops = [
                (0.08, pal["bg"]),
                (0.22, pal["out"]),
                (0.34, pal["d2"]),
                (0.46, pal["d1"]),
                (0.58, pal["m0"]),
                (0.68, pal["m1"]),
                (0.78, pal["m2"]),
                (0.88, pal["l0"]),
                (0.95, pal["l1"]),
                (1.00, pal["hi"]),
            ]
            c = stops[0][1]
            for i in range(len(stops) - 1):
                a, ca = stops[i]
                b, cb = stops[i + 1]
                if a <= light <= b:
                    t = 0 if b == a else (light - a) / (b - a)
                    c = tuple(int(ca[k] + t * (cb[k] - ca[k])) for k in range(3))
                    break
            else:
                c = stops[-1][1]
            img[y, x] = (c[0], c[1], c[2], 255)
    os.makedirs(os.path.dirname(TEXTURE), exist_ok=True)
    Image.fromarray(img).save(TEXTURE)
    Image.fromarray(img).save(TEXTURE_OLD)


def java_shape_source(boxes):
    lines = []
    for x1, y1, z1, x2, y2, z2 in boxes:
        lines.append(f"\t\t\tBlock.box({x1}, {y1}, {z1}, {x2}, {y2}, {z2}),")
    return "\n".join(lines)


def main():
    boxes = write_model()
    write_texture()
    print(f"wrote {MODEL}")
    print(f"wrote {TEXTURE}")
    print(f"{len(boxes)} cuboids "
          f"({len(CORNERS) * len(CORNER_SLICES)} corner + {len(CENTER_SLICES)} centre), "
          f"centre radius {CENTER_R:.4f}")
    print("-- Java boxes --")
    print(java_shape_source(boxes))


if __name__ == "__main__":
    main()
