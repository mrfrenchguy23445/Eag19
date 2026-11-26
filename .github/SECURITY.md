# Security Policy

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.

Use GitHub's private reporting on this repository
(**Security** → **Report a vulnerability**). If that is unavailable, contact a
maintainer directly rather than filing a public issue.

Please include:

- what the issue is and where in the tree it lives,
- how to reproduce it,
- the impact you believe it has.

## Scope

This project compiles a **decompiled Minecraft 1.19.4 tree** and runs it in the
browser via TeaVM/WASM-GC. Two things follow from that:

1. **Most of `src/main/java` is Mojang's code, not ours.** Vulnerabilities in
   the vanilla game tree should be reported to Mojang/Minecraft, not here. We
   will still fix anything that is specific to this port.
2. **The interesting attack surface is the web runtime** — the TeaVM-generated
   module, the loader/bootstrap, the boot menu, local-storage handling, and
   anything that takes input from `window.eaglercraftXOpts` or network
   messages. Issues there are squarely in scope.

## Threat model notes

The client parses attacker-controllable data by design: server packets, EPK
asset archives, and the `eaglercraftXOpts` configuration object. Reports about
how those inputs are trusted are welcome.

WASM-GC and the JS backend run the same game logic under different memory
models; a finding that reproduces in one but not the other is still worth
reporting.

## Supported versions

There is no released version yet — this is a work-in-progress port, and `main`
is the only branch. Fixes land on `main`.