package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public abstract class BaseSpawner {
   public static final String SPAWN_DATA_TAG = "SpawnData";
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int EVENT_SPAWN = 1;
   private int spawnDelay = 20;
   private SimpleWeightedRandomList<SpawnData> spawnPotentials = SimpleWeightedRandomList.empty();
   @Nullable
   private SpawnData nextSpawnData;
   private double spin;
   private double oSpin;
   private int minSpawnDelay = 200;
   private int maxSpawnDelay = 800;
   private int spawnCount = 4;
   @Nullable
   private Entity displayEntity;
   private int maxNearbyEntities = 6;
   private int requiredPlayerRange = 16;
   private int spawnRange = 4;

   public void setEntityId(EntityType<?> param0, @Nullable Level param1, RandomSource param2, BlockPos param3) {
      this.getOrCreateNextSpawnData(param1, param2, param3).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(param0).toString());
   }

   private boolean isNearPlayer(Level param0, BlockPos param1) {
      return param0.hasNearbyAlivePlayer(param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5, this.requiredPlayerRange);
   }

   public void clientTick(Level param0, BlockPos param1) {
      if (!this.isNearPlayer(param0, param1)) {
         this.oSpin = this.spin;
      } else if (this.displayEntity != null) {
         RandomSource var3 = param0.getRandom();
         double var4 = param1.getX() + var3.nextDouble();
         double var6 = param1.getY() + var3.nextDouble();
         double var8 = param1.getZ() + var3.nextDouble();
         param0.addParticle(ParticleTypes.SMOKE, var4, var6, var8, 0.0, 0.0, 0.0);
         param0.addParticle(ParticleTypes.FLAME, var4, var6, var8, 0.0, 0.0, 0.0);
         if (this.spawnDelay > 0) {
            this.spawnDelay--;
         }

         this.oSpin = this.spin;
         this.spin = (this.spin + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0;
      }
   }

   public void serverTick(ServerLevel param0, BlockPos param1) {
      if (this.isNearPlayer(param0, param1)) {
         if (this.spawnDelay == -1) {
            this.delay(param0, param1);
         }

         if (this.spawnDelay > 0) {
            this.spawnDelay--;
         } else {
            boolean var3 = false;
            RandomSource var4 = param0.getRandom();
            SpawnData var5 = this.getOrCreateNextSpawnData(param0, var4, param1);

            for (int var6 = 0; var6 < this.spawnCount; var6++) {
               CompoundTag var7 = var5.getEntityToSpawn();
               Optional<EntityType<?>> var8 = EntityType.by(var7);
               if (var8.isEmpty()) {
                  this.delay(param0, param1);
                  return;
               }

               ListTag var9 = var7.getList("Pos", 6);
               int var10 = var9.size();
               double var11 = var10 >= 1 ? var9.getDouble(0) : param1.getX() + (var4.nextDouble() - var4.nextDouble()) * this.spawnRange + 0.5;
               double var13 = var10 >= 2 ? var9.getDouble(1) : param1.getY() + var4.nextInt(3) - 1;
               double var15 = var10 >= 3 ? var9.getDouble(2) : param1.getZ() + (var4.nextDouble() - var4.nextDouble()) * this.spawnRange + 0.5;
               if (param0.noCollision(var8.get().getAABB(var11, var13, var15))) {
                  BlockPos var17 = BlockPos.containing(var11, var13, var15);
                  if (var5.getCustomSpawnRules().isPresent()) {
                     if (!var8.get().getCategory().isFriendly() && param0.getDifficulty() == Difficulty.PEACEFUL) {
                        continue;
                     }

                     SpawnData.CustomSpawnRules var18 = var5.getCustomSpawnRules().get();
                     if (!var18.blockLightLimit().isValueInRange(param0.getBrightness(LightLayer.BLOCK, var17))
                        || !var18.skyLightLimit().isValueInRange(param0.getBrightness(LightLayer.SKY, var17))) {
                        continue;
                     }
                  } else if (!SpawnPlacements.checkSpawnRules(var8.get(), param0, MobSpawnType.SPAWNER, var17, param0.getRandom())) {
                     continue;
                  }

                  Entity var18 = EntityType.loadEntityRecursive(var7, param0, l_param6 -> {
                     l_param6.moveTo(var11, var13, var15, l_param6.getYRot(), l_param6.getXRot());
                     return l_param6;
                  });
                  if (var18 == null) {
                     this.delay(param0, param1);
                     return;
                  }

                  int var19 = param0.getEntitiesOfClass(
                        var18.getClass(),
                        new AABB(param1.getX(), param1.getY(), param1.getZ(), param1.getX() + 1, param1.getY() + 1, param1.getZ() + 1).inflate(this.spawnRange)
                     )
                     .size();
                  if (var19 >= this.maxNearbyEntities) {
                     this.delay(param0, param1);
                     return;
                  }

                  var18.moveTo(var18.getX(), var18.getY(), var18.getZ(), var4.nextFloat() * 360.0F, 0.0F);
                  if (var18 instanceof Mob var20) {
                     if (var5.getCustomSpawnRules().isEmpty() && !var20.checkSpawnRules(param0, MobSpawnType.SPAWNER) || !var20.checkSpawnObstruction(param0)) {
                        continue;
                     }

                     if (var5.getEntityToSpawn().size() == 1 && var5.getEntityToSpawn().contains("id", 8)) {
                        ((Mob)var18).finalizeSpawn(param0, param0.getCurrentDifficultyAt(var18.blockPosition()), MobSpawnType.SPAWNER, null, null);
                     }
                  }

                  if (!param0.tryAddFreshEntityWithPassengers(var18)) {
                     this.delay(param0, param1);
                     return;
                  }

                  param0.levelEvent(2004, param1, 0);
                  param0.gameEvent(var18, GameEvent.ENTITY_PLACE, var17);
                  if (var18 instanceof Mob) {
                     ((Mob)var18).spawnAnim();
                  }

                  var3 = true;
               }
            }

            if (var3) {
               this.delay(param0, param1);
            }
         }
      }
   }

   private void delay(Level param0, BlockPos param1) {
      RandomSource var3 = param0.random;
      if (this.maxSpawnDelay <= this.minSpawnDelay) {
         this.spawnDelay = this.minSpawnDelay;
      } else {
         this.spawnDelay = this.minSpawnDelay + var3.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
      }

      this.spawnPotentials.getRandom(var3).ifPresent(l_param2 -> this.setNextSpawnData(param0, param1, l_param2.getData()));
      this.broadcastEvent(param0, param1, 1);
   }

   public void load(@Nullable Level param0, BlockPos param1, CompoundTag param2) {
      this.spawnDelay = param2.getShort("Delay");
      boolean var4 = param2.contains("SpawnData", 10);
      if (var4) {
         SpawnData var5 = SpawnData.CODEC
            .parse(NbtOps.INSTANCE, param2.getCompound("SpawnData"))
            .resultOrPartial(l_param0 -> LOGGER.warn("Invalid SpawnData: {}", l_param0))
            .orElseGet(SpawnData::new);
         this.setNextSpawnData(param0, param1, var5);
      }

      boolean var5 = param2.contains("SpawnPotentials", 9);
      if (var5) {
         ListTag var6 = param2.getList("SpawnPotentials", 10);
         this.spawnPotentials = SpawnData.LIST_CODEC
            .parse(NbtOps.INSTANCE, var6)
            .resultOrPartial(l_param0 -> LOGGER.warn("Invalid SpawnPotentials list: {}", l_param0))
            .orElseGet(SimpleWeightedRandomList::empty);
      } else {
         this.spawnPotentials = SimpleWeightedRandomList.single(this.nextSpawnData != null ? this.nextSpawnData : new SpawnData());
      }

      if (param2.contains("MinSpawnDelay", 99)) {
         this.minSpawnDelay = param2.getShort("MinSpawnDelay");
         this.maxSpawnDelay = param2.getShort("MaxSpawnDelay");
         this.spawnCount = param2.getShort("SpawnCount");
      }

      if (param2.contains("MaxNearbyEntities", 99)) {
         this.maxNearbyEntities = param2.getShort("MaxNearbyEntities");
         this.requiredPlayerRange = param2.getShort("RequiredPlayerRange");
      }

      if (param2.contains("SpawnRange", 99)) {
         this.spawnRange = param2.getShort("SpawnRange");
      }

      this.displayEntity = null;
   }

   public CompoundTag save(CompoundTag param0) {
      param0.putShort("Delay", (short)this.spawnDelay);
      param0.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
      param0.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
      param0.putShort("SpawnCount", (short)this.spawnCount);
      param0.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
      param0.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
      param0.putShort("SpawnRange", (short)this.spawnRange);
      if (this.nextSpawnData != null) {
         param0.put(
            "SpawnData",
            SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, this.nextSpawnData).result().orElseThrow(() -> new IllegalStateException("Invalid SpawnData"))
         );
      }

      param0.put("SpawnPotentials", SpawnData.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.spawnPotentials).result().orElseThrow());
      return param0;
   }

   @Nullable
   public Entity getOrCreateDisplayEntity(Level param0, RandomSource param1, BlockPos param2) {
      if (this.displayEntity == null) {
         CompoundTag var4 = this.getOrCreateNextSpawnData(param0, param1, param2).getEntityToSpawn();
         if (!var4.contains("id", 8)) {
            return null;
         }

         this.displayEntity = EntityType.loadEntityRecursive(var4, param0, Function.identity());
         if (var4.size() == 1 && this.displayEntity instanceof Mob) {
         }
      }

      return this.displayEntity;
   }

   public boolean onEventTriggered(Level param0, int param1) {
      if (param1 == 1) {
         if (param0.isClientSide) {
            this.spawnDelay = this.minSpawnDelay;
         }

         return true;
      } else {
         return false;
      }
   }

   protected void setNextSpawnData(@Nullable Level param0, BlockPos param1, SpawnData param2) {
      this.nextSpawnData = param2;
   }

   private SpawnData getOrCreateNextSpawnData(@Nullable Level param0, RandomSource param1, BlockPos param2) {
      if (this.nextSpawnData != null) {
         return this.nextSpawnData;
      }

      this.setNextSpawnData(param0, param2, this.spawnPotentials.getRandom(param1).map(WeightedEntry.Wrapper::getData).orElseGet(SpawnData::new));
      return this.nextSpawnData;
   }

   public abstract void broadcastEvent(Level var1, BlockPos var2, int var3);

   public double getSpin() {
      return this.spin;
   }

   public double getoSpin() {
      return this.oSpin;
   }
}
