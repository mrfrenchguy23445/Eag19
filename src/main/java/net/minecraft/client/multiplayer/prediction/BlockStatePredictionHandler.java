package net.minecraft.client.multiplayer.prediction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockStatePredictionHandler implements AutoCloseable {
   private final Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap<>();
   private int currentSequenceNr;
   private boolean isPredicting;

   public void retainKnownServerState(BlockPos param0, BlockState param1, LocalPlayer param2) {
      this.serverVerifiedStates
         .compute(
            param0.asLong(),
            (l_param2, l_param3) -> l_param3 != null
               ? l_param3.setSequence(this.currentSequenceNr)
               : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, param1, param2.position())
         );
   }

   public boolean updateKnownServerState(BlockPos param0, BlockState param1) {
      BlockStatePredictionHandler.ServerVerifiedState var3 = this.serverVerifiedStates.get(param0.asLong());
      if (var3 == null) {
         return false;
      }

      var3.setBlockState(param1);
      return true;
   }

   public void endPredictionsUpTo(int param0, ClientLevel param1) {
      ObjectIterator<Entry<BlockStatePredictionHandler.ServerVerifiedState>> var3 = this.serverVerifiedStates.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<BlockStatePredictionHandler.ServerVerifiedState> var4 = var3.next();
         BlockStatePredictionHandler.ServerVerifiedState var5 = var4.getValue();
         if (var5.sequence <= param0) {
            BlockPos var6 = BlockPos.of(var4.getLongKey());
            var3.remove();
            param1.syncBlockState(var6, var5.blockState, var5.playerPos);
         }
      }
   }

   public BlockStatePredictionHandler startPredicting() {
      this.currentSequenceNr++;
      this.isPredicting = true;
      return this;
   }

   @Override
   public void close() {
      this.isPredicting = false;
   }

   public int currentSequence() {
      return this.currentSequenceNr;
   }

   public boolean isPredicting() {
      return this.isPredicting;
   }

   static class ServerVerifiedState {
      final Vec3 playerPos;
      int sequence;
      BlockState blockState;

      ServerVerifiedState(int param0, BlockState param1, Vec3 param2) {
         this.sequence = param0;
         this.blockState = param1;
         this.playerPos = param2;
      }

      BlockStatePredictionHandler.ServerVerifiedState setSequence(int param0) {
         this.sequence = param0;
         return this;
      }

      void setBlockState(BlockState param0) {
         this.blockState = param0;
      }
   }
}
