package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class BeehiveBlockEntity extends BlockEntity {
   public static final String TAG_FLOWER_POS = "FlowerPos";
   public static final String MIN_OCCUPATION_TICKS = "MinOccupationTicks";
   public static final String ENTITY_DATA = "EntityData";
   public static final String TICKS_IN_HIVE = "TicksInHive";
   public static final String HAS_NECTAR = "HasNectar";
   public static final String BEES = "Bees";
   private static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
      "Air",
      "ArmorDropChances",
      "ArmorItems",
      "Brain",
      "CanPickUpLoot",
      "DeathTime",
      "FallDistance",
      "FallFlying",
      "Fire",
      "HandDropChances",
      "HandItems",
      "HurtByTimestamp",
      "HurtTime",
      "LeftHanded",
      "Motion",
      "NoGravity",
      "OnGround",
      "PortalCooldown",
      "Pos",
      "Rotation",
      "CannotEnterHiveTicks",
      "TicksSincePollination",
      "CropsGrownSincePollination",
      "HivePos",
      "Passengers",
      "Leash",
      "UUID"
   );
   public static final int MAX_OCCUPANTS = 3;
   private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
   private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
   public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
   private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
   @Nullable
   private BlockPos savedFlowerPos;

   public BeehiveBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.BEEHIVE, param0, param1);
   }

   @Override
   public void setChanged() {
      if (this.isFireNearby()) {
         this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      super.setChanged();
   }

   public boolean isFireNearby() {
      if (this.level == null) {
         return false;
      }

      for (BlockPos var2 : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
         if (this.level.getBlockState(var2).getBlock() instanceof FireBlock) {
            return true;
         }
      }

      return false;
   }

   public boolean isEmpty() {
      return this.stored.isEmpty();
   }

   public boolean isFull() {
      return this.stored.size() == 3;
   }

   public void emptyAllLivingFromHive(@Nullable Player param0, BlockState param1, BeehiveBlockEntity.BeeReleaseStatus param2) {
      List<Entity> var4 = this.releaseAllOccupants(param1, param2);
      if (param0 != null) {
         for (Entity var6 : var4) {
            if (var6 instanceof Bee var7 && param0.position().distanceToSqr(var6.position()) <= 16.0) {
               if (!this.isSedated()) {
                  var7.setTarget(param0);
               } else {
                  var7.setStayOutOfHiveCountdown(400);
               }
            }
         }
      }
   }

   private List<Entity> releaseAllOccupants(BlockState param0, BeehiveBlockEntity.BeeReleaseStatus param1) {
      List<Entity> var3 = Lists.newArrayList();
      this.stored.removeIf(l_param3 -> releaseOccupant(this.level, this.worldPosition, param0, l_param3, var3, param1, this.savedFlowerPos));
      if (!var3.isEmpty()) {
         super.setChanged();
      }

      return var3;
   }

   public void addOccupant(Entity param0, boolean param1) {
      this.addOccupantWithPresetTicks(param0, param1, 0);
   }

   @VisibleForDebug
   public int getOccupantCount() {
      return this.stored.size();
   }

   public static int getHoneyLevel(BlockState param0) {
      return param0.getValue(BeehiveBlock.HONEY_LEVEL);
   }

   @VisibleForDebug
   public boolean isSedated() {
      return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
   }

   public void addOccupantWithPresetTicks(Entity param0, boolean param1, int param2) {
      if (this.stored.size() < 3) {
         param0.stopRiding();
         param0.ejectPassengers();
         CompoundTag var4 = new CompoundTag();
         param0.save(var4);
         this.storeBee(var4, param2, param1);
         if (this.level != null) {
            if (param0 instanceof Bee var5 && var5.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
               this.savedFlowerPos = var5.getSavedFlowerPos();
            }

            BlockPos var5 = this.getBlockPos();
            this.level.playSound(null, var5.getX(), var5.getY(), var5.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, var5, GameEvent.Context.of(param0, this.getBlockState()));
         }

         param0.discard();
         super.setChanged();
      }
   }

   public void storeBee(CompoundTag param0, int param1, boolean param2) {
      this.stored.add(new BeehiveBlockEntity.BeeData(param0, param1, param2 ? 2400 : 600));
   }

   private static boolean releaseOccupant(
      Level param0,
      BlockPos param1,
      BlockState param2,
      BeehiveBlockEntity.BeeData param3,
      @Nullable List<Entity> param4,
      BeehiveBlockEntity.BeeReleaseStatus param5,
      @Nullable BlockPos param6
   ) {
      if ((param0.isNight() || param0.isRaining()) && param5 != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      }

      CompoundTag var7 = param3.entityData.copy();
      removeIgnoredBeeTags(var7);
      var7.put("HivePos", NbtUtils.writeBlockPos(param1));
      var7.putBoolean("NoGravity", true);
      Direction var8 = param2.getValue(BeehiveBlock.FACING);
      BlockPos var9 = param1.relative(var8);
      boolean var10 = !param0.getBlockState(var9).getCollisionShape(param0, var9).isEmpty();
      if (var10 && param5 != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      }

      Entity var11 = EntityType.loadEntityRecursive(var7, param0, l_param0 -> l_param0);
      if (var11 != null) {
         if (!var11.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
            return false;
         }

         if (var11 instanceof Bee var12) {
            if (param6 != null && !var12.hasSavedFlowerPos() && param0.random.nextFloat() < 0.9F) {
               var12.setSavedFlowerPos(param6);
            }

            if (param5 == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
               var12.dropOffNectar();
               if (param2.is(BlockTags.BEEHIVES, l_param0 -> l_param0.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                  int var13 = getHoneyLevel(param2);
                  if (var13 < 5) {
                     int var14 = param0.random.nextInt(100) == 0 ? 2 : 1;
                     if (var13 + var14 > 5) {
                        var14--;
                     }

                     param0.setBlockAndUpdate(param1, param2.setValue(BeehiveBlock.HONEY_LEVEL, var13 + var14));
                  }
               }
            }

            setBeeReleaseData(param3.ticksInHive, var12);
            if (param4 != null) {
               param4.add(var12);
            }

            float var13 = var11.getBbWidth();
            double var14 = var10 ? 0.0 : 0.55 + var13 / 2.0F;
            double var16 = param1.getX() + 0.5 + var14 * var8.getStepX();
            double var18 = param1.getY() + 0.5 - var11.getBbHeight() / 2.0F;
            double var20 = param1.getZ() + 0.5 + var14 * var8.getStepZ();
            var11.moveTo(var16, var18, var20, var11.getYRot(), var11.getXRot());
         }

         param0.playSound((Player)null, param1, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
         param0.gameEvent(GameEvent.BLOCK_CHANGE, param1, GameEvent.Context.of(var11, param0.getBlockState(param1)));
         return param0.addFreshEntity(var11);
      } else {
         return false;
      }
   }

   static void removeIgnoredBeeTags(CompoundTag param0) {
      for (String var2 : IGNORED_BEE_TAGS) {
         param0.remove(var2);
      }
   }

   private static void setBeeReleaseData(int param0, Bee param1) {
      int var2 = param1.getAge();
      if (var2 < 0) {
         param1.setAge(Math.min(0, var2 + param0));
      } else if (var2 > 0) {
         param1.setAge(Math.max(0, var2 - param0));
      }

      param1.setInLoveTime(Math.max(0, param1.getInLoveTime() - param0));
   }

   private boolean hasSavedFlowerPos() {
      return this.savedFlowerPos != null;
   }

   private static void tickOccupants(Level param0, BlockPos param1, BlockState param2, List<BeehiveBlockEntity.BeeData> param3, @Nullable BlockPos param4) {
      boolean var5 = false;
      Iterator<BeehiveBlockEntity.BeeData> var6 = param3.iterator();

      while (var6.hasNext()) {
         BeehiveBlockEntity.BeeData var7 = var6.next();
         if (var7.ticksInHive > var7.minOccupationTicks) {
            BeehiveBlockEntity.BeeReleaseStatus var8 = var7.entityData.getBoolean("HasNectar")
               ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED
               : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
            if (releaseOccupant(param0, param1, param2, var7, null, var8, param4)) {
               var5 = true;
               var6.remove();
            }
         }

         var7.ticksInHive++;
      }

      if (var5) {
         setChanged(param0, param1, param2);
      }
   }

   public static void serverTick(Level param0, BlockPos param1, BlockState param2, BeehiveBlockEntity param3) {
      tickOccupants(param0, param1, param2, param3.stored, param3.savedFlowerPos);
      if (!param3.stored.isEmpty() && param0.getRandom().nextDouble() < 0.005) {
         double var4 = param1.getX() + 0.5;
         double var6 = param1.getY();
         double var8 = param1.getZ() + 0.5;
         param0.playSound(null, var4, var6, var8, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      DebugPackets.sendHiveInfo(param0, param1, param2, param3);
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      this.stored.clear();
      ListTag var2 = param0.getList("Bees", 10);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         CompoundTag var4 = var2.getCompound(var3);
         BeehiveBlockEntity.BeeData var5 = new BeehiveBlockEntity.BeeData(
            var4.getCompound("EntityData"), var4.getInt("TicksInHive"), var4.getInt("MinOccupationTicks")
         );
         this.stored.add(var5);
      }

      this.savedFlowerPos = null;
      if (param0.contains("FlowerPos")) {
         this.savedFlowerPos = NbtUtils.readBlockPos(param0.getCompound("FlowerPos"));
      }
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      param0.put("Bees", this.writeBees());
      if (this.hasSavedFlowerPos()) {
         param0.put("FlowerPos", NbtUtils.writeBlockPos(this.savedFlowerPos));
      }
   }

   public ListTag writeBees() {
      ListTag var1 = new ListTag();

      for (BeehiveBlockEntity.BeeData var3 : this.stored) {
         CompoundTag var4 = var3.entityData.copy();
         var4.remove("UUID");
         CompoundTag var5 = new CompoundTag();
         var5.put("EntityData", var4);
         var5.putInt("TicksInHive", var3.ticksInHive);
         var5.putInt("MinOccupationTicks", var3.minOccupationTicks);
         var1.add(var5);
      }

      return var1;
   }

   static class BeeData {
      final CompoundTag entityData;
      int ticksInHive;
      final int minOccupationTicks;

      BeeData(CompoundTag param0, int param1, int param2) {
         BeehiveBlockEntity.removeIgnoredBeeTags(param0);
         this.entityData = param0;
         this.ticksInHive = param1;
         this.minOccupationTicks = param2;
      }
   }

   public enum BeeReleaseStatus {
      HONEY_DELIVERED,
      BEE_RELEASED,
      EMERGENCY;
   }
}
