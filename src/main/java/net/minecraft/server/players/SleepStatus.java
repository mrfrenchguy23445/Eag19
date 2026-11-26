package net.minecraft.server.players;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class SleepStatus {
   private int activePlayers;
   private int sleepingPlayers;

   public boolean areEnoughSleeping(int param0) {
      return this.sleepingPlayers >= this.sleepersNeeded(param0);
   }

   public boolean areEnoughDeepSleeping(int param0, List<ServerPlayer> param1) {
      int var3 = (int)param1.stream().filter(Player::isSleepingLongEnough).count();
      return var3 >= this.sleepersNeeded(param0);
   }

   public int sleepersNeeded(int param0) {
      return Math.max(1, Mth.ceil(this.activePlayers * param0 / 100.0F));
   }

   public void removeAllSleepers() {
      this.sleepingPlayers = 0;
   }

   public int amountSleeping() {
      return this.sleepingPlayers;
   }

   public boolean update(List<ServerPlayer> param0) {
      int var2 = this.activePlayers;
      int var3 = this.sleepingPlayers;
      this.activePlayers = 0;
      this.sleepingPlayers = 0;

      for (ServerPlayer var5 : param0) {
         if (!var5.isSpectator()) {
            this.activePlayers++;
            if (var5.isSleeping()) {
               this.sleepingPlayers++;
            }
         }
      }

      return (var3 > 0 || this.sleepingPlayers > 0) && (var2 != this.activePlayers || var3 != this.sleepingPlayers);
   }
}
