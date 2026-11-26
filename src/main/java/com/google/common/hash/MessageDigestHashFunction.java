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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * {@link HashFunction} adapter for {@link PortableMessageDigest} instances.
 *
 * <p><b>Port note.</b> Upstream this wraps {@code java.security.MessageDigest} and keeps a live
 * prototype instance, cloning it per {@code newHasher()}. {@code java.security} does not exist in
 * this target's TeaVM classlib, so the digest comes from {@link PortableMessageDigest} instead.
 *
 * <p>The prototype field and the {@code supportsClone} dance are gone with it. Storing a
 * non-serialisable live digest in a {@code @Immutable}, {@code Serializable} hash function was always
 * a little awkward -- upstream only got away with it because {@code MessageDigest} happens to be
 * serialisable. The algorithm name is a proper immutable value, so the object now holds that plus
 * the digest length, and builds a digest per hasher.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 */
@Immutable
@ElementTypesAreNonnullByDefault
final class MessageDigestHashFunction extends AbstractHashFunction implements Serializable {

  private final String algorithmName;
  private final int bytes;
  private final String toString;

  MessageDigestHashFunction(String algorithmName, String toString) {
    this(algorithmName, digestLengthOf(algorithmName), toString);
  }

  MessageDigestHashFunction(String algorithmName, int bytes, String toString) {
    this.algorithmName = checkNotNull(algorithmName);
    this.toString = checkNotNull(toString);
    checkArgument(bytes >= 4 && bytes <= digestLengthOf(algorithmName),
        "bytes (%s) must be >= 4 and < %s", bytes, digestLengthOf(algorithmName));
    this.bytes = bytes;
  }

  /**
   * The output size, in bytes, of the named digest.
   *
   * <p>This is a static table rather than a call into a live digest instance, and that is
   * deliberate: {@code Hashing}'s five {@code MessageDigestHashFunction} constants are static
   * initialised, so anything that threw here would fail {@code Hashing.<clinit>} and take
   * {@link Hashing#sha1()} -- needed for skins, server icons and pack icons -- down with it.
   * Construction must stay total; only actual hashing may refuse an unimplemented algorithm, which
   * is why {@link PortableMessageDigest#create} is called from {@link #newHasher()} instead.
   */
  private static int digestLengthOf(String algorithmName) {
    String name = algorithmName.toUpperCase(java.util.Locale.ROOT);
    switch (name) {
      case "MD5":
        return 16;
      case "SHA-1":
        return 20;
      case "SHA-256":
        return 32;
      case "SHA-384":
        return 48;
      case "SHA-512":
        return 64;
      default:
        throw new IllegalArgumentException(
            "unknown digest algorithm: " + algorithmName + " (" + PortableMessageDigest.UNSUPPORTED + ")");
    }
  }

  @Override
  public int bits() {
    return bytes * Byte.SIZE;
  }

  @Override
  public String toString() {
    return toString;
  }

  @Override
  public Hasher newHasher() {
    return new MessageDigestHasher(PortableMessageDigest.create(algorithmName), bytes);
  }

  private static final class SerializedForm implements Serializable {
    private final String algorithmName;
    private final int bytes;
    private final String toString;

    private SerializedForm(String algorithmName, int bytes, String toString) {
      this.algorithmName = algorithmName;
      this.bytes = bytes;
      this.toString = toString;
    }

    private Object readResolve() {
      return new MessageDigestHashFunction(algorithmName, bytes, toString);
    }

    private static final long serialVersionUID = 0;
  }

  Object writeReplace() {
    return new SerializedForm(algorithmName, bytes, toString);
  }

  /** Hasher that updates a message digest. */
  private static final class MessageDigestHasher extends AbstractByteHasher {
    private final PortableMessageDigest digest;
    private final int bytes;
    private boolean done;

    private MessageDigestHasher(PortableMessageDigest digest, int bytes) {
      this.digest = digest;
      this.bytes = bytes;
    }

    @Override
    protected void update(byte b) {
      checkNotDone();
      digest.update(b);
    }

    @Override
    protected void update(byte[] b, int off, int len) {
      checkNotDone();
      digest.update(b, off, len);
    }

    @Override
    protected void update(ByteBuffer bytes) {
      checkNotDone();
      digest.update(bytes);
    }

    private void checkNotDone() {
      checkState(!done, "Cannot re-use a Hasher after calling hash() on it");
    }

    @Override
    public HashCode hash() {
      checkNotDone();
      done = true;
      return (bytes == digest.getDigestLength())
          ? HashCode.fromBytesNoCopy(digest.digest())
          : HashCode.fromBytesNoCopy(Arrays.copyOf(digest.digest(), bytes));
    }
  }
}
