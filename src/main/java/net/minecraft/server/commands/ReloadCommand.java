package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class ReloadCommand {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void reloadPacks(Collection<String> param0, CommandSourceStack param1) {
      param1.getServer().reloadResources(param0).exceptionally(l_param1 -> {
         LOGGER.warn("Failed to execute reload", l_param1);
         param1.sendFailure(Component.translatable("commands.reload.failure"));
         return null;
      });
   }

   private static Collection<String> discoverNewPacks(PackRepository param0, WorldData param1, Collection<String> param2) {
      param0.reload();
      Collection<String> var3 = Lists.newArrayList(param2);
      Collection<String> var4 = param1.getDataConfiguration().dataPacks().getDisabled();

      for (String var6 : param0.getAvailableIds()) {
         if (!var4.contains(var6) && !var3.contains(var6)) {
            var3.add(var6);
         }
      }

      return var3;
   }

   public static void register(CommandDispatcher<CommandSourceStack> param0) {
      param0.register(Commands.literal("reload").requires(l_param0 -> l_param0.hasPermission(2)).executes(l_param0 -> {
         CommandSourceStack l_var1 = l_param0.getSource();
         MinecraftServer l_var2 = l_var1.getServer();
         PackRepository l_var3 = l_var2.getPackRepository();
         WorldData l_var4 = l_var2.getWorldData();
         Collection<String> l_var5 = l_var3.getSelectedIds();
         Collection<String> l_var6 = discoverNewPacks(l_var3, l_var4, l_var5);
         l_var1.sendSuccess(Component.translatable("commands.reload.success"), true);
         reloadPacks(l_var6, l_var1);
         return 0;
      }));
   }
}
