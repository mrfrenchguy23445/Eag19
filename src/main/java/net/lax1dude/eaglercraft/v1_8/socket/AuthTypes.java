package net.lax1dude.eaglercraft.v1_8.socket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class AuthTypes {

    private static final byte[] HEX = new byte[] {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public static byte[] applyEaglerSHA256(String password, byte[] salt) {
        MessageDigest digest = sha256();

        int passLen = password.length();

        digest.update((byte) ((passLen >>> 8) & 0xFF));
        digest.update((byte) (passLen & 0xFF));

        for (int i = 0; i < passLen; ++i) {
            char codePoint = password.charAt(i);
            digest.update((byte) ((codePoint >>> 8) & 0xFF));
            digest.update((byte) (codePoint & 0xFF));
        }

        digest.update(HandshakePacketTypes.EAGLER_SHA256_SALT_SAVE, 0, 32);

        byte[] hashed = digest.digest();

        digest.update(hashed, 0, 32);
        digest.update(salt, 0, 32);
        digest.update(HandshakePacketTypes.EAGLER_SHA256_SALT_BASE, 0, 32);

        hashed = digest.digest();

        digest.update(hashed, 0, 32);
        digest.update(salt, 32, 32);
        digest.update(HandshakePacketTypes.EAGLER_SHA256_SALT_BASE, 0, 32);

        return digest.digest();
    }

    public static byte[] applyAuthMeSHA256(String password, byte[] salt) {
        byte[] passwd = password.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = sha256();

        digest.update(passwd, 0, passwd.length);
        byte[] hashed = digest.digest();

        byte[] toHexAndSalt = new byte[64];
        for (int i = 0; i < 32; ++i) {
            toHexAndSalt[i << 1] = HEX[(hashed[i] >> 4) & 0xF];
            toHexAndSalt[(i << 1) + 1] = HEX[hashed[i] & 0xF];
        }

        digest.update(toHexAndSalt, 0, 64);
        digest.update(salt, 0, salt.length);
        hashed = digest.digest();

        for (int i = 0; i < 32; ++i) {
            toHexAndSalt[i << 1] = HEX[(hashed[i] >> 4) & 0xF];
            toHexAndSalt[(i << 1) + 1] = HEX[hashed[i] & 0xF];
        }

        return toHexAndSalt;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private AuthTypes() {
    }

}
