package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class Particle {
   private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square(100.0);
   protected final ClientLevel level;
   protected double xo;
   protected double yo;
   protected double zo;
   protected double x;
   protected double y;
   protected double z;
   protected double xd;
   protected double yd;
   protected double zd;
   private AABB bb = INITIAL_AABB;
   protected boolean onGround;
   protected boolean hasPhysics = true;
   private boolean stoppedByCollision;
   protected boolean removed;
   protected float bbWidth = 0.6F;
   protected float bbHeight = 1.8F;
   protected final RandomSource random = RandomSource.create();
   protected int age;
   protected int lifetime;
   protected float gravity;
   protected float rCol = 1.0F;
   protected float gCol = 1.0F;
   protected float bCol = 1.0F;
   protected float alpha = 1.0F;
   protected float roll;
   protected float oRoll;
   protected float friction = 0.98F;
   protected boolean speedUpWhenYMotionIsBlocked = false;

   protected Particle(ClientLevel param0, double param1, double param3, double param5) {
      this.level = param0;
      this.setSize(0.2F, 0.2F);
      this.setPos(param1, param3, param5);
      this.xo = param1;
      this.yo = param3;
      this.zo = param5;
      this.lifetime = (int)(4.0F / (this.random.nextFloat() * 0.9F + 0.1F));
   }

   public Particle(ClientLevel param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      this(param0, param1, param3, param5);
      this.xd = param7 + (Math.random() * 2.0 - 1.0) * 0.4F;
      this.yd = param9 + (Math.random() * 2.0 - 1.0) * 0.4F;
      this.zd = param11 + (Math.random() * 2.0 - 1.0) * 0.4F;
      double var14 = (Math.random() + Math.random() + 1.0) * 0.15F;
      double var16 = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
      this.xd = this.xd / var16 * var14 * 0.4F;
      this.yd = this.yd / var16 * var14 * 0.4F + 0.1F;
      this.zd = this.zd / var16 * var14 * 0.4F;
   }

   public Particle setPower(float param0) {
      this.xd *= param0;
      this.yd = (this.yd - 0.1F) * param0 + 0.1F;
      this.zd *= param0;
      return this;
   }

   public void setParticleSpeed(double param0, double param2, double param4) {
      this.xd = param0;
      this.yd = param2;
      this.zd = param4;
   }

   public Particle scale(float param0) {
      this.setSize(0.2F * param0, 0.2F * param0);
      return this;
   }

   public void setColor(float param0, float param1, float param2) {
      this.rCol = param0;
      this.gCol = param1;
      this.bCol = param2;
   }

   protected void setAlpha(float param0) {
      this.alpha = param0;
   }

   public void setLifetime(int param0) {
      this.lifetime = param0;
   }

   public int getLifetime() {
      return this.lifetime;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.yd = this.yd - 0.04 * this.gravity;
         this.move(this.xd, this.yd, this.zd);
         if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
            this.xd *= 1.1;
            this.zd *= 1.1;
         }

         this.xd = this.xd * this.friction;
         this.yd = this.yd * this.friction;
         this.zd = this.zd * this.friction;
         if (this.onGround) {
            this.xd *= 0.7F;
            this.zd *= 0.7F;
         }
      }
   }

   public abstract void render(VertexConsumer var1, Camera var2, float var3);

   public abstract ParticleRenderType getRenderType();

   @Override
   public String toString() {
      return this.getClass().getSimpleName()
         + ", Pos ("
         + this.x
         + ","
         + this.y
         + ","
         + this.z
         + "), RGBA ("
         + this.rCol
         + ","
         + this.gCol
         + ","
         + this.bCol
         + ","
         + this.alpha
         + "), Age "
         + this.age;
   }

   public void remove() {
      this.removed = true;
   }

   protected void setSize(float param0, float param1) {
      if (param0 != this.bbWidth || param1 != this.bbHeight) {
         this.bbWidth = param0;
         this.bbHeight = param1;
         AABB var3 = this.getBoundingBox();
         double var4 = (var3.minX + var3.maxX - param0) / 2.0;
         double var6 = (var3.minZ + var3.maxZ - param0) / 2.0;
         this.setBoundingBox(new AABB(var4, var3.minY, var6, var4 + this.bbWidth, var3.minY + this.bbHeight, var6 + this.bbWidth));
      }
   }

   public void setPos(double param0, double param2, double param4) {
      this.x = param0;
      this.y = param2;
      this.z = param4;
      float var7 = this.bbWidth / 2.0F;
      float var8 = this.bbHeight;
      this.setBoundingBox(new AABB(param0 - var7, param2, param4 - var7, param0 + var7, param2 + var8, param4 + var7));
   }

   public void move(double param0, double param2, double param4) {
      if (!this.stoppedByCollision) {
         double var7 = param0;
         double var9 = param2;
         double var11 = param4;
         if (this.hasPhysics
            && (param0 != 0.0 || param2 != 0.0 || param4 != 0.0)
            && param0 * param0 + param2 * param2 + param4 * param4 < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 var13 = Entity.collideBoundingBox(null, new Vec3(param0, param2, param4), this.getBoundingBox(), this.level, List.of());
            param0 = var13.x;
            param2 = var13.y;
            param4 = var13.z;
         }

         if (param0 != 0.0 || param2 != 0.0 || param4 != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(param0, param2, param4));
            this.setLocationFromBoundingbox();
         }

         if (Math.abs(var9) >= 1.0E-5F && Math.abs(param2) < 1.0E-5F) {
            this.stoppedByCollision = true;
         }

         this.onGround = var9 != param2 && var9 < 0.0;
         if (var7 != param0) {
            this.xd = 0.0;
         }

         if (var11 != param4) {
            this.zd = 0.0;
         }
      }
   }

   protected void setLocationFromBoundingbox() {
      AABB var1 = this.getBoundingBox();
      this.x = (var1.minX + var1.maxX) / 2.0;
      this.y = var1.minY;
      this.z = (var1.minZ + var1.maxZ) / 2.0;
   }

   protected int getLightColor(float param0) {
      BlockPos var2 = BlockPos.containing(this.x, this.y, this.z);
      return this.level.hasChunkAt(var2) ? LevelRenderer.getLightColor(this.level, var2) : 0;
   }

   public boolean isAlive() {
      return !this.removed;
   }

   public AABB getBoundingBox() {
      return this.bb;
   }

   public void setBoundingBox(AABB param0) {
      this.bb = param0;
   }

   public Optional<ParticleGroup> getParticleGroup() {
      return Optional.empty();
   }
}
