package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;

public class HorseInventoryScreen extends AbstractContainerScreen<HorseInventoryMenu> {
   private static final ResourceLocation HORSE_INVENTORY_LOCATION = new ResourceLocation("textures/gui/container/horse.png");
   private final AbstractHorse horse;
   private float xMouse;
   private float yMouse;

   public HorseInventoryScreen(HorseInventoryMenu param0, Inventory param1, AbstractHorse param2) {
      super(param0, param1, param2.getDisplayName());
      this.horse = param2;
      this.passEvents = false;
   }

   @Override
   protected void renderBg(PoseStack param0, float param1, int param2, int param3) {
      RenderSystem.setShaderTexture(0, HORSE_INVENTORY_LOCATION);
      int var5 = (this.width - this.imageWidth) / 2;
      int var6 = (this.height - this.imageHeight) / 2;
      blit(param0, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      if (this.horse instanceof AbstractChestedHorse var7 && var7.hasChest()) {
         blit(param0, var5 + 79, var6 + 17, 0, this.imageHeight, var7.getInventoryColumns() * 18, 54);
      }

      if (this.horse.isSaddleable()) {
         blit(param0, var5 + 7, var6 + 35 - 18, 18, this.imageHeight + 54, 18, 18);
      }

      if (this.horse.canWearArmor()) {
         if (this.horse instanceof Llama) {
            blit(param0, var5 + 7, var6 + 35, 36, this.imageHeight + 54, 18, 18);
         } else {
            blit(param0, var5 + 7, var6 + 35, 0, this.imageHeight + 54, 18, 18);
         }
      }

      InventoryScreen.renderEntityInInventoryFollowsMouse(param0, var5 + 51, var6 + 60, 17, var5 + 51 - this.xMouse, var6 + 75 - 50 - this.yMouse, this.horse);
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.renderBackground(param0);
      this.xMouse = param1;
      this.yMouse = param2;
      super.render(param0, param1, param2, param3);
      this.renderTooltip(param0, param1, param2);
   }
}
