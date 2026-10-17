package net.minecraft.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;

public class BlockEntityWithoutLevelRenderer implements ResourceManagerReloadListener {
   private static final ShulkerBoxBlockEntity[] SHULKER_BOXES = Arrays.stream(DyeColor.values())
      .sorted(Comparator.comparingInt(DyeColor::getId))
      .map(param0 -> new ShulkerBoxBlockEntity(param0, BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState()))
      .toArray(ShulkerBoxBlockEntity[]::new);
   private static final ShulkerBoxBlockEntity DEFAULT_SHULKER_BOX = new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
   private final ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
   private final ChestBlockEntity trappedChest = new TrappedChestBlockEntity(BlockPos.ZERO, Blocks.TRAPPED_CHEST.defaultBlockState());
   private final EnderChestBlockEntity enderChest = new EnderChestBlockEntity(BlockPos.ZERO, Blocks.ENDER_CHEST.defaultBlockState());
   private final BannerBlockEntity banner = new BannerBlockEntity(BlockPos.ZERO, Blocks.WHITE_BANNER.defaultBlockState());
   private final BedBlockEntity bed = new BedBlockEntity(BlockPos.ZERO, Blocks.RED_BED.defaultBlockState());
   private final ConduitBlockEntity conduit = new ConduitBlockEntity(BlockPos.ZERO, Blocks.CONDUIT.defaultBlockState());
   private final DecoratedPotBlockEntity decoratedPot = new DecoratedPotBlockEntity(BlockPos.ZERO, Blocks.DECORATED_POT.defaultBlockState());
   private ShieldModel shieldModel;
   private TridentModel tridentModel;
   private Map<SkullBlock.Type, SkullModelBase> skullModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final EntityModelSet entityModelSet;

   public BlockEntityWithoutLevelRenderer(BlockEntityRenderDispatcher param0, EntityModelSet param1) {
      this.blockEntityRenderDispatcher = param0;
      this.entityModelSet = param1;
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      this.shieldModel = new ShieldModel(this.entityModelSet.bakeLayer(ModelLayers.SHIELD));
      this.tridentModel = new TridentModel(this.entityModelSet.bakeLayer(ModelLayers.TRIDENT));
      this.skullModels = SkullBlockRenderer.createSkullRenderers(this.entityModelSet);
   }

   public void renderByItem(ItemStack param0, ItemDisplayContext param1, PoseStack param2, MultiBufferSource param3, int param4, int param5) {
      Item var7 = param0.getItem();
      if (var7 instanceof BlockItem) {
         Block var8 = ((BlockItem)var7).getBlock();
         if (var8 instanceof AbstractSkullBlock) {
            GameProfile var9 = null;
            if (param0.hasTag()) {
               CompoundTag var10 = param0.getTag();
               if (var10.contains("SkullOwner", 10)) {
                  var9 = NbtUtils.readGameProfile(var10.getCompound("SkullOwner"));
               } else if (var10.contains("SkullOwner", 8) && !StringUtils.isBlank(var10.getString("SkullOwner"))) {
                  var9 = new GameProfile(null, var10.getString("SkullOwner"));
                  var10.remove("SkullOwner");
                  SkullBlockEntity.updateGameprofile(var9, l_param1 -> var10.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), l_param1)));
               }
            }

            SkullBlock.Type var10 = ((AbstractSkullBlock)var8).getType();
            SkullModelBase var11 = this.skullModels.get(var10);
            RenderType var12 = SkullBlockRenderer.getRenderType(var10, var9);
            SkullBlockRenderer.renderSkull(null, 180.0F, 0.0F, param2, param3, param4, var11, var12);
         } else {
            BlockState var10 = var8.defaultBlockState();
            BlockEntity var9;
            if (var8 instanceof AbstractBannerBlock) {
               this.banner.fromItem(param0, ((AbstractBannerBlock)var8).getColor());
               var9 = this.banner;
            } else if (var8 instanceof BedBlock) {
               this.bed.setColor(((BedBlock)var8).getColor());
               var9 = this.bed;
            } else if (var10.is(Blocks.CONDUIT)) {
               var9 = this.conduit;
            } else if (var10.is(Blocks.CHEST)) {
               var9 = this.chest;
            } else if (var10.is(Blocks.ENDER_CHEST)) {
               var9 = this.enderChest;
            } else if (var10.is(Blocks.TRAPPED_CHEST)) {
               var9 = this.trappedChest;
            } else if (var10.is(Blocks.DECORATED_POT)) {
               this.decoratedPot.setFromItem(param0);
               var9 = this.decoratedPot;
            } else {
               if (!(var8 instanceof ShulkerBoxBlock)) {
                  return;
               }

               DyeColor var11 = ShulkerBoxBlock.getColorFromItem(var7);
               if (var11 == null) {
                  var9 = DEFAULT_SHULKER_BOX;
               } else {
                  var9 = SHULKER_BOXES[var11.getId()];
               }
            }

            this.blockEntityRenderDispatcher.renderItem(var9, param2, param3, param4, param5);
         }
      } else {
         if (param0.is(Items.SHIELD)) {
            boolean var8 = BlockItem.getBlockEntityData(param0) != null;
            param2.pushPose();
            param2.scale(1.0F, -1.0F, -1.0F);
            Material var9 = var8 ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
            VertexConsumer var10 = var9.sprite()
               .wrap(ItemRenderer.getFoilBufferDirect(param3, this.shieldModel.renderType(var9.atlasLocation()), true, param0.hasFoil()));
            this.shieldModel.handle().render(param2, var10, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
            if (var8) {
               List<Pair<Holder<BannerPattern>, DyeColor>> var11 = BannerBlockEntity.createPatterns(
                  ShieldItem.getColor(param0), BannerBlockEntity.getItemPatterns(param0)
               );
               BannerRenderer.renderPatterns(param2, param3, param4, param5, this.shieldModel.plate(), var9, false, var11, param0.hasFoil());
            } else {
               this.shieldModel.plate().render(param2, var10, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            param2.popPose();
         } else if (param0.is(Items.TRIDENT)) {
            param2.pushPose();
            param2.scale(1.0F, -1.0F, -1.0F);
            VertexConsumer var8 = ItemRenderer.getFoilBufferDirect(param3, this.tridentModel.renderType(TridentModel.TEXTURE), false, param0.hasFoil());
            this.tridentModel.renderToBuffer(param2, var8, param4, param5, 1.0F, 1.0F, 1.0F, 1.0F);
            param2.popPose();
         }
      }
   }
}
