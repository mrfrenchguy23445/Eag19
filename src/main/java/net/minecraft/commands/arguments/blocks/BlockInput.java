package net.minecraft.commands.arguments.blocks;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockInput implements Predicate<BlockInWorld> {
   private final BlockState state;
   private final Set<Property<?>> properties;
   @Nullable
   private final CompoundTag tag;

   public BlockInput(BlockState param0, Set<Property<?>> param1, @Nullable CompoundTag param2) {
      this.state = param0;
      this.properties = param1;
      this.tag = param2;
   }

   public BlockState getState() {
      return this.state;
   }

   public Set<Property<?>> getDefinedProperties() {
      return this.properties;
   }

   public boolean test(BlockInWorld param0) {
      BlockState var2 = param0.getState();
      if (!var2.is(this.state.getBlock())) {
         return false;
      }

      for (Property<?> var4 : this.properties) {
         if (var2.getValue(var4) != this.state.getValue(var4)) {
            return false;
         }
      }

      if (this.tag == null) {
         return true;
      }

      BlockEntity var3 = param0.getEntity();
      return var3 != null && NbtUtils.compareNbt(this.tag, var3.saveWithFullMetadata(), true);
   }

   public boolean test(ServerLevel param0, BlockPos param1) {
      return this.test(new BlockInWorld(param0, param1, false));
   }

   public boolean place(ServerLevel param0, BlockPos param1, int param2) {
      BlockState var4 = Block.updateFromNeighbourShapes(this.state, param0, param1);
      if (var4.isAir()) {
         var4 = this.state;
      }

      if (!param0.setBlock(param1, var4, param2)) {
         return false;
      }

      if (this.tag != null) {
         BlockEntity var5 = param0.getBlockEntity(param1);
         if (var5 != null) {
            var5.load(this.tag);
         }
      }

      return true;
   }
}
