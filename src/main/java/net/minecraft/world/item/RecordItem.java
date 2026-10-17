package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class RecordItem extends Item {
   private static final Map<SoundEvent, RecordItem> BY_NAME = Maps.newHashMap();
   private final int analogOutput;
   private final SoundEvent sound;
   private final int lengthInTicks;

   protected RecordItem(int param0, SoundEvent param1, Item.Properties param2, int param3) {
      super(param2);
      this.analogOutput = param0;
      this.sound = param1;
      this.lengthInTicks = param3 * 20;
      BY_NAME.put(this.sound, this);
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      Level var2 = param0.getLevel();
      BlockPos var3 = param0.getClickedPos();
      BlockState var4 = var2.getBlockState(var3);
      if (var4.is(Blocks.JUKEBOX) && !var4.getValue(JukeboxBlock.HAS_RECORD)) {
         ItemStack var5 = param0.getItemInHand();
         if (!var2.isClientSide) {
            Player var6 = param0.getPlayer();
            if (var2.getBlockEntity(var3) instanceof JukeboxBlockEntity var7) {
               var7.setFirstItem(var5.copy());
               var2.gameEvent(GameEvent.BLOCK_CHANGE, var3, GameEvent.Context.of(var6, var4));
            }

            var5.shrink(1);
            if (var6 != null) {
               var6.awardStat(Stats.PLAY_RECORD);
            }
         }

         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public int getAnalogOutput() {
      return this.analogOutput;
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      param2.add(this.getDisplayName().withStyle(ChatFormatting.GRAY));
   }

   public MutableComponent getDisplayName() {
      return Component.translatable(this.getDescriptionId() + ".desc");
   }

   @Nullable
   public static RecordItem getBySound(SoundEvent param0) {
      return BY_NAME.get(param0);
   }

   public SoundEvent getSound() {
      return this.sound;
   }

   public int getLengthInTicks() {
      return this.lengthInTicks;
   }
}
