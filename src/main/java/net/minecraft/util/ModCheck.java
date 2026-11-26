package net.minecraft.util;

import java.util.function.Supplier;
import org.apache.commons.lang3.ObjectUtils;

public record ModCheck(ModCheck.Confidence confidence, String description) {
   public static ModCheck identify(String param0, Supplier<String> param1, String param2, Class<?> param3) {
      String var4 = param1.get();
      if (!param0.equals(var4)) {
         return new ModCheck(ModCheck.Confidence.DEFINITELY, param2 + " brand changed to '" + var4 + "'");
      } else {
         // Upstream distinguishes the remaining two cases with param3.getSigners() == null, i.e. "was
         // the jar's signature stripped?". Class.getSigners() is a ClassLoader and JAR-signing API
         // that does not exist in the WASM-GC TeaVM classlib, and it could not answer the question
         // here in any case: the client is served as assets over HTTP, not as a signed jar. Only the
         // brand string is verifiable, so the "signature untouched" verdict is the only one left and
         // is reported directly. param3 is retained in the signature because callers pass a
         // meaningful class and dropping it would churn every call site for no behavioural gain.
         return new ModCheck(ModCheck.Confidence.PROBABLY_NOT, param2 + " jar signature and brand is untouched");
      }
   }

   public boolean shouldReportAsModified() {
      return this.confidence.shouldReportAsModified;
   }

   public ModCheck merge(ModCheck param0) {
      return new ModCheck(ObjectUtils.max(this.confidence, param0.confidence), this.description + "; " + param0.description);
   }

   public String fullDescription() {
      return this.confidence.description + " " + this.description;
   }

   public enum Confidence {
      PROBABLY_NOT("Probably not.", false),
      VERY_LIKELY("Very likely;", true),
      DEFINITELY("Definitely;", true);

      final String description;
      final boolean shouldReportAsModified;

      Confidence(String param2, boolean param3) {
         this.description = param2;
         this.shouldReportAsModified = param3;
      }
   }
}
