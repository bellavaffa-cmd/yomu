// Lightweight PWA linter — no external deps.
// Validates the web manifest, that every referenced icon exists, and that the
// service worker's precache list points only at files that are actually present.
// Exits non-zero (with a list of problems) if anything is off.
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const errors = [];
const ok = [];
const fail = (m) => errors.push(m);
const pass = (m) => ok.push(m);
const rel = (p) => join(ROOT, p);

// ---- manifest ----------------------------------------------------------
let manifest;
try {
  manifest = JSON.parse(readFileSync(rel('manifest.webmanifest'), 'utf8'));
  pass('manifest.webmanifest is valid JSON');
} catch (e) {
  fail(`manifest.webmanifest is not valid JSON: ${e.message}`);
}

if (manifest) {
  const required = ['name', 'short_name', 'start_url', 'display', 'icons', 'theme_color', 'background_color'];
  for (const key of required) {
    if (manifest[key] == null || (Array.isArray(manifest[key]) && manifest[key].length === 0)) {
      fail(`manifest is missing required field: "${key}"`);
    }
  }
  if (Array.isArray(manifest.icons)) {
    for (const icon of manifest.icons) {
      if (!icon.src) { fail('manifest icon entry has no "src"'); continue; }
      if (!existsSync(rel(icon.src))) fail(`manifest icon file not found: ${icon.src}`);
    }
    const hasMaskable = manifest.icons.some((i) => /maskable/.test(i.purpose || ''));
    const has512 = manifest.icons.some((i) => (i.sizes || '').includes('512'));
    if (!hasMaskable) fail('manifest has no maskable icon (purpose: "maskable")');
    if (!has512) fail('manifest has no 512x512 icon');
    if (hasMaskable && has512) pass('manifest declares 512px and maskable icons, all files present');
  }
}

// ---- HTML wiring -------------------------------------------------------
if (existsSync(rel('index.html'))) {
  const html = readFileSync(rel('index.html'), 'utf8');
  if (!/rel=["']manifest["']/.test(html)) fail('index.html does not link the web manifest');
  else pass('index.html links the web manifest');
  if (!/serviceWorker/.test(readFileSync(rel('app.js'), 'utf8'))) fail('app.js does not register a service worker');
  else pass('app.js registers a service worker');
} else {
  fail('index.html is missing');
}

// ---- service worker precache -------------------------------------------
if (existsSync(rel('sw.js'))) {
  const sw = readFileSync(rel('sw.js'), 'utf8');
  const m = sw.match(/const\s+ASSETS\s*=\s*\[([\s\S]*?)\]/);
  if (!m) {
    fail('sw.js has no ASSETS precache array');
  } else {
    const assets = [...m[1].matchAll(/['"]([^'"]+)['"]/g)].map((x) => x[1]);
    let missing = 0;
    for (const a of assets) {
      const clean = a.replace(/^\.\//, '');
      if (clean === '' || clean === '/') continue; // the app root
      if (!existsSync(rel(clean))) { fail(`sw.js precaches a missing file: ${a}`); missing++; }
    }
    if (!missing) pass(`sw.js precache list is complete (${assets.length} entries)`);
  }
} else {
  fail('sw.js is missing');
}

// ---- report ------------------------------------------------------------
for (const m of ok) console.log(`  ✓ ${m}`);
if (errors.length) {
  console.error('\nPWA checks failed:');
  for (const m of errors) console.error(`  ✗ ${m}`);
  process.exit(1);
}
console.log('\nAll PWA checks passed.');
