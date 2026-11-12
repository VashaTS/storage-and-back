#!/usr/bin/env python3
"""Mod icon (512x512): three storage blocks drawn as isometric cubes. Run after gen_textures.py."""
import math
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

T = "src/main/resources/assets/storageback/textures/block"


def tex(name):
    return np.array(Image.open(f"{T}/{name}.png").convert("RGBA"))


def cube(top, left, right, L, W, H):
    """Renders a unit cube with edge length L px into an RGBA array (W x H), origin = top vertex of the top face."""
    c, s = math.cos(math.radians(30)), math.sin(math.radians(30))
    ex = np.array([L * c, L * s]); ez = np.array([-L * c, L * s]); ey = np.array([0.0, -L])
    out = np.zeros((H, W, 4), np.uint8)
    O = np.array([W / 2, L + 2.0])  # corner (0,0,0); the top face's back corner (0,1,0) sits at y = 2
    ys, xs = np.mgrid[0:H, 0:W]
    P = np.stack([xs + .5, ys + .5], -1).astype(float)

    def face(origin, a, b, texture, light, uv):
        M = np.array([a, b]).T
        inv = np.linalg.inv(M)
        d = P - origin
        st = d @ inv.T
        m = (st[..., 0] >= 0) & (st[..., 0] < 1) & (st[..., 1] >= 0) & (st[..., 1] < 1)
        u, v = uv(st[..., 0], st[..., 1])
        ui = np.clip((u * 16).astype(int), 0, 15); vi = np.clip((v * 16).astype(int), 0, 15)
        col = texture[vi, ui].astype(float)
        col[..., :3] *= light
        out[m] = col[m].astype(np.uint8)

    top_origin = O + ey  # point (0,1,0)
    face(top_origin, ex, ez, top, 1.0, lambda a, b: (a, b))
    face(O + ez + ey, ex, -ey, left, 0.82, lambda a, b: (a, b))          # z = 1 face, params x, (1-y)
    face(O + ex + ey + ez, -ez, -ey, right, 0.62, lambda a, b: (a, b))   # x = 1 face, params (1-z), (1-y)
    return Image.fromarray(out)


def main(size=512):
    S = size * 2  # supersample the background only; cubes stay crisp
    bg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    grad = Image.new("RGBA", (S, S))
    gd = ImageDraw.Draw(grad)
    for y in range(S):
        t = y / S
        gd.line([(0, y), (S, y)], fill=(int(58 + 30 * t), int(74 + 18 * t), int(92 - 10 * t), 255))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=S // 7, fill=255)
    bg.paste(grad, (0, 0), mask)
    bg = bg.resize((size, size), Image.LANCZOS)

    L = 124
    c30, s30 = math.cos(math.radians(30)), math.sin(math.radians(30))
    W, H = int(2 * L * c30) + 4, int(2 * L) + 6
    crate = cube(tex("apple_crate_top"), tex("apple_crate_side"), tex("apple_crate_side"), L, W, H)
    flint = cube(tex("blaze_rod_bundle_top"), tex("blaze_rod_bundle_side"), tex("blaze_rod_bundle_side"), L, W, H)
    cane = cube(tex("sugar_cane_bundle_top"), tex("sugar_cane_bundle_side"), tex("sugar_cane_bundle_side"), L, W, H)

    def shadow(img):
        a = img.split()[3].point(lambda v: 120 if v else 0)
        sh = Image.new("RGBA", img.size, (8, 10, 16, 0)); sh.putalpha(a)
        return sh.filter(ImageFilter.GaussianBlur(10))

    # grid cells: top block on (0,1,0) behind, lower blocks at (0,0,1) and (1,0,0)
    big = Image.new("RGBA", (W * 3, H * 3), (0, 0, 0, 0))
    ox, oy = W, H
    cells = [(cane, (0, -L)), (crate, (-L * c30, L * s30)), (flint, (L * c30, L * s30))]
    for img, (dx, dy) in cells:
        big.alpha_composite(img, (int(ox + dx), int(oy + dy)))
    bbox = big.getbbox()
    art = big.crop(bbox)
    x = (size - art.width) // 2; y = (size - art.height) // 2 + 4
    sh = Image.new("RGBA", (size, size), (0, 0, 0, 0)); sh.alpha_composite(art, (x + 6, y + 12))
    bg.alpha_composite(shadow(sh))
    bg.alpha_composite(art, (x, y))
    bg.save("src/main/resources/assets/storageback/icon.png")
    bg.save("docs/icon.png")




def block_icons():
    """64x64 isometric icons of every block for the README / Modrinth tables (docs/icons)."""
    import json
    os.makedirs("docs/icons", exist_ok=True)
    L = 32
    W, H = int(2 * L * math.cos(math.radians(30))) + 4, int(2 * L) + 6
    for name in sorted(os.listdir("src/main/resources/assets/storageback/models/block")):
        if name.endswith("_horizontal.json"):
            continue
        model = json.load(open(f"src/main/resources/assets/storageback/models/block/{name}"))["textures"]
        p = lambda k: tex(model[k].split("/")[-1])
        if "all" in model:
            top = side = p("all")
        elif "end" in model:
            top, side = p("end"), p("side")
        else:
            top, side = p("top"), p("side")
        img = cube(top, side, side, L, W, H)
        canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        canvas.alpha_composite(img.crop(img.getbbox()), ((64 - img.getbbox()[2] + img.getbbox()[0]) // 2, 0))
        canvas.save(f"docs/icons/{name[:-5]}.png")


if __name__ == "__main__":
    os.makedirs("docs", exist_ok=True)
    main()
    block_icons()
