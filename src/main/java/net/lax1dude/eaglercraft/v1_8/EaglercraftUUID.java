package net.lax1dude.eaglercraft.v1_8;


/**
 * Copyright (c) 2022 lax1dude. All Rights Reserved.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
public class EaglercraftUUID implements Comparable<EaglercraftUUID> {

    private static final String HEX = "0123456789ABCDEF";
    public final long msb;
    public final long lsb;

    public EaglercraftUUID(long msb, long lsb) {
        this.msb = msb;
        this.lsb = lsb;
    }

    public EaglercraftUUID(byte[] uuid) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (uuid[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (uuid[i] & 0xff);
        this.msb = msb;
        this.lsb = lsb;
    }

    public EaglercraftUUID(String uuid) {
        String[] components = uuid.split("-");
        if (components.length != 5)
            throw new IllegalArgumentException("Invalid UUID string: " + uuid);
        for (int i = 0; i < 5; i++)
            components[i] = "0x" + components[i];

        long mostSigBits = Long.decode(components[0]).longValue();
        mostSigBits <<= 16;
        mostSigBits |= Long.decode(components[1]).longValue();
        mostSigBits <<= 16;
        mostSigBits |= Long.decode(components[2]).longValue();

        long leastSigBits = Long.decode(components[3]).longValue();
        leastSigBits <<= 48;
        leastSigBits |= Long.decode(components[4]).longValue();

        this.msb = mostSigBits;
        this.lsb = leastSigBits;
    }

    private static byte long7(long x) {
        return (byte) (x >> 56);
    }

    private static byte long6(long x) {
        return (byte) (x >> 48);
    }

    private static byte long5(long x) {
        return (byte) (x >> 40);
    }

    private static byte long4(long x) {
        return (byte) (x >> 32);
    }

    private static byte long3(long x) {
        return (byte) (x >> 24);
    }

    private static byte long2(long x) {
        return (byte) (x >> 16);
    }

    private static byte long1(long x) {
        return (byte) (x >> 8);
    }

    private static byte long0(long x) {
        return (byte) (x);
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    private static int nibbleValue(char c) {
        int v = HEX.indexOf(Character.toUpperCase(c));
        if (v == -1) {
            return 0;
        } else {
            return v;
        }
    }

    private static long parse4Nibbles(String name, int pos) {
        int ch1 = nibbleValue(name.charAt(pos));
        int ch2 = nibbleValue(name.charAt(pos + 1));
        int ch3 = nibbleValue(name.charAt(pos + 2));
        int ch4 = nibbleValue(name.charAt(pos + 3));
        return (ch1 << 12) | (ch2 << 8) | (ch3 << 4) | ch4;
    }

    public static EaglercraftUUID fromString(String name) {
        if (name.length() == 36) {
            char ch1 = name.charAt(8);
            char ch2 = name.charAt(13);
            char ch3 = name.charAt(18);
            char ch4 = name.charAt(23);
            if (ch1 == '-' && ch2 == '-' && ch3 == '-' && ch4 == '-') {
                long msb1 = parse4Nibbles(name, 0);
                long msb2 = parse4Nibbles(name, 4);
                long msb3 = parse4Nibbles(name, 9);
                long msb4 = parse4Nibbles(name, 14);
                long lsb1 = parse4Nibbles(name, 19);
                long lsb2 = parse4Nibbles(name, 24);
                long lsb3 = parse4Nibbles(name, 28);
                long lsb4 = parse4Nibbles(name, 32);
                if ((msb1 | msb2 | msb3 | msb4 | lsb1 | lsb2 | lsb3 | lsb4) >= 0) {
                    return new EaglercraftUUID(msb1 << 48 | msb2 << 32 | msb3 << 16 | msb4,
                            lsb1 << 48 | lsb2 << 32 | lsb3 << 16 | lsb4);
                }
            }
        }
        return fromString1(name);
    }

    private static EaglercraftUUID fromString1(String name) {
        int len = name.length();
        if (len > 36) {
            throw new IllegalArgumentException("UUID string too large");
        }

        int dash1 = name.indexOf('-', 0);
        int dash2 = name.indexOf('-', dash1 + 1);
        int dash3 = name.indexOf('-', dash2 + 1);
        int dash4 = name.indexOf('-', dash3 + 1);
        int dash5 = name.indexOf('-', dash4 + 1);

        if (dash4 < 0 || dash5 >= 0) {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }

        long mostSigBits = JDKBackports.parseLong(name, 0, dash1, 16) & 0xffffffffL;
        mostSigBits <<= 16;
        mostSigBits |= JDKBackports.parseLong(name, dash1 + 1, dash2, 16) & 0xffffL;
        mostSigBits <<= 16;
        mostSigBits |= JDKBackports.parseLong(name, dash2 + 1, dash3, 16) & 0xffffL;
        long leastSigBits = JDKBackports.parseLong(name, dash3 + 1, dash4, 16) & 0xffffL;
        leastSigBits <<= 48;
        leastSigBits |= JDKBackports.parseLong(name, dash4 + 1, len, 16) & 0xffffffffffffL;

        return new EaglercraftUUID(mostSigBits, leastSigBits);
    }

    public static EaglercraftUUID nameUUIDFromBytes(byte[] bytes) {
        // 1.19.4 port: the Eaglercraft-vendored net.lax1dude.eaglercraft.v1_8.crypto
        // package has been removed along with the other non-vanilla subsystems.
        //
        // Do NOT delegate to java.util.UUID.nameUUIDFromBytes here. TeaVM renames
        // this class to java.util.UUID at link time (src/teavm/resources/META-INF/
        // teavm.properties: "mapClass|net.lax1dude.eaglercraft.v1_8.EaglercraftUUID
        // =java.util.UUID"), so that call resolves back to THIS method and
        // recurses until the JS stack overflows. TeaVM's classlib also ships no
        // java.security.MessageDigest, so MD5 is inlined below.
        //
        // Matches java.util.UUID.nameUUIDFromBytes exactly: MD5, clear/set the
        // version-3 nibble, set the IETF variant bits, read big-endian.
        byte[] hash = md5(bytes);
        hash[6] &= 0x0f; /* clear version */
        hash[6] |= 0x30; /* set to version 3 */
        hash[8] &= 0x3f; /* clear variant */
        hash[8] |= 0x80; /* set to IETF variant */
        return new EaglercraftUUID(hash);
    }

    private static final int[] MD5_S = { 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 5, 9, 14, 20,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21 };

    private static final int[] MD5_K = { 0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a,
            0xa8304613, 0xfd469501, 0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193,
            0xa679438e, 0x49b40821, 0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453,
            0xd8a1e681, 0xe7d3fbc8, 0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8,
            0x676f02d9, 0x8d2a4c8a, 0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9,
            0xf6bb4b60, 0xbebfbc70, 0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5,
            0x1fa27cf8, 0xc4ac5665, 0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92,
            0xffeff47d, 0x85845dd1, 0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235,
            0x2ad7d2bb, 0xeb86d391 };

    private static int rotl32(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static byte[] md5(byte[] input) {
        int a0 = 0x67452301;
        int b0 = 0xefcdab89;
        int c0 = 0x98badcfe;
        int d0 = 0x10325476;

        long bitLen = (long) input.length * 8L;
        int totalLen = (input.length + 1 + 8 + 63) & ~63;
        byte[] buf = new byte[totalLen];
        System.arraycopy(input, 0, buf, 0, input.length);
        buf[input.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            buf[totalLen - 8 + i] = (byte) (bitLen >>> (8 * i));
        }

        int[] m = new int[16];
        for (int off = 0; off < totalLen; off += 64) {
            for (int i = 0; i < 16; i++) {
                int p = off + i * 4;
                m[i] = (buf[p] & 0xff) | ((buf[p + 1] & 0xff) << 8) | ((buf[p + 2] & 0xff) << 16)
                        | ((buf[p + 3] & 0xff) << 24);
            }

            int a = a0, b = b0, c = c0, d = d0;
            for (int i = 0; i < 64; i++) {
                int f, g;
                if (i < 16) {
                    f = (b & c) | (~b & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | (~d & c);
                    g = (5 * i + 1) & 15;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) & 15;
                } else {
                    f = c ^ (b | ~d);
                    g = (7 * i) & 15;
                }
                int tmp = d;
                d = c;
                c = b;
                b = b + rotl32(a + f + MD5_K[i] + m[g], MD5_S[i]);
                a = tmp;
            }
            a0 += a;
            b0 += b;
            c0 += c;
            d0 += d;
        }

        byte[] out = new byte[16];
        for (int i = 0; i < 4; i++) {
            out[i] = (byte) (a0 >>> (8 * i));
            out[4 + i] = (byte) (b0 >>> (8 * i));
            out[8 + i] = (byte) (c0 >>> (8 * i));
            out[12 + i] = (byte) (d0 >>> (8 * i));
        }
        return out;
    }

    public static EaglercraftUUID randomUUID() {
        byte[] randomBytes = new byte[16];
        (new Random()).nextBytes(randomBytes);
        randomBytes[6] &= 0x0f; /* clear version */
        randomBytes[6] |= 0x40; /* set to version 4 */
        randomBytes[8] &= 0x3f; /* clear variant */
        randomBytes[8] |= 0x80; /* set to IETF variant */
        return new EaglercraftUUID(randomBytes);
    }

    public byte[] getBytes() {
        byte[] ret = new byte[16];
        ret[0] = long7(msb);
        ret[1] = long6(msb);
        ret[2] = long5(msb);
        ret[3] = long4(msb);
        ret[4] = long3(msb);
        ret[5] = long2(msb);
        ret[6] = long1(msb);
        ret[7] = long0(msb);
        ret[8] = long7(lsb);
        ret[9] = long6(lsb);
        ret[10] = long5(lsb);
        ret[11] = long4(lsb);
        ret[12] = long3(lsb);
        ret[13] = long2(lsb);
        ret[14] = long1(lsb);
        ret[15] = long0(lsb);
        return ret;
    }

    @Override
    public String toString() {
        return (digits(msb >> 32, 8) + "-" + digits(msb >> 16, 4) + "-" + digits(msb, 4) + "-" + digits(lsb >> 48, 4)
                + "-" + digits(lsb, 12));
    }

    @Override
    public int hashCode() {
        long hilo = msb ^ lsb;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof EaglercraftUUID) && ((EaglercraftUUID) o).lsb == lsb && ((EaglercraftUUID) o).msb == msb;
    }

    public long getMostSignificantBits() {
        return msb;
    }

    public long getLeastSignificantBits() {
        return lsb;
    }

    @Override
    public int compareTo(EaglercraftUUID val) {
        return (this.msb < val.msb ? -1
                : (this.msb > val.msb ? 1 : (this.lsb < val.lsb ? -1 : (this.lsb > val.lsb ? 1 : 0))));
    }

}