#!/usr/bin/env python3
"""Procedural 16x16 block textures for Storage & Back (original pixel art, colours picked to match vanilla items).
Run from the repository root: python3 tools/gen_textures.py"""
import math, os, random
from PIL import Image

OUT = "src/main/resources/assets/storageback/textures/block"
S = 16


def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


class Tex:
    def __init__(self, fill=(0, 0, 0)):
        self.p = [[fill for _ in range(S)] for _ in range(S)]

    def set(self, x, y, c):
        self.p[y % S][x % S] = c

    def get(self, x, y):
        return self.p[y % S][x % S]

    def save(self, name):
        im = Image.new("RGBA", (S, S))
        for y in range(S):
            for x in range(S):
                im.putpixel((x, y), self.p[y][x] + (255,))
        os.makedirs(OUT, exist_ok=True)
        im.save(f"{OUT}/{name}.png")
        return im


def value_noise(seed, cell=4):
    """Tileable smooth noise in 0..1."""
    rnd = random.Random(seed)
    n = S // cell
    g = [[rnd.random() for _ in range(n)] for _ in range(n)]

    def f(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx), int(gy)
        tx, ty = gx - x0, gy - y0
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0 % n][x0 % n]; b = g[y0 % n][(x0 + 1) % n]
        c = g[(y0 + 1) % n][x0 % n]; d = g[(y0 + 1) % n][(x0 + 1) % n]
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return f


def pick(pal, t):
    t = max(0.0, min(0.999, t))
    return pal[int(t * len(pal))]


def torus_d(ax, ay, bx, by):
    dx = abs(ax - bx); dy = abs(ay - by)
    dx = min(dx, S - dx); dy = min(dy, S - dy)
    return math.hypot(dx, dy)


# ---------------------------------------------------------------- flint
def flint_block():
    rnd = random.Random(11)
    pts = [(1, 2), (7, 1), (12, 3), (4, 7), (10, 8), (15, 9), (2, 12), (7, 13), (12, 14)]
    pts = [(x + rnd.random(), y + rnd.random()) for x, y in pts]
    tones = [hx("222020"), hx("2e2d2d"), hx("3d3c3c"), hx("2a2929"), hx("353434"), hx("454444"), hx("262424"), hx("3d3c3c"), hx("302f2f")]
    t = Tex()
    region = [[0] * S for _ in range(S)]
    for y in range(S):
        for x in range(S):
            d = sorted((torus_d(x + .5, y + .5, px, py), i) for i, (px, py) in enumerate(pts))
            region[y][x] = d[0][1]
            i = d[0][1]
            # conchoidal ripples: shade by distance to the facet seed
            base = tones[i]
            f = 1.0 + 0.10 * math.cos(d[0][0] * 1.6)
            t.set(x, y, shade(base, f))
    for y in range(S):
        for x in range(S):
            r = region[y][x]
            if region[(y + 1) % S][x] != r or region[y][(x + 1) % S] != r:
                t.set(x, y, hx("121111"))
            elif region[(y - 1) % S][x] != r or region[y][(x - 1) % S] != r:
                t.set(x, y, hx("5e5d5d") if rnd.random() < .55 else hx("4a4949"))
    for x, y in [(3, 4), (9, 5), (13, 11), (5, 10)]:
        t.set(x, y, hx("7f7f7f"))
    t.save("flint_block")


# ---------------------------------------------------------------- sugar cane
CANE = [hx("97f544"), hx("7ccc35"), hx("60a028"), hx("47821e"), hx("3b6318")]
TWINE = [hx("d9b77a"), hx("b8935a"), hx("8c6a3c")]


