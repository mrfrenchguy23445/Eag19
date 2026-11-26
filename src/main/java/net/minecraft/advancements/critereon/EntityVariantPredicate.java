package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVariantPredicate<V> {
   private static final String VARIANT_KEY = "variant";
   final Codec<V> variantCodec;
   final Function<Entity, Optional<V>> getter;
   final EntitySubPredicate.Type type;

   public static <V> EntityVariantPredicate<V> create(Registry<V> param0, Function<Entity, Optional<V>> param1) {
      return new EntityVariantPredicate<>(param0.byNameCodec(), param1);
   }

   public static <V> EntityVariantPredicate<V> create(Codec<V> param0, Function<Entity, Optional<V>> param1) {
      return new EntityVariantPredicate<>(param0, param1);
   }

   private EntityVariantPredicate(Codec<V> param0, Function<Entity, Optional<V>> param1) {
      this.variantCodec = param0;
      this.getter = param1;
      this.type = l_param1 -> {
         JsonElement l_var3 = l_param1.get("variant");
         if (l_var3 == null) {
            throw new JsonParseException("Missing variant field");
         }

         V l_var4 = Util.getOrThrow(param0.decode(new Dynamic<>(JsonOps.INSTANCE, l_var3)), JsonParseException::new).getFirst();
         return this.createPredicate(l_var4);
      };
   }

   public EntitySubPredicate.Type type() {
      return this.type;
   }

   public EntitySubPredicate createPredicate(final V param0) {
      return new EntitySubPredicate() {
         @Override
         public boolean matches(Entity l_param0, ServerLevel l_param1, @Nullable Vec3 l_param2) {
            return EntityVariantPredicate.this.getter.apply(l_param0).filter(l2_param1 -> l2_param1.equals(param0)).isPresent();
         }

         @Override
         public JsonObject serializeCustomData() {
            JsonObject l_var1 = new JsonObject();
            l_var1.add(
               "variant",
               Util.getOrThrow(
                  EntityVariantPredicate.this.variantCodec.encodeStart(JsonOps.INSTANCE, param0),
                  l2_param1 -> new JsonParseException("Can't serialize variant " + param0 + ", message " + l2_param1)
               )
            );
            return l_var1;
         }

         @Override
         public EntitySubPredicate.Type type() {
            return EntityVariantPredicate.this.type;
         }
      };
   }
}
