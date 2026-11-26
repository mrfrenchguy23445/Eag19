package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class StructureTemplate {
   public static final String PALETTE_TAG = "palette";
   public static final String PALETTE_LIST_TAG = "palettes";
   public static final String ENTITIES_TAG = "entities";
   public static final String BLOCKS_TAG = "blocks";
   public static final String BLOCK_TAG_POS = "pos";
   public static final String BLOCK_TAG_STATE = "state";
   public static final String BLOCK_TAG_NBT = "nbt";
   public static final String ENTITY_TAG_POS = "pos";
   public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
   public static final String ENTITY_TAG_NBT = "nbt";
   public static final String SIZE_TAG = "size";
   private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
   private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
   private Vec3i size = Vec3i.ZERO;
   private String author = "?";

   public Vec3i getSize() {
      return this.size;
   }

   public void setAuthor(String param0) {
      this.author = param0;
   }

   public String getAuthor() {
      return this.author;
   }

   public void fillFromWorld(Level param0, BlockPos param1, Vec3i param2, boolean param3, @Nullable Block param4) {
      if (param2.getX() >= 1 && param2.getY() >= 1 && param2.getZ() >= 1) {
         BlockPos var6 = param1.offset(param2).offset(-1, -1, -1);
         List<StructureTemplate.StructureBlockInfo> var7 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> var8 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> var9 = Lists.newArrayList();
         BlockPos var10 = new BlockPos(Math.min(param1.getX(), var6.getX()), Math.min(param1.getY(), var6.getY()), Math.min(param1.getZ(), var6.getZ()));
         BlockPos var11 = new BlockPos(Math.max(param1.getX(), var6.getX()), Math.max(param1.getY(), var6.getY()), Math.max(param1.getZ(), var6.getZ()));
         this.size = param2;

         for (BlockPos var13 : BlockPos.betweenClosed(var10, var11)) {
            BlockPos var14 = var13.subtract(var10);
            BlockState var15 = param0.getBlockState(var13);
            if (param4 == null || !var15.is(param4)) {
               BlockEntity var16 = param0.getBlockEntity(var13);
               StructureTemplate.StructureBlockInfo var17;
               if (var16 != null) {
                  var17 = new StructureTemplate.StructureBlockInfo(var14, var15, var16.saveWithId());
               } else {
                  var17 = new StructureTemplate.StructureBlockInfo(var14, var15, null);
               }

               addToLists(var17, var7, var8, var9);
            }
         }

         List<StructureTemplate.StructureBlockInfo> var12 = buildInfoList(var7, var8, var9);
         this.palettes.clear();
         this.palettes.add(new StructureTemplate.Palette(var12));
         if (param3) {
            this.fillEntityList(param0, var10, var11.offset(1, 1, 1));
         } else {
            this.entityInfoList.clear();
         }
      }
   }

   private static void addToLists(
      StructureTemplate.StructureBlockInfo param0,
      List<StructureTemplate.StructureBlockInfo> param1,
      List<StructureTemplate.StructureBlockInfo> param2,
      List<StructureTemplate.StructureBlockInfo> param3
   ) {
      if (param0.nbt != null) {
         param2.add(param0);
      } else if (!param0.state.getBlock().hasDynamicShape() && param0.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
         param1.add(param0);
      } else {
         param3.add(param0);
      }
   }

   private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
      List<StructureTemplate.StructureBlockInfo> param0, List<StructureTemplate.StructureBlockInfo> param1, List<StructureTemplate.StructureBlockInfo> param2
   ) {
      Comparator<StructureTemplate.StructureBlockInfo> var3 = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(l_param0 -> l_param0.pos.getY())
         .thenComparingInt(l_param0 -> l_param0.pos.getX())
         .thenComparingInt(l_param0 -> l_param0.pos.getZ());
      param0.sort(var3);
      param2.sort(var3);
      param1.sort(var3);
      List<StructureTemplate.StructureBlockInfo> var4 = Lists.newArrayList();
      var4.addAll(param0);
      var4.addAll(param2);
      var4.addAll(param1);
      return var4;
   }

   private void fillEntityList(Level param0, BlockPos param1, BlockPos param2) {
      List<Entity> var4 = param0.getEntitiesOfClass(Entity.class, new AABB(param1, param2), l_param0 -> !(l_param0 instanceof Player));
      this.entityInfoList.clear();

      for (Entity var6 : var4) {
         Vec3 var7 = new Vec3(var6.getX() - param1.getX(), var6.getY() - param1.getY(), var6.getZ() - param1.getZ());
         CompoundTag var8 = new CompoundTag();
         var6.save(var8);
         BlockPos var9;
         if (var6 instanceof Painting) {
            var9 = ((Painting)var6).getPos().subtract(param1);
         } else {
            var9 = BlockPos.containing(var7);
         }

         this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(var7, var9, var8.copy()));
      }
   }

   public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos param0, StructurePlaceSettings param1, Block param2) {
      return this.filterBlocks(param0, param1, param2, true);
   }

   public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos param0, StructurePlaceSettings param1, Block param2, boolean param3) {
      ObjectArrayList<StructureTemplate.StructureBlockInfo> var5 = new ObjectArrayList<>();
      BoundingBox var6 = param1.getBoundingBox();
      if (this.palettes.isEmpty()) {
         return var5;
      }

      for (StructureTemplate.StructureBlockInfo var8 : param1.getRandomPalette(this.palettes, param0).blocks(param2)) {
         BlockPos var9 = param3 ? calculateRelativePosition(param1, var8.pos).offset(param0) : var8.pos;
         if (var6 == null || var6.isInside(var9)) {
            var5.add(new StructureTemplate.StructureBlockInfo(var9, var8.state.rotate(param1.getRotation()), var8.nbt));
         }
      }

      return var5;
   }

   public BlockPos calculateConnectedPosition(StructurePlaceSettings param0, BlockPos param1, StructurePlaceSettings param2, BlockPos param3) {
      BlockPos var5 = calculateRelativePosition(param0, param1);
      BlockPos var6 = calculateRelativePosition(param2, param3);
      return var5.subtract(var6);
   }

   public static BlockPos calculateRelativePosition(StructurePlaceSettings param0, BlockPos param1) {
      return transform(param1, param0.getMirror(), param0.getRotation(), param0.getRotationPivot());
   }

   public boolean placeInWorld(ServerLevelAccessor param0, BlockPos param1, BlockPos param2, StructurePlaceSettings param3, RandomSource param4, int param5) {
      if (this.palettes.isEmpty()) {
         return false;
      }

      List<StructureTemplate.StructureBlockInfo> var7 = param3.getRandomPalette(this.palettes, param1).blocks();
      if ((!var7.isEmpty() || !param3.isIgnoreEntities() && !this.entityInfoList.isEmpty())
         && this.size.getX() >= 1
         && this.size.getY() >= 1
         && this.size.getZ() >= 1) {
         BoundingBox var8 = param3.getBoundingBox();
         List<BlockPos> var9 = Lists.newArrayListWithCapacity(param3.shouldKeepLiquids() ? var7.size() : 0);
         List<BlockPos> var10 = Lists.newArrayListWithCapacity(param3.shouldKeepLiquids() ? var7.size() : 0);
         List<Pair<BlockPos, CompoundTag>> var11 = Lists.newArrayListWithCapacity(var7.size());
         int var12 = Integer.MAX_VALUE;
         int var13 = Integer.MAX_VALUE;
         int var14 = Integer.MAX_VALUE;
         int var15 = Integer.MIN_VALUE;
         int var16 = Integer.MIN_VALUE;
         int var17 = Integer.MIN_VALUE;

         for (StructureTemplate.StructureBlockInfo var20 : processBlockInfos(param0, param1, param2, param3, var7)) {
            BlockPos var21 = var20.pos;
            if (var8 == null || var8.isInside(var21)) {
               FluidState var22 = param3.shouldKeepLiquids() ? param0.getFluidState(var21) : null;
               BlockState var23 = var20.state.mirror(param3.getMirror()).rotate(param3.getRotation());
               if (var20.nbt != null) {
                  BlockEntity var24 = param0.getBlockEntity(var21);
                  Clearable.tryClear(var24);
                  param0.setBlock(var21, Blocks.BARRIER.defaultBlockState(), 20);
               }

               if (param0.setBlock(var21, var23, param5)) {
                  var12 = Math.min(var12, var21.getX());
                  var13 = Math.min(var13, var21.getY());
                  var14 = Math.min(var14, var21.getZ());
                  var15 = Math.max(var15, var21.getX());
                  var16 = Math.max(var16, var21.getY());
                  var17 = Math.max(var17, var21.getZ());
                  var11.add(Pair.of(var21, var20.nbt));
                  if (var20.nbt != null) {
                     BlockEntity var24 = param0.getBlockEntity(var21);
                     if (var24 != null) {
                        if (var24 instanceof RandomizableContainerBlockEntity) {
                           var20.nbt.putLong("LootTableSeed", param4.nextLong());
                        }

                        var24.load(var20.nbt);
                     }
                  }

                  if (var22 != null) {
                     if (var23.getFluidState().isSource()) {
                        var10.add(var21);
                     } else if (var23.getBlock() instanceof LiquidBlockContainer) {
                        ((LiquidBlockContainer)var23.getBlock()).placeLiquid(param0, var21, var23, var22);
                        if (!var22.isSource()) {
                           var9.add(var21);
                        }
                     }
                  }
               }
            }
         }

         boolean var19 = true;
         Direction[] var20 = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

         while (var19 && !var9.isEmpty()) {
            var19 = false;
            Iterator<BlockPos> var21 = var9.iterator();

            while (var21.hasNext()) {
               BlockPos var22 = var21.next();
               FluidState var23 = param0.getFluidState(var22);

               for (int var24 = 0; var24 < var20.length && !var23.isSource(); var24++) {
                  BlockPos var25 = var22.relative(var20[var24]);
                  FluidState var26 = param0.getFluidState(var25);
                  if (var26.isSource() && !var10.contains(var25)) {
                     var23 = var26;
                  }
               }

               if (var23.isSource()) {
                  BlockState var24 = param0.getBlockState(var22);
                  Block var25 = var24.getBlock();
                  if (var25 instanceof LiquidBlockContainer) {
                     ((LiquidBlockContainer)var25).placeLiquid(param0, var22, var24, var23);
                     var19 = true;
                     var21.remove();
                  }
               }
            }
         }

         if (var12 <= var15) {
            if (!param3.getKnownShape()) {
               DiscreteVoxelShape var21 = new BitSetDiscreteVoxelShape(var15 - var12 + 1, var16 - var13 + 1, var17 - var14 + 1);
               int var22 = var12;
               int var23 = var13;
               int var24 = var14;

               for (Pair<BlockPos, CompoundTag> var26 : var11) {
                  BlockPos var27 = var26.getFirst();
                  var21.fill(var27.getX() - var22, var27.getY() - var23, var27.getZ() - var24);
               }

               updateShapeAtEdge(param0, param5, var21, var22, var23, var24);
            }

            for (Pair<BlockPos, CompoundTag> var22 : var11) {
               BlockPos var23 = var22.getFirst();
               if (!param3.getKnownShape()) {
                  BlockState var24 = param0.getBlockState(var23);
                  BlockState var25 = Block.updateFromNeighbourShapes(var24, param0, var23);
                  if (var24 != var25) {
                     param0.setBlock(var23, var25, param5 & -2 | 16);
                  }

                  param0.blockUpdated(var23, var25.getBlock());
               }

               if (var22.getSecond() != null) {
                  BlockEntity var24 = param0.getBlockEntity(var23);
                  if (var24 != null) {
                     var24.setChanged();
                  }
               }
            }
         }

         if (!param3.isIgnoreEntities()) {
            this.placeEntities(param0, param1, param3.getMirror(), param3.getRotation(), param3.getRotationPivot(), var8, param3.shouldFinalizeEntities());
         }

         return true;
      } else {
         return false;
      }
   }

   public static void updateShapeAtEdge(LevelAccessor param0, int param1, DiscreteVoxelShape param2, int param3, int param4, int param5) {
      param2.forAllFaces((l_param5, l_param6, l_param7, l_param8) -> {
         BlockPos l_var9 = new BlockPos(param3 + l_param6, param4 + l_param7, param5 + l_param8);
         BlockPos l_var10 = l_var9.relative(l_param5);
         BlockState l_var11 = param0.getBlockState(l_var9);
         BlockState l_var12 = param0.getBlockState(l_var10);
         BlockState l_var13 = l_var11.updateShape(l_param5, l_var12, param0, l_var9, l_var10);
         if (l_var11 != l_var13) {
            param0.setBlock(l_var9, l_var13, param1 & -2);
         }

         BlockState l_var14 = l_var12.updateShape(l_param5.getOpposite(), l_var13, param0, l_var10, l_var9);
         if (l_var12 != l_var14) {
            param0.setBlock(l_var10, l_var14, param1 & -2);
         }
      });
   }

   public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
      LevelAccessor param0, BlockPos param1, BlockPos param2, StructurePlaceSettings param3, List<StructureTemplate.StructureBlockInfo> param4
   ) {
      List<StructureTemplate.StructureBlockInfo> var5 = Lists.newArrayList();

      for (StructureTemplate.StructureBlockInfo var7 : param4) {
         BlockPos var8 = calculateRelativePosition(param3, var7.pos).offset(param1);
         StructureTemplate.StructureBlockInfo var9 = new StructureTemplate.StructureBlockInfo(var8, var7.state, var7.nbt != null ? var7.nbt.copy() : null);
         Iterator<StructureProcessor> var10 = param3.getProcessors().iterator();

         while (var9 != null && var10.hasNext()) {
            var9 = var10.next().processBlock(param0, param1, param2, var7, var9, param3);
         }

         if (var9 != null) {
            var5.add(var9);
         }
      }

      for (StructureProcessor var7 : param3.getProcessors()) {
         var7.finalizeStructure(param0, param1, param2, param3, var5);
      }

      return var5;
   }

   private void placeEntities(
      ServerLevelAccessor param0, BlockPos param1, Mirror param2, Rotation param3, BlockPos param4, @Nullable BoundingBox param5, boolean param6
   ) {
      for (StructureTemplate.StructureEntityInfo var9 : this.entityInfoList) {
         BlockPos var10 = transform(var9.blockPos, param2, param3, param4).offset(param1);
         if (param5 == null || param5.isInside(var10)) {
            CompoundTag var11 = var9.nbt.copy();
            Vec3 var12 = transform(var9.pos, param2, param3, param4);
            Vec3 var13 = var12.add(param1.getX(), param1.getY(), param1.getZ());
            ListTag var14 = new ListTag();
            var14.add(DoubleTag.valueOf(var13.x));
            var14.add(DoubleTag.valueOf(var13.y));
            var14.add(DoubleTag.valueOf(var13.z));
            var11.put("Pos", var14);
            var11.remove("UUID");
            createEntityIgnoreException(param0, var11).ifPresent(l_param6 -> {
               float l_var7 = l_param6.rotate(param3);
               l_var7 += l_param6.mirror(param2) - l_param6.getYRot();
               l_param6.moveTo(var13.x, var13.y, var13.z, l_var7, l_param6.getXRot());
               if (param6 && l_param6 instanceof Mob) {
                  ((Mob)l_param6).finalizeSpawn(param0, param0.getCurrentDifficultyAt(BlockPos.containing(var13)), MobSpawnType.STRUCTURE, null, var11);
               }

               param0.addFreshEntityWithPassengers(l_param6);
            });
         }
      }
   }

   private static Optional<Entity> createEntityIgnoreException(ServerLevelAccessor param0, CompoundTag param1) {
      try {
         return EntityType.create(param1, param0.getLevel());
      } catch (Exception var2) {
         return Optional.empty();
      }
   }

   public Vec3i getSize(Rotation param0) {
      switch (param0) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
         default:
            return this.size;
      }
   }

   public static BlockPos transform(BlockPos param0, Mirror param1, Rotation param2, BlockPos param3) {
      int var4 = param0.getX();
      int var5 = param0.getY();
      int var6 = param0.getZ();
      boolean var7 = true;
      switch (param1) {
         case LEFT_RIGHT:
            var6 = -var6;
            break;
         case FRONT_BACK:
            var4 = -var4;
            break;
         default:
            var7 = false;
      }

      int var8 = param3.getX();
      int var9 = param3.getZ();
      switch (param2) {
         case COUNTERCLOCKWISE_90:
            return new BlockPos(var8 - var9 + var6, var5, var8 + var9 - var4);
         case CLOCKWISE_90:
            return new BlockPos(var8 + var9 - var6, var5, var9 - var8 + var4);
         case CLOCKWISE_180:
            return new BlockPos(var8 + var8 - var4, var5, var9 + var9 - var6);
         default:
            return var7 ? new BlockPos(var4, var5, var6) : param0;
      }
   }

   public static Vec3 transform(Vec3 param0, Mirror param1, Rotation param2, BlockPos param3) {
      double var4 = param0.x;
      double var6 = param0.y;
      double var8 = param0.z;
      boolean var10 = true;
      switch (param1) {
         case LEFT_RIGHT:
            var8 = 1.0 - var8;
            break;
         case FRONT_BACK:
            var4 = 1.0 - var4;
            break;
         default:
            var10 = false;
      }

      int var11 = param3.getX();
      int var12 = param3.getZ();
      switch (param2) {
         case COUNTERCLOCKWISE_90:
            return new Vec3(var11 - var12 + var8, var6, var11 + var12 + 1 - var4);
         case CLOCKWISE_90:
            return new Vec3(var11 + var12 + 1 - var8, var6, var12 - var11 + var4);
         case CLOCKWISE_180:
            return new Vec3(var11 + var11 + 1 - var4, var6, var12 + var12 + 1 - var8);
         default:
            return var10 ? new Vec3(var4, var6, var8) : param0;
      }
   }

   public BlockPos getZeroPositionWithTransform(BlockPos param0, Mirror param1, Rotation param2) {
      return getZeroPositionWithTransform(param0, param1, param2, this.getSize().getX(), this.getSize().getZ());
   }

   public static BlockPos getZeroPositionWithTransform(BlockPos param0, Mirror param1, Rotation param2, int param3, int param4) {
      param3--;
      param4--;
      int var5 = param1 == Mirror.FRONT_BACK ? param3 : 0;
      int var6 = param1 == Mirror.LEFT_RIGHT ? param4 : 0;
      BlockPos var7 = param0;
      switch (param2) {
         case COUNTERCLOCKWISE_90:
            var7 = param0.offset(var6, 0, param3 - var5);
            break;
         case CLOCKWISE_90:
            var7 = param0.offset(param4 - var6, 0, var5);
            break;
         case CLOCKWISE_180:
            var7 = param0.offset(param3 - var5, 0, param4 - var6);
            break;
         case NONE:
            var7 = param0.offset(var5, 0, var6);
      }

      return var7;
   }

   public BoundingBox getBoundingBox(StructurePlaceSettings param0, BlockPos param1) {
      return this.getBoundingBox(param1, param0.getRotation(), param0.getRotationPivot(), param0.getMirror());
   }

   public BoundingBox getBoundingBox(BlockPos param0, Rotation param1, BlockPos param2, Mirror param3) {
      return getBoundingBox(param0, param1, param2, param3, this.size);
   }

   @VisibleForTesting
   protected static BoundingBox getBoundingBox(BlockPos param0, Rotation param1, BlockPos param2, Mirror param3, Vec3i param4) {
      Vec3i var5 = param4.offset(-1, -1, -1);
      BlockPos var6 = transform(BlockPos.ZERO, param3, param1, param2);
      BlockPos var7 = transform(BlockPos.ZERO.offset(var5), param3, param1, param2);
      return BoundingBox.fromCorners(var6, var7).move(param0);
   }

   public CompoundTag save(CompoundTag param0) {
      if (this.palettes.isEmpty()) {
         param0.put("blocks", new ListTag());
         param0.put("palette", new ListTag());
      } else {
         List<StructureTemplate.SimplePalette> var2 = Lists.newArrayList();
         StructureTemplate.SimplePalette var3 = new StructureTemplate.SimplePalette();
         var2.add(var3);

         for (int var4 = 1; var4 < this.palettes.size(); var4++) {
            var2.add(new StructureTemplate.SimplePalette());
         }

         ListTag var4 = new ListTag();
         List<StructureTemplate.StructureBlockInfo> var5 = this.palettes.get(0).blocks();

         for (int var6 = 0; var6 < var5.size(); var6++) {
            StructureTemplate.StructureBlockInfo var7 = var5.get(var6);
            CompoundTag var8 = new CompoundTag();
            var8.put("pos", this.newIntegerList(var7.pos.getX(), var7.pos.getY(), var7.pos.getZ()));
            int var9 = var3.idFor(var7.state);
            var8.putInt("state", var9);
            if (var7.nbt != null) {
               var8.put("nbt", var7.nbt);
            }

            var4.add(var8);

            for (int var10 = 1; var10 < this.palettes.size(); var10++) {
               StructureTemplate.SimplePalette var11 = var2.get(var10);
               var11.addMapping(this.palettes.get(var10).blocks().get(var6).state, var9);
            }
         }

         param0.put("blocks", var4);
         if (var2.size() == 1) {
            ListTag var6 = new ListTag();

            for (BlockState var8 : var3) {
               var6.add(NbtUtils.writeBlockState(var8));
            }

            param0.put("palette", var6);
         } else {
            ListTag var6 = new ListTag();

            for (StructureTemplate.SimplePalette var8 : var2) {
               ListTag var9 = new ListTag();

               for (BlockState var11 : var8) {
                  var9.add(NbtUtils.writeBlockState(var11));
               }

               var6.add(var9);
            }

            param0.put("palettes", var6);
         }
      }

      ListTag var2 = new ListTag();

      for (StructureTemplate.StructureEntityInfo var4 : this.entityInfoList) {
         CompoundTag var5 = new CompoundTag();
         var5.put("pos", this.newDoubleList(var4.pos.x, var4.pos.y, var4.pos.z));
         var5.put("blockPos", this.newIntegerList(var4.blockPos.getX(), var4.blockPos.getY(), var4.blockPos.getZ()));
         if (var4.nbt != null) {
            var5.put("nbt", var4.nbt);
         }

         var2.add(var5);
      }

      param0.put("entities", var2);
      param0.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
      return NbtUtils.addCurrentDataVersion(param0);
   }

   public void load(HolderGetter<Block> param0, CompoundTag param1) {
      this.palettes.clear();
      this.entityInfoList.clear();
      ListTag var3 = param1.getList("size", 3);
      this.size = new Vec3i(var3.getInt(0), var3.getInt(1), var3.getInt(2));
      ListTag var4 = param1.getList("blocks", 10);
      if (param1.contains("palettes", 9)) {
         ListTag var5 = param1.getList("palettes", 9);

         for (int var6 = 0; var6 < var5.size(); var6++) {
            this.loadPalette(param0, var5.getList(var6), var4);
         }
      } else {
         this.loadPalette(param0, param1.getList("palette", 10), var4);
      }

      ListTag var5 = param1.getList("entities", 10);

      for (int var6 = 0; var6 < var5.size(); var6++) {
         CompoundTag var7 = var5.getCompound(var6);
         ListTag var8 = var7.getList("pos", 6);
         Vec3 var9 = new Vec3(var8.getDouble(0), var8.getDouble(1), var8.getDouble(2));
         ListTag var10 = var7.getList("blockPos", 3);
         BlockPos var11 = new BlockPos(var10.getInt(0), var10.getInt(1), var10.getInt(2));
         if (var7.contains("nbt")) {
            CompoundTag var12 = var7.getCompound("nbt");
            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(var9, var11, var12));
         }
      }
   }

   private void loadPalette(HolderGetter<Block> param0, ListTag param1, ListTag param2) {
      StructureTemplate.SimplePalette var4 = new StructureTemplate.SimplePalette();

      for (int var5 = 0; var5 < param1.size(); var5++) {
         var4.addMapping(NbtUtils.readBlockState(param0, param1.getCompound(var5)), var5);
      }

      List<StructureTemplate.StructureBlockInfo> var5 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> var6 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> var7 = Lists.newArrayList();

      for (int var8 = 0; var8 < param2.size(); var8++) {
         CompoundTag var9 = param2.getCompound(var8);
         ListTag var10 = var9.getList("pos", 3);
         BlockPos var11 = new BlockPos(var10.getInt(0), var10.getInt(1), var10.getInt(2));
         BlockState var12 = var4.stateFor(var9.getInt("state"));
         CompoundTag var13;
         if (var9.contains("nbt")) {
            var13 = var9.getCompound("nbt");
         } else {
            var13 = null;
         }

         StructureTemplate.StructureBlockInfo var14 = new StructureTemplate.StructureBlockInfo(var11, var12, var13);
         addToLists(var14, var5, var6, var7);
      }

      List<StructureTemplate.StructureBlockInfo> var8 = buildInfoList(var5, var6, var7);
      this.palettes.add(new StructureTemplate.Palette(var8));
   }

   private ListTag newIntegerList(int... param0) {
      ListTag var2 = new ListTag();

      for (int var6 : param0) {
         var2.add(IntTag.valueOf(var6));
      }

      return var2;
   }

   private ListTag newDoubleList(double... param0) {
      ListTag var2 = new ListTag();

      for (double var6 : param0) {
         var2.add(DoubleTag.valueOf(var6));
      }

      return var2;
   }

   public static final class Palette {
      private final List<StructureTemplate.StructureBlockInfo> blocks;
      private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();

      Palette(List<StructureTemplate.StructureBlockInfo> param0) {
         this.blocks = param0;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks() {
         return this.blocks;
      }

      public List<StructureTemplate.StructureBlockInfo> blocks(Block param0) {
         return this.cache
            .computeIfAbsent(param0, l_param0 -> this.blocks.stream().filter(l2_param1 -> l2_param1.state.is(l_param0)).collect(Collectors.toList()));
      }
   }

   static class SimplePalette implements Iterable<BlockState> {
      public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
      private final IdMapper<BlockState> ids = new IdMapper<>(16);
      private int lastId;

      public int idFor(BlockState param0) {
         int var2 = this.ids.getId(param0);
         if (var2 == -1) {
            var2 = this.lastId++;
            this.ids.addMapping(param0, var2);
         }

         return var2;
      }

      @Nullable
      public BlockState stateFor(int param0) {
         BlockState var2 = this.ids.byId(param0);
         return var2 == null ? DEFAULT_BLOCK_STATE : var2;
      }

      @Override
      public Iterator<BlockState> iterator() {
         return this.ids.iterator();
      }

      public void addMapping(BlockState param0, int param1) {
         this.ids.addMapping(param0, param1);
      }
   }

   public static class StructureBlockInfo {
      public final BlockPos pos;
      public final BlockState state;
      public final CompoundTag nbt;

      public StructureBlockInfo(BlockPos param0, BlockState param1, @Nullable CompoundTag param2) {
         this.pos = param0;
         this.state = param1;
         this.nbt = param2;
      }

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
      }
   }

   public static class StructureEntityInfo {
      public final Vec3 pos;
      public final BlockPos blockPos;
      public final CompoundTag nbt;

      public StructureEntityInfo(Vec3 param0, BlockPos param1, CompoundTag param2) {
         this.pos = param0;
         this.blockPos = param1;
         this.nbt = param2;
      }
   }
}
