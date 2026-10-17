package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private final int radius = 12;
   @Nullable
   private ChunkDebugRenderer.ChunkData data;

   public ChunkDebugRenderer(Minecraft param0) {
      this.minecraft = param0;
   }

   @Override
   public void render(PoseStack param0, MultiBufferSource param1, double param2, double param4, double param6) {
      double var9 = Util.getNanos();
      if (var9 - this.lastUpdateTime > 3.0E9) {
         this.lastUpdateTime = var9;
         IntegratedServer var11 = this.minecraft.getSingleplayerServer();
         if (var11 != null) {
            this.data = new ChunkDebugRenderer.ChunkData(var11, param2, param6);
         } else {
            this.data = null;
         }
      }

      if (this.data != null) {
         Map<ChunkPos, String> var11 = this.data.serverData.getNow(null);
         double var12 = this.minecraft.gameRenderer.getMainCamera().getPosition().y * 0.85;

         for (Entry<ChunkPos, String> var15 : this.data.clientData.entrySet()) {
            ChunkPos var16 = var15.getKey();
            String var17 = var15.getValue();
            if (var11 != null) {
               var17 = var17 + var11.get(var16);
            }

            String[] var18 = var17.split("\n");
            int var19 = 0;

            for (String var23 : var18) {
               DebugRenderer.renderFloatingText(
                  param0,
                  param1,
                  var23,
                  SectionPos.sectionToBlockCoord(var16.x, 8),
                  var12 + var19,
                  SectionPos.sectionToBlockCoord(var16.z, 8),
                  -1,
                  0.15F,
                  true,
                  0.0F,
                  true
               );
               var19 -= 2;
            }
         }
      }
   }

   final class ChunkData {
      final Map<ChunkPos, String> clientData;
      final CompletableFuture<Map<ChunkPos, String>> serverData;

      ChunkData(IntegratedServer param1, double param2, double param4) {
         ClientLevel var7 = ChunkDebugRenderer.this.minecraft.level;
         ResourceKey<Level> var8 = var7.dimension();
         int var9 = SectionPos.posToSectionCoord(param2);
         int var10 = SectionPos.posToSectionCoord(param4);
         Builder<ChunkPos, String> var11 = ImmutableMap.builder();
         ClientChunkCache var12 = var7.getChunkSource();

         for (int var13 = var9 - 12; var13 <= var9 + 12; var13++) {
            for (int var14 = var10 - 12; var14 <= var10 + 12; var14++) {
               ChunkPos var15 = new ChunkPos(var13, var14);
               String var16 = "";
               LevelChunk var17 = var12.getChunk(var13, var14, false);
               var16 = var16 + "Client: ";
               if (var17 == null) {
                  var16 = var16 + "0n/a\n";
               } else {
                  var16 = var16 + (var17.isEmpty() ? " E" : "");
                  var16 = var16 + "\n";
               }

               var11.put(var15, var16);
            }
         }

         this.clientData = var11.build();
         this.serverData = param1.submit(() -> {
            ServerLevel l_var5 = param1.getLevel(var8);
            if (l_var5 == null) {
               return ImmutableMap.of();
            }

            Builder<ChunkPos, String> l_var6 = ImmutableMap.builder();
            ServerChunkCache l_var7 = l_var5.getChunkSource();

            for (int l_var8 = var9 - 12; l_var8 <= var9 + 12; l_var8++) {
               for (int l_var9 = var10 - 12; l_var9 <= var10 + 12; l_var9++) {
                  ChunkPos l_var10 = new ChunkPos(l_var8, l_var9);
                  l_var6.put(l_var10, "Server: " + l_var7.getChunkDebugData(l_var10));
               }
            }

            return l_var6.build();
         });
      }
   }
}
