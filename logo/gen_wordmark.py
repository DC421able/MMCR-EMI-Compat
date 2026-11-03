"""MMCR x EMI COMPAT wordmark logo, built from the EMI icon reference.

Reference metrics (analyze_emi_icon / dump_emi_glyphs / probe_emi_cells / grid
extraction): 16 px cells, 13-row glyphs, flat colours with a base*0.24 bottom
band, and a background that is NOT a smooth gradient -- it is five flat tones
on the 32x32 grid:
    (65,45,82) interior  (54,37,73) edge  (47,30,66) corner
    (72,52,89) glow +7   (81,61,98) glow +16
BG_GRID below is the reference's own background layout lifted verbatim
('.'=interior ','=edge ':'=corner 'S'/'F'=decorative square and dashed band),
except '?' cells (reference letters + their halo), which are regenerated from
the radial model with a Bayer dither.  The halo around OUR letters is then
re-laid as two quantised rings (<=1 cell strong, <=2 cells faint) tuned to the
reference's halo-to-letter-height ratio.
Glyphs M/E/I are copied from the reference dump; C/R/O/P/A/T use its grammar.
Line 1: MMCR x EMI   Line 2: COMPAT in pale yellow; uniform 5 px cells.

Outputs are written next to this script.
"""
import os

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))

S = 512
BG_CENTER = (65, 45, 82)
BG_MID = (54, 37, 73)
BG_EDGE = (47, 30, 66)
GLOW_FAINT = (72, 52, 89)     # +7
GLOW_STRONG = (81, 61, 98)    # +16
PALETTE = (BG_EDGE, BG_MID, BG_CENTER, GLOW_FAINT, GLOW_STRONG)
BG_STOPS = (0.0, 0.547, 0.729, 0.943, 1.0)
BG_COLS = (BG_CENTER, BG_CENTER, BG_MID, BG_EDGE, BG_EDGE)
TEX = 16     # texture / quantisation cell, same as the reference

CREAM = (240, 232, 220)
CYAN = (123, 234, 251)
GREEN = (123, 252, 162)
PINK = (235, 122, 253)
MAGENTA = (242, 110, 196)
PALE_YELLOW = (245, 232, 170)   # COMPAT

# 4x4 ordered dither (centre 7.5), scaled by DITHER_K colour units
BAYER = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))
DITHER_K = 2.0

# reference background grid, 32 rows x 32 cells (verbatim; '?' = regenerate)
BG_GRID = [
    ":::::,:,,,,,,,,,,,,,,,,,,,:,::::",
    "::,:,:,,,,,,,,,,,,,,,,,,,,,:,:,:",
    ":,:,:,,,,..............,,,,,:,:,",
    ",:,,,,,.......SSS........,,,,,,:",
    ":,,,,.........SSS..........,,,:,",
    ",,,,...........F............,,,:",
    ",,,..???????????????????????.,,,",
    ",,...?....?...?..?...?..?..?..,,",
    ",,..???..???.???.?..???.?.???.,,",
    ",...???..???.???....???...???..,",
    ",.???????.?????...????.???????.,",
    ",.???????.??????.?????????????.,",
    ",.???????.??????.?????.???????.,",
    ",.????.?.?????????????.?????...,",
    ",.????...?????????????.?????...,",
    ",.??????..????????????..????...,",
    ",.??????.????.????????..????...,",
    ",.??????.????..??..???..????...,",
    ",.?????.?????..???.???.?????...,",
    ",.????..?????..???.???.?????...,",
    ",.???????????...?..???.???????.,",
    ",.???????????...?..???.???????.,",
    ",.???????.???...?..???.???????.,",
    ",,...?.???.???.???.?.?.???....,,",
    ",,.....???.???.???.....???....,,",
    ",,,.....?...?...?.......?....,,:",
    ":,,,....?.?????.?......???..,,,,",
    ",,,,,...F.F.F.F.F......F.F.,,,,:",
    ":,:,,,,.S.S.S.S.S......S.S,,,,:,",
    "::,:,,,,,...............,,,,,:,:",
    ":::,:,,,,,,,,,,,,,,,,,,,,,,,:,::",
    "::::,:,,,,,,,,,,,,,,,,,,,,,:,:::",
]
GRID_TONES = {'.': BG_CENTER, ',': BG_MID, ':': BG_EDGE,
              'S': GLOW_STRONG, 'F': GLOW_FAINT}

