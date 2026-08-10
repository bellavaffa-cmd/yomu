# Threadboard — Clothes Designer PWA

A dependency-free, installable **Progressive Web App** for designing custom
apparel in the browser. Pick a garment, colour the fabric, drop in text,
graphics, emoji stickers and your own photos, then export a print-ready PNG or
save the design to a local gallery. Works fully **offline** once loaded.

![Threadboard designing a t-shirt](icons/icon.svg)

## Features

- **5 garments** — T-shirt, long sleeve, tank top, hoodie, tote bag — each drawn
  as a scalable vector silhouette with front / back views and realistic details
  (collar ribs, hood, kangaroo pocket, drawstrings, cuffs, bag handles).
- **Fabric colours** — 14 presets plus a custom colour picker; the garment shades
  its seams and gradient automatically for light or dark fabric.
- **Design layers** — add editable **text** (8 fonts, bold/italic, colour),
  **shapes** (box, circle, triangle, star), **emoji stickers**, and **uploaded
  photos**.
- **Direct manipulation** — drag to move, corner handle to resize, top handle to
  rotate (hold <kbd>Shift</kbd> to snap to 15°). Full layer list with reorder,
  duplicate and delete.
- **Undo / redo**, opacity, and a live inspector.
- **Save & gallery** — designs (with thumbnails) persist in `localStorage`; your
  in-progress work auto-saves and is restored on return.
- **Export** — one-click 2× **PNG** with a transparent background, ready to send
  to a print shop.
- **Installable PWA** — web app manifest, maskable icons, and a service worker
  that caches the app shell for offline use. Add it to your home screen.

## Run it

It's a static site — no build step, no dependencies. Serve the folder over HTTP
(a service worker and the manifest need `http://`/`https://`, not `file://`):

```bash
cd clothes-designer
python3 -m http.server 8000
# then open http://localhost:8000
```

Any static host works (GitHub Pages, Netlify, S3, …). To install as an app, open
it in a Chromium-based browser or Safari and choose **Install** / **Add to Home
Screen**.

## Project layout

| File | Purpose |
| --- | --- |
| `index.html` | App shell and panel layout |
| `styles.css` | Dark, responsive UI (desktop 3-pane, mobile tabbed) |
| `app.js` | Everything: garment library, canvas renderer, pointer interaction (move/scale/rotate), layers, history, persistence, PNG export, PWA wiring |
| `manifest.webmanifest` | PWA metadata, icons, shortcuts |
| `sw.js` | Service worker — offline-first cache of the app shell |
| `icons/` | App icons (SVG + generated PNGs, incl. a maskable variant) |
| `tools/gen-icons.mjs` | Regenerates the PNG icons from pure Node (no deps) |

Regenerate icons after editing the mark:

```bash
node tools/gen-icons.mjs
```

## Tech notes

- **No frameworks, no runtime dependencies.** Garments are `Path2D` outlines
  authored in the canvas coordinate space; design elements are drawn on top and
  clipped to each garment's "print area".
- **State & history** are plain snapshots (JSON), which also power auto-save and
  the saved-design gallery.
- **Export/thumbnails** reuse the same renderer against an off-screen canvas, so
  what you see is exactly what you get.

## Browser support

Latest Chrome, Edge, Firefox and Safari. Install prompts and maskable icons are
best on Chromium; iOS Safari supports Add-to-Home-Screen with the apple-touch
icon.
