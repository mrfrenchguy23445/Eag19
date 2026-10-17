package net.minecraft.client.tutorial;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface TutorialStepInstance {
   default void clear() {
   }

   default void tick() {
   }

   default void onInput(Input param0) {
   }

   default void onMouse(double param0, double param2) {
   }

   default void onLookAt(ClientLevel param0, HitResult param1) {
   }

   default void onDestroyBlock(ClientLevel param0, BlockPos param1, BlockState param2, float param3) {
   }

   default void onOpenInventory() {
   }

   default void onGetItem(ItemStack param0) {
   }
}