def band(c):
    return tuple(round(v * 0.24) for v in c)

# ------------------------------------------------------------------ glyphs
# 'X' base cell, 'd' dark band cell, '.' empty; 13 rows each
M12 = [
    "XXXX....XXXX",
    "XXXXX..XXXXX",
    "XXXXX..XXXXX",
    "XXXXXXXXXXXX",
    "XXXXXXXXXXXX",
    "XXXXXXXXXXXX",
    "XXX.XXXX.XXX",
    "XXX..dd..XXX",     # centre leg tapers into its dark bottom
    "XXX......XXX",
    "XXX......XXX",
    "XXX......XXX",
    "XXX......XXX",
    "ddd......ddd",     # stems end in the dark band
]
C7 = [
    "XXXXXXX", "XXXXXXX", "XXXXXXX",
    "XXX.....", "XXX.....", "XXX.....",
    "XXX.....", "XXX.....", "XXX.....",
    "XXXXXXX", "XXXXXXX",
    "ddddddd",
]
R9 = [
    "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX",
    "XXXdddXXX",          # bowl counter uses the dark shade (like EMI's E)
    "XXXdddXXX",
    "XXXXXXXXX", "XXXXXXXXX",
    "XXX..XXX.",
    "XXX..XXX.",
    "XXX...XXX", "XXX...XXX", "XXX...XXX",
    "ddd...ddd",
]
# EMI glyphs
# E: left stem is base colour on EVERY row (only the area under each bar is dark).
E7 = [
    "XXXXXXX", "XXXXXXX",
    "XXXdddd", "XXXdddd", "XXXdddd",
    "XXXXXX.", "XXXXXX.",
    "XXXddd.", "XXXddd.", "XXXddd.",
    "XXXXXXX", "XXXXXXX",
    "ddddddd",
]
I7 = [
    "XXXXXXX", "XXXXXXX", "XXXXXXX",
    "..XXX..", "..XXX..", "..XXX..",
    "..XXX..", "..XXX..", "..XXX..",
    "XXXXXXX", "XXXXXXX",
    "ddddddd",
]
# O / P / A / T use the reference glyph grammar (2-row bar + up to 3 dark rows
# under it, vertical strokes stay base, bottom row is the dark band).
O8 = [
    "XXXXXXXX", "XXXXXXXX",
    "XXXddXXX", "XXXddXXX", "XXXddXXX",
    "XXX..XXX", "XXX..XXX", "XXX..XXX",
    "XXX..XXX", "XXX..XXX",
    "XXXXXXXX", "XXXXXXXX",
    "dddddddd",
]
P9 = [
    "XXXXXXXXX", "XXXXXXXXX",
    "XXXdddXXX", "XXXdddXXX", "XXXdddXXX",
    "XXXXXXXXX", "XXXXXXXXX",
    "XXXdddddd", "XXXdddddd", "XXXdddddd",
    "XXX......", "XXX......",
    "ddd......",
]
A8 = [
    "XXXXXXXX", "XXXXXXXX",
    "XXXddXXX", "XXXddXXX", "XXXddXXX",
    "XXXXXXXX", "XXXXXXXX",
    "XXXddXXX", "XXXddXXX", "XXXddXXX",
    "XXX..XXX", "XXX..XXX",
    "ddd..ddd",
]
T7 = [
    "XXXXXXX", "XXXXXXX",
    "ddXXXdd", "ddXXXdd", "ddXXXdd",
    "..XXX..", "..XXX..", "..XXX..",
    "..XXX..", "..XXX..",
    "..XXX..", "..XXX..",
    "..ddd..",
]

