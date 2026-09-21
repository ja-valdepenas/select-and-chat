"""Render the Play Store 512x512 icon from the adaptive launcher icon's own geometry.

The launcher icon is a VectorDrawable on a 108x108 viewport, and the launcher mask shows
the central 72x72 of it. Rendering that same 72x72 window is what makes the Play listing
icon match what people see on their home screen, instead of a shrunken version of it.

Geometry is the Moss "Hand-off" mark: a line of page text with its first stretch
highlighted, and a round bubble whose tail points back down at that highlight. Every
number below is the same number that appears in ic_launcher_foreground.xml — change one
and change both.
"""
import os
from PIL import Image, ImageDraw

OUT = "C:/Users/jvald/Projects/whatsapp-smartselect/docs/store"

# Moss palette. Same five values as the drawables.
FIELD_FROM = (141, 180, 143)   # #8DB48F
FIELD_TO = (87, 129, 95)       # #57815F
BUBBLE = (247, 245, 232)       # #F7F5E8
BAR = (61, 98, 71)             # #3D6247
LINE_DIM = (175, 203, 177)     # #AFCBB1
LINE_HOT = (44, 75, 54)        # #2C4B36

WINDOW = 72.0          # the launcher-mask window, in viewport units
ORIGIN = 18.0          # its top-left corner within the 108 viewport
SIZE = 512
SS = 4                 # supersample factor
CANVAS = SIZE * SS
S = CANVAS / WINDOW    # viewport unit -> supersampled pixel


def vp(x, y):
    """Viewport coords -> supersampled canvas coords."""
    return ((x - ORIGIN) * S, (y - ORIGIN) * S)


def diagonal_gradient(size, c0, c1, p0, p1):
    """Linear gradient between two canvas points, built small and upscaled.

    A linear gradient is exactly reproducible under bicubic upscaling, so building it at
    low resolution and resizing is both fast and lossless here.
    """
    small = 128
    img = Image.new("RGB", (small, small))
    px = []
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    denom = float(dx * dx + dy * dy)
    for j in range(small):
        cy = (j + 0.5) * size / small
        for i in range(small):
            cx = (i + 0.5) * size / small
            t = ((cx - p0[0]) * dx + (cy - p0[1]) * dy) / denom
            t = 0.0 if t < 0 else (1.0 if t > 1 else t)
            px.append((
                int(round(c0[0] + (c1[0] - c0[0]) * t)),
                int(round(c0[1] + (c1[1] - c0[1]) * t)),
                int(round(c0[2] + (c1[2] - c0[2]) * t)),
            ))
    img.putdata(px)
    return img.resize((size, size), Image.BICUBIC)


def cubic(p0, p1, p2, p3, steps=48):
    """Sample a cubic Bezier in viewport coords.

    Pillow has no curve primitive, so the bubble's tail — two cubics in the vector — is
    flattened here. 48 steps is well past the point where the difference survives the
    downsample to 512.
    """
    pts = []
    for i in range(steps + 1):
        t = i / float(steps)
        u = 1.0 - t
        a, b, c, d = u * u * u, 3 * u * u * t, 3 * u * t * t, t * t * t
        pts.append((
            a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
            a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1],
        ))
    return pts


# Background: the gradient is declared across the full 108 viewport, so its endpoints sit
# outside the 72 window we are rendering.
img = diagonal_gradient(CANVAS, FIELD_FROM, FIELD_TO, vp(0, 0), vp(108, 108)).convert("RGBA")
d = ImageDraw.Draw(img)

# The line of page text, and the selected stretch of it.
d.rounded_rectangle([*vp(36, 74), *vp(74, 82)], radius=4 * S, fill=LINE_DIM)
d.rounded_rectangle([*vp(36, 74), *vp(53, 82)], radius=4 * S, fill=LINE_HOT)

# The bubble's tail: M40,60 C37,66 35,69 30,71 C38,70 46,67 52,63 Z
tail = (cubic((40, 60), (37, 66), (35, 69), (30, 71))
        + cubic((30, 71), (38, 70), (46, 67), (52, 63)))
d.polygon([vp(x, y) for x, y in tail], fill=BUBBLE)

# The bubble itself, drawn over the tail's base so the join disappears.
d.ellipse([*vp(33, 24), *vp(75, 66)], fill=BUBBLE)

# The number, inside the chat.
d.rounded_rectangle([*vp(40, 40), *vp(68, 50)], radius=5 * S, fill=BAR)

icon = img.resize((SIZE, SIZE), Image.LANCZOS)

os.makedirs(OUT, exist_ok=True)
path = os.path.join(OUT, "play-icon-512.png")
icon.save(path, "PNG")
print("wrote", os.path.normpath(path), icon.size, icon.mode)
