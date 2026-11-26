package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LighthingBoltPredicate implements EntitySubPredicate {
   private static final String BLOCKS_SET_ON_FIRE_KEY = "blocks_set_on_fire";
   private static final String ENTITY_STRUCK_KEY = "entity_struck";
   private final MinMaxBounds.Ints blocksSetOnFire;
   private final EntityPredicate entityStruck;

   private LighthingBoltPredicate(MinMaxBounds.Ints param0, EntityPredicate param1) {
      this.blocksSetOnFire = param0;
      this.entityStruck = param1;
   }

   public static LighthingBoltPredicate blockSetOnFire(MinMaxBounds.Ints param0) {
      return new LighthingBoltPredicate(param0, EntityPredicate.ANY);
   }

   public static LighthingBoltPredicate fromJson(JsonObject param0) {
      return new LighthingBoltPredicate(MinMaxBounds.Ints.fromJson(param0.get("blocks_set_on_fire")), EntityPredicate.fromJson(param0.get("entity_struck")));
   }

   @Override
   public JsonObject serializeCustomData() {
      JsonObject var1 = new JsonObject();
      var1.add("blocks_set_on_fire", this.blocksSetOnFire.serializeToJson());
      var1.add("entity_struck", this.entityStruck.serializeToJson());
      return var1;
   }

   @Override
   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.LIGHTNING;
   }

   @Override
   public boolean matches(Entity param0, ServerLevel param1, @Nullable Vec3 param2) {
      return !(param0 instanceof LightningBolt var4)
         ? false
         : this.blocksSetOnFire.matches(var4.getBlocksSetOnFire())
            && (this.entityStruck == EntityPredicate.ANY || var4.getHitEntities().anyMatch(l_param2 -> this.entityStruck.matches(param1, param2, l_param2)));
   }
}