def sugar_cane_bundle():
    rnd = random.Random(3)
    t = Tex()
    widths = [3, 3, 2, 3, 3, 2]
    x = 0
    for w in widths:
        off = rnd.randrange(6)
        for dx in range(w):
            for y in range(S):
                if w == 3:
                    c = [CANE[1], CANE[2], CANE[3]][dx]
                else:
                    c = [CANE[1], CANE[3]][dx]
                seg = (y + off) % 6
                if seg == 0:
                    c = CANE[0] if dx == 0 else CANE[1]
                elif seg == 1:
                    c = CANE[4] if dx == w - 1 else CANE[3]
                if dx == w - 1 and seg not in (0,):
                    c = shade(c, 0.88)
                t.set(x + dx, y, c)
        x += w
    for band in (2, 12):
        for xx in range(S):
            t.set(xx, band, TWINE[0] if (xx % 4) else TWINE[1])
            t.set(xx, band + 1, TWINE[1] if (xx % 4) != 2 else TWINE[2])
            t.set(xx, band + 2, shade(t.get(xx, band + 2), 0.7))
    t.save("sugar_cane_bundle_side")

    top = Tex(hx("2a4c0d"))
    rnd = random.Random(5)
    for row in range(5):
        for col in range(6):
            cx = col * 3 + (1 if row % 2 else 0) - 1
            cy = row * 3 + 1
            ring = [CANE[2], CANE[1], CANE[2], CANE[3]]
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if dx == 0 and dy == 0:
                        c = hx("d8f2a8") if rnd.random() < .7 else hx("c2e48a")
                    elif dx == -1 or dy == -1:
                        c = CANE[1]
                    else:
                        c = CANE[3]
                    if abs(dx) + abs(dy) == 2:
                        c = shade(c, 0.8)
                    top.set(cx + dx, cy + dy, c)
    # one dark gap row so the 3-px rows tile on 16 px
    for xx in range(S):
        top.set(xx, 15, hx("23400b") if xx % 3 else CANE[4])
    top.save("sugar_cane_bundle_top")


# ---------------------------------------------------------------- charcoal
def charcoal_block():
    pal = [hx("13110d"), hx("1d1a14"), hx("231f18"), hx("2b261d"), hx("312b22")]
    n = value_noise(21, 4)
    rnd = random.Random(21)
    t = Tex()
    for y in range(S):
        for x in range(S):
            # wood grain survives charring: horizontal streaks
            g = 0.5 + 0.18 * math.sin((y + 1.2 * math.sin(x / 3.1 + y)) * 1.6) + 0.55 * (n(x, y) - .5)
            t.set(x, y, pick(pal, g + (rnd.random() - .5) * .25))
    # cracks across the grain
    for cx, y0, length in [(4, 1, 5), (11, 7, 6), (7, 12, 4), (14, 0, 3)]:
        x = cx
        for i in range(length):
            t.set(x, y0 + i, hx("0a0907"))
            if rnd.random() < .4:
                x += rnd.choice((-1, 1))
    for x, y in [(2, 5), (9, 3), (13, 12), (6, 9), (1, 14), (10, 15)]:
        t.set(x, y, hx("4a4234"))
    t.save("charcoal_block")


# ---------------------------------------------------------------- gunpowder
def gunpowder_block():
    pal = [hx("2d2d2d"), hx("3d3d3d"), hx("494949"), hx("505050"), hx("5e5e5e"), hx("727272")]
    n = value_noise(8, 4)
    rnd = random.Random(8)
    t = Tex()
    for y in range(S):
        for x in range(S):
            v = 0.55 * n(x, y) + 0.45 * rnd.random()
            t.set(x, y, pick(pal, v))
    for _ in range(9):
        t.set(rnd.randrange(S), rnd.randrange(S), hx("8a8a8a"))
    for _ in range(7):
        t.set(rnd.randrange(S), rnd.randrange(S), hx("1e1e1e"))
    t.save("gunpowder_block")