def cross_grid():
    """pixel X, 11 wide, 13 tall, 3-cell arms"""
    rows = []
    for r in range(13):
        t = r / 12
        cl = round(t * 8)
        cr = 10 - round(t * 8)
        cells = ["."] * 11
        for c in range(cl, cl + 3):
            if c <= 10:
                cells[c] = "X"
        for c in range(cr - 2, cr + 1):
            if 0 <= c <= 10:
                cells[c] = "X"
        rows.append("".join(cells))
    rows[12] = rows[12].replace("X", "d")
    return rows

X11 = cross_grid()

# ------------------------------------------------------------------ layout
def place(canvas_draw, glyph_rows, cwx, chy, x0, y0, colour):
    dk = band(colour)
    for r, row in enumerate(glyph_rows):
        for c, cell in enumerate(row):
            if cell == "X":
                fill = colour
            elif cell == "d":
                fill = dk
            else:
                continue
            canvas_draw.rectangle(
                [x0 + c * cwx, y0 + r * chy, x0 + (c + 1) * cwx - 1, y0 + (r + 1) * chy - 1],
                fill=fill)

# line 1: "MMCR x EMI" -- uniform pixel cell, 88 cells incl. gaps = 440 px
CELL = 5
main = [(M12, CYAN), (M12, GREEN), (C7, PINK), (R9, MAGENTA),   # MMCR
        (X11, CREAM),                                            # x
        (E7, PINK), (M12, GREEN), (I7, CYAN)]                    # EMI
mgaps = [1, 1, 1, 3, 3, 1, 1, 0]
total = sum(len(g[0]) for g, _ in main) * CELL + sum(mgaps) * CELL
x_cursor = (S - total) // 2
y_main = 166

# line 2: "COMPAT" in pale yellow -- 56 cells = 280 px, centred
small = [(C7, PALE_YELLOW), (O8, PALE_YELLOW), (M12, PALE_YELLOW),
         (P9, PALE_YELLOW), (A8, PALE_YELLOW), (T7, PALE_YELLOW)]
sgaps = [1, 1, 1, 1, 1, 0]
total2 = sum(len(g[0]) for g, _ in small) * CELL + sum(sgaps) * CELL
x_small = (S - total2) // 2
y_small = y_main + 13 * CELL + 50

# ------------------------------------- background: verbatim grid + dithered '?'
N = S // TEX
yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
rad = np.sqrt((xx - S / 2) ** 2 + (yy - S / 2) ** 2) / (S * 0.70710678)
rad = np.clip(rad, 0.0, 1.0)
model = np.zeros((S, S, 3), np.float32)
for i in range(len(BG_STOPS) - 1):
    lo, hi = BG_STOPS[i], BG_STOPS[i + 1]
    seg = (rad >= lo) & (rad <= hi) if i == len(BG_STOPS) - 2 else (rad >= lo) & (rad < hi)
    w = ((rad[seg] - lo) / (hi - lo))[:, None]
    model[seg] = np.array(BG_COLS[i], np.float32) * (1 - w) + np.array(BG_COLS[i + 1], np.float32) * w

