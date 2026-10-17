package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;

public class WorldOptions {
   public static final MapCodec<WorldOptions> CODEC = RecordCodecBuilder.mapCodec(
      param0 -> param0.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(WorldOptions::seed),
            Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldOptions::generateStructures),
            Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldOptions::generateBonusChest),
            Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter(l_param0 -> l_param0.legacyCustomOptions)
         )
         .apply(param0, param0.stable(WorldOptions::new))
   );
   public static final WorldOptions DEMO_OPTIONS = new WorldOptions("North Carolina".hashCode(), true, true);
   private final long seed;
   private final boolean generateStructures;
   private final boolean generateBonusChest;
   private final Optional<String> legacyCustomOptions;

   public WorldOptions(long param0, boolean param2, boolean param3) {
      this(param0, param2, param3, Optional.empty());
   }

   public static WorldOptions defaultWithRandomSeed() {
      return new WorldOptions(randomSeed(), true, false);
   }

   private WorldOptions(long param0, boolean param2, boolean param3, Optional<String> param4) {
      this.seed = param0;
      this.generateStructures = param2;
      this.generateBonusChest = param3;
      this.legacyCustomOptions = param4;
   }

   public long seed() {
      return this.seed;
   }

   public boolean generateStructures() {
      return this.generateStructures;
   }

   public boolean generateBonusChest() {
      return this.generateBonusChest;
   }

   public boolean isOldCustomizedWorld() {
      return this.legacyCustomOptions.isPresent();
   }

   public WorldOptions withBonusChest(boolean param0) {
      return new WorldOptions(this.seed, this.generateStructures, param0, this.legacyCustomOptions);
   }

   public WorldOptions withStructures(boolean param0) {
      return new WorldOptions(this.seed, param0, this.generateBonusChest, this.legacyCustomOptions);
   }

   public WorldOptions withSeed(OptionalLong param0) {
      return new WorldOptions(param0.orElse(randomSeed()), this.generateStructures, this.generateBonusChest, this.legacyCustomOptions);
   }

   public static OptionalLong parseSeed(String param0) {
      param0 = param0.trim();
      if (StringUtils.isEmpty(param0)) {
         return OptionalLong.empty();
      }

      try {
         return OptionalLong.of(Long.parseLong(param0));
      } catch (NumberFormatException var1) {
         return OptionalLong.of(param0.hashCode());
      }
   }

   public static long randomSeed() {
      return RandomSource.create().nextLong();
   }
}
