package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;

public class BlockEntityRenderDispatcher implements ResourceManagerReloadListener {
   private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = ImmutableMap.of();
   private final Font font;
   private final EntityModelSet entityModelSet;
   public Level level;
   public Camera camera;
   public HitResult cameraHitResult;
   private final Supplier<BlockRenderDispatcher> blockRenderDispatcher;
   private final Supplier<ItemRenderer> itemRenderer;
   private final Supplier<EntityRenderDispatcher> entityRenderer;

   public BlockEntityRenderDispatcher(
      Font param0, EntityModelSet param1, Supplier<BlockRenderDispatcher> param2, Supplier<ItemRenderer> param3, Supplier<EntityRenderDispatcher> param4
   ) {
      this.itemRenderer = param3;
      this.entityRenderer = param4;
      this.font = param0;
      this.entityModelSet = param1;
      this.blockRenderDispatcher = param2;
   }

   @Nullable
   public <E extends BlockEntity> BlockEntityRenderer<E> getRenderer(E param0) {
      return (BlockEntityRenderer<E>)this.renderers.get(param0.getType());
   }

   public void prepare(Level param0, Camera param1, HitResult param2) {
      if (this.level != param0) {
         this.setLevel(param0);
      }

      this.camera = param1;
      this.cameraHitResult = param2;
   }

   public <E extends BlockEntity> void render(E param0, float param1, PoseStack param2, MultiBufferSource param3) {
      BlockEntityRenderer<E> var5 = this.getRenderer(param0);
      if (var5 != null) {
         if (param0.hasLevel() && param0.getType().isValid(param0.getBlockState())) {
            if (var5.shouldRender(param0, this.camera.getPosition())) {
               tryRender(param0, () -> setupAndRender(var5, param0, param1, param2, param3));
            }
         }
      }
   }

   private static <T extends BlockEntity> void setupAndRender(BlockEntityRenderer<T> param0, T param1, float param2, PoseStack param3, MultiBufferSource param4) {
      Level var6 = param1.getLevel();
      int var5;
      if (var6 != null) {
         var5 = LevelRenderer.getLightColor(var6, param1.getBlockPos());
      } else {
         var5 = 15728880;
      }

      param0.render(param1, param2, param3, param4, var5, OverlayTexture.NO_OVERLAY);
   }

   public <E extends BlockEntity> boolean renderItem(E param0, PoseStack param1, MultiBufferSource param2, int param3, int param4) {
      BlockEntityRenderer<E> var6 = this.getRenderer(param0);
      if (var6 == null) {
         return true;
      }

      tryRender(param0, () -> var6.render(param0, 0.0F, param1, param2, param3, param4));
      return false;
   }

   private static void tryRender(BlockEntity param0, Runnable param1) {
      try {
         param1.run();
      } catch (Throwable var2) {
         CrashReport var3 = CrashReport.forThrowable(var2, "Rendering Block Entity");
         CrashReportCategory var4 = var3.addCategory("Block Entity Details");
         param0.fillCrashReportCategory(var4);
         throw new ReportedException(var3);
      }
   }

   public void setLevel(@Nullable Level param0) {
      this.level = param0;
      if (param0 == null) {
         this.camera = null;
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      BlockEntityRendererProvider.Context var2 = new BlockEntityRendererProvider.Context(
         this, this.blockRenderDispatcher.get(), this.itemRenderer.get(), this.entityRenderer.get(), this.entityModelSet, this.font
      );
      this.renderers = BlockEntityRenderers.createEntityRenderers(var2);
   }
}
