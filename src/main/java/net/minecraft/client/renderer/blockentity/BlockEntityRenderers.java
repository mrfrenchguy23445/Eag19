package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRenderers {
   private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> PROVIDERS = Maps.newHashMap();

   private static <T extends BlockEntity> void register(BlockEntityType<? extends T> param0, BlockEntityRendererProvider<T> param1) {
      PROVIDERS.put(param0, param1);
   }

   public static Map<BlockEntityType<?>, BlockEntityRenderer<?>> createEntityRenderers(BlockEntityRendererProvider.Context param0) {
      Builder<BlockEntityType<?>, BlockEntityRenderer<?>> var1 = ImmutableMap.builder();
      PROVIDERS.forEach((l_param2, l_param3) -> {
         try {
            var1.put((BlockEntityType<?>)l_param2, l_param3.create(param0));
         } catch (Exception l_var4) {
            throw new IllegalStateException("Failed to create model for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)l_param2), l_var4);
         }
      });
      return var1.build();
   }

   static {
      register(BlockEntityType.SIGN, SignRenderer::new);
      register(BlockEntityType.HANGING_SIGN, HangingSignRenderer::new);
      register(BlockEntityType.MOB_SPAWNER, SpawnerRenderer::new);
      register(BlockEntityType.PISTON, PistonHeadRenderer::new);
      register(BlockEntityType.CHEST, ChestRenderer::new);
      register(BlockEntityType.ENDER_CHEST, ChestRenderer::new);
      register(BlockEntityType.TRAPPED_CHEST, ChestRenderer::new);
      register(BlockEntityType.ENCHANTING_TABLE, EnchantTableRenderer::new);
      register(BlockEntityType.LECTERN, LecternRenderer::new);
      register(BlockEntityType.END_PORTAL, TheEndPortalRenderer::new);
      register(BlockEntityType.END_GATEWAY, TheEndGatewayRenderer::new);
      register(BlockEntityType.BEACON, BeaconRenderer::new);
      register(BlockEntityType.SKULL, SkullBlockRenderer::new);
      register(BlockEntityType.BANNER, BannerRenderer::new);
      register(BlockEntityType.STRUCTURE_BLOCK, StructureBlockRenderer::new);
      register(BlockEntityType.SHULKER_BOX, ShulkerBoxRenderer::new);
      register(BlockEntityType.BED, BedRenderer::new);
      register(BlockEntityType.CONDUIT, ConduitRenderer::new);
      register(BlockEntityType.BELL, BellRenderer::new);
      register(BlockEntityType.CAMPFIRE, CampfireRenderer::new);
      register(BlockEntityType.SUSPICIOUS_SAND, SuspiciousSandRenderer::new);
      register(BlockEntityType.DECORATED_POT, DecoratedPotRenderer::new);
   }
}
