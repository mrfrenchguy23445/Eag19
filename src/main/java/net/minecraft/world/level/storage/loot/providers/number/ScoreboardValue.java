package net.minecraft.world.level.storage.loot.providers.number;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardValue implements NumberProvider {
   final ScoreboardNameProvider target;
   final String score;
   final float scale;

   ScoreboardValue(ScoreboardNameProvider param0, String param1, float param2) {
      this.target = param0;
      this.score = param1;
      this.scale = param2;
   }

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.SCORE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.target.getReferencedContextParams();
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget param0, String param1) {
      return fromScoreboard(param0, param1, 1.0F);
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget param0, String param1, float param2) {
      return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(param0), param1, param2);
   }

   @Override
   public float getFloat(LootContext param0) {
      String var2 = this.target.getScoreboardName(param0);
      if (var2 == null) {
         return 0.0F;
      } else {
         Scoreboard var3 = param0.getLevel().getScoreboard();
         Objective var4 = var3.getObjective(this.score);
         if (var4 == null) {
            return 0.0F;
         } else {
            return !var3.hasPlayerScore(var2, var4) ? 0.0F : var3.getOrCreatePlayerScore(var2, var4).getScore() * this.scale;
         }
      }
   }

   public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ScoreboardValue> {
      public ScoreboardValue deserialize(JsonObject param0, JsonDeserializationContext param1) {
         String var3 = GsonHelper.getAsString(param0, "score");
         float var4 = GsonHelper.getAsFloat(param0, "scale", 1.0F);
         ScoreboardNameProvider var5 = GsonHelper.getAsObject(param0, "target", param1, ScoreboardNameProvider.class);
         return new ScoreboardValue(var5, var3, var4);
      }

      public void serialize(JsonObject param0, ScoreboardValue param1, JsonSerializationContext param2) {
         param0.addProperty("score", param1.score);
         param0.add("target", param2.serialize(param1.target));
         param0.addProperty("scale", param1.scale);
      }
   }
}
