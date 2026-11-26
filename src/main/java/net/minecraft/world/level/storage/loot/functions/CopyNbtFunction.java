package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;

public class CopyNbtFunction extends LootItemConditionalFunction {
   final NbtProvider source;
   final List<CopyNbtFunction.CopyOperation> operations;

   CopyNbtFunction(LootItemCondition[] param0, NbtProvider param1, List<CopyNbtFunction.CopyOperation> param2) {
      super(param0);
      this.source = param1;
      this.operations = ImmutableList.copyOf(param2);
   }

   @Override
   public LootItemFunctionType getType() {
      return LootItemFunctions.COPY_NBT;
   }

   static NbtPathArgument.NbtPath compileNbtPath(String param0) {
      try {
         return new NbtPathArgument().parse(new StringReader(param0));
      } catch (CommandSyntaxException var1) {
         throw new IllegalArgumentException("Failed to parse path " + param0, var1);
      }
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.source.getReferencedContextParams();
   }

   @Override
   public ItemStack run(ItemStack param0, LootContext param1) {
      Tag var3 = this.source.get(param1);
      if (var3 != null) {
         this.operations.forEach(l_param2 -> l_param2.apply(param0::getOrCreateTag, var3));
      }

      return param0;
   }

   public static CopyNbtFunction.Builder copyData(NbtProvider param0) {
      return new CopyNbtFunction.Builder(param0);
   }

   public static CopyNbtFunction.Builder copyData(LootContext.EntityTarget param0) {
      return new CopyNbtFunction.Builder(ContextNbtProvider.forContextEntity(param0));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyNbtFunction.Builder> {
      private final NbtProvider source;
      private final List<CopyNbtFunction.CopyOperation> ops = Lists.newArrayList();

      Builder(NbtProvider param0) {
         this.source = param0;
      }

      public CopyNbtFunction.Builder copy(String param0, String param1, CopyNbtFunction.MergeStrategy param2) {
         this.ops.add(new CopyNbtFunction.CopyOperation(param0, param1, param2));
         return this;
      }

      public CopyNbtFunction.Builder copy(String param0, String param1) {
         return this.copy(param0, param1, CopyNbtFunction.MergeStrategy.REPLACE);
      }

      protected CopyNbtFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyNbtFunction(this.getConditions(), this.source, this.ops);
      }
   }

   static class CopyOperation {
      private final String sourcePathText;
      private final NbtPathArgument.NbtPath sourcePath;
      private final String targetPathText;
      private final NbtPathArgument.NbtPath targetPath;
      private final CopyNbtFunction.MergeStrategy op;

      CopyOperation(String param0, String param1, CopyNbtFunction.MergeStrategy param2) {
         this.sourcePathText = param0;
         this.sourcePath = CopyNbtFunction.compileNbtPath(param0);
         this.targetPathText = param1;
         this.targetPath = CopyNbtFunction.compileNbtPath(param1);
         this.op = param2;
      }

      public void apply(Supplier<Tag> param0, Tag param1) {
         try {
            List<Tag> var3 = this.sourcePath.get(param1);
            if (!var3.isEmpty()) {
               this.op.merge(param0.get(), this.targetPath, var3);
            }
         } catch (CommandSyntaxException var4) {
         }
      }

      public JsonObject toJson() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("source", this.sourcePathText);
         var1.addProperty("target", this.targetPathText);
         var1.addProperty("op", this.op.name);
         return var1;
      }

      public static CopyNbtFunction.CopyOperation fromJson(JsonObject param0) {
         String var1 = GsonHelper.getAsString(param0, "source");
         String var2 = GsonHelper.getAsString(param0, "target");
         CopyNbtFunction.MergeStrategy var3 = CopyNbtFunction.MergeStrategy.getByName(GsonHelper.getAsString(param0, "op"));
         return new CopyNbtFunction.CopyOperation(var1, var2, var3);
      }
   }

   public enum MergeStrategy {
      REPLACE("replace") {
         @Override
         public void merge(Tag param0, NbtPathArgument.NbtPath param1, List<Tag> param2) throws CommandSyntaxException {
            param1.set(param0, Iterables.getLast(param2));
         }
      },
      APPEND("append") {
         @Override
         public void merge(Tag param0, NbtPathArgument.NbtPath param1, List<Tag> param2) throws CommandSyntaxException {
            List<Tag> var4 = param1.getOrCreate(param0, ListTag::new);
            var4.forEach(l_param1 -> {
               if (l_param1 instanceof ListTag) {
                  param2.forEach(l2_param1 -> ((ListTag)l_param1).add(l2_param1.copy()));
               }
            });
         }
      },
      MERGE("merge") {
         @Override
         public void merge(Tag param0, NbtPathArgument.NbtPath param1, List<Tag> param2) throws CommandSyntaxException {
            List<Tag> var4 = param1.getOrCreate(param0, CompoundTag::new);
            var4.forEach(l_param1 -> {
               if (l_param1 instanceof CompoundTag) {
                  param2.forEach(l2_param1 -> {
                     if (l2_param1 instanceof CompoundTag) {
                        ((CompoundTag)l_param1).merge((CompoundTag)l2_param1);
                     }
                  });
               }
            });
         }
      };

      final String name;

      public abstract void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException;

      MergeStrategy(String param2) {
         this.name = param2;
      }

      public static CopyNbtFunction.MergeStrategy getByName(String param0) {
         for (CopyNbtFunction.MergeStrategy var4 : values()) {
            if (var4.name.equals(param0)) {
               return var4;
            }
         }

         throw new IllegalArgumentException("Invalid merge strategy" + param0);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<CopyNbtFunction> {
      public void serialize(JsonObject param0, CopyNbtFunction param1, JsonSerializationContext param2) {
         super.serialize(param0, param1, param2);
         param0.add("source", param2.serialize(param1.source));
         JsonArray var4 = new JsonArray();
         param1.operations.stream().map(CopyNbtFunction.CopyOperation::toJson).forEach(var4::add);
         param0.add("ops", var4);
      }

      public CopyNbtFunction deserialize(JsonObject param0, JsonDeserializationContext param1, LootItemCondition[] param2) {
         NbtProvider var4 = GsonHelper.getAsObject(param0, "source", param1, NbtProvider.class);
         List<CopyNbtFunction.CopyOperation> var5 = Lists.newArrayList();

         for (JsonElement var8 : GsonHelper.getAsJsonArray(param0, "ops")) {
            JsonObject var9 = GsonHelper.convertToJsonObject(var8, "op");
            var5.add(CopyNbtFunction.CopyOperation.fromJson(var9));
         }

         return new CopyNbtFunction(param2, var4, var5);
      }
   }
}
