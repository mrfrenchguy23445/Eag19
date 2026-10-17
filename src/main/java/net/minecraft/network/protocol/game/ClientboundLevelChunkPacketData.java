package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientboundLevelChunkPacketData {
   private static final int TWO_MEGABYTES = 2097152;
   private final CompoundTag heightmaps;
   private final byte[] buffer;
   private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

   public ClientboundLevelChunkPacketData(LevelChunk param0) {
      this.heightmaps = new CompoundTag();

      for (Entry<Heightmap.Types, Heightmap> var3 : param0.getHeightmaps()) {
         if (var3.getKey().sendToClient()) {
            this.heightmaps.put(var3.getKey().getSerializationKey(), new LongArrayTag(var3.getValue().getRawData()));
         }
      }

      this.buffer = new byte[calculateChunkSize(param0)];
      extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), param0);
      this.blockEntitiesData = Lists.newArrayList();

      for (Entry<BlockPos, BlockEntity> var3 : param0.getBlockEntities().entrySet()) {
         this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create(var3.getValue()));
      }
   }

   public ClientboundLevelChunkPacketData(FriendlyByteBuf param0, int param1, int param2) {
      this.heightmaps = param0.readNbt();
      if (this.heightmaps == null) {
         throw new RuntimeException("Can't read heightmap in packet for [" + param1 + ", " + param2 + "]");
      }

      int var4 = param0.readVarInt();
      if (var4 > 2097152) {
         throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
      }

      this.buffer = new byte[var4];
      param0.readBytes(this.buffer);
      this.blockEntitiesData = param0.readList(ClientboundLevelChunkPacketData.BlockEntityInfo::new);
   }

   public void write(FriendlyByteBuf param0) {
      param0.writeNbt(this.heightmaps);
      param0.writeVarInt(this.buffer.length);
      param0.writeBytes(this.buffer);
      param0.writeCollection(this.blockEntitiesData, (l_param0, l_param1) -> l_param1.write(l_param0));
   }

   private static int calculateChunkSize(LevelChunk param0) {
      int var1 = 0;

      for (LevelChunkSection var5 : param0.getSections()) {
         var1 += var5.getSerializedSize();
      }

      return var1;
   }

   private ByteBuf getWriteBuffer() {
      ByteBuf var1 = Unpooled.wrappedBuffer(this.buffer);
      var1.writerIndex(0);
      return var1;
   }

   public static void extractChunkData(FriendlyByteBuf param0, LevelChunk param1) {
      for (LevelChunkSection var5 : param1.getSections()) {
         var5.write(param0);
      }
   }

   public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int param0, int param1) {
      return l_param2 -> this.getBlockEntitiesTags(l_param2, param0, param1);
   }

   private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput param0, int param1, int param2) {
      int var4 = 16 * param1;
      int var5 = 16 * param2;
      BlockPos.MutableBlockPos var6 = new BlockPos.MutableBlockPos();

      for (ClientboundLevelChunkPacketData.BlockEntityInfo var8 : this.blockEntitiesData) {
         int var9 = var4 + SectionPos.sectionRelative(var8.packedXZ >> 4);
         int var10 = var5 + SectionPos.sectionRelative(var8.packedXZ);
         var6.set(var9, var8.y, var10);
         param0.accept(var6, var8.type, var8.tag);
      }
   }

   public FriendlyByteBuf getReadBuffer() {
      return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
   }

   public CompoundTag getHeightmaps() {
      return this.heightmaps;
   }

   static class BlockEntityInfo {
      final int packedXZ;
      final int y;
      final BlockEntityType<?> type;
      @Nullable
      final CompoundTag tag;

      private BlockEntityInfo(int param0, int param1, BlockEntityType<?> param2, @Nullable CompoundTag param3) {
         this.packedXZ = param0;
         this.y = param1;
         this.type = param2;
         this.tag = param3;
      }

      private BlockEntityInfo(FriendlyByteBuf param0) {
         this.packedXZ = param0.readByte();
         this.y = param0.readShort();
         this.type = param0.readById(BuiltInRegistries.BLOCK_ENTITY_TYPE);
         this.tag = param0.readNbt();
      }

      void write(FriendlyByteBuf param0) {
         param0.writeByte(this.packedXZ);
         param0.writeShort(this.y);
         param0.writeId(BuiltInRegistries.BLOCK_ENTITY_TYPE, this.type);
         param0.writeNbt(this.tag);
      }

      static ClientboundLevelChunkPacketData.BlockEntityInfo create(BlockEntity param0) {
         CompoundTag var1 = param0.getUpdateTag();
         BlockPos var2 = param0.getBlockPos();
         int var3 = SectionPos.sectionRelative(var2.getX()) << 4 | SectionPos.sectionRelative(var2.getZ());
         return new ClientboundLevelChunkPacketData.BlockEntityInfo(var3, var2.getY(), param0.getType(), var1.isEmpty() ? null : var1);
      }
   }

   @FunctionalInterface
   public interface BlockEntityTagOutput {
      void accept(BlockPos var1, BlockEntityType<?> var2, @Nullable CompoundTag var3);
   }
}
