package net.minecraft.client.gui.spectator.categories;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.spectator.PlayerMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

public class TeleportToPlayerMenuCategory implements SpectatorMenuCategory, SpectatorMenuItem {
   private static final Comparator<PlayerInfo> PROFILE_ORDER = Comparator.comparing(param0 -> param0.getProfile().getId());
   private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.teleport");
   private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.teleport.prompt");
   private final List<SpectatorMenuItem> items;

   public TeleportToPlayerMenuCategory() {
      this(Minecraft.getInstance().getConnection().getListedOnlinePlayers());
   }

   public TeleportToPlayerMenuCategory(Collection<PlayerInfo> param0) {
      this.items = (List<SpectatorMenuItem>)(List<?>)param0.stream()
         .filter(l_param0 -> l_param0.getGameMode() != GameType.SPECTATOR)
         .sorted(PROFILE_ORDER)
         .map(l_param0 -> new PlayerMenuItem(l_param0.getProfile()))
         .toList();
   }

   @Override
   public List<SpectatorMenuItem> getItems() {
      return this.items;
   }

   @Override
   public Component getPrompt() {
      return TELEPORT_PROMPT;
   }

   @Override
   public void selectItem(SpectatorMenu param0) {
      param0.selectCategory(this);
   }

   @Override
   public Component getName() {
      return TELEPORT_TEXT;
   }

   @Override
   public void renderIcon(PoseStack param0, float param1, int param2) {
      RenderSystem.setShaderTexture(0, SpectatorGui.SPECTATOR_LOCATION);
      GuiComponent.blit(param0, 0, 0, 0.0F, 0.0F, 16, 16, 256, 256);
   }

   @Override
   public boolean isEnabled() {
      return !this.items.isEmpty();
   }
}
