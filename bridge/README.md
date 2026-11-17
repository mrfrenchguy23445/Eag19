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
- The browser must be able to reach the bridge over WebSocket.

The bridge cannot run inside the offline HTML file or a static website: browsers
cannot open raw TCP sockets. It must run as a small Node process either on the
player's PC (offline download) or on a host you control (website).

## Offline download, same PC as Bungee

Install once, then start the bridge:

```bat
cd C:\Users\Disas\Documents\Eag190\bridge
npm ci
node eagler-bridge.js --bind 127.0.0.1 --listen 8081 --target 127.0.0.1 --port 25565
```

In the client, add:

```
ws://localhost:8081/
```

Keep the bridge running while you play. No client rebuild is needed.

## Any Bungee host or port

Add `--allow-any-target`. Each client then chooses the backend in the WebSocket
URL with `?host=...&port=...`:

```bat
node eagler-bridge.js --bind 127.0.0.1 --listen 8081 --allow-any-target --token CHANGE_ME
```

Client address:

```
ws://localhost:8081/?host=127.0.0.1&port=25566&token=CHANGE_ME
```

`--target` and `--port` become the fallback when the URL omits `host` or `port`.

The `host` is resolved **by the bridge machine**, not by the browser. On an
offline download that is the player's PC; on a website it is the bridge server.
Use a hostname/IP reachable from wherever the bridge runs.

### Security

`--allow-any-target` turns the bridge into a TCP relay. If you bind to anything
other than `127.0.0.1`, always set `--token`:

```
ws://bridge.example.com:8081/?host=mc.example.com&port=25565&token=CHANGE_ME
```

Requests with a missing or wrong token are closed with WebSocket code `1008`
before any backend connection is opened.

## Website / HTTPS hosting

A static site can serve the HTML, but it cannot host the bridge. Run the bridge
on a Node-capable server, then point the client at it.

If your site is served over **HTTPS**, the client must use `wss://`; browsers
block `ws://` from HTTPS pages. Two ways to do that:

### Option A: Caddy reverse proxy (recommended)

Run the bridge on localhost:

```bat
node eagler-bridge.js --bind 127.0.0.1 --listen 8081 --allow-any-target --token CHANGE_ME
```

Caddyfile:

```caddyfile
bridge.example.com {
	reverse_proxy 127.0.0.1:8081
}
```

Caddy handles TLS automatically. Client address:

```
wss://bridge.example.com/?host=mc.example.com&port=25565&token=CHANGE_ME
```

### Option B: native TLS in the bridge

Give the bridge a PEM certificate and key:

```bat
node eagler-bridge.js --bind 0.0.0.0 --listen 8081 --allow-any-target --token CHANGE_ME --tls-cert fullchain.pem --tls-key privkey.pem
```

Client address:

```
wss://bridge.example.com:8081/?host=mc.example.com&port=25565&token=CHANGE_ME
```

nginx, Apache, or a cloud load balancer can terminate TLS the same way.

If the website is plain HTTP, `ws://bridge.example.com:8081/?...` is fine.

## Verify it

```bat
cd C:\Users\Disas\Documents\Eag190\bridge
npm test
```

The test proves:

1. A wrong `--token` is rejected with close code `1008`.
2. Per-client `?host=&port=` overrides reach the backend.
3. `Accept: MOTD` gets a JSON text frame plus a 64×64 RGBA icon binary frame.
4. The backend handshake uses protocol 762 with next state `login`.
5. `LoginStart` contains the username, `false` for `Optional<UUID>`, and no UUID
   bytes. This is the byte that EaglerXServer forgets.
6. Play packets relay both ways with vanilla TCP framing and no WebSocket length
   prefix.

## Server-list queries

The bridge answers Eagler `Accept: MOTD` queries itself with a small static
MOTD/icon. It does not use the vanilla status protocol for the list, because
that is not what this client sends. Actual joins still go to the real backend.

## All options

```text
--bind ip             WebSocket listen address (default 127.0.0.1)
--listen port         WebSocket listen port (default 8081)
--target host         default Bungee host (default 127.0.0.1)
--port port           default Bungee port (default 25565)
--allow-any-target    let each client choose host/port with ?host=&port=
--token secret        require ?token=secret in the WebSocket URL
--tls-cert file       PEM certificate for native wss support
--tls-key file        PEM private key for native wss support
```

## Protocol notes

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
