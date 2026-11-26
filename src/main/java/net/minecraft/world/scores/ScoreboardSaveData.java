package net.minecraft.world.scores;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardSaveData extends SavedData {
   public static final String FILE_ID = "scoreboard";
   private final Scoreboard scoreboard;

   public ScoreboardSaveData(Scoreboard param0) {
      this.scoreboard = param0;
   }

   public ScoreboardSaveData load(CompoundTag param0) {
      this.loadObjectives(param0.getList("Objectives", 10));
      this.scoreboard.loadPlayerScores(param0.getList("PlayerScores", 10));
      if (param0.contains("DisplaySlots", 10)) {
         this.loadDisplaySlots(param0.getCompound("DisplaySlots"));
      }

      if (param0.contains("Teams", 9)) {
         this.loadTeams(param0.getList("Teams", 10));
      }

      return this;
   }

   private void loadTeams(ListTag param0) {
      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         String var4 = var3.getString("Name");
         PlayerTeam var5 = this.scoreboard.addPlayerTeam(var4);
         Component var6 = Component.Serializer.fromJson(var3.getString("DisplayName"));
         if (var6 != null) {
            var5.setDisplayName(var6);
         }

         if (var3.contains("TeamColor", 8)) {
            var5.setColor(ChatFormatting.getByName(var3.getString("TeamColor")));
         }

         if (var3.contains("AllowFriendlyFire", 99)) {
            var5.setAllowFriendlyFire(var3.getBoolean("AllowFriendlyFire"));
         }

         if (var3.contains("SeeFriendlyInvisibles", 99)) {
            var5.setSeeFriendlyInvisibles(var3.getBoolean("SeeFriendlyInvisibles"));
         }

         if (var3.contains("MemberNamePrefix", 8)) {
            Component var7 = Component.Serializer.fromJson(var3.getString("MemberNamePrefix"));
            if (var7 != null) {
               var5.setPlayerPrefix(var7);
            }
         }

         if (var3.contains("MemberNameSuffix", 8)) {
            Component var7 = Component.Serializer.fromJson(var3.getString("MemberNameSuffix"));
            if (var7 != null) {
               var5.setPlayerSuffix(var7);
            }
         }

         if (var3.contains("NameTagVisibility", 8)) {
            Team.Visibility var7 = Team.Visibility.byName(var3.getString("NameTagVisibility"));
            if (var7 != null) {
               var5.setNameTagVisibility(var7);
            }
         }

         if (var3.contains("DeathMessageVisibility", 8)) {
            Team.Visibility var7 = Team.Visibility.byName(var3.getString("DeathMessageVisibility"));
            if (var7 != null) {
               var5.setDeathMessageVisibility(var7);
            }
         }

         if (var3.contains("CollisionRule", 8)) {
            Team.CollisionRule var7 = Team.CollisionRule.byName(var3.getString("CollisionRule"));
            if (var7 != null) {
               var5.setCollisionRule(var7);
            }
         }

         this.loadTeamPlayers(var5, var3.getList("Players", 8));
      }
   }

   private void loadTeamPlayers(PlayerTeam param0, ListTag param1) {
      for (int var3 = 0; var3 < param1.size(); var3++) {
         this.scoreboard.addPlayerToTeam(param1.getString(var3), param0);
      }
   }

   private void loadDisplaySlots(CompoundTag param0) {
      for (int var2 = 0; var2 < 19; var2++) {
         if (param0.contains("slot_" + var2, 8)) {
            String var3 = param0.getString("slot_" + var2);
            Objective var4 = this.scoreboard.getObjective(var3);
            this.scoreboard.setDisplayObjective(var2, var4);
         }
      }
   }

   private void loadObjectives(ListTag param0) {
      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         ObjectiveCriteria.byName(var3.getString("CriteriaName")).ifPresent(l_param1 -> {
            String l_var3 = var3.getString("Name");
            Component l_var4 = Component.Serializer.fromJson(var3.getString("DisplayName"));
            ObjectiveCriteria.RenderType l_var5 = ObjectiveCriteria.RenderType.byId(var3.getString("RenderType"));
            this.scoreboard.addObjective(l_var3, l_param1, l_var4, l_var5);
         });
      }
   }

   @Override
   public CompoundTag save(CompoundTag param0) {
      param0.put("Objectives", this.saveObjectives());
      param0.put("PlayerScores", this.scoreboard.savePlayerScores());
      param0.put("Teams", this.saveTeams());
      this.saveDisplaySlots(param0);
      return param0;
   }

   private ListTag saveTeams() {
      ListTag var1 = new ListTag();

      for (PlayerTeam var4 : this.scoreboard.getPlayerTeams()) {
         CompoundTag var5 = new CompoundTag();
         var5.putString("Name", var4.getName());
         var5.putString("DisplayName", Component.Serializer.toJson(var4.getDisplayName()));
         if (var4.getColor().getId() >= 0) {
            var5.putString("TeamColor", var4.getColor().getName());
         }

         var5.putBoolean("AllowFriendlyFire", var4.isAllowFriendlyFire());
         var5.putBoolean("SeeFriendlyInvisibles", var4.canSeeFriendlyInvisibles());
         var5.putString("MemberNamePrefix", Component.Serializer.toJson(var4.getPlayerPrefix()));
         var5.putString("MemberNameSuffix", Component.Serializer.toJson(var4.getPlayerSuffix()));
         var5.putString("NameTagVisibility", var4.getNameTagVisibility().name);
         var5.putString("DeathMessageVisibility", var4.getDeathMessageVisibility().name);
         var5.putString("CollisionRule", var4.getCollisionRule().name);
         ListTag var6 = new ListTag();

         for (String var8 : var4.getPlayers()) {
            var6.add(StringTag.valueOf(var8));
         }

         var5.put("Players", var6);
         var1.add(var5);
      }

      return var1;
   }

   private void saveDisplaySlots(CompoundTag param0) {
      CompoundTag var2 = new CompoundTag();
      boolean var3 = false;

      for (int var4 = 0; var4 < 19; var4++) {
         Objective var5 = this.scoreboard.getDisplayObjective(var4);
         if (var5 != null) {
            var2.putString("slot_" + var4, var5.getName());
            var3 = true;
         }
      }

      if (var3) {
         param0.put("DisplaySlots", var2);
      }
   }

   private ListTag saveObjectives() {
      ListTag var1 = new ListTag();

      for (Objective var4 : this.scoreboard.getObjectives()) {
         if (var4.getCriteria() != null) {
            CompoundTag var5 = new CompoundTag();
            var5.putString("Name", var4.getName());
            var5.putString("CriteriaName", var4.getCriteria().getName());
            var5.putString("DisplayName", Component.Serializer.toJson(var4.getDisplayName()));
            var5.putString("RenderType", var4.getRenderType().getId());
            var1.add(var5);
         }
      }

      return var1;
   }
}
