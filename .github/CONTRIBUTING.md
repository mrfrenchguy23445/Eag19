# Contributing to Eag19

Thanks for helping port EaglercraftX to Minecraft 1.19.4. This is a large,
unusual codebase, so a few conventions go a long way.

## Buildability is the primary constraint

`./gradlew :compileJava` must stay green. CI enforces exactly this and nothing
more. Before opening a PR:

```bash
./gradlew :compileJava
```

It is one javac run over ~5,650 sources and takes several minutes. That is
expected — it is not a hang.

**Use JDK 17.** A newer JDK will not work; the toolchain is pinned and no
resolver is configured.

## Know which target you are touching

| Source directory | Compiled by | State |
|---|---|---|
| `src/main/` | root `:compileJava` | Builds. |
| `src/wasm-gc-teavm/` | `target_teavm_wasm_gc` | Builds (placeholder entry point). |
| `src/teavm/` | `target_teavm_javascript` | **Broken** — calls `Main.appMain()`. |
| `src/lwjgl/` | `target_lwjgl_desktop` | **Broken** — calls `Main.appMain()`. |

If your change touches `src/teavm/` or `src/lwjgl/`, CI will stay green while
that target remains uncompilable. Say so explicitly in the PR description so
reviewers know.

## Do not bump pinned dependencies

Every `mc-*` coordinate in `gradle/libs.versions.toml` is the exact set
Mojang ships for 1.19.4. The decompiled sources are written against those
APIs. Bumping one is not a routine change — it is a porting project.

Likewise `gson`, `guava`, `slf4j-api` and `joml` are vendored as source under
`src/main/java`, and their catalog coordinates are reference-only.

If you genuinely need a version change, explain in the PR what API shifted and
what you had to port. `.github/dependabot.yml` deliberately ignores these on
purpose; do not remove those ignore rules to "help" Dependabot.

## Never commit decompiler scratch

`.decompile_work/`, `.backup_*/` and `build/` output are all gitignored. They
contain Mojang's code and are not ours to publish. If you need a snapshot
while working, keep it local.

## Code style

Match the file you are editing. The decompiled Mojang sources use 3-space
indentation and `param0`-style parameter names — leave them alone unless a
change is specifically about porting them. Eaglercraft's own code
(`net/lax1dude/**`) uses tabs.

Line endings: the tree is CRLF in most places. `.gitattributes` pins `*.sh` to
LF and `*.bat`/`gradlew` appropriately — do not mass-normalise line endings in
a functional change; it destroys reviewability.

## The big open task

Reintroducing an `appMain()`-equivalent launch seam so all three targets start
the 1.19.4 tree the same way, replacing the hardcoded

```java
Main.main(new String[] { "--version", "1.19.4", "--accessToken", "0" });
```

in `src/wasm-gc-teavm/.../ClientMain.java`. That call bypasses Eaglercraft's
profile, boot menu and local-storage handling. If you take this on, please open
an issue first — it touches every runtime.