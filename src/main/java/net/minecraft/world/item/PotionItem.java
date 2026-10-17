package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {
   private static final int DRINK_DURATION = 32;

   public PotionItem(Item.Properties param0) {
      super(param0);
   }

   @Override
   public ItemStack getDefaultInstance() {
      return PotionUtils.setPotion(super.getDefaultInstance(), Potions.WATER);
   }

   @Override
   public ItemStack finishUsingItem(ItemStack param0, Level param1, LivingEntity param2) {
      Player var4 = param2 instanceof Player ? (Player)param2 : null;
      if (var4 instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)var4, param0);
      }

      if (!param1.isClientSide) {
         for (MobEffectInstance var7 : PotionUtils.getMobEffects(param0)) {
            if (var7.getEffect().isInstantenous()) {
               var7.getEffect().applyInstantenousEffect(var4, var4, param2, var7.getAmplifier(), 1.0);
            } else {
               param2.addEffect(new MobEffectInstance(var7));
            }
         }
      }

      if (var4 != null) {
         var4.awardStat(Stats.ITEM_USED.get(this));
         if (!var4.getAbilities().instabuild) {
            param0.shrink(1);
         }
      }

      if (var4 == null || !var4.getAbilities().instabuild) {
         if (param0.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
         }

         if (var4 != null) {
            var4.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
         }
      }

      param2.gameEvent(GameEvent.DRINK);
      return param0;
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      Player var4 = param0.getPlayer();
      ItemStack var5 = param0.getItemInHand();
      BlockState var6 = var2.getBlockState(var3);
      if (param0.getClickedFace() != Direction.DOWN && var6.is(BlockTags.CONVERTABLE_TO_MUD) && PotionUtils.getPotion(var5) == Potions.WATER) {
         var2.playSound((Player)null, var3, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
         var4.setItemInHand(param0.getHand(), ItemUtils.createFilledResult(var5, var4, new ItemStack(Items.GLASS_BOTTLE)));
         var4.awardStat(Stats.ITEM_USED.get(var5.getItem()));
         if (!var2.isClientSide) {
            ServerLevel var7 = (ServerLevel)var2;

            for (int var8 = 0; var8 < 5; var8++) {
               var7.sendParticles(
                  ParticleTypes.SPLASH, var3.getX() + var2.random.nextDouble(), var3.getY() + 1, var3.getZ() + var2.random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0
               );
            }
         }

         var2.playSound((Player)null, var3, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         var2.gameEvent((Entity)null, GameEvent.FLUID_PLACE, var3);
         var2.setBlockAndUpdate(var3, Blocks.MUD.defaultBlockState());
         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      return 32;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.DRINK;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      return ItemUtils.startUsingInstantly(param0, param1, param2);
   }

   @Override
   public String getDescriptionId(ItemStack param0) {
      return PotionUtils.getPotion(param0).getName(this.getDescriptionId() + ".effect.");
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      PotionUtils.addPotionTooltip(param0, param2, 1.0F);
   }
}
