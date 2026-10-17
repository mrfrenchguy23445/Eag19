package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.security.PrivateKey;
import java.security.Signature;
import org.slf4j.Logger;

public interface Signer {
   Logger LOGGER = LogUtils.getLogger();

   byte[] sign(SignatureUpdater var1);

   default byte[] sign(byte[] param0) {
      return this.sign(l_param1 -> l_param1.update(param0));
   }

   static Signer from(PrivateKey param0, String param1) {
      return l_param2 -> {
         try {
            Signature l_var3 = Signature.getInstance(param1);
            l_var3.initSign(param0);
            l_param2.update(l_var3::update);
            return l_var3.sign();
         } catch (Exception l_var3) {
            throw new IllegalStateException("Failed to sign message", l_var3);
         }
      };
   }
}
