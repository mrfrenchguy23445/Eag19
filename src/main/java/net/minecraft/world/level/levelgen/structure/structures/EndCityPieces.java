package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class EndCityPieces {
   private static final int MAX_GEN_DEPTH = 8;
   static final EndCityPieces.SectionGenerator HOUSE_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager param0, int param1, EndCityPieces.EndCityPiece param2, BlockPos param3, List<StructurePiece> param4, RandomSource param5
      ) {
         if (param1 > 8) {
            return false;
         }

         Rotation var7 = param2.placeSettings().getRotation();
         EndCityPieces.EndCityPiece var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, param2, param3, "base_floor", var7, true));
         int var9 = param5.nextInt(3);
         if (var9 == 0) {
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 4, -1), "base_roof", var7, true));
         } else if (var9 == 1) {
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 0, -1), "second_floor_2", var7, false));
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 8, -1), "second_roof", var7, false));
            EndCityPieces.recursiveChildren(param0, EndCityPieces.TOWER_GENERATOR, param1 + 1, var8, null, param4, param5);
         } else if (var9 == 2) {
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 0, -1), "second_floor_2", var7, false));
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 4, -1), "third_floor_2", var7, false));
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 8, -1), "third_roof", var7, true));
            EndCityPieces.recursiveChildren(param0, EndCityPieces.TOWER_GENERATOR, param1 + 1, var8, null, param4, param5);
         }

         return true;
      }
   };
   static final List<Tuple<Rotation, BlockPos>> TOWER_BRIDGES = Lists.newArrayList(
      new Tuple<>(Rotation.NONE, new BlockPos(1, -1, 0)),
      new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(6, -1, 1)),
      new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 5)),
      new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(5, -1, 6))
   );
   static final EndCityPieces.SectionGenerator TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager param0, int param1, EndCityPieces.EndCityPiece param2, BlockPos param3, List<StructurePiece> param4, RandomSource param5
      ) {
         Rotation var7 = param2.placeSettings().getRotation();
         EndCityPieces.EndCityPiece var8 = param2;
         var8 = EndCityPieces.addHelper(
            param4, EndCityPieces.addPiece(param0, var8, new BlockPos(3 + param5.nextInt(2), -3, 3 + param5.nextInt(2)), "tower_base", var7, true)
         );
         var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(0, 7, 0), "tower_piece", var7, true));
         EndCityPieces.EndCityPiece var9 = param5.nextInt(3) == 0 ? var8 : null;
         int var10 = 1 + param5.nextInt(3);

         for (int var11 = 0; var11 < var10; var11++) {
            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(0, 4, 0), "tower_piece", var7, true));
            if (var11 < var10 - 1 && param5.nextBoolean()) {
               var9 = var8;
            }
         }

         if (var9 != null) {
            for (Tuple<Rotation, BlockPos> var12 : EndCityPieces.TOWER_BRIDGES) {
               if (param5.nextBoolean()) {
                  EndCityPieces.EndCityPiece var13 = EndCityPieces.addHelper(
                     param4, EndCityPieces.addPiece(param0, var9, var12.getB(), "bridge_end", var7.getRotated(var12.getA()), true)
                  );
                  EndCityPieces.recursiveChildren(param0, EndCityPieces.TOWER_BRIDGE_GENERATOR, param1 + 1, var13, null, param4, param5);
               }
            }

            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 4, -1), "tower_top", var7, true));
         } else {
            if (param1 != 7) {
               return EndCityPieces.recursiveChildren(param0, EndCityPieces.FAT_TOWER_GENERATOR, param1 + 1, var8, null, param4, param5);
            }

            var8 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var8, new BlockPos(-1, 4, -1), "tower_top", var7, true));
         }

         return true;
      }
   };
   static final EndCityPieces.SectionGenerator TOWER_BRIDGE_GENERATOR = new EndCityPieces.SectionGenerator() {
      public boolean shipCreated;

      @Override
      public void init() {
         this.shipCreated = false;
      }

      @Override
      public boolean generate(
         StructureTemplateManager param0, int param1, EndCityPieces.EndCityPiece param2, BlockPos param3, List<StructurePiece> param4, RandomSource param5
      ) {
         Rotation var7 = param2.placeSettings().getRotation();
         int var8 = param5.nextInt(4) + 1;
         EndCityPieces.EndCityPiece var9 = EndCityPieces.addHelper(
            param4, EndCityPieces.addPiece(param0, param2, new BlockPos(0, 0, -4), "bridge_piece", var7, true)
         );
         var9.setGenDepth(-1);
         int var10 = 0;

         for (int var11 = 0; var11 < var8; var11++) {
            if (param5.nextBoolean()) {
               var9 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var9, new BlockPos(0, var10, -4), "bridge_piece", var7, true));
               var10 = 0;
            } else {
               if (param5.nextBoolean()) {
                  var9 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var9, new BlockPos(0, var10, -4), "bridge_steep_stairs", var7, true));
               } else {
                  var9 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var9, new BlockPos(0, var10, -8), "bridge_gentle_stairs", var7, true));
               }

               var10 = 4;
            }
         }

         if (!this.shipCreated && param5.nextInt(10 - param1) == 0) {
            EndCityPieces.addHelper(
               param4, EndCityPieces.addPiece(param0, var9, new BlockPos(-8 + param5.nextInt(8), var10, -70 + param5.nextInt(10)), "ship", var7, true)
            );
            this.shipCreated = true;
         } else if (!EndCityPieces.recursiveChildren(
            param0, EndCityPieces.HOUSE_TOWER_GENERATOR, param1 + 1, var9, new BlockPos(-3, var10 + 1, -11), param4, param5
         )) {
            return false;
         }

         var9 = EndCityPieces.addHelper(
            param4, EndCityPieces.addPiece(param0, var9, new BlockPos(4, var10, 0), "bridge_end", var7.getRotated(Rotation.CLOCKWISE_180), true)
         );
         var9.setGenDepth(-1);
         return true;
      }
   };
   static final List<Tuple<Rotation, BlockPos>> FAT_TOWER_BRIDGES = Lists.newArrayList(
      new Tuple<>(Rotation.NONE, new BlockPos(4, -1, 0)),
      new Tuple<>(Rotation.CLOCKWISE_90, new BlockPos(12, -1, 4)),
      new Tuple<>(Rotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 8)),
      new Tuple<>(Rotation.CLOCKWISE_180, new BlockPos(8, -1, 12))
   );
   static final EndCityPieces.SectionGenerator FAT_TOWER_GENERATOR = new EndCityPieces.SectionGenerator() {
      @Override
      public void init() {
      }

      @Override
      public boolean generate(
         StructureTemplateManager param0, int param1, EndCityPieces.EndCityPiece param2, BlockPos param3, List<StructurePiece> param4, RandomSource param5
      ) {
         Rotation var8 = param2.placeSettings().getRotation();
         EndCityPieces.EndCityPiece var7 = EndCityPieces.addHelper(
            param4, EndCityPieces.addPiece(param0, param2, new BlockPos(-3, 4, -3), "fat_tower_base", var8, true)
         );
         var7 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var7, new BlockPos(0, 4, 0), "fat_tower_middle", var8, true));

         for (int var9 = 0; var9 < 2 && param5.nextInt(3) != 0; var9++) {
            var7 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var7, new BlockPos(0, 8, 0), "fat_tower_middle", var8, true));

            for (Tuple<Rotation, BlockPos> var11 : EndCityPieces.FAT_TOWER_BRIDGES) {
               if (param5.nextBoolean()) {
                  EndCityPieces.EndCityPiece var12 = EndCityPieces.addHelper(
                     param4, EndCityPieces.addPiece(param0, var7, var11.getB(), "bridge_end", var8.getRotated(var11.getA()), true)
                  );
                  EndCityPieces.recursiveChildren(param0, EndCityPieces.TOWER_BRIDGE_GENERATOR, param1 + 1, var12, null, param4, param5);
               }
            }
         }

         var7 = EndCityPieces.addHelper(param4, EndCityPieces.addPiece(param0, var7, new BlockPos(-2, 8, -2), "fat_tower_top", var8, true));
         return true;
      }
   };

   static EndCityPieces.EndCityPiece addPiece(
      StructureTemplateManager param0, EndCityPieces.EndCityPiece param1, BlockPos param2, String param3, Rotation param4, boolean param5
   ) {
      EndCityPieces.EndCityPiece var6 = new EndCityPieces.EndCityPiece(param0, param3, param1.templatePosition(), param4, param5);
      BlockPos var7 = param1.template().calculateConnectedPosition(param1.placeSettings(), param2, var6.placeSettings(), BlockPos.ZERO);
      var6.move(var7.getX(), var7.getY(), var7.getZ());
      return var6;
   }

   public static void startHouseTower(StructureTemplateManager param0, BlockPos param1, Rotation param2, List<StructurePiece> param3, RandomSource param4) {
      FAT_TOWER_GENERATOR.init();
      HOUSE_TOWER_GENERATOR.init();
      TOWER_BRIDGE_GENERATOR.init();
      TOWER_GENERATOR.init();
      EndCityPieces.EndCityPiece var5 = addHelper(param3, new EndCityPieces.EndCityPiece(param0, "base_floor", param1, param2, true));
      var5 = addHelper(param3, addPiece(param0, var5, new BlockPos(-1, 0, -1), "second_floor_1", param2, false));
      var5 = addHelper(param3, addPiece(param0, var5, new BlockPos(-1, 4, -1), "third_floor_1", param2, false));
      var5 = addHelper(param3, addPiece(param0, var5, new BlockPos(-1, 8, -1), "third_roof", param2, true));
      recursiveChildren(param0, TOWER_GENERATOR, 1, var5, null, param3, param4);
   }

   static EndCityPieces.EndCityPiece addHelper(List<StructurePiece> param0, EndCityPieces.EndCityPiece param1) {
      param0.add(param1);
      return param1;
   }

   static boolean recursiveChildren(
      StructureTemplateManager param0,
      EndCityPieces.SectionGenerator param1,
      int param2,
      EndCityPieces.EndCityPiece param3,
      BlockPos param4,
      List<StructurePiece> param5,
      RandomSource param6
   ) {
      if (param2 > 8) {
         return false;
      }

      List<StructurePiece> var7 = Lists.newArrayList();
      if (param1.generate(param0, param2, param3, param4, var7, param6)) {
         boolean var8 = false;
         int var9 = param6.nextInt();

         for (StructurePiece var11 : var7) {
            var11.setGenDepth(var9);
            StructurePiece var12 = StructurePiece.findCollisionPiece(param5, var11.getBoundingBox());
            if (var12 != null && var12.getGenDepth() != param3.getGenDepth()) {
               var8 = true;
               break;
            }
         }

         if (!var8) {
            param5.addAll(var7);
            return true;
         }
      }

      return false;
   }

   public static class EndCityPiece extends TemplateStructurePiece {
      public EndCityPiece(StructureTemplateManager param0, String param1, BlockPos param2, Rotation param3, boolean param4) {
         super(StructurePieceType.END_CITY_PIECE, 0, param0, makeResourceLocation(param1), param1, makeSettings(param4, param3), param2);
      }

      public EndCityPiece(StructureTemplateManager param0, CompoundTag param1) {
         super(StructurePieceType.END_CITY_PIECE, param1, param0, l_param1 -> makeSettings(param1.getBoolean("OW"), Rotation.valueOf(param1.getString("Rot"))));
      }

      private static StructurePlaceSettings makeSettings(boolean param0, Rotation param1) {
         BlockIgnoreProcessor var2 = param0 ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
         return new StructurePlaceSettings().setIgnoreEntities(true).addProcessor(var2).setRotation(param1);
      }

      @Override
      protected ResourceLocation makeTemplateLocation() {
         return makeResourceLocation(this.templateName);
      }

      private static ResourceLocation makeResourceLocation(String param0) {
         return new ResourceLocation("end_city/" + param0);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext param0, CompoundTag param1) {
         super.addAdditionalSaveData(param0, param1);
         param1.putString("Rot", this.placeSettings.getRotation().name());
         param1.putBoolean("OW", this.placeSettings.getProcessors().get(0) == BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void handleDataMarker(String param0, BlockPos param1, ServerLevelAccessor param2, RandomSource param3, BoundingBox param4) {
         if (param0.startsWith("Chest")) {
            BlockPos var6 = param1.below();
            if (param4.isInside(var6)) {
               RandomizableContainerBlockEntity.setLootTable(param2, param3, var6, BuiltInLootTables.END_CITY_TREASURE);
            }
         } else if (param4.isInside(param1) && Level.isInSpawnableBounds(param1)) {
            if (param0.startsWith("Sentry")) {
               Shulker var6 = EntityType.SHULKER.create(param2.getLevel());
               if (var6 != null) {
                  var6.setPos(param1.getX() + 0.5, param1.getY(), param1.getZ() + 0.5);
                  param2.addFreshEntity(var6);
               }
            } else if (param0.startsWith("Elytra")) {
               ItemFrame var6 = new ItemFrame(param2.getLevel(), param1, this.placeSettings.getRotation().rotate(Direction.SOUTH));
               var6.setItem(new ItemStack(Items.ELYTRA), false);
               param2.addFreshEntity(var6);
            }
         }
      }
   }

   interface SectionGenerator {
      void init();

      boolean generate(StructureTemplateManager var1, int var2, EndCityPieces.EndCityPiece var3, BlockPos var4, List<StructurePiece> var5, RandomSource var6);
   }
}
