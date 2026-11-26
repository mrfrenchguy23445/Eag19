package net.minecraft.world.level.timers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public class FunctionCallback implements TimerCallback<MinecraftServer> {
   final ResourceLocation functionId;

   public FunctionCallback(ResourceLocation param0) {
      this.functionId = param0;
   }

   public void handle(MinecraftServer param0, TimerQueue<MinecraftServer> param1, long param2) {
      ServerFunctionManager var5 = param0.getFunctions();
      var5.get(this.functionId).ifPresent(l_param1 -> var5.execute(l_param1, var5.getGameLoopSender()));
   }

   public static class Serializer extends TimerCallback.Serializer<MinecraftServer, FunctionCallback> {
      public Serializer() {
         super(new ResourceLocation("function"), FunctionCallback.class);
      }

      public void serialize(CompoundTag param0, FunctionCallback param1) {
         param0.putString("Name", param1.functionId.toString());
      }

      public FunctionCallback deserialize(CompoundTag param0) {
         ResourceLocation var2 = new ResourceLocation(param0.getString("Name"));
         return new FunctionCallback(var2);
      }
   }
}
