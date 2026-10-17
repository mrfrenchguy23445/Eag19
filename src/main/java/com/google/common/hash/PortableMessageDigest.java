/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.hash;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A minimal, dependency-free replacement for the subset of {@code java.security.MessageDigest} that
 * Guava's {@link MessageDigestHashFunction} actually uses.
 *
 * <p><b>Why this exists.</b> {@code java.security.MessageDigest} is absent from the WASM-GC TeaVM
 * classlib used by the Eaglercraft targets -- that classlib contains no {@code java.security}
 * package at all (the shipped {@code teavm-classlib} sources jar has 1100 entries, none of them
 * under {@code java/security/}), and {@code java.*} classes cannot be supplied from this project's
 * own source tree because TeaVM resolves that package from its internal model. Guava is vendored as
 * source precisely so that it <i>can</i> be patched, which makes this the natural home for the fix.
 *
 * <p>This is not optional plumbing for Minecraft: {@code Hashing.sha1()} backs skin texture
 * locations ({@code AbstractClientPlayer}), multiplayer server-list icons
 * ({@code ServerSelectionList}), resource- and world-icon paths, downloaded-pack paths and
 * {@code HashCache}. Without SHA-1 those are all broken, not merely degraded.
 *
 * <p><b>Scope.</b> MD5, SHA-1 and SHA-256 are implemented, because those are the algorithms
 * Minecraft reaches for. SHA-384 and SHA-512 are deliberately <i>not</i> implemented -- see
 * {@link #UNSUPPORTED} and {@link MessageDigestHashFunction} for why shipping a hand-written
 * SHA-512 is worse than refusing one.
 *
 * <p><b>Correctness.</b> These implementations are verified against the published FIPS/RFC test
 * vectors (empty string, {@code "abc"}, and the 56-byte SHA-256 vector). They are not
 * side-channel-hardened and must not be used where that matters.
 *
 * <p>The streaming {@code update}/{@code digest} contract matches {@code MessageDigest}: it is
 * incremental, {@link #digest()} finalises and resets the instance, and {@link #update(ByteBuffer)}
 * consumes from the buffer's position without disturbing it.
 */
abstract class PortableMessageDigest implements Cloneable {

  /** Algorithms recognised but deliberately not implemented. See the class comment. */
  static final String UNSUPPORTED = "not implemented on this platform (no java.security)";

  private final String algorithm;
  private final int digestLength;
  private final int blockLength;
  // Mutable digest state. These are `protected` rather than `private` on purpose: the algorithm
  // implementations below are static nested classes, and a static nested class cannot inherit a
  // private field from its enclosing class -- javac would resolve the simple name against the
  // (nonexistent) enclosing instance and reject it as a static-context reference.
  protected final byte[] block;
  protected final long[] state;
  protected int blockPos;
  protected long totalBytes;

  /**
   * The chaining state as the algorithm was defined, so {@link #reset()} can restore it. Without
   * this a digest instance silently returns wrong answers for every message after the first, which
   * is exactly the kind of bug that corrupts downloaded hashes rather than crashing.
   */
  private final long[] initialState;

  PortableMessageDigest(String algorithm, int digestLength, int blockLength, long[] state) {
    this.algorithm = algorithm;
    this.digestLength = digestLength;
    this.blockLength = blockLength;
    this.state = state;
    this.initialState = state.clone();
    this.block = new byte[blockLength];
  }

  /**
   * Creates a digest for {@code algorithmName}, or throws {@link UnsupportedOperationException} for
   * one this class does not implement.
   *
   * <p>Unknown algorithm names throw {@link UnsupportedOperationException} rather than
   * {@code NoSuchAlgorithmException}, because there is no provider to enumerate and a caller
   * reaching this has already lost the {@code java.security} contract.
   */
  static PortableMessageDigest create(String algorithmName) {
    String name = algorithmName.toUpperCase(java.util.Locale.ROOT);
    switch (name) {
      case "MD5":
        return new Md5();
      case "SHA-1":
        return new Sha1();
      case "SHA-256":
        return new Sha256();
      default:
        throw new UnsupportedOperationException(algorithmName + ": " + UNSUPPORTED);
    }
  }

  final String getAlgorithm() {
    return algorithm;
  }

  final int getDigestLength() {
    return digestLength;
  }

  final void update(byte b) {
    block[blockPos++] = b;
    totalBytes++;
    if (blockPos == blockLength) {
      compress(block, 0);
      blockPos = 0;
    }
  }

  final void update(byte[] input, int offset, int length) {
    if (offset < 0 || length < 0 || offset + length > input.length) {
      throw new IndexOutOfBoundsException(
          "offset=" + offset + ", length=" + length + ", array.length=" + input.length);
    }
    totalBytes += length;
    int i = 0;
    while (i < length) {
      int n = Math.min(blockLength - blockPos, length - i);
      System.arraycopy(input, offset + i, block, blockPos, n);
      blockPos += n;
      i += n;
      if (blockPos == blockLength) {
        compress(block, 0);
        blockPos = 0;
      }
    }
  }

  final void update(ByteBuffer input) {
    // Drain through a bounded scratch buffer rather than the caller's, and restore the caller's
    // position afterwards: this overload is a read-only view of the caller's data.
    int start = input.position();
    int remaining = input.remaining();
    byte[] scratch = new byte[Math.min(remaining, blockLength)];
    while (remaining > 0) {
      int chunk = Math.min(remaining, scratch.length);
      input.get(scratch, 0, chunk);
      update(scratch, 0, chunk);
      remaining -= chunk;
    }
    input.position(start);
  }

  /** Finalises, emits the digest, and resets to a fresh state so the instance can be reused. */
  final byte[] digest() {
    byte[] out = new byte[digestLength];
    padAndFinalise(out);
    reset();
    return out;
  }

  final void reset() {
    Arrays.fill(block, (byte) 0);
    System.arraycopy(initialState, 0, state, 0, state.length);
    blockPos = 0;
    totalBytes = 0;
  }

  /**
   * Appends the 0x80 marker, zero padding and the length, runs the final block, and writes the
   * digest into {@code out}.
   */
  private void padAndFinalise(byte[] out) {
    long bitLength = totalBytes * 8L;
    block[blockPos++] = (byte) 0x80;

    // The 8-byte length must fit in the final block. If it does not, pad this one out and emit an
    // extra all-zero block to make room.
    if (blockPos > blockLength - lengthFieldBytes()) {
      Arrays.fill(block, blockPos, blockLength, (byte) 0);
      compress(block, 0);
      blockPos = 0;
    }
    Arrays.fill(block, blockPos, blockLength - lengthFieldBytes(), (byte) 0);
    writeLength(block, bitLength);
    compress(block, 0);
    writeState(out);
  }

  /** Number of bytes the trailing length field occupies. 16 for the 128-bit-block digests. */
  int lengthFieldBytes() {
    return 8;
  }

  /** Writes the message bit-length, in the algorithm's own byte order, into the final block. */
  abstract void writeLength(byte[] block, long bitLength);

  /** Writes the chaining state into {@code out}, truncating or byte-swapping as the algorithm needs. */
  abstract void writeState(byte[] out);

  /** Processes one full block at {@code offset}. */
  abstract void compress(byte[] block, int offset);

  /**
   * Returns a digest in the same state as this one, sharing no mutable storage with it.
   *
   * <p>Implemented by each subclass rather than reflectively: the nested constructors are private
   * and {@code getDeclaredConstructor().newInstance()} on them requires {@code setAccessible},
   * which is exactly the kind of reflection this port is trying to avoid.
   */
  abstract PortableMessageDigest newInstance();

  private PortableMessageDigest copyStateInto(PortableMessageDigest copy) {
    System.arraycopy(this.block, 0, copy.block, 0, this.block.length);
    System.arraycopy(this.state, 0, copy.state, 0, this.state.length);
    copy.blockPos = this.blockPos;
    copy.totalBytes = this.totalBytes;
    return copy;
  }

  @Override
  public PortableMessageDigest clone() {
    return copyStateInto(newInstance());
  }

  // ---------------------------------------------------------------------------------------------
  // Shared helpers for reading a block as big-endian words.
  // ---------------------------------------------------------------------------------------------

  static int word(byte[] b, int off, int index) {
    int i = off + index * 4;
    return ((b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
  }

  static void putInt(byte[] out, int index, int value) {
    int i = index * 4;
    out[i] = (byte) (value >>> 24);
    out[i + 1] = (byte) (value >>> 16);
    out[i + 2] = (byte) (value >>> 8);
    out[i + 3] = (byte) value;
  }

  static int rotl(int x, int n) {
    return (x << n) | (x >>> (32 - n));
  }

  static int rotr(int x, int n) {
    return (x >>> n) | (x << (32 - n));
  }

  // ---------------------------------------------------------------------------------------------
  // MD5 (RFC 1321). Little-endian blocks, little-endian state output, 64-bit length.
  // ---------------------------------------------------------------------------------------------

  static final class Md5 extends PortableMessageDigest {
    private static final int[] K = {
      0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
      0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
      0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
      0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
      0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
      0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
      0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
      0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
    };
    private static final int[] S = {
      7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
      5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
      4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
      6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    };

    private final int[] m = new int[16];

    Md5() {
      super("MD5", 16, 64, new long[] {0x67452301L, 0xefcdab89L, 0x98badcfeL, 0x10325476L});
    }

    @Override
    PortableMessageDigest newInstance() {
      return new Md5();
    }

    @Override
    void compress(byte[] block, int off) {
      for (int i = 0; i < 16; i++) {
        m[i] = leWord(block, off, i);
      }
      int a = (int) state[0];
      int b = (int) state[1];
      int c = (int) state[2];
      int d = (int) state[3];
      for (int i = 0; i < 64; i++) {
        int f;
        int g;
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
        int sum = a + f + K[i] + m[g];
        b = b + rotl(sum, S[i]);
        a = tmp;
      }
      state[0] = (state[0] + a) & 0xFFFFFFFFL;
      state[1] = (state[1] + b) & 0xFFFFFFFFL;
      state[2] = (state[2] + c) & 0xFFFFFFFFL;
      state[3] = (state[3] + d) & 0xFFFFFFFFL;
    }

    @Override
    void writeLength(byte[] block, long bitLength) {
      for (int i = 0; i < 8; i++) {
        block[56 + i] = (byte) (bitLength >>> (8 * i));
      }
    }

    @Override
    void writeState(byte[] out) {
      for (int i = 0; i < 4; i++) {
        int v = (int) state[i];
        out[i * 4] = (byte) v;
        out[i * 4 + 1] = (byte) (v >>> 8);
        out[i * 4 + 2] = (byte) (v >>> 16);
        out[i * 4 + 3] = (byte) (v >>> 24);
      }
    }

    private static int leWord(byte[] b, int off, int index) {
      int i = off + index * 4;
      return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8) | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // SHA-1 (FIPS 180-4). Big-endian, 64-bit length, 20-byte digest.
  // ---------------------------------------------------------------------------------------------

  static final class Sha1 extends PortableMessageDigest {
    private final int[] w = new int[80];

    Sha1() {
      super("SHA-1", 20, 64, new long[] {0x67452301L, 0xEFCDAB89L, 0x98BADCFEL, 0x10325476L, 0xC3D2E1F0L});
    }

    @Override
    PortableMessageDigest newInstance() {
      return new Sha1();
    }

    @Override
    void compress(byte[] block, int off) {
      for (int i = 0; i < 16; i++) {
        w[i] = word(block, off, i);
      }
      for (int i = 16; i < 80; i++) {
        w[i] = rotl(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
      }
      int a = (int) state[0];
      int b = (int) state[1];
      int c = (int) state[2];
      int d = (int) state[3];
      int e = (int) state[4];
      for (int i = 0; i < 80; i++) {
        int f;
        int k;
        if (i < 20) {
          f = (b & c) | (~b & d);
          k = 0x5A827999;
        } else if (i < 40) {
          f = b ^ c ^ d;
          k = 0x6ED9EBA1;
        } else if (i < 60) {
          f = (b & c) | (b & d) | (c & d);
          k = 0x8F1BBCDC;
        } else {
          f = b ^ c ^ d;
          k = 0xCA62C1D6;
        }
        int temp = rotl(a, 5) + f + e + k + w[i];
        e = d;
        d = c;
        c = rotl(b, 30);
        b = a;
        a = temp;
      }
      state[0] = (state[0] + a) & 0xFFFFFFFFL;
      state[1] = (state[1] + b) & 0xFFFFFFFFL;
      state[2] = (state[2] + c) & 0xFFFFFFFFL;
      state[3] = (state[3] + d) & 0xFFFFFFFFL;
      state[4] = (state[4] + e) & 0xFFFFFFFFL;
    }

    @Override
    void writeLength(byte[] block, long bitLength) {
      for (int i = 0; i < 8; i++) {
        block[56 + i] = (byte) (bitLength >>> (56 - 8 * i));
      }
    }

    @Override
    void writeState(byte[] out) {
      for (int i = 0; i < 5; i++) {
        putInt(out, i, (int) state[i]);
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // SHA-256 (FIPS 180-2). Big-endian, 64-bit length, 32-byte digest.
  // ---------------------------------------------------------------------------------------------

  static final class Sha256 extends PortableMessageDigest {
    private static final int[] K = {
      0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
      0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
      0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
      0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
      0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
      0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
      0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
      0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    private final int[] w = new int[64];

    Sha256() {
      super("SHA-256", 32, 64, new long[] {
        0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL,
        0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L
      });
    }

    @Override
    PortableMessageDigest newInstance() {
      return new Sha256();
    }

    @Override
    void compress(byte[] block, int off) {
      for (int i = 0; i < 16; i++) {
        w[i] = word(block, off, i);
      }
      for (int i = 16; i < 64; i++) {
        int s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >>> 3);
        int s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >>> 10);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
      }
      int a = (int) state[0];
      int b = (int) state[1];
      int c = (int) state[2];
      int d = (int) state[3];
      int e = (int) state[4];
      int f = (int) state[5];
      int g = (int) state[6];
      int h = (int) state[7];
      for (int i = 0; i < 64; i++) {
        int S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25);
        int ch = (e & f) ^ (~e & g);
        int temp1 = h + S1 + ch + K[i] + w[i];
        int S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22);
        int maj = (a & b) ^ (a & c) ^ (b & c);
        int temp2 = S0 + maj;
        h = g;
        g = f;
        f = e;
        e = d + temp1;
        d = c;
        c = b;
        b = a;
        a = temp1 + temp2;
      }
      state[0] = (state[0] + a) & 0xFFFFFFFFL;
      state[1] = (state[1] + b) & 0xFFFFFFFFL;
      state[2] = (state[2] + c) & 0xFFFFFFFFL;
      state[3] = (state[3] + d) & 0xFFFFFFFFL;
      state[4] = (state[4] + e) & 0xFFFFFFFFL;
      state[5] = (state[5] + f) & 0xFFFFFFFFL;
      state[6] = (state[6] + g) & 0xFFFFFFFFL;
      state[7] = (state[7] + h) & 0xFFFFFFFFL;
    }

    @Override
    void writeLength(byte[] block, long bitLength) {
      for (int i = 0; i < 8; i++) {
        block[56 + i] = (byte) (bitLength >>> (56 - 8 * i));
      }
    }

    @Override
    void writeState(byte[] out) {
      for (int i = 0; i < 8; i++) {
        putInt(out, i, (int) state[i]);
      }
    }
  }
}