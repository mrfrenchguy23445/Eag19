package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class StructurePlaceSettings {
   private Mirror mirror = Mirror.NONE;
   private Rotation rotation = Rotation.NONE;
   private BlockPos rotationPivot = BlockPos.ZERO;
   private boolean ignoreEntities;
   @Nullable
   private BoundingBox boundingBox;
   private boolean keepLiquids = true;
   @Nullable
   private RandomSource random;
   private int palette;
   private final List<StructureProcessor> processors = Lists.newArrayList();
   private boolean knownShape;
   private boolean finalizeEntities;

   public StructurePlaceSettings copy() {
      StructurePlaceSettings var1 = new StructurePlaceSettings();
      var1.mirror = this.mirror;
      var1.rotation = this.rotation;
      var1.rotationPivot = this.rotationPivot;
      var1.ignoreEntities = this.ignoreEntities;
      var1.boundingBox = this.boundingBox;
      var1.keepLiquids = this.keepLiquids;
      var1.random = this.random;
      var1.palette = this.palette;
      var1.processors.addAll(this.processors);
      var1.knownShape = this.knownShape;
      var1.finalizeEntities = this.finalizeEntities;
      return var1;
   }

   public StructurePlaceSettings setMirror(Mirror param0) {
      this.mirror = param0;
      return this;
   }

   public StructurePlaceSettings setRotation(Rotation param0) {
      this.rotation = param0;
      return this;
   }

   public StructurePlaceSettings setRotationPivot(BlockPos param0) {
      this.rotationPivot = param0;
      return this;
   }

   public StructurePlaceSettings setIgnoreEntities(boolean param0) {
      this.ignoreEntities = param0;
      return this;
   }

   public StructurePlaceSettings setBoundingBox(BoundingBox param0) {
      this.boundingBox = param0;
      return this;
   }

   public StructurePlaceSettings setRandom(@Nullable RandomSource param0) {
      this.random = param0;
      return this;
   }

   public StructurePlaceSettings setKeepLiquids(boolean param0) {
      this.keepLiquids = param0;
      return this;
   }

   public StructurePlaceSettings setKnownShape(boolean param0) {
      this.knownShape = param0;
      return this;
   }

   public StructurePlaceSettings clearProcessors() {
      this.processors.clear();
      return this;
   }

   public StructurePlaceSettings addProcessor(StructureProcessor param0) {
      this.processors.add(param0);
      return this;
   }

   public StructurePlaceSettings popProcessor(StructureProcessor param0) {
      this.processors.remove(param0);
      return this;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public BlockPos getRotationPivot() {
      return this.rotationPivot;
   }

   public RandomSource getRandom(@Nullable BlockPos param0) {
      if (this.random != null) {
         return this.random;
      } else {
         return param0 == null ? RandomSource.create(Util.getMillis()) : RandomSource.create(Mth.getSeed(param0));
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   @Nullable
   public BoundingBox getBoundingBox() {
      return this.boundingBox;
   }

   public boolean getKnownShape() {
      return this.knownShape;
   }

   public List<StructureProcessor> getProcessors() {
      return this.processors;
   }

   public boolean shouldKeepLiquids() {
      return this.keepLiquids;
   }

   public StructureTemplate.Palette getRandomPalette(List<StructureTemplate.Palette> param0, @Nullable BlockPos param1) {
      int var3 = param0.size();
      if (var3 == 0) {
         throw new IllegalStateException("No palettes");
      } else {
         return param0.get(this.getRandom(param1).nextInt(var3));
      }
   }

   public StructurePlaceSettings setFinalizeEntities(boolean param0) {
      this.finalizeEntities = param0;
      return this;
   }

   public boolean shouldFinalizeEntities() {
      return this.finalizeEntities;
   }
}
