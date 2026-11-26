package net.lax1dude.eaglercraft.v1_8.socket;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;

import io.netty.buffer.Unpooled;
import net.lax1dude.eaglercraft.v1_8.ArrayUtils;
import net.lax1dude.eaglercraft.v1_8.EaglercraftUUID;
import net.lax1dude.eaglercraft.v1_8.EaglercraftVersion;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.v1_8.internal.IWebSocketFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Runs the Eaglercraft connection handshake over an already-open WebSocket.
 *
 * <p>Eaglercraft servers do not speak the vanilla Minecraft handshake and login
 * exchange. Instead the client advertises the Eaglercraft protocol versions it
 * supports, the server picks one and answers with its own identity and the
 * authentication method it wants, the two exchange profile data, and the client
 * finishes login. Only then does the server start sending normal play packets.
 *
 * <p>This class drives that exchange. {@link #tick()} must be called once per
 * client tick from the screen that owns the connection; when
 * {@link #isFinished()} becomes true the connection has already been moved to
 * {@link ConnectionProtocol#PLAY} and given a {@link ClientPacketListener}, or
 * {@link #getFailureReason()} holds the reason it could not be.
 */
public class EaglercraftHandshaker {

    public static final int GAME_PROTOCOL_VERSION = 762;

    private static final int PROTOCOL_V3 = 3;
    private static final int PROTOCOL_V4 = 4;
    private static final int PROTOCOL_V5 = 5;

    /** REDIRECT | NOTIFICATION | PAUSE_MENU, matching the reference client. */
    private static final int CLIENT_CAPABILITIES = 0x1C;

    private static final int STATE_SEND_VERSION = 0;
    private static final int STATE_AWAIT_VERSION = 1;
    private static final int STATE_AWAIT_LOGIN = 2;
    private static final int STATE_AWAIT_FINISH = 3;
    private static final int STATE_FINISHED = 4;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final IWebSocketClient websocket;
    private final Minecraft minecraft;
    private final Screen parent;
    @Nullable
    private final ServerData serverData;
    private final Connection connection;
    private final String username;
    @Nullable
    private final String password;

    private int state = STATE_SEND_VERSION;
    private int protocolVersion;
    private int selectedGameProtocol = EaglerProtocolBridge.CLIENT_GAME_PROTOCOL;
    private boolean nicknameSelection = true;
    private String pluginBrand = "";
    private String pluginVersion = "";
    private String loggedInUsername;
    @Nullable
    private EaglercraftUUID loggedInUUID;
    @Nullable
    private Component failureReason;
    @Nullable
    private String redirectAddress;

    public EaglercraftHandshaker(IWebSocketClient websocket, Minecraft minecraft, Screen parent,
            @Nullable ServerData serverData, Connection connection) {
        this.websocket = websocket;
        this.minecraft = minecraft;
        this.parent = parent;
        this.serverData = serverData;
        this.connection = connection;
        this.username = minecraft.getUser().getName();
        this.password = null;
    }

    public boolean isFinished() {
        return state == STATE_FINISHED;
    }

    @Nullable
    public Component getFailureReason() {
        return failureReason;
    }

    @Nullable
    public String getRedirectAddress() {
        return redirectAddress;
    }

    public String getPluginBrand() {
        return pluginBrand;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void tick() {
        switch (state) {
            case STATE_SEND_VERSION:
                if (websocket.isClosed()) {
                    fail(Component.translatable("disconnect.genericReason", "Connection closed"));
                    return;
                }

                if (!websocket.isOpen()) {
                    return;
                }

                sendClientVersion();
                state = STATE_AWAIT_VERSION;
                break;
            case STATE_AWAIT_VERSION:
            case STATE_AWAIT_LOGIN:
            case STATE_AWAIT_FINISH:
                pump();
                break;
            default:
                break;
        }
    }

    private void pump() {
        IWebSocketFrame frame;
        while (state != STATE_FINISHED && (frame = websocket.getNextBinaryFrame()) != null) {
            byte[] data = frame.getByteArray();
            if (data == null || data.length == 0) {
                continue;
            }

            try {
                handlePacket(data);
            } catch (Exception ex) {
                LOGGER.error("Invalid handshake packet received", ex);
                fail(Component.translatable("connect.failed"));
            }
        }

        if (state != STATE_FINISHED && websocket.isClosed()) {
            fail(Component.translatable("multiplayer.disconnect.server_shutdown"));
        }
    }

    private void handlePacket(byte[] data) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        int packetId = buffer.readUnsignedByte();

        switch (packetId) {
            case HandshakePacketTypes.PROTOCOL_SERVER_VERSION:
                handleServerVersion(buffer);
                break;
            case HandshakePacketTypes.PROTOCOL_VERSION_MISMATCH:
                handleVersionMismatch(buffer);
                break;
            case HandshakePacketTypes.PROTOCOL_SERVER_ALLOW_LOGIN:
                handleServerAllowLogin(buffer);
                break;
            case HandshakePacketTypes.PROTOCOL_SERVER_DENY_LOGIN:
                handleServerDenyLogin(buffer);
                break;
            case HandshakePacketTypes.PROTOCOL_SERVER_FINISH_LOGIN:
                handleServerFinishLogin();
                break;
            case HandshakePacketTypes.PROTOCOL_SERVER_REDIRECT_TO:
                handleServerRedirect(buffer);
                break;
            case HandshakePacketTypes.PROTOCOL_SERVER_ERROR:
                handleServerError(buffer);
                break;
            default:
                fail(Component.literal("Unknown handshake packet type " + packetId + " received"));
                break;
        }
    }

    private void sendClientVersion() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(HandshakePacketTypes.PROTOCOL_CLIENT_VERSION);
        buffer.writeByte(2);
        buffer.writeShort(3);
        buffer.writeShort(PROTOCOL_V5);
        buffer.writeShort(PROTOCOL_V4);
        buffer.writeShort(PROTOCOL_V3);

        if (EaglerProtocolBridge.isAvailable()) {
            // The V5 port lets the proxy pick a wire protocol we can translate
            // client-side. 764 fixes the vanilla login-start UUID mismatch on
            // modern Bungee; 758 is kept as the ViaVersion fallback path.
            buffer.writeShort(2);
            buffer.writeShort(EaglerProtocolBridge.SERVER_GAME_PROTOCOL_1_20_2);
            buffer.writeShort(EaglerProtocolBridge.SERVER_GAME_PROTOCOL_1_18_2);
        } else {
            buffer.writeShort(1);
            buffer.writeShort(GAME_PROTOCOL_VERSION);
        }

        String clientBrand = EaglercraftVersion.projectForkName;
        buffer.writeByte(clientBrand.length());
        writeASCII(buffer, clientBrand);

        String clientVers = EaglercraftVersion.projectOriginVersion;
        buffer.writeByte(clientVers.length());
        writeASCII(buffer, clientVers);

        buffer.writeBoolean(password != null);

        buffer.writeByte(username.length());
        writeASCII(buffer, username);

        send(buffer);
    }

    private void handleServerVersion(FriendlyByteBuf buffer) {
        if (state != STATE_AWAIT_VERSION) {
            fail(Component.literal("Unexpected server version packet in state " + state));
            return;
        }

        int version = buffer.readUnsignedShort();
        if (version != PROTOCOL_V3 && version != PROTOCOL_V4 && version != PROTOCOL_V5) {
            fail(Component.literal(version < PROTOCOL_V3 ? "Outdated Server" : "Outdated Client"));
            return;
        }

        int gameVers = buffer.readUnsignedShort();
        if (EaglerProtocolBridge.isAvailable()) {
            if (!EaglerProtocolBridge.supportsServerProtocol(gameVers)) {
                fail(Component.literal("This server does not support a compatible game protocol!"));
                return;
            }
        } else if (gameVers != GAME_PROTOCOL_VERSION) {
            fail(Component.literal("This server does not support 1.19.4!"));
            return;
        }

        protocolVersion = version;
        selectedGameProtocol = gameVers;

        int msgLen = buffer.readUnsignedByte();
        byte[] dat = new byte[msgLen];
        buffer.readBytes(dat);
        pluginBrand = ArrayUtils.asciiString(dat);

        msgLen = buffer.readUnsignedByte();
        dat = new byte[msgLen];
        buffer.readBytes(dat);
        pluginVersion = ArrayUtils.asciiString(dat);

        LOGGER.info("Eaglercraft handshake: protocol v{}, server {} {}", version, pluginBrand, pluginVersion);

        int authType = buffer.readUnsignedByte();
        int saltLength = buffer.readUnsignedShort();
        byte[] salt = new byte[saltLength];
        buffer.readBytes(salt);

        if (version >= PROTOCOL_V5) {
            nicknameSelection = buffer.readBoolean();
        }

        sendClientRequestLogin(authType, salt);
        if (state == STATE_FINISHED) {
            return;
        }

        state = STATE_AWAIT_LOGIN;
    }

    private void sendClientRequestLogin(int authType, byte[] salt) {
        byte[] hashedPassword = resolvePassword(authType, salt);
        if (state == STATE_FINISHED) {
            return;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(HandshakePacketTypes.PROTOCOL_CLIENT_REQUEST_LOGIN);

        if (protocolVersion >= PROTOCOL_V5 && !nicknameSelection) {
            buffer.writeByte(0);
        } else {
            buffer.writeByte(username.length());
            writeASCII(buffer, username);
        }

        String requestedServer = "default";
        buffer.writeByte(requestedServer.length());
        writeASCII(buffer, requestedServer);

        if (hashedPassword != null) {
            buffer.writeByte(hashedPassword.length);
            buffer.writeBytes(hashedPassword);
        } else {
            buffer.writeByte(0);
        }

        if (protocolVersion >= PROTOCOL_V4) {
            buffer.writeBoolean(false);
            buffer.writeByte(0);
        }

        if (protocolVersion >= PROTOCOL_V5) {
            // Advertise the same standard capabilities the reference 1.19.4
            // client does: REDIRECT (2), NOTIFICATION (3) and PAUSE_MENU (4),
            // each at capability version 1. Sending zero made some proxies
            // treat the client as a bare-bones 1.8 client.
            buffer.writeVarInt(CLIENT_CAPABILITIES);
            buffer.writeByte(1);
            buffer.writeByte(1);
            buffer.writeByte(1);
            buffer.writeByte(0);
        }

        send(buffer);
    }

    @Nullable
    private byte[] resolvePassword(int authType, byte[] salt) {
        if (password == null) {
            return null;
        }

        switch (authType) {
            case HandshakePacketTypes.AUTH_METHOD_NONE:
                return null;
            case HandshakePacketTypes.AUTH_METHOD_EAGLER_SHA256:
                if (salt.length < 64) {
                    fail(Component.literal("Server sent an invalid authentication salt"));
                    return null;
                }
                return AuthTypes.applyEaglerSHA256(password, salt);
            case HandshakePacketTypes.AUTH_METHOD_AUTHME_SHA256:
                return AuthTypes.applyAuthMeSHA256(password, salt);
            case HandshakePacketTypes.AUTH_METHOD_PLAINTEXT:
                byte[] plain = password.getBytes(StandardCharsets.UTF_8);
                if (plain.length > 255) {
                    fail(Component.literal("Password is too long"));
                    return null;
                }
                return plain;
            default:
                fail(Component.literal("Unknown auth method #" + authType + " requested"));
                return null;
        }
    }

    private void handleServerAllowLogin(FriendlyByteBuf buffer) {
        if (state != STATE_AWAIT_LOGIN) {
            fail(Component.literal("Unexpected allow login packet in state " + state));
            return;
        }

        int msgLen = buffer.readUnsignedByte();
        byte[] dat = new byte[msgLen];
        buffer.readBytes(dat);
        String name = ArrayUtils.asciiString(dat);

        EaglercraftUUID uuid = new EaglercraftUUID(buffer.readLong(), buffer.readLong());

        if (protocolVersion >= PROTOCOL_V5) {
            int standardCaps = buffer.readVarInt();
            int capCount = Integer.bitCount(standardCaps);
            if (capCount > 0) {
                buffer.readBytes(new byte[capCount]);
            }

            int extCaps = buffer.readUnsignedByte();
            for (int i = 0; i < extCaps; ++i) {
                buffer.readLong();
                buffer.readLong();
                buffer.readByte();
            }
        }

        loggedInUsername = name;
        loggedInUUID = uuid;

        LOGGER.info("Eaglercraft login allowed for {} ({})", name, uuid);

        sendClientProfileData();
        if (state == STATE_FINISHED) {
            return;
        }

        websocket.send(new byte[] { (byte) HandshakePacketTypes.PROTOCOL_CLIENT_FINISH_LOGIN });
        state = STATE_AWAIT_FINISH;
    }

    private void sendClientProfileData() {
        if (protocolVersion < PROTOCOL_V4) {
            return;
        }

        String profileDataType = "brand_uuid_v1";
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeByte(HandshakePacketTypes.PROTOCOL_CLIENT_PROFILE_DATA);
        buffer.writeByte(1);
        buffer.writeByte(profileDataType.length());
        writeASCII(buffer, profileDataType);
        buffer.writeShort(16);
        buffer.writeLong(EaglercraftVersion.clientBrandUUID.msb);
        buffer.writeLong(EaglercraftVersion.clientBrandUUID.lsb);
        send(buffer);
    }

    private void handleServerFinishLogin() {
        if (state != STATE_AWAIT_FINISH) {
            fail(Component.literal("Unexpected finish login packet in state " + state));
            return;
        }

        state = STATE_FINISHED;

        if (loggedInUUID == null) {
            fail(Component.literal("Server finished login without offering a profile"));
            return;
        }

        websocket.setEnableStringFrames(false);
        websocket.clearStringFrames();

        UUID profileId = new UUID(loggedInUUID.msb, loggedInUUID.lsb);
        if (EaglerProtocolBridge.isAvailable()) {
            LOGGER.info("Eaglercraft protocol bridge: translating {} <-> {}", GAME_PROTOCOL_VERSION, selectedGameProtocol);
            if (!EaglerProtocolBridge.initialize(loggedInUsername, profileId, GAME_PROTOCOL_VERSION, selectedGameProtocol)) {
                fail(Component.literal("Failed to initialize the client-side protocol bridge"));
                return;
            }
        }

        connection.setProtocol(ConnectionProtocol.PLAY);
        GameProfile profile = new GameProfile(profileId, loggedInUsername);
        connection.setListener(new ClientPacketListener(minecraft, parent, connection, serverData, profile,
                minecraft.getTelemetryManager().createWorldSessionManager(false, null)));
    }

    private void handleServerDenyLogin(FriendlyByteBuf buffer) {
        int msgLen = buffer.readUnsignedShort();
        byte[] dat = new byte[msgLen];
        buffer.readBytes(dat);
        fail(parseMessage(dat));
    }

    private void handleServerError(FriendlyByteBuf buffer) {
        boolean preVersion = state == STATE_AWAIT_VERSION;
        int errCode = buffer.readUnsignedByte();
        int msgLen = preVersion ? buffer.readUnsignedByte() : buffer.readUnsignedShort();
        byte[] dat = new byte[msgLen];
        buffer.readBytes(dat);

        if (errCode == HandshakePacketTypes.SERVER_ERROR_CUSTOM_MESSAGE) {
            fail(preVersion ? literalMessage(dat) : parseMessage(dat));
        } else if (errCode == HandshakePacketTypes.SERVER_ERROR_AUTHENTICATION_REQUIRED) {
            fail(literalMessage(dat));
        } else {
            fail(Component.literal("Server error #" + errCode + "\n" + new String(dat, StandardCharsets.UTF_8)));
        }
    }

    private void handleVersionMismatch(FriendlyByteBuf buffer) {
        int count = buffer.readUnsignedShort();
        for (int i = 0; i < count; ++i) {
            buffer.readUnsignedShort();
        }

        count = buffer.readUnsignedShort();
        for (int i = 0; i < count; ++i) {
            buffer.readUnsignedShort();
        }

        int msgLen = buffer.readUnsignedByte();
        byte[] dat = new byte[msgLen];
        buffer.readBytes(dat);
        fail(literalMessage(dat));
    }

    private void handleServerRedirect(FriendlyByteBuf buffer) {
        if (protocolVersion < PROTOCOL_V5) {
            fail(Component.literal("Unexpected login redirect packet"));
            return;
        }
        int len = buffer.readShort();
        byte[] dat = new byte[len];
        buffer.readBytes(dat);
        redirectAddress = new String(dat, StandardCharsets.UTF_8);
        state = STATE_FINISHED;
    }

    private Component parseMessage(byte[] json) {
        String str = new String(json, StandardCharsets.UTF_8);
        try {
            Component parsed = Component.Serializer.fromJson(str);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not parse a disconnect message as JSON", ex);
        }

        return Component.literal(str);
    }

    private static Component literalMessage(byte[] data) {
        return Component.literal(new String(data, StandardCharsets.UTF_8));
    }

    private void fail(Component reason) {
        if (state == STATE_FINISHED) {
            return;
        }

        state = STATE_FINISHED;
        failureReason = reason;
    }

    private void send(FriendlyByteBuf buffer) {
        byte[] out = new byte[buffer.readableBytes()];
        buffer.readBytes(out);
        websocket.send(out);
    }

    private static void writeASCII(FriendlyByteBuf buffer, String str) {
        for (int i = 0, l = str.length(); i < l; ++i) {
            buffer.writeByte(str.charAt(i));
        }
    }

}
