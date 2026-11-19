# exclamationstudios site

Static landing page for the Eaglercraft 1.19.4 client-side V5/Via port.

The public repository ships the platform-neutral bridge seam and this site. The
GPLv3 Via binaries and WASM adaptations are maintainer-local and gitignored; see
[`../THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).

- `index.html` — the single page
- `styles.css` — print-shop theme: yellow, black ink, hard shadows, square corners
- `launcher.js` — launcher tab and protocol-note behaviour
- `serve-site.mjs` (in `../scripts/`) — dependency-free preview server on port 8123

## Preview

```bat
node scripts/serve-site.mjs
```

Then open <http://localhost:8123>.

## Hosting

Serve the repository root so the real artifact links resolve:

- `../sites-web/public/index.html` — chunked web launcher
- `../target_teavm_wasm_gc/javascript_dist/EaglercraftX_1.19.4_WASM_Offline.html` — single-file offline client
- `../sites-web/probe/public/index.html` — reduced probe build

If deploying only this directory, copy those targets beside it and update the links in `index.html`.

Regenerate the silent web launcher and offline file with:

```bat
node scripts/make-web-bundle.mjs
node scripts/make-offline-html.mjs
```

Both generators omit the 98 MiB sound pack by default. Add `--sounds` to either
command if you explicitly want the legacy sound build back.
