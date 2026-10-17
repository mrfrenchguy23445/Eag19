package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Giant;

public class GiantZombieModel extends AbstractZombieModel<Giant> {
   public GiantZombieModel(ModelPart param0) {
      super(param0);
   }

   public boolean isAggressive(Giant param0) {
      return false;
   }
}
