# Regenerates feature_graphic.png. Requires: pip install resvg-py Pillow
import os
import resvg_py
from PIL import Image, ImageDraw, ImageFont
import io

HERE = os.path.dirname(os.path.abspath(__file__))

# Supersample at 2x: final output is 1024x500.
SS = 2
W, H = 1024 * SS, 500 * SS

BG = (15, 25, 35)          # #0F1923 deep navy
SKY_COLOR = (138, 155, 173)   # #8A9BAD dark-theme secondary text
SPEAK_COLOR = (224, 230, 237) # #E0E6ED dark-theme primary text
TAGLINE_COLOR = (138, 155, 173, 220)

img = Image.new("RGB", (W, H), BG)
draw = ImageDraw.Draw(img, "RGBA")

# --- Icon glyph (transparent bg) ---
glyph_png = resvg_py.svg_to_bytes(svg_path=f"{HERE}/icon_glyph_only.svg", width=1200, height=1200)
glyph = Image.open(io.BytesIO(bytearray(glyph_png))).convert("RGBA")

glyph_size = int(340 * SS)
glyph_resized = glyph.resize((glyph_size, glyph_size), Image.LANCZOS)
glyph_x = int(70 * SS)
glyph_y = (H - glyph_size) // 2
img.paste(glyph_resized, (glyph_x, glyph_y), glyph_resized)

# --- Wordmark: "sky" + "speak" with letter-spacing, Helvetica Neue Light ---
FONT_PATH = "/System/Library/Fonts/HelveticaNeue.ttc"
wordmark_size = int(96 * SS)
font_light = ImageFont.truetype(FONT_PATH, wordmark_size, index=7)   # Light

letter_spacing = int(6 * SS)
text_x = int(470 * SS)
text_baseline_y = int(190 * SS)

def draw_tracked_text(draw, xy, text, font, fill, spacing):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        bbox = font.getbbox(ch)
        char_w = bbox[2] - bbox[0]
        advance = draw.textlength(ch, font=font)
        x += advance + spacing
    return x

x_after_sky = draw_tracked_text(draw, (text_x, text_baseline_y), "sky", font_light, SKY_COLOR, letter_spacing)
draw_tracked_text(draw, (x_after_sky, text_baseline_y), "speak", font_light, SPEAK_COLOR, letter_spacing)

# --- Tagline ---
max_width = W - text_x - int(40 * SS)
tagline = "Ad-free weather, live radar & an AI assistant"
tagline_size = int(30 * SS)
font_tagline = ImageFont.truetype(FONT_PATH, tagline_size, index=10)  # Medium
while draw.textlength(tagline, font=font_tagline) > max_width and tagline_size > int(18 * SS):
    tagline_size -= 2
    font_tagline = ImageFont.truetype(FONT_PATH, tagline_size, index=10)
tagline_y = int(300 * SS)
draw.text((text_x, tagline_y), tagline, font=font_tagline, fill=TAGLINE_COLOR)

# Downsample for anti-aliasing.
final = img.resize((1024, 500), Image.LANCZOS)
out_path = f"{HERE}/feature_graphic.png"
final.save(out_path)
print("saved", out_path)
