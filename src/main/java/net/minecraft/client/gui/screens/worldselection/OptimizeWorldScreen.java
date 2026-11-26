package net.minecraft.client.gui.screens.worldselection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Object2IntMap<ResourceKey<Level>> DIMENSION_COLORS = Util.make(new Object2IntOpenCustomHashMap<>(Util.identityStrategy()), param0 -> {
      param0.put(Level.OVERWORLD, -13408734);
      param0.put(Level.NETHER, -10075085);
      param0.put(Level.END, -8943531);
      param0.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   @Nullable
   public static OptimizeWorldScreen create(
      Minecraft param0, BooleanConsumer param1, DataFixer param2, LevelStorageSource.LevelStorageAccess param3, boolean param4
   ) {
      try (WorldStem var5 = param0.createWorldOpenFlows().loadWorldStem(param3, false)) {
         WorldData var6 = var5.worldData();
         RegistryAccess.Frozen var7 = var5.registries().compositeAccess();
         param3.saveDataTag(var7, var6);
         return new OptimizeWorldScreen(param1, param2, param3, var6.getLevelSettings(), param4, var7.registryOrThrow(Registries.LEVEL_STEM));
      } catch (Exception var5) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", var5);
         return null;
      }
   }

   private OptimizeWorldScreen(
      BooleanConsumer param0, DataFixer param1, LevelStorageSource.LevelStorageAccess param2, LevelSettings param3, boolean param4, Registry<LevelStem> param5
   ) {
      super(Component.translatable("optimizeWorld.title", param3.levelName()));
      this.callback = param0;
      this.upgrader = new WorldUpgrader(param2, param1, param5, param4);
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, l_param0 -> {
         this.upgrader.cancel();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
   }

   @Override
   public void tick() {
      if (this.upgrader.isFinished()) {
         this.callback.accept(true);
      }
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   @Override
   public void removed() {
      this.upgrader.cancel();
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      drawCenteredString(param0, this.font, this.title, this.width / 2, 20, 16777215);
      int var5 = this.width / 2 - 150;
      int var6 = this.width / 2 + 150;
      int var7 = this.height / 4 + 100;
      int var8 = var7 + 10;
      drawCenteredString(param0, this.font, this.upgrader.getStatus(), this.width / 2, var7 - 9 - 2, 10526880);
      if (this.upgrader.getTotalChunks() > 0) {
         fill(param0, var5 - 1, var7 - 1, var6 + 1, var8 + 1, -16777216);
         drawString(param0, this.font, Component.translatable("optimizeWorld.info.converted", this.upgrader.getConverted()), var5, 40, 10526880);
         drawString(param0, this.font, Component.translatable("optimizeWorld.info.skipped", this.upgrader.getSkipped()), var5, 40 + 9 + 3, 10526880);
         drawString(param0, this.font, Component.translatable("optimizeWorld.info.total", this.upgrader.getTotalChunks()), var5, 40 + (9 + 3) * 2, 10526880);
         int var9 = 0;

         for (ResourceKey<Level> var11 : this.upgrader.levels()) {
            int var12 = Mth.floor(this.upgrader.dimensionProgress(var11) * (var6 - var5));
            fill(param0, var5 + var9, var7, var5 + var9 + var12, var8, DIMENSION_COLORS.getInt(var11));
            var9 += var12;
         }

         int var10 = this.upgrader.getConverted() + this.upgrader.getSkipped();
         drawCenteredString(param0, this.font, var10 + " / " + this.upgrader.getTotalChunks(), this.width / 2, var7 + 2 * 9 + 2, 10526880);
         drawCenteredString(
            param0, this.font, Mth.floor(this.upgrader.getProgress() * 100.0F) + "%", this.width / 2, var7 + (var8 - var7) / 2 - 9 / 2, 10526880
         );
      }

      super.render(param0, param1, param2, param3);
   }
}
