#!/usr/bin/env python3
"""
Renders store-assets/feature_graphic_1024x500.png - the Play Store listing's banner image.
Reuses the same arcade-cabinet icon render and color palette as render_icon.py so the store
listing matches the actual app icon instead of introducing new branding.
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

import render_icon

REPO_ROOT = Path(__file__).resolve().parent.parent
W, H = 1024, 500

BG_TOP = (16, 12, 26)
BG_BOTTOM = (36, 20, 64)


def gradient_bg():
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        t = y / H
        r = int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * t)
        g = int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * t)
        b = int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * t)
        for x in range(W):
            px[x, y] = (r, g, b)
    return img


def load_font(size, bold=True):
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for c in candidates:
        if Path(c).exists():
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()


def main():
    img = gradient_bg()
    d = ImageDraw.Draw(img)

    # Reuse the real icon render for brand consistency, but drop its own flat background so
    # it blends into this banner's gradient instead of showing a mismatched square behind it.
    icon_rgba = render_icon.render_square()
    bg_px = render_icon.BG
    mask = Image.new("L", icon_rgba.size, 255)
    mpx = mask.load()
    ipx = icon_rgba.load()
    for yy in range(icon_rgba.height):
        for xx in range(icon_rgba.width):
            if ipx[xx, yy][:3] == bg_px[:3]:
                mpx[xx, yy] = 0
    icon_size = 380
    icon_rgba = icon_rgba.resize((icon_size, icon_size), Image.LANCZOS)
    mask = mask.resize((icon_size, icon_size), Image.LANCZOS)
    icon_pos = (70, (H - icon_size) // 2)
    img.paste(icon_rgba.convert("RGB"), icon_pos, mask)

    text_x = icon_pos[0] + icon_size + 60

    title_font = load_font(84)
    d.text((text_x, 150), "Game Hub", font=title_font, fill=(255, 255, 255))

    subtitle_font = load_font(34, bold=False)
    d.text((text_x, 260), "Free games. No ads. No accounts.", font=subtitle_font, fill=(34, 211, 238))

    tag_font = load_font(26, bold=False)
    d.text((text_x, 320), "A whole arcade in one app.", font=tag_font, fill=(190, 185, 205))

    out_dir = REPO_ROOT / "store-assets"
    out_dir.mkdir(parents=True, exist_ok=True)
    img.save(out_dir / "feature_graphic_1024x500.png", "PNG")
    print("wrote store-assets/feature_graphic_1024x500.png")


if __name__ == "__main__":
    main()
