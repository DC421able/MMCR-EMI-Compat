"""Analyse the EMI icon reference: glyph grids, cell size, colours, shading."""
from PIL import Image
import collections

REF = r"C:\Users\PC\AppData\Roaming\dsh-desktop\harness\attachments\v1\objects\69\695b2c1aa6181f5338cebb76f0c9dde0ad70288e67b704b528cf35d697b40145"
img = Image.open(REF).convert("RGB")
W, H = img.size
print("size", img.size)
px = img.load()

# background = most common colour
cnt = collections.Counter(img.getdata())
bg = cnt.most_common(1)[0][0]
print("bg", bg)

def is_fg(c):
    return max(abs(c[i] - bg[i]) for i in range(3)) > 28

# bounding box of letters
xs = [x for x in range(W) for y in range(H) if is_fg(px[x, y])]
ys = [y for x in range(W) for y in range(H) if is_fg(px[x, y])]
print("bbox x", min(xs), max(xs), "y", min(ys), max(ys))

# column profile -> letter segments
colfill = [sum(1 for y in range(H) if is_fg(px[x, y])) for x in range(W)]
segs, start = [], None
for x in range(W):
    if colfill[x] > 3 and start is None:
        start = x
    elif colfill[x] <= 3 and start is not None:
        segs.append((start, x - 1))
        start = None
if start is not None:
    segs.append((start, W - 1))
print("letter segments (x ranges):", segs)

# estimate cell size: run lengths of transitions inside first letter
# look at horizontal run lengths of fg in the vertical middle row
mid = (min(ys) + max(ys)) // 2
row = [is_fg(px[x, mid]) for x in range(W)]
runs = []
cur, n = row[0], 1
for v in row[1:]:
    if v == cur:
        n += 1
    else:
        runs.append((cur, n))
        cur, v2 = v, v
        n = 1
runs.append((cur, n))
print("mid-row run lengths:", runs)

# ASCII grid of letter 1 & 2 & 3 using candidate cell size
# derive cell from gcd-ish: use column gaps
x0, x1 = min(xs), max(xs)
y0, y1 = min(ys), max(ys)
print("letter box w,h:", x1 - x0 + 1, y1 - y0 + 1)

# sample colours: top band / middle / bottom band of each letter segment
for i, (sx, ex) in enumerate(segs):
    cxm = (sx + ex) // 2
    tops = [px[cxm, y] for y in range(y0, y0 + 12)]
    mids = [px[cxm, y] for y in range((y0 + y1) // 2 - 5, (y0 + y1) // 2 + 5)]
    bots = [px[cxm, y] for y in range(y1 - 11, y1 + 1)]
    avg = lambda L: tuple(round(sum(p[j] for p in L) / len(L)) for j in range(3))
    print(f"letter{i} x[{sx},{ex}] w={ex-sx+1} top={avg(tops)} mid={avg(mids)} bot={avg(bots)}")
