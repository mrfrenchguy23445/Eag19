package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;
import net.lax1dude.eaglercraft.v1_8.EagRuntime;
import net.lax1dude.eaglercraft.v1_8.internal.EnumServerRateLimit;
import net.lax1dude.eaglercraft.v1_8.internal.IServerQuery;
import net.lax1dude.eaglercraft.v1_8.internal.QueryResponse;
import net.lax1dude.eaglercraft.v1_8.socket.AddressResolver;
import net.lax1dude.eaglercraft.v1_8.socket.RateLimitTracker;
import net.lax1dude.eaglercraft.v1_8.socket.ServerQueryDispatch;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.status.ServerStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Pings servers for the multiplayer list.
 *
 * <p>Vanilla pings with raw status packets, but an Eaglercraft server only
 * exposes its MOTD over the WebSocket query protocol: the client opens a
 * socket, sends {@code Accept: MOTD}, and the server answers with a JSON
 * document (and optionally a 64x64 icon in a binary frame). Sending vanilla
 * packets to the socket makes the server close it, which is why the list used
 * to show every server as unreachable.
 */
public class ServerStatusPinger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component CANT_CONNECT_MESSAGE = Component.translatable("multiplayer.status.cannot_connect")
        .withStyle(param0 -> param0.withColor(-65536));
    private static final Component RATE_LIMIT_MESSAGE = Component.literal("Too Many Requests!\nTry again later")
        .withStyle(ChatFormatting.RED);
    private static final long QUERY_TIMEOUT_MS = 10000L;
    private static final long ICON_GRACE_MS = 750L;
    private static final int ICON_SIZE = 64;
    private static final int ICON_BYTES = ICON_SIZE * ICON_SIZE * 4;
    private final List<ServerStatusPinger.Ping> pings = Collections.synchronizedList(Lists.newArrayList());

    public void pingServer(final ServerData param0, final Runnable param1) throws UnknownHostException {
        String var2 = AddressResolver.resolveURI(param0.ip);
        param0.motd = Component.translatable("multiplayer.status.pinging");
        param0.status = CommonComponents.EMPTY;
        param0.players = null;
        param0.playerList = Collections.emptyList();
        param0.ping = -2L;
        if (RateLimitTracker.isLockedOut(var2)) {
            LOGGER.error("Server {} locked this client out on a previous connection, will not attempt to reconnect", param0.ip);
            param0.motd = RATE_LIMIT_MESSAGE;
            param0.ping = -1L;
            return;
        }

        IServerQuery var3 = ServerQueryDispatch.sendServerQuery(var2, "MOTD");
        if (var3 == null) {
            this.onPingFailed(CANT_CONNECT_MESSAGE, param0);
        } else {
            this.pings.add(new ServerStatusPinger.Ping(var3, var2, param0, param1));
        }
    }

    public void tick() {
        RateLimitTracker.tick();
        synchronized (this.pings) {
            long var1 = EagRuntime.steadyTimeMillis();
            Iterator<ServerStatusPinger.Ping> var3 = this.pings.iterator();

            while (var3.hasNext()) {
                ServerStatusPinger.Ping var4 = var3.next();
                var4.query.update();
                EnumServerRateLimit var5 = var4.query.getRateLimit();
                if (var5 != EnumServerRateLimit.OK) {
                    if (var5 == EnumServerRateLimit.LOCKED_OUT) {
                        RateLimitTracker.registerLockOut(var4.uri);
                    } else {
                        RateLimitTracker.registerBlock(var4.uri);
                    }

                    this.onPingFailed(RATE_LIMIT_MESSAGE, var4.data);
                    var4.query.close();
                    var3.remove();
                    continue;
                }

                if (var4.query.responsesAvailable() > 0) {
                    QueryResponse var6;
                    do {
                        var6 = var4.query.getResponse();
                    } while (var4.query.responsesAvailable() > 0);

                    if (var6 != null && "motd".equalsIgnoreCase(var6.responseType) && var6.isResponseJSON()) {
                        this.applyMotd(var4, var6);
                        var4.motdTime = var1;
                        if (var4.callback != null) {
                            var4.callback.run();
                        }
                    }
                }

                byte[] var10 = null;
                if (var4.query.binaryResponsesAvailable() > 0) {
                    do {
                        var10 = var4.query.getBinaryResponse();
                    } while (var4.query.binaryResponsesAvailable() > 0);
                }

                if (var10 != null) {
                    // The MOTD document advertises whether an icon follows. Only
                    // accept the binary frame when the server said to expect one,
                    // otherwise a stray frame would attach a bogus icon.
                    if (var4.iconEnabled && var10.length == ICON_BYTES) {
                        byte[] var7 = encodeIconPng(var10);
                        if (var7 != null) {
                            var4.data.setIconBytes(var7);
                        }
                    }
                } else if (var4.motdTime != -1L && !var4.iconEnabled) {
                    // No icon (or the server dropped it): clear whatever the list
                    // cached so a previous server's icon cannot linger.
                    var4.data.setIconBytes(null);
                }

                if (var4.motdTime != -1L && (var10 != null || !var4.iconEnabled || var1 - var4.motdTime > ICON_GRACE_MS || var4.query.isClosed())) {
                    var4.query.close();
                    var3.remove();
                } else if (!var4.query.isClosed() && var1 - var4.startTime > QUERY_TIMEOUT_MS) {
                    if (var4.motdTime == -1L) {
                        this.onPingFailed(RateLimitTracker.isProbablyLockedOut(var4.uri) ? RATE_LIMIT_MESSAGE : CANT_CONNECT_MESSAGE, var4.data);
                    }

                    var4.query.close();
                    var3.remove();
                } else if (var4.query.isClosed() && var4.motdTime == -1L) {
                    this.onPingFailed(CANT_CONNECT_MESSAGE, var4.data);
                    var3.remove();
                }
            }
        }
    }

    private void applyMotd(ServerStatusPinger.Ping param0, QueryResponse param1) {
        try {
            ServerData serverData = param0.data;
            JSONObject var4 = param1.getResponseJSON();
            serverData.motd = motdComponent(var4, param1.serverName);
            param0.iconEnabled = var4.optBoolean("icon", false);
            int var5 = var4.optInt("online", 0);
            int var6 = var4.optInt("max", 0);
            JSONArray var7 = var4.optJSONArray("players");
            List<GameProfile> var8 = Lists.newArrayList();
            List<Component> var9 = Lists.newArrayList();
            if (var7 != null) {
                for (int var10 = 0; var10 < var7.length(); var10++) {
                    String var11 = var7.optString(var10, "");
                    if (!var11.isEmpty()) {
                        var8.add(new GameProfile(offlineUuid(var11), var11));
                        if (var9.size() < 5) {
                            var9.add(Component.literal(var11));
                        }
                    }
                }

                if (var7.length() > 5) {
                    var9.add(Component.translatable("multiplayer.status.and_more", var7.length() - 5));
                }
            }

            serverData.players = new ServerStatus.Players(var6, var5, var8);
            serverData.status = var6 > 0 ? formatPlayerCount(var5, var6) : Component.literal(Integer.toString(var5));
            serverData.playerList = var9;
            serverData.version = Component.literal(param1.serverVersion);
            serverData.ping = Math.max(1L, param1.ping);
            serverData.setEnforcesSecureChat(false);
        } catch (Throwable var12) {
            LOGGER.error("Could not decode MOTD from {}", param0.data.ip, var12);
            param0.data.ping = -1L;
        }
    }

    private static Component motdComponent(JSONObject param0, String param1) {
        JSONArray var2 = param0.optJSONArray("motd");
        if (var2 == null || var2.length() == 0) {
            return legacyComponent(param1 == null ? "" : param1);
        }

        MutableComponent var3 = Component.empty();

        for (int var4 = 0; var4 < var2.length(); var4++) {
            if (var4 > 0) {
                var3.append(Component.literal("\n"));
            }

            var3.append(legacyComponent(var2.optString(var4, "")));
        }

        return var3;
    }

    /**
     * Parses the {@code §}-style formatting codes Eaglercraft servers use in
     * their MOTD into proper {@link Component} styles. Vanilla's font renderer
     * only applies styles carried by components, so without this the raw
     * codes would be drawn as text.
     */
    static Component legacyComponent(String param0) {
        if (param0 == null || param0.isEmpty()) {
            return Component.empty();
        }

        MutableComponent var1 = Component.empty();
        StringBuilder var2 = new StringBuilder();
        Style var3 = Style.EMPTY;

        for (int var4 = 0; var4 < param0.length(); var4++) {
            char var5 = param0.charAt(var4);
            if (var5 == ChatFormatting.PREFIX_CODE && var4 + 1 < param0.length()) {
                ChatFormatting var6 = ChatFormatting.getByCode(Character.toLowerCase(param0.charAt(var4 + 1)));
                if (var6 != null) {
                    if (var2.length() > 0) {
                        var1.append(Component.literal(var2.toString()).withStyle(var3));
                        var2.setLength(0);
                    }

                    if (var6 == ChatFormatting.RESET) {
                        var3 = Style.EMPTY;
                    } else if (var6.isColor()) {
                        var3 = var3.withColor(var6);
                    } else {
                        var3 = var3.applyFormat(var6);
                    }

                    var4++;
                    continue;
                }
            }

            var2.append(var5);
        }

        if (var2.length() > 0) {
            var1.append(Component.literal(var2.toString()).withStyle(var3));
        }

        return var1;
    }

    private static UUID offlineUuid(String param0) {
        return new UUID(0L, param0.hashCode());
    }

    /**
     * Turns the raw 64x64 RGBA icon frame into a PNG, which is what
     * {@code ServerData.setIconBytes} and the server list texture loader expect.
     */
    private static byte[] encodeIconPng(byte[] param0) {
        try {
            byte[] var1 = new byte[ICON_SIZE * (ICON_SIZE * 4 + 1)];

            for (int var2 = 0; var2 < ICON_SIZE; var2++) {
                int var3 = var2 * ICON_SIZE * 4;
                int var4 = var2 * (ICON_SIZE * 4 + 1);
                var1[var4] = 0;
                System.arraycopy(param0, var3, var1, var4 + 1, ICON_SIZE * 4);
            }

            Deflater var13 = new Deflater(Deflater.BEST_SPEED);
            byte[] var15;
            try {
                var13.setInput(var1);
                var13.finish();
                ByteArrayOutputStream var5 = new ByteArrayOutputStream();
                byte[] var6 = new byte[512];

                while (!var13.finished()) {
                    int var7 = var13.deflate(var6);
                    var5.write(var6, 0, var7);
                }

                var15 = var5.toByteArray();
            } finally {
                var13.end();
            }

            ByteArrayOutputStream var16 = new ByteArrayOutputStream();
            var16.write(new byte[] { (byte)137, 80, 78, 71, 13, 10, 26, 10 }, 0, 8);
            byte[] var17 = new byte[13];
            writeBigEndianInt(var17, 0, ICON_SIZE);
            writeBigEndianInt(var17, 4, ICON_SIZE);
            var17[8] = 8;
            var17[9] = 6;
            writeChunk(var16, "IHDR", var17);
            writeChunk(var16, "IDAT", var15);
            writeChunk(var16, "IEND", new byte[0]);
            return var16.toByteArray();
        } catch (Throwable var12) {
            LOGGER.error("Could not encode server icon", var12);
            return null;
        }
    }

    private static void writeChunk(ByteArrayOutputStream param0, String param1, byte[] param2) {
        byte[] var3 = param1.getBytes(StandardCharsets.US_ASCII);
        writeBigEndianInt(param0, param2.length);
        param0.write(var3, 0, 4);
        param0.write(param2, 0, param2.length);
        byte[] var4 = new byte[4 + param2.length];
        System.arraycopy(var3, 0, var4, 0, 4);
        System.arraycopy(param2, 0, var4, 4, param2.length);
        writeBigEndianInt(param0, crc32(var4, 0, var4.length));
    }

    private static void writeBigEndianInt(ByteArrayOutputStream param0, int param1) {
        param0.write(param1 >>> 24 & 0xFF);
        param0.write(param1 >>> 16 & 0xFF);
        param0.write(param1 >>> 8 & 0xFF);
        param0.write(param1 & 0xFF);
    }

    private static void writeBigEndianInt(byte[] param0, int param1, int param2) {
        param0[param1] = (byte)(param2 >>> 24);
        param0[param1 + 1] = (byte)(param2 >>> 16);
        param0[param1 + 2] = (byte)(param2 >>> 8);
        param0[param1 + 3] = (byte)param2;
    }

    private static int crc32(byte[] param0, int param1, int param2) {
        int var3 = -1;

        for (int var4 = 0; var4 < param2; var4++) {
            var3 ^= param0[param1 + var4] & 0xFF;

            for (int var5 = 0; var5 < 8; var5++) {
                var3 = (var3 & 1) != 0 ? var3 >>> 1 ^ 0xEDB88320 : var3 >>> 1;
            }
        }

        return ~var3;
    }

    void onPingFailed(Component param0, ServerData param1) {
        LOGGER.error("Can't ping {}: {}", param1.ip, param0.getString());
        param1.motd = param0;
        param1.status = CommonComponents.EMPTY;
        param1.players = null;
        param1.playerList = Collections.emptyList();
        param1.ping = -1L;
    }

    static Component formatPlayerCount(int param0, int param1) {
        return Component.literal(Integer.toString(param0))
            .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
            .append(Integer.toString(param1))
            .withStyle(ChatFormatting.GRAY);
    }

    public void removeAll() {
        synchronized (this.pings) {
            Iterator<ServerStatusPinger.Ping> var2 = this.pings.iterator();

            while (var2.hasNext()) {
                ServerStatusPinger.Ping var3 = var2.next();
                var3.query.close();
                var2.remove();
            }
        }
    }

    private static final class Ping {
        final IServerQuery query;
        final String uri;
        final ServerData data;
        final Runnable callback;
        final long startTime;
        long motdTime = -1L;
        boolean iconEnabled;

        Ping(IServerQuery param0, String param1, ServerData param2, Runnable param3) {
            this.query = param0;
            this.uri = param1;
            this.data = param2;
            this.callback = param3;
            this.startTime = EagRuntime.steadyTimeMillis();
        }
    }
}
