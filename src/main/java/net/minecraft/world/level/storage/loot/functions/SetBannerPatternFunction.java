package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemConditionalFunction {
   final List<Pair<Holder<BannerPattern>, DyeColor>> patterns;
   final boolean append;

   SetBannerPatternFunction(LootItemCondition[] param0, List<Pair<Holder<BannerPattern>, DyeColor>> param1, boolean param2) {
      super(param0);
      this.patterns = param1;
      this.append = param2;
   }

   @Override
   protected ItemStack run(ItemStack param0, LootContext param1) {
      CompoundTag var3 = BlockItem.getBlockEntityData(param0);
      if (var3 == null) {
         var3 = new CompoundTag();
      }

      BannerPattern.Builder var4 = new BannerPattern.Builder();
      this.patterns.forEach(var4::addPattern);
      ListTag var5 = var4.toListTag();
      ListTag var6;
      if (this.append) {
         var6 = var3.getList("Patterns", 10).copy();
         var6.addAll(var5);
      } else {
         var6 = var5;
      }

      var3.put("Patterns", var6);
      BlockItem.setBlockEntityData(param0, BlockEntityType.BANNER, var3);
      return param0;
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_BANNER_PATTERN;
   }

   public static SetBannerPatternFunction.Builder setBannerPattern(boolean param0) {
      return new SetBannerPatternFunction.Builder(param0);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
      private final ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> patterns = ImmutableList.builder();
      private final boolean append;

      Builder(boolean param0) {
         this.append = param0;
      }

      protected SetBannerPatternFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder addPattern(ResourceKey<BannerPattern> param0, DyeColor param1) {
         return this.addPattern(BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(param0), param1);
      }

      public SetBannerPatternFunction.Builder addPattern(Holder<BannerPattern> param0, DyeColor param1) {
         this.patterns.add(Pair.of(param0, param1));
         return this;
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetBannerPatternFunction> {
      public void serialize(JsonObject param0, SetBannerPatternFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         JsonArray var4 = new JsonArray();
         param1.patterns
            .forEach(
               l_param1 -> {
                  JsonObject l_var2 = new JsonObject();
                  l_var2.addProperty(
                     "pattern",
                     l_param1.getFirst()
                        .unwrapKey()
                        .orElseThrow(() -> new JsonSyntaxException("Unknown pattern: " + l_param1.getFirst()))
                        .location()
                        .toString()
                  );
                  l_var2.addProperty("color", l_param1.getSecond().getName());
                  var4.add(l_var2);
               }
            );
         param0.add("patterns", var4);
         param0.addProperty("append", param1.append);
      }

      public SetBannerPatternFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> var4 = ImmutableList.builder();
         JsonArray var5 = GsonHelper.getAsJsonArray(param0, "patterns");

         for (int var6 = 0; var6 < var5.size(); var6++) {
            JsonObject var7 = GsonHelper.convertToJsonObject(var5.get(var6), "pattern[" + var6 + "]");
            String var8 = GsonHelper.getAsString(var7, "pattern");
            Optional<? extends Holder<BannerPattern>> var9 = BuiltInRegistries.BANNER_PATTERN
               .getHolder(ResourceKey.create(Registries.BANNER_PATTERN, new ResourceLocation(var8)));
            if (var9.isEmpty()) {
               throw new JsonSyntaxException("Unknown pattern: " + var8);
            }

            String var10 = GsonHelper.getAsString(var7, "color");
            DyeColor var11 = DyeColor.byName(var10, null);
            if (var11 == null) {
               throw new JsonSyntaxException("Unknown color: " + var10);
            }

            var4.add(Pair.of((Holder<BannerPattern>)var9.get(), var11));
         }

         boolean var6 = GsonHelper.getAsBoolean(param0, "append");
         return new SetBannerPatternFunction(param2, var4.build(), var6);
      }
   }
}
