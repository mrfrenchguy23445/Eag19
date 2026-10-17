package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class ScoreContents implements ComponentContents {
   private static final String SCORER_PLACEHOLDER = "*";
   private final String name;
   @Nullable
   private final EntitySelector selector;
   private final String objective;

   @Nullable
   private static EntitySelector parseSelector(String param0) {
      try {
         return new EntitySelectorParser(new StringReader(param0)).parse();
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   public ScoreContents(String param0, String param1) {
      this.name = param0;
      this.selector = parseSelector(param0);
      this.objective = param1;
   }

   public String getName() {
      return this.name;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   public String getObjective() {
      return this.objective;
   }

   private String findTargetName(CommandSourceStack param0) throws CommandSyntaxException {
      if (this.selector != null) {
         List<? extends Entity> var2 = this.selector.findEntities(param0);
         if (!var2.isEmpty()) {
            if (var2.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            }

            return var2.get(0).getScoreboardName();
         }
      }

      return this.name;
   }

   private String getScore(String param0, CommandSourceStack param1) {
      MinecraftServer var3 = param1.getServer();
      if (var3 != null) {
         Scoreboard var4 = var3.getScoreboard();
         Objective var5 = var4.getObjective(this.objective);
         if (var4.hasPlayerScore(param0, var5)) {
            Score var6 = var4.getOrCreatePlayerScore(param0, var5);
            return Integer.toString(var6.getScore());
         }
      }

      return "";
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack param0, @Nullable Entity param1, int param2) throws CommandSyntaxException {
      if (param0 == null) {
         return Component.empty();
      }

      String var4 = this.findTargetName(param0);
      String var5 = param1 != null && var4.equals("*") ? param1.getScoreboardName() : var4;
      return Component.literal(this.getScore(var5, param0));
   }

   @Override
   public boolean equals(Object param0) {
      return this == param0 ? true : param0 instanceof ScoreContents var2 && this.name.equals(var2.name) && this.objective.equals(var2.objective);
   }

   @Override
   public int hashCode() {
      int var1 = this.name.hashCode();
      return 31 * var1 + this.objective.hashCode();
   }

   @Override
   public String toString() {
      return "score{name='" + this.name + "', objective='" + this.objective + "'}";
   }
}
