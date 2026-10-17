package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

public class CompassItem extends Item implements Vanishable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String TAG_LODESTONE_POS = "LodestonePos";
   public static final String TAG_LODESTONE_DIMENSION = "LodestoneDimension";
   public static final String TAG_LODESTONE_TRACKED = "LodestoneTracked";

   public CompassItem(Item.Properties param0) {
      super(param0);
   }

   public static boolean isLodestoneCompass(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      return var1 != null && (var1.contains("LodestoneDimension") || var1.contains("LodestonePos"));
   }

   private static Optional<ResourceKey<Level>> getLodestoneDimension(CompoundTag param0) {
      return Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, param0.get("LodestoneDimension")).result();
   }

   @Nullable
   public static GlobalPos getLodestonePosition(CompoundTag param0) {
      boolean var1 = param0.contains("LodestonePos");
      boolean var2 = param0.contains("LodestoneDimension");
      if (var1 && var2) {
         Optional<ResourceKey<Level>> var3 = getLodestoneDimension(param0);
         if (var3.isPresent()) {
            BlockPos var4 = NbtUtils.readBlockPos(param0.getCompound("LodestonePos"));
            return GlobalPos.of(var3.get(), var4);
         }
      }

      return null;
   }

   @Nullable
   public static GlobalPos getSpawnPosition(Level param0) {
      return param0.dimensionType().natural() ? GlobalPos.of(param0.dimension(), param0.getSharedSpawnPos()) : null;
   }

   @Override
   public boolean isFoil(ItemStack param0) {
      return isLodestoneCompass(param0) || super.isFoil(param0);
   }

   @Override
   public void inventoryTick(ItemStack param0, Level param1, Entity param2, int param3, boolean param4) {
      if (!param1.isClientSide) {
         if (isLodestoneCompass(param0)) {
            CompoundTag var6 = param0.getOrCreateTag();
            if (var6.contains("LodestoneTracked") && !var6.getBoolean("LodestoneTracked")) {
               return;
            }

            Optional<ResourceKey<Level>> var7 = getLodestoneDimension(var6);
            if (var7.isPresent() && var7.get() == param1.dimension() && var6.contains("LodestonePos")) {
               BlockPos var8 = NbtUtils.readBlockPos(var6.getCompound("LodestonePos"));
               if (!param1.isInWorldBounds(var8) || !((ServerLevel)param1).getPoiManager().existsAtPosition(PoiTypes.LODESTONE, var8)) {
                  var6.remove("LodestonePos");
               }
            }
         }
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      BlockPos var2 = param0.getClickedPos();
      Level var3 = param0.getLevel();
      if (!var3.getBlockState(var2).is(Blocks.LODESTONE)) {
         return super.useOn(param0);
      }

      var3.playSound((Player)null, var2, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
      Player var4 = param0.getPlayer();
      ItemStack var5 = param0.getItemInHand();
      boolean var6 = !var4.getAbilities().instabuild && var5.getCount() == 1;
      if (var6) {
         this.addLodestoneTags(var3.dimension(), var2, var5.getOrCreateTag());
      } else {
         ItemStack var7 = new ItemStack(Items.COMPASS, 1);
         CompoundTag var8 = var5.hasTag() ? var5.getTag().copy() : new CompoundTag();
         var7.setTag(var8);
         if (!var4.getAbilities().instabuild) {
            var5.shrink(1);
         }

         this.addLodestoneTags(var3.dimension(), var2, var8);
         if (!var4.getInventory().add(var7)) {
            var4.drop(var7, false);
         }
      }

      return InteractionResult.sidedSuccess(var3.isClientSide);
   }

   private void addLodestoneTags(ResourceKey<Level> param0, BlockPos param1, CompoundTag param2) {
      param2.put("LodestonePos", NbtUtils.writeBlockPos(param1));
      Level.RESOURCE_KEY_CODEC
         .encodeStart(NbtOps.INSTANCE, param0)
         .resultOrPartial(LOGGER::error)
         .ifPresent(l_param1 -> param2.put("LodestoneDimension", l_param1));
      param2.putBoolean("LodestoneTracked", true);
   }

   @Override
   public String getDescriptionId(ItemStack param0) {
      return isLodestoneCompass(param0) ? "item.minecraft.lodestone_compass" : super.getDescriptionId(param0);
   }
}
