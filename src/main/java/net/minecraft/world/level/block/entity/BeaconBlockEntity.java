package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable {
   private static final int MAX_LEVELS = 4;
   public static final MobEffect[][] BEACON_EFFECTS = new MobEffect[][]{
      {MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED}, {MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP}, {MobEffects.DAMAGE_BOOST}, {MobEffects.REGENERATION}
   };
   private static final Set<MobEffect> VALID_EFFECTS = Arrays.stream(BEACON_EFFECTS).flatMap(Arrays::stream).collect(Collectors.toSet());
   public static final int DATA_LEVELS = 0;
   public static final int DATA_PRIMARY = 1;
   public static final int DATA_SECONDARY = 2;
   public static final int NUM_DATA_VALUES = 3;
   private static final int BLOCKS_CHECK_PER_TICK = 10;
   private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
   List<BeaconBlockEntity.BeaconBeamSection> beamSections = Lists.newArrayList();
   private List<BeaconBlockEntity.BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
   int levels;
   private int lastCheckY;
   @Nullable
   MobEffect primaryPower;
   @Nullable
   MobEffect secondaryPower;
   @Nullable
   private Component name;
   private LockCode lockKey = LockCode.NO_LOCK;
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int param0) {
         return switch (param0) {
            case 0 -> BeaconBlockEntity.this.levels;
            case 1 -> MobEffect.getIdFromNullable(BeaconBlockEntity.this.primaryPower);
            case 2 -> MobEffect.getIdFromNullable(BeaconBlockEntity.this.secondaryPower);
            default -> 0;
         };
      }

      @Override
      public void set(int param0, int param1) {
         switch (param0) {
            case 0:
               BeaconBlockEntity.this.levels = param1;
               break;
            case 1:
               if (!BeaconBlockEntity.this.level.isClientSide && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                  BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
               }

               BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.getValidEffectById(param1);
               break;
            case 2:
               BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.getValidEffectById(param1);
         }
      }

      @Override
      public int getCount() {
         return 3;
      }
   };

   public BeaconBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.BEACON, param0, param1);
   }

   public static void tick(Level param0, BlockPos param1, BlockState param2, BeaconBlockEntity param3) {
      int var4 = param1.getX();
      int var5 = param1.getY();
      int var6 = param1.getZ();
      BlockPos var7;
      if (param3.lastCheckY < var5) {
         var7 = param1;
         param3.checkingBeamSections = Lists.newArrayList();
         param3.lastCheckY = var7.getY() - 1;
      } else {
         var7 = new BlockPos(var4, param3.lastCheckY + 1, var6);
      }

      BeaconBlockEntity.BeaconBeamSection var8 = param3.checkingBeamSections.isEmpty()
         ? null
         : param3.checkingBeamSections.get(param3.checkingBeamSections.size() - 1);
      int var9 = param0.getHeight(Heightmap.Types.WORLD_SURFACE, var4, var6);

      for (int var10 = 0; var10 < 10 && var7.getY() <= var9; var10++) {
         BlockState var11 = param0.getBlockState(var7);
         Block var12 = var11.getBlock();
         if (var12 instanceof BeaconBeamBlock) {
            float[] var13 = ((BeaconBeamBlock)var12).getColor().getTextureDiffuseColors();
            if (param3.checkingBeamSections.size() <= 1) {
               var8 = new BeaconBlockEntity.BeaconBeamSection(var13);
               param3.checkingBeamSections.add(var8);
            } else if (var8 != null) {
               if (Arrays.equals(var13, var8.color)) {
                  var8.increaseHeight();
               } else {
                  var8 = new BeaconBlockEntity.BeaconBeamSection(
                     new float[]{(var8.color[0] + var13[0]) / 2.0F, (var8.color[1] + var13[1]) / 2.0F, (var8.color[2] + var13[2]) / 2.0F}
                  );
                  param3.checkingBeamSections.add(var8);
               }
            }
         } else {
            if (var8 == null || var11.getLightBlock(param0, var7) >= 15 && !var11.is(Blocks.BEDROCK)) {
               param3.checkingBeamSections.clear();
               param3.lastCheckY = var9;
               break;
            }

            var8.increaseHeight();
         }

         var7 = var7.above();
         param3.lastCheckY++;
      }

      int var10 = param3.levels;
      if (param0.getGameTime() % 80L == 0L) {
         if (!param3.beamSections.isEmpty()) {
            param3.levels = updateBase(param0, var4, var5, var6);
         }

         if (param3.levels > 0 && !param3.beamSections.isEmpty()) {
            applyEffects(param0, param1, param3.levels, param3.primaryPower, param3.secondaryPower);
            playSound(param0, param1, SoundEvents.BEACON_AMBIENT);
         }
      }

      if (param3.lastCheckY >= var9) {
         param3.lastCheckY = param0.getMinBuildHeight() - 1;
         boolean var11 = var10 > 0;
         param3.beamSections = param3.checkingBeamSections;
         if (!param0.isClientSide) {
            boolean var12 = param3.levels > 0;
            if (!var11 && var12) {
               playSound(param0, param1, SoundEvents.BEACON_ACTIVATE);

               for (ServerPlayer var14 : param0.getEntitiesOfClass(
                  ServerPlayer.class, new AABB(var4, var5, var6, var4, var5 - 4, var6).inflate(10.0, 5.0, 10.0)
               )) {
                  CriteriaTriggers.CONSTRUCT_BEACON.trigger(var14, param3.levels);
               }
            } else if (var11 && !var12) {
               playSound(param0, param1, SoundEvents.BEACON_DEACTIVATE);
            }
         }
      }
   }

   private static int updateBase(Level param0, int param1, int param2, int param3) {
      int var4 = 0;

      for (int var5 = 1; var5 <= 4; var4 = var5++) {
         int var6 = param2 - var5;
         if (var6 < param0.getMinBuildHeight()) {
            break;
         }

         boolean var7 = true;

         for (int var8 = param1 - var5; var8 <= param1 + var5 && var7; var8++) {
            for (int var9 = param3 - var5; var9 <= param3 + var5; var9++) {
               if (!param0.getBlockState(new BlockPos(var8, var6, var9)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                  var7 = false;
                  break;
               }
            }
         }

         if (!var7) {
            break;
         }
      }

      return var4;
   }

   @Override
   public void setRemoved() {
      playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
      super.setRemoved();
   }

   private static void applyEffects(Level param0, BlockPos param1, int param2, @Nullable MobEffect param3, @Nullable MobEffect param4) {
      if (!param0.isClientSide && param3 != null) {
         double var5 = param2 * 10 + 10;
         int var7 = 0;
         if (param2 >= 4 && param3 == param4) {
            var7 = 1;
         }

         int var8 = (9 + param2 * 2) * 20;
         AABB var9 = new AABB(param1).inflate(var5).expandTowards(0.0, param0.getHeight(), 0.0);
         List<Player> var10 = param0.getEntitiesOfClass(Player.class, var9);

         for (Player var12 : var10) {
            var12.addEffect(new MobEffectInstance(param3, var8, var7, true, true));
         }

         if (param2 >= 4 && param3 != param4 && param4 != null) {
            for (Player var12 : var10) {
               var12.addEffect(new MobEffectInstance(param4, var8, 0, true, true));
            }
         }
      }
   }

   public static void playSound(Level param0, BlockPos param1, SoundEvent param2) {
      param0.playSound((Player)null, param1, param2, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   public List<BeaconBlockEntity.BeaconBeamSection> getBeamSections() {
      return this.levels == 0 ? ImmutableList.of() : this.beamSections;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   @Nullable
   static MobEffect getValidEffectById(int param0) {
      MobEffect var1 = MobEffect.byId(param0);
      return VALID_EFFECTS.contains(var1) ? var1 : null;
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.primaryPower = getValidEffectById(param0.getInt("Primary"));
      this.secondaryPower = getValidEffectById(param0.getInt("Secondary"));
      if (param0.contains("CustomName", 8)) {
         this.name = Component.Serializer.fromJson(param0.getString("CustomName"));
      }

      this.lockKey = LockCode.fromTag(param0);
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.putInt("Primary", MobEffect.getIdFromNullable(this.primaryPower));
      param0.putInt("Secondary", MobEffect.getIdFromNullable(this.secondaryPower));
      param0.putInt("Levels", this.levels);
      if (this.name != null) {
         param0.putString("CustomName", Component.Serializer.toJson(this.name));
      }

      this.lockKey.addToTag(param0);
   }

   public void setCustomName(@Nullable Component param0) {
      this.name = param0;
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int param0, Inventory param1, Player param2) {
      return BaseContainerBlockEntity.canUnlock(param2, this.lockKey, this.getDisplayName())
         ? new BeaconMenu(param0, param1, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()))
         : null;
   }

   @Override
   public Component getDisplayName() {
      return this.getName();
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   @Override
   public void setLevel(Level param0) {
      super.setLevel(param0);
      this.lastCheckY = param0.getMinBuildHeight() - 1;
   }

   public static class BeaconBeamSection {
      final float[] color;
      private int height;

      public BeaconBeamSection(float[] param0) {
         this.color = param0;
         this.height = 1;
      }

      protected void increaseHeight() {
         this.height++;
      }

      public float[] getColor() {
         return this.color;
      }

      public int getHeight() {
         return this.height;
      }
   }
}
