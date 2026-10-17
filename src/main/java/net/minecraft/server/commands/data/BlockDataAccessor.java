package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDataAccessor implements DataAccessor {
   static final SimpleCommandExceptionType ERROR_NOT_A_BLOCK_ENTITY = new SimpleCommandExceptionType(Component.translatable("commands.data.block.invalid"));
   public static final Function<String, DataCommands.DataProvider> PROVIDER = param0 -> new DataCommands.DataProvider() {
      @Override
      public DataAccessor access(CommandContext<CommandSourceStack> l_param0) throws CommandSyntaxException {
         BlockPos l_var2 = BlockPosArgument.getLoadedBlockPos(l_param0, param0 + "Pos");
         BlockEntity l_var3 = l_param0.getSource().getLevel().getBlockEntity(l_var2);
         if (l_var3 == null) {
            throw BlockDataAccessor.ERROR_NOT_A_BLOCK_ENTITY.create();
         } else {
            return new BlockDataAccessor(l_var3, l_var2);
         }
      }

      @Override
      public ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> l_param0, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> l_param1
      ) {
         return l_param0.then(Commands.literal("block").then(l_param1.apply(Commands.argument(param0 + "Pos", BlockPosArgument.blockPos()))));
      }
   };
   private final BlockEntity entity;
   private final BlockPos pos;

   public BlockDataAccessor(BlockEntity param0, BlockPos param1) {
      this.entity = param0;
      this.pos = param1;
   }

   @Override
   public void setData(CompoundTag param0) {
      BlockState var2 = this.entity.getLevel().getBlockState(this.pos);
      this.entity.load(param0);
      this.entity.setChanged();
      this.entity.getLevel().sendBlockUpdated(this.pos, var2, var2, 3);
   }

   @Override
   public CompoundTag getData() {
      return this.entity.saveWithFullMetadata();
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.block.modified", this.pos.getX(), this.pos.getY(), this.pos.getZ());
   }

   @Override
   public Component getPrintSuccess(Tag param0) {
      return Component.translatable("commands.data.block.query", this.pos.getX(), this.pos.getY(), this.pos.getZ(), NbtUtils.toPrettyComponent(param0));
   }

   @Override
   public Component getPrintSuccess(NbtPathArgument.NbtPath param0, double param1, int param3) {
      return Component.translatable(
         "commands.data.block.get", param0, this.pos.getX(), this.pos.getY(), this.pos.getZ(), String.format(Locale.ROOT, "%.2f", param1), param3
      );
   }
}
