package net.minecraft.stats;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceLocationException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
   public static final String RECIPE_BOOK_TAG = "recipeBook";
   private static final Logger LOGGER = LogUtils.getLogger();

   public int addRecipes(Collection<Recipe<?>> param0, ServerPlayer param1) {
      List<ResourceLocation> var3 = Lists.newArrayList();
      int var4 = 0;

      for (Recipe<?> var6 : param0) {
         ResourceLocation var7 = var6.getId();
         if (!this.known.contains(var7) && !var6.isSpecial()) {
            this.add(var7);
            this.addHighlight(var7);
            var3.add(var7);
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(param1, var6);
            var4++;
         }
      }

      this.sendRecipes(ClientboundRecipePacket.State.ADD, param1, var3);
      return var4;
   }

   public int removeRecipes(Collection<Recipe<?>> param0, ServerPlayer param1) {
      List<ResourceLocation> var3 = Lists.newArrayList();
      int var4 = 0;

      for (Recipe<?> var6 : param0) {
         ResourceLocation var7 = var6.getId();
         if (this.known.contains(var7)) {
            this.remove(var7);
            var3.add(var7);
            var4++;
         }
      }

      this.sendRecipes(ClientboundRecipePacket.State.REMOVE, param1, var3);
      return var4;
   }

   private void sendRecipes(ClientboundRecipePacket.State param0, ServerPlayer param1, List<ResourceLocation> param2) {
      param1.connection.send(new ClientboundRecipePacket(param0, param2, Collections.emptyList(), this.getBookSettings()));
   }

   public CompoundTag toNbt() {
      CompoundTag var1 = new CompoundTag();
      this.getBookSettings().write(var1);
      ListTag var2 = new ListTag();

      for (ResourceLocation var4 : this.known) {
         var2.add(StringTag.valueOf(var4.toString()));
      }

      var1.put("recipes", var2);
      ListTag var3 = new ListTag();

      for (ResourceLocation var5 : this.highlight) {
         var3.add(StringTag.valueOf(var5.toString()));
      }

      var1.put("toBeDisplayed", var3);
      return var1;
   }

   public void fromNbt(CompoundTag param0, RecipeManager param1) {
      this.setBookSettings(RecipeBookSettings.read(param0));
      ListTag var3 = param0.getList("recipes", 8);
      this.loadRecipes(var3, this::add, param1);
      ListTag var4 = param0.getList("toBeDisplayed", 8);
      this.loadRecipes(var4, this::addHighlight, param1);
   }

   private void loadRecipes(ListTag param0, Consumer<Recipe<?>> param1, RecipeManager param2) {
      for (int var4 = 0; var4 < param0.size(); var4++) {
         String var5 = param0.getString(var4);

         try {
            ResourceLocation var6 = new ResourceLocation(var5);
            Optional<? extends Recipe<?>> var7 = param2.byKey(var6);
            if (!var7.isPresent()) {
               LOGGER.error("Tried to load unrecognized recipe: {} removed now.", var6);
            } else {
               param1.accept((Recipe<?>)var7.get());
            }
         } catch (ResourceLocationException var6) {
            LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", var5);
         }
      }
   }

   public void sendInitialRecipeBook(ServerPlayer param0) {
      param0.connection.send(new ClientboundRecipePacket(ClientboundRecipePacket.State.INIT, this.known, this.highlight, this.getBookSettings()));
   }
}
