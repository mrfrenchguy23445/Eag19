package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SuspiciousSandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BrushItem extends Item {
   public static final int TICKS_BETWEEN_SWEEPS = 10;
   private static final int USE_DURATION = 225;

   public BrushItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Player var2 = param0.getPlayer();
      if (var2 != null) {
         var2.startUsingItem(param0.getHand());
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.BRUSH;
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return 225;
   }

   @Override
   public void onUseTick(Level param0, LivingEntity param1, ItemStack param2, int param3) {
      if (param3 >= 0 && param1 instanceof Player var5) {
         BlockHitResult var6 = Item.getPlayerPOVHitResult(param0, var5, ClipContext.Fluid.NONE);
         BlockPos var7 = var6.getBlockPos();
         if (var6.getType() == HitResult.Type.MISS) {
            param1.releaseUsingItem();
         } else {
            int var8 = this.getUseDuration(param2) - param3 + 1;
            if (var8 == 1 || var8 % 10 == 0) {
               BlockState var9 = param0.getBlockState(var7);
               this.spawnDustParticles(param0, var6, var9, param1.getViewVector(0.0F));
               param0.playSound(var5, var7, SoundEvents.BRUSH_BRUSHING, SoundSource.PLAYERS);
               if (!param0.isClientSide() && var9.is(Blocks.SUSPICIOUS_SAND) && param0.getBlockEntity(var7) instanceof SuspiciousSandBlockEntity var10) {
                  boolean var11 = var10.brush(param0.getGameTime(), var5, var6.getDirection());
                  if (var11) {
                     param2.hurtAndBreak(1, param1, l_param0 -> l_param0.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                  }
               }
            }
         }
      } else {
         param1.releaseUsingItem();
      }
   }

   public void spawnDustParticles(Level param0, BlockHitResult param1, BlockState param2, Vec3 param3) {
      double var5 = 3.0;
      int var7 = param0.getRandom().nextInt(7, 12);
      BlockParticleOption var8 = new BlockParticleOption(ParticleTypes.BLOCK, param2);
      Direction var9 = param1.getDirection();
      BrushItem.DustParticlesDelta var10 = BrushItem.DustParticlesDelta.fromDirection(param3, var9);
      Vec3 var11 = param1.getLocation();

      for (int var12 = 0; var12 < var7; var12++) {
         param0.addParticle(
            var8,
            var11.x - (var9 == Direction.WEST ? 1.0E-6F : 0.0F),
            var11.y,
            var11.z - (var9 == Direction.NORTH ? 1.0E-6F : 0.0F),
            var10.xd() * 3.0 * param0.getRandom().nextDouble(),
            0.0,
            var10.zd() * 3.0 * param0.getRandom().nextDouble()
         );
      }
   }

   record DustParticlesDelta(double xd, double yd, double zd) {
      private static final double ALONG_SIDE_DELTA = 1.0;
      private static final double OUT_FROM_SIDE_DELTA = 0.1;

      public static BrushItem.DustParticlesDelta fromDirection(Vec3 param0, Direction param1) {
         double var2 = 0.0;

         return switch (param1) {
            case DOWN -> new BrushItem.DustParticlesDelta(-param0.x(), 0.0, param0.z());
            case UP -> new BrushItem.DustParticlesDelta(param0.z(), 0.0, -param0.x());
            case NORTH -> new BrushItem.DustParticlesDelta(1.0, 0.0, -0.1);
            case SOUTH -> new BrushItem.DustParticlesDelta(-1.0, 0.0, 0.1);
            case WEST -> new BrushItem.DustParticlesDelta(-0.1, 0.0, -1.0);
            case EAST -> new BrushItem.DustParticlesDelta(0.1, 0.0, 1.0);
         };
      }
   }
}
