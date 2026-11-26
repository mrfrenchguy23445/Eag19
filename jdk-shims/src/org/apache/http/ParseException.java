package org.apache.http;

/**
 * Stub of org.apache.http.ParseException (httpcore 4.4.15).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * Only here because EntityUtils.toString declares it in its throws clause. Unchecked, as
 * in the real jar -- extending RuntimeException rather than Exception is load-bearing, and
 * is what keeps the callers' existing catch (Exception) blocks compiling unchanged.
 */
public class ParseException extends RuntimeException {

   public ParseException() {
   }

   public ParseException(String param0) {
      super(param0);
   }

   public ParseException(String param0, Throwable param1) {
      super(param0, param1);
   }

   public ParseException(Throwable param0) {
      super(param0);
   }
}
