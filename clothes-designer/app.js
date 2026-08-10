'use strict';
/* Threadboard — a clothes-designing PWA. Vanilla JS, no dependencies.
   Everything renders to one <canvas>: a garment silhouette (front/back) with a
   clipped "print area" holding draggable/scalable/rotatable design layers. */

/* ------------------------------------------------------------------ *
 * Garment library
 * Outlines are authored in the 880x1040 canvas coordinate space.
 * ------------------------------------------------------------------ */
const GARMENTS = {
  tee: {
    name: 'T-Shirt',
    outline:
      'M 318 208 L 366 196 Q 440 252 514 196 L 562 208 L 726 288 Q 736 300 728 316 ' +
      'L 690 372 Q 682 386 666 380 L 612 344 L 636 872 Q 638 894 616 896 L 264 896 ' +
      'Q 242 894 244 872 L 268 344 L 214 380 Q 198 386 190 372 L 152 316 Q 144 300 154 288 Z',
    behind: [],
    frontDetails: [{ d: 'M 372 206 Q 440 262 508 206', stroke: true, width: 6 }],
    backDetails: [{ d: 'M 372 202 Q 440 226 508 202', stroke: true, width: 6 }],
    backNeckFill: 'M 366 196 Q 440 252 514 196 Q 440 224 366 196 Z',
    print: { front: { x: 320, y: 352, w: 240, h: 320 }, back: { x: 300, y: 320, w: 280, h: 400 } },
  },
  longsleeve: {
    name: 'Long Sleeve',
    outline:
      'M 366 196 Q 440 252 514 196 L 560 206 L 700 270 L 760 650 Q 762 674 738 672 ' +
      'L 700 664 L 620 350 L 636 872 Q 638 894 616 896 L 264 896 Q 242 894 244 872 ' +
      'L 260 350 L 180 664 L 142 672 Q 118 674 120 650 L 160 270 L 320 206 Z',
    behind: [],
    frontDetails: [
      { d: 'M 372 206 Q 440 262 508 206', stroke: true, width: 6 },
      { d: 'M 700 664 L 738 672', stroke: true, width: 6 },
      { d: 'M 142 672 L 180 664', stroke: true, width: 6 },
    ],
    backDetails: [{ d: 'M 372 202 Q 440 226 508 202', stroke: true, width: 6 }],
    backNeckFill: 'M 366 196 Q 440 252 514 196 Q 440 224 366 196 Z',
    print: { front: { x: 322, y: 360, w: 236, h: 300 }, back: { x: 300, y: 330, w: 280, h: 380 } },
  },
  tank: {
    name: 'Tank Top',
    outline:
      'M 388 196 Q 440 240 492 196 L 516 196 L 548 214 Q 596 300 588 430 L 600 884 ' +
      'Q 602 900 580 896 L 300 896 Q 278 900 280 884 L 292 430 Q 284 300 332 214 L 364 196 Z',
    behind: [],
    frontDetails: [{ d: 'M 388 198 Q 440 244 492 198', stroke: true, width: 6 }],
    backDetails: [{ d: 'M 364 200 Q 440 218 516 200', stroke: true, width: 6 }],
    backNeckFill: 'M 388 196 Q 440 240 492 196 Q 440 214 388 196 Z',
    print: { front: { x: 332, y: 300, w: 216, h: 340 }, back: { x: 316, y: 280, w: 248, h: 400 } },
  },
  hoodie: {
    name: 'Hoodie',
    outline:
      'M 352 214 Q 440 300 528 214 L 566 226 L 704 292 L 762 660 Q 764 684 740 682 ' +
      'L 702 674 L 624 360 L 640 872 Q 642 894 620 896 L 260 896 Q 238 894 240 872 ' +
      'L 256 360 L 178 674 L 140 682 Q 116 684 118 660 L 176 292 L 314 226 Z',
    behind: [
      { d: 'M 352 214 Q 296 116 440 108 Q 584 116 528 214 Q 440 262 352 214 Z', fill: 'shade' },
    ],
    frontDetails: [
      { d: 'M 352 214 Q 440 300 528 214', stroke: true, width: 7 },
      { d: 'M 416 236 L 410 336', stroke: true, width: 6 },
      { d: 'M 464 236 L 470 336', stroke: true, width: 6 },
      { d: 'M 348 632 L 532 632 L 552 764 L 328 764 Z', stroke: true, width: 6 },
      { d: 'M 348 664 L 388 664', stroke: true, width: 6 },
      { d: 'M 532 664 L 492 664', stroke: true, width: 6 },
      { d: 'M 702 674 L 740 682', stroke: true, width: 6 },
      { d: 'M 140 682 L 178 674', stroke: true, width: 6 },
    ],
    backDetails: [{ d: 'M 352 214 Q 440 252 528 214', stroke: true, width: 7 }],
    backNeckFill: 'M 352 214 Q 440 300 528 214 Q 440 248 352 214 Z',
    print: { front: { x: 356, y: 372, w: 168, h: 232 }, back: { x: 300, y: 340, w: 280, h: 380 } },
  },
  tote: {
    name: 'Tote Bag',
    outline: 'M 262 330 L 618 330 L 636 884 Q 636 902 618 902 L 262 902 Q 244 902 244 884 Z',
    behind: [
      { d: 'M 322 330 C 316 206 404 206 404 330', stroke: true, width: 20, raw: true },
      { d: 'M 476 330 C 476 206 564 206 560 330', stroke: true, width: 20, raw: true },
    ],
    frontDetails: [{ d: 'M 262 356 L 618 356', stroke: true, width: 5 }],
    backDetails: [{ d: 'M 262 356 L 618 356', stroke: true, width: 5 }],
    backNeckFill: null,
    print: { front: { x: 300, y: 420, w: 280, h: 400 }, back: { x: 300, y: 420, w: 280, h: 400 } },
  },
};

