package net.minecraft.client.tutorial;

import java.util.function.Function;

public enum TutorialSteps {
   MOVEMENT("movement", MovementTutorialStepInstance::new),
   FIND_TREE("find_tree", FindTreeTutorialStepInstance::new),
   PUNCH_TREE("punch_tree", PunchTreeTutorialStepInstance::new),
   OPEN_INVENTORY("open_inventory", OpenInventoryTutorialStep::new),
   CRAFT_PLANKS("craft_planks", CraftPlanksTutorialStep::new),
   NONE("none", CompletedTutorialStepInstance::new);

   private final String name;
   private final Function<Tutorial, ? extends TutorialStepInstance> constructor;

   <T extends TutorialStepInstance> TutorialSteps(String param2, Function<Tutorial, T> param3) {
      this.name = param2;
      this.constructor = param3;
   }

   public TutorialStepInstance create(Tutorial param0) {
      return this.constructor.apply(param0);
   }

   public String getName() {
      return this.name;
   }

   public static TutorialSteps getByName(String param0) {
      for (TutorialSteps var4 : values()) {
         if (var4.name.equals(param0)) {
            return var4;
         }
      }

      return NONE;
   }
}
