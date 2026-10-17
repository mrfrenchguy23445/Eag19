package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClientboundSectionBlocksUpdatePacket implements Packet<ClientGamePacketListener> {
   private static final int POS_IN_SECTION_BITS = 12;
   private final SectionPos sectionPos;
   private final short[] positions;
   private final BlockState[] states;
   private final boolean suppressLightUpdates;

   public ClientboundSectionBlocksUpdatePacket(SectionPos param0, ShortSet param1, LevelChunkSection param2, boolean param3) {
      this.sectionPos = param0;
      this.suppressLightUpdates = param3;
      int var5 = param1.size();
      this.positions = new short[var5];
      this.states = new BlockState[var5];
      int var6 = 0;

      for (short var8 : param1) {
         this.positions[var6] = var8;
         this.states[var6] = param2.getBlockState(SectionPos.sectionRelativeX(var8), SectionPos.sectionRelativeY(var8), SectionPos.sectionRelativeZ(var8));
         var6++;
      }
   }

   public ClientboundSectionBlocksUpdatePacket(FriendlyByteBuf param0) {
      this.sectionPos = SectionPos.of(param0.readLong());
      this.suppressLightUpdates = param0.readBoolean();
      int var2 = param0.readVarInt();
      this.positions = new short[var2];
      this.states = new BlockState[var2];

      for (int var3 = 0; var3 < var2; var3++) {
         long var4 = param0.readVarLong();
         this.positions[var3] = (short)(var4 & 4095L);
         this.states[var3] = Block.BLOCK_STATE_REGISTRY.byId((int)(var4 >>> 12));
      }
   }

   @Override
   public void write(FriendlyByteBuf param0) {
      param0.writeLong(this.sectionPos.asLong());
      param0.writeBoolean(this.suppressLightUpdates);
      param0.writeVarInt(this.positions.length);

      for (int var2 = 0; var2 < this.positions.length; var2++) {
         param0.writeVarLong((long)Block.getId(this.states[var2]) << 12 | this.positions[var2]);
      }
   }

   public void handle(ClientGamePacketListener param0) {
      param0.handleChunkBlocksUpdate(this);
   }

   public void runUpdates(BiConsumer<BlockPos, BlockState> param0) {
      BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();

      for (int var3 = 0; var3 < this.positions.length; var3++) {
         short var4 = this.positions[var3];
         var2.set(this.sectionPos.relativeToBlockX(var4), this.sectionPos.relativeToBlockY(var4), this.sectionPos.relativeToBlockZ(var4));
         param0.accept(var2, this.states[var3]);
      }
   }

   public boolean shouldSuppressLightUpdates() {
      return this.suppressLightUpdates;
   }
}
