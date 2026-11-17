# Running a server for Eag19

Eag19 speaks the Eaglercraft WebSocket transport, not raw Minecraft TCP, so it
cannot join a normal Java server directly. A Java 1.19.4 server is still the
authority for the world; a WebSocket/translation layer sits in front of it.

The layout that matches the client's protocol (handshake v5, game protocol
`762`) is:

```
browser (ws:// or wss://)
        |
        v
EaglerXServer listener  (on Velocity, BungeeCord, or Paper directly)
        |
        v
Paper 1.19.4 backend    (ordinary Minecraft protocol)
```

Java players can use the backend port directly; browser players use the
EaglerXServer listener port.

## Versions that matter

| Component | Version |
| --- | --- |
| Eag19 client | Eaglercraft handshake v5, Minecraft protocol `762` (1.19.4) |
| Backend | Paper 1.19.4 (or a 1.19.4-compatible fork) |
| Proxy | Velocity 3.x or BungeeCord, Java 17+ |
| WebSocket plugin | EaglerXServer 1.1.x |

EaglerXServer translates the Eaglercraft framing, and its
`protocols.max_minecraft_protocol_v5` setting defaults to `-1` ("any Minecraft
protocol version"), so a v5 client advertising `762` is accepted by a current
release. If you changed that value, set it back to `-1`.

Public 1.19.4-capable Eaglercraft servers are rare; most existing networks run
1.8 or 1.12.2 backends. Self-hosting is the reliable path.

## Option A: Velocity + Paper (recommended)

1. Create a Paper 1.19.4 server and let it finish generating a world once.
2. In `server.properties` set `online-mode=false` and make sure the backend
   port is not exposed to the internet. Authentication is the proxy's job.
3. Create a Velocity proxy. In `velocity.toml`:
   - `online-mode = false` (Eaglercraft players have no Mojang session)
   - add the Paper server under `[servers]` and put it in `try`
   - use `player-info-forwarding-mode = "modern"` with a forwarding secret, or
     `legacy` with BungeeCord-style forwarding, and install the matching
     forwarding plugin on Paper
4. Put `EaglerXServer.jar` in Velocity's `plugins/` directory and start once to
   generate `plugins/eaglerxserver/`.
5. Edit `plugins/eaglerxserver/listeners.toml` (YAML on BungeeCord):
   - set `bind_address`/port to the WebSocket listener you want (for example
     `0.0.0.0:8081`)
   - set `inject_address` to the Velocity listener address; it must match a
     listener in `velocity.toml`, not an arbitrary new port
   - enable `allow_motd` and set `server_motd` / `server_icon`
   - optionally set `tls_config` if EaglerXServer terminates TLS itself;
     otherwise leave TLS to a reverse proxy
6. Start Velocity and Paper. In the client, add:

   ```
   ws://your-server.example:8081
   ```

## Option B: single Paper server

EaglerXServer can be installed directly on Paper, which removes the proxy but
loses Velocity/BungeeCord's forwarding and plugin ecosystem. Bukkit plugin
support is only maintained through 1.17, so for a 1.19.4 backend prefer
Option A. If you try it anyway, use Java 17+, keep `online-mode=false`, and
follow the same listener configuration; the listener port replaces the normal
Minecraft port for browser clients.

## Secure WebSockets (wss)

If the client page is served over HTTPS, browsers block plain `ws://`. Serve a
`wss://` endpoint, either with EaglerXServer's `tls_config` or with Nginx/Caddy
in front:

```nginx
location / {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 300s;
}
```

Set `forward_ip = true` on the EaglerXServer listener if a reverse proxy is
used, and use `forward_secret` so clients cannot bypass the proxy and forge the
forwarded address.

## Addresses in the client

- Server list ping and join both accept `host:port`; the client adds `ws://`
  when the page is HTTP and `wss://` when the page is HTTPS.
- You can type the scheme explicitly, e.g. `ws://localhost:8081`.
- For local testing, `scripts/mock-eagler-server.mjs` implements the query and
  login handshake (but not a playable world):

  ```sh
  node scripts/mock-eagler-server.mjs 8080
  ```

  Add `ws://localhost:8080` in the server list. Expect the client to reach
  "Joining world..." and then wait, because the mock sends no play packets.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Ping shows but joining closes immediately (WebSocket close `1006`) | Backend/game protocol rejected: confirm Paper is 1.19.4 and `max_minecraft_protocol_v5 = -1` |
| Browser refuses the connection on an HTTPS page | Use `wss://`, not `ws://` |
| "End of stream" during play | Lower `view-distance`; Eaglercraft's bandwidth is the usual limit |
| Player is shown as offline / can use any name | Expected with `online-mode=false`; add an auth plugin such as AuthMe or nLogin |
| Proxy connects but `inject_address` fails | It must equal an address configured in `velocity.toml` / `config.yml` |

## Legal note

Running a server still requires a legitimate Minecraft Java Edition 1.19.4
installation for the backend files, and hosting is subject to Mojang's EULA.
This guide does not grant any rights to Minecraft or its assets.