# our letters' footprint in texture cells (for the halo rings)
letter_cells = set()
gx = x_cursor
for gi, (g, _) in enumerate(main):
    for r, row in enumerate(g):
        for c, ch in enumerate(row):
            if ch in "Xd":
                px0 = gx + c * CELL
                py0 = y_main + r * CELL
                for tx in range(px0 // TEX, (px0 + CELL - 1) // TEX + 1):
                    for ty in range(py0 // TEX, (py0 + CELL - 1) // TEX + 1):
                        letter_cells.add((tx, ty))
    gx += len(g[0]) * CELL + mgaps[gi] * CELL
sx = x_small
for gi, (g, _) in enumerate(small):
    for r, row in enumerate(g):
        for c, ch in enumerate(row):
            if ch in "Xd":
                px0 = sx + c * CELL
                py0 = y_small + r * CELL
                for tx in range(px0 // TEX, (px0 + CELL - 1) // TEX + 1):
                    for ty in range(py0 // TEX, (py0 + CELL - 1) // TEX + 1):
                        letter_cells.add((tx, ty))
    sx += len(g[0]) * CELL + sgaps[gi] * CELL

def halo_distance(tx, ty):
    if (tx, ty) in letter_cells:
        return 0
    best = 99
    for (lx, ly) in letter_cells:
        best = min(best, max(abs(tx - lx), abs(ty - ly)))
        if best == 1:
            break
    return best

pal = np.array(PALETTE, np.float32)
tones = np.empty((N, N, 3), np.float32)
for ty in range(N):
    for tx in range(N):
        ch = BG_GRID[ty][tx]
        decorative = ch in "SF"
        if ch == "?":
            # regenerate: radial model + ordered dither, snapped to the palette
            c = model[ty * TEX + TEX // 2, tx * TEX + TEX // 2]
            d = (BAYER[ty % 4][tx % 4] - 7.5) * DITHER_K
            probe = c + d
            tone = pal[int(np.argmin(((pal - probe) ** 2).sum(axis=1)))]
        else:
            tone = np.array(GRID_TONES[ch], np.float32)
        if not decorative:
            h = halo_distance(tx, ty)
            if h <= 1:
                tone = np.array(GLOW_STRONG, np.float32)
            elif h <= 2:
                tone = np.array(GLOW_FAINT, np.float32)
        tones[ty, tx] = tone

canvas = Image.fromarray(
    np.repeat(np.repeat(tones, TEX, axis=0), TEX, axis=1).astype(np.uint8), "RGB")

d = ImageDraw.Draw(canvas)
gx = x_cursor
for i, (g, col) in enumerate(main):
    place(d, g, CELL, CELL, gx, y_main, col)
    gx += len(g[0]) * CELL + mgaps[i] * CELL
sx = x_small
for i, (g, col) in enumerate(small):
    place(d, g, CELL, CELL, sx, y_small, col)
    sx += len(g[0]) * CELL + sgaps[i] * CELL

canvas.convert("RGB").save(os.path.join(HERE, "mmcr-emi-wordmark.png"))
print("png saved", canvas.size)

# ------------------------------------------------------------------ SVG
def cells_of(glyph_rows, cwx, chy, x0, y0, colour):
    dk = band(colour)
    out = []
    for r, row in enumerate(glyph_rows):
        for c, cell in enumerate(row):
            if cell == "X":
                out.append((x0 + c * cwx, y0 + r * chy, cwx, chy, colour))
            elif cell == "d":
                out.append((x0 + c * cwx, y0 + r * chy, cwx, chy, dk))
    return out

letters = []
gx = x_cursor
for i, (g, col) in enumerate(main):
    letters += cells_of(g, CELL, CELL, gx, y_main, col)
    gx += len(g[0]) * CELL + mgaps[i] * CELL
sx = x_small
for i, (g, col) in enumerate(small):
    letters += cells_of(g, CELL, CELL, sx, y_small, col)
    sx += len(g[0]) * CELL + sgaps[i] * CELL

svg = []
svg.append('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">')
svg.append(f'<rect width="{S}" height="{S}" fill="rgb{BG_CENTER}"/>')
for ty in range(N):
    for tx in range(N):
        tone = tuple(int(v) for v in tones[ty, tx])
        if tone != BG_CENTER:
            svg.append(f'<rect x="{tx * TEX}" y="{ty * TEX}" width="{TEX}" height="{TEX}" fill="rgb{tone}"/>')
for (x, y, w, h, c) in letters:
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="rgb{c}"/>')
svg.append('</svg>')

with open(os.path.join(HERE, "mmcr-emi-wordmark.svg"), "w", encoding="utf-8") as f:
    f.write("\n".join(svg))
print("svg saved")
