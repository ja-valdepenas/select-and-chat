"""Render the 1024x500 Play Store feature graphic.

Play can overlay a play button in the centre and crops the edges on some surfaces, so the
composition keeps the icon and text inside a generous inset and leaves the right third
quiet.
"""
from PIL import Image, ImageDraw, ImageFont

OUT = "C:/Users/jvald/Projects/whatsapp-smartselect/docs/store"
ICON = OUT + "/play-icon-512.png"

W, H = 1024, 500
SS = 2  # supersample

# Deep Moss. The icon's own field is a mid-tone, which a 1024x500 sheet of would wash the
# icon out entirely, so the ground goes darker and the icon keeps the light.
BG_FROM = (46, 74, 56)    # #2E4A38
BG_TO = (17, 28, 21)      # #111C15
WHITE = (255, 255, 255)
MINT = (175, 203, 177)    # #AFCBB1, the mark's own page-text green

BOLD = "C:/Windows/Fonts/segoeuib.ttf"
REG = "C:/Windows/Fonts/segoeui.ttf"


def diagonal_gradient(w, h, c0, c1):
    small_w, small_h = 128, 64
    img = Image.new("RGB", (small_w, small_h))
    px = []
    for j in range(small_h):
        for i in range(small_w):
            t = (i / (small_w - 1.0) * 0.72) + (j / (small_h - 1.0) * 0.28)
            t = 0.0 if t < 0 else (1.0 if t > 1 else t)
            px.append((
                int(round(c0[0] + (c1[0] - c0[0]) * t)),
                int(round(c0[1] + (c1[1] - c0[1]) * t)),
                int(round(c0[2] + (c1[2] - c0[2]) * t)),
            ))
    img.putdata(px)
    return img.resize((w, h), Image.BICUBIC)


def rounded(img, radius):
    """Apply an app-icon style corner radius."""
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1],
                                           radius=radius, fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


cw, ch = W * SS, H * SS
img = diagonal_gradient(cw, ch, BG_FROM, BG_TO).convert("RGBA")

# Icon, rounded the way a launcher would mask it.
icon_size = 232 * SS
icon = Image.open(ICON).convert("RGBA").resize((icon_size, icon_size), Image.LANCZOS)
icon = rounded(icon, int(icon_size * 0.235))
icon_x, icon_y = 96 * SS, (H - 232) // 2 * SS
img.alpha_composite(icon, (icon_x, icon_y))

d = ImageDraw.Draw(img)
tx = 392 * SS

title = ImageFont.truetype(BOLD, 82 * SS)
sub = ImageFont.truetype(REG, 35 * SS)
tag = ImageFont.truetype(REG, 28 * SS)

d.text((tx, 168 * SS), "Select & Chat", font=title, fill=WHITE)
d.text((tx, 268 * SS), "Tap any phone number,", font=sub, fill=MINT)
d.text((tx, 312 * SS), "land straight in the chat.", font=sub, fill=MINT)
d.text((tx, 372 * SS), "No saving the contact  ·  No permissions",
       font=tag, fill=(143, 178, 150))

out = img.convert("RGB").resize((W, H), Image.LANCZOS)
path = OUT + "/play-feature-graphic-1024x500.png"
out.save(path, "PNG")
print("wrote", path, out.size, out.mode)
