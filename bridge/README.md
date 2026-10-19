# EaglerBridge

A tiny WebSocket → TCP bridge that lets the Eaglercraft client join a normal
Minecraft server. It speaks the Eagler v5 handshake on the WebSocket side and
vanilla 1.19.4 (protocol 762) on the TCP side.

Scope: **offline-mode ("cracked") Java 1.19.4 servers only.** Online-mode
servers need Mojang session auth, which the client cannot perform.

## Run it

```sh
node bridge/eagler-bridge.js --listen 8081 --target 127.0.0.1 --port 25565
```

Requires the `ws` module (`npm i ws`) and a backend server set to
`online-mode=false`.

Then add the server in the client as `ws://localhost:8081/`.

## Protocol notes

These are the parts that are easy to get wrong, and that cost the most time:

- **Eagler WebSocket framing is one binary message per packet.** There is no
  length prefix; the vanilla framing (`frame()`) only applies to the TCP side.
- **Identity is split across two packets.** `CLIENT_VERSION` always carries the
  real username. `REQUEST_LOGIN` carries a *nickname override* that is
  deliberately 0-length unless the server enabled nickname selection in
  `SERVER_VERSION` (see `HandshakerInstance.handlePacketRequestLogin()` in
  EaglerXServer). Reading the name only from `REQUEST_LOGIN` yields `""`, and
  the backend then rejects the login with
  `IllegalArgumentException: Name and ID cannot be both blank`.
- **`SERVER_ALLOW_LOGIN` must come after the backend's `Login Success`.** If it
  goes out first, the backend's Login Success leaks to the client as a bogus
  PLAY packet.
- **Status pings are vanilla, not Eagler.** The server list pings with the
  ordinary handshake/status/ping sequence; only real joins use the Eagler
  handshake.
- **Nickname selection is sent as `false`**, so the client sends its profile
  name through `CLIENT_VERSION` and an empty name slot in `REQUEST_LOGIN`.
