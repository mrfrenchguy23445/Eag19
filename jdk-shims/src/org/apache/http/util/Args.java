package org.apache.http.util;

/**
 * Stub of org.apache.http.util.Args (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Only notNull(T, String) is kept, because it is the only one this port calls:
 * FileUpload.CustomInputStreamEntity.writeTo opens with Args.notNull(out, "Output
 * stream"). The real one throws NullPointerException with the name in the message, and so
 * does this one -- that is the behaviour, not an implementation detail, since a caller
 * catching NullPointerException around a null check is relying on it.
 *
 * The real class also offers check, notEmpty, notBlank, positive and notNegative. Those
 * are omitted rather than stubbed: nothing calls them, and each has a different return
 * type that would widen this file's surface for no benefit.
 */
public class Args {

   public static <T> T notNull(T param0, String param1) {
      if (param0 == null) {
         throw new NullPointerException(param1 + " may not be null");
      }

      return param0;
   }
}
