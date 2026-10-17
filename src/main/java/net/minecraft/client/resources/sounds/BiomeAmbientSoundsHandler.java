package net.minecraft.client.resources.sounds;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

public class BiomeAmbientSoundsHandler implements AmbientSoundHandler {
   private static final int LOOP_SOUND_CROSS_FADE_TIME = 40;
   private static final float SKY_MOOD_RECOVERY_RATE = 0.001F;
   private final LocalPlayer player;
   private final SoundManager soundManager;
   private final BiomeManager biomeManager;
   private final RandomSource random;
   private final Object2ObjectArrayMap<Biome, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds = new Object2ObjectArrayMap<>();
   private Optional<AmbientMoodSettings> moodSettings = Optional.empty();
   private Optional<AmbientAdditionsSettings> additionsSettings = Optional.empty();
   private float moodiness;
   @Nullable
   private Biome previousBiome;

   public BiomeAmbientSoundsHandler(LocalPlayer param0, SoundManager param1, BiomeManager param2) {
      this.random = param0.level.getRandom();
      this.player = param0;
      this.soundManager = param1;
      this.biomeManager = param2;
   }

   public float getMoodiness() {
      return this.moodiness;
   }

   @Override
   public void tick() {
      this.loopSounds.values().removeIf(AbstractTickableSoundInstance::isStopped);
      Biome var1 = this.biomeManager.getNoiseBiomeAtPosition(this.player.getX(), this.player.getY(), this.player.getZ()).value();
      if (var1 != this.previousBiome) {
         this.previousBiome = var1;
         this.moodSettings = var1.getAmbientMood();
         this.additionsSettings = var1.getAmbientAdditions();
         this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
         var1.getAmbientLoop().ifPresent(l_param1 -> this.loopSounds.compute(var1, (l2_param1, l2_param2) -> {
            if (l2_param2 == null) {
               l2_param2 = new BiomeAmbientSoundsHandler.LoopSoundInstance((SoundEvent)l_param1.value());
               this.soundManager.play(l2_param2);
            }

            l2_param2.fadeIn();
            return (BiomeAmbientSoundsHandler.LoopSoundInstance)l2_param2;
         }));
      }

      this.additionsSettings.ifPresent(l_param0 -> {
         if (this.random.nextDouble() < l_param0.getTickChance()) {
            this.soundManager.play(SimpleSoundInstance.forAmbientAddition(l_param0.getSoundEvent().value()));
         }
      });
      this.moodSettings
         .ifPresent(
            l_param0 -> {
               Level l_var2 = this.player.level;
               int l_var3 = l_param0.getBlockSearchExtent() * 2 + 1;
               BlockPos l_var4 = BlockPos.containing(
                  this.player.getX() + this.random.nextInt(l_var3) - l_param0.getBlockSearchExtent(),
                  this.player.getEyeY() + this.random.nextInt(l_var3) - l_param0.getBlockSearchExtent(),
                  this.player.getZ() + this.random.nextInt(l_var3) - l_param0.getBlockSearchExtent()
               );
               int l_var5 = l_var2.getBrightness(LightLayer.SKY, l_var4);
               if (l_var5 > 0) {
                  this.moodiness = this.moodiness - (float)l_var5 / l_var2.getMaxLightLevel() * 0.001F;
               } else {
                  this.moodiness = this.moodiness - (float)(l_var2.getBrightness(LightLayer.BLOCK, l_var4) - 1) / l_param0.getTickDelay();
               }

               if (this.moodiness >= 1.0F) {
                  double l_var6 = l_var4.getX() + 0.5;
                  double l_var8 = l_var4.getY() + 0.5;
                  double l_var10 = l_var4.getZ() + 0.5;
                  double l_var12 = l_var6 - this.player.getX();
                  double l_var14 = l_var8 - this.player.getEyeY();
                  double l_var16 = l_var10 - this.player.getZ();
                  double l_var18 = Math.sqrt(l_var12 * l_var12 + l_var14 * l_var14 + l_var16 * l_var16);
                  double l_var20 = l_var18 + l_param0.getSoundPositionOffset();
                  SimpleSoundInstance l_var22 = SimpleSoundInstance.forAmbientMood(
                     l_param0.getSoundEvent().value(),
                     this.random,
                     this.player.getX() + l_var12 / l_var18 * l_var20,
                     this.player.getEyeY() + l_var14 / l_var18 * l_var20,
                     this.player.getZ() + l_var16 / l_var18 * l_var20
                  );
                  this.soundManager.play(l_var22);
                  this.moodiness = 0.0F;
               } else {
                  this.moodiness = Math.max(this.moodiness, 0.0F);
               }
            }
         );
   }

   public static class LoopSoundInstance extends AbstractTickableSoundInstance {
      private int fadeDirection;
      private int fade;

      public LoopSoundInstance(SoundEvent param0) {
         super(param0, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
         this.looping = true;
         this.delay = 0;
         this.volume = 1.0F;
         this.relative = true;
      }

      @Override
      public void tick() {
         if (this.fade < 0) {
            this.stop();
         }

         this.fade = this.fade + this.fadeDirection;
         this.volume = Mth.clamp(this.fade / 40.0F, 0.0F, 1.0F);
      }

      public void fadeOut() {
         this.fade = Math.min(this.fade, 40);
         this.fadeDirection = -1;
      }

      public void fadeIn() {
         this.fade = Math.max(0, this.fade);
         this.fadeDirection = 1;
      }
   }
}
