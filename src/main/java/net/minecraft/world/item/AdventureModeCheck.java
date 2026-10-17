package net.minecraft.world.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class AdventureModeCheck {
   private final String tagName;
   @Nullable
   private BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModeCheck(String param0) {
      this.tagName = param0;
   }

   private static boolean areSameBlocks(BlockInWorld param0, @Nullable BlockInWorld param1, boolean param2) {
      if (param1 == null || param0.getState() != param1.getState()) {
         return false;
      } else if (!param2) {
         return true;
      } else if (param0.getEntity() == null && param1.getEntity() == null) {
         return true;
      } else {
         return param0.getEntity() != null && param1.getEntity() != null
            ? Objects.equals(param0.getEntity().saveWithId(), param1.getEntity().saveWithId())
            : false;
      }
   }

   public boolean test(ItemStack param0, Registry<Block> param1, BlockInWorld param2) {
      if (areSameBlocks(param2, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      }

      this.lastCheckedBlock = param2;
      this.checksBlockEntity = false;
      CompoundTag var4 = param0.getTag();
      if (var4 != null && var4.contains(this.tagName, 9)) {
         ListTag var5 = var4.getList(this.tagName, 8);

         for (int var6 = 0; var6 < var5.size(); var6++) {
            String var7 = var5.getString(var6);

            try {
               BlockPredicateArgument.Result var8 = BlockPredicateArgument.parse(param1.asLookup(), new StringReader(var7));
               this.checksBlockEntity = this.checksBlockEntity | var8.requiresNbt();
               if (var8.test(param2)) {
                  this.lastResult = true;
                  return true;
               }
            } catch (CommandSyntaxException var9) {
            }
         }
      }

      this.lastResult = false;
      return false;
   }
}
