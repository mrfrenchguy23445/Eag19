package com.mojang.realmsclient.exception;

import com.mojang.realmsclient.client.RealmsError;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;

public class RealmsServiceException extends Exception {
   public final int httpResultCode;
   public final String rawResponse;
   @Nullable
   public final RealmsError realmsError;

   public RealmsServiceException(int param0, String param1, RealmsError param2) {
      super(param1);
      this.httpResultCode = param0;
      this.rawResponse = param1;
      this.realmsError = param2;
   }

   public RealmsServiceException(int param0, String param1) {
      super(param1);
      this.httpResultCode = param0;
      this.rawResponse = param1;
      this.realmsError = null;
   }

   @Override
   public String toString() {
      if (this.realmsError != null) {
         String var1 = "mco.errorMessage." + this.realmsError.getErrorCode();
         String var2 = I18n.exists(var1) ? I18n.get(var1) : this.realmsError.getErrorMessage();
         return String.format(Locale.ROOT, "Realms service error (%d/%d) %s", this.httpResultCode, this.realmsError.getErrorCode(), var2);
      } else {
         return String.format(Locale.ROOT, "Realms service error (%d) %s", this.httpResultCode, this.rawResponse);
      }
   }

   public int realmsErrorCodeOrDefault(int param0) {
      return this.realmsError != null ? this.realmsError.getErrorCode() : param0;
   }
}
