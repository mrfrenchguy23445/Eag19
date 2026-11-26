package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
   private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
   private final EntityType<? extends HangingEntity> type;

   public HangingEntityItem(EntityType<? extends HangingEntity> param0, Item.Properties param1) {
      super(param1);
      this.type = param0;
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      BlockPos var2 = param0.getClickedPos();
      Direction var3 = param0.getClickedFace();
      BlockPos var4 = var2.relative(var3);
      Player var5 = param0.getPlayer();
      ItemStack var6 = param0.getItemInHand();
      if (var5 != null && !this.mayPlace(var5, var3, var6, var4)) {
         return InteractionResult.FAIL;
      }

      Level var7 = param0.getLevel();
      HangingEntity var8;
      if (this.type == EntityType.PAINTING) {
         Optional<Painting> var9 = Painting.create(var7, var4, var3);
         if (var9.isEmpty()) {
            return InteractionResult.CONSUME;
         }

         var8 = var9.get();
      } else if (this.type == EntityType.ITEM_FRAME) {
         var8 = new ItemFrame(var7, var4, var3);
      } else {
         if (this.type != EntityType.GLOW_ITEM_FRAME) {
            return InteractionResult.sidedSuccess(var7.isClientSide);
         }

         var8 = new GlowItemFrame(var7, var4, var3);
      }

      CompoundTag var9 = var6.getTag();
      if (var9 != null) {
         EntityType.updateCustomEntityTag(var7, var5, var8, var9);
      }

      if (var8.survives()) {
         if (!var7.isClientSide) {
            var8.playPlacementSound();
            var7.gameEvent(var5, GameEvent.ENTITY_PLACE, var8.position());
            var7.addFreshEntity(var8);
         }

         var6.shrink(1);
         return InteractionResult.sidedSuccess(var7.isClientSide);
      } else {
         return InteractionResult.CONSUME;
      }
   }

   protected boolean mayPlace(Player param0, Direction param1, ItemStack param2, BlockPos param3) {
      return !param1.getAxis().isVertical() && param0.mayUseItemAt(param3, param1, param2);
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      super.appendHoverText(param0, param1, param2, param3);
      if (this.type == EntityType.PAINTING) {
         CompoundTag var5 = param0.getTag();
         if (var5 != null && var5.contains("EntityTag", 10)) {
            CompoundTag var6 = var5.getCompound("EntityTag");
            Painting.loadVariant(var6)
               .ifPresentOrElse(
                  l_param1 -> {
                     l_param1.unwrapKey().ifPresent(l2_param1 -> {
                        param2.add(Component.translatable(l2_param1.location().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW));
                        param2.add(Component.translatable(l2_param1.location().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY));
                     });
                     param2.add(
                        Component.translatable(
                           "painting.dimensions", Mth.positiveCeilDiv(l_param1.value().getWidth(), 16), Mth.positiveCeilDiv(l_param1.value().getHeight(), 16)
                        )
                     );
                  },
                  () -> param2.add(TOOLTIP_RANDOM_VARIANT)
               );
         } else if (param3.isCreative()) {
            param2.add(TOOLTIP_RANDOM_VARIANT);
         }
      }
   }
}
