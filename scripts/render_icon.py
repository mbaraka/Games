#!/usr/bin/env python3
"""
Renders the app's actual adaptive icon (app/src/main/res/drawable/ic_launcher_background.xml
+ ic_launcher_foreground.xml - a dark gaming-themed controller design) as real raster images,
at high-res supersample then downscaled, so every output is clean rather than regenerated
mipmap bitmaps that just went stale after someone last redrew the vector icon.

Writes:
  - app/src/main/res/mipmap-*/ic_launcher.webp (square, legacy pre-adaptive-icon fallback)
  - app/src/main/res/mipmap-*/ic_launcher_round.webp (circle-masked, same fallback use)
  - store-assets/app_icon_512.png (Play Console's standalone 512x512 PNG, no alpha)

The shapes below are a hand-transcription of the two vector drawables' <path> data (both on a
108x108 viewport) - keep them in sync if the vector drawables ever change.
"""

from pathlib import Path
from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parent.parent
SUPERSAMPLE = 864  # 108 * 8, divisible cleanly down to every output size below
VIEWPORT = 108

BG_DARK = (30, 30, 30, 255)      # #1E1E1E
BG_STRIPE = (42, 42, 42, 255)    # #2A2A2A
GREEN = (76, 175, 80, 255)       # #4CAF50
ORANGE = (255, 87, 34, 255)      # #FF5722
WHITE = (255, 255, 255, 255)


def s(v: float) -> float:
    return v * SUPERSAMPLE / VIEWPORT


def render_square() -> Image.Image:
    img = Image.new("RGBA", (SUPERSAMPLE, SUPERSAMPLE), BG_DARK)
    d = ImageDraw.Draw(img)

    # ic_launcher_background.xml: dark canvas with a subtle lighter frame
    d.rectangle([s(0), s(0), s(108), s(8)], fill=BG_STRIPE)
    d.rectangle([s(0), s(100), s(108), s(108)], fill=BG_STRIPE)
    d.rectangle([s(0), s(0), s(8), s(108)], fill=BG_STRIPE)
    d.rectangle([s(100), s(0), s(108), s(108)], fill=BG_STRIPE)

    # ic_launcher_foreground.xml: dark circle "badge", then a green game controller with
    # orange D-pad/buttons, plus four corner pixel accents and a white center hub dot.
    d.ellipse([s(6), s(6), s(102), s(102)], fill=BG_DARK)

    d.rounded_rectangle([s(26), s(40), s(82), s(68)], radius=s(4), fill=GREEN)
    d.rounded_rectangle([s(18), s(44), s(26), s(64)], radius=s(4), fill=GREEN)
    d.rounded_rectangle([s(82), s(44), s(90), s(64)], radius=s(4), fill=GREEN)

    # D-pad: an L/plus-ish silhouette made of the two source rects, not their merged bbox.
    d.rectangle([s(36), s(42), s(52), s(50)], fill=ORANGE)
    d.rectangle([s(36), s(50), s(44), s(58)], fill=ORANGE)

    d.ellipse([s(58), s(46), s(66), s(54)], fill=ORANGE)
    d.ellipse([s(70), s(46), s(78), s(54)], fill=ORANGE)

    d.rectangle([s(20), s(20), s(24), s(24)], fill=GREEN)
    d.rectangle([s(84), s(20), s(88), s(24)], fill=ORANGE)
    d.rectangle([s(20), s(84), s(24), s(88)], fill=GREEN)
    d.rectangle([s(84), s(84), s(88), s(88)], fill=ORANGE)

    d.ellipse([s(54), s(52), s(58), s(56)], fill=WHITE)

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
