package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose = PartPose.ZERO;

   public ModelPart(List<ModelPart.Cube> param0, Map<String, ModelPart> param1) {
      this.cubes = param0;
      this.children = param1;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose param0) {
      this.initialPose = param0;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose param0) {
      this.x = param0.x;
      this.y = param0.y;
      this.z = param0.z;
      this.xRot = param0.xRot;
      this.yRot = param0.yRot;
      this.zRot = param0.zRot;
      this.xScale = 1.0F;
      this.yScale = 1.0F;
      this.zScale = 1.0F;
   }

   public void copyFrom(ModelPart param0) {
      this.xScale = param0.xScale;
      this.yScale = param0.yScale;
      this.zScale = param0.zScale;
      this.xRot = param0.xRot;
      this.yRot = param0.yRot;
      this.zRot = param0.zRot;
      this.x = param0.x;
      this.y = param0.y;
      this.z = param0.z;
   }

   public boolean hasChild(String param0) {
      return this.children.containsKey(param0);
   }

   public ModelPart getChild(String param0) {
      ModelPart var2 = this.children.get(param0);
      if (var2 == null) {
         throw new NoSuchElementException("Can't find part " + param0);
      } else {
         return var2;
      }
   }

   public void setPos(float param0, float param1, float param2) {
      this.x = param0;
      this.y = param1;
      this.z = param2;
   }

   public void setRotation(float param0, float param1, float param2) {
      this.xRot = param0;
      this.yRot = param1;
      this.zRot = param2;
   }

   public void render(PoseStack param0, VertexConsumer param1, int param2, int param3) {
      this.render(param0, param1, param2, param3, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void render(PoseStack param0, VertexConsumer param1, int param2, int param3, float param4, float param5, float param6, float param7) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            param0.pushPose();
            this.translateAndRotate(param0);
            if (!this.skipDraw) {
               this.compile(param0.last(), param1, param2, param3, param4, param5, param6, param7);
            }

            for (ModelPart var10 : this.children.values()) {
               var10.render(param0, param1, param2, param3, param4, param5, param6, param7);
            }

            param0.popPose();
         }
      }
   }

   public void visit(PoseStack param0, ModelPart.Visitor param1) {
      this.visit(param0, param1, "");
   }

   private void visit(PoseStack param0, ModelPart.Visitor param1, String param2) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         param0.pushPose();
         this.translateAndRotate(param0);
         PoseStack.Pose var4 = param0.last();

         for (int var5 = 0; var5 < this.cubes.size(); var5++) {
            param1.visit(var4, param2, var5, this.cubes.get(var5));
         }

         String var5 = param2 + "/";
         this.children.forEach((l_param3, l_param4) -> l_param4.visit(param0, param1, var5 + l_param3));
         param0.popPose();
      }
   }

   public void translateAndRotate(PoseStack param0) {
      param0.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         param0.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         param0.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   private void compile(PoseStack.Pose param0, VertexConsumer param1, int param2, int param3, float param4, float param5, float param6, float param7) {
      for (ModelPart.Cube var10 : this.cubes) {
         var10.compile(param0, param1, param2, param3, param4, param5, param6, param7);
      }
   }

   public ModelPart.Cube getRandomCube(RandomSource param0) {
      return this.cubes.get(param0.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f param0) {
      this.x = this.x + param0.x();
      this.y = this.y + param0.y();
      this.z = this.z + param0.z();
   }

   public void offsetRotation(Vector3f param0) {
      this.xRot = this.xRot + param0.x();
      this.yRot = this.yRot + param0.y();
      this.zRot = this.zRot + param0.z();
   }

   public void offsetScale(Vector3f param0) {
      this.xScale = this.xScale + param0.x();
      this.yScale = this.yScale + param0.y();
      this.zScale = this.zScale + param0.z();
   }

   public Stream<ModelPart> getAllParts() {
      return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
   }

   public static class Cube {
      private final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(
         int param0,
         int param1,
         float param2,
         float param3,
         float param4,
         float param5,
         float param6,
         float param7,
         float param8,
         float param9,
         float param10,
         boolean param11,
         float param12,
         float param13,
         Set<Direction> param14
      ) {
         this.minX = param2;
         this.minY = param3;
         this.minZ = param4;
         this.maxX = param2 + param5;
         this.maxY = param3 + param6;
         this.maxZ = param4 + param7;
         this.polygons = new ModelPart.Polygon[param14.size()];
         float var16 = param2 + param5;
         float var17 = param3 + param6;
         float var18 = param4 + param7;
         param2 -= param8;
         param3 -= param9;
         param4 -= param10;
         var16 += param8;
         var17 += param9;
         var18 += param10;
         if (param11) {
            float var19 = var16;
            var16 = param2;
            param2 = var19;
         }

         ModelPart.Vertex var19 = new ModelPart.Vertex(param2, param3, param4, 0.0F, 0.0F);
         ModelPart.Vertex var20 = new ModelPart.Vertex(var16, param3, param4, 0.0F, 8.0F);
         ModelPart.Vertex var21 = new ModelPart.Vertex(var16, var17, param4, 8.0F, 8.0F);
         ModelPart.Vertex var22 = new ModelPart.Vertex(param2, var17, param4, 8.0F, 0.0F);
         ModelPart.Vertex var23 = new ModelPart.Vertex(param2, param3, var18, 0.0F, 0.0F);
         ModelPart.Vertex var24 = new ModelPart.Vertex(var16, param3, var18, 0.0F, 8.0F);
         ModelPart.Vertex var25 = new ModelPart.Vertex(var16, var17, var18, 8.0F, 8.0F);
         ModelPart.Vertex var26 = new ModelPart.Vertex(param2, var17, var18, 8.0F, 0.0F);
         float var27 = param0;
         float var28 = param0 + param7;
         float var29 = param0 + param7 + param5;
         float var30 = param0 + param7 + param5 + param5;
         float var31 = param0 + param7 + param5 + param7;
         float var32 = param0 + param7 + param5 + param7 + param5;
         float var33 = param1;
         float var34 = param1 + param7;
         float var35 = param1 + param7 + param6;
         int var36 = 0;
         if (param14.contains(Direction.DOWN)) {
            this.polygons[var36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var24, var23, var19, var20}, var28, var33, var29, var34, param12, param13, param11, Direction.DOWN
            );
         }

         if (param14.contains(Direction.UP)) {
            this.polygons[var36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var21, var22, var26, var25}, var29, var34, var30, var33, param12, param13, param11, Direction.UP
            );
         }

         if (param14.contains(Direction.WEST)) {
            this.polygons[var36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var19, var23, var26, var22}, var27, var34, var28, var35, param12, param13, param11, Direction.WEST
            );
         }

         if (param14.contains(Direction.NORTH)) {
            this.polygons[var36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var20, var19, var22, var21}, var28, var34, var29, var35, param12, param13, param11, Direction.NORTH
            );
         }

         if (param14.contains(Direction.EAST)) {
            this.polygons[var36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var24, var20, var21, var25}, var29, var34, var31, var35, param12, param13, param11, Direction.EAST
            );
         }

         if (param14.contains(Direction.SOUTH)) {
            this.polygons[var36] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{var23, var24, var25, var26}, var31, var34, var32, var35, param12, param13, param11, Direction.SOUTH
            );
         }
      }

      public void compile(PoseStack.Pose param0, VertexConsumer param1, int param2, int param3, float param4, float param5, float param6, float param7) {
         Matrix4f var9 = param0.pose();
         Matrix3f var10 = param0.normal();

         for (ModelPart.Polygon var14 : this.polygons) {
            Vector3f var15 = var10.transform(new Vector3f(var14.normal));
            float var16 = var15.x();
            float var17 = var15.y();
            float var18 = var15.z();

            for (ModelPart.Vertex var22 : var14.vertices) {
               float var23 = var22.pos.x() / 16.0F;
               float var24 = var22.pos.y() / 16.0F;
               float var25 = var22.pos.z() / 16.0F;
               Vector4f var26 = var9.transform(new Vector4f(var23, var24, var25, 1.0F));
               param1.vertex(var26.x(), var26.y(), var26.z(), param4, param5, param6, param7, var22.u, var22.v, param3, param2, var16, var17, var18);
            }
         }
      }
   }

   static class Polygon {
      public final ModelPart.Vertex[] vertices;
      public final Vector3f normal;

      public Polygon(
         ModelPart.Vertex[] param0, float param1, float param2, float param3, float param4, float param5, float param6, boolean param7, Direction param8
      ) {
         this.vertices = param0;
         float var10 = 0.0F / param5;
         float var11 = 0.0F / param6;
         param0[0] = param0[0].remap(param3 / param5 - var10, param2 / param6 + var11);
         param0[1] = param0[1].remap(param1 / param5 + var10, param2 / param6 + var11);
         param0[2] = param0[2].remap(param1 / param5 + var10, param4 / param6 - var11);
         param0[3] = param0[3].remap(param3 / param5 - var10, param4 / param6 - var11);
         if (param7) {
            int var12 = param0.length;

            for (int var13 = 0; var13 < var12 / 2; var13++) {
               ModelPart.Vertex var14 = param0[var13];
               param0[var13] = param0[var12 - 1 - var13];
               param0[var12 - 1 - var13] = var14;
            }
         }

         this.normal = param8.step();
         if (param7) {
            this.normal.mul(-1.0F, 1.0F, 1.0F);
         }
      }
   }

   static class Vertex {
      public final Vector3f pos;
      public final float u;
      public final float v;

      public Vertex(float param0, float param1, float param2, float param3, float param4) {
         this(new Vector3f(param0, param1, param2), param3, param4);
      }

      public ModelPart.Vertex remap(float param0, float param1) {
         return new ModelPart.Vertex(this.pos, param0, param1);
      }

      public Vertex(Vector3f param0, float param1, float param2) {
         this.pos = param0;
         this.u = param1;
         this.v = param2;
      }
   }

   @FunctionalInterface
   public interface Visitor {
      void visit(PoseStack.Pose var1, String var2, int var3, ModelPart.Cube var4);
   }
}
