#!/usr/bin/env python3
"""Export the project's original folder-plus mark at Appstore icon sizes."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "store-assets"


def render(size: int) -> None:
    scale = size * 4 / 108
    canvas = Image.new("RGBA", (size * 4, size * 4), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    def box(points):
        return tuple(round(value * scale) for value in points)

    # Dark outline, amber folder and a simple white plus; all shapes are original.
    draw.rounded_rectangle(box((15, 28, 93, 86)), radius=round(11 * scale), fill="#205C48")
    draw.rounded_rectangle(box((20, 27, 52, 55)), radius=round(5 * scale), fill="#EABD66")
    draw.rounded_rectangle(box((20, 36, 89, 80)), radius=round(6 * scale), fill="#EABD66")
    draw.rectangle(box((49, 47, 59, 70)), fill="#FFF9EA")
    draw.rectangle(box((42, 54, 66, 63)), fill="#FFF9EA")

    OUTPUT.mkdir(exist_ok=True)
    canvas.resize((size, size), Image.Resampling.LANCZOS).save(OUTPUT / f"folderkit-{size}.png")


if __name__ == "__main__":
    for dimension in (114, 512):
        render(dimension)
