package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPredicate {
   public static final BlockPredicate ANY = new BlockPredicate(null, null, StatePropertiesPredicate.ANY, NbtPredicate.ANY);
   @Nullable
   private final TagKey<Block> tag;
   @Nullable
   private final Set<Block> blocks;
   private final StatePropertiesPredicate properties;
   private final NbtPredicate nbt;

   public BlockPredicate(@Nullable TagKey<Block> param0, @Nullable Set<Block> param1, StatePropertiesPredicate param2, NbtPredicate param3) {
      this.tag = param0;
      this.blocks = param1;
      this.properties = param2;
      this.nbt = param3;
   }

   public boolean matches(ServerLevel param0, BlockPos param1) {
      if (this == ANY) {
         return true;
      }

      if (!param0.isLoaded(param1)) {
         return false;
      }

      BlockState var3 = param0.getBlockState(param1);
      if (this.tag != null && !var3.is(this.tag)) {
         return false;
      }

      if (this.blocks != null && !this.blocks.contains(var3.getBlock())) {
         return false;
      }

      if (!this.properties.matches(var3)) {
         return false;
      }

      if (this.nbt != NbtPredicate.ANY) {
         BlockEntity var4 = param0.getBlockEntity(param1);
         if (var4 == null || !this.nbt.matches(var4.saveWithFullMetadata())) {
            return false;
         }
      }

      return true;
   }

   public static BlockPredicate fromJson(@Nullable JsonElement param0) {
      if (param0 != null && !param0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(param0, "block");
         NbtPredicate var2 = NbtPredicate.fromJson(var1.get("nbt"));
         Set<Block> var3 = null;
         JsonArray var4 = GsonHelper.getAsJsonArray(var1, "blocks", null);
         if (var4 != null) {
            ImmutableSet.Builder<Block> var5 = ImmutableSet.builder();

            for (JsonElement var7 : var4) {
               ResourceLocation var8 = new ResourceLocation(GsonHelper.convertToString(var7, "block"));
               var5.add(BuiltInRegistries.BLOCK.getOptional(var8).orElseThrow(() -> new JsonSyntaxException("Unknown block id '" + var8 + "'")));
            }

            var3 = var5.build();
         }

         TagKey<Block> var5 = null;
         if (var1.has("tag")) {
            ResourceLocation var6 = new ResourceLocation(GsonHelper.getAsString(var1, "tag"));
            var5 = TagKey.create(Registries.BLOCK, var6);
         }

         StatePropertiesPredicate var6 = StatePropertiesPredicate.fromJson(var1.get("state"));
         return new BlockPredicate(var5, var3, var6, var2);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      }

      JsonObject var1 = new JsonObject();
      if (this.blocks != null) {
         JsonArray var2 = new JsonArray();

         for (Block var4 : this.blocks) {
            var2.add(BuiltInRegistries.BLOCK.getKey(var4).toString());
         }

         var1.add("blocks", var2);
      }

      if (this.tag != null) {
         var1.addProperty("tag", this.tag.location().toString());
      }

      var1.add("nbt", this.nbt.serializeToJson());
      var1.add("state", this.properties.serializeToJson());
      return var1;
   }

   public static class Builder {
      @Nullable
      private Set<Block> blocks;
      @Nullable
      private TagKey<Block> tag;
      private StatePropertiesPredicate properties = StatePropertiesPredicate.ANY;
      private NbtPredicate nbt = NbtPredicate.ANY;

      private Builder() {
      }

      public static BlockPredicate.Builder block() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder of(Block... param0) {
         this.blocks = ImmutableSet.copyOf(param0);
         return this;
      }

      public BlockPredicate.Builder of(Iterable<Block> param0) {
         this.blocks = ImmutableSet.copyOf(param0);
         return this;
      }

      public BlockPredicate.Builder of(TagKey<Block> param0) {
         this.tag = param0;
         return this;
      }

      public BlockPredicate.Builder hasNbt(CompoundTag param0) {
         this.nbt = new NbtPredicate(param0);
         return this;
      }

      public BlockPredicate.Builder setProperties(StatePropertiesPredicate param0) {
         this.properties = param0;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.tag, this.blocks, this.properties, this.nbt);
      }
   }
}
