package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * Stub of org.apache.http.entity.InputStreamEntity (httpclient 4.5.13).
 *
 * Part of the Apache HttpClient 4 stub set; see
 * org/apache/http/impl/client/HttpClientBuilder.java for the rationale.
 *
 * This one has a real subclass in the game: FileUpload.CustomInputStreamEntity extends it,
 * calls super(InputStream), and overrides writeTo to stream the file with a progress
 * callback. So the class must exist, must be non-final, must extend or implement
 * something HttpPost.setEntity accepts, and writeTo must be overridable with the real
 * signature -- which is why this stub implements HttpEntity directly instead of going
 * through AbstractHttpEntity the way the real class does.
 *
 * setContentType(String) is kept for the same reason: CustomInputStreamEntity calls it to
 * set application/octet-stream before handing itself to the request.
 *
 * writeTo's default here is the real behaviour for an InputStreamEntity -- copy the
 * stream to the output, nothing else. It is unreachable in this port (nothing can produce
 * an OutputStream to write to, since no request is ever executed) but it is written out
 * rather than left abstract, so that the class is usable on its own.
 */
public class InputStreamEntity implements HttpEntity {

   private static final int OUTPUT_BUFFER_SIZE = 4096;

   private final InputStream content;
   private final long length;
   private String contentType;

   public InputStreamEntity(InputStream param0) {
      this(param0, -1L);
   }

   public InputStreamEntity(InputStream param0, long param1) {
      this.content = param0;
      this.length = param1;
   }

   public boolean isRepeatable() {
      return false;
   }

   public boolean isChunked() {
      return false;
   }

   public long getContentLength() {
      return this.length;
   }

   public Header getContentType() {
      return this.contentType == null ? null : new BasicHeader("Content-Type", this.contentType);
   }

   public Header getContentEncoding() {
      return null;
   }

   public void setContentType(String param0) {
      this.contentType = param0;
   }

   public InputStream getContent() throws IOException, UnsupportedOperationException {
      return this.content;
   }

   public void writeTo(OutputStream param0) throws IOException {
      if (param0 == null) {
         throw new IllegalArgumentException("Output stream may not be null");
      }

      byte[] var1 = new byte[OUTPUT_BUFFER_SIZE];
      int var2;

      while ((var2 = this.content.read(var1)) != -1) {
         param0.write(var1, 0, var2);
      }
   }

   public boolean isStreaming() {
      return true;
   }

   public void consumeContent() throws IOException {
      this.content.close();
   }

   /**
    * The one value type here; the real httpcore BasicHeader, privately, because nothing
    * outside this file can observe it.
    */
   private static final class BasicHeader implements Header {

      private final String name;
      private final String value;

      BasicHeader(String param0, String param1) {
         this.name = param0;
         this.value = param1;
      }

      @Override
      public String getName() {
         return this.name;
      }

      @Override
      public String getValue() {
         return this.value;
      }
   }
}
