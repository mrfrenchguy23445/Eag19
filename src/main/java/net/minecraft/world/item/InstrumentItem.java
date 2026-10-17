package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
   private static final String TAG_INSTRUMENT = "instrument";
   private final TagKey<Instrument> instruments;

   public InstrumentItem(Item.Properties param0, TagKey<Instrument> param1) {
      super(param0);
      this.instruments = param1;
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      super.appendHoverText(param0, param1, param2, param3);
      Optional<ResourceKey<Instrument>> var5 = this.getInstrument(param0).flatMap(Holder::unwrapKey);
      if (var5.isPresent()) {
         MutableComponent var6 = Component.translatable(Util.makeDescriptionId("instrument", var5.get().location()));
         param2.add(var6.withStyle(ChatFormatting.GRAY));
      }
   }

   public static ItemStack create(Item param0, Holder<Instrument> param1) {
      ItemStack var2 = new ItemStack(param0);
      setSoundVariantId(var2, param1);
      return var2;
   }

   public static void setRandom(ItemStack param0, TagKey<Instrument> param1, RandomSource param2) {
      Optional<Holder<Instrument>> var3 = BuiltInRegistries.INSTRUMENT.getTag(param1).flatMap(l_param1 -> l_param1.getRandomElement(param2));
      var3.ifPresent(l_param1 -> setSoundVariantId(param0, (Holder<Instrument>)l_param1));
   }

   private static void setSoundVariantId(ItemStack param0, Holder<Instrument> param1) {
      CompoundTag var2 = param0.getOrCreateTag();
      var2.putString("instrument", param1.unwrapKey().orElseThrow(() -> new IllegalStateException("Invalid instrument")).location().toString());
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      ItemStack var4 = param1.getItemInHand(param2);
      Optional<? extends Holder<Instrument>> var5 = this.getInstrument(var4);
      if (var5.isPresent()) {
         Instrument var6 = var5.get().value();
         param1.startUsingItem(param2);
         play(param0, param1, var6);
         param1.getCooldowns().addCooldown(this, var6.useDuration());
         param1.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.consume(var4);
      } else {
         return InteractionResultHolder.fail(var4);
      }
   }

   @Override
   public int getUseDuration(ItemStack param0) {
      Optional<? extends Holder<Instrument>> var2 = this.getInstrument(param0);
      return var2.<Integer>map(l_param0 -> l_param0.value().useDuration()).orElse(0);
   }

   private Optional<? extends Holder<Instrument>> getInstrument(ItemStack param0) {
      CompoundTag var2 = param0.getTag();
      if (var2 != null && var2.contains("instrument", 8)) {
         ResourceLocation var3 = ResourceLocation.tryParse(var2.getString("instrument"));
         if (var3 != null) {
            return BuiltInRegistries.INSTRUMENT.getHolder(ResourceKey.create(Registries.INSTRUMENT, var3));
         }
      }

      Iterator<Holder<Instrument>> var3 = BuiltInRegistries.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
      return var3.hasNext() ? Optional.of(var3.next()) : Optional.empty();
   }

   @Override
   public UseAnim getUseAnimation(ItemStack param0) {
      return UseAnim.TOOT_HORN;
   }

   private static void play(Level param0, Player param1, Instrument param2) {
      SoundEvent var3 = param2.soundEvent().value();
      float var4 = param2.range() / 16.0F;
      param0.playSound(param1, param1, var3, SoundSource.RECORDS, var4, 1.0F);
      param0.gameEvent(GameEvent.INSTRUMENT_PLAY, param1.position(), GameEvent.Context.of(param1));
   }
}
