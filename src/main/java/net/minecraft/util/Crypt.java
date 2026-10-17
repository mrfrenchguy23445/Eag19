package net.minecraft.util;

import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.network.FriendlyByteBuf;

public class Crypt {
   private static final String SYMMETRIC_ALGORITHM = "AES";
   private static final int SYMMETRIC_BITS = 128;
   private static final String ASYMMETRIC_ALGORITHM = "RSA";
   private static final int ASYMMETRIC_BITS = 1024;
   private static final String BYTE_ENCODING = "ISO_8859_1";
   private static final String HASH_ALGORITHM = "SHA-1";
   public static final String SIGNING_ALGORITHM = "SHA256withRSA";
   public static final int SIGNATURE_BYTES = 256;
   private static final String PEM_RSA_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
   private static final String PEM_RSA_PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
   public static final String RSA_PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
   private static final String RSA_PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";
   public static final String MIME_LINE_SEPARATOR = "\n";
   // Base64.getMimeEncoder(int, byte[]) is not present in the TeaVM classlib. The MIME
     // encoder from the standard JDK produces a similar newline behaviour in this context.
     // Neither Base64.getMimeEncoder() nor its (int, byte[]) overload exists in the
  // WASM-GC TeaVM classlib, and Base64.Encoder cannot be subclassed because its
  // constructor is package-private. MIME_ENCODER wraps the standard encoder at the
  // same 76 characters per line the JDK MIME encoder would use, which is what the
  // PEM armour below expects. Only encodeToString is needed by this class.
  public static final MimeLineEncoder MIME_ENCODER = new MimeLineEncoder(76);
   // Counterpart to MIME_ENCODER; see MimeLineDecoder for why the JDK's getMimeDecoder() is
   // unavailable here.
   public static final MimeLineDecoder MIME_DECODER = new MimeLineDecoder();
   public static final Codec<PublicKey> PUBLIC_KEY_CODEC = Codec.STRING.comapFlatMap(param0 -> {
      try {
         return DataResult.success(stringToRsaPublicKey(param0));
      } catch (CryptException var1) {
         return DataResult.error(var1::getMessage);
      }
   }, Crypt::rsaPublicKeyToString);
   public static final Codec<PrivateKey> PRIVATE_KEY_CODEC = Codec.STRING.comapFlatMap(param0 -> {
      try {
         return DataResult.success(stringToPemRsaPrivateKey(param0));
      } catch (CryptException var1) {
         return DataResult.error(var1::getMessage);
      }
   }, Crypt::pemRsaPrivateKeyToString);

   public static SecretKey generateSecretKey() throws CryptException {
      try {
         KeyGenerator var0 = KeyGenerator.getInstance("AES");
         var0.init(128);
         return var0.generateKey();
      } catch (Exception var0) {
         throw new CryptException(var0);
      }
   }

   public static KeyPair generateKeyPair() throws CryptException {
      try {
         KeyPairGenerator var0 = KeyPairGenerator.getInstance("RSA");
         var0.initialize(1024);
         return var0.generateKeyPair();
      } catch (Exception var0) {
         throw new CryptException(var0);
      }
   }

   public static byte[] digestData(String param0, PublicKey param1, SecretKey param2) throws CryptException {
      try {
         return digestData(param0.getBytes("ISO_8859_1"), param2.getEncoded(), param1.getEncoded());
      } catch (Exception var3) {
         throw new CryptException(var3);
      }
   }

   private static byte[] digestData(byte[]... param0) throws Exception {
      MessageDigest var1 = MessageDigest.getInstance("SHA-1");

      for (byte[] var5 : param0) {
         var1.update(var5);
      }

      return var1.digest();
   }

   private static <T extends Key> T rsaStringToKey(String param0, String param1, String param2, Crypt.ByteArrayToKeyFunction<T> param3) throws CryptException {
      int var4 = param0.indexOf(param1);
      if (var4 != -1) {
         var4 += param1.length();
         int var5 = param0.indexOf(param2, var4);
         param0 = param0.substring(var4, var5 + 1);
      }

      try {
         return param3.apply(MIME_DECODER.decode(param0));
      } catch (IllegalArgumentException var5) {
         throw new CryptException(var5);
      }
   }