const FABRICS = ['#f4f4f5', '#111827', '#ef4444', '#f59e0b', '#facc15', '#22c55e',
  '#0ea5e9', '#6366f1', '#a855f7', '#ec4899', '#78350f', '#14b8a6', '#e5e7eb', '#475569'];

const FONTS = [
  { label: 'Sans', css: 'Arial, sans-serif' },
  { label: 'Rounded', css: '"Trebuchet MS", sans-serif' },
  { label: 'Serif', css: 'Georgia, serif' },
  { label: 'Slab', css: '"Rockwell", "Courier New", monospace' },
  { label: 'Mono', css: '"Courier New", monospace' },
  { label: 'Display', css: 'Impact, "Arial Black", sans-serif' },
  { label: 'Comic', css: '"Comic Sans MS", "Trebuchet MS", sans-serif' },
  { label: 'Script', css: '"Brush Script MT", "Segoe Script", cursive' },
];

const STICKERS = ['⭐', '❤️', '🔥', '⚡', '🌈', '☀️', '🌙', '🍕', '🎧', '🐱', '🐉', '💀', '✌️', '🌸', '👑', '🛹'];

/* ------------------------------------------------------------------ *
 * State
 * ------------------------------------------------------------------ */
const state = {
  garment: 'tee',
  view: 'front',
  fabric: '#f4f4f5',
  layers: [],
  selected: null,
};
let uid = 1;
const nextId = () => uid++;

const history = { stack: [], index: -1 };
const canvas = document.getElementById('canvas');
let ctx = canvas.getContext('2d'); // swappable so we can render into an offscreen canvas
const W = canvas.width, H = canvas.height;
const pathCache = new Map();
const p2d = (d) => { if (!pathCache.has(d)) pathCache.set(d, new Path2D(d)); return pathCache.get(d); };

/* ------------------------------------------------------------------ *
 * Colour helpers
 * ------------------------------------------------------------------ */
