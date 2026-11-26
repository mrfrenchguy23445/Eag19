package net.minecraft.world.level.levelgen.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public class MonsterRoomFeature extends Feature<NoneFeatureConfiguration> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityType<?>[] MOBS = new EntityType[]{EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER};
   private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

   public MonsterRoomFeature(Codec<NoneFeatureConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> param0) {
      Predicate<BlockState> var2 = Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE);
      BlockPos var3 = param0.origin();
      RandomSource var4 = param0.random();
      WorldGenLevel var5 = param0.level();
      int var6 = 3;
      int var7 = var4.nextInt(2) + 2;
      int var8 = -var7 - 1;
      int var9 = var7 + 1;
      int var10 = -1;
      int var11 = 4;
      int var12 = var4.nextInt(2) + 2;
      int var13 = -var12 - 1;
      int var14 = var12 + 1;
      int var15 = 0;

      for (int var16 = var8; var16 <= var9; var16++) {
         for (int var17 = -1; var17 <= 4; var17++) {
            for (int var18 = var13; var18 <= var14; var18++) {
               BlockPos var19 = var3.offset(var16, var17, var18);
               Material var20 = var5.getBlockState(var19).getMaterial();
               boolean var21 = var20.isSolid();
               if (var17 == -1 && !var21) {
                  return false;
               }

               if (var17 == 4 && !var21) {
                  return false;
               }

               if ((var16 == var8 || var16 == var9 || var18 == var13 || var18 == var14)
                  && var17 == 0
                  && var5.isEmptyBlock(var19)
                  && var5.isEmptyBlock(var19.above())) {
                  var15++;
               }
            }
         }
      }

      if (var15 >= 1 && var15 <= 5) {
         for (int var16 = var8; var16 <= var9; var16++) {
            for (int var17 = 3; var17 >= -1; var17--) {
               for (int var18 = var13; var18 <= var14; var18++) {
                  BlockPos var19 = var3.offset(var16, var17, var18);
                  BlockState var20 = var5.getBlockState(var19);
                  if (var16 == var8 || var17 == -1 || var18 == var13 || var16 == var9 || var17 == 4 || var18 == var14) {
                     if (var19.getY() >= var5.getMinBuildHeight() && !var5.getBlockState(var19.below()).getMaterial().isSolid()) {
                        var5.setBlock(var19, AIR, 2);
                     } else if (var20.getMaterial().isSolid() && !var20.is(Blocks.CHEST)) {
                        if (var17 == -1 && var4.nextInt(4) != 0) {
                           this.safeSetBlock(var5, var19, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), var2);
                        } else {
                           this.safeSetBlock(var5, var19, Blocks.COBBLESTONE.defaultBlockState(), var2);
                        }
                     }
                  } else if (!var20.is(Blocks.CHEST) && !var20.is(Blocks.SPAWNER)) {
                     this.safeSetBlock(var5, var19, AIR, var2);
                  }
               }
            }
         }

         for (int var16 = 0; var16 < 2; var16++) {
            for (int var17 = 0; var17 < 3; var17++) {
               int var18 = var3.getX() + var4.nextInt(var7 * 2 + 1) - var7;
               int var19 = var3.getY();
               int var20 = var3.getZ() + var4.nextInt(var12 * 2 + 1) - var12;
               BlockPos var21 = new BlockPos(var18, var19, var20);
               if (var5.isEmptyBlock(var21)) {
                  int var22 = 0;

                  for (Direction var24 : Direction.Plane.HORIZONTAL) {
                     if (var5.getBlockState(var21.relative(var24)).getMaterial().isSolid()) {
                        var22++;
                     }
                  }

                  if (var22 == 1) {
                     this.safeSetBlock(var5, var21, StructurePiece.reorient(var5, var21, Blocks.CHEST.defaultBlockState()), var2);
                     RandomizableContainerBlockEntity.setLootTable(var5, var4, var21, BuiltInLootTables.SIMPLE_DUNGEON);
                     break;
                  }
               }
            }
         }

         this.safeSetBlock(var5, var3, Blocks.SPAWNER.defaultBlockState(), var2);
         if (var5.getBlockEntity(var3) instanceof SpawnerBlockEntity var17) {
            var17.setEntityId(this.randomEntityId(var4), var4);
         } else {
            LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", var3.getX(), var3.getY(), var3.getZ());
         }

         return true;
      } else {
         return false;
      }
   }

   private EntityType<?> randomEntityId(RandomSource param0) {
      return Util.getRandom(MOBS, param0);
   }
}