# ---------------------------------------------------------------- blaze rods
def blaze_rod_bundle():
    rod = [hx("8a3a00"), hx("ffd23a"), hx("fff32d"), hx("e09a17")]
    t = Tex()
    for r in range(4):
        off = (r * 5) % 4
        for dx in range(4):
            for y in range(S):
                c = rod[dx]
                if (y + off) % 4 == 0 and dx in (1, 2):
                    c = hx("ffb627") if dx == 1 else hx("ffe066")
                if (y + off) % 4 == 2 and dx == 3:
                    c = hx("bf5a00")
                t.set(r * 4 + dx, y, c)
    for band in (3, 11):
        for x in range(S):
            t.set(x, band, hx("5a2a10") if x % 4 else hx("7a3a14"))
            t.set(x, band + 1, hx("3a1a0a"))
            t.set(x, band - 1, shade(t.get(x, band - 1), 0.85))
    t.save("blaze_rod_bundle_side")

    top = Tex(hx("2a1206"))
    ring = {(-1, -1): hx("e09a17"), (0, -1): hx("ffc100"), (1, -1): hx("ffc100"), (2, -1): hx("bf5a00"),
            (-1, 0): hx("ffc100"), (2, 0): hx("bf5a00"), (-1, 1): hx("ffc100"), (2, 1): hx("ae3c00"),
            (-1, 2): hx("bf5a00"), (0, 2): hx("ae3c00"), (1, 2): hx("ae3c00"), (2, 2): hx("7a2a00")}
    for gy in range(4):
        for gx in range(4):
            cx, cy = gx * 4 + 1, gy * 4 + 1
            for (dx, dy), c in ring.items():
                if abs(dx - .5) + abs(dy - .5) >= 3:
                    continue
                top.set(cx + dx, cy + dy, c)
            top.set(cx, cy, hx("ffffb0")); top.set(cx + 1, cy, hx("fff32d"))
            top.set(cx, cy + 1, hx("fff32d")); top.set(cx + 1, cy + 1, hx("ffd23a"))
    top.save("blaze_rod_bundle_top")


# ---------------------------------------------------------------- leather
def leather_block():
    pal = [hx("893b25"), hx("9e492a"), hx("a8502e"), hx("b65733"), hx("c65c35")]
    n = value_noise(31, 4)
    rnd = random.Random(31)
    t = Tex()
    for y in range(S):
        for x in range(S):
            t.set(x, y, pick(pal, 0.65 * n(x, y) + 0.35 * rnd.random()))
    for i in range(S):
        t.set(i, 0, hx("6b2f1b")); t.set(0, i, hx("6b2f1b"))
        t.set(i, 15, hx("542716")); t.set(15, i, hx("542716"))
        t.set(i, 1, shade(t.get(i, 1), 1.08)); t.set(1, i, shade(t.get(1, i), 1.08))
    for i in range(2, 14):
        if i % 2 == 0:
            for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
                t.set(x, y, hx("e8c08a"))
        else:
            for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
                t.set(x, y, shade(t.get(x, y), 0.82))
    for x, y in [(6, 6), (9, 8), (7, 10), (10, 5)]:
        t.set(x, y, hx("d76b43"))
    t.save("leather_block")


# ---------------------------------------------------------------- feathers
def feather_block():
    """Overlapping rows of feathers, like fish scales."""
    t = Tex(hx("8e8e8e"))
    shape = [".LLLRMMM", "LLLLRMMM", "LLLLRMMM", "DLLLRMMD", ".DLLRMD.", "..DLRD..", "...DD..."]
    col = {"L": hx("ffffff"), "M": hx("e2e2e2"), "R": hx("b8b8b8"), "D": hx("9e9e9e")}

    def draw_row(r, only_tip=False):
        for f in range(2):
            ox = f * 8 + 4 * (r % 2)
            for dy, line in enumerate(shape):
                if only_tip and dy < 4:  # rows 4.. hang over the next row
                    continue
                for dx, ch in enumerate(line):
                    if ch != ".":
                        c = col[ch]
                        if ch == "L" and dx == 0 and dy < 3:
                            c = hx("efefef")
                        t.set(ox + dx, r * 4 + dy, c)
    for r in (3, 2, 1, 0):
        draw_row(r)
    draw_row(3, only_tip=True)
    t.save("feather_block")


