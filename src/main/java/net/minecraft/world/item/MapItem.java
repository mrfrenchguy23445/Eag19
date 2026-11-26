package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapItem extends ComplexItem {
   public static final int IMAGE_WIDTH = 128;
   public static final int IMAGE_HEIGHT = 128;
   private static final int DEFAULT_MAP_COLOR = -12173266;
   private static final String TAG_MAP = "map";
   public static final String MAP_SCALE_TAG = "map_scale_direction";
   public static final String MAP_LOCK_TAG = "map_to_lock";

   public MapItem(Item.Properties param0) {
      super(param0);
   }

   public static ItemStack create(Level param0, int param1, int param2, byte param3, boolean param4, boolean param5) {
      ItemStack var6 = new ItemStack(Items.FILLED_MAP);
      createAndStoreSavedData(var6, param0, param1, param2, param3, param4, param5, param0.dimension());
      return var6;
   }

   @Nullable
   public static MapItemSavedData getSavedData(@Nullable Integer param0, Level param1) {
      return param0 == null ? null : param1.getMapData(makeKey(param0));
   }

   @Nullable
   public static MapItemSavedData getSavedData(ItemStack param0, Level param1) {
      Integer var2 = getMapId(param0);
      return getSavedData(var2, param1);
   }

   @Nullable
   public static Integer getMapId(ItemStack param0) {
      CompoundTag var1 = param0.getTag();
      return var1 != null && var1.contains("map", 99) ? var1.getInt("map") : null;
   }

   private static int createNewSavedData(Level param0, int param1, int param2, int param3, boolean param4, boolean param5, ResourceKey<Level> param6) {
      MapItemSavedData var7 = MapItemSavedData.createFresh(param1, param2, (byte)param3, param4, param5, param6);
      int var8 = param0.getFreeMapId();
      param0.setMapData(makeKey(var8), var7);
      return var8;
   }

   private static void storeMapData(ItemStack param0, int param1) {
      param0.getOrCreateTag().putInt("map", param1);
   }

   private static void createAndStoreSavedData(
      ItemStack param0, Level param1, int param2, int param3, int param4, boolean param5, boolean param6, ResourceKey<Level> param7
   ) {
      int var8 = createNewSavedData(param1, param2, param3, param4, param5, param6, param7);
      storeMapData(param0, var8);
   }

   public static String makeKey(int param0) {
      return "map_" + param0;
   }

   public void update(Level param0, Entity param1, MapItemSavedData param2) {
      if (param0.dimension() == param2.dimension && param1 instanceof Player) {
         int var4 = 1 << param2.scale;
         int var5 = param2.centerX;
         int var6 = param2.centerZ;
         int var7 = Mth.floor(param1.getX() - var5) / var4 + 64;
         int var8 = Mth.floor(param1.getZ() - var6) / var4 + 64;
         int var9 = 128 / var4;
         if (param0.dimensionType().hasCeiling()) {
            var9 /= 2;
         }

         MapItemSavedData.HoldingPlayer var10 = param2.getHoldingPlayer((Player)param1);
         var10.step++;
         BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos var12 = new BlockPos.MutableBlockPos();
         boolean var13 = false;

         for (int var14 = var7 - var9 + 1; var14 < var7 + var9; var14++) {
            if ((var14 & 15) == (var10.step & 15) || var13) {
               var13 = false;
               double var15 = 0.0;

               for (int var17 = var8 - var9 - 1; var17 < var8 + var9; var17++) {
                  if (var14 >= 0 && var17 >= -1 && var14 < 128 && var17 < 128) {
                     int var18 = Mth.square(var14 - var7) + Mth.square(var17 - var8);
                     boolean var19 = var18 > (var9 - 2) * (var9 - 2);
                     int var20 = (var5 / var4 + var14 - 64) * var4;
                     int var21 = (var6 / var4 + var17 - 64) * var4;
                     Multiset<MaterialColor> var22 = LinkedHashMultiset.create();
                     LevelChunk var23 = param0.getChunk(SectionPos.blockToSectionCoord(var20), SectionPos.blockToSectionCoord(var21));
                     if (!var23.isEmpty()) {
                        int var24 = 0;
                        double var25 = 0.0;
                        if (param0.dimensionType().hasCeiling()) {
                           int var27 = var20 + var21 * 231871;
                           var27 = var27 * var27 * 31287121 + var27 * 11;
                           if ((var27 >> 20 & 1) == 0) {
                              var22.add(Blocks.DIRT.defaultBlockState().getMapColor(param0, BlockPos.ZERO), 10);
                           } else {
                              var22.add(Blocks.STONE.defaultBlockState().getMapColor(param0, BlockPos.ZERO), 100);
                           }

                           var25 = 100.0;
                        } else {
                           for (int var27 = 0; var27 < var4; var27++) {
                              for (int var28 = 0; var28 < var4; var28++) {
                                 var11.set(var20 + var27, 0, var21 + var28);
                                 int var29 = var23.getHeight(Heightmap.Types.WORLD_SURFACE, var11.getX(), var11.getZ()) + 1;
                                 BlockState var30;
                                 if (var29 <= param0.getMinBuildHeight() + 1) {
                                    var30 = Blocks.BEDROCK.defaultBlockState();
                                 } else {
                                    do {
                                       var11.setY(--var29);
                                       var30 = var23.getBlockState(var11);
                                    } while (var30.getMapColor(param0, var11) == MaterialColor.NONE && var29 > param0.getMinBuildHeight());

                                    if (var29 > param0.getMinBuildHeight() && !var30.getFluidState().isEmpty()) {
                                       int var31 = var29 - 1;
                                       var12.set(var11);

                                       BlockState var32;
                                       do {
                                          var12.setY(var31--);
                                          var32 = var23.getBlockState(var12);
                                          var24++;
                                       } while (var31 > param0.getMinBuildHeight() && !var32.getFluidState().isEmpty());

                                       var30 = this.getCorrectStateForFluidBlock(param0, var30, var11);
                                    }
                                 }

                                 param2.checkBanners(param0, var11.getX(), var11.getZ());
                                 var25 += (double)var29 / (var4 * var4);
                                 var22.add(var30.getMapColor(param0, var11));
                              }
                           }
                        }

                        var24 /= var4 * var4;
                        MaterialColor var27 = Iterables.getFirst(Multisets.copyHighestCountFirst(var22), MaterialColor.NONE);
                        MaterialColor.Brightness var28;
                        if (var27 == MaterialColor.WATER) {
                           double var29 = var24 * 0.1 + (var14 + var17 & 1) * 0.2;
                           if (var29 < 0.5) {
                              var28 = MaterialColor.Brightness.HIGH;
                           } else if (var29 > 0.9) {
                              var28 = MaterialColor.Brightness.LOW;
                           } else {
                              var28 = MaterialColor.Brightness.NORMAL;
                           }
                        } else {
                           double var29 = (var25 - var15) * 4.0 / (var4 + 4) + ((var14 + var17 & 1) - 0.5) * 0.4;
                           if (var29 > 0.6) {
                              var28 = MaterialColor.Brightness.HIGH;
                           } else if (var29 < -0.6) {
                              var28 = MaterialColor.Brightness.LOW;
                           } else {
                              var28 = MaterialColor.Brightness.NORMAL;
                           }
                        }

                        var15 = var25;
                        if (var17 >= 0 && var18 < var9 * var9 && (!var19 || (var14 + var17 & 1) != 0)) {
                           var13 |= param2.updateColor(var14, var17, var27.getPackedId(var28));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState getCorrectStateForFluidBlock(Level param0, BlockState param1, BlockPos param2) {
      FluidState var4 = param1.getFluidState();
      return !var4.isEmpty() && !param1.isFaceSturdy(param0, param2, Direction.UP) ? var4.createLegacyBlock() : param1;
   }

   private static boolean isBiomeWatery(boolean[] param0, int param1, int param2) {
      return param0[param2 * 128 + param1];
   }

   public static void renderBiomePreviewMap(ServerLevel param0, ItemStack param1) {
      MapItemSavedData var2 = getSavedData(param1, param0);
      if (var2 != null) {
         if (param0.dimension() == var2.dimension) {
            int var3 = 1 << var2.scale;
            int var4 = var2.centerX;
            int var5 = var2.centerZ;
            boolean[] var6 = new boolean[16384];
            int var7 = var4 / var3 - 64;
            int var8 = var5 / var3 - 64;
            BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

            for (int var10 = 0; var10 < 128; var10++) {
               for (int var11 = 0; var11 < 128; var11++) {
                  Holder<Biome> var12 = param0.getBiome(var9.set((var7 + var11) * var3, 0, (var8 + var10) * var3));
                  var6[var10 * 128 + var11] = var12.is(BiomeTags.WATER_ON_MAP_OUTLINES);
               }
            }

            for (int var10 = 1; var10 < 127; var10++) {
               for (int var11 = 1; var11 < 127; var11++) {
                  int var12 = 0;

                  for (int var13 = -1; var13 < 2; var13++) {
                     for (int var14 = -1; var14 < 2; var14++) {
                        if ((var13 != 0 || var14 != 0) && isBiomeWatery(var6, var10 + var13, var11 + var14)) {
                           var12++;
                        }
                     }
                  }

                  MaterialColor.Brightness var13 = MaterialColor.Brightness.LOWEST;
                  MaterialColor var14 = MaterialColor.NONE;
                  if (isBiomeWatery(var6, var10, var11)) {
                     var14 = MaterialColor.COLOR_ORANGE;
                     if (var12 > 7 && var11 % 2 == 0) {
                        switch ((var10 + (int)(Mth.sin(var11 + 0.0F) * 7.0F)) / 8 % 5) {
                           case 0:
                           case 4:
                              var13 = MaterialColor.Brightness.LOW;
                              break;
                           case 1:
                           case 3:
                              var13 = MaterialColor.Brightness.NORMAL;
                              break;
                           case 2:
                              var13 = MaterialColor.Brightness.HIGH;
                        }
                     } else if (var12 > 7) {
                        var14 = MaterialColor.NONE;
                     } else if (var12 > 5) {
                        var13 = MaterialColor.Brightness.NORMAL;
                     } else if (var12 > 3) {
                        var13 = MaterialColor.Brightness.LOW;
                     } else if (var12 > 1) {
                        var13 = MaterialColor.Brightness.LOW;
                     }
                  } else if (var12 > 0) {
                     var14 = MaterialColor.COLOR_BROWN;
                     if (var12 > 3) {
                        var13 = MaterialColor.Brightness.NORMAL;
                     } else {
                        var13 = MaterialColor.Brightness.LOWEST;
                     }
                  }

                  if (var14 != MaterialColor.NONE) {
                     var2.setColor(var10, var11, var14.getPackedId(var13));
                  }
               }
            }
         }
      }
   }

   @Override
   public void inventoryTick(ItemStack param0, Level param1, Entity param2, int param3, boolean param4) {
      if (!param1.isClientSide) {
         MapItemSavedData var6 = getSavedData(param0, param1);
         if (var6 != null) {
            if (param2 instanceof Player var7) {
               var6.tickCarriedBy(var7, param0);
            }

            if (!var6.locked && (param4 || param2 instanceof Player && ((Player)param2).getOffhandItem() == param0)) {
               this.update(param1, param2, var6);
            }
         }
      }
   }

   @Nullable
   @Override
   public Packet<?> getUpdatePacket(ItemStack param0, Level param1, Player param2) {
      Integer var4 = getMapId(param0);
      MapItemSavedData var5 = getSavedData(var4, param1);
      return var5 != null ? var5.getUpdatePacket(var4, param2) : null;
   }

   @Override
   public void onCraftedBy(ItemStack param0, Level param1, Player param2) {
      CompoundTag var4 = param0.getTag();
      if (var4 != null && var4.contains("map_scale_direction", 99)) {
         scaleMap(param0, param1, var4.getInt("map_scale_direction"));
         var4.remove("map_scale_direction");
      } else if (var4 != null && var4.contains("map_to_lock", 1) && var4.getBoolean("map_to_lock")) {
         lockMap(param1, param0);
         var4.remove("map_to_lock");
      }
   }

   private static void scaleMap(ItemStack param0, Level param1, int param2) {
      MapItemSavedData var3 = getSavedData(param0, param1);
      if (var3 != null) {
         int var4 = param1.getFreeMapId();
         param1.setMapData(makeKey(var4), var3.scaled(param2));
         storeMapData(param0, var4);
      }
   }

   public static void lockMap(Level param0, ItemStack param1) {
      MapItemSavedData var2 = getSavedData(param1, param0);
      if (var2 != null) {
         int var3 = param0.getFreeMapId();
         String var4 = makeKey(var3);
         MapItemSavedData var5 = var2.locked();
         param0.setMapData(var4, var5);
         storeMapData(param1, var3);
      }
   }

   @Override
   public void appendHoverText(ItemStack param0, @Nullable Level param1, List<Component> param2, TooltipFlag param3) {
      Integer var5 = getMapId(param0);
      MapItemSavedData var6 = param1 == null ? null : getSavedData(var5, param1);
      CompoundTag var7 = param0.getTag();
      boolean var8;
      byte var9;
      if (var7 != null) {
         var8 = var7.getBoolean("map_to_lock");
         var9 = var7.getByte("map_scale_direction");
      } else {
         var8 = false;
         var9 = 0;
      }

      if (var6 != null && (var6.locked || var8)) {
         param2.add(Component.translatable("filled_map.locked", var5).withStyle(ChatFormatting.GRAY));
      }

      if (param3.isAdvanced()) {
         if (var6 != null) {
            if (!var8 && var9 == 0) {
               param2.add(Component.translatable("filled_map.id", var5).withStyle(ChatFormatting.GRAY));
            }

            int var10 = Math.min(var6.scale + var9, 4);
            param2.add(Component.translatable("filled_map.scale", 1 << var10).withStyle(ChatFormatting.GRAY));
            param2.add(Component.translatable("filled_map.level", var10, 4).withStyle(ChatFormatting.GRAY));
         } else {
            param2.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
         }
      }
   }

   public static int getColor(ItemStack param0) {
      CompoundTag var1 = param0.getTagElement("display");
      if (var1 != null && var1.contains("MapColor", 99)) {
         int var2 = var1.getInt("MapColor");
         return 0xFF000000 | var2 & 16777215;
      } else {
         return -12173266;
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext param0) {
      BlockState var2 = param0.getLevel().getBlockState(param0.getClickedPos());
      if (var2.is(BlockTags.BANNERS)) {
         if (!param0.getLevel().isClientSide) {
            MapItemSavedData var3 = getSavedData(param0.getItemInHand(), param0.getLevel());
            if (var3 != null && !var3.toggleBanner(param0.getLevel(), param0.getClickedPos())) {
               return InteractionResult.FAIL;
            }
         }

         return InteractionResult.sidedSuccess(param0.getLevel().isClientSide);
      } else {
         return super.useOn(param0);
      }
   }
}
