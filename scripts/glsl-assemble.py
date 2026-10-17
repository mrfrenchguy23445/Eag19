#!/usr/bin/env python3
"""Assemble the vanilla core shaders exactly as the game does and emit a
headless-Chromium page that compiles every one of them under WebGL2 / GLSL ES
3.00. Used by scripts/validate-shaders.sh.

This mirrors two things that live in the JVM:

  * com.mojang.blaze3d.preprocessor.GlslPreprocessor.process - inlines
    #moj_import, comments out the imported files' #version so only the top-level
    one remains.
  * PlatformOpenGL.translateVanillaGlsl - rewrites "#version 150" to
    "#version 300 es" + precision, and applies the desktop-GLSL -> ES 3.00
    implicit-conversion fixes.

KEEP THE translate() REPLACEMENTS IN SYNC WITH
src/wasm-gc-teavm/java/.../PlatformOpenGL.translateVanillaGlsl. If they drift,
this tool silently stops predicting the real build.
"""
import argparse
import json
import os
import re
import sys

IMPORT_RE = re.compile(r'#moj_import\s*(?:"([^"]+)"|<([^>]+)>)')


def find_default_root():
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(here, '..', 'desktopRuntime', 'resources', 'assets', 'minecraft', 'shaders')


def load(root):
    files = {}
    for dirpath, _, names in os.walk(root):
        for n in names:
            p = os.path.join(dirpath, n)
            rel = os.path.relpath(p, root).replace(os.sep, '/')
            with open(p, 'r', encoding='utf-8', errors='replace') as f:
                files[rel] = f.read()
    return files


def comment_first_version(s):
    m = re.search(r'#\s*version\s+\d+', s)
    if not m:
        return s
    return s[:m.start()] + '/*' + s[m.start():m.end()] + '*/' + s[m.end():]


def expand(files, text, base, imported):
    def repl(m):
        quoted = m.group(1) is not None
        imp = m.group(1) if quoted else m.group(2)
        if quoted:
            full = os.path.normpath(os.path.join(base, imp))
        else:
            full = os.path.normpath(os.path.join('include', imp))
        full = full.replace(os.sep, '/')
        if full in imported or full not in files:
            return '/*#moj_import ' + imp + '*/'
        imported.add(full)
        sub = expand(files, files[full], os.path.dirname(full), imported)
        return comment_first_version(sub)
    return IMPORT_RE.sub(repl, text)


def translate(s):
    """Must match PlatformOpenGL.translateVanillaGlsl."""
    i = s.find('#version')
    if i < 0:
        return s
    j = s.find('\n', i)
    if j < 0:
        j = len(s)
    body = s[j:]
    body = body.replace('uv / 256.0', 'vec2(uv) / 256.0')
    body = body.replace('texCoord2 = UV2;', 'texCoord2 = vec2(UV2);')
    return s[:i] + '#version 300 es\nprecision highp float;\nprecision highp int;' + body


PAGE = """<!doctype html>
<html><head><meta charset="utf-8"><title>glslcheck</title></head>
<body><pre id="out">running</pre>
<script src="shaders.js"></script>
<script>
(function () {
	var out = document.getElementById('out');
	var gl = document.createElement('canvas').getContext('webgl2');
	if (!gl) { out.textContent = 'GLSLCHECK_RESULT NO_WEBGL2'; return; }
	var lines = [], nfail = 0, n = 0;
	for (var name in window.SHADERS) {
		var isVert = /\\.vsh$/.test(name);
		var sh = gl.createShader(isVert ? gl.VERTEX_SHADER : gl.FRAGMENT_SHADER);
		gl.shaderSource(sh, window.SHADERS[name]);
		gl.compileShader(sh);
		n++;
		if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
			nfail++;
			lines.push('FAIL ' + name + '\\n' + gl.getShaderInfoLog(sh));
		}
		gl.deleteShader(sh);
	}
	out.textContent = 'GLSLCHECK_RESULT total=' + n + ' failed=' + nfail +
		(nfail === 0 ? ' ALL_OK' : '') + '\\n' + lines.join('\\n');
})();
</script></body></html>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default=find_default_root())
    ap.add_argument('--out', default='/tmp/opencode/glslcheck')
    args = ap.parse_args()

    if not os.path.isdir(args.root):
        print('shader root not found: ' + args.root, file=sys.stderr)
        return 2
    os.makedirs(args.out, exist_ok=True)
    files = load(args.root)

    shaders = {}
    for name in sorted(files):
        if not re.match(r'core/[^/]+\.(vsh|fsh)$', name):
            continue
        imported = set()
        assembled = expand(files, files[name], os.path.dirname(name), imported)
        shaders['shaders/' + name] = translate(assembled)

    with open(os.path.join(args.out, 'shaders.js'), 'w', encoding='utf-8') as f:
        f.write('window.SHADERS = ' + json.dumps(shaders) + ';\n')
    with open(os.path.join(args.out, 'index.html'), 'w', encoding='utf-8') as f:
        f.write(PAGE)
    print('assembled %d core shaders -> %s' % (len(shaders), args.out))
    return 0


if __name__ == '__main__':
    sys.exit(main())