# ---------------------------------------------------------------- ender pearls
def ender_pearl_block():
    pal = [hx("032620"), hx("063931"), hx("0b4d42"), hx("105e51"), hx("258474"), hx("349988")]
    t = Tex(hx("021a16"))
    centers = [(4.0, 4.0), (12.0, 4.0), (8.0, 12.0), (16.0, 12.0)]
    r = 3.9
    for y in range(S):
        for x in range(S):
            for cx, cy in centers:
                dx = x + .5 - cx; dy = y + .5 - cy
                dx = (dx + 8) % 16 - 8; dy = (dy + 8) % 16 - 8
                d = math.hypot(dx, dy)
                if d <= r:
                    light = 0.55 - (dx + dy) / (2.6 * r) - 0.25 * (d / r) ** 2
                    c = pick(pal, light + 0.35)
                    if d > r - 0.9:
                        c = shade(c, 0.75)
                    # dark swirl in the middle, like the pearl item
                    if abs(dx + 0.3) < 0.9 and abs(dy + 0.3) < 0.9:
                        c = hx("063931")
                    t.set(x, y, c)
    for cx, cy in centers:
        t.set(int(cx) - 2, int(cy) - 2, hx("8fd9cc"))
        t.set(int(cx) - 1, int(cy) - 2, hx("4fb3a2"))
    t.save("ender_pearl_block")


# ---------------------------------------------------------------- rotten flesh
def rotten_flesh_block():
    flesh = [hx("622c10"), hx("8b3418"), hx("a03c1c"), hx("b44420"), hx("c56541")]
    rot = [hx("4f5a22"), hx("67752c"), hx("7f8f3a")]
    n = value_noise(41, 4); m = value_noise(42, 8)
    rnd = random.Random(41)
    t = Tex()
    for y in range(S):
        for x in range(S):
            v = 0.7 * n(x, y) + 0.3 * rnd.random()
            c = pick(flesh, v)
            if m(x, y) > 0.6:
                c = pick(rot, (m(x, y) - .6) * 2.5 + rnd.random() * .3)
            t.set(x, y, c)
    for _ in range(10):
        t.set(rnd.randrange(S), rnd.randrange(S), hx("28140a"))
    for _ in range(5):
        x, y = rnd.randrange(S), rnd.randrange(S)
        t.set(x, y, hx("d98a6a"))
    t.save("rotten_flesh_block")


# ---------------------------------------------------------------- crates
WOOD = [hx("c29d62"), hx("b8945f"), hx("af8f55"), hx("9f844d"), hx("7e6237"), hx("67502c")]
INSIDE = hx("2b2012")


def plank_row(t, y, rnd, shade_f=1.0):
    for x in range(S):
        c = WOOD[rnd.choice((0, 1, 1, 2, 2, 3))]
        t.set(x, y, shade(c, shade_f))


def produce_px(kind, rnd):
    return {
        "carrot": [hx("ff8a1a"), hx("d36a0d"), hx("ac3900"), hx("33be30")],
        "potato": [hx("e9ba62"), hx("d9aa51"), hx("c8973a"), hx("9d772e")],
        "beetroot": [hx("b6484c"), hx("a4272c"), hx("71160d"), hx("1f9a1c")],
        "apple": [hx("ff1c2b"), hx("dd1725"), hx("b4131e"), hx("9c1017")],
    }[kind]


def crate_side(kind):
    rnd = random.Random(sum(map(ord, kind)) * 31)
    t = Tex(INSIDE)
    pal = produce_px(kind, rnd)
    # produce visible through the gaps
    for y in range(S):
        for x in range(S):
            if rnd.random() < .78:
                c = pal[rnd.choice((0, 1, 1, 2, 2, 3))] if kind != "carrot" else pal[rnd.choice((0, 1, 1, 2))]
                if kind == "beetroot" and rnd.random() < .08:
                    c = pal[3]
                t.set(x, y, shade(c, 0.62 + 0.25 * ((x * 7 + y * 3) % 5) / 4))
    # frame: top/bottom boards, corner posts, two slats
    for y in (0, 1, 14, 15):
        plank_row(t, y, rnd)
    for y in (6, 7, 10, 11) if False else (5, 6, 9, 10):
        plank_row(t, y, rnd)
    for x in range(S):
        for y in (1, 6, 10, 15):
            t.set(x, y, shade(t.get(x, y), 0.78))
        for y in (2, 7, 11):
            t.set(x, y, shade(t.get(x, y), 0.6))  # shadow under each board
    for y in range(S):
        for x, f in ((0, 0.95), (1, 0.88), (14, 0.88), (15, 0.7)):
            t.set(x, y, shade(WOOD[2 if y % 5 else 4], f))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15), (1, 1), (14, 1), (1, 14), (14, 14)):
        t.set(x, y, WOOD[5])
    return t


