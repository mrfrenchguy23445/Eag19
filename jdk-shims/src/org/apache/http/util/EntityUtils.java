package org.apache.http.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;

/**
 * Stub of org.apache.http.util.EntityUtils (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Only toString(HttpEntity, String) is kept: FileUpload.handleResponse parses the Realms
 * error body with it, inside a try that already catches Exception, so ParseException being
 * unchecked and IOException being declared are both load-bearing and are reproduced
 * exactly.
 *
 * The decoding is the real algorithm -- drain the entity, decode with the named charset,
 * falling back to ISO-8859-1 when the name is unknown, which is what httpcore does because
 * that is the HTTP default. It cannot be reached in this port, since the only way to
 * obtain an HttpEntity here is from a response that would have had to come from
 * HttpClientBuilder.build(), which refuses.
 */
public final class EntityUtils {

   private EntityUtils() {
   }

   public static String toString(HttpEntity param0, String param1) throws IOException, ParseException {
      if (param0 == null) {
         return null;
      }

      InputStream var2 = param0.getContent();

      if (var2 == null) {
         return null;
      }

      try {
         ByteArrayOutputStream var3 = new ByteArrayOutputStream();
         byte[] var4 = new byte[4096];
         int var5;

         while ((var5 = var2.read(var4)) != -1) {
            var3.write(var4, 0, var5);
         }

         return new String(var3.toByteArray(), toCharset(param1));
      } finally {
         var2.close();
      }
   }

   private static Charset toCharset(String param0) throws ParseException {
      if (param0 == null) {
         return StandardCharsets.ISO_8859_1;
      }

      try {
         return Charset.forName(param0);
      } catch (RuntimeException var1) {
         throw new ParseException("Unsupported charset: " + param0, var1);
      }
   }
}
