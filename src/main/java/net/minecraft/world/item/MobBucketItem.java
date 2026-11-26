package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

public class MobBucketItem extends BucketItem {
   private final EntityType<?> type;
   private final SoundEvent emptySound;

   public MobBucketItem(EntityType<?> param0, Fluid param1, SoundEvent param2, Item.Properties param3) {
      super(param1, param3);
      this.type = param0;
      this.emptySound = param2;
   }

   @Override
   public void checkExtraContent(@Nullable Player param0, Level param1, ItemStack param2, BlockPos param3) {
      if (param1 instanceof ServerLevel) {
         this.spawn((ServerLevel)param1, param2, param3);
         param1.gameEvent(param0, GameEvent.ENTITY_PLACE, param3);
      }
   }

   @Override
   protected void playEmptySound(@Nullable Player param0, LevelAccessor param1, BlockPos param2) {
      param1.playSound(param0, param2, this.emptySound, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   private void spawn(ServerLevel param0, ItemStack param1, BlockPos param2) {
      if (this.type.spawn(param0, param1, null, param2, MobSpawnType.BUCKET, true, false) instanceof Bucketable var5) {
         var5.loadFromBucketTag(param1.getOrCreateTag());
         var5.setFromBucket(true);
      }
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      if (this.type == EntityType.TROPICAL_FISH) {
         CompoundTag var5 = param0.getTag();
         if (var5 != null && var5.contains("BucketVariantTag", 3)) {
            int var6 = var5.getInt("BucketVariantTag");
            ChatFormatting[] var7 = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
            String var8 = "color.minecraft." + TropicalFish.getBaseColor(var6);
            String var9 = "color.minecraft." + TropicalFish.getPatternColor(var6);

            for (int var10 = 0; var10 < TropicalFish.COMMON_VARIANTS.size(); var10++) {
               if (var6 == TropicalFish.COMMON_VARIANTS.get(var10).getPackedId()) {
                  param2.add(Component.translatable(TropicalFish.getPredefinedName(var10)).withStyle(var7));
                  return;
               }
            }

            param2.add(TropicalFish.getPattern(var6).displayName().plainCopy().withStyle(var7));
            MutableComponent var10 = Component.translatable(var8);
            if (!var8.equals(var9)) {
               var10.append(", ").append(Component.translatable(var9));
            }

            var10.withStyle(var7);
            param2.add(var10);
         }
      }
   }
}
