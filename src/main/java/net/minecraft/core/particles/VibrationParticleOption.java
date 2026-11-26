package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.phys.Vec3;

public class VibrationParticleOption implements ParticleOptions {
   public static final Codec<VibrationParticleOption> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            PositionSource.CODEC.fieldOf("destination").forGetter(l_param0 -> l_param0.destination),
            Codec.INT.fieldOf("arrival_in_ticks").forGetter(l_param0 -> l_param0.arrivalInTicks)
         )
         .apply(param0, VibrationParticleOption::new)
   );
   public static final ParticleOptions.Deserializer<VibrationParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<VibrationParticleOption>() {
      public VibrationParticleOption fromCommand(ParticleType<VibrationParticleOption> param0, StringReader param1) throws CommandSyntaxException {
         param1.expect(' ');
         float var3 = (float)param1.readDouble();
         param1.expect(' ');
         float var4 = (float)param1.readDouble();
         param1.expect(' ');
         float var5 = (float)param1.readDouble();
         param1.expect(' ');
         int var6 = param1.readInt();
         BlockPos var7 = BlockPos.containing(var3, var4, var5);
         return new VibrationParticleOption(new BlockPositionSource(var7), var6);
      }

      public VibrationParticleOption fromNetwork(ParticleType<VibrationParticleOption> param0, FriendlyByteBuf param1) {
         PositionSource var3 = PositionSourceType.fromNetwork(param1);
         int var4 = param1.readVarInt();
         return new VibrationParticleOption(var3, var4);
      }
   };
   private final PositionSource destination;
   private final int arrivalInTicks;

   public VibrationParticleOption(PositionSource param0, int param1) {
      this.destination = param0;
      this.arrivalInTicks = param1;
   }

   @Override
   public void writeToNetwork(FriendlyByteBuf param0) {
      PositionSourceType.toNetwork(this.destination, param0);
      param0.writeVarInt(this.arrivalInTicks);
   }

   @Override
   public String writeToString() {
      Vec3 var1 = this.destination.getPosition(null).get();
      double var2 = var1.x();
      double var4 = var1.y();
      double var6 = var1.z();
      return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %d", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), var2, var4, var6, this.arrivalInTicks);
   }

   @Override
   public ParticleType<VibrationParticleOption> getType() {
      return ParticleTypes.VIBRATION;
   }

   public PositionSource getDestination() {
      return this.destination;
   }

   public int getArrivalInTicks() {
      return this.arrivalInTicks;
   }
}
