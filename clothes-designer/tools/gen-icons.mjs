// Pure-Node icon generator — no external deps.
// Draws a stylised t-shirt mark on a rounded indigo tile and encodes real PNGs
// using Node's built-in zlib for the IDAT stream. Run: `node tools/gen-icons.mjs`
import { deflateSync } from 'node:zlib';
import { writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'icons');
mkdirSync(OUT, { recursive: true });

// ---- tiny raster canvas -------------------------------------------------
class Raster {
  constructor(w, h) {
    this.w = w; this.h = h;
    this.px = new Uint8Array(w * h * 4); // RGBA, transparent
  }
  set(x, y, [r, g, b, a = 255]) {
    x |= 0; y |= 0;
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
    const i = (y * this.w + x) * 4;
    if (a === 255) { this.px[i] = r; this.px[i + 1] = g; this.px[i + 2] = b; this.px[i + 3] = 255; return; }
    // alpha blend over existing
    const ba = this.px[i + 3] / 255, fa = a / 255, oa = fa + ba * (1 - fa);
    if (oa === 0) return;
    this.px[i]     = (r * fa + this.px[i]     * ba * (1 - fa)) / oa;
    this.px[i + 1] = (g * fa + this.px[i + 1] * ba * (1 - fa)) / oa;
    this.px[i + 2] = (b * fa + this.px[i + 2] * ba * (1 - fa)) / oa;
    this.px[i + 3] = oa * 255;
  }
  fillRoundRect(x, y, w, h, r, color) {
    for (let py = y; py < y + h; py++) {
      for (let px = x; px < x + w; px++) {
        const dx = Math.min(px - x, x + w - 1 - px);
        const dy = Math.min(py - y, y + h - 1 - py);
        if (dx < r && dy < r) {
          const d = Math.hypot(r - dx, r - dy);
          if (d > r + 0.5) continue;
          const a = d > r - 0.5 ? (r + 0.5 - d) : 1;
          this.set(px, py, [color[0], color[1], color[2], 255 * a]);
        } else this.set(px, py, color);
      }
    }
  }
  fillPolygon(pts, color) {
    let minY = Infinity, maxY = -Infinity;
    for (const [, py] of pts) { minY = Math.min(minY, py); maxY = Math.max(maxY, py); }
    for (let y = Math.floor(minY); y <= Math.ceil(maxY); y++) {
      const xs = [];
      for (let i = 0; i < pts.length; i++) {
        const [x1, y1] = pts[i], [x2, y2] = pts[(i + 1) % pts.length];
        if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
          xs.push(x1 + (y - y1) / (y2 - y1) * (x2 - x1));
        }
      }
      xs.sort((a, b) => a - b);
      for (let k = 0; k + 1 < xs.length; k += 2) {
        for (let x = Math.floor(xs[k]); x <= Math.ceil(xs[k + 1]); x++) {
          if (x >= xs[k] && x <= xs[k + 1]) this.set(x, y, color);
        }
      }
    }
  }
  fillCircle(cx, cy, r, color) {
    for (let y = cy - r; y <= cy + r; y++)
      for (let x = cx - r; x <= cx + r; x++) {
        const d = Math.hypot(x - cx, y - cy);
        if (d <= r + 0.5) this.set(x, y, [color[0], color[1], color[2], d > r - 0.5 ? 255 * (r + 0.5 - d) : 255]);
      }
  }
  png() {
    const { w, h, px } = this;
    const raw = Buffer.alloc((w * 4 + 1) * h);
    for (let y = 0; y < h; y++) {
      raw[y * (w * 4 + 1)] = 0; // filter: none
      px.slice(y * w * 4, (y + 1) * w * 4).forEach((v, i) => { raw[y * (w * 4 + 1) + 1 + i] = v; });
    }
    const idat = deflateSync(raw, { level: 9 });
    const chunk = (type, data) => {
      const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
      const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
      const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(body) >>> 0);
      return Buffer.concat([len, body, crc]);
    };
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
    ihdr[8] = 8; ihdr[9] = 6; // 8-bit, RGBA
    return Buffer.concat([
      Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
      chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0)),
    ]);
  }
}

const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return c ^ 0xffffffff;
}

// ---- the mark -----------------------------------------------------------
// tee outline in a 100x100 design box, scaled to the icon.
const TEE = [
  [18, 34], [34, 23], [42, 25], [50, 33], [58, 25], [66, 23], [82, 34],
  [73, 49], [66, 43], [68, 83], [32, 83], [34, 43], [27, 49],
];
const INDIGO = [99, 102, 241];
const WHITE = [255, 255, 255];
const ACCENT = [244, 114, 182]; // pink chest accent

function drawIcon(size, { maskable = false } = {}) {
  const r = new Raster(size, size);
  const pad = maskable ? 0 : Math.round(size * 0.06);
  const radius = maskable ? 0 : Math.round(size * 0.22);
  r.fillRoundRect(pad, pad, size - pad * 2, size - pad * 2, radius, INDIGO);
  // scale tee into the safe area (smaller for maskable)
  const inset = maskable ? size * 0.20 : size * 0.16;
  const box = size - inset * 2;
  const scale = ([x, y]) => [inset + (x / 100) * box, inset + (y / 100) * box];
  r.fillPolygon(TEE.map(scale), WHITE);
  const [cx, cy] = scale([50, 55]);
  r.fillCircle(cx, cy, box * 0.10, ACCENT);
  return r.png();
}

const targets = [
  ['icon-192.png', 192, {}],
  ['icon-512.png', 512, {}],
  ['icon-512-maskable.png', 512, { maskable: true }],
  ['apple-touch-icon.png', 180, {}],
];
for (const [name, size, opts] of targets) {
  writeFileSync(join(OUT, name), drawIcon(size, opts));
  console.log('wrote', name, size + 'x' + size);
}