   public static PrivateKey stringToPemRsaPrivateKey(String param0) throws CryptException {
      return rsaStringToKey(param0, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----", Crypt::byteToPrivateKey);
   }

   public static PublicKey stringToRsaPublicKey(String param0) throws CryptException {
      return rsaStringToKey(param0, "-----BEGIN RSA PUBLIC KEY-----", "-----END RSA PUBLIC KEY-----", Crypt::byteToPublicKey);
   }

   public static String rsaPublicKeyToString(PublicKey param0) {
      if (!"RSA".equals(param0.getAlgorithm())) {
         throw new IllegalArgumentException("Public key must be RSA");
      } else {
         return "-----BEGIN RSA PUBLIC KEY-----\n" + MIME_ENCODER.encodeToString(param0.getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n";
      }
   }

   public static String pemRsaPrivateKeyToString(PrivateKey param0) {
      if (!"RSA".equals(param0.getAlgorithm())) {
         throw new IllegalArgumentException("Private key must be RSA");
      } else {
         return "-----BEGIN RSA PRIVATE KEY-----\n" + MIME_ENCODER.encodeToString(param0.getEncoded()) + "\n-----END RSA PRIVATE KEY-----\n";
      }
   }

   private static PrivateKey byteToPrivateKey(byte[] param0) throws CryptException {
      try {
         EncodedKeySpec var1 = new PKCS8EncodedKeySpec(param0);
         KeyFactory var2 = KeyFactory.getInstance("RSA");
         return var2.generatePrivate(var1);
      } catch (Exception var1) {
         throw new CryptException(var1);
      }
   }

   public static PublicKey byteToPublicKey(byte[] param0) throws CryptException {
      try {
         EncodedKeySpec var1 = new X509EncodedKeySpec(param0);
         KeyFactory var2 = KeyFactory.getInstance("RSA");
         return var2.generatePublic(var1);
      } catch (Exception var1) {
         throw new CryptException(var1);
      }
   }

   public static SecretKey decryptByteToSecretKey(PrivateKey param0, byte[] param1) throws CryptException {
      byte[] var2 = decryptUsingKey(param0, param1);

      try {
         return new SecretKeySpec(var2, "AES");
      } catch (Exception var3) {
         throw new CryptException(var3);
      }
   }

   public static byte[] encryptUsingKey(Key param0, byte[] param1) throws CryptException {
      return cipherData(1, param0, param1);
   }

   public static byte[] decryptUsingKey(Key param0, byte[] param1) throws CryptException {
      return cipherData(2, param0, param1);
   }

   private static byte[] cipherData(int param0, Key param1, byte[] param2) throws CryptException {
      try {
         return setupCipher(param0, param1.getAlgorithm(), param1).doFinal(param2);
      } catch (Exception var3) {
         throw new CryptException(var3);
      }
   }

   private static Cipher setupCipher(int param0, String param1, Key param2) throws Exception {
      Cipher var3 = Cipher.getInstance(param1);
      var3.init(param0, param2);
      return var3;
   }

   public static Cipher getCipher(int param0, Key param1) throws CryptException {
      try {
         Cipher var2 = Cipher.getInstance("AES/CFB8/NoPadding");
         var2.init(param0, param1, new IvParameterSpec(param1.getEncoded()));
         return var2;
      } catch (Exception var2) {
         throw new CryptException(var2);
      }
   }

   interface ByteArrayToKeyFunction<T extends Key> {
      T apply(byte[] var1) throws CryptException;
   }

   public record SaltSignaturePair(long salt, byte[] signature) {
      public static final Crypt.SaltSignaturePair EMPTY = new Crypt.SaltSignaturePair(0L, ByteArrays.EMPTY_ARRAY);

      public SaltSignaturePair(FriendlyByteBuf param0) {
         this(param0.readLong(), param0.readByteArray());
      }

      public boolean isValid() {
         return this.signature.length > 0;
      }

      public static void write(FriendlyByteBuf param0, Crypt.SaltSignaturePair param1) {
         param0.writeLong(param1.salt);
         param0.writeByteArray(param1.signature);
      }

      public byte[] saltAsBytes() {
         return Longs.toByteArray(this.salt);
      }
   }

   public static class SaltSupplier {
      private static final SecureRandom secureRandom = new SecureRandom();

      public static long getLong() {
         return secureRandom.nextLong();
      }
   }

   /**
    * Line-wraps Base64 output at a fixed column, standing in for the JDK's MIME encoder,
    * which the WASM-GC TeaVM classlib does not provide.
    *
    * <p>Wrapping happens after encoding rather than during it, so the output is identical to
    * {@code Base64.getMimeEncoder(lineLength, "\n")} for the column counts used here. No
    * trailing newline is added, matching the JDK encoder.
    */
   public static final class MimeLineEncoder {

      private final int lineLength;
      private final Encoder delegate = Base64.getEncoder();

      MimeLineEncoder(int lineLength) {
         this.lineLength = lineLength;
      }

      public byte[] encode(byte[] src) {
         return delegate.encode(src);
      }

      public String encodeToString(byte[] src) {
         String encoded = delegate.encodeToString(src);
         if (encoded.length() <= lineLength) {
            return encoded;
         }
         StringBuilder wrapped = new StringBuilder(encoded.length() + encoded.length() / lineLength + 1);
         for (int i = 0; i < encoded.length(); i += lineLength) {
            if (i > 0) {
               wrapped.append('\n');
            }
            wrapped.append(encoded, i, Math.min(i + lineLength, encoded.length()));
         }
         return wrapped.toString();
      }
   }

   /**
    * Decoder counterpart to {@link MimeLineEncoder}, replacing {@code Base64.getMimeDecoder()}.
    *
    * <p>The JDK's MIME decoder differs from the basic one in exactly one way: it ignores every
    * character outside the base64 alphabet instead of rejecting it. That is all this needs to do,
    * because the only input is PEM armour this class produced itself -- "-----BEGIN...-----" and
    * "-----END...-----" delimiters plus 76-column base64 with newlines -- and {@link
    * #rsaStringToKey} has already stripped the delimiters before calling.
    *
    * <p>Rather than filter the string and hand it to the basic decoder, this decodes directly with
    * {@link Base64#getDecoder()} over a compact copy. Filtering into a new array would be the more
    * obvious implementation, but reusing the JDK decoder keeps the alphabet and padding validation
    * in one place rather than duplicating it here.
    */
   public static final class MimeLineDecoder {

      private final Decoder delegate = Base64.getDecoder();

      public byte[] decode(String src) {
         StringBuilder compact = new StringBuilder(src.length());
         for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (isBase64(c)) {
               compact.append(c);
            }
         }
         return delegate.decode(compact.toString());
      }

      private static boolean isBase64(char c) {
         return (c >= 'A' && c <= 'Z')
            || (c >= 'a' && c <= 'z')
            || (c >= '0' && c <= '9')
            || c == '+'
            || c == '/'
            || c == '=';
      }
   }
}
