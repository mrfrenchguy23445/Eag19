#!/usr/bin/env python3
"""Compile a small boolean/map fixture and validate the actual generated WASM in V8."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
os.chdir(root)
cache = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle"))) / "caches/modules-2/files-2.1"
version = "0.12.1-EAGLER-R3"
local = root / "gradle/local-libs/org/teavm"
jars = [local / name / version / f"{name}-{version}.jar" for name in ("teavm-core", "teavm-classlib")]
jars += [p for p in cache.rglob("*.jar") if "-sources" not in p.name and "-javadoc" not in p.name
         and ("/org.teavm/" not in str(p) or f"/{version}/" in str(p))]

def run(command):
    result = subprocess.run(command)
    if result.returncode:
        raise SystemExit(result.returncode)

with tempfile.TemporaryDirectory(prefix="eag19-compiler-") as directory:
    cp = os.pathsep.join(map(str, [*jars, directory]))
    source = root / "scripts/verify/compiler"
    run(["javac", "-cp", cp, "-d", directory, *map(str, source.glob("*.java"))])
    run(["java", "-cp", cp, "InferenceJoinVerify"])
    for fixture in ("BooleanFixture", "NestedCallbackFixture"):
        output = str(Path(directory) / fixture)
        run(["java", "-Xmx2g", "-cp", cp, "BuildFixture", output, fixture])
        run(["node", "-e", "const fs=require('fs');WebAssembly.compile(fs.readFileSync(process.argv[1])).then(()=>console.log('WASM compiler regression passed'),e=>{console.error(e);process.exitCode=1});", str(Path(output) / "fixture.wasm")])
