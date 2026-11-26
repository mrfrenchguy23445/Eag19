package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ClientboundUpdateRecipesPacket implements Packet<ClientGamePacketListener> {
   private final List<Recipe<?>> recipes;

   public ClientboundUpdateRecipesPacket(Collection<Recipe<?>> param0) {
      this.recipes = Lists.newArrayList(param0);
   }

   public ClientboundUpdateRecipesPacket(FriendlyByteBuf param0) {
      this.recipes = param0.readList(ClientboundUpdateRecipesPacket::fromNetwork);
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeCollection(this.recipes, ClientboundUpdateRecipesPacket::toNetwork);
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleUpdateRecipes(this);
   }

   public List<Recipe<?>> getRecipes() {
      return this.recipes;
   }

   public static Recipe<?> fromNetwork(FriendlyByteBuf param0) {
      ResourceLocation var1 = param0.readResourceLocation();
      ResourceLocation var2 = param0.readResourceLocation();
      return BuiltInRegistries.RECIPE_SERIALIZER
         .getOptional(var1)
         .orElseThrow(() -> new IllegalArgumentException("Unknown recipe serializer " + var1))
         .fromNetwork(var2, param0);
   }

   public static <T extends Recipe<?>> void toNetwork(FriendlyByteBuf param0, T param1) {
      param0.writeResourceLocation(BuiltInRegistries.RECIPE_SERIALIZER.getKey(param1.getSerializer()));
      param0.writeResourceLocation(param1.getId());
      ((RecipeSerializer<T>)param1.getSerializer()).toNetwork(param0, param1);
   }
}
