"""Render the MMCR x EMI logo (same geometry as mmcr-emi-logo.svg) to PNG with Pillow."""
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))

S = 1024          # canvas
K = S / 512.0     # scale from SVG viewBox units

BG0, BG1, BG2 = (58, 34, 102), (139, 63, 209), (217, 79, 240)
CREAM, CREAM_D = (244, 238, 230), (207, 194, 174)
INK = (29, 17, 54)
GREEN0, GREEN1 = (99, 243, 154), (44, 196, 101)
GREEN_DARK = (14, 58, 31)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# --- diagonal gradient ---
grad = Image.new("RGB", (S, S))
px = grad.load()
for y in range(S):
    for x in range(S):
        t = (x + y) / (2 * (S - 1))
        px[x, y] = lerp(BG0, BG1, t / 0.55) if t < 0.55 else lerp(BG1, BG2, (t - 0.55) / 0.45)

# --- rounded badge mask ---
mask = Image.new("L", (S, S), 0)
md = ImageDraw.Draw(mask)
md.rounded_rectangle([8 * K, 8 * K, 504 * K, 504 * K], radius=112 * K, fill=255)

out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
out.paste(grad, (0, 0), mask)
d = ImageDraw.Draw(out)
d.rounded_rectangle([8 * K, 8 * K, 504 * K, 504 * K], radius=112 * K,
                    outline=INK, width=round(16 * K))


def slot(x, y, w=90, h=90, r=12):
    """cream slot with a 4px drop edge below-right"""
    d.rounded_rectangle([(x + 3) * K, (y + 3) * K, (x + w) * K, (y + h) * K],
                        radius=r * K, fill=CREAM_D)
    d.rounded_rectangle([x * K, y * K, (x + w) * K, (y + h) * K],
                        radius=r * K, fill=CREAM)


# 3x3 grid at x/y = 101, 211, 321
for gy in (101, 211, 321):
    for gx in (101, 211, 321):
        slot(gx, gy)

# recipe arrow: shaft + head, drawn over middle row
arrow = [(56, 236), (360, 236), (360, 206), (456, 256), (360, 306), (360, 276), (56, 276)]
# gradient fill for arrow: vertical cream ramp -> simple two-tone
d.polygon([(x * K, y * K) for x, y in arrow], fill=(255, 250, 240))
d.line([(56 * K, 236 * K), (360 * K, 236 * K), (360 * K, 206 * K),
        (456 * K, 256 * K), (360 * K, 306 * K), (360 * K, 276 * K),
        (56 * K, 276 * K), (56 * K, 236 * K)],
       fill=INK, width=round(10 * K), joint="curve")
# soft shading under shaft
d.line([(60 * K, 272 * K), (356 * K, 272 * K)], fill=(230, 220, 203), width=round(8 * K))

# check badge
cx, cy = 416 * K, 416 * K
r = 62 * K
d.ellipse([cx - r - 10 * K, cy - r - 10 * K, cx + r + 10 * K, cy + r + 10 * K], fill=INK)
# smooth diagonal gradient inside the circle (light top-left -> dark bottom-right)
bx0, by0, bx1, by1 = int(cx - r), int(cy - r), int(cx + r), int(cy + r)
badge = Image.new("RGBA", (bx1 - bx0, by1 - by0), (0, 0, 0, 0))
bp = badge.load()
for yy in range(by1 - by0):
    for xx in range(bx1 - bx0):
        t = (xx + yy) / max(1, (bx1 - bx0) + (by1 - by0) - 2)
        bp[xx, yy] = lerp(GREEN0, GREEN1, t) + (255,)
bmask = Image.new("L", badge.size, 0)
ImageDraw.Draw(bmask).ellipse([0, 0, badge.size[0] - 1, badge.size[1] - 1], fill=255)
out.paste(badge, (bx0, by0), bmask)
d.line([(384 * K, 416 * K), (406 * K, 440 * K), (448 * K, 392 * K)],
       fill=GREEN_DARK, width=round(16 * K), joint="curve")

out.save(os.path.join(HERE, "mmcr-emi-logo.png"))
print("saved", out.size)
