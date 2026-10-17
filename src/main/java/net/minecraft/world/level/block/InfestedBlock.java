package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class InfestedBlock extends Block {
   private final Block hostBlock;
   private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
   private static final Map<BlockState, BlockState> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
   private static final Map<BlockState, BlockState> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

   public InfestedBlock(Block param0, BlockBehaviour.Properties param1) {
      super(param1.destroyTime(param0.defaultDestroyTime() / 2.0F).explosionResistance(0.75F));
      this.hostBlock = param0;
      BLOCK_BY_HOST_BLOCK.put(param0, this);
   }

   public Block getHostBlock() {
      return this.hostBlock;
   }

   public static boolean isCompatibleHostBlock(BlockState param0) {
      return BLOCK_BY_HOST_BLOCK.containsKey(param0.getBlock());
   }

   private void spawnInfestation(ServerLevel param0, BlockPos param1) {
      Silverfish var3 = EntityType.SILVERFISH.create(param0);
      if (var3 != null) {
         var3.moveTo(param1.getX() + 0.5, param1.getY(), param1.getZ() + 0.5, 0.0F, 0.0F);
         param0.addFreshEntity(var3);
         var3.spawnAnim();
      }
   }

   @Override
   public void spawnAfterBreak(BlockState param0, ServerLevel param1, BlockPos param2, ItemStack param3, boolean param4) {
      super.spawnAfterBreak(param0, param1, param2, param3, param4);
      if (param1.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, param3) == 0) {
         this.spawnInfestation(param1, param2);
      }
   }

   public static BlockState infestedStateByHost(BlockState param0) {
      return getNewStateWithProperties(HOST_TO_INFESTED_STATES, param0, () -> BLOCK_BY_HOST_BLOCK.get(param0.getBlock()).defaultBlockState());
   }

   public BlockState hostStateByInfested(BlockState param0) {
      return getNewStateWithProperties(INFESTED_TO_HOST_STATES, param0, () -> this.getHostBlock().defaultBlockState());
   }

   private static BlockState getNewStateWithProperties(Map<BlockState, BlockState> param0, BlockState param1, Supplier<BlockState> param2) {
      return param0.computeIfAbsent(param1, l_param1 -> {
         BlockState l_var2 = param2.get();

         for (Property l_var4 : l_param1.getProperties()) {
            l_var2 = l_var2.hasProperty(l_var4) ? l_var2.setValue(l_var4, l_param1.getValue(l_var4)) : l_var2;
         }

         return l_var2;
      });
   }
}
