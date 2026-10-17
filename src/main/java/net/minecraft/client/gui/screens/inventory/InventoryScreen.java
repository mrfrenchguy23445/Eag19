package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class InventoryScreen extends EffectRenderingInventoryScreen<InventoryMenu> implements RecipeUpdateListener {
   private static final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("textures/gui/recipe_button.png");
   private float xMouse;
   private float yMouse;
   private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
   private boolean recipeBookComponentInitialized;
   private boolean widthTooNarrow;
   private boolean buttonClicked;

   public InventoryScreen(Player param0) {
      super(param0.inventoryMenu, param0.getInventory(), Component.translatable("container.crafting"));
      this.passEvents = true;
      this.titleLabelX = 97;
   }

   @Override
   public void containerTick() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      } else {
         this.recipeBookComponent.tick();
      }
   }

   @Override
   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         this.minecraft
            .setScreen(
               new CreativeModeInventoryScreen(
                  this.minecraft.player, this.minecraft.player.connection.enabledFeatures(), this.minecraft.options.operatorItemsTab().get()
               )
            );
      } else {
         super.init();
         this.widthTooNarrow = this.width < 379;
         this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
         this.recipeBookComponentInitialized = true;
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         this.addRenderableWidget(new ImageButton(this.leftPos + 104, this.height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, l_param0 -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            l_param0.setPosition(this.leftPos + 104, this.height / 2 - 22);
            this.buttonClicked = true;
         }));
         this.addWidget(this.recipeBookComponent);
         this.setInitialFocus(this.recipeBookComponent);
      }
   }

   @Override
   protected void renderLabels(PoseStack param0, int param1, int param2) {
      this.font.draw(param0, this.title, this.titleLabelX, this.titleLabelY, 4210752);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBg(param0, param3, param1, param2);
         this.recipeBookComponent.render(param0, param1, param2, param3);
      } else {
         this.recipeBookComponent.render(param0, param1, param2, param3);
         super.render(param0, param1, param2, param3);
         this.recipeBookComponent.renderGhostRecipe(param0, this.leftPos, this.topPos, false, param3);
      }

      this.renderTooltip(param0, param1, param2);
      this.recipeBookComponent.renderTooltip(param0, this.leftPos, this.topPos, param1, param2);
      this.xMouse = param1;
      this.yMouse = param2;
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      RenderSystem.setShaderTexture(0, INVENTORY_LOCATION);
      int var5 = this.leftPos;
      int var6 = this.topPos;
      blit(param0, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      renderEntityInInventoryFollowsMouse(param0, var5 + 51, var6 + 75, 30, var5 + 51 - this.xMouse, var6 + 75 - 50 - this.yMouse, this.minecraft.player);
   }

   public static void renderEntityInInventoryFollowsMouse(PoseStack param0, int param1, int param2, int param3, float param4, float param5, LivingEntity param6) {
      float var7 = (float)Math.atan(param4 / 40.0F);
      float var8 = (float)Math.atan(param5 / 40.0F);
      Quaternionf var9 = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf var10 = new Quaternionf().rotateX(var8 * 20.0F * (float) (Math.PI / 180.0));
      var9.mul(var10);
      float var11 = param6.yBodyRot;
      float var12 = param6.getYRot();
      float var13 = param6.getXRot();
      float var14 = param6.yHeadRotO;
      float var15 = param6.yHeadRot;
      param6.yBodyRot = 180.0F + var7 * 20.0F;
      param6.setYRot(180.0F + var7 * 40.0F);
      param6.setXRot(-var8 * 20.0F);
      param6.yHeadRot = param6.getYRot();
      param6.yHeadRotO = param6.getYRot();
      renderEntityInInventory(param0, param1, param2, param3, var9, var10, param6);
      param6.yBodyRot = var11;
      param6.setYRot(var12);
      param6.setXRot(var13);
      param6.yHeadRotO = var14;
      param6.yHeadRot = var15;
   }

   public static void renderEntityInInventory(
      PoseStack param0, int param1, int param2, int param3, Quaternionf param4, @Nullable Quaternionf param5, LivingEntity param6
   ) {
      double var7 = 1000.0;
      PoseStack var9 = RenderSystem.getModelViewStack();
      var9.pushPose();
      var9.translate(0.0, 0.0, 1000.0);
      RenderSystem.applyModelViewMatrix();
      param0.pushPose();
      param0.translate(param1, param2, -950.0);
      param0.mulPoseMatrix(new Matrix4f().scaling(param3, param3, -param3));
      param0.mulPose(param4);
      Lighting.setupForEntityInInventory();
      EntityRenderDispatcher var10 = Minecraft.getInstance().getEntityRenderDispatcher();
      if (param5 != null) {
         param5.conjugate();
         var10.overrideCameraOrientation(param5);
      }

      var10.setRenderShadow(false);
      MultiBufferSource.BufferSource var11 = Minecraft.getInstance().renderBuffers().bufferSource();
      RenderSystem.runAsFancy(() -> var10.render(param6, 0.0, 0.0, 0.0, 0.0F, 1.0F, param0, var11, 15728880));
      var11.endBatch();
      var10.setRenderShadow(true);
      param0.popPose();
      Lighting.setupFor3DItems();
      var9.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   @Override
   protected boolean isHovering(int param0, int param1, int param2, int param3, double param4, double param6) {
      return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(param0, param1, param2, param3, param4, param6);
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.recipeBookComponent.mouseClicked(param0, param2, param4)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? false : super.mouseClicked(param0, param2, param4);
      }
   }

   @Override
   public boolean mouseReleased(double param0, double param2, int param4) {
      if (this.buttonClicked) {
         this.buttonClicked = false;
         return true;
      } else {
         return super.mouseReleased(param0, param2, param4);
      }
   }

   @Override
   protected boolean hasClickedOutside(double param0, double param2, int param4, int param5, int param6) {
      boolean var8 = param0 < param4 || param2 < param5 || param0 >= param4 + this.imageWidth || param2 >= param5 + this.imageHeight;
      return this.recipeBookComponent.hasClickedOutside(param0, param2, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, param6) && var8;
   }

   @Override
   protected void slotClicked(Slot param0, int param1, int param2, ClickType param3) {
      super.slotClicked(param0, param1, param2, param3);
      this.recipeBookComponent.slotClicked(param0);
   }

   @Override
   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   @Override
   public RecipeBookComponent getRecipeBookComponent() {
      return this.recipeBookComponent;
   }
}
