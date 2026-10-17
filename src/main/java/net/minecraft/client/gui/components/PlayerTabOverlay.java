package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class PlayerTabOverlay extends GuiComponent {
   private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(
         param0 -> param0.getGameMode() == GameType.SPECTATOR ? 1 : 0
      )
      .thenComparing(param0 -> Optionull.mapOrDefault(param0.getTeam(), PlayerTeam::getName, ""))
      .thenComparing(param0 -> param0.getProfile().getName(), String::compareToIgnoreCase);
   public static final int MAX_ROWS_PER_COL = 20;
   public static final int HEART_EMPTY_CONTAINER = 16;
   public static final int HEART_EMPTY_CONTAINER_BLINKING = 25;
   public static final int HEART_FULL = 52;
   public static final int HEART_HALF_FULL = 61;
   public static final int HEART_GOLDEN_FULL = 160;
   public static final int HEART_GOLDEN_HALF_FULL = 169;
   public static final int HEART_GHOST_FULL = 70;
   public static final int HEART_GHOST_HALF_FULL = 79;
   private final Minecraft minecraft;
   private final Gui gui;
   @Nullable
   private Component footer;
   @Nullable
   private Component header;
   private boolean visible;
   private final Map<UUID, PlayerTabOverlay.HealthState> healthStates = new Object2ObjectOpenHashMap<>();

   public PlayerTabOverlay(Minecraft param0, Gui param1) {
      this.minecraft = param0;
      this.gui = param1;
   }

   public Component getNameForDisplay(PlayerInfo param0) {
      return param0.getTabListDisplayName() != null
         ? this.decorateName(param0, param0.getTabListDisplayName().copy())
         : this.decorateName(param0, PlayerTeam.formatNameForTeam(param0.getTeam(), Component.literal(param0.getProfile().getName())));
   }

   private Component decorateName(PlayerInfo param0, MutableComponent param1) {
      return param0.getGameMode() == GameType.SPECTATOR ? param1.withStyle(ChatFormatting.ITALIC) : param1;
   }

   public void setVisible(boolean param0) {
      if (this.visible != param0) {
         this.healthStates.clear();
         this.visible = param0;
         if (param0) {
            Component var2 = ComponentUtils.formatList(this.getPlayerInfos(), Component.literal(", "), this::getNameForDisplay);
            this.minecraft.getNarrator().sayNow(Component.translatable("multiplayer.player.list.narration", var2));
         }
      }
   }

   private List<PlayerInfo> getPlayerInfos() {
      return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
   }

   public void render(PoseStack param0, int param1, Scoreboard param2, @Nullable Objective param3) {
      List<PlayerInfo> var5 = this.getPlayerInfos();
      int var6 = 0;
      int var7 = 0;

      for (PlayerInfo var9 : var5) {
         int var10 = this.minecraft.font.width(this.getNameForDisplay(var9));
         var6 = Math.max(var6, var10);
         if (param3 != null && param3.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
            var10 = this.minecraft.font.width(" " + param2.getOrCreatePlayerScore(var9.getProfile().getName(), param3).getScore());
            var7 = Math.max(var7, var10);
         }
      }

      if (!this.healthStates.isEmpty()) {
         Set<UUID> var8 = var5.stream().map(l_param0 -> l_param0.getProfile().getId()).collect(Collectors.toSet());
         this.healthStates.keySet().removeIf(l_param1 -> !var8.contains(l_param1));
      }

      int var8 = var5.size();
      int var9 = var8;

      int var10;
      for (var10 = 1; var9 > 20; var9 = (var8 + var10 - 1) / var10) {
         var10++;
      }

      boolean var11 = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
      int var12;
      if (param3 != null) {
         if (param3.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            var12 = 90;
         } else {
            var12 = var7;
         }
      } else {
         var12 = 0;
      }

      int var13 = Math.min(var10 * ((var11 ? 9 : 0) + var6 + var12 + 13), param1 - 50) / var10;
      int var14 = param1 / 2 - (var13 * var10 + (var10 - 1) * 5) / 2;
      int var15 = 10;
      int var16 = var13 * var10 + (var10 - 1) * 5;
      List<FormattedCharSequence> var17 = null;
      if (this.header != null) {
         var17 = this.minecraft.font.split(this.header, param1 - 50);

         for (FormattedCharSequence var19 : var17) {
            var16 = Math.max(var16, this.minecraft.font.width(var19));
         }
      }

      List<FormattedCharSequence> var18 = null;
      if (this.footer != null) {
         var18 = this.minecraft.font.split(this.footer, param1 - 50);

         for (FormattedCharSequence var20 : var18) {
            var16 = Math.max(var16, this.minecraft.font.width(var20));
         }
      }

      if (var17 != null) {
         fill(param0, param1 / 2 - var16 / 2 - 1, var15 - 1, param1 / 2 + var16 / 2 + 1, var15 + var17.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence var20 : var17) {
            int var21 = this.minecraft.font.width(var20);
            this.minecraft.font.drawShadow(param0, var20, param1 / 2 - var21 / 2, var15, -1);
            var15 += 9;
         }

         var15++;
      }

      fill(param0, param1 / 2 - var16 / 2 - 1, var15 - 1, param1 / 2 + var16 / 2 + 1, var15 + var9 * 9, Integer.MIN_VALUE);
      int var19 = this.minecraft.options.getBackgroundColor(553648127);

      for (int var20 = 0; var20 < var8; var20++) {
         int var21 = var20 / var9;
         int var22 = var20 % var9;
         int var23 = var14 + var21 * var13 + var21 * 5;
         int var24 = var15 + var22 * 9;
         fill(param0, var23, var24, var23 + var13, var24 + 8, var19);
         RenderSystem.enableBlend();
         if (var20 < var5.size()) {
            PlayerInfo var25 = var5.get(var20);
            GameProfile var26 = var25.getProfile();
            if (var11) {
               Player var27 = this.minecraft.level.getPlayerByUUID(var26.getId());
               boolean var28 = var27 != null && LivingEntityRenderer.isEntityUpsideDown(var27);
               boolean var29 = var27 != null && var27.isModelPartShown(PlayerModelPart.HAT);
               RenderSystem.setShaderTexture(0, var25.getSkinLocation());
               PlayerFaceRenderer.draw(param0, var23, var24, 8, var29, var28);
               var23 += 9;
            }

            this.minecraft.font.drawShadow(param0, this.getNameForDisplay(var25), var23, var24, var25.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1);
            if (param3 != null && var25.getGameMode() != GameType.SPECTATOR) {
               int var27 = var23 + var6 + 1;
               int var28 = var27 + var12;
               if (var28 - var27 > 5) {
                  this.renderTablistScore(param3, var24, var26.getName(), var27, var28, var26.getId(), param0);
               }
            }

            this.renderPingIcon(param0, var13, var23 - (var11 ? 9 : 0), var24, var25);
         }
      }

      if (var18 != null) {
         var15 += var9 * 9 + 1;
         fill(param0, param1 / 2 - var16 / 2 - 1, var15 - 1, param1 / 2 + var16 / 2 + 1, var15 + var18.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence var21 : var18) {
            int var22 = this.minecraft.font.width(var21);
            this.minecraft.font.drawShadow(param0, var21, param1 / 2 - var22 / 2, var15, -1);
            var15 += 9;
         }
      }
   }

   protected void renderPingIcon(PoseStack param0, int param1, int param2, int param3, PlayerInfo param4) {
      RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
      int var6 = 0;
      int var7;
      if (param4.getLatency() < 0) {
         var7 = 5;
      } else if (param4.getLatency() < 150) {
         var7 = 0;
      } else if (param4.getLatency() < 300) {
         var7 = 1;
      } else if (param4.getLatency() < 600) {
         var7 = 2;
      } else if (param4.getLatency() < 1000) {
         var7 = 3;
      } else {
         var7 = 4;
      }

      param0.pushPose();
      param0.translate(0.0F, 0.0F, 100.0F);
      blit(param0, param2 + param1 - 11, param3, 0, 176 + var7 * 8, 10, 8);
      param0.popPose();
   }

   private void renderTablistScore(Objective param0, int param1, String param2, int param3, int param4, UUID param5, PoseStack param6) {
      int var8 = param0.getScoreboard().getOrCreatePlayerScore(param2, param0).getScore();
      if (param0.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
         this.renderTablistHearts(param1, param3, param4, param5, param6, var8);
      } else {
         String var9 = "" + ChatFormatting.YELLOW + var8;
         this.minecraft.font.drawShadow(param6, var9, param4 - this.minecraft.font.width(var9), param1, 16777215);
      }
   }

   private void renderTablistHearts(int param0, int param1, int param2, UUID param3, PoseStack param4, int param5) {
      PlayerTabOverlay.HealthState var7 = this.healthStates.computeIfAbsent(param3, l_param1 -> new PlayerTabOverlay.HealthState(param5));
      var7.update(param5, this.gui.getGuiTicks());
      int var8 = Mth.positiveCeilDiv(Math.max(param5, var7.displayedValue()), 2);
      int var9 = Math.max(param5, Math.max(var7.displayedValue(), 20)) / 2;
      boolean var10 = var7.isBlinking(this.gui.getGuiTicks());
      if (var8 > 0) {
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         int var11 = Mth.floor(Math.min((float)(param2 - param1 - 4) / var9, 9.0F));
         if (var11 <= 3) {
            float var12 = Mth.clamp(param5 / 20.0F, 0.0F, 1.0F);
            int var13 = (int)((1.0F - var12) * 255.0F) << 16 | (int)(var12 * 255.0F) << 8;
            String var14 = param5 / 2.0F + "";
            if (param2 - this.minecraft.font.width(var14 + "hp") >= param1) {
               var14 = var14 + "hp";
            }

            this.minecraft.font.drawShadow(param4, var14, (param2 + param1 - this.minecraft.font.width(var14)) / 2, param0, var13);
         } else {
            for (int var12 = var8; var12 < var9; var12++) {
               blit(param4, param1 + var12 * var11, param0, var10 ? 25 : 16, 0, 9, 9);
            }

            for (int var12 = 0; var12 < var8; var12++) {
               blit(param4, param1 + var12 * var11, param0, var10 ? 25 : 16, 0, 9, 9);
               if (var10) {
                  if (var12 * 2 + 1 < var7.displayedValue()) {
                     blit(param4, param1 + var12 * var11, param0, 70, 0, 9, 9);
                  }

                  if (var12 * 2 + 1 == var7.displayedValue()) {
                     blit(param4, param1 + var12 * var11, param0, 79, 0, 9, 9);
                  }
               }

               if (var12 * 2 + 1 < param5) {
                  blit(param4, param1 + var12 * var11, param0, var12 >= 10 ? 160 : 52, 0, 9, 9);
               }

               if (var12 * 2 + 1 == param5) {
                  blit(param4, param1 + var12 * var11, param0, var12 >= 10 ? 169 : 61, 0, 9, 9);
               }
            }
         }
      }
   }

   public void setFooter(@Nullable Component param0) {
      this.footer = param0;
   }

   public void setHeader(@Nullable Component param0) {
      this.header = param0;
   }

   public void reset() {
      this.header = null;
      this.footer = null;
   }

   static class HealthState {
      private static final long DISPLAY_UPDATE_DELAY = 20L;
      private static final long DECREASE_BLINK_DURATION = 20L;
      private static final long INCREASE_BLINK_DURATION = 10L;
      private int lastValue;
      private int displayedValue;
      private long lastUpdateTick;
      private long blinkUntilTick;

      public HealthState(int param0) {
         this.displayedValue = param0;
         this.lastValue = param0;
      }

      public void update(int param0, long param1) {
         if (param0 != this.lastValue) {
            long var4 = param0 < this.lastValue ? 20L : 10L;
            this.blinkUntilTick = param1 + var4;
            this.lastValue = param0;
            this.lastUpdateTick = param1;
         }

         if (param1 - this.lastUpdateTick > 20L) {
            this.displayedValue = param0;
         }
      }

      public int displayedValue() {
         return this.displayedValue;
      }

      public boolean isBlinking(long param0) {
         return this.blinkUntilTick > param0 && (this.blinkUntilTick - param0) % 6L >= 3L;
      }
   }
}
