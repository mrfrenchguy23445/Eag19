package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;

public class SpikeConfiguration implements FeatureConfiguration {
   public static final Codec<SpikeConfiguration> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            Codec.BOOL.fieldOf("crystal_invulnerable").orElse(false).forGetter(l_param0 -> l_param0.crystalInvulnerable),
            SpikeFeature.EndSpike.CODEC.listOf().fieldOf("spikes").forGetter(l_param0 -> l_param0.spikes),
            BlockPos.CODEC.optionalFieldOf("crystal_beam_target").forGetter(l_param0 -> Optional.ofNullable(l_param0.crystalBeamTarget))
         )
         .apply(param0, SpikeConfiguration::new)
   );
   private final boolean crystalInvulnerable;
   private final List<SpikeFeature.EndSpike> spikes;
   @Nullable
   private final BlockPos crystalBeamTarget;

   public SpikeConfiguration(boolean param0, List<SpikeFeature.EndSpike> param1, @Nullable BlockPos param2) {
      this(param0, param1, Optional.ofNullable(param2));
   }

   private SpikeConfiguration(boolean param0, List<SpikeFeature.EndSpike> param1, Optional<BlockPos> param2) {
      this.crystalInvulnerable = param0;
      this.spikes = param1;
      this.crystalBeamTarget = param2.orElse(null);
   }

   public boolean isCrystalInvulnerable() {
      return this.crystalInvulnerable;
   }

   public List<SpikeFeature.EndSpike> getSpikes() {
      return this.spikes;
   }

   @Nullable
   public BlockPos getCrystalBeamTarget() {
      return this.crystalBeamTarget;
   }
}
