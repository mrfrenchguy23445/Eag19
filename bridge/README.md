# EaglerBridge

A small WebSocket → TCP bridge that lets this Eaglercraft 1.19.4 client join a
normal Java 1.19.4 server. It speaks Eagler v5 on the browser side and vanilla
protocol 762 on the TCP side.

**This is the client-side multiplayer fix.** It bypasses EaglerXServer's broken
v5 → Bungee login path entirely, so the server plugin does not need to be
patched. The browser still uses the normal client; only the address you add in
the multiplayer menu changes.

Scope: **offline-mode (`online-mode=false`) servers only.** The client has no
Mojang session auth, so an online-mode backend will send an Encryption Request
and the bridge will reject it with a clear message.

## Requirements

- Node.js 18+
- The backend must listen on a normal vanilla TCP port and accept protocol 762
  (1.19.4). A Bungee/Paper setup with ViaVersion etc. is fine.
- Bungee (or the backend) must be in offline mode.

## Run it

From this directory:

```sh
npm ci
node eagler-bridge.js --bind 127.0.0.1 --listen 8081 --target 127.0.0.1 --port 25565
```

Replace `--target` / `--port` with the real Bungee host and TCP port. Then, in
the client, add this server address:

```
ws://localhost:8081/
```

Keep the bridge running while you play. No client rebuild is needed; the
existing offline HTML already knows how to talk to it.

If the browser is on another device, bind to all interfaces and use this PC's
LAN IP instead:

```sh
node eagler-bridge.js --bind 0.0.0.0 --listen 8081 --target 127.0.0.1 --port 25565
```

```
ws://192.168.1.2:8081/
```

Allow port 8081 through the firewall in that case.

## Verify it

```sh
npm test
```

The test starts a fake vanilla backend and proves all four critical pieces:

1. `Accept: MOTD` gets a JSON text frame plus a 64×64 RGBA icon binary frame.
2. The backend handshake uses protocol 762 with next state `login`.
3. `LoginStart` contains the username, `false` for `Optional<UUID>`, and no UUID
   bytes. This is the byte that EaglerXServer forgets.
4. Play packets relay both ways with vanilla TCP framing and no WebSocket length
   prefix.

## Server-list queries

The bridge answers Eagler `Accept: MOTD` queries itself with a small static
MOTD/icon. It does not use the vanilla status protocol for the list, because
that is not what this client sends. Actual joins still go to the real backend.

## Protocol notes

These are the parts that are easy to get wrong:

- **Eagler WebSocket framing is one binary message per packet.** There is no
  length prefix; the vanilla framing (`frame()`) only applies to the TCP side.
- **Identity is split across two packets.** `CLIENT_VERSION` always carries the
  real username. `REQUEST_LOGIN` carries a *nickname override* that is
  deliberately 0-length unless the server enabled nickname selection in
  `SERVER_VERSION`. Reading the name only from `REQUEST_LOGIN` yields `""`, and
  the backend then rejects the login with
  `IllegalArgumentException: Name and ID cannot be both blank`.
- **`SERVER_ALLOW_LOGIN` must come after the backend's `Login Success`.** If it
  goes out first, the backend's Login Success leaks to the client as a bogus
  PLAY packet.
- **Status pings are Eagler text queries, not vanilla packets.** The client
  opens a WebSocket, sends `Accept: MOTD`, and expects a JSON string frame
  followed by an optional RGBA icon binary frame.
- **Nickname selection is sent as `false`**, so the client sends its profile
  name through `CLIENT_VERSION` and an empty name slot in `REQUEST_LOGIN`.
