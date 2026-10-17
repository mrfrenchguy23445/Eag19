package net.lax1dude.eaglercraft.v1_8.socket;

public final class HandshakePacketTypes {

    public static final int PROTOCOL_CLIENT_VERSION = 0x01;
    public static final int PROTOCOL_SERVER_VERSION = 0x02;
    public static final int PROTOCOL_VERSION_MISMATCH = 0x03;
    public static final int PROTOCOL_CLIENT_REQUEST_LOGIN = 0x04;
    public static final int PROTOCOL_SERVER_ALLOW_LOGIN = 0x05;
    public static final int PROTOCOL_SERVER_DENY_LOGIN = 0x06;
    public static final int PROTOCOL_CLIENT_PROFILE_DATA = 0x07;
    public static final int PROTOCOL_CLIENT_FINISH_LOGIN = 0x08;
    public static final int PROTOCOL_SERVER_FINISH_LOGIN = 0x09;
    public static final int PROTOCOL_SERVER_REDIRECT_TO = 0x0A;
    public static final int PROTOCOL_SERVER_ERROR = 0xFF;

    public static final int AUTH_METHOD_NONE = 0x00;
    public static final int AUTH_METHOD_EAGLER_SHA256 = 0x01;
    public static final int AUTH_METHOD_AUTHME_SHA256 = 0x02;
    public static final int AUTH_METHOD_PLAINTEXT = 0xFF;

    public static final int SERVER_ERROR_RATELIMIT_BLOCKED = 0x06;
    public static final int SERVER_ERROR_RATELIMIT_LOCKED = 0x07;
    public static final int SERVER_ERROR_CUSTOM_MESSAGE = 0x08;
    public static final int SERVER_ERROR_AUTHENTICATION_REQUIRED = 0x09;

    public static final byte[] EAGLER_SHA256_SALT_BASE = new byte[] {
            117, 43, 1, 112, 75, 3, (byte) 188, 61, 121, 31, 34, (byte) 181, (byte) 234, 31, (byte) 247, 72,
            12, (byte) 168, (byte) 138, 45, (byte) 143, 77, 118, (byte) 245, (byte) 187, (byte) 242, (byte) 188,
            (byte) 219, (byte) 160, (byte) 235, (byte) 235, 68
    };

    public static final byte[] EAGLER_SHA256_SALT_SAVE = new byte[] {
            49, 25, 39, 38, (byte) 253, 85, 70, (byte) 245, 71, (byte) 150, (byte) 253, (byte) 206, 4, 26,
            (byte) 198, (byte) 249, (byte) 145, (byte) 251, (byte) 232, (byte) 174, (byte) 186, 98, 27,
            (byte) 232, 55, (byte) 144, 83, 21, 36, 55, (byte) 170, 118
    };

    private HandshakePacketTypes() {
    }

}
