#!/usr/bin/env python3
"""
Renders the app's actual adaptive icon (app/src/main/res/drawable/ic_launcher_background.xml
+ ic_launcher_foreground.xml - a retro arcade-cabinet silhouette) as real raster images, at
high-res supersample then downscaled, so every output is clean rather than regenerated
mipmap bitmaps that just went stale after someone last redrew the vector icon.

Writes:
  - app/src/main/res/mipmap-*/ic_launcher.webp (square, legacy pre-adaptive-icon fallback)
  - app/src/main/res/mipmap-*/ic_launcher_round.webp (circle-masked, same fallback use)
  - store-assets/app_icon_512.png (Play Console's standalone 512x512 PNG, no alpha)

The shapes below are a hand-transcription of the vector drawables' <path> data (108x108
viewport) - keep them in sync if the vector drawables ever change.
"""

from pathlib import Path
from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parent.parent
SUPERSAMPLE = 864  # 108 * 8, divisible cleanly down to every output size below
VIEWPORT = 108

BG = (16, 12, 26, 255)        # #100C1A
CABINET = (237, 237, 242, 255)  # #EDEDF2
CYAN = (34, 211, 238, 255)      # #22D3EE
PURPLE = (124, 58, 237, 255)    # #7C3AED
SCREEN = (20, 10, 46, 255)      # #140A2E
PANEL = (35, 31, 51, 255)       # #231F33
ORANGE = (255, 94, 58, 255)     # #FF5E3A
BASE = (75, 70, 92, 255)        # #4B465C


def s(v: float) -> float:
    return v * SUPERSAMPLE / VIEWPORT


def render_square() -> Image.Image:
    img = Image.new("RGBA", (SUPERSAMPLE, SUPERSAMPLE), BG)
    d = ImageDraw.Draw(img)

    # Cabinet body (trapezoid, wider at the base)
    d.polygon([(s(31), s(16)), (s(77), s(16)), (s(85), s(90)), (s(23), s(90))], fill=CABINET)

    # Marquee stripe
    d.rectangle([s(31), s(16), s(77), s(21)], fill=CYAN)

    # Screen glow + screen
    d.rounded_rectangle([s(33), s(24), s(75), s(54)], radius=s(3), fill=PURPLE)
    d.rounded_rectangle([s(37), s(28), s(71), s(50)], radius=s(2), fill=SCREEN)

    # Control panel + buttons
    d.rounded_rectangle([s(20), s(64), s(88), s(72)], radius=s(2), fill=PANEL)
    d.ellipse([s(24.6), s(64.8), s(31), s(71.2)], fill=ORANGE)
    d.ellipse([s(35.6), s(64.8), s(42), s(71.2)], fill=ORANGE)
    d.ellipse([s(66), s(64.8), s(72.4), s(71.2)], fill=CYAN)
    d.ellipse([s(77), s(64.8), s(83.4), s(71.2)], fill=CYAN)

    # Base
    d.rectangle([s(23), s(90), s(85), s(94)], fill=BASE)

    return img


def circle_mask(img: Image.Image) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse([0, 0, img.size[0], img.size[1]], fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


def main():
    square = render_square()
    round_icon = circle_mask(square)

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    res_dir = REPO_ROOT / "app" / "src" / "main" / "res"
    for folder, size in densities.items():
        out_dir = res_dir / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        square.resize((size, size), Image.LANCZOS).save(out_dir / "ic_launcher.webp", "WEBP", lossless=True)
        round_icon.resize((size, size), Image.LANCZOS).save(out_dir / "ic_launcher_round.webp", "WEBP", lossless=True)
        print(f"wrote {folder}/ic_launcher.webp and ic_launcher_round.webp @ {size}x{size}")

    store_dir = REPO_ROOT / "store-assets"
    store_dir.mkdir(parents=True, exist_ok=True)
    flat = square.convert("RGB").resize((512, 512), Image.LANCZOS)  # Play's 512x512 icon must have no alpha
    flat.save(store_dir / "app_icon_512.png", "PNG")
    print("wrote store-assets/app_icon_512.png @ 512x512")


if __name__ == "__main__":
    main()
