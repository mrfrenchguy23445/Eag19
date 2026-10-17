package net.minecraft.client.gui.screens.recipebook;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeBookTabButton extends StateSwitchingButton {
   private final RecipeBookCategories category;
   private static final float ANIMATION_TIME = 15.0F;
   private float animationTime;

   public RecipeBookTabButton(RecipeBookCategories param0) {
      super(0, 0, 35, 27, false);
      this.category = param0;
      this.initTextureValues(153, 2, 35, 0, RecipeBookComponent.RECIPE_BOOK_LOCATION);
   }

   public void startAnimation(Minecraft param0) {
      ClientRecipeBook var2 = param0.player.getRecipeBook();
      List<RecipeCollection> var3 = var2.getCollection(this.category);
      if (param0.player.containerMenu instanceof RecipeBookMenu) {
         for (RecipeCollection var5 : var3) {
            for (Recipe<?> var7 : var5.getRecipes(var2.isFiltering((RecipeBookMenu<?>)param0.player.containerMenu))) {
               if (var2.willHighlight(var7)) {
                  this.animationTime = 15.0F;
                  return;
               }
            }
         }
      }
   }

   @Override
   public void renderWidget(PoseStack param0, int param1, int param2, float param3) {
      if (this.animationTime > 0.0F) {
         float var5 = 1.0F + 0.1F * (float)Math.sin(this.animationTime / 15.0F * (float) Math.PI);
         param0.pushPose();
         param0.translate(this.getX() + 8, this.getY() + 12, 0.0F);
         param0.scale(1.0F, var5, 1.0F);
         param0.translate(-(this.getX() + 8), -(this.getY() + 12), 0.0F);
      }

      Minecraft var5 = Minecraft.getInstance();
      RenderSystem.setShaderTexture(0, this.resourceLocation);
      RenderSystem.disableDepthTest();
      int var6 = this.xTexStart;
      int var7 = this.yTexStart;
      if (this.isStateTriggered) {
         var6 += this.xDiffTex;
      }

      if (this.isHoveredOrFocused()) {
         var7 += this.yDiffTex;
      }

      int var8 = this.getX();
      if (this.isStateTriggered) {
         var8 -= 2;
      }

      blit(param0, var8, this.getY(), var6, var7, this.width, this.height);
      RenderSystem.enableDepthTest();
      this.renderIcon(param0, var5.getItemRenderer());
      if (this.animationTime > 0.0F) {
         param0.popPose();
         this.animationTime -= param3;
      }
   }

   private void renderIcon(PoseStack param0, ItemRenderer param1) {
      List<ItemStack> var3 = this.category.getIconItems();
      int var4 = this.isStateTriggered ? -2 : 0;
      if (var3.size() == 1) {
         param1.renderAndDecorateFakeItem(param0, var3.get(0), this.getX() + 9 + var4, this.getY() + 5);
      } else if (var3.size() == 2) {
         param1.renderAndDecorateFakeItem(param0, var3.get(0), this.getX() + 3 + var4, this.getY() + 5);
         param1.renderAndDecorateFakeItem(param0, var3.get(1), this.getX() + 14 + var4, this.getY() + 5);
      }
   }

   public RecipeBookCategories getCategory() {
      return this.category;
   }

   public boolean updateVisibility(ClientRecipeBook param0) {
      List<RecipeCollection> var2 = param0.getCollection(this.category);
      this.visible = false;
      if (var2 != null) {
         for (RecipeCollection var4 : var2) {
            if (var4.hasKnownRecipes() && var4.hasFitting()) {
               this.visible = true;
               break;
            }
         }
      }

      return this.visible;
   }
}
