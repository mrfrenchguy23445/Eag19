package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SPAWN_TIME = 200;
   private static final int COOLDOWN_TIME = 40;
   private static final int ATTENTION_INTERVAL = 2400;
   private static final int EVENT_COOLDOWN = 1;
   private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
   private long age;
   private int teleportCooldown;
   @Nullable
   private BlockPos exitPortal;
   private boolean exactTeleport;

   public TheEndGatewayBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.END_GATEWAY, param0, param1);
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.putLong("Age", this.age);
      if (this.exitPortal != null) {
         param0.put("ExitPortal", NbtUtils.writeBlockPos(this.exitPortal));
      }

      if (this.exactTeleport) {
         param0.putBoolean("ExactTeleport", true);
      }
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.age = param0.getLong("Age");
      if (param0.contains("ExitPortal", 10)) {
         BlockPos var2 = NbtUtils.readBlockPos(param0.getCompound("ExitPortal"));
         if (Level.isInSpawnableBounds(var2)) {
            this.exitPortal = var2;
         }
      }

      this.exactTeleport = param0.getBoolean("ExactTeleport");
   }

   public static void beamAnimationTick(Level param0, BlockPos param1, BlockState param2, TheEndGatewayBlockEntity param3) {
      param3.age++;
      if (param3.isCoolingDown()) {
         param3.teleportCooldown--;
      }
   }

   public static void teleportTick(Level param0, BlockPos param1, BlockState param2, TheEndGatewayBlockEntity param3) {
      boolean var4 = param3.isSpawning();
      boolean var5 = param3.isCoolingDown();
      param3.age++;
      if (var5) {
         param3.teleportCooldown--;
      } else {
         List<Entity> var6 = param0.getEntitiesOfClass(Entity.class, new AABB(param1), TheEndGatewayBlockEntity::canEntityTeleport);
         if (!var6.isEmpty()) {
            teleportEntity(param0, param1, param2, var6.get(param0.random.nextInt(var6.size())), param3);
         }

         if (param3.age % 2400L == 0L) {
            triggerCooldown(param0, param1, param2, param3);
         }
      }

      if (var4 != param3.isSpawning() || var5 != param3.isCoolingDown()) {
         setChanged(param0, param1, param2);
      }
   }

   public static boolean canEntityTeleport(Entity param0) {
      return EntitySelector.NO_SPECTATORS.test(param0) && !param0.getRootVehicle().isOnPortalCooldown();
   }

   public boolean isSpawning() {
      return this.age < 200L;
   }

   public boolean isCoolingDown() {
      return this.teleportCooldown > 0;
   }

   public float getSpawnPercent(float param0) {
      return Mth.clamp(((float)this.age + param0) / 200.0F, 0.0F, 1.0F);
   }

   public float getCooldownPercent(float param0) {
      return 1.0F - Mth.clamp((this.teleportCooldown - param0) / 40.0F, 0.0F, 1.0F);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   private static void triggerCooldown(Level param0, BlockPos param1, BlockState param2, TheEndGatewayBlockEntity param3) {
      if (!param0.isClientSide) {
         param3.teleportCooldown = 40;
         param0.blockEvent(param1, param2.getBlock(), 1, 0);
         setChanged(param0, param1, param2);
      }
   }

   @Override
   public boolean triggerEvent(int param0, int param1) {
      if (param0 == 1) {
         this.teleportCooldown = 40;
         return true;
      } else {
         return super.triggerEvent(param0, param1);
      }
   }

   public static void teleportEntity(Level param0, BlockPos param1, BlockState param2, Entity param3, TheEndGatewayBlockEntity param4) {
      if (param0 instanceof ServerLevel && !param4.isCoolingDown()) {
         ServerLevel var5 = (ServerLevel)param0;
         param4.teleportCooldown = 100;
         if (param4.exitPortal == null && param0.dimension() == Level.END) {
            BlockPos var6 = findOrCreateValidTeleportPos(var5, param1);
            var6 = var6.above(10);
            LOGGER.debug("Creating portal at {}", var6);
            spawnGatewayPortal(var5, var6, EndGatewayConfiguration.knownExit(param1, false));
            param4.exitPortal = var6;
         }

         if (param4.exitPortal != null) {
            BlockPos var6 = param4.exactTeleport ? param4.exitPortal : findExitPosition(param0, param4.exitPortal);
            Entity var7;
            if (param3 instanceof ThrownEnderpearl) {
               Entity var8 = ((ThrownEnderpearl)param3).getOwner();
               if (var8 instanceof ServerPlayer) {
                  CriteriaTriggers.ENTER_BLOCK.trigger((ServerPlayer)var8, param2);
               }

               if (var8 != null) {
                  var7 = var8;
                  param3.discard();
               } else {
                  var7 = param3;
               }
            } else {
               var7 = param3.getRootVehicle();
            }

            var7.setPortalCooldown();
            var7.teleportToWithTicket(var6.getX() + 0.5, var6.getY(), var6.getZ() + 0.5);
         }

         triggerCooldown(param0, param1, param2, param4);
      }
   }

   private static BlockPos findExitPosition(Level param0, BlockPos param1) {
      BlockPos var2 = findTallestBlock(param0, param1.offset(0, 2, 0), 5, false);
      LOGGER.debug("Best exit position for portal at {} is {}", param1, var2);
      return var2.above();
   }

   private static BlockPos findOrCreateValidTeleportPos(ServerLevel param0, BlockPos param1) {
      Vec3 var2 = findExitPortalXZPosTentative(param0, param1);
      LevelChunk var3 = getChunk(param0, var2);
      BlockPos var4 = findValidSpawnInChunk(var3);
      if (var4 == null) {
         BlockPos var5 = BlockPos.containing(var2.x + 0.5, 75.0, var2.z + 0.5);
         LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", var5);
         param0.registryAccess()
            .registry(Registries.CONFIGURED_FEATURE)
            .flatMap(l_param0 -> l_param0.getHolder(EndFeatures.END_ISLAND))
            .ifPresent(l_param2 -> l_param2.value().place(param0, param0.getChunkSource().getGenerator(), RandomSource.create(var5.asLong()), var5));
         var4 = var5;
      } else {
         LOGGER.debug("Found suitable block to teleport to: {}", var4);
      }

      return findTallestBlock(param0, var4, 16, true);
   }

   private static Vec3 findExitPortalXZPosTentative(ServerLevel param0, BlockPos param1) {
      Vec3 var2 = new Vec3(param1.getX(), 0.0, param1.getZ()).normalize();
      int var3 = 1024;
      Vec3 var4 = var2.scale(1024.0);

      for (int var5 = 16; !isChunkEmpty(param0, var4) && var5-- > 0; var4 = var4.add(var2.scale(-16.0))) {
         LOGGER.debug("Skipping backwards past nonempty chunk at {}", var4);
      }

      for (int var6 = 16; isChunkEmpty(param0, var4) && var6-- > 0; var4 = var4.add(var2.scale(16.0))) {
         LOGGER.debug("Skipping forward past empty chunk at {}", var4);
      }

      LOGGER.debug("Found chunk at {}", var4);
      return var4;
   }

   private static boolean isChunkEmpty(ServerLevel param0, Vec3 param1) {
      return getChunk(param0, param1).getHighestSectionPosition() <= param0.getMinBuildHeight();
   }

   private static BlockPos findTallestBlock(BlockGetter param0, BlockPos param1, int param2, boolean param3) {
      BlockPos var4 = null;

      for (int var5 = -param2; var5 <= param2; var5++) {
         for (int var6 = -param2; var6 <= param2; var6++) {
            if (var5 != 0 || var6 != 0 || param3) {
               for (int var7 = param0.getMaxBuildHeight() - 1; var7 > (var4 == null ? param0.getMinBuildHeight() : var4.getY()); var7--) {
                  BlockPos var8 = new BlockPos(param1.getX() + var5, var7, param1.getZ() + var6);
                  BlockState var9 = param0.getBlockState(var8);
                  if (var9.isCollisionShapeFullBlock(param0, var8) && (param3 || !var9.is(Blocks.BEDROCK))) {
                     var4 = var8;
                     break;
                  }
               }
            }
         }
      }

      return var4 == null ? param1 : var4;
   }

   private static LevelChunk getChunk(Level param0, Vec3 param1) {
      return param0.getChunk(Mth.floor(param1.x / 16.0), Mth.floor(param1.z / 16.0));
   }

   @Nullable
   private static BlockPos findValidSpawnInChunk(LevelChunk param0) {
      ChunkPos var1 = param0.getPos();
      BlockPos var2 = new BlockPos(var1.getMinBlockX(), 30, var1.getMinBlockZ());
      int var3 = param0.getHighestSectionPosition() + 16 - 1;
      BlockPos var4 = new BlockPos(var1.getMaxBlockX(), var3, var1.getMaxBlockZ());
      BlockPos var5 = null;
      double var6 = 0.0;

      for (BlockPos var9 : BlockPos.betweenClosed(var2, var4)) {
         BlockState var10 = param0.getBlockState(var9);
         BlockPos var11 = var9.above();
         BlockPos var12 = var9.above(2);
         if (var10.is(Blocks.END_STONE)
            && !param0.getBlockState(var11).isCollisionShapeFullBlock(param0, var11)
            && !param0.getBlockState(var12).isCollisionShapeFullBlock(param0, var12)) {
            double var13 = var9.distToCenterSqr(0.0, 0.0, 0.0);
            if (var5 == null || var13 < var6) {
               var5 = var9;
               var6 = var13;
            }
         }
      }

      return var5;
   }

   private static void spawnGatewayPortal(ServerLevel param0, BlockPos param1, EndGatewayConfiguration param2) {
      Feature.END_GATEWAY.place(param2, param0, param0.getChunkSource().getGenerator(), RandomSource.create(), param1);
   }

   @Override
   public boolean shouldRenderFace(Direction param0) {
      return Block.shouldRenderFace(this.getBlockState(), this.level, this.getBlockPos(), param0, this.getBlockPos().relative(param0));
   }

   public int getParticleAmount() {
      int var1 = 0;

      for (Direction var5 : Direction.values()) {
         var1 += this.shouldRenderFace(var5) ? 1 : 0;
      }

      return var1;
   }

   public void setExitPosition(BlockPos param0, boolean param1) {
      this.exactTeleport = param1;
      this.exitPortal = param0;
   }
}
