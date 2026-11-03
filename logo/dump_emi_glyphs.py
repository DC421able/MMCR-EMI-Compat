"""Dump exact glyph grids + colour profiles from the EMI icon reference."""
from PIL import Image

REF = r"C:\Users\PC\AppData\Roaming\dsh-desktop\harness\attachments\v1\objects\69\695b2c1aa6181f5338cebb76f0c9dde0ad70288e67b704b528cf35d697b40145"
img = Image.open(REF).convert("RGB")
px = img.load()
BG = (65, 45, 82)
CELL = 16
X0, Y0 = 32, 160          # letter origin (top-left of grid)
ROWS, = (13,)

def is_fg(c):
    return max(abs(c[i] - BG[i]) for i in range(3)) > 30

segments = [("E", 0, 7), ("M", 8, 12), ("I", 21, 7)]  # (name, start col, width) cols relative to X0
# derive: E cols0-6 (32..143), gap col7, M cols8-19 (160..351), gap col20, I cols21-27 (368..479)

for name, c0, w in segments:
    print(f"== {name} (cols {c0}..{c0+w-1}) ==")
    grid = []
    for r in range(ROWS):
        row = ""
        for c in range(w):
            cx = X0 + (c0 + c) * CELL + CELL // 2
            cy = Y0 + r * CELL + CELL // 2
            row += "X" if is_fg(px[cx, cy]) else "."
        grid.append(row)
        print(row)
    # colour profile: centre column of letter, every2 rows
    cx = X0 + (c0 + w // 2) * CELL + CELL // 2
    print("colours down the letter centre:")
    for r in range(ROWS):
        cy = Y0 + r * CELL + CELL // 2
        print(f"  row{r:2d} {px[cx, cy]}")
    print()
