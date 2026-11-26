package net.minecraft.client;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {
   private boolean initialized;
   private BlockGetter level;
   private Entity entity;
   private Vec3 position = Vec3.ZERO;
   private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
   private final Vector3f forwards = new Vector3f(0.0F, 0.0F, 1.0F);
   private final Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
   private final Vector3f left = new Vector3f(1.0F, 0.0F, 0.0F);
   private float xRot;
   private float yRot;
   private final Quaternionf rotation = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
   private boolean detached;
   private float eyeHeight;
   private float eyeHeightOld;
   public static final float FOG_DISTANCE_SCALE = 0.083333336F;

   public void setup(BlockGetter param0, Entity param1, boolean param2, boolean param3, float param4) {
      this.initialized = true;
      this.level = param0;
      this.entity = param1;
      this.detached = param2;
      this.setRotation(param1.getViewYRot(param4), param1.getViewXRot(param4));
      this.setPosition(
         Mth.lerp(param4, param1.xo, param1.getX()),
         Mth.lerp(param4, param1.yo, param1.getY()) + Mth.lerp(param4, this.eyeHeightOld, this.eyeHeight),
         Mth.lerp(param4, param1.zo, param1.getZ())
      );
      if (param2) {
         if (param3) {
            this.setRotation(this.yRot + 180.0F, -this.xRot);
         }

         this.move(-this.getMaxZoom(4.0), 0.0, 0.0);
      } else if (param1 instanceof LivingEntity && ((LivingEntity)param1).isSleeping()) {
         Direction var6 = ((LivingEntity)param1).getBedOrientation();
         this.setRotation(var6 != null ? var6.toYRot() - 180.0F : 0.0F, 0.0F);
         this.move(0.0, 0.3, 0.0);
      }
   }

   public void tick() {
      if (this.entity != null) {
         this.eyeHeightOld = this.eyeHeight;
         this.eyeHeight = this.eyeHeight + (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
      }
   }

   private double getMaxZoom(double param0) {
      for (int var3 = 0; var3 < 8; var3++) {
         float var4 = (var3 & 1) * 2 - 1;
         float var5 = (var3 >> 1 & 1) * 2 - 1;
         float var6 = (var3 >> 2 & 1) * 2 - 1;
         var4 *= 0.1F;
         var5 *= 0.1F;
         var6 *= 0.1F;
         Vec3 var7 = this.position.add(var4, var5, var6);
         Vec3 var8 = new Vec3(
            this.position.x - this.forwards.x() * param0 + var4,
            this.position.y - this.forwards.y() * param0 + var5,
            this.position.z - this.forwards.z() * param0 + var6
         );
         HitResult var9 = this.level.clip(new ClipContext(var7, var8, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
         if (var9.getType() != HitResult.Type.MISS) {
            double var10 = var9.getLocation().distanceTo(this.position);
            if (var10 < param0) {
               param0 = var10;
            }
         }
      }

      return param0;
   }

   protected void move(double param0, double param2, double param4) {
      double var7 = this.forwards.x() * param0 + this.up.x() * param2 + this.left.x() * param4;
      double var9 = this.forwards.y() * param0 + this.up.y() * param2 + this.left.y() * param4;
      double var11 = this.forwards.z() * param0 + this.up.z() * param2 + this.left.z() * param4;
      this.setPosition(new Vec3(this.position.x + var7, this.position.y + var9, this.position.z + var11));
   }

   protected void setRotation(float param0, float param1) {
      this.xRot = param1;
      this.yRot = param0;
      this.rotation.rotationYXZ(-param0 * (float) (Math.PI / 180.0), param1 * (float) (Math.PI / 180.0), 0.0F);
      this.forwards.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
      this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
      this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
   }

   protected void setPosition(double param0, double param2, double param4) {
      this.setPosition(new Vec3(param0, param2, param4));
   }

   protected void setPosition(Vec3 param0) {
      this.position = param0;
      this.blockPosition.set(param0.x, param0.y, param0.z);
   }

   public Vec3 getPosition() {
      return this.position;
   }

   public BlockPos getBlockPosition() {
      return this.blockPosition;
   }

   public float getXRot() {
      return this.xRot;
   }

   public float getYRot() {
      return this.yRot;
   }

   public Quaternionf rotation() {
      return this.rotation;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public boolean isDetached() {
      return this.detached;
   }

   public Camera.NearPlane getNearPlane() {
      Minecraft var1 = Minecraft.getInstance();
      double var2 = (double)var1.getWindow().getWidth() / var1.getWindow().getHeight();
      double var4 = Math.tan(var1.options.fov().get().intValue() * (float) (Math.PI / 180.0) / 2.0) * 0.05F;
      double var6 = var4 * var2;
      Vec3 var8 = new Vec3(this.forwards).scale(0.05F);
      Vec3 var9 = new Vec3(this.left).scale(var6);
      Vec3 var10 = new Vec3(this.up).scale(var4);
      return new Camera.NearPlane(var8, var9, var10);
   }

   public FogType getFluidInCamera() {
      if (!this.initialized) {
         return FogType.NONE;
      }

      FluidState var1 = this.level.getFluidState(this.blockPosition);
      if (var1.is(FluidTags.WATER) && this.position.y < this.blockPosition.getY() + var1.getHeight(this.level, this.blockPosition)) {
         return FogType.WATER;
      }

      Camera.NearPlane var2 = this.getNearPlane();

      for (Vec3 var5 : Arrays.asList(var2.forward, var2.getTopLeft(), var2.getTopRight(), var2.getBottomLeft(), var2.getBottomRight())) {
         Vec3 var6 = this.position.add(var5);
         BlockPos var7 = BlockPos.containing(var6);
         FluidState var8 = this.level.getFluidState(var7);
         if (var8.is(FluidTags.LAVA)) {
            if (var6.y <= var8.getHeight(this.level, var7) + var7.getY()) {
               return FogType.LAVA;
            }
         } else {
            BlockState var9 = this.level.getBlockState(var7);
            if (var9.is(Blocks.POWDER_SNOW)) {
               return FogType.POWDER_SNOW;
            }
         }
      }

      return FogType.NONE;
   }

   public final Vector3f getLookVector() {
      return this.forwards;
   }

   public final Vector3f getUpVector() {
      return this.up;
   }

   public final Vector3f getLeftVector() {
      return this.left;
   }

   public void reset() {
      this.level = null;
      this.entity = null;
      this.initialized = false;
   }

   public static class NearPlane {
      final Vec3 forward;
      private final Vec3 left;
      private final Vec3 up;

      NearPlane(Vec3 param0, Vec3 param1, Vec3 param2) {
         this.forward = param0;
         this.left = param1;
         this.up = param2;
      }

      public Vec3 getTopLeft() {
         return this.forward.add(this.up).add(this.left);
      }

      public Vec3 getTopRight() {
         return this.forward.add(this.up).subtract(this.left);
      }

      public Vec3 getBottomLeft() {
         return this.forward.subtract(this.up).add(this.left);
      }

      public Vec3 getBottomRight() {
         return this.forward.subtract(this.up).subtract(this.left);
      }

      public Vec3 getPointOnPlane(float param0, float param1) {
         return this.forward.add(this.up.scale(param1)).subtract(this.left.scale(param0));
      }
   }
}
