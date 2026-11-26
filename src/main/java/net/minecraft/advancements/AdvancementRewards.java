package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AdvancementRewards {
   public static final AdvancementRewards EMPTY = new AdvancementRewards(
      0, new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE
   );
   private final int experience;
   private final ResourceLocation[] loot;
   private final ResourceLocation[] recipes;
   private final CommandFunction.CacheableFunction function;

   public AdvancementRewards(int param0, ResourceLocation[] param1, ResourceLocation[] param2, CommandFunction.CacheableFunction param3) {
      this.experience = param0;
      this.loot = param1;
      this.recipes = param2;
      this.function = param3;
   }

   public ResourceLocation[] getRecipes() {
      return this.recipes;
   }

   public void grant(ServerPlayer param0) {
      param0.giveExperiencePoints(this.experience);
      LootContext var2 = new LootContext.Builder(param0.getLevel())
         .withParameter(LootContextParams.THIS_ENTITY, param0)
         .withParameter(LootContextParams.ORIGIN, param0.position())
         .withRandom(param0.getRandom())
         .create(LootContextParamSets.ADVANCEMENT_REWARD);
      boolean var3 = false;

      for (ResourceLocation var7 : this.loot) {
         for (ItemStack var9 : param0.server.getLootTables().get(var7).getRandomItems(var2)) {
            if (param0.addItem(var9)) {
               param0.level
                  .playSound(
                     null,
                     param0.getX(),
                     param0.getY(),
                     param0.getZ(),
                     SoundEvents.ITEM_PICKUP,
                     SoundSource.PLAYERS,
                     0.2F,
                     ((param0.getRandom().nextFloat() - param0.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                  );
               var3 = true;
            } else {
               ItemEntity var10 = param0.drop(var9, false);
               if (var10 != null) {
                  var10.setNoPickUpDelay();
                  var10.setTarget(param0.getUUID());
               }
            }
         }
      }

      if (var3) {
         param0.containerMenu.broadcastChanges();
      }

      if (this.recipes.length > 0) {
         param0.awardRecipesByKey(this.recipes);
      }

      MinecraftServer var4 = param0.server;
      this.function
         .get(var4.getFunctions())
         .ifPresent(l_param2 -> var4.getFunctions().execute(l_param2, param0.createCommandSourceStack().withSuppressedOutput().withPermission(2)));
   }

   @Override
   public String toString() {
      return "AdvancementRewards{experience="
         + this.experience
         + ", loot="
         + Arrays.toString(this.loot)
         + ", recipes="
         + Arrays.toString(this.recipes)
         + ", function="
         + this.function
         + "}";
   }

   public JsonElement serializeToJson() {
      if (this == EMPTY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      if (this.experience != 0) {
         var1.addProperty("experience", this.experience);
      }

      if (this.loot.length > 0) {
         JsonArray var2 = new JsonArray();

         for (ResourceLocation var6 : this.loot) {
            var2.add(var6.toString());
         }

         var1.add("loot", var2);
      }

      if (this.recipes.length > 0) {
         JsonArray var2 = new JsonArray();

         for (ResourceLocation var6 : this.recipes) {
            var2.add(var6.toString());
         }

         var1.add("recipes", var2);
      }

      if (this.function.getId() != null) {
         var1.addProperty("function", this.function.getId().toString());
      }

      return var1;
   }

   public static AdvancementRewards deserialize(JsonObject param0) throws JsonParseException {
      int var1 = GsonHelper.getAsInt(param0, "experience", 0);
      JsonArray var2 = GsonHelper.getAsJsonArray(param0, "loot", new JsonArray());
      ResourceLocation[] var3 = new ResourceLocation[var2.size()];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = new ResourceLocation(GsonHelper.convertToString(var2.get(var4), "loot[" + var4 + "]"));
      }

      JsonArray var4 = GsonHelper.getAsJsonArray(param0, "recipes", new JsonArray());
      ResourceLocation[] var5 = new ResourceLocation[var4.size()];

      for (int var6 = 0; var6 < var5.length; var6++) {
         var5[var6] = new ResourceLocation(GsonHelper.convertToString(var4.get(var6), "recipes[" + var6 + "]"));
      }

      CommandFunction.CacheableFunction var6;
      if (param0.has("function")) {
         var6 = new CommandFunction.CacheableFunction(new ResourceLocation(GsonHelper.getAsString(param0, "function")));
      } else {
         var6 = CommandFunction.CacheableFunction.NONE;
      }

      return new AdvancementRewards(var1, var3, var5, var6);
   }

   public static class Builder {
      private int experience;
      private final List<ResourceLocation> loot = Lists.newArrayList();
      private final List<ResourceLocation> recipes = Lists.newArrayList();
      @Nullable
      private ResourceLocation function;

      public static AdvancementRewards.Builder experience(int param0) {
         return new AdvancementRewards.Builder().addExperience(param0);
      }

      public AdvancementRewards.Builder addExperience(int param0) {
         this.experience += param0;
         return this;
      }

      public static AdvancementRewards.Builder loot(ResourceLocation param0) {
         return new AdvancementRewards.Builder().addLootTable(param0);
      }

      public AdvancementRewards.Builder addLootTable(ResourceLocation param0) {
         this.loot.add(param0);
         return this;
      }

      public static AdvancementRewards.Builder recipe(ResourceLocation param0) {
         return new AdvancementRewards.Builder().addRecipe(param0);
      }

      public AdvancementRewards.Builder addRecipe(ResourceLocation param0) {
         this.recipes.add(param0);
         return this;
      }

      public static AdvancementRewards.Builder function(ResourceLocation param0) {
         return new AdvancementRewards.Builder().runs(param0);
      }

      public AdvancementRewards.Builder runs(ResourceLocation param0) {
         this.function = param0;
         return this;
      }

      public AdvancementRewards build() {
         return new AdvancementRewards(
            this.experience,
            this.loot.toArray(new ResourceLocation[0]),
            this.recipes.toArray(new ResourceLocation[0]),
            this.function == null ? CommandFunction.CacheableFunction.NONE : new CommandFunction.CacheableFunction(this.function)
         );
      }
   }
}
