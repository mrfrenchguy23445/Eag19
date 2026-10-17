package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class Scoreboard {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DISPLAY_SLOT_LIST = 0;
   public static final int DISPLAY_SLOT_SIDEBAR = 1;
   public static final int DISPLAY_SLOT_BELOW_NAME = 2;
   public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_START = 3;
   public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_END = 18;
   public static final int DISPLAY_SLOTS = 19;
   private final Map<String, Objective> objectivesByName = Maps.newHashMap();
   private final Map<ObjectiveCriteria, List<Objective>> objectivesByCriteria = Maps.newHashMap();
   private final Map<String, Map<Objective, Score>> playerScores = Maps.newHashMap();
   private final Objective[] displayObjectives = new Objective[19];
   private final Map<String, PlayerTeam> teamsByName = Maps.newHashMap();
   private final Map<String, PlayerTeam> teamsByPlayer = Maps.newHashMap();
   @Nullable
   private static String[] displaySlotNames;

   public boolean hasObjective(String param0) {
      return this.objectivesByName.containsKey(param0);
   }

   public Objective getOrCreateObjective(String param0) {
      return this.objectivesByName.get(param0);
   }

   @Nullable
   public Objective getObjective(@Nullable String param0) {
      return this.objectivesByName.get(param0);
   }

   public Objective addObjective(String param0, ObjectiveCriteria param1, Component param2, ObjectiveCriteria.RenderType param3) {
      if (this.objectivesByName.containsKey(param0)) {
         throw new IllegalArgumentException("An objective with the name '" + param0 + "' already exists!");
      }

      Objective var5 = new Objective(this, param0, param1, param2, param3);
      this.objectivesByCriteria.computeIfAbsent(param1, l_param0 -> Lists.newArrayList()).add(var5);
      this.objectivesByName.put(param0, var5);
      this.onObjectiveAdded(var5);
      return var5;
   }

   public final void forAllObjectives(ObjectiveCriteria param0, String param1, Consumer<Score> param2) {
      this.objectivesByCriteria.getOrDefault(param0, Collections.emptyList()).forEach(l_param2 -> param2.accept(this.getOrCreatePlayerScore(param1, l_param2)));
   }

   public boolean hasPlayerScore(String param0, Objective param1) {
      Map<Objective, Score> var3 = this.playerScores.get(param0);
      if (var3 == null) {
         return false;
      }

      Score var4 = var3.get(param1);
      return var4 != null;
   }

   public Score getOrCreatePlayerScore(String param0, Objective param1) {
      Map<Objective, Score> var3 = this.playerScores.computeIfAbsent(param0, l_param0 -> Maps.newHashMap());
      return var3.computeIfAbsent(param1, l_param1 -> {
         Score l_var3 = new Score(this, l_param1, param0);
         l_var3.setScore(0);
         return l_var3;
      });
   }

   public Collection<Score> getPlayerScores(Objective param0) {
      List<Score> var2 = Lists.newArrayList();

      for (Map<Objective, Score> var4 : this.playerScores.values()) {
         Score var5 = var4.get(param0);
         if (var5 != null) {
            var2.add(var5);
         }
      }

      var2.sort(Score.SCORE_COMPARATOR);
      return var2;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<String> getTrackedPlayers() {
      return Lists.newArrayList(this.playerScores.keySet());
   }

   public void resetPlayerScore(String param0, @Nullable Objective param1) {
      if (param1 == null) {
         Map<Objective, Score> var3 = this.playerScores.remove(param0);
         if (var3 != null) {
            this.onPlayerRemoved(param0);
         }
      } else {
         Map<Objective, Score> var3 = this.playerScores.get(param0);
         if (var3 != null) {
            Score var4 = var3.remove(param1);
            if (var3.size() < 1) {
               Map<Objective, Score> var5 = this.playerScores.remove(param0);
               if (var5 != null) {
                  this.onPlayerRemoved(param0);
               }
            } else if (var4 != null) {
               this.onPlayerScoreRemoved(param0, param1);
            }
         }
      }
   }

   public Map<Objective, Score> getPlayerScores(String param0) {
      Map<Objective, Score> var2 = this.playerScores.get(param0);
      if (var2 == null) {
         var2 = Maps.newHashMap();
      }

      return var2;
   }

   public void removeObjective(Objective param0) {
      this.objectivesByName.remove(param0.getName());

      for (int var2 = 0; var2 < 19; var2++) {
         if (this.getDisplayObjective(var2) == param0) {
            this.setDisplayObjective(var2, null);
         }
      }

      List<Objective> var2 = this.objectivesByCriteria.get(param0.getCriteria());
      if (var2 != null) {
         var2.remove(param0);
      }

      for (Map<Objective, Score> var4 : this.playerScores.values()) {
         var4.remove(param0);
      }

      this.onObjectiveRemoved(param0);
   }

   public void setDisplayObjective(int param0, @Nullable Objective param1) {
      this.displayObjectives[param0] = param1;
   }

   @Nullable
   public Objective getDisplayObjective(int param0) {
      return this.displayObjectives[param0];
   }

   @Nullable
   public PlayerTeam getPlayerTeam(String param0) {
      return this.teamsByName.get(param0);
   }

   public PlayerTeam addPlayerTeam(String param0) {
      PlayerTeam var2 = this.getPlayerTeam(param0);
      if (var2 != null) {
         LOGGER.warn("Requested creation of existing team '{}'", param0);
         return var2;
      } else {
         var2 = new PlayerTeam(this, param0);
         this.teamsByName.put(param0, var2);
         this.onTeamAdded(var2);
         return var2;
      }
   }

   public void removePlayerTeam(PlayerTeam param0) {
      this.teamsByName.remove(param0.getName());

      for (String var3 : param0.getPlayers()) {
         this.teamsByPlayer.remove(var3);
      }

      this.onTeamRemoved(param0);
   }

   public boolean addPlayerToTeam(String param0, PlayerTeam param1) {
      if (this.getPlayersTeam(param0) != null) {
         this.removePlayerFromTeam(param0);
      }

      this.teamsByPlayer.put(param0, param1);
      return param1.getPlayers().add(param0);
   }

   public boolean removePlayerFromTeam(String param0) {
      PlayerTeam var2 = this.getPlayersTeam(param0);
      if (var2 != null) {
         this.removePlayerFromTeam(param0, var2);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(String param0, PlayerTeam param1) {
      if (this.getPlayersTeam(param0) != param1) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + param1.getName() + "'.");
      }

      this.teamsByPlayer.remove(param0);
      param1.getPlayers().remove(param0);
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   @Nullable
   public PlayerTeam getPlayersTeam(String param0) {
      return this.teamsByPlayer.get(param0);
   }

   public void onObjectiveAdded(Objective param0) {
   }

   public void onObjectiveChanged(Objective param0) {
   }

   public void onObjectiveRemoved(Objective param0) {
   }

   public void onScoreChanged(Score param0) {
   }

   public void onPlayerRemoved(String param0) {
   }

   public void onPlayerScoreRemoved(String param0, Objective param1) {
   }

   public void onTeamAdded(PlayerTeam param0) {
   }

   public void onTeamChanged(PlayerTeam param0) {
   }

   public void onTeamRemoved(PlayerTeam param0) {
   }

   public static String getDisplaySlotName(int param0) {
      switch (param0) {
         case 0:
            return "list";
         case 1:
            return "sidebar";
         case 2:
            return "belowName";
         default:
            if (param0 >= 3 && param0 <= 18) {
               ChatFormatting var1 = ChatFormatting.getById(param0 - 3);
               if (var1 != null && var1 != ChatFormatting.RESET) {
                  return "sidebar.team." + var1.getName();
               }
            }

            return null;
      }
   }

   public static int getDisplaySlotByName(String param0) {
      if ("list".equalsIgnoreCase(param0)) {
         return 0;
      }

      if ("sidebar".equalsIgnoreCase(param0)) {
         return 1;
      }

      if ("belowName".equalsIgnoreCase(param0)) {
         return 2;
      }

      if (param0.startsWith("sidebar.team.")) {
         String var1 = param0.substring("sidebar.team.".length());
         ChatFormatting var2 = ChatFormatting.getByName(var1);
         if (var2 != null && var2.getId() >= 0) {
            return var2.getId() + 3;
         }
      }

      return -1;
   }

   public static String[] getDisplaySlotNames() {
      if (displaySlotNames == null) {
         displaySlotNames = new String[19];

         for (int var0 = 0; var0 < 19; var0++) {
            displaySlotNames[var0] = getDisplaySlotName(var0);
         }
      }

      return displaySlotNames;
   }

   public void entityRemoved(Entity param0) {
      if (param0 != null && !(param0 instanceof Player) && !param0.isAlive()) {
         String var2 = param0.getStringUUID();
         this.resetPlayerScore(var2, null);
         this.removePlayerFromTeam(var2);
      }
   }

   protected ListTag savePlayerScores() {
      ListTag var1 = new ListTag();
      this.playerScores
         .values()
         .stream()
         .map(Map::values)
         .forEach(l_param1 -> l_param1.stream().filter(l2_param0 -> l2_param0.getObjective() != null).forEach(l2_param1 -> {
            CompoundTag l2_var2 = new CompoundTag();
            l2_var2.putString("Name", l2_param1.getOwner());
            l2_var2.putString("Objective", l2_param1.getObjective().getName());
            l2_var2.putInt("Score", l2_param1.getScore());
            l2_var2.putBoolean("Locked", l2_param1.isLocked());
            var1.add(l2_var2);
         }));
      return var1;
   }

   protected void loadPlayerScores(ListTag param0) {
      for (int var2 = 0; var2 < param0.size(); var2++) {
         CompoundTag var3 = param0.getCompound(var2);
         Objective var4 = this.getOrCreateObjective(var3.getString("Objective"));
         String var5 = var3.getString("Name");
         Score var6 = this.getOrCreatePlayerScore(var5, var4);
         var6.setScore(var3.getInt("Score"));
         if (var3.contains("Locked")) {
            var6.setLocked(var3.getBoolean("Locked"));
         }
      }
   }
}
