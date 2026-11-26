package net.minecraft.world.level.gameevent;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityPositionSource implements PositionSource {
   public static final Codec<EntityPositionSource> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            UUIDUtil.CODEC.fieldOf("source_entity").forGetter(EntityPositionSource::getUuid),
            Codec.FLOAT.fieldOf("y_offset").orElse(0.0F).forGetter(l_param0 -> l_param0.yOffset)
         )
         .apply(param0, (l_param0, l_param1) -> new EntityPositionSource(Either.right(Either.left(l_param0)), l_param1))
   );
   private Either<Entity, Either<UUID, Integer>> entityOrUuidOrId;
   final float yOffset;

   public EntityPositionSource(Entity param0, float param1) {
      this(Either.left(param0), param1);
   }

   EntityPositionSource(Either<Entity, Either<UUID, Integer>> param0, float param1) {
      this.entityOrUuidOrId = param0;
      this.yOffset = param1;
   }

   @Override
   public Optional<Vec3> getPosition(Level param0) {
      if (this.entityOrUuidOrId.left().isEmpty()) {
         this.resolveEntity(param0);
      }

      return this.entityOrUuidOrId.left().map(l_param0 -> l_param0.position().add(0.0, this.yOffset, 0.0));
   }

   private void resolveEntity(Level param0) {
      this.entityOrUuidOrId
         .map(
            Optional::of,
            l_param1 -> Optional.ofNullable(
               l_param1.map(l2_param1 -> param0 instanceof ServerLevel l2_var2 ? l2_var2.getEntity(l2_param1) : null, param0::getEntity)
            )
         )
         .ifPresent(l_param0 -> this.entityOrUuidOrId = Either.left(l_param0));
   }

   private UUID getUuid() {
      return this.entityOrUuidOrId.map(Entity::getUUID, l_param0 -> l_param0.map(Function.identity(), l2_param0 -> {
         throw new RuntimeException("Unable to get entityId from uuid");
      }));
   }

   int getId() {
      return this.entityOrUuidOrId.map(Entity::getId, l_param0 -> l_param0.map(l2_param0 -> {
         throw new IllegalStateException("Unable to get entityId from uuid");
      }, Function.identity()));
   }

   @Override
   public PositionSourceType<?> getType() {
      return PositionSourceType.ENTITY;
   }

   public static class Type implements PositionSourceType<EntityPositionSource> {
      public EntityPositionSource read(FriendlyByteBuf param0) {
         return new EntityPositionSource(Either.right(Either.right(param0.readVarInt())), param0.readFloat());
      }

      public void write(FriendlyByteBuf param0, EntityPositionSource param1) {
         param0.writeVarInt(param1.getId());
         param0.writeFloat(param1.yOffset);
      }

      @Override
      public Codec<EntityPositionSource> codec() {
         return EntityPositionSource.CODEC;
      }
   }
}
