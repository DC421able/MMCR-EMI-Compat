"""Sample specific cells of the EMI icon to pin down shading rules."""
from PIL import Image

REF = r"C:\Users\PC\AppData\Roaming\dsh-desktop\harness\attachments\v1\objects\69\695b2c1aa6181f5338cebb76f0c9dde0ad70288e67b704b528cf35d697b40145"
img = Image.open(REF).convert("RGB")
px = img.load()
X0, Y0, CELL = 32, 160, 16

def at(col, row):
    return px[X0 + col * CELL + 8, Y0 + row * CELL + 8]

probes = [
    ("E counter col3 r3", 3, 3),
    ("E counter col3 r4", 3, 4),
    ("E stem col0 r6", 0, 6),
    ("E under-bar col4 r5", 4, 5),
    ("gap between E and M col7 r6", 7, 6),
    ("gap ... r1", 7, 1),
    ("M stem col0 r0 (top)", 0 + 8, 0),
    ("M stem col0 r6", 8, 6),
    ("M stem col0 r11", 8, 11),
    ("M stem col0 r12", 8, 12),
    ("M open lower col5 r9", 5 + 8, 9),
    ("M open lower col5 r12", 5 + 8, 12),
    ("M horn inner col7 r1", 7 + 8, 1),
    ("I counter col21 r5", 21, 5),
    ("I counter col22 r5", 22, 5),
    ("I counter col26 r5", 26, 5),
    ("I counter col21 r9", 21, 9),
    ("I stem col23 r5", 23, 5),
    ("below I row13 col24", 24, 13),
    ("below E row13 col3", 3, 13),
]
for name, c, r in probes:
    x, y = X0 + c * CELL + 8, Y0 + r * CELL + 8
    if 0 <= x < 512 and 0 <= y < 512:
        print(f"{name:34s} ({x:3d},{y:3d}) = {px[x, y]}")

print("corners/edges:", px[8, 8], px[504, 504], px[256, 8], px[8, 256], px[450, 450], px[60, 450])
print("bg reference:", px[10, 250])
