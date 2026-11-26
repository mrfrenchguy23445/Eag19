package com.mojang.realmsclient.util.task;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.network.chat.Component;

public class ResettingTemplateWorldTask extends ResettingWorldTask {
   private final WorldTemplate template;

   public ResettingTemplateWorldTask(WorldTemplate param0, long param1, Component param3, Runnable param4) {
      super(param1, param3, param4);
      this.template = param0;
   }

   @Override
   protected void sendResetRequest(RealmsClient param0, long param1) throws RealmsServiceException {
      param0.resetWorldWithTemplate(param1, this.template.id);
   }
}
