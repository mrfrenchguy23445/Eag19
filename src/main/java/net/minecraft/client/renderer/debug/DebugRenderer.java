package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class DebugRenderer {
   public final PathfindingRenderer pathfindingRenderer = new PathfindingRenderer();
   public final DebugRenderer.SimpleDebugRenderer waterDebugRenderer;
   public final DebugRenderer.SimpleDebugRenderer chunkBorderRenderer;
   public final DebugRenderer.SimpleDebugRenderer heightMapRenderer;
   public final DebugRenderer.SimpleDebugRenderer collisionBoxRenderer;
   public final DebugRenderer.SimpleDebugRenderer neighborsUpdateRenderer;
   public final StructureRenderer structureRenderer;
   public final DebugRenderer.SimpleDebugRenderer lightDebugRenderer;
   public final DebugRenderer.SimpleDebugRenderer worldGenAttemptRenderer;
   public final DebugRenderer.SimpleDebugRenderer solidFaceRenderer;
   public final DebugRenderer.SimpleDebugRenderer chunkRenderer;
   public final BrainDebugRenderer brainDebugRenderer;
   public final VillageSectionsDebugRenderer villageSectionsDebugRenderer;
   public final BeeDebugRenderer beeDebugRenderer;
   public final RaidDebugRenderer raidDebugRenderer;
   public final GoalSelectorDebugRenderer goalSelectorRenderer;
   public final GameTestDebugRenderer gameTestDebugRenderer;
   public final GameEventListenerRenderer gameEventListenerRenderer;
   private boolean renderChunkborder;

   public DebugRenderer(Minecraft param0) {
      this.waterDebugRenderer = new WaterDebugRenderer(param0);
      this.chunkBorderRenderer = new ChunkBorderRenderer(param0);
      this.heightMapRenderer = new HeightMapRenderer(param0);
      this.collisionBoxRenderer = new CollisionBoxRenderer(param0);
      this.neighborsUpdateRenderer = new NeighborsUpdateRenderer(param0);
      this.structureRenderer = new StructureRenderer(param0);
      this.lightDebugRenderer = new LightDebugRenderer(param0);
      this.worldGenAttemptRenderer = new WorldGenAttemptRenderer();
      this.solidFaceRenderer = new SolidFaceRenderer(param0);
      this.chunkRenderer = new ChunkDebugRenderer(param0);
      this.brainDebugRenderer = new BrainDebugRenderer(param0);
      this.villageSectionsDebugRenderer = new VillageSectionsDebugRenderer();
      this.beeDebugRenderer = new BeeDebugRenderer(param0);
      this.raidDebugRenderer = new RaidDebugRenderer(param0);
      this.goalSelectorRenderer = new GoalSelectorDebugRenderer(param0);
      this.gameTestDebugRenderer = new GameTestDebugRenderer();
      this.gameEventListenerRenderer = new GameEventListenerRenderer(param0);
   }

   public void clear() {
      this.pathfindingRenderer.clear();
      this.waterDebugRenderer.clear();
      this.chunkBorderRenderer.clear();
      this.heightMapRenderer.clear();
      this.collisionBoxRenderer.clear();
      this.neighborsUpdateRenderer.clear();
      this.structureRenderer.clear();
      this.lightDebugRenderer.clear();
      this.worldGenAttemptRenderer.clear();
      this.solidFaceRenderer.clear();
      this.chunkRenderer.clear();
      this.brainDebugRenderer.clear();
      this.villageSectionsDebugRenderer.clear();
      this.beeDebugRenderer.clear();
      this.raidDebugRenderer.clear();
      this.goalSelectorRenderer.clear();
      this.gameTestDebugRenderer.clear();
      this.gameEventListenerRenderer.clear();
   }

   public boolean switchRenderChunkborder() {
      this.renderChunkborder = !this.renderChunkborder;
      return this.renderChunkborder;
   }

   public void render(PoseStack param0, MultiBufferSource.BufferSource param1, double param2, double param4, double param6) {
      if (this.renderChunkborder && !Minecraft.getInstance().showOnlyReducedInfo()) {
         this.chunkBorderRenderer.render(param0, param1, param2, param4, param6);
      }

      this.gameTestDebugRenderer.render(param0, param1, param2, param4, param6);
   }

   public static Optional<Entity> getTargetedEntity(@Nullable Entity param0, int param1) {
      if (param0 == null) {
         return Optional.empty();
      } else {
         Vec3 var2 = param0.getEyePosition();
         Vec3 var3 = param0.getViewVector(1.0F).scale(param1);
         Vec3 var4 = var2.add(var3);
         AABB var5 = param0.getBoundingBox().expandTowards(var3).inflate(1.0);
         int var6 = param1 * param1;
         Predicate<Entity> var7 = l_param0 -> !l_param0.isSpectator() && l_param0.isPickable();
         EntityHitResult var8 = ProjectileUtil.getEntityHitResult(param0, var2, var4, var5, var7, var6);
         if (var8 == null) {
            return Optional.empty();
         } else {
            return var2.distanceToSqr(var8.getLocation()) > var6 ? Optional.empty() : Optional.of(var8.getEntity());
         }
      }
   }

   public static void renderFilledBox(
      PoseStack param0, MultiBufferSource param1, BlockPos param2, BlockPos param3, float param4, float param5, float param6, float param7
   ) {
      Camera var8 = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (var8.isInitialized()) {
         Vec3 var9 = var8.getPosition().reverse();
         AABB var10 = new AABB(param2, param3).move(var9);
         renderFilledBox(param0, param1, var10, param4, param5, param6, param7);
      }
   }

   public static void renderFilledBox(
      PoseStack param0, MultiBufferSource param1, BlockPos param2, float param3, float param4, float param5, float param6, float param7
   ) {
      Camera var8 = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (var8.isInitialized()) {
         Vec3 var9 = var8.getPosition().reverse();
         AABB var10 = new AABB(param2).move(var9).inflate(param3);
         renderFilledBox(param0, param1, var10, param4, param5, param6, param7);
      }
   }

   public static void renderFilledBox(PoseStack param0, MultiBufferSource param1, AABB param2, float param3, float param4, float param5, float param6) {
      renderFilledBox(param0, param1, param2.minX, param2.minY, param2.minZ, param2.maxX, param2.maxY, param2.maxZ, param3, param4, param5, param6);
   }

   public static void renderFilledBox(
      PoseStack param0,
      MultiBufferSource param1,
      double param2,
      double param4,
      double param6,
      double param8,
      double param10,
      double param12,
      float param14,
      float param15,
      float param16,
      float param17
   ) {
      VertexConsumer var18 = param1.getBuffer(RenderType.debugFilledBox());
      LevelRenderer.addChainedFilledBoxVertices(param0, var18, param2, param4, param6, param8, param10, param12, param14, param15, param16, param17);
   }

   public static void renderFloatingText(PoseStack param0, MultiBufferSource param1, String param2, int param3, int param4, int param5, int param6) {
      renderFloatingText(param0, param1, param2, param3 + 0.5, param4 + 0.5, param5 + 0.5, param6);
   }

   public static void renderFloatingText(PoseStack param0, MultiBufferSource param1, String param2, double param3, double param5, double param7, int param9) {
      renderFloatingText(param0, param1, param2, param3, param5, param7, param9, 0.02F);
   }

   public static void renderFloatingText(
      PoseStack param0, MultiBufferSource param1, String param2, double param3, double param5, double param7, int param9, float param10
   ) {
      renderFloatingText(param0, param1, param2, param3, param5, param7, param9, param10, true, 0.0F, false);
   }

   public static void renderFloatingText(
      PoseStack param0,
      MultiBufferSource param1,
      String param2,
      double param3,
      double param5,
      double param7,
      int param9,
      float param10,
      boolean param11,
      float param12,
      boolean param13
   ) {
      Minecraft var14 = Minecraft.getInstance();
      Camera var15 = var14.gameRenderer.getMainCamera();
      if (var15.isInitialized() && var14.getEntityRenderDispatcher().options != null) {
         Font var16 = var14.font;
         double var17 = var15.getPosition().x;
         double var19 = var15.getPosition().y;
         double var21 = var15.getPosition().z;
         param0.pushPose();
         param0.translate((float)(param3 - var17), (float)(param5 - var19) + 0.07F, (float)(param7 - var21));
         param0.mulPoseMatrix(new Matrix4f().rotation(var15.rotation()));
         param0.scale(-param10, -param10, param10);
         float var23 = param11 ? -var16.width(param2) / 2.0F : 0.0F;
         var23 -= param12 / param10;
         var16.drawInBatch(
            param2, var23, 0.0F, param9, false, param0.last().pose(), param1, param13 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, 15728880
         );
         param0.popPose();
      }
   }

   public interface SimpleDebugRenderer {
      void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7);

      default void clear() {
      }
   }
}