FRUIT = {
    "apple": {"rows": ["...sg..", ".rrsrr.", "rhRrrrd", "rRrrrrd", "rrrrrdd", ".rrddd.", "..ddd.."],
              "h": "ff9a9a", "R": "ff1c2b", "r": "dd1725", "d": "9c1017", "s": "752802", "g": "33be30"},
    "beetroot": {"rows": ["..g.g..", "...gg..", ".bBBbb.", "bBhbbbd", "bbbbbdd", ".bbddd.", "...dt.."],
                 "h": "c07279", "B": "b6484c", "b": "a4272c", "d": "71160d", "t": "5b1d17", "g": "1f9a1c"},
    "potato": {"rows": [".......", ".pPPpp.", "pPhppep", "ppppppd", "pepppdd", ".pdddd.", "......."],
               "h": "f2cf86", "P": "e9ba62", "p": "d9aa51", "d": "9d772e", "e": "6d3701"},
}


def crate_top(kind):
    rnd = random.Random(sum(map(ord, kind)) * 17)
    t = Tex(INSIDE)
    if kind in ("apple", "beetroot", "potato"):
        sprite = FRUIT[kind]
        for i, (ox, oy) in enumerate(((1, 1), (8, 1), (1, 8), (8, 8))):
            flip = i in (1, 2)
            for dy, line in enumerate(sprite["rows"]):
                for dx, ch in enumerate(line):
                    if ch == ".":
                        continue
                    x = ox + (6 - dx if flip and kind != "potato" else dx)
                    t.set(x, oy + dy, hx(sprite[ch]))
    else:  # carrots lying in rows, alternating direction
        body = [hx("ff9a2a"), hx("e8790f"), hx("c45a05"), hx("9a3c00")]
        leaf = [hx("33be30"), hx("1f9a1c"), hx("036703")]
        for i, y0 in enumerate((1, 4, 8, 11)):
            length = 13
            left = i % 2 == 0
            for k in range(length + 1):
                x = (1 + k) if left else (14 - k)
                if k < 2:
                    for dy in range(3):
                        t.set(x, y0 + dy, leaf[(k + dy) % 3])
                    continue
                thick = 3 if k < 9 else (2 if k < 12 else 1)
                for dy in range(thick):
                    yy = y0 + dy + (1 if thick == 1 else 0)
                    c = body[min(3, dy + (1 if k % 4 == 0 else 0))]
                    t.set(x, yy, c)
    # wooden rim
    for i in range(S):
        for (x, y) in ((i, 0), (0, i)):
            t.set(x, y, WOOD[1])
        for (x, y) in ((i, 15), (15, i)):
            t.set(x, y, WOOD[4])
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        t.set(x, y, WOOD[5])
    return t


def crate_bottom():
    rnd = random.Random(77)
    t = Tex()
    for y in range(S):
        plank_row(t, y, rnd)
    for y in (3, 7, 11, 15):
        for x in range(S):
            t.set(x, y, WOOD[4])
    for y, x in ((1, 5), (5, 11), (9, 3), (13, 9)):
        t.set(x, y, WOOD[5])
    t.save("crate_bottom")


def main():
    flint_block(); sugar_cane_bundle(); charcoal_block(); gunpowder_block(); blaze_rod_bundle()
    leather_block(); feather_block(); ender_pearl_block(); rotten_flesh_block()
    for kind in ("carrot", "potato", "beetroot", "apple"):
        crate_side(kind).save(f"{kind}_crate_side")
        crate_top(kind).save(f"{kind}_crate_top")
    crate_bottom()


if __name__ == "__main__":
    main()
