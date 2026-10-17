package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DustColorTransitionOptions extends DustParticleOptionsBase {
   public static final Vector3f SCULK_PARTICLE_COLOR = Vec3.fromRGB24(3790560).toVector3f();
   public static final DustColorTransitionOptions SCULK_TO_REDSTONE = new DustColorTransitionOptions(
      SCULK_PARTICLE_COLOR, DustParticleOptions.REDSTONE_PARTICLE_COLOR, 1.0F
   );
   public static final Codec<DustColorTransitionOptions> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ExtraCodecs.VECTOR3F.fieldOf("fromColor").forGetter(l_param0 -> l_param0.color),
            ExtraCodecs.VECTOR3F.fieldOf("toColor").forGetter(l_param0 -> l_param0.toColor),
            Codec.FLOAT.fieldOf("scale").forGetter(l_param0 -> l_param0.scale)
         )
         .apply(param0, DustColorTransitionOptions::new)
   );
   public static final ParticleOptions.Deserializer<DustColorTransitionOptions> DESERIALIZER = new ParticleOptions.Deserializer<DustColorTransitionOptions>() {
      public DustColorTransitionOptions fromCommand(ParticleType<DustColorTransitionOptions> param0, StringReader param1) throws CommandSyntaxException {
         Vector3f var3 = DustParticleOptionsBase.readVector3f(param1);
         param1.expect(' ');
         float var4 = param1.readFloat();
         Vector3f var5 = DustParticleOptionsBase.readVector3f(param1);
         return new DustColorTransitionOptions(var3, var5, var4);
      }

      public DustColorTransitionOptions fromNetwork(ParticleType<DustColorTransitionOptions> param0, FriendlyByteBuf param1) {
         Vector3f var3 = DustParticleOptionsBase.readVector3f(param1);
         float var4 = param1.readFloat();
         Vector3f var5 = DustParticleOptionsBase.readVector3f(param1);
         return new DustColorTransitionOptions(var3, var5, var4);
      }
   };
   private final Vector3f toColor;

   public DustColorTransitionOptions(Vector3f param0, Vector3f param1, float param2) {
      super(param0, param2);
      this.toColor = param1;
   }

   public Vector3f getFromColor() {
      return this.color;
   }

   public Vector3f getToColor() {
      return this.toColor;
   }

   @Override
   public void writeToNetwork(FriendlyByteBuf param0) {
      super.writeToNetwork(param0);
      param0.writeFloat(this.toColor.x());
      param0.writeFloat(this.toColor.y());
      param0.writeFloat(this.toColor.z());
   }

   @Override
   public String writeToString() {
      return String.format(
         Locale.ROOT,
         "%s %.2f %.2f %.2f %.2f %.2f %.2f %.2f",
         BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()),
         this.color.x(),
         this.color.y(),
         this.color.z(),
         this.scale,
         this.toColor.x(),
         this.toColor.y(),
         this.toColor.z()
      );
   }

   @Override
   public ParticleType<DustColorTransitionOptions> getType() {
      return ParticleTypes.DUST_COLOR_TRANSITION;
   }
}