function hexToRgb(hex) {
  const m = hex.replace('#', '');
  const n = m.length === 3 ? m.split('').map((c) => c + c).join('') : m;
  return [parseInt(n.slice(0, 2), 16), parseInt(n.slice(2, 4), 16), parseInt(n.slice(4, 6), 16)];
}
function luminance(hex) {
  const [r, g, b] = hexToRgb(hex).map((v) => v / 255);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
function mix(hex, target, t) {
  const a = hexToRgb(hex), b = hexToRgb(target);
  return `rgb(${a.map((v, i) => Math.round(v + (b[i] - v) * t)).join(',')})`;
}
const shadeFor = (hex) => (luminance(hex) > 0.5 ? mix(hex, '#000000', 0.16) : mix(hex, '#ffffff', 0.22));
const seamColor = (hex) => (luminance(hex) > 0.5 ? 'rgba(0,0,0,0.16)' : 'rgba(255,255,255,0.18)');

/* ------------------------------------------------------------------ *
 * Layer geometry
 * ------------------------------------------------------------------ */
function layerBox(layer) {
  if (layer.type === 'text' || layer.type === 'sticker') {
    ctx.save();
    ctx.font = fontString(layer);
    const lines = String(layer.text || '').split('\n');
    let w = 1;
    for (const ln of lines) w = Math.max(w, ctx.measureText(ln || ' ').width);
    ctx.restore();
    const lh = layer.scale * 1.18;
    return { w: Math.max(w, layer.scale * 0.5), h: lh * lines.length };
  }
  if (layer.type === 'image') {
    const ar = layer.aspect || 1;
    return { w: layer.scale, h: layer.scale / ar };
  }
  if (layer.type === 'rect') return { w: layer.scale, h: layer.scale * 0.66 };
  return { w: layer.scale, h: layer.scale }; // circle, triangle, star
}
function fontString(layer) {
  const f = FONTS[layer.font || 0].css;
  return `${layer.italic ? 'italic ' : ''}${layer.bold ? '700 ' : '400 '}${layer.scale}px ${f}`;
}

/* ------------------------------------------------------------------ *
 * Rendering
 * ------------------------------------------------------------------ */
function currentPrint() {
  return GARMENTS[state.garment].print[state.view];
}

function draw() {
  ctx.clearRect(0, 0, W, H);
  const g = GARMENTS[state.garment];
  const fabric = state.fabric;
  const shade = shadeFor(fabric);
  const seam = seamColor(fabric);

  // behind pieces (hood, handles)
  for (const b of g.behind) drawDetail(b, fabric, shade, seam);

  // main silhouette with soft vertical gradient
  const grad = ctx.createLinearGradient(0, 150, 0, 920);
  grad.addColorStop(0, luminance(fabric) > 0.5 ? mix(fabric, '#ffffff', 0.10) : mix(fabric, '#ffffff', 0.06));
  grad.addColorStop(1, mix(fabric, '#000000', 0.10));
  ctx.fillStyle = grad;
  ctx.fill(p2d(g.outline));
  ctx.lineJoin = 'round';
  ctx.strokeStyle = luminance(fabric) > 0.5 ? 'rgba(0,0,0,0.25)' : 'rgba(0,0,0,0.4)';
  ctx.lineWidth = 3;
  ctx.stroke(p2d(g.outline));

  // raise back collar
  if (state.view === 'back' && g.backNeckFill) { ctx.fillStyle = fabric; ctx.fill(p2d(g.backNeckFill)); }

  // garment details
  const details = state.view === 'front' ? g.frontDetails : g.backDetails;
  for (const d of details) drawDetail(d, fabric, shade, seam);

  // print area (clipped) + design layers
  const pr = currentPrint();
  ctx.save();
  ctx.beginPath();
  ctx.rect(pr.x, pr.y, pr.w, pr.h);
  ctx.clip();
  for (const layer of state.layers) drawLayer(layer);
  ctx.restore();

  // selection UI on top (unclipped)
  if (state.selected != null) {
    const layer = state.layers.find((l) => l.id === state.selected);
    if (layer) drawSelection(layer);
  }
}

function drawDetail(d, fabric, shade, seam) {
  const path = p2d(d.d);
  if (d.fill) {
    ctx.fillStyle = d.fill === 'shade' ? shade : d.fill;
    ctx.fill(path);
  }
  if (d.stroke) {
    ctx.strokeStyle = d.raw ? shade : seam;
    ctx.lineWidth = d.width || 5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke(path);
  }
}

function drawLayer(layer) {
  const { w, h } = layerBox(layer);
  ctx.save();
  ctx.globalAlpha = (layer.opacity ?? 100) / 100;
  ctx.translate(layer.x, layer.y);
  ctx.rotate((layer.rotation || 0) * Math.PI / 180);

  switch (layer.type) {
    case 'text':
    case 'sticker': {
      ctx.font = fontString(layer);
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = layer.color || '#111827';
      const lines = String(layer.text || '').split('\n');
      const lh = layer.scale * 1.18;
      const start = -((lines.length - 1) * lh) / 2;
      lines.forEach((ln, i) => ctx.fillText(ln, 0, start + i * lh));
      break;
    }
    case 'image': {
      if (layer._img && layer._img.complete) ctx.drawImage(layer._img, -w / 2, -h / 2, w, h);
      break;
    }
    case 'rect': {
      ctx.fillStyle = layer.color;
      roundRect(ctx, -w / 2, -h / 2, w, h, Math.min(w, h) * 0.12);
      ctx.fill();
      break;
    }
    case 'circle': {
      ctx.fillStyle = layer.color;
      ctx.beginPath(); ctx.arc(0, 0, w / 2, 0, Math.PI * 2); ctx.fill();
      break;
    }
    case 'triangle': {
      ctx.fillStyle = layer.color;
      ctx.beginPath();
      ctx.moveTo(0, -h / 2); ctx.lineTo(w / 2, h / 2); ctx.lineTo(-w / 2, h / 2);
      ctx.closePath(); ctx.fill();
      break;
    }
    case 'star': {
      ctx.fillStyle = layer.color;
      star(ctx, 0, 0, w / 2, w / 4, 5); ctx.fill();
      break;
    }
  }
  ctx.restore();
}

function drawSelection(layer) {
  const { w, h } = layerBox(layer);
  const hw = w / 2 + 8, hh = h / 2 + 8;
  ctx.save();
  ctx.translate(layer.x, layer.y);
  ctx.rotate((layer.rotation || 0) * Math.PI / 180);
  ctx.strokeStyle = '#6366f1';
  ctx.lineWidth = 2;
  ctx.setLineDash([7, 5]);
  ctx.strokeRect(-hw, -hh, hw * 2, hh * 2);
  ctx.setLineDash([]);
  // rotate handle
  ctx.beginPath(); ctx.moveTo(0, -hh); ctx.lineTo(0, -hh - 30); ctx.stroke();
  handleDot(0, -hh - 30, '#f472b6');
  // scale handle (bottom-right)
  handleDot(hw, hh, '#6366f1');
  ctx.restore();
}
function handleDot(x, y, color) {
  ctx.beginPath(); ctx.arc(x, y, 9, 0, Math.PI * 2);
  ctx.fillStyle = '#fff'; ctx.fill();
  ctx.lineWidth = 3; ctx.strokeStyle = color; ctx.stroke();
}
function roundRect(c, x, y, w, h, r) {
  c.beginPath();
  c.moveTo(x + r, y);
  c.arcTo(x + w, y, x + w, y + h, r);
  c.arcTo(x + w, y + h, x, y + h, r);
  c.arcTo(x, y + h, x, y, r);
  c.arcTo(x, y, x + w, y, r);
  c.closePath();
}
function star(c, cx, cy, outer, inner, points) {
  c.beginPath();
  for (let i = 0; i < points * 2; i++) {
    const r = i % 2 === 0 ? outer : inner;
    const a = (Math.PI / points) * i - Math.PI / 2;
    const x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
    i === 0 ? c.moveTo(x, y) : c.lineTo(x, y);
  }
  c.closePath();
}

/* ------------------------------------------------------------------ *
 * Pointer interaction (move / scale / rotate)
 * ------------------------------------------------------------------ */
let drag = null;

function canvasPoint(e) {
  const r = canvas.getBoundingClientRect();
  return { x: (e.clientX - r.left) * (W / r.width), y: (e.clientY - r.top) * (H / r.height) };
}
function toLocal(layer, p) {
  const dx = p.x - layer.x, dy = p.y - layer.y;
  const a = -(layer.rotation || 0) * Math.PI / 180;
  return { x: dx * Math.cos(a) - dy * Math.sin(a), y: dx * Math.sin(a) + dy * Math.cos(a) };
}
function handlePositions(layer) {
  const { w, h } = layerBox(layer);
  const hw = w / 2 + 8, hh = h / 2 + 8;
  const a = (layer.rotation || 0) * Math.PI / 180, cos = Math.cos(a), sin = Math.sin(a);
  const rot = (lx, ly) => ({ x: layer.x + lx * cos - ly * sin, y: layer.y + lx * sin + ly * cos });
  return { scale: rot(hw, hh), rotate: rot(0, -hh - 30) };
}

canvas.addEventListener('pointerdown', (e) => {
  e.preventDefault();
  canvas.setPointerCapture(e.pointerId);
  const p = canvasPoint(e);
  const sel = state.layers.find((l) => l.id === state.selected);

  if (sel) {
    const hp = handlePositions(sel);
    if (Math.hypot(p.x - hp.rotate.x, p.y - hp.rotate.y) < 16) {
      drag = { mode: 'rotate', layer: sel }; return;
    }
    if (Math.hypot(p.x - hp.scale.x, p.y - hp.scale.y) < 16) {
      const { w, h } = layerBox(sel);
      drag = { mode: 'scale', layer: sel, startScale: sel.scale, startDist: Math.hypot(w / 2, h / 2) };
      return;
    }
  }
  // hit test top-most first
  for (let i = state.layers.length - 1; i >= 0; i--) {
    const l = state.layers[i];
    const loc = toLocal(l, p);
    const { w, h } = layerBox(l);
    if (Math.abs(loc.x) <= w / 2 + 8 && Math.abs(loc.y) <= h / 2 + 8) {
      selectLayer(l.id);
      drag = { mode: 'move', layer: l, offx: p.x - l.x, offy: p.y - l.y };
      draw();
      return;
    }
  }
  selectLayer(null);
  draw();
});

canvas.addEventListener('pointermove', (e) => {
  if (!drag) return;
  const p = canvasPoint(e);
  const l = drag.layer;
  if (drag.mode === 'move') {
    l.x = p.x - drag.offx; l.y = p.y - drag.offy;
  } else if (drag.mode === 'rotate') {
    let deg = Math.atan2(p.y - l.y, p.x - l.x) * 180 / Math.PI + 90;
    if (e.shiftKey) deg = Math.round(deg / 15) * 15;
    l.rotation = ((deg + 180) % 360) - 180;
  } else if (drag.mode === 'scale') {
    const dist = Math.hypot(p.x - l.x, p.y - l.y);
    l.scale = Math.max(12, Math.min(360, drag.startScale * (dist / drag.startDist)));
  }
  draw();
  syncInspector();
});

function endDrag() {
  if (!drag) return;
  drag = null;
  pushHistory();
  autosave();
}
canvas.addEventListener('pointerup', endDrag);
canvas.addEventListener('pointercancel', endDrag);

// double-click text to edit inline via prompt fallback (focus textarea)
canvas.addEventListener('dblclick', () => {
  const l = state.layers.find((x) => x.id === state.selected);
  if (l && l.type === 'text') document.getElementById('p-text').focus();
});

/* ------------------------------------------------------------------ *
 * Layer operations
 * ------------------------------------------------------------------ */
function centreOfPrint() {
  const pr = currentPrint();
  return { x: pr.x + pr.w / 2, y: pr.y + pr.h / 2 };
}
function addLayer(partial) {
  const c = centreOfPrint();
  const base = { id: nextId(), x: c.x, y: c.y, rotation: 0, opacity: 100, scale: 120 };
  const layer = Object.assign(base, partial);
  state.layers.push(layer);
  selectLayer(layer.id);
  pushHistory();
  renderLayerList();
  draw();
  autosave();
  return layer;
}
function addText() {
  addLayer({ type: 'text', text: 'YOUR TEXT', color: '#111827', font: 5, bold: true, italic: false, scale: 84 });
}
function addSticker(emoji) {
  addLayer({ type: 'sticker', text: emoji, color: '#000000', font: 0, scale: 120 });
}
function addShape(kind) {
  const colors = { rect: '#6366f1', circle: '#f472b6', triangle: '#22c55e', star: '#f59e0b' };
  addLayer({ type: kind, color: colors[kind] || '#6366f1', scale: 150 });
}
function addImageFromDataUrl(dataUrl) {
  const img = new Image();
  img.onload = () => {
    const ar = img.naturalWidth / img.naturalHeight || 1;
    const layer = addLayer({ type: 'image', src: dataUrl, aspect: ar, scale: 200 });
    layer._img = img;
    draw();
  };
  img.src = dataUrl;
}
function selectLayer(id) {
  state.selected = id;
  renderLayerList();
  syncInspector();
}
function deleteLayer(id) {
  state.layers = state.layers.filter((l) => l.id !== id);
  if (state.selected === id) state.selected = null;
  pushHistory(); renderLayerList(); syncInspector(); draw(); autosave();
}
function duplicateLayer(id) {
  const l = state.layers.find((x) => x.id === id);
  if (!l) return;
  const copy = Object.assign({}, l, { id: nextId(), x: l.x + 24, y: l.y + 24, _img: l._img });
  state.layers.push(copy);
  selectLayer(copy.id);
  pushHistory(); renderLayerList(); draw(); autosave();
}
function reorder(id, dir) {
  const i = state.layers.findIndex((l) => l.id === id);
  const j = i + dir;
  if (i < 0 || j < 0 || j >= state.layers.length) return;
  [state.layers[i], state.layers[j]] = [state.layers[j], state.layers[i]];
  pushHistory(); renderLayerList(); draw(); autosave();
}

/* ------------------------------------------------------------------ *
 * History (undo / redo)
 * ------------------------------------------------------------------ */
function snapshot() {
  return JSON.stringify({
    garment: state.garment, view: state.view, fabric: state.fabric, selected: state.selected,
    layers: state.layers.map(({ _img, ...l }) => l),
  });
}
function pushHistory() {
  history.stack = history.stack.slice(0, history.index + 1);
  history.stack.push(snapshot());
  if (history.stack.length > 60) history.stack.shift();
  history.index = history.stack.length - 1;
  updateHistoryButtons();
}
function restore(json) {
  const s = JSON.parse(json);
  state.garment = s.garment; state.view = s.view; state.fabric = s.fabric; state.selected = s.selected;
  state.layers = s.layers.map((l) => {
    if (l.type === 'image' && l.src) { const img = new Image(); img.src = l.src; l._img = img; }
    return l;
  });
  uid = state.layers.reduce((m, l) => Math.max(m, l.id), 0) + 1;
  syncControls(); renderLayerList(); syncInspector(); draw();
}
function undo() { if (history.index > 0) { history.index--; restore(history.stack[history.index]); updateHistoryButtons(); autosave(); } }
function redo() { if (history.index < history.stack.length - 1) { history.index++; restore(history.stack[history.index]); updateHistoryButtons(); autosave(); } }
function updateHistoryButtons() {
  document.getElementById('btn-undo').disabled = history.index <= 0;
  document.getElementById('btn-redo').disabled = history.index >= history.stack.length - 1;
}

/* ------------------------------------------------------------------ *
 * Inspector + layer list UI
 * ------------------------------------------------------------------ */
const $ = (id) => document.getElementById(id);

function renderLayerList() {
  const ul = $('layer-list');
  ul.innerHTML = '';
  const icons = { text: 'T', sticker: '☺', image: '🖼', rect: '▭', circle: '⬤', triangle: '▲', star: '★' };
  // show top layer first
  [...state.layers].reverse().forEach((l) => {
    const li = document.createElement('li');
    li.className = 'layer-item' + (l.id === state.selected ? ' active' : '');
    const label = l.type === 'text' ? (l.text || 'Text').slice(0, 18)
      : l.type === 'sticker' ? 'Sticker ' + l.text
      : l.type === 'image' ? 'Photo' : l.type[0].toUpperCase() + l.type.slice(1);
    li.innerHTML = `<span class="li-icon">${icons[l.type] || '?'}</span>` +
      `<span class="li-name">${escapeHtml(label)}</span>` +
      `<button class="li-del" title="Delete" aria-label="Delete layer">🗑</button>`;
    li.addEventListener('click', (e) => {
      if (e.target.classList.contains('li-del')) { deleteLayer(l.id); return; }
      selectLayer(l.id); draw();
    });
    ul.appendChild(li);
  });
  $('layers-empty').style.display = state.layers.length ? 'none' : 'block';
}

function syncInspector() {
  const l = state.layers.find((x) => x.id === state.selected);
  const props = $('properties');
  if (!l) { props.hidden = true; return; }
  props.hidden = false;
  $('prop-title').textContent = ({ text: 'Text', sticker: 'Sticker', image: 'Photo', rect: 'Box', circle: 'Circle', triangle: 'Triangle', star: 'Star' })[l.type];

  const showFor = (types) => types.includes(l.type);
  document.querySelectorAll('.prop-row[data-for="text"]').forEach((r) => r.style.display = showFor(['text']) ? '' : 'none');
  const fillRows = document.querySelectorAll('.prop-row[data-for="fill"]');
  fillRows.forEach((r) => r.style.display = ['text', 'rect', 'circle', 'triangle', 'star'].includes(l.type) ? '' : 'none');

  if (l.type === 'text') {
    $('p-text').value = l.text || '';
    $('p-font').value = String(l.font || 0);
    $('p-bold').checked = !!l.bold;
    $('p-italic').checked = !!l.italic;
  }
  if ($('p-color').closest('.prop-row').style.display !== 'none') $('p-color').value = toHex(l.color || '#111827');
  $('p-scale').value = Math.round(l.scale);
  $('p-scale-val').textContent = Math.round(l.scale);
  $('p-rot').value = Math.round(l.rotation || 0);
  $('p-rot-val').textContent = Math.round(l.rotation || 0) + '°';
  $('p-opacity').value = l.opacity ?? 100;
  $('p-opacity-val').textContent = (l.opacity ?? 100) + '%';
}

function bindInspector() {
  const withSel = (fn) => { const l = state.layers.find((x) => x.id === state.selected); if (l) { fn(l); draw(); } };
  $('p-text').addEventListener('input', () => withSel((l) => { l.text = $('p-text').value; renderLayerList(); }));
  $('p-text').addEventListener('change', () => { pushHistory(); autosave(); });
  $('p-font').addEventListener('change', () => withSel((l) => { l.font = +$('p-font').value; pushHistory(); autosave(); }));
  $('p-bold').addEventListener('change', () => withSel((l) => { l.bold = $('p-bold').checked; pushHistory(); autosave(); }));
  $('p-italic').addEventListener('change', () => withSel((l) => { l.italic = $('p-italic').checked; pushHistory(); autosave(); }));
  $('p-color').addEventListener('input', () => withSel((l) => { l.color = $('p-color').value; }));
  $('p-color').addEventListener('change', () => { pushHistory(); autosave(); });
  const live = (id, label, prop, fmt) => {
    $(id).addEventListener('input', () => withSel((l) => { l[prop] = +$(id).value; $(label).textContent = fmt(+$(id).value); }));
    $(id).addEventListener('change', () => { pushHistory(); autosave(); });
  };
  live('p-scale', 'p-scale-val', 'scale', (v) => Math.round(v));
  live('p-rot', 'p-rot-val', 'rotation', (v) => Math.round(v) + '°');
  live('p-opacity', 'p-opacity-val', 'opacity', (v) => v + '%');
  $('p-delete').addEventListener('click', () => state.selected != null && deleteLayer(state.selected));
  $('p-dup').addEventListener('click', () => state.selected != null && duplicateLayer(state.selected));
  $('p-front').addEventListener('click', () => state.selected != null && reorder(state.selected, +1));
  $('p-back').addEventListener('click', () => state.selected != null && reorder(state.selected, -1));
}

/* ------------------------------------------------------------------ *
 * Left panel controls
 * ------------------------------------------------------------------ */
function buildGarmentPicker() {
  const wrap = $('garment-picker');
  wrap.innerHTML = '';
  for (const [id, g] of Object.entries(GARMENTS)) {
    const btn = document.createElement('button');
    btn.className = 'chip' + (id === state.garment ? ' active' : '');
    btn.dataset.garment = id;
    btn.innerHTML =
      `<svg viewBox="120 120 640 800" aria-hidden="true"><path d="${g.outline}" fill="currentColor"/></svg>` +
      `<span>${g.name}</span>`;
    btn.style.color = id === state.garment ? '#a5b4fc' : '#8b90b5';
    btn.addEventListener('click', () => {
      state.garment = id;
      buildGarmentPicker();
      pushHistory(); draw(); autosave();
    });
    wrap.appendChild(btn);
  }
}
function buildSwatches() {
  const wrap = $('fabric-swatches');
  wrap.innerHTML = '';
  FABRICS.forEach((c) => {
    const b = document.createElement('button');
    b.className = 'swatch' + (c.toLowerCase() === state.fabric.toLowerCase() ? ' active' : '');
    b.style.background = c;
    b.title = c;
    b.addEventListener('click', () => setFabric(c));
    wrap.appendChild(b);
  });
}
function setFabric(c) {
  state.fabric = c;
  $('fabric-custom').value = toHex(c);
  buildSwatches();
  pushHistory(); draw(); autosave();
}
function buildStickers() {
  const row = $('sticker-row');
  STICKERS.forEach((s) => {
    const b = document.createElement('button');
    b.textContent = s; b.title = 'Add ' + s;
    b.addEventListener('click', () => addSticker(s));
    row.appendChild(b);
  });
}
function buildFonts() {
  const sel = $('p-font');
  FONTS.forEach((f, i) => { const o = document.createElement('option'); o.value = i; o.textContent = f.label; sel.appendChild(o); });
}
function syncControls() {
  buildGarmentPicker();
  buildSwatches();
  $('fabric-custom').value = toHex(state.fabric);
  document.querySelectorAll('#view-toggle button').forEach((b) => b.classList.toggle('active', b.dataset.view === state.view));
}

/* ------------------------------------------------------------------ *
 * Export + persistence
 * ------------------------------------------------------------------ */
function renderToCanvas(scale = 2) {
  const prevSel = state.selected;
  state.selected = null;
  const off = document.createElement('canvas');
  off.width = W * scale; off.height = H * scale;
  const octx = off.getContext('2d');
  octx.scale(scale, scale);
  drawInto(octx);
  state.selected = prevSel;
  return off;
}
// draw everything into an arbitrary context (used for export/thumbnail)
function drawInto(c) {
  const saved = ctx;
  ctx = c;
  draw();
  ctx = saved;
}

function exportPng() {
  const off = renderToCanvas(2);
  off.toBlob((blob) => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${GARMENTS[state.garment].name.replace(/\s+/g, '-').toLowerCase()}-${state.view}.png`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    toast('Exported PNG');
  }, 'image/png');
}

const GKEY = 'threadboard:gallery';
const CKEY = 'threadboard:current';
function autosave() {
  try { localStorage.setItem(CKEY, snapshot()); } catch (_) {}
}
function loadAutosave() {
  try {
    const j = localStorage.getItem(CKEY);
    if (j) { restore(j); pushHistory(); return true; }
  } catch (_) {}
  return false;
}
function thumbnail() {
  const off = renderToCanvas(1);
  const t = document.createElement('canvas');
  t.width = 300; t.height = 354;
  const tc = t.getContext('2d');
  tc.fillStyle = '#ffffff'; tc.fillRect(0, 0, t.width, t.height);
  tc.drawImage(off, 120, 130, 640, 800, 6, 6, 288, 342);
  return t.toDataURL('image/png');
}
function getGallery() { try { return JSON.parse(localStorage.getItem(GKEY)) || []; } catch (_) { return []; } }
function saveDesign() {
  const gallery = getGallery();
  const entry = {
    id: 'd' + history.stack.length + '_' + gallery.length + '_' + (state.layers.length),
    name: GARMENTS[state.garment].name,
    date: new Date().toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }),
    thumb: thumbnail(),
    data: snapshot(),
  };
  gallery.unshift(entry);
  try { localStorage.setItem(GKEY, JSON.stringify(gallery.slice(0, 40))); toast('Design saved to gallery'); }
  catch (_) { toast('Storage full — could not save'); }
}
function openGallery() {
  const gallery = getGallery();
  const grid = $('gallery-grid');
  grid.innerHTML = '';
  $('gallery-empty').style.display = gallery.length ? 'none' : 'block';
  gallery.forEach((g) => {
    const card = document.createElement('div');
    card.className = 'gallery-card';
    card.innerHTML =
      `<img src="${g.thumb}" alt="${escapeHtml(g.name)} design" />` +
      `<div class="gc-date">${escapeHtml(g.name)} · ${escapeHtml(g.date)}</div>` +
      `<div class="gc-actions">
         <button class="btn primary" data-act="load">Open</button>
         <button class="btn danger" data-act="del">Delete</button>
       </div>`;
    card.querySelector('[data-act="load"]').addEventListener('click', () => {
      restore(g.data); pushHistory(); autosave();
      $('gallery-dialog').close(); toast('Loaded design');
    });
    card.querySelector('[data-act="del"]').addEventListener('click', () => {
      const rest = getGallery().filter((x) => x.id !== g.id);
      localStorage.setItem(GKEY, JSON.stringify(rest));
      openGallery();
    });
    grid.appendChild(card);
  });
  $('gallery-dialog').showModal();
}

/* ------------------------------------------------------------------ *
 * Small utilities
 * ------------------------------------------------------------------ */
function toHex(c) {
  if (!c) return '#000000';
  if (c[0] === '#') return c.length === 4 ? '#' + [...c.slice(1)].map((x) => x + x).join('') : c;
  const m = c.match(/\d+/g);
  if (!m) return '#000000';
  return '#' + m.slice(0, 3).map((n) => (+n).toString(16).padStart(2, '0')).join('');
}
function escapeHtml(s) { return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
let toastTimer;
function toast(msg) {
  const t = $('toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 1800);
}

/* ------------------------------------------------------------------ *
 * Wire up global controls + init
 * ------------------------------------------------------------------ */
function init() {
  buildFonts();
  buildStickers();
  bindInspector();

  document.querySelectorAll('#view-toggle button').forEach((b) => {
    b.addEventListener('click', () => {
      state.view = b.dataset.view;
      document.querySelectorAll('#view-toggle button').forEach((x) => x.classList.toggle('active', x === b));
      pushHistory(); draw(); autosave();
    });
  });
  $('fabric-custom').addEventListener('input', () => setFabric($('fabric-custom').value));
  document.querySelectorAll('.add-btn').forEach((b) => b.addEventListener('click', () => {
    const kind = b.dataset.add;
    if (kind === 'text') addText();
    else if (kind === 'image') $('file-input').click();
    else addShape(kind);
  }));
  $('file-input').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => addImageFromDataUrl(reader.result);
    reader.readAsDataURL(file);
    e.target.value = '';
  });

  $('btn-export').addEventListener('click', exportPng);
  $('btn-save').addEventListener('click', saveDesign);
  $('btn-gallery').addEventListener('click', openGallery);
  $('gallery-close').addEventListener('click', () => $('gallery-dialog').close());
  $('gallery-dialog').addEventListener('click', (e) => { if (e.target.id === 'gallery-dialog') $('gallery-dialog').close(); });
  $('btn-undo').addEventListener('click', undo);
  $('btn-redo').addEventListener('click', redo);

  // mobile tabs
  document.body.dataset.tab = 'stage';
  document.querySelectorAll('.mobile-tabs button').forEach((b) => b.addEventListener('click', () => {
    document.body.dataset.tab = b.dataset.tab;
    document.querySelectorAll('.mobile-tabs button').forEach((x) => x.classList.toggle('active', x === b));
  }));
  document.querySelector('.mobile-tabs button[data-tab="stage"]').classList.add('active');

  // keyboard
  window.addEventListener('keydown', (e) => {
    if (['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName)) return;
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') { e.preventDefault(); e.shiftKey ? redo() : undo(); }
    else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') { e.preventDefault(); redo(); }
    else if ((e.key === 'Delete' || e.key === 'Backspace') && state.selected != null) { e.preventDefault(); deleteLayer(state.selected); }
    else if (e.key === 'd' && (e.ctrlKey || e.metaKey) && state.selected != null) { e.preventDefault(); duplicateLayer(state.selected); }
    else if (e.key === 'Escape') { selectLayer(null); draw(); }
  });

  const params = new URLSearchParams(location.search);
  const restored = params.get('new') ? false : loadAutosave();
  if (!restored) { syncControls(); renderLayerList(); pushHistory(); }
  syncControls();
  draw();

  registerSW();
  setupInstall();
}

/* ------------------------------------------------------------------ *
 * PWA plumbing
 * ------------------------------------------------------------------ */
function registerSW() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
  }
}
function setupInstall() {
  let deferred = null;
  const btn = $('btn-install');
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault(); deferred = e; btn.hidden = false;
  });
  btn.addEventListener('click', async () => {
    if (!deferred) return;
    deferred.prompt();
    await deferred.userChoice;
    deferred = null; btn.hidden = true;
  });
  window.addEventListener('appinstalled', () => { btn.hidden = true; toast('Installed!'); });
}

document.addEventListener('DOMContentLoaded', init);
