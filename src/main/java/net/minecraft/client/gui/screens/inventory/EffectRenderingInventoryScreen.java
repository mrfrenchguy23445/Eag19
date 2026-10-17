package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class EffectRenderingInventoryScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   public EffectRenderingInventoryScreen(T param0, Inventory param1, Component param2) {
      super(param0, param1, param2);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      super.render(param0, param1, param2, param3);
      this.renderEffects(param0, param1, param2);
   }

   public boolean canSeeEffects() {
      int var1 = this.leftPos + this.imageWidth + 2;
      int var2 = this.width - var1;
      return var2 >= 32;
   }

   private void renderEffects(PoseStack param0, int param1, int param2) {
      int var4 = this.leftPos + this.imageWidth + 2;
      int var5 = this.width - var4;
      Collection<MobEffectInstance> var6 = this.minecraft.player.getActiveEffects();
      if (!var6.isEmpty() && var5 >= 32) {
         boolean var7 = var5 >= 120;
         int var8 = 33;
         if (var6.size() > 5) {
            var8 = 132 / (var6.size() - 1);
         }

         Iterable<MobEffectInstance> var9 = Ordering.natural().sortedCopy(var6);
         this.renderBackgrounds(param0, var4, var8, var9, var7);
         this.renderIcons(param0, var4, var8, var9, var7);
         if (var7) {
            this.renderLabels(param0, var4, var8, var9);
         } else if (param1 >= var4 && param1 <= var4 + 33) {
            int var10 = this.topPos;
            MobEffectInstance var11 = null;

            for (MobEffectInstance var13 : var9) {
               if (param2 >= var10 && param2 <= var10 + var8) {
                  var11 = var13;
               }

               var10 += var8;
            }

            if (var11 != null) {
               List<Component> var12 = List.of(this.getEffectName(var11), MobEffectUtil.formatDuration(var11, 1.0F));
               this.renderTooltip(param0, var12, Optional.empty(), param1, param2);
            }
         }
      }
   }

   private void renderBackgrounds(PoseStack param0, int param1, int param2, Iterable<MobEffectInstance> param3, boolean param4) {
      RenderSystem.setShaderTexture(0, INVENTORY_LOCATION);
      int var6 = this.topPos;

      for (MobEffectInstance var8 : param3) {
         if (param4) {
            blit(param0, param1, var6, 0, 166, 120, 32);
         } else {
            blit(param0, param1, var6, 0, 198, 32, 32);
         }

         var6 += param2;
      }
   }

   private void renderIcons(PoseStack param0, int param1, int param2, Iterable<MobEffectInstance> param3, boolean param4) {
      MobEffectTextureManager var6 = this.minecraft.getMobEffectTextures();
      int var7 = this.topPos;

      for (MobEffectInstance var9 : param3) {
         MobEffect var10 = var9.getEffect();
         TextureAtlasSprite var11 = var6.get(var10);
         RenderSystem.setShaderTexture(0, var11.atlasLocation());
         blit(param0, param1 + (param4 ? 6 : 7), var7 + 7, 0, 18, 18, var11);
         var7 += param2;
      }
   }

   private void renderLabels(PoseStack param0, int param1, int param2, Iterable<MobEffectInstance> param3) {
      int var5 = this.topPos;

      for (MobEffectInstance var7 : param3) {
         Component var8 = this.getEffectName(var7);
         this.font.drawShadow(param0, var8, param1 + 10 + 18, var5 + 6, 16777215);
         Component var9 = MobEffectUtil.formatDuration(var7, 1.0F);
         this.font.drawShadow(param0, var9, param1 + 10 + 18, var5 + 6 + 10, 8355711);
         var5 += param2;
      }
   }

   private Component getEffectName(MobEffectInstance param0) {
      MutableComponent var2 = param0.getEffect().getDisplayName().copy();
      if (param0.getAmplifier() >= 1 && param0.getAmplifier() <= 9) {
         var2.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (param0.getAmplifier() + 1)));
      }

      return var2;
   }
}
