## What does this change?

<!-- One or two sentences. What is different after this PR? -->

## Why?

<!-- The problem being solved. Link the issue if there is one. -->

## Scope

- [ ] `src/main/` — merged game tree (checked by CI)
- [ ] `src/wasm-gc-teavm/` — WASM-GC runtime
- [ ] `src/teavm/` — JS runtime *(currently does not compile — see below)*
- [ ] `src/lwjgl/` — desktop runtime *(currently does not compile — see below)*
- [ ] build / dependencies
- [ ] docs

## Verification

- [ ] `./gradlew :compileJava` passes on **JDK 17**
- [ ] Runtime behaviour manually checked (say how, and on which target)

### If you touched `src/teavm/` or `src/lwjgl/`

CI only compiles the root `:compileJava`, so it will stay green even though
those targets are broken at HEAD (`Main.appMain()` does not exist). If your
change affects them, note here what you verified and what you could not:

<!--
Example:
Touches src/teavm/. Compiled that target manually with <command> and got
<result>. Did not verify <x> because <reason>.
-->

## Dependency changes

- [ ] No dependency versions changed

If you **did** change a version in `gradle/libs.versions.toml`, you must
explain what API moved and what you ported:

<!--
The mc-* coordinates are pinned to Minecraft 1.19.4's version.json.
Bumping one is a porting task, not a maintenance chore.
-->

## Checklist

- [ ] Did not commit `.decompile_work/`, `.backup_*/`, or `build/` output
- [ ] Did not add or remove entries in `.github/dependabot.yml` ignore lists
      without saying why
- [ ] Matched the surrounding indentation (3 spaces in decompiled Mojang
      sources, tabs in `net/lax1dude/**`)
- [ ] Did not mass-normalise line endings