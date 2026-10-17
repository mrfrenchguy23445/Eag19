package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(ResourceLocation param0, @Nullable String param1) {
      this.subtitle = param1 == null ? null : Component.translatable(param1);
   }

   @Override
   public int getWeight() {
      int var1 = 0;

      for (Weighted<Sound> var3 : this.list) {
         var1 += var3.getWeight();
      }

      return var1;
   }

   public Sound getSound(RandomSource param0) {
      int var2 = this.getWeight();
      if (!this.list.isEmpty() && var2 != 0) {
         int var3 = param0.nextInt(var2);

         for (Weighted<Sound> var5 : this.list) {
            var3 -= var5.getWeight();
            if (var3 < 0) {
               return var5.getSound(param0);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> param0) {
      this.list.add(param0);
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(SoundEngine param0) {
      for (Weighted<Sound> var3 : this.list) {
         var3.preloadIfRequired(param0);
      }
   }
}
