package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;

public class EndGatewayFeature extends Feature<EndGatewayConfiguration> {
   public EndGatewayFeature(Codec<EndGatewayConfiguration> param0) {
      super(param0);
   }

   @Override
   public boolean place(FeaturePlaceContext<EndGatewayConfiguration> param0) {
      BlockPos var2 = param0.origin();
      WorldGenLevel var3 = param0.level();
      EndGatewayConfiguration var4 = param0.config();

      for (BlockPos var6 : BlockPos.betweenClosed(var2.offset(-1, -2, -1), var2.offset(1, 2, 1))) {
         boolean var7 = var6.getX() == var2.getX();
         boolean var8 = var6.getY() == var2.getY();
         boolean var9 = var6.getZ() == var2.getZ();
         boolean var10 = Math.abs(var6.getY() - var2.getY()) == 2;
         if (var7 && var8 && var9) {
            BlockPos var11 = var6.immutable();
            this.setBlock(var3, var11, Blocks.END_GATEWAY.defaultBlockState());
            var4.getExit().ifPresent(l_param3 -> {
               BlockEntity l_var4 = var3.getBlockEntity(var11);
               if (l_var4 instanceof TheEndGatewayBlockEntity l_var5) {
                  l_var5.setExitPosition(l_param3, var4.isExitExact());
                  l_var4.setChanged();
               }
            });
         } else if (var8) {
            this.setBlock(var3, var6, Blocks.AIR.defaultBlockState());
         } else if (var10 && var7 && var9) {
            this.setBlock(var3, var6, Blocks.BEDROCK.defaultBlockState());
         } else if ((var7 || var9) && !var10) {
            this.setBlock(var3, var6, Blocks.BEDROCK.defaultBlockState());
         } else {
            this.setBlock(var3, var6, Blocks.AIR.defaultBlockState());
         }
      }

      return true;
   }
}
