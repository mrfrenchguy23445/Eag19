package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.slf4j.Logger;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int CHUNK_SIZE = 16;
   private static final int HALF_CHUNK_SIZE = 8;
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
   private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
   private static final int MIN_FOG_DISTANCE = 32;
   private static final int RAIN_RADIUS = 10;
   private static final int RAIN_DIAMETER = 21;
   private static final int TRANSPARENT_SORT_COUNT = 15;
   private static final int HALF_A_SECOND_IN_MILLIS = 500;
   private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
   private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
   private static final ResourceLocation CLOUDS_LOCATION = new ResourceLocation("textures/environment/clouds.png");
   private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
   private static final ResourceLocation FORCEFIELD_LOCATION = new ResourceLocation("textures/misc/forcefield.png");
   private static final ResourceLocation RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
   private static final ResourceLocation SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");
   public static final Direction[] DIRECTIONS = Direction.values();
   private final Minecraft minecraft;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   @Nullable
   private ClientLevel level;
   private final BlockingQueue<ChunkRenderDispatcher.RenderChunk> recentlyCompiledChunks = new LinkedBlockingQueue<>();
   private final AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference<>();
   private final ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum = new ObjectArrayList<>(10000);
   private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
   /**
    * Set when the most recent full render chunk update threw.
    *
    * <p>Upstream tracks this with a {@code Future<?>} returned by
    * {@code Util.backgroundExecutor().submit(..)} and rethrows here with
    * {@code get()}. {@code ExecutorService.submit} is not part of the {@code Executor} interface
    * and {@code ExecutorService} itself is absent from the WASM-GC TeaVM classlib, so the
    * background executor is a direct executor that runs the task inline; the update can therefore
    * never still be in flight, and all that needs carrying forward is whether it failed.
    */
   private boolean lastFullRenderChunkUpdateFailed;
   /** Always false: the full update runs inline, so it is never pending across a frame. */
   private boolean lastFullRenderChunkUpdateInProgress;
   @Nullable
   private ViewArea viewArea;
   @Nullable
   private VertexBuffer starBuffer;
   @Nullable
   private VertexBuffer skyBuffer;
   @Nullable
   private VertexBuffer darkBuffer;
   private boolean generateClouds = true;
   @Nullable
   private VertexBuffer cloudBuffer;
   private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
   private int ticks;
   private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
   private final Map<BlockPos, SoundInstance> playingRecords = Maps.newHashMap();
   @Nullable
   private RenderTarget entityTarget;
   @Nullable
   private PostChain entityEffect;
   @Nullable
   private RenderTarget translucentTarget;
   @Nullable
   private RenderTarget itemEntityTarget;
   @Nullable
   private RenderTarget particlesTarget;
   @Nullable
   private RenderTarget weatherTarget;
   @Nullable
   private RenderTarget cloudsTarget;
   @Nullable
   private PostChain transparencyChain;
   private double lastCameraX = Double.MIN_VALUE;
   private double lastCameraY = Double.MIN_VALUE;
   private double lastCameraZ = Double.MIN_VALUE;
   private int lastCameraChunkX = Integer.MIN_VALUE;
   private int lastCameraChunkY = Integer.MIN_VALUE;
   private int lastCameraChunkZ = Integer.MIN_VALUE;
   private double prevCamX = Double.MIN_VALUE;
   private double prevCamY = Double.MIN_VALUE;
   private double prevCamZ = Double.MIN_VALUE;
   private double prevCamRotX = Double.MIN_VALUE;
   private double prevCamRotY = Double.MIN_VALUE;
   private int prevCloudX = Integer.MIN_VALUE;
   private int prevCloudY = Integer.MIN_VALUE;
   private int prevCloudZ = Integer.MIN_VALUE;
   private Vec3 prevCloudColor = Vec3.ZERO;
   @Nullable
   private CloudStatus prevCloudsType;
   @Nullable
   private ChunkRenderDispatcher chunkRenderDispatcher;
   private int lastViewDistance = -1;
   private int renderedEntities;
   private int culledEntities;
   private Frustum cullingFrustum;
   private boolean captureFrustum;
   @Nullable
   private Frustum capturedFrustum;
   private final Vector4f[] frustumPoints = new Vector4f[8];
   private final Vector3d frustumPos = new Vector3d(0.0, 0.0, 0.0);
   private double xTransparentOld;
   private double yTransparentOld;
   private double zTransparentOld;
   private boolean needsFullRenderChunkUpdate = true;
   private final AtomicLong nextFullUpdateMillis = new AtomicLong(0L);
   private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
   private int rainSoundTime;
   private final float[] rainSizeX = new float[1024];
   private final float[] rainSizeZ = new float[1024];

   public LevelRenderer(Minecraft param0, EntityRenderDispatcher param1, BlockEntityRenderDispatcher param2, RenderBuffers param3) {
      this.minecraft = param0;
      this.entityRenderDispatcher = param1;
      this.blockEntityRenderDispatcher = param2;
      this.renderBuffers = param3;

      for (int var5 = 0; var5 < 32; var5++) {
         for (int var6 = 0; var6 < 32; var6++) {
            float var7 = var6 - 16;
            float var8 = var5 - 16;
            float var9 = Mth.sqrt(var7 * var7 + var8 * var8);
            this.rainSizeX[var5 << 5 | var6] = -var8 / var9;
            this.rainSizeZ[var5 << 5 | var6] = var7 / var9;
         }
      }

      this.createStars();
      this.createLightSky();
      this.createDarkSky();
   }

   private void renderSnowAndRain(LightTexture param0, float param1, double param2, double param4, double param6) {
      float var9 = this.minecraft.level.getRainLevel(param1);
      if (!(var9 <= 0.0F)) {
         param0.turnOnLightLayer();
         Level var10 = this.minecraft.level;
         int var11 = Mth.floor(param2);
         int var12 = Mth.floor(param4);
         int var13 = Mth.floor(param6);
         Tesselator var14 = Tesselator.getInstance();
         BufferBuilder var15 = var14.getBuilder();
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         int var16 = 5;
         if (Minecraft.useFancyGraphics()) {
            var16 = 10;
         }

         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         int var17 = -1;
         float var18 = this.ticks + param1;
         RenderSystem.setShader(GameRenderer::getParticleShader);
         BlockPos.MutableBlockPos var19 = new BlockPos.MutableBlockPos();

         for (int var20 = var13 - var16; var20 <= var13 + var16; var20++) {
            for (int var21 = var11 - var16; var21 <= var11 + var16; var21++) {
               int var22 = (var20 - var13 + 16) * 32 + var21 - var11 + 16;
               double var23 = this.rainSizeX[var22] * 0.5;
               double var25 = this.rainSizeZ[var22] * 0.5;
               var19.set(var21, param4, var20);
               Biome var27 = var10.getBiome(var19).value();
               if (var27.hasPrecipitation()) {
                  int var28 = var10.getHeight(Heightmap.Types.MOTION_BLOCKING, var21, var20);
                  int var29 = var12 - var16;
                  int var30 = var12 + var16;
                  if (var29 < var28) {
                     var29 = var28;
                  }

                  if (var30 < var28) {
                     var30 = var28;
                  }

                  int var31 = var28;
                  if (var31 < var12) {
                     var31 = var12;
                  }

                  if (var29 != var30) {
                     RandomSource var32 = RandomSource.create(var21 * var21 * 3121 + var21 * 45238971 ^ var20 * var20 * 418711 + var20 * 13761);
                     var19.set(var21, var29, var20);
                     Biome.Precipitation var33 = var27.getPrecipitationAt(var19);
                     if (var33 == Biome.Precipitation.RAIN) {
                        if (var17 != 0) {
                           if (var17 >= 0) {
                              var14.end();
                           }

                           var17 = 0;
                           RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                           var15.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        int var34 = this.ticks + var21 * var21 * 3121 + var21 * 45238971 + var20 * var20 * 418711 + var20 * 13761 & 31;
                        float var35 = -(var34 + param1) / 32.0F * (3.0F + var32.nextFloat());
                        double var36 = var21 + 0.5 - param2;
                        double var38 = var20 + 0.5 - param6;
                        float var40 = (float)Math.sqrt(var36 * var36 + var38 * var38) / var16;
                        float var41 = ((1.0F - var40 * var40) * 0.5F + 0.5F) * var9;
                        var19.set(var21, var31, var20);
                        int var42 = getLightColor(var10, var19);
                        var15.vertex(var21 - param2 - var23 + 0.5, var30 - param4, var20 - param6 - var25 + 0.5)
                           .uv(0.0F, var29 * 0.25F + var35)
                           .color(1.0F, 1.0F, 1.0F, var41)
                           .uv2(var42)
                           .endVertex();
                        var15.vertex(var21 - param2 + var23 + 0.5, var30 - param4, var20 - param6 + var25 + 0.5)
                           .uv(1.0F, var29 * 0.25F + var35)
                           .color(1.0F, 1.0F, 1.0F, var41)
                           .uv2(var42)
                           .endVertex();
                        var15.vertex(var21 - param2 + var23 + 0.5, var29 - param4, var20 - param6 + var25 + 0.5)
                           .uv(1.0F, var30 * 0.25F + var35)
                           .color(1.0F, 1.0F, 1.0F, var41)
                           .uv2(var42)
                           .endVertex();
                        var15.vertex(var21 - param2 - var23 + 0.5, var29 - param4, var20 - param6 - var25 + 0.5)
                           .uv(0.0F, var30 * 0.25F + var35)
                           .color(1.0F, 1.0F, 1.0F, var41)
                           .uv2(var42)
                           .endVertex();
                     } else if (var33 == Biome.Precipitation.SNOW) {
                        if (var17 != 1) {
                           if (var17 >= 0) {
                              var14.end();
                           }

                           var17 = 1;
                           RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                           var15.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        float var34 = -((this.ticks & 511) + param1) / 512.0F;
                        float var35 = (float)(var32.nextDouble() + var18 * 0.01 * (float)var32.nextGaussian());
                        float var36 = (float)(var32.nextDouble() + var18 * (float)var32.nextGaussian() * 0.001);
                        double var37 = var21 + 0.5 - param2;
                        double var39 = var20 + 0.5 - param6;
                        float var41 = (float)Math.sqrt(var37 * var37 + var39 * var39) / var16;
                        float var42 = ((1.0F - var41 * var41) * 0.3F + 0.5F) * var9;
                        var19.set(var21, var31, var20);
                        int var43 = getLightColor(var10, var19);
                        int var44 = var43 >> 16 & 65535;
                        int var45 = var43 & 65535;
                        int var46 = (var44 * 3 + 240) / 4;
                        int var47 = (var45 * 3 + 240) / 4;
                        var15.vertex(var21 - param2 - var23 + 0.5, var30 - param4, var20 - param6 - var25 + 0.5)
                           .uv(0.0F + var35, var29 * 0.25F + var34 + var36)
                           .color(1.0F, 1.0F, 1.0F, var42)
                           .uv2(var47, var46)
                           .endVertex();
                        var15.vertex(var21 - param2 + var23 + 0.5, var30 - param4, var20 - param6 + var25 + 0.5)
                           .uv(1.0F + var35, var29 * 0.25F + var34 + var36)
                           .color(1.0F, 1.0F, 1.0F, var42)
                           .uv2(var47, var46)
                           .endVertex();
                        var15.vertex(var21 - param2 + var23 + 0.5, var29 - param4, var20 - param6 + var25 + 0.5)
                           .uv(1.0F + var35, var30 * 0.25F + var34 + var36)
                           .color(1.0F, 1.0F, 1.0F, var42)
                           .uv2(var47, var46)
                           .endVertex();
                        var15.vertex(var21 - param2 - var23 + 0.5, var29 - param4, var20 - param6 - var25 + 0.5)
                           .uv(0.0F + var35, var30 * 0.25F + var34 + var36)
                           .color(1.0F, 1.0F, 1.0F, var42)
                           .uv2(var47, var46)
                           .endVertex();
                     }
                  }
               }
            }
         }

         if (var17 >= 0) {
            var14.end();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         param0.turnOffLightLayer();
      }
   }

   public void tickRain(Camera param0) {
      float var2 = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
      if (!(var2 <= 0.0F)) {
         RandomSource var3 = RandomSource.create(this.ticks * 312987231L);
         LevelReader var4 = this.minecraft.level;
         BlockPos var5 = BlockPos.containing(param0.getPosition());
         BlockPos var6 = null;
         int var7 = (int)(100.0F * var2 * var2) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

         for (int var8 = 0; var8 < var7; var8++) {
            int var9 = var3.nextInt(21) - 10;
            int var10 = var3.nextInt(21) - 10;
            BlockPos var11 = var4.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var5.offset(var9, 0, var10));
            if (var11.getY() > var4.getMinBuildHeight() && var11.getY() <= var5.getY() + 10 && var11.getY() >= var5.getY() - 10) {
               Biome var12 = var4.getBiome(var11).value();
               if (var12.getPrecipitationAt(var11) == Biome.Precipitation.RAIN) {
                  var6 = var11.below();
                  if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                     break;
                  }

                  double var13 = var3.nextDouble();
                  double var15 = var3.nextDouble();
                  BlockState var17 = var4.getBlockState(var6);
                  FluidState var18 = var4.getFluidState(var6);
                  VoxelShape var19 = var17.getCollisionShape(var4, var6);
                  double var20 = var19.max(Direction.Axis.Y, var13, var15);
                  double var22 = var18.getHeight(var4, var6);
                  double var24 = Math.max(var20, var22);
                  ParticleOptions var26 = !var18.is(FluidTags.LAVA) && !var17.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(var17)
                     ? ParticleTypes.RAIN
                     : ParticleTypes.SMOKE;
                  this.minecraft.level.addParticle(var26, var6.getX() + var13, var6.getY() + var24, var6.getZ() + var15, 0.0, 0.0, 0.0);
               }
            }
         }

         if (var6 != null && var3.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (var6.getY() > var5.getY() + 1 && var4.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var5).getY() > Mth.floor(var5.getY())) {
               this.minecraft.level.playLocalSound(var6, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               this.minecraft.level.playLocalSound(var6, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }
      }
   }

   @Override
   public void close() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.close();
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      this.initOutline();
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      }
   }

   public void initOutline() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      ResourceLocation var1 = new ResourceLocation("shaders/post/entity_outline.json");

      try {
         this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), var1);
         this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         this.entityTarget = this.entityEffect.getTempTarget("final");
      } catch (IOException var2) {
         LOGGER.warn("Failed to load shader: {}", var1, var2);
         this.entityEffect = null;
         this.entityTarget = null;
      } catch (JsonSyntaxException var2) {
         LOGGER.warn("Failed to parse shader: {}", var1, var2);
         this.entityEffect = null;
         this.entityTarget = null;
      }
   }

   private void initTransparency() {
      this.deinitTransparency();
      ResourceLocation var1 = new ResourceLocation("shaders/post/transparency.json");

      try {
         PostChain var2 = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), var1);
         var2.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         RenderTarget var3 = var2.getTempTarget("translucent");
         RenderTarget var4 = var2.getTempTarget("itemEntity");
         RenderTarget var5 = var2.getTempTarget("particles");
         RenderTarget var6 = var2.getTempTarget("weather");
         RenderTarget var7 = var2.getTempTarget("clouds");
         this.transparencyChain = var2;
         this.translucentTarget = var3;
         this.itemEntityTarget = var4;
         this.particlesTarget = var5;
         this.weatherTarget = var6;
         this.cloudsTarget = var7;
      } catch (Exception var2) {
         String var3 = var2 instanceof JsonSyntaxException ? "parse" : "load";
         String var4 = "Failed to " + var3 + " shader: " + var1;
         LevelRenderer.TransparencyShaderException var5 = new LevelRenderer.TransparencyShaderException(var4, var2);
         if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
            Component var6 = this.minecraft.getResourceManager().listPacks().findFirst().map(l_param0 -> Component.literal(l_param0.packId())).orElse(null);
            this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
            this.minecraft.clearResourcePacksOnError(var5, var6);
         } else {
            CrashReport var6 = this.minecraft.fillReport(new CrashReport(var4, var5));
            this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
            this.minecraft.options.save();
            LOGGER.error(LogUtils.FATAL_MARKER, var4, var5);
            this.minecraft.emergencySave();
            Minecraft.crash(var6);
         }
      }
   }

   private void deinitTransparency() {
      if (this.transparencyChain != null) {
         this.transparencyChain.close();
         this.translucentTarget.destroyBuffers();
         this.itemEntityTarget.destroyBuffers();
         this.particlesTarget.destroyBuffers();
         this.weatherTarget.destroyBuffers();
         this.cloudsTarget.destroyBuffers();
         this.transparencyChain = null;
         this.translucentTarget = null;
         this.itemEntityTarget = null;
         this.particlesTarget = null;
         this.weatherTarget = null;
         this.cloudsTarget = null;
      }
   }

   public void doEntityOutline() {
      if (this.shouldShowEntityOutlines()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ZERO,
            GlStateManager.DestFactor.ONE
         );
         this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   protected boolean shouldShowEntityOutlines() {
      return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null;
   }

   private void createDarkSky() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      if (this.darkBuffer != null) {
         this.darkBuffer.close();
      }

      this.darkBuffer = new VertexBuffer();
      BufferBuilder.RenderedBuffer var3 = buildSkyDisc(var2, -16.0F);
      this.darkBuffer.bind();
      this.darkBuffer.upload(var3);
      VertexBuffer.unbind();
   }

   private void createLightSky() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      if (this.skyBuffer != null) {
         this.skyBuffer.close();
      }

      this.skyBuffer = new VertexBuffer();
      BufferBuilder.RenderedBuffer var3 = buildSkyDisc(var2, 16.0F);
      this.skyBuffer.bind();
      this.skyBuffer.upload(var3);
      VertexBuffer.unbind();
   }

   private static BufferBuilder.RenderedBuffer buildSkyDisc(BufferBuilder param0, float param1) {
      float var2 = Math.signum(param1) * 512.0F;
      float var3 = 512.0F;
      RenderSystem.setShader(GameRenderer::getPositionShader);
      param0.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
      param0.vertex(0.0, param1, 0.0).endVertex();

      for (int var4 = -180; var4 <= 180; var4 += 45) {
         param0.vertex(var2 * Mth.cos(var4 * (float) (Math.PI / 180.0)), param1, 512.0F * Mth.sin(var4 * (float) (Math.PI / 180.0))).endVertex();
      }

      return param0.end();
   }

   private void createStars() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      if (this.starBuffer != null) {
         this.starBuffer.close();
      }

      this.starBuffer = new VertexBuffer();
      BufferBuilder.RenderedBuffer var3 = this.drawStars(var2);
      this.starBuffer.bind();
      this.starBuffer.upload(var3);
      VertexBuffer.unbind();
   }

   private BufferBuilder.RenderedBuffer drawStars(BufferBuilder param0) {
      RandomSource var2 = RandomSource.create(10842L);
      param0.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

      for (int var3 = 0; var3 < 1500; var3++) {
         double var4 = var2.nextFloat() * 2.0F - 1.0F;
         double var6 = var2.nextFloat() * 2.0F - 1.0F;
         double var8 = var2.nextFloat() * 2.0F - 1.0F;
         double var10 = 0.15F + var2.nextFloat() * 0.1F;
         double var12 = var4 * var4 + var6 * var6 + var8 * var8;
         if (var12 < 1.0 && var12 > 0.01) {
            var12 = 1.0 / Math.sqrt(var12);
            var4 *= var12;
            var6 *= var12;
            var8 *= var12;
            double var14 = var4 * 100.0;
            double var16 = var6 * 100.0;
            double var18 = var8 * 100.0;
            double var20 = Math.atan2(var4, var8);
            double var22 = Math.sin(var20);
            double var24 = Math.cos(var20);
            double var26 = Math.atan2(Math.sqrt(var4 * var4 + var8 * var8), var6);
            double var28 = Math.sin(var26);
            double var30 = Math.cos(var26);
            double var32 = var2.nextDouble() * Math.PI * 2.0;
            double var34 = Math.sin(var32);
            double var36 = Math.cos(var32);

            for (int var38 = 0; var38 < 4; var38++) {
               double var39 = 0.0;
               double var41 = ((var38 & 2) - 1) * var10;
               double var43 = ((var38 + 1 & 2) - 1) * var10;
               double var45 = 0.0;
               double var47 = var41 * var36 - var43 * var34;
               double var49 = var43 * var36 + var41 * var34;
               double var51 = var49;
               double var53 = var47 * var28 + 0.0 * var30;
               double var55 = 0.0 * var28 - var47 * var30;
               double var57 = var55 * var22 - var51 * var24;
               double var59 = var53;
               double var61 = var51 * var22 + var55 * var24;
               param0.vertex(var14 + var57, var16 + var59, var18 + var61).endVertex();
            }
         }
      }

      return param0.end();
   }

   public void setLevel(@Nullable ClientLevel param0) {
      this.lastCameraX = Double.MIN_VALUE;
      this.lastCameraY = Double.MIN_VALUE;
      this.lastCameraZ = Double.MIN_VALUE;
      this.lastCameraChunkX = Integer.MIN_VALUE;
      this.lastCameraChunkY = Integer.MIN_VALUE;
      this.lastCameraChunkZ = Integer.MIN_VALUE;
      this.entityRenderDispatcher.setLevel(param0);
      this.level = param0;
      if (param0 != null) {
         this.allChanged();
      } else {
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
         }

         if (this.chunkRenderDispatcher != null) {
            this.chunkRenderDispatcher.dispose();
         }

         this.chunkRenderDispatcher = null;
         this.globalBlockEntities.clear();
         this.renderChunkStorage.set(null);
         this.renderChunksInFrustum.clear();
      }
   }

   public void graphicsChanged() {
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      } else {
         this.deinitTransparency();
      }
   }

   public void allChanged() {
      if (this.level != null) {
         this.graphicsChanged();
         this.level.clearTintCaches();
         if (this.chunkRenderDispatcher == null) {
            this.chunkRenderDispatcher = new ChunkRenderDispatcher(
               this.level, this, Util.backgroundExecutor(), this.minecraft.is64Bit(), this.renderBuffers.fixedBufferPack()
            );
         } else {
            this.chunkRenderDispatcher.setLevel(this.level);
         }

         this.needsFullRenderChunkUpdate = true;
         this.generateClouds = true;
         this.recentlyCompiledChunks.clear();
         ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
         this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
         }

         this.chunkRenderDispatcher.blockUntilClear();
         synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.clear();
         }

         this.viewArea = new ViewArea(this.chunkRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
         if (this.lastFullRenderChunkUpdateFailed) {
            this.lastFullRenderChunkUpdateFailed = false;
            LOGGER.warn("Full update failed");
         }

         this.renderChunkStorage.set(new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length));
         this.renderChunksInFrustum.clear();
         Entity var1 = this.minecraft.getCameraEntity();
         if (var1 != null) {
            this.viewArea.repositionCamera(var1.getX(), var1.getZ());
         }
      }
   }

   public void resize(int param0, int param1) {
      this.needsUpdate();
      if (this.entityEffect != null) {
         this.entityEffect.resize(param0, param1);
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.resize(param0, param1);
      }
   }

   public String getChunkStatistics() {
      int var1 = this.viewArea.chunks.length;
      int var2 = this.countRenderedChunks();
      return String.format(
         Locale.ROOT,
         "C: %d/%d %sD: %d, %s",
         var2,
         var1,
         this.minecraft.smartCull ? "(s) " : "",
         this.lastViewDistance,
         this.chunkRenderDispatcher == null ? "null" : this.chunkRenderDispatcher.getStats()
      );
   }

   public ChunkRenderDispatcher getChunkRenderDispatcher() {
      return this.chunkRenderDispatcher;
   }

   public double getTotalChunks() {
      return this.viewArea.chunks.length;
   }

   public double getLastViewDistance() {
      return this.lastViewDistance;
   }

   public int countRenderedChunks() {
      int var1 = 0;

      for (LevelRenderer.RenderChunkInfo var3 : this.renderChunksInFrustum) {
         if (!var3.chunk.getCompiledChunk().hasNoRenderableLayers()) {
            var1++;
         }
      }

      return var1;
   }

   public String getEntityStatistics() {
      return "E: "
         + this.renderedEntities
         + "/"
         + this.level.getEntityCount()
         + ", B: "
         + this.culledEntities
         + ", SD: "
         + this.level.getServerSimulationDistance();
   }

   private void setupRender(Camera param0, Frustum param1, boolean param2, boolean param3) {
      Vec3 var5 = param0.getPosition();
      if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
         this.allChanged();
      }

      this.level.getProfiler().push("camera");
      double var6 = this.minecraft.player.getX();
      double var8 = this.minecraft.player.getY();
      double var10 = this.minecraft.player.getZ();
      int var12 = SectionPos.posToSectionCoord(var6);
      int var13 = SectionPos.posToSectionCoord(var8);
      int var14 = SectionPos.posToSectionCoord(var10);
      if (this.lastCameraChunkX != var12 || this.lastCameraChunkY != var13 || this.lastCameraChunkZ != var14) {
         this.lastCameraX = var6;
         this.lastCameraY = var8;
         this.lastCameraZ = var10;
         this.lastCameraChunkX = var12;
         this.lastCameraChunkY = var13;
         this.lastCameraChunkZ = var14;
         this.viewArea.repositionCamera(var6, var10);
      }

      this.chunkRenderDispatcher.setCamera(var5);
      this.level.getProfiler().popPush("cull");
      this.minecraft.getProfiler().popPush("culling");
      BlockPos var15 = param0.getBlockPosition();
      double var16 = Math.floor(var5.x / 8.0);
      double var18 = Math.floor(var5.y / 8.0);
      double var20 = Math.floor(var5.z / 8.0);
      this.needsFullRenderChunkUpdate = this.needsFullRenderChunkUpdate || var16 != this.prevCamX || var18 != this.prevCamY || var20 != this.prevCamZ;
      this.nextFullUpdateMillis.updateAndGet(l_param0 -> {
         if (l_param0 > 0L && System.currentTimeMillis() > l_param0) {
            this.needsFullRenderChunkUpdate = true;
            return 0L;
         } else {
            return l_param0;
         }
      });
      this.prevCamX = var16;
      this.prevCamY = var18;
      this.prevCamZ = var20;
      this.minecraft.getProfiler().popPush("update");
      boolean var22 = this.minecraft.smartCull;
      if (param3 && this.level.getBlockState(var15).isSolidRender(this.level, var15)) {
         var22 = false;
      }

      if (!param2) {
         if (this.needsFullRenderChunkUpdate && !this.lastFullRenderChunkUpdateInProgress) {
            this.minecraft.getProfiler().push("full_update_schedule");
            this.needsFullRenderChunkUpdate = false;
            boolean var23 = var22;
            // Util.backgroundExecutor() is a direct executor on WASM-GC, so this runs inline and
            // has already finished by the time the next line executes. Upstream instead submits
            // to a background pool and holds on to a Future to join before rebuilding the view
            // area; there is nothing to join here, so only the failure flag survives.
            try {
               Util.backgroundExecutor().execute(() -> {
                  Queue<LevelRenderer.RenderChunkInfo> l_var4 = Queues.newArrayDeque();
                  this.initializeQueueForFullUpdate(param0, l_var4);
                  LevelRenderer.RenderChunkStorage l_var5 = new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length);
                  this.updateRenderChunks(l_var5.renderChunks, l_var5.renderInfoMap, var5, l_var4, var23);
                  this.renderChunkStorage.set(l_var5);
                  this.needsFrustumUpdate.set(true);
               });
            } catch (Throwable var24) {
               this.lastFullRenderChunkUpdateFailed = true;
               throw var24;
            }
            this.minecraft.getProfiler().pop();
         }

         LevelRenderer.RenderChunkStorage var23 = this.renderChunkStorage.get();
         if (!this.recentlyCompiledChunks.isEmpty()) {
            this.minecraft.getProfiler().push("partial_update");
            Queue<LevelRenderer.RenderChunkInfo> var24 = Queues.newArrayDeque();

            while (!this.recentlyCompiledChunks.isEmpty()) {
               ChunkRenderDispatcher.RenderChunk var25 = this.recentlyCompiledChunks.poll();
               LevelRenderer.RenderChunkInfo var26 = var23.renderInfoMap.get(var25);
               if (var26 != null && var26.chunk == var25) {
                  var24.add(var26);
               }
            }

            this.updateRenderChunks(var23.renderChunks, var23.renderInfoMap, var5, var24, var22);
            this.needsFrustumUpdate.set(true);
            this.minecraft.getProfiler().pop();
         }

         double var24 = Math.floor(param0.getXRot() / 2.0F);
         double var26 = Math.floor(param0.getYRot() / 2.0F);
         if (this.needsFrustumUpdate.compareAndSet(true, false) || var24 != this.prevCamRotX || var26 != this.prevCamRotY) {
            this.applyFrustum(new Frustum(param1).offsetToFullyIncludeCameraCube(8));
            this.prevCamRotX = var24;
            this.prevCamRotY = var26;
         }
      }

      this.minecraft.getProfiler().pop();
   }

   private void applyFrustum(Frustum param0) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
      }

      this.minecraft.getProfiler().push("apply_frustum");
      this.renderChunksInFrustum.clear();

      for (LevelRenderer.RenderChunkInfo var3 : this.renderChunkStorage.get().renderChunks) {
         if (param0.isVisible(var3.chunk.getBoundingBox())) {
            this.renderChunksInFrustum.add(var3);
         }
      }

      this.minecraft.getProfiler().pop();
   }

   private void initializeQueueForFullUpdate(Camera param0, Queue<LevelRenderer.RenderChunkInfo> param1) {
      int var3 = 16;
      Vec3 var4 = param0.getPosition();
      BlockPos var5 = param0.getBlockPosition();
      ChunkRenderDispatcher.RenderChunk var6 = this.viewArea.getRenderChunkAt(var5);
      if (var6 == null) {
         boolean var7 = var5.getY() > this.level.getMinBuildHeight();
         int var8 = var7 ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
         int var9 = Mth.floor(var4.x / 16.0) * 16;
         int var10 = Mth.floor(var4.z / 16.0) * 16;
         List<LevelRenderer.RenderChunkInfo> var11 = Lists.newArrayList();

         for (int var12 = -this.lastViewDistance; var12 <= this.lastViewDistance; var12++) {
            for (int var13 = -this.lastViewDistance; var13 <= this.lastViewDistance; var13++) {
               ChunkRenderDispatcher.RenderChunk var14 = this.viewArea
                  .getRenderChunkAt(new BlockPos(var9 + SectionPos.sectionToBlockCoord(var12, 8), var8, var10 + SectionPos.sectionToBlockCoord(var13, 8)));
               if (var14 != null) {
                  var11.add(new LevelRenderer.RenderChunkInfo(var14, null, 0));
               }
            }
         }

         var11.sort(Comparator.comparingDouble(l_param1 -> var5.distSqr(l_param1.chunk.getOrigin().offset(8, 8, 8))));
         param1.addAll(var11);
      } else {
         param1.add(new LevelRenderer.RenderChunkInfo(var6, null, 0));
      }
   }

   public void addRecentlyCompiledChunk(ChunkRenderDispatcher.RenderChunk param0) {
      this.recentlyCompiledChunks.add(param0);
   }

   private void updateRenderChunks(
      LinkedHashSet<LevelRenderer.RenderChunkInfo> param0,
      LevelRenderer.RenderInfoMap param1,
      Vec3 param2,
      Queue<LevelRenderer.RenderChunkInfo> param3,
      boolean param4
   ) {
      int var6 = 16;
      BlockPos var7 = new BlockPos(Mth.floor(param2.x / 16.0) * 16, Mth.floor(param2.y / 16.0) * 16, Mth.floor(param2.z / 16.0) * 16);
      BlockPos var8 = var7.offset(8, 8, 8);
      Entity.setViewScale(Mth.clamp(this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());

      while (!param3.isEmpty()) {
         LevelRenderer.RenderChunkInfo var9 = param3.poll();
         ChunkRenderDispatcher.RenderChunk var10 = var9.chunk;
         param0.add(var9);
         boolean var11 = Math.abs(var10.getOrigin().getX() - var7.getX()) > 60
            || Math.abs(var10.getOrigin().getY() - var7.getY()) > 60
            || Math.abs(var10.getOrigin().getZ() - var7.getZ()) > 60;

         for (Direction var15 : DIRECTIONS) {
            ChunkRenderDispatcher.RenderChunk var16 = this.getRelativeFrom(var7, var10, var15);
            if (var16 != null && (!param4 || !var9.hasDirection(var15.getOpposite()))) {
               if (param4 && var9.hasSourceDirections()) {
                  ChunkRenderDispatcher.CompiledChunk var17 = var10.getCompiledChunk();
                  boolean var18 = false;

                  for (int var19 = 0; var19 < DIRECTIONS.length; var19++) {
                     if (var9.hasSourceDirection(var19) && var17.facesCanSeeEachother(DIRECTIONS[var19].getOpposite(), var15)) {
                        var18 = true;
                        break;
                     }
                  }

                  if (!var18) {
                     continue;
                  }
               }

               if (param4 && var11) {
                  BlockPos var17 = var16.getOrigin();
                  BlockPos var18 = var17.offset(
                     (var15.getAxis() == Direction.Axis.X ? var8.getX() <= var17.getX() : var8.getX() >= var17.getX()) ? 0 : 16,
                     (var15.getAxis() == Direction.Axis.Y ? var8.getY() <= var17.getY() : var8.getY() >= var17.getY()) ? 0 : 16,
                     (var15.getAxis() == Direction.Axis.Z ? var8.getZ() <= var17.getZ() : var8.getZ() >= var17.getZ()) ? 0 : 16
                  );
                  Vec3 var19 = new Vec3(var18.getX(), var18.getY(), var18.getZ());
                  Vec3 var20 = param2.subtract(var19).normalize().scale(CEILED_SECTION_DIAGONAL);
                  boolean var21 = true;

                  while (param2.subtract(var19).lengthSqr() > 3600.0) {
                     var19 = var19.add(var20);
                     if (var19.y > this.level.getMaxBuildHeight() || var19.y < this.level.getMinBuildHeight()) {
                        break;
                     }

                     ChunkRenderDispatcher.RenderChunk var22 = this.viewArea.getRenderChunkAt(BlockPos.containing(var19.x, var19.y, var19.z));
                     if (var22 == null || param1.get(var22) == null) {
                        var21 = false;
                        break;
                     }
                  }

                  if (!var21) {
                     continue;
                  }
               }

               LevelRenderer.RenderChunkInfo var17 = param1.get(var16);
               if (var17 != null) {
                  var17.addSourceDirection(var15);
               } else if (!var16.hasAllNeighbors()) {
                  if (!this.closeToBorder(var7, var10)) {
                     this.nextFullUpdateMillis.set(System.currentTimeMillis() + 500L);
                  }
               } else {
                  LevelRenderer.RenderChunkInfo var18 = new LevelRenderer.RenderChunkInfo(var16, var15, var9.step + 1);
                  var18.setDirections(var9.directions, var15);
                  param3.add(var18);
                  param1.put(var16, var18);
               }
            }
         }
      }
   }

   @Nullable
   private ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos param0, ChunkRenderDispatcher.RenderChunk param1, Direction param2) {
      BlockPos var4 = param1.getRelativeOrigin(param2);
      if (Mth.abs(param0.getX() - var4.getX()) > this.lastViewDistance * 16) {
         return null;
      } else if (Mth.abs(param0.getY() - var4.getY()) > this.lastViewDistance * 16
         || var4.getY() < this.level.getMinBuildHeight()
         || var4.getY() >= this.level.getMaxBuildHeight()) {
         return null;
      } else {
         return Mth.abs(param0.getZ() - var4.getZ()) > this.lastViewDistance * 16 ? null : this.viewArea.getRenderChunkAt(var4);
      }
   }

   private boolean closeToBorder(BlockPos param0, ChunkRenderDispatcher.RenderChunk param1) {
      int var3 = SectionPos.blockToSectionCoord(param0.getX());
      int var4 = SectionPos.blockToSectionCoord(param0.getZ());
      BlockPos var5 = param1.getOrigin();
      int var6 = SectionPos.blockToSectionCoord(var5.getX());
      int var7 = SectionPos.blockToSectionCoord(var5.getZ());
      return !ChunkMap.isChunkInRange(var6, var7, var3, var4, this.lastViewDistance - 2);
   }

   private void captureFrustum(Matrix4f param0, Matrix4f param1, double param2, double param4, double param6, Frustum param8) {
      this.capturedFrustum = param8;
      Matrix4f var10 = new Matrix4f(param1);
      var10.mul(param0);
      var10.invert();
      this.frustumPos.x = param2;
      this.frustumPos.y = param4;
      this.frustumPos.z = param6;
      this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

      for (int var11 = 0; var11 < 8; var11++) {
         var10.transform(this.frustumPoints[var11]);
         this.frustumPoints[var11].div(this.frustumPoints[var11].w());
      }
   }

   public void prepareCullFrustum(PoseStack param0, Vec3 param1, Matrix4f param2) {
      Matrix4f var4 = param0.last().pose();
      double var5 = param1.x();
      double var7 = param1.y();
      double var9 = param1.z();
      this.cullingFrustum = new Frustum(var4, param2);
      this.cullingFrustum.prepare(var5, var7, var9);
   }

   public void renderLevel(
      PoseStack param0, float param1, long param2, boolean param4, Camera param5, GameRenderer param6, LightTexture param7, Matrix4f param8
   ) {
      RenderSystem.setShaderGameTime(this.level.getGameTime(), param1);
      this.blockEntityRenderDispatcher.prepare(this.level, param5, this.minecraft.hitResult);
      this.entityRenderDispatcher.prepare(this.level, param5, this.minecraft.crosshairPickEntity);
      ProfilerFiller var10 = this.level.getProfiler();
      var10.popPush("light_update_queue");
      this.level.pollLightUpdates();
      var10.popPush("light_updates");
      boolean var11 = this.level.isLightUpdateQueueEmpty();
      this.level.getChunkSource().getLightEngine().runUpdates(Integer.MAX_VALUE, var11, true);
      Vec3 var12 = param5.getPosition();
      double var13 = var12.x();
      double var15 = var12.y();
      double var17 = var12.z();
      Matrix4f var19 = param0.last().pose();
      var10.popPush("culling");
      boolean var20 = this.capturedFrustum != null;
      Frustum var21;
      if (var20) {
         var21 = this.capturedFrustum;
         var21.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
      } else {
         var21 = this.cullingFrustum;
      }

      this.minecraft.getProfiler().popPush("captureFrustum");
      if (this.captureFrustum) {
         this.captureFrustum(var19, param8, var12.x, var12.y, var12.z, var20 ? new Frustum(var19, param8) : var21);
         this.captureFrustum = false;
      }

      var10.popPush("clear");
      FogRenderer.setupColor(param5, param1, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), param6.getDarkenWorldAmount(param1));
      FogRenderer.levelFogColor();
      RenderSystem.clear(16640, Minecraft.ON_OSX);
      float var22 = param6.getRenderDistance();
      boolean var23 = this.minecraft.level.effects().isFoggyAt(Mth.floor(var13), Mth.floor(var15))
         || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      var10.popPush("sky");
      RenderSystem.setShader(GameRenderer::getPositionShader);
      this.renderSky(param0, param8, param1, param5, var23, () -> FogRenderer.setupFog(param5, FogRenderer.FogMode.FOG_SKY, var22, var23, param1));
      var10.popPush("fog");
      FogRenderer.setupFog(param5, FogRenderer.FogMode.FOG_TERRAIN, Math.max(var22, 32.0F), var23, param1);
      var10.popPush("terrain_setup");
      this.setupRender(param5, var21, var20, this.minecraft.player.isSpectator());
      var10.popPush("compilechunks");
      this.compileChunks(param5);
      var10.popPush("terrain");
      this.renderChunkLayer(RenderType.solid(), param0, var13, var15, var17, param8);
      this.renderChunkLayer(RenderType.cutoutMipped(), param0, var13, var15, var17, param8);
      this.renderChunkLayer(RenderType.cutout(), param0, var13, var15, var17, param8);
      if (this.level.effects().constantAmbientLight()) {
         Lighting.setupNetherLevel(param0.last().pose());
      } else {
         Lighting.setupLevel(param0.last().pose());
      }

      var10.popPush("entities");
      this.renderedEntities = 0;
      this.culledEntities = 0;
      if (this.itemEntityTarget != null) {
         this.itemEntityTarget.clear(Minecraft.ON_OSX);
         this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      if (this.weatherTarget != null) {
         this.weatherTarget.clear(Minecraft.ON_OSX);
      }

      if (this.shouldShowEntityOutlines()) {
         this.entityTarget.clear(Minecraft.ON_OSX);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      boolean var24 = false;
      MultiBufferSource.BufferSource var25 = this.renderBuffers.bufferSource();

      for (Entity var27 : this.level.entitiesForRendering()) {
         if (this.entityRenderDispatcher.shouldRender(var27, var21, var13, var15, var17) || var27.hasIndirectPassenger(this.minecraft.player)) {
            BlockPos var28 = var27.blockPosition();
            if ((this.level.isOutsideBuildHeight(var28.getY()) || this.isChunkCompiled(var28))
               && (
                  var27 != param5.getEntity()
                     || param5.isDetached()
                     || param5.getEntity() instanceof LivingEntity && ((LivingEntity)param5.getEntity()).isSleeping()
               )
               && (!(var27 instanceof LocalPlayer) || param5.getEntity() == var27)) {
               this.renderedEntities++;
               if (var27.tickCount == 0) {
                  var27.xOld = var27.getX();
                  var27.yOld = var27.getY();
                  var27.zOld = var27.getZ();
               }

               MultiBufferSource var29;
               if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(var27)) {
                  var24 = true;
                  OutlineBufferSource var30 = this.renderBuffers.outlineBufferSource();
                  var29 = var30;
                  int var31 = var27.getTeamColor();
                  var30.setColor(FastColor.ARGB32.red(var31), FastColor.ARGB32.green(var31), FastColor.ARGB32.blue(var31), 255);
               } else {
                  var29 = var25;
               }

               this.renderEntity(var27, var13, var15, var17, param1, param0, var29);
            }
         }
      }

      var25.endLastBatch();
      this.checkPoseStack(param0);
      var25.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
      var25.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
      var25.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
      var25.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
      var10.popPush("blockentities");

      for (LevelRenderer.RenderChunkInfo var27 : this.renderChunksInFrustum) {
         List<BlockEntity> var28 = var27.chunk.getCompiledChunk().getRenderableBlockEntities();
         if (!var28.isEmpty()) {
            for (BlockEntity var30 : var28) {
               BlockPos var31 = var30.getBlockPos();
               MultiBufferSource var32 = var25;
               param0.pushPose();
               param0.translate(var31.getX() - var13, var31.getY() - var15, var31.getZ() - var17);
               SortedSet<BlockDestructionProgress> var33 = this.destructionProgress.get(var31.asLong());
               if (var33 != null && !var33.isEmpty()) {
                  int var34 = var33.last().getProgress();
                  if (var34 >= 0) {
                     PoseStack.Pose var35 = param0.last();
                     VertexConsumer var36 = new SheetedDecalTextureGenerator(
                        this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(var34)), var35.pose(), var35.normal(), 1.0F
                     );
                     var32 = l_param2 -> {
                        VertexConsumer l_var3 = var25.getBuffer(l_param2);
                        return l_param2.affectsCrumbling() ? VertexMultiConsumer.create(var36, l_var3) : l_var3;
                     };
                  }
               }

               this.blockEntityRenderDispatcher.render(var30, param1, param0, var32);
               param0.popPose();
            }
         }
      }

      synchronized (this.globalBlockEntities) {
         for (BlockEntity var28 : this.globalBlockEntities) {
            BlockPos var29 = var28.getBlockPos();
            param0.pushPose();
            param0.translate(var29.getX() - var13, var29.getY() - var15, var29.getZ() - var17);
            this.blockEntityRenderDispatcher.render(var28, param1, param0, var25);
            param0.popPose();
         }
      }

      this.checkPoseStack(param0);
      var25.endBatch(RenderType.solid());
      var25.endBatch(RenderType.endPortal());
      var25.endBatch(RenderType.endGateway());
      var25.endBatch(Sheets.solidBlockSheet());
      var25.endBatch(Sheets.cutoutBlockSheet());
      var25.endBatch(Sheets.bedSheet());
      var25.endBatch(Sheets.shulkerBoxSheet());
      var25.endBatch(Sheets.signSheet());
      var25.endBatch(Sheets.hangingSignSheet());
      var25.endBatch(Sheets.chestSheet());
      this.renderBuffers.outlineBufferSource().endOutlineBatch();
      if (var24) {
         this.entityEffect.process(param1);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      var10.popPush("destroyProgress");

      for (Entry<SortedSet<BlockDestructionProgress>> var27 : this.destructionProgress.long2ObjectEntrySet()) {
         BlockPos var28 = BlockPos.of(var27.getLongKey());
         double var29 = var28.getX() - var13;
         double var31 = var28.getY() - var15;
         double var33 = var28.getZ() - var17;
         if (!(var29 * var29 + var31 * var31 + var33 * var33 > 1024.0)) {
            SortedSet<BlockDestructionProgress> var35 = var27.getValue();
            if (var35 != null && !var35.isEmpty()) {
               int var36 = var35.last().getProgress();
               param0.pushPose();
               param0.translate(var28.getX() - var13, var28.getY() - var15, var28.getZ() - var17);
               PoseStack.Pose var37 = param0.last();
               VertexConsumer var38 = new SheetedDecalTextureGenerator(
                  this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(var36)), var37.pose(), var37.normal(), 1.0F
               );
               this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(var28), var28, this.level, param0, var38);
               param0.popPose();
            }
         }
      }

      this.checkPoseStack(param0);
      HitResult var26 = this.minecraft.hitResult;
      if (param4 && var26 != null && var26.getType() == HitResult.Type.BLOCK) {
         var10.popPush("outline");
         BlockPos var27 = ((BlockHitResult)var26).getBlockPos();
         BlockState var28 = this.level.getBlockState(var27);
         if (!var28.isAir() && this.level.getWorldBorder().isWithinBounds(var27)) {
            VertexConsumer var29 = var25.getBuffer(RenderType.lines());
            this.renderHitOutline(param0, var29, param5.getEntity(), var13, var15, var17, var27, var28);
         }
      }

      this.minecraft.debugRenderer.render(param0, var25, var13, var15, var17);
      var25.endLastBatch();
      PoseStack var27 = RenderSystem.getModelViewStack();
      RenderSystem.applyModelViewMatrix();
      var25.endBatch(Sheets.translucentCullBlockSheet());
      var25.endBatch(Sheets.bannerSheet());
      var25.endBatch(Sheets.shieldSheet());
      var25.endBatch(RenderType.armorGlint());
      var25.endBatch(RenderType.armorEntityGlint());
      var25.endBatch(RenderType.glint());
      var25.endBatch(RenderType.glintDirect());
      var25.endBatch(RenderType.glintTranslucent());
      var25.endBatch(RenderType.entityGlint());
      var25.endBatch(RenderType.entityGlintDirect());
      var25.endBatch(RenderType.waterMask());
      this.renderBuffers.crumblingBufferSource().endBatch();
      if (this.transparencyChain != null) {
         var25.endBatch(RenderType.lines());
         var25.endBatch();
         this.translucentTarget.clear(Minecraft.ON_OSX);
         this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         var10.popPush("translucent");
         this.renderChunkLayer(RenderType.translucent(), param0, var13, var15, var17, param8);
         var10.popPush("string");
         this.renderChunkLayer(RenderType.tripwire(), param0, var13, var15, var17, param8);
         this.particlesTarget.clear(Minecraft.ON_OSX);
         this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         RenderStateShard.PARTICLES_TARGET.setupRenderState();
         var10.popPush("particles");
         this.minecraft.particleEngine.render(param0, var25, param7, param5, param1);
         RenderStateShard.PARTICLES_TARGET.clearRenderState();
      } else {
         var10.popPush("translucent");
         if (this.translucentTarget != null) {
            this.translucentTarget.clear(Minecraft.ON_OSX);
         }

         this.renderChunkLayer(RenderType.translucent(), param0, var13, var15, var17, param8);
         var25.endBatch(RenderType.lines());
         var25.endBatch();
         var10.popPush("string");
         this.renderChunkLayer(RenderType.tripwire(), param0, var13, var15, var17, param8);
         var10.popPush("particles");
         this.minecraft.particleEngine.render(param0, var25, param7, param5, param1);
      }

      var27.pushPose();
      var27.mulPoseMatrix(param0.last().pose());
      RenderSystem.applyModelViewMatrix();
      if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
         if (this.transparencyChain != null) {
            this.cloudsTarget.clear(Minecraft.ON_OSX);
            RenderStateShard.CLOUDS_TARGET.setupRenderState();
            var10.popPush("clouds");
            this.renderClouds(param0, param8, param1, var13, var15, var17);
            RenderStateShard.CLOUDS_TARGET.clearRenderState();
         } else {
            var10.popPush("clouds");
            RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            this.renderClouds(param0, param8, param1, var13, var15, var17);
         }
      }

      if (this.transparencyChain != null) {
         RenderStateShard.WEATHER_TARGET.setupRenderState();
         var10.popPush("weather");
         this.renderSnowAndRain(param7, param1, var13, var15, var17);
         this.renderWorldBorder(param5);
         RenderStateShard.WEATHER_TARGET.clearRenderState();
         this.transparencyChain.process(param1);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      } else {
         RenderSystem.depthMask(false);
         var10.popPush("weather");
         this.renderSnowAndRain(param7, param1, var13, var15, var17);
         this.renderWorldBorder(param5);
         RenderSystem.depthMask(true);
      }

      var27.popPose();
      RenderSystem.applyModelViewMatrix();
      this.renderDebug(param0, var25, param5);
      var25.endLastBatch();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      FogRenderer.setupNoFog();
   }

   private void checkPoseStack(PoseStack param0) {
      if (!param0.clear()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void renderEntity(Entity param0, double param1, double param3, double param5, float param7, PoseStack param8, MultiBufferSource param9) {
      double var11 = Mth.lerp(param7, param0.xOld, param0.getX());
      double var13 = Mth.lerp(param7, param0.yOld, param0.getY());
      double var15 = Mth.lerp(param7, param0.zOld, param0.getZ());
      float var17 = Mth.lerp(param7, param0.yRotO, param0.getYRot());
      this.entityRenderDispatcher
         .render(
            param0,
            var11 - param1,
            var13 - param3,
            var15 - param5,
            var17,
            param7,
            param8,
            param9,
            this.entityRenderDispatcher.getPackedLightCoords(param0, param7)
         );
   }

   private void renderChunkLayer(RenderType param0, PoseStack param1, double param2, double param4, double param6, Matrix4f param8) {
      RenderSystem.assertOnRenderThread();
      param0.setupRenderState();
      if (param0 == RenderType.translucent()) {
         this.minecraft.getProfiler().push("translucent_sort");
         double var10 = param2 - this.xTransparentOld;
         double var12 = param4 - this.yTransparentOld;
         double var14 = param6 - this.zTransparentOld;
         if (var10 * var10 + var12 * var12 + var14 * var14 > 1.0) {
            int var16 = SectionPos.posToSectionCoord(param2);
            int var17 = SectionPos.posToSectionCoord(param4);
            int var18 = SectionPos.posToSectionCoord(param6);
            boolean var19 = var16 != SectionPos.posToSectionCoord(this.xTransparentOld)
               || var18 != SectionPos.posToSectionCoord(this.zTransparentOld)
               || var17 != SectionPos.posToSectionCoord(this.yTransparentOld);
            this.xTransparentOld = param2;
            this.yTransparentOld = param4;
            this.zTransparentOld = param6;
            int var20 = 0;

            for (LevelRenderer.RenderChunkInfo var22 : this.renderChunksInFrustum) {
               if (var20 < 15 && (var19 || var22.isAxisAlignedWith(var16, var17, var18)) && var22.chunk.resortTransparency(param0, this.chunkRenderDispatcher)) {
                  var20++;
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }

      this.minecraft.getProfiler().push("filterempty");
      this.minecraft.getProfiler().popPush(() -> "render_" + param0);
      boolean var10 = param0 != RenderType.translucent();
      ObjectListIterator<LevelRenderer.RenderChunkInfo> var11 = this.renderChunksInFrustum.listIterator(var10 ? 0 : this.renderChunksInFrustum.size());
      ShaderInstance var12 = RenderSystem.getShader();

      for (int var13 = 0; var13 < 12; var13++) {
         int var14 = RenderSystem.getShaderTexture(var13);
         var12.setSampler("Sampler" + var13, var14);
      }

      if (var12.MODEL_VIEW_MATRIX != null) {
         var12.MODEL_VIEW_MATRIX.set(param1.last().pose());
      }

      if (var12.PROJECTION_MATRIX != null) {
         var12.PROJECTION_MATRIX.set(param8);
      }

      if (var12.COLOR_MODULATOR != null) {
         var12.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (var12.GLINT_ALPHA != null) {
         var12.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
      }

      if (var12.FOG_START != null) {
         var12.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (var12.FOG_END != null) {
         var12.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (var12.FOG_COLOR != null) {
         var12.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (var12.FOG_SHAPE != null) {
         var12.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
      }

      if (var12.TEXTURE_MATRIX != null) {
         var12.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (var12.GAME_TIME != null) {
         var12.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      RenderSystem.setupShaderLights(var12);
      var12.apply();
      Uniform var13 = var12.CHUNK_OFFSET;

      while (var10 ? var11.hasNext() : var11.hasPrevious()) {
         LevelRenderer.RenderChunkInfo var14 = var10 ? var11.next() : var11.previous();
         ChunkRenderDispatcher.RenderChunk var15 = var14.chunk;
         if (!var15.getCompiledChunk().isEmpty(param0)) {
            VertexBuffer var16 = var15.getBuffer(param0);
            BlockPos var17 = var15.getOrigin();
            if (var13 != null) {
               var13.set((float)(var17.getX() - param2), (float)(var17.getY() - param4), (float)(var17.getZ() - param6));
               var13.upload();
            }

            var16.bind();
            var16.draw();
         }
      }

      if (var13 != null) {
         var13.set(0.0F, 0.0F, 0.0F);
      }

      var12.clear();
      VertexBuffer.unbind();
      this.minecraft.getProfiler().pop();
      param0.clearRenderState();
   }

   private void renderDebug(PoseStack param0, MultiBufferSource param1, Camera param2) {
      if (this.minecraft.chunkPath || this.minecraft.chunkVisibility) {
         double var4 = param2.getPosition().x();
         double var6 = param2.getPosition().y();
         double var8 = param2.getPosition().z();

         for (LevelRenderer.RenderChunkInfo var11 : this.renderChunksInFrustum) {
            ChunkRenderDispatcher.RenderChunk var12 = var11.chunk;
            BlockPos var13 = var12.getOrigin();
            param0.pushPose();
            param0.translate(var13.getX() - var4, var13.getY() - var6, var13.getZ() - var8);
            Matrix4f var14 = param0.last().pose();
            if (this.minecraft.chunkPath) {
               VertexConsumer var15 = param1.getBuffer(RenderType.lines());
               int var16 = var11.step == 0 ? 0 : Mth.hsvToRgb(var11.step / 50.0F, 0.9F, 0.9F);
               int var17 = var16 >> 16 & 0xFF;
               int var18 = var16 >> 8 & 0xFF;
               int var19 = var16 & 0xFF;

               for (int var20 = 0; var20 < DIRECTIONS.length; var20++) {
                  if (var11.hasSourceDirection(var20)) {
                     Direction var21 = DIRECTIONS[var20];
                     var15.vertex(var14, 8.0F, 8.0F, 8.0F)
                        .color(var17, var18, var19, 255)
                        .normal(var21.getStepX(), var21.getStepY(), var21.getStepZ())
                        .endVertex();
                     var15.vertex(var14, 8 - 16 * var21.getStepX(), 8 - 16 * var21.getStepY(), 8 - 16 * var21.getStepZ())
                        .color(var17, var18, var19, 255)
                        .normal(var21.getStepX(), var21.getStepY(), var21.getStepZ())
                        .endVertex();
                  }
               }
            }

            if (this.minecraft.chunkVisibility && !var12.getCompiledChunk().hasNoRenderableLayers()) {
               VertexConsumer var15 = param1.getBuffer(RenderType.lines());
               int var16 = 0;

               for (Direction var20 : DIRECTIONS) {
                  for (Direction var24 : DIRECTIONS) {
                     boolean var25 = var12.getCompiledChunk().facesCanSeeEachother(var20, var24);
                     if (!var25) {
                        var16++;
                        var15.vertex(var14, 8 + 8 * var20.getStepX(), 8 + 8 * var20.getStepY(), 8 + 8 * var20.getStepZ())
                           .color(255, 0, 0, 255)
                           .normal(var20.getStepX(), var20.getStepY(), var20.getStepZ())
                           .endVertex();
                        var15.vertex(var14, 8 + 8 * var24.getStepX(), 8 + 8 * var24.getStepY(), 8 + 8 * var24.getStepZ())
                           .color(255, 0, 0, 255)
                           .normal(var24.getStepX(), var24.getStepY(), var24.getStepZ())
                           .endVertex();
                     }
                  }
               }

               if (var16 > 0) {
                  VertexConsumer var17 = param1.getBuffer(RenderType.debugQuads());
                  float var18 = 0.5F;
                  float var19 = 0.2F;
                  var17.vertex(var14, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  var17.vertex(var14, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
               }
            }

            param0.popPose();
         }
      }

      if (this.capturedFrustum != null) {
         param0.pushPose();
         param0.translate(
            (float)(this.frustumPos.x - param2.getPosition().x),
            (float)(this.frustumPos.y - param2.getPosition().y),
            (float)(this.frustumPos.z - param2.getPosition().z)
         );
         Matrix4f var4 = param0.last().pose();
         VertexConsumer var5 = param1.getBuffer(RenderType.debugQuads());
         this.addFrustumQuad(var5, var4, 0, 1, 2, 3, 0, 1, 1);
         this.addFrustumQuad(var5, var4, 4, 5, 6, 7, 1, 0, 0);
         this.addFrustumQuad(var5, var4, 0, 1, 5, 4, 1, 1, 0);
         this.addFrustumQuad(var5, var4, 2, 3, 7, 6, 0, 0, 1);
         this.addFrustumQuad(var5, var4, 0, 4, 7, 3, 0, 1, 0);
         this.addFrustumQuad(var5, var4, 1, 5, 6, 2, 1, 0, 1);
         VertexConsumer var6 = param1.getBuffer(RenderType.lines());
         this.addFrustumVertex(var6, var4, 0);
         this.addFrustumVertex(var6, var4, 1);
         this.addFrustumVertex(var6, var4, 1);
         this.addFrustumVertex(var6, var4, 2);
         this.addFrustumVertex(var6, var4, 2);
         this.addFrustumVertex(var6, var4, 3);
         this.addFrustumVertex(var6, var4, 3);
         this.addFrustumVertex(var6, var4, 0);
         this.addFrustumVertex(var6, var4, 4);
         this.addFrustumVertex(var6, var4, 5);
         this.addFrustumVertex(var6, var4, 5);
         this.addFrustumVertex(var6, var4, 6);
         this.addFrustumVertex(var6, var4, 6);
         this.addFrustumVertex(var6, var4, 7);
         this.addFrustumVertex(var6, var4, 7);
         this.addFrustumVertex(var6, var4, 4);
         this.addFrustumVertex(var6, var4, 0);
         this.addFrustumVertex(var6, var4, 4);
         this.addFrustumVertex(var6, var4, 1);
         this.addFrustumVertex(var6, var4, 5);
         this.addFrustumVertex(var6, var4, 2);
         this.addFrustumVertex(var6, var4, 6);
         this.addFrustumVertex(var6, var4, 3);
         this.addFrustumVertex(var6, var4, 7);
         param0.popPose();
      }
   }

   private void addFrustumVertex(VertexConsumer param0, Matrix4f param1, int param2) {
      param0.vertex(param1, this.frustumPoints[param2].x(), this.frustumPoints[param2].y(), this.frustumPoints[param2].z())
         .color(0, 0, 0, 255)
         .normal(0.0F, 0.0F, -1.0F)
         .endVertex();
   }

   private void addFrustumQuad(VertexConsumer param0, Matrix4f param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8) {
      float var10 = 0.25F;
      param0.vertex(param1, this.frustumPoints[param2].x(), this.frustumPoints[param2].y(), this.frustumPoints[param2].z())
         .color(param6, param7, param8, 0.25F)
         .endVertex();
      param0.vertex(param1, this.frustumPoints[param3].x(), this.frustumPoints[param3].y(), this.frustumPoints[param3].z())
         .color(param6, param7, param8, 0.25F)
         .endVertex();
      param0.vertex(param1, this.frustumPoints[param4].x(), this.frustumPoints[param4].y(), this.frustumPoints[param4].z())
         .color(param6, param7, param8, 0.25F)
         .endVertex();
      param0.vertex(param1, this.frustumPoints[param5].x(), this.frustumPoints[param5].y(), this.frustumPoints[param5].z())
         .color(param6, param7, param8, 0.25F)
         .endVertex();
   }

   public void captureFrustum() {
      this.captureFrustum = true;
   }

   public void killFrustum() {
      this.capturedFrustum = null;
   }

   public void tick() {
      this.ticks++;
      if (this.ticks % 20 == 0) {
         Iterator<BlockDestructionProgress> var1 = this.destroyingBlocks.values().iterator();

         while (var1.hasNext()) {
            BlockDestructionProgress var2 = var1.next();
            int var3 = var2.getUpdatedRenderTick();
            if (this.ticks - var3 > 400) {
               var1.remove();
               this.removeProgress(var2);
            }
         }
      }
   }

   private void removeProgress(BlockDestructionProgress param0) {
      long var2 = param0.getPos().asLong();
      Set<BlockDestructionProgress> var4 = this.destructionProgress.get(var2);
      var4.remove(param0);
      if (var4.isEmpty()) {
         this.destructionProgress.remove(var2);
      }
   }

   private void renderEndSky(PoseStack param0) {
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
      Tesselator var2 = Tesselator.getInstance();
      BufferBuilder var3 = var2.getBuilder();

      for (int var4 = 0; var4 < 6; var4++) {
         param0.pushPose();
         if (var4 == 1) {
            param0.mulPose(Axis.XP.rotationDegrees(90.0F));
         }

         if (var4 == 2) {
            param0.mulPose(Axis.XP.rotationDegrees(-90.0F));
         }

         if (var4 == 3) {
            param0.mulPose(Axis.XP.rotationDegrees(180.0F));
         }

         if (var4 == 4) {
            param0.mulPose(Axis.ZP.rotationDegrees(90.0F));
         }

         if (var4 == 5) {
            param0.mulPose(Axis.ZP.rotationDegrees(-90.0F));
         }

         Matrix4f var5 = param0.last().pose();
         var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         var3.vertex(var5, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         var2.end();
         param0.popPose();
      }

      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

   public void renderSky(PoseStack param0, Matrix4f param1, float param2, Camera param3, boolean param4, Runnable param5) {
      param5.run();
      if (!param4) {
         FogType var7 = param3.getFluidInCamera();
         if (var7 != FogType.POWDER_SNOW && var7 != FogType.LAVA && !this.doesMobEffectBlockSky(param3)) {
            if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
               this.renderEndSky(param0);
            } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
               Vec3 var8 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), param2);
               float var9 = (float)var8.x;
               float var10 = (float)var8.y;
               float var11 = (float)var8.z;
               FogRenderer.levelFogColor();
               BufferBuilder var12 = Tesselator.getInstance().getBuilder();
               RenderSystem.depthMask(false);
               RenderSystem.setShaderColor(var9, var10, var11, 1.0F);
               ShaderInstance var13 = RenderSystem.getShader();
               this.skyBuffer.bind();
               this.skyBuffer.drawWithShader(param0.last().pose(), param1, var13);
               VertexBuffer.unbind();
               RenderSystem.enableBlend();
               float[] var14 = this.level.effects().getSunriseColor(this.level.getTimeOfDay(param2), param2);
               if (var14 != null) {
                  RenderSystem.setShader(GameRenderer::getPositionColorShader);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  param0.pushPose();
                  param0.mulPose(Axis.XP.rotationDegrees(90.0F));
                  float var15 = Mth.sin(this.level.getSunAngle(param2)) < 0.0F ? 180.0F : 0.0F;
                  param0.mulPose(Axis.ZP.rotationDegrees(var15));
                  param0.mulPose(Axis.ZP.rotationDegrees(90.0F));
                  float var16 = var14[0];
                  float var17 = var14[1];
                  float var18 = var14[2];
                  Matrix4f var19 = param0.last().pose();
                  var12.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                  var12.vertex(var19, 0.0F, 100.0F, 0.0F).color(var16, var17, var18, var14[3]).endVertex();
                  int var20 = 16;

                  for (int var21 = 0; var21 <= 16; var21++) {
                     float var22 = var21 * (float) (Math.PI * 2) / 16.0F;
                     float var23 = Mth.sin(var22);
                     float var24 = Mth.cos(var22);
                     var12.vertex(var19, var23 * 120.0F, var24 * 120.0F, -var24 * 40.0F * var14[3]).color(var14[0], var14[1], var14[2], 0.0F).endVertex();
                  }

                  BufferUploader.drawWithShader(var12.end());
                  param0.popPose();
               }

               RenderSystem.blendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
               );
               param0.pushPose();
               float var15 = 1.0F - this.level.getRainLevel(param2);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var15);
               param0.mulPose(Axis.YP.rotationDegrees(-90.0F));
               param0.mulPose(Axis.XP.rotationDegrees(this.level.getTimeOfDay(param2) * 360.0F));
               Matrix4f var16 = param0.last().pose();
               float var17 = 30.0F;
               RenderSystem.setShader(GameRenderer::getPositionTexShader);
               RenderSystem.setShaderTexture(0, SUN_LOCATION);
               var12.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
               var12.vertex(var16, -var17, 100.0F, -var17).uv(0.0F, 0.0F).endVertex();
               var12.vertex(var16, var17, 100.0F, -var17).uv(1.0F, 0.0F).endVertex();
               var12.vertex(var16, var17, 100.0F, var17).uv(1.0F, 1.0F).endVertex();
               var12.vertex(var16, -var17, 100.0F, var17).uv(0.0F, 1.0F).endVertex();
               BufferUploader.drawWithShader(var12.end());
               var17 = 20.0F;
               RenderSystem.setShaderTexture(0, MOON_LOCATION);
               int var18 = this.level.getMoonPhase();
               int var19 = var18 % 4;
               int var20 = var18 / 4 % 2;
               float var21 = (var19 + 0) / 4.0F;
               float var22 = (var20 + 0) / 2.0F;
               float var23 = (var19 + 1) / 4.0F;
               float var24 = (var20 + 1) / 2.0F;
               var12.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
               var12.vertex(var16, -var17, -100.0F, var17).uv(var23, var24).endVertex();
               var12.vertex(var16, var17, -100.0F, var17).uv(var21, var24).endVertex();
               var12.vertex(var16, var17, -100.0F, -var17).uv(var21, var22).endVertex();
               var12.vertex(var16, -var17, -100.0F, -var17).uv(var23, var22).endVertex();
               BufferUploader.drawWithShader(var12.end());
               float var25 = this.level.getStarBrightness(param2) * var15;
               if (var25 > 0.0F) {
                  RenderSystem.setShaderColor(var25, var25, var25, var25);
                  FogRenderer.setupNoFog();
                  this.starBuffer.bind();
                  this.starBuffer.drawWithShader(param0.last().pose(), param1, GameRenderer.getPositionShader());
                  VertexBuffer.unbind();
                  param5.run();
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.disableBlend();
               RenderSystem.defaultBlendFunc();
               param0.popPose();
               RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
               double var15x = this.minecraft.player.getEyePosition(param2).y - this.level.getLevelData().getHorizonHeight(this.level);
               if (var15x < 0.0) {
                  param0.pushPose();
                  param0.translate(0.0F, 12.0F, 0.0F);
                  this.darkBuffer.bind();
                  this.darkBuffer.drawWithShader(param0.last().pose(), param1, var13);
                  VertexBuffer.unbind();
                  param0.popPose();
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.depthMask(true);
            }
         }
      }
   }

   private boolean doesMobEffectBlockSky(Camera param0) {
      return !(param0.getEntity() instanceof LivingEntity var2) ? false : var2.hasEffect(MobEffects.BLINDNESS) || var2.hasEffect(MobEffects.DARKNESS);
   }

   public void renderClouds(PoseStack param0, Matrix4f param1, float param2, double param3, double param5, double param7) {
      float var10 = this.level.effects().getCloudHeight();
      if (!Float.isNaN(var10)) {
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
         );
         RenderSystem.depthMask(true);
         float var11 = 12.0F;
         float var12 = 4.0F;
         double var13 = 2.0E-4;
         double var15 = (this.ticks + param2) * 0.03F;
         double var17 = (param3 + var15) / 12.0;
         double var19 = var10 - (float)param5 + 0.33F;
         double var21 = param7 / 12.0 + 0.33F;
         var17 -= Mth.floor(var17 / 2048.0) * 2048;
         var21 -= Mth.floor(var21 / 2048.0) * 2048;
         float var23 = (float)(var17 - Mth.floor(var17));
         float var24 = (float)(var19 / 4.0 - Mth.floor(var19 / 4.0)) * 4.0F;
         float var25 = (float)(var21 - Mth.floor(var21));
         Vec3 var26 = this.level.getCloudColor(param2);
         int var27 = (int)Math.floor(var17);
         int var28 = (int)Math.floor(var19 / 4.0);
         int var29 = (int)Math.floor(var21);
         if (var27 != this.prevCloudX
            || var28 != this.prevCloudY
            || var29 != this.prevCloudZ
            || this.minecraft.options.getCloudsType() != this.prevCloudsType
            || this.prevCloudColor.distanceToSqr(var26) > 2.0E-4) {
            this.prevCloudX = var27;
            this.prevCloudY = var28;
            this.prevCloudZ = var29;
            this.prevCloudColor = var26;
            this.prevCloudsType = this.minecraft.options.getCloudsType();
            this.generateClouds = true;
         }

         if (this.generateClouds) {
            this.generateClouds = false;
            BufferBuilder var30 = Tesselator.getInstance().getBuilder();
            if (this.cloudBuffer != null) {
               this.cloudBuffer.close();
            }

            this.cloudBuffer = new VertexBuffer();
            BufferBuilder.RenderedBuffer var31 = this.buildClouds(var30, var17, var19, var21, var26);
            this.cloudBuffer.bind();
            this.cloudBuffer.upload(var31);
            VertexBuffer.unbind();
         }

         RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
         RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
         FogRenderer.levelFogColor();
         param0.pushPose();
         param0.scale(12.0F, 1.0F, 12.0F);
         param0.translate(-var23, var24, -var25);
         if (this.cloudBuffer != null) {
            this.cloudBuffer.bind();
            int var30 = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

            for (int var31 = var30; var31 < 2; var31++) {
               if (var31 == 0) {
                  RenderSystem.colorMask(false, false, false, false);
               } else {
                  RenderSystem.colorMask(true, true, true, true);
               }

               ShaderInstance var32 = RenderSystem.getShader();
               this.cloudBuffer.drawWithShader(param0.last().pose(), param1, var32);
            }

            VertexBuffer.unbind();
         }

         param0.popPose();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   private BufferBuilder.RenderedBuffer buildClouds(BufferBuilder param0, double param1, double param3, double param5, Vec3 param7) {
      float var9 = 4.0F;
      float var10 = 0.00390625F;
      int var11 = 8;
      int var12 = 4;
      float var13 = 9.765625E-4F;
      float var14 = Mth.floor(param1) * 0.00390625F;
      float var15 = Mth.floor(param5) * 0.00390625F;
      float var16 = (float)param7.x;
      float var17 = (float)param7.y;
      float var18 = (float)param7.z;
      float var19 = var16 * 0.9F;
      float var20 = var17 * 0.9F;
      float var21 = var18 * 0.9F;
      float var22 = var16 * 0.7F;
      float var23 = var17 * 0.7F;
      float var24 = var18 * 0.7F;
      float var25 = var16 * 0.8F;
      float var26 = var17 * 0.8F;
      float var27 = var18 * 0.8F;
      RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
      param0.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
      float var28 = (float)Math.floor(param3 / 4.0) * 4.0F;
      if (this.prevCloudsType == CloudStatus.FANCY) {
         for (int var29 = -3; var29 <= 4; var29++) {
            for (int var30 = -3; var30 <= 4; var30++) {
               float var31 = var29 * 8;
               float var32 = var30 * 8;
               if (var28 > -5.0F) {
                  param0.vertex(var31 + 0.0F, var28 + 0.0F, var32 + 8.0F)
                     .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                     .color(var22, var23, var24, 0.8F)
                     .normal(0.0F, -1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 8.0F, var28 + 0.0F, var32 + 8.0F)
                     .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                     .color(var22, var23, var24, 0.8F)
                     .normal(0.0F, -1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 8.0F, var28 + 0.0F, var32 + 0.0F)
                     .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                     .color(var22, var23, var24, 0.8F)
                     .normal(0.0F, -1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 0.0F, var28 + 0.0F, var32 + 0.0F)
                     .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                     .color(var22, var23, var24, 0.8F)
                     .normal(0.0F, -1.0F, 0.0F)
                     .endVertex();
               }

               if (var28 <= 5.0F) {
                  param0.vertex(var31 + 0.0F, var28 + 4.0F - 9.765625E-4F, var32 + 8.0F)
                     .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                     .color(var16, var17, var18, 0.8F)
                     .normal(0.0F, 1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 8.0F, var28 + 4.0F - 9.765625E-4F, var32 + 8.0F)
                     .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                     .color(var16, var17, var18, 0.8F)
                     .normal(0.0F, 1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 8.0F, var28 + 4.0F - 9.765625E-4F, var32 + 0.0F)
                     .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                     .color(var16, var17, var18, 0.8F)
                     .normal(0.0F, 1.0F, 0.0F)
                     .endVertex();
                  param0.vertex(var31 + 0.0F, var28 + 4.0F - 9.765625E-4F, var32 + 0.0F)
                     .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                     .color(var16, var17, var18, 0.8F)
                     .normal(0.0F, 1.0F, 0.0F)
                     .endVertex();
               }

               if (var29 > -1) {
                  for (int var33 = 0; var33 < 8; var33++) {
                     param0.vertex(var31 + var33 + 0.0F, var28 + 0.0F, var32 + 8.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(-1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 0.0F, var28 + 4.0F, var32 + 8.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(-1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 0.0F, var28 + 4.0F, var32 + 0.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(-1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 0.0F, var28 + 0.0F, var32 + 0.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(-1.0F, 0.0F, 0.0F)
                        .endVertex();
                  }
               }

               if (var29 <= 1) {
                  for (int var33 = 0; var33 < 8; var33++) {
                     param0.vertex(var31 + var33 + 1.0F - 9.765625E-4F, var28 + 0.0F, var32 + 8.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 1.0F - 9.765625E-4F, var28 + 4.0F, var32 + 8.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 1.0F - 9.765625E-4F, var28 + 4.0F, var32 + 0.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(1.0F, 0.0F, 0.0F)
                        .endVertex();
                     param0.vertex(var31 + var33 + 1.0F - 9.765625E-4F, var28 + 0.0F, var32 + 0.0F)
                        .uv((var31 + var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15)
                        .color(var19, var20, var21, 0.8F)
                        .normal(1.0F, 0.0F, 0.0F)
                        .endVertex();
                  }
               }

               if (var30 > -1) {
                  for (int var33 = 0; var33 < 8; var33++) {
                     param0.vertex(var31 + 0.0F, var28 + 4.0F, var32 + var33 + 0.0F)
                        .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, -1.0F)
                        .endVertex();
                     param0.vertex(var31 + 8.0F, var28 + 4.0F, var32 + var33 + 0.0F)
                        .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, -1.0F)
                        .endVertex();
                     param0.vertex(var31 + 8.0F, var28 + 0.0F, var32 + var33 + 0.0F)
                        .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, -1.0F)
                        .endVertex();
                     param0.vertex(var31 + 0.0F, var28 + 0.0F, var32 + var33 + 0.0F)
                        .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, -1.0F)
                        .endVertex();
                  }
               }

               if (var30 <= 1) {
                  for (int var33 = 0; var33 < 8; var33++) {
                     param0.vertex(var31 + 0.0F, var28 + 4.0F, var32 + var33 + 1.0F - 9.765625E-4F)
                        .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, 1.0F)
                        .endVertex();
                     param0.vertex(var31 + 8.0F, var28 + 4.0F, var32 + var33 + 1.0F - 9.765625E-4F)
                        .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, 1.0F)
                        .endVertex();
                     param0.vertex(var31 + 8.0F, var28 + 0.0F, var32 + var33 + 1.0F - 9.765625E-4F)
                        .uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, 1.0F)
                        .endVertex();
                     param0.vertex(var31 + 0.0F, var28 + 0.0F, var32 + var33 + 1.0F - 9.765625E-4F)
                        .uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + var33 + 0.5F) * 0.00390625F + var15)
                        .color(var25, var26, var27, 0.8F)
                        .normal(0.0F, 0.0F, 1.0F)
                        .endVertex();
                  }
               }
            }
         }
      } else {
         int var29 = 1;
         int var30 = 32;

         for (int var31 = -32; var31 < 32; var31 += 32) {
            for (int var32 = -32; var32 < 32; var32 += 32) {
               param0.vertex(var31 + 0, var28, var32 + 32)
                  .uv((var31 + 0) * 0.00390625F + var14, (var32 + 32) * 0.00390625F + var15)
                  .color(var16, var17, var18, 0.8F)
                  .normal(0.0F, -1.0F, 0.0F)
                  .endVertex();
               param0.vertex(var31 + 32, var28, var32 + 32)
                  .uv((var31 + 32) * 0.00390625F + var14, (var32 + 32) * 0.00390625F + var15)
                  .color(var16, var17, var18, 0.8F)
                  .normal(0.0F, -1.0F, 0.0F)
                  .endVertex();
               param0.vertex(var31 + 32, var28, var32 + 0)
                  .uv((var31 + 32) * 0.00390625F + var14, (var32 + 0) * 0.00390625F + var15)
                  .color(var16, var17, var18, 0.8F)
                  .normal(0.0F, -1.0F, 0.0F)
                  .endVertex();
               param0.vertex(var31 + 0, var28, var32 + 0)
                  .uv((var31 + 0) * 0.00390625F + var14, (var32 + 0) * 0.00390625F + var15)
                  .color(var16, var17, var18, 0.8F)
                  .normal(0.0F, -1.0F, 0.0F)
                  .endVertex();
            }
         }
      }

      return param0.end();
   }

   private void compileChunks(Camera param0) {
      this.minecraft.getProfiler().push("populate_chunks_to_compile");
      RenderRegionCache var2 = new RenderRegionCache();
      BlockPos var3 = param0.getBlockPosition();
      List<ChunkRenderDispatcher.RenderChunk> var4 = Lists.newArrayList();
      List<ChunkRenderDispatcher.RenderChunk> var5 = Lists.newArrayList();
      PrioritizeChunkUpdates var6 = this.minecraft.options.prioritizeChunkUpdates().get();

      for (LevelRenderer.RenderChunkInfo var7 : this.renderChunksInFrustum) {
         ChunkRenderDispatcher.RenderChunk var8 = var7.chunk;
         ChunkPos var9 = new ChunkPos(var8.getOrigin());
         if (var8.isDirty() && this.level.getChunk(var9.x, var9.z).isClientLightReady()) {
            boolean var10 = false;
            if (var6 == PrioritizeChunkUpdates.NEARBY) {
               BlockPos var11 = var8.getOrigin().offset(8, 8, 8);
               var10 = var11.distSqr(var3) < 768.0 || var8.isDirtyFromPlayer();
            } else if (var6 == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
               var10 = var8.isDirtyFromPlayer();
            }

            (var10 ? var4 : var5).add(var8);
         }
      }

      this.minecraft.getProfiler().popPush("upload");
      this.chunkRenderDispatcher.uploadAllPendingUploads();
      this.minecraft.getProfiler().popPush("schedule_async_compile");

      for (ChunkRenderDispatcher.RenderChunk var7 : var4) {
         var7.rebuildChunkAsync(this.chunkRenderDispatcher, var2);
         var7.setNotDirty();
      }

      for (ChunkRenderDispatcher.RenderChunk var7 : var5) {
         var7.rebuildChunkAsync(this.chunkRenderDispatcher, var2);
         var7.setNotDirty();
      }

      this.minecraft.getProfiler().pop();
   }

   private void renderWorldBorder(Camera param0) {
      BufferBuilder var2 = Tesselator.getInstance().getBuilder();
      WorldBorder var3 = this.level.getWorldBorder();
      double var4 = this.minecraft.options.getEffectiveRenderDistance() * 16;
      if (!(param0.getPosition().x < var3.getMaxX() - var4)
         || !(param0.getPosition().x > var3.getMinX() + var4)
         || !(param0.getPosition().z < var3.getMaxZ() - var4)
         || !(param0.getPosition().z > var3.getMinZ() + var4)) {
         double var6 = 1.0 - var3.getDistanceToBorder(param0.getPosition().x, param0.getPosition().z) / var4;
         var6 = Math.pow(var6, 4.0);
         var6 = Mth.clamp(var6, 0.0, 1.0);
         double var8 = param0.getPosition().x;
         double var10 = param0.getPosition().z;
         double var12 = this.minecraft.gameRenderer.getDepthFar();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
         );
         RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         PoseStack var14 = RenderSystem.getModelViewStack();
         var14.pushPose();
         RenderSystem.applyModelViewMatrix();
         int var15 = var3.getStatus().getColor();
         float var16 = (var15 >> 16 & 0xFF) / 255.0F;
         float var17 = (var15 >> 8 & 0xFF) / 255.0F;
         float var18 = (var15 & 0xFF) / 255.0F;
         RenderSystem.setShaderColor(var16, var17, var18, (float)var6);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.polygonOffset(-3.0F, -3.0F);
         RenderSystem.enablePolygonOffset();
         RenderSystem.disableCull();
         float var19 = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float var20 = (float)(-Mth.frac(param0.getPosition().y * 0.5));
         float var21 = var20 + (float)var12;
         var2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         double var22 = Math.max(Mth.floor(var10 - var4), var3.getMinZ());
         double var24 = Math.min(Mth.ceil(var10 + var4), var3.getMaxZ());
         float var26 = (Mth.floor(var22) & 1) * 0.5F;
         if (var8 > var3.getMaxX() - var4) {
            float var27 = var26;

            for (double var28 = var22; var28 < var24; var27 += 0.5F) {
               double var30 = Math.min(1.0, var24 - var28);
               float var32 = (float)var30 * 0.5F;
               var2.vertex(var3.getMaxX() - var8, -var12, var28 - var10).uv(var19 - var27, var19 + var21).endVertex();
               var2.vertex(var3.getMaxX() - var8, -var12, var28 + var30 - var10).uv(var19 - (var32 + var27), var19 + var21).endVertex();
               var2.vertex(var3.getMaxX() - var8, var12, var28 + var30 - var10).uv(var19 - (var32 + var27), var19 + var20).endVertex();
               var2.vertex(var3.getMaxX() - var8, var12, var28 - var10).uv(var19 - var27, var19 + var20).endVertex();
               var28++;
            }
         }

         if (var8 < var3.getMinX() + var4) {
            float var27 = var26;

            for (double var28 = var22; var28 < var24; var27 += 0.5F) {
               double var30 = Math.min(1.0, var24 - var28);
               float var32 = (float)var30 * 0.5F;
               var2.vertex(var3.getMinX() - var8, -var12, var28 - var10).uv(var19 + var27, var19 + var21).endVertex();
               var2.vertex(var3.getMinX() - var8, -var12, var28 + var30 - var10).uv(var19 + var32 + var27, var19 + var21).endVertex();
               var2.vertex(var3.getMinX() - var8, var12, var28 + var30 - var10).uv(var19 + var32 + var27, var19 + var20).endVertex();
               var2.vertex(var3.getMinX() - var8, var12, var28 - var10).uv(var19 + var27, var19 + var20).endVertex();
               var28++;
            }
         }

         var22 = Math.max(Mth.floor(var8 - var4), var3.getMinX());
         var24 = Math.min(Mth.ceil(var8 + var4), var3.getMaxX());
         var26 = (Mth.floor(var22) & 1) * 0.5F;
         if (var10 > var3.getMaxZ() - var4) {
            float var27 = var26;

            for (double var28 = var22; var28 < var24; var27 += 0.5F) {
               double var30 = Math.min(1.0, var24 - var28);
               float var32 = (float)var30 * 0.5F;
               var2.vertex(var28 - var8, -var12, var3.getMaxZ() - var10).uv(var19 + var27, var19 + var21).endVertex();
               var2.vertex(var28 + var30 - var8, -var12, var3.getMaxZ() - var10).uv(var19 + var32 + var27, var19 + var21).endVertex();
               var2.vertex(var28 + var30 - var8, var12, var3.getMaxZ() - var10).uv(var19 + var32 + var27, var19 + var20).endVertex();
               var2.vertex(var28 - var8, var12, var3.getMaxZ() - var10).uv(var19 + var27, var19 + var20).endVertex();
               var28++;
            }
         }

         if (var10 < var3.getMinZ() + var4) {
            float var27 = var26;

            for (double var28 = var22; var28 < var24; var27 += 0.5F) {
               double var30 = Math.min(1.0, var24 - var28);
               float var32 = (float)var30 * 0.5F;
               var2.vertex(var28 - var8, -var12, var3.getMinZ() - var10).uv(var19 - var27, var19 + var21).endVertex();
               var2.vertex(var28 + var30 - var8, -var12, var3.getMinZ() - var10).uv(var19 - (var32 + var27), var19 + var21).endVertex();
               var2.vertex(var28 + var30 - var8, var12, var3.getMinZ() - var10).uv(var19 - (var32 + var27), var19 + var20).endVertex();
               var2.vertex(var28 - var8, var12, var3.getMinZ() - var10).uv(var19 - var27, var19 + var20).endVertex();
               var28++;
            }
         }

         BufferUploader.drawWithShader(var2.end());
         RenderSystem.enableCull();
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         var14.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.depthMask(true);
      }
   }

   private void renderHitOutline(
      PoseStack param0, VertexConsumer param1, Entity param2, double param3, double param5, double param7, BlockPos param9, BlockState param10
   ) {
      renderShape(
         param0,
         param1,
         param10.getShape(this.level, param9, CollisionContext.of(param2)),
         param9.getX() - param3,
         param9.getY() - param5,
         param9.getZ() - param7,
         0.0F,
         0.0F,
         0.0F,
         0.4F
      );
   }

   public static void renderVoxelShape(
      PoseStack param0,
      VertexConsumer param1,
      VoxelShape param2,
      double param3,
      double param5,
      double param7,
      float param9,
      float param10,
      float param11,
      float param12
   ) {
      List<AABB> var13 = param2.toAabbs();
      int var14 = Mth.ceil(var13.size() / 3.0);

      for (int var15 = 0; var15 < var13.size(); var15++) {
         AABB var16 = var13.get(var15);
         float var17 = (var15 % var14 + 1.0F) / var14;
         float var18 = var15 / var14;
         float var19 = var17 * (var18 == 0.0F ? 1 : 0);
         float var20 = var17 * (var18 == 1.0F ? 1 : 0);
         float var21 = var17 * (var18 == 2.0F ? 1 : 0);
         renderShape(param0, param1, Shapes.create(var16.move(0.0, 0.0, 0.0)), param3, param5, param7, var19, var20, var21, 1.0F);
      }
   }

   private static void renderShape(
      PoseStack param0,
      VertexConsumer param1,
      VoxelShape param2,
      double param3,
      double param5,
      double param7,
      float param9,
      float param10,
      float param11,
      float param12
   ) {
      PoseStack.Pose var13 = param0.last();
      param2.forAllEdges(
         (l_param12, l_param14, l_param16, l_param18, l_param20, l_param22) -> {
            float l_var24 = (float)(l_param18 - l_param12);
            float l_var25 = (float)(l_param20 - l_param14);
            float l_var26 = (float)(l_param22 - l_param16);
            float l_var27 = Mth.sqrt(l_var24 * l_var24 + l_var25 * l_var25 + l_var26 * l_var26);
            l_var24 /= l_var27;
            l_var25 /= l_var27;
            l_var26 /= l_var27;
            param1.vertex(var13.pose(), (float)(l_param12 + param3), (float)(l_param14 + param5), (float)(l_param16 + param7))
               .color(param9, param10, param11, param12)
               .normal(var13.normal(), l_var24, l_var25, l_var26)
               .endVertex();
            param1.vertex(var13.pose(), (float)(l_param18 + param3), (float)(l_param20 + param5), (float)(l_param22 + param7))
               .color(param9, param10, param11, param12)
               .normal(var13.normal(), l_var24, l_var25, l_var26)
               .endVertex();
         }
      );
   }

   public static void renderLineBox(
      VertexConsumer param0,
      double param1,
      double param3,
      double param5,
      double param7,
      double param9,
      double param11,
      float param13,
      float param14,
      float param15,
      float param16
   ) {
      renderLineBox(new PoseStack(), param0, param1, param3, param5, param7, param9, param11, param13, param14, param15, param16, param13, param14, param15);
   }

   public static void renderLineBox(PoseStack param0, VertexConsumer param1, AABB param2, float param3, float param4, float param5, float param6) {
      renderLineBox(
         param0, param1, param2.minX, param2.minY, param2.minZ, param2.maxX, param2.maxY, param2.maxZ, param3, param4, param5, param6, param3, param4, param5
      );
   }

   public static void renderLineBox(
      PoseStack param0,
      VertexConsumer param1,
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
      renderLineBox(param0, param1, param2, param4, param6, param8, param10, param12, param14, param15, param16, param17, param14, param15, param16);
   }

   public static void renderLineBox(
      PoseStack param0,
      VertexConsumer param1,
      double param2,
      double param4,
      double param6,
      double param8,
      double param10,
      double param12,
      float param14,
      float param15,
      float param16,
      float param17,
      float param18,
      float param19,
      float param20
   ) {
      Matrix4f var21 = param0.last().pose();
      Matrix3f var22 = param0.last().normal();
      float var23 = (float)param2;
      float var24 = (float)param4;
      float var25 = (float)param6;
      float var26 = (float)param8;
      float var27 = (float)param10;
      float var28 = (float)param12;
      param1.vertex(var21, var23, var24, var25).color(param14, param19, param20, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var24, var25).color(param14, param19, param20, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var24, var25).color(param18, param15, param20, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var27, var25).color(param18, param15, param20, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var24, var25).color(param18, param19, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      param1.vertex(var21, var23, var24, var28).color(param18, param19, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      param1.vertex(var21, var26, var24, var25).color(param14, param15, param16, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var27, var25).color(param14, param15, param16, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var27, var25).color(param14, param15, param16, param17).normal(var22, -1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var27, var25).color(param14, param15, param16, param17).normal(var22, -1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var27, var25).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      param1.vertex(var21, var23, var27, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      param1.vertex(var21, var23, var27, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, -1.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var24, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, -1.0F, 0.0F).endVertex();
      param1.vertex(var21, var23, var24, var28).color(param14, param15, param16, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var24, var28).color(param14, param15, param16, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var24, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, -1.0F).endVertex();
      param1.vertex(var21, var26, var24, var25).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, -1.0F).endVertex();
      param1.vertex(var21, var23, var27, var28).color(param14, param15, param16, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var27, var28).color(param14, param15, param16, param17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var24, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var27, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      param1.vertex(var21, var26, var27, var25).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      param1.vertex(var21, var26, var27, var28).color(param14, param15, param16, param17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
   }

   public static void addChainedFilledBoxVertices(
      PoseStack param0,
      VertexConsumer param1,
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
      addChainedFilledBoxVertices(
         param0, param1, (float)param2, (float)param4, (float)param6, (float)param8, (float)param10, (float)param12, param14, param15, param16, param17
      );
   }

   public static void addChainedFilledBoxVertices(
      PoseStack param0,
      VertexConsumer param1,
      float param2,
      float param3,
      float param4,
      float param5,
      float param6,
      float param7,
      float param8,
      float param9,
      float param10,
      float param11
   ) {
      Matrix4f var12 = param0.last().pose();
      param1.vertex(var12, param2, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param3, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param2, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param4).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param7).color(param8, param9, param10, param11).endVertex();
      param1.vertex(var12, param5, param6, param7).color(param8, param9, param10, param11).endVertex();
   }

   public void blockChanged(BlockGetter param0, BlockPos param1, BlockState param2, BlockState param3, int param4) {
      this.setBlockDirty(param1, (param4 & 8) != 0);
   }

   private void setBlockDirty(BlockPos param0, boolean param1) {
      for (int var3 = param0.getZ() - 1; var3 <= param0.getZ() + 1; var3++) {
         for (int var4 = param0.getX() - 1; var4 <= param0.getX() + 1; var4++) {
            for (int var5 = param0.getY() - 1; var5 <= param0.getY() + 1; var5++) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(var4), SectionPos.blockToSectionCoord(var5), SectionPos.blockToSectionCoord(var3), param1);
            }
         }
      }
   }

   public void setBlocksDirty(int param0, int param1, int param2, int param3, int param4, int param5) {
      for (int var7 = param2 - 1; var7 <= param5 + 1; var7++) {
         for (int var8 = param0 - 1; var8 <= param3 + 1; var8++) {
            for (int var9 = param1 - 1; var9 <= param4 + 1; var9++) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(var8), SectionPos.blockToSectionCoord(var9), SectionPos.blockToSectionCoord(var7));
            }
         }
      }
   }

   public void setBlockDirty(BlockPos param0, BlockState param1, BlockState param2) {
      if (this.minecraft.getModelManager().requiresRender(param1, param2)) {
         this.setBlocksDirty(param0.getX(), param0.getY(), param0.getZ(), param0.getX(), param0.getY(), param0.getZ());
      }
   }

   public void setSectionDirtyWithNeighbors(int param0, int param1, int param2) {
      for (int var4 = param2 - 1; var4 <= param2 + 1; var4++) {
         for (int var5 = param0 - 1; var5 <= param0 + 1; var5++) {
            for (int var6 = param1 - 1; var6 <= param1 + 1; var6++) {
               this.setSectionDirty(var5, var6, var4);
            }
         }
      }
   }

   public void setSectionDirty(int param0, int param1, int param2) {
      this.setSectionDirty(param0, param1, param2, false);
   }

   private void setSectionDirty(int param0, int param1, int param2, boolean param3) {
      this.viewArea.setDirty(param0, param1, param2, param3);
   }

   public void playStreamingMusic(@Nullable SoundEvent param0, BlockPos param1) {
      SoundInstance var3 = this.playingRecords.get(param1);
      if (var3 != null) {
         this.minecraft.getSoundManager().stop(var3);
         this.playingRecords.remove(param1);
      }

      if (param0 != null) {
         RecordItem var4 = RecordItem.getBySound(param0);
         if (var4 != null) {
            this.minecraft.gui.setNowPlaying(var4.getDisplayName());
         }

         var3 = SimpleSoundInstance.forRecord(param0, Vec3.atCenterOf(param1));
         this.playingRecords.put(param1, var3);
         this.minecraft.getSoundManager().play(var3);
      }

      this.notifyNearbyEntities(this.level, param1, param0 != null);
   }

   private void notifyNearbyEntities(Level param0, BlockPos param1, boolean param2) {
      for (LivingEntity var6 : param0.getEntitiesOfClass(LivingEntity.class, new AABB(param1).inflate(3.0))) {
         var6.setRecordPlayingNearby(param1, param2);
      }
   }

   public void addParticle(ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12) {
      this.addParticle(param0, param1, false, param2, param4, param6, param8, param10, param12);
   }

   public void addParticle(
      ParticleOptions param0, boolean param1, boolean param2, double param3, double param5, double param7, double param9, double param11, double param13
   ) {
      try {
         this.addParticleInternal(param0, param1, param2, param3, param5, param7, param9, param11, param13);
      } catch (Throwable var16) {
         CrashReport var17 = CrashReport.forThrowable(var16, "Exception while adding particle");
         CrashReportCategory var18 = var17.addCategory("Particle being added");
         var18.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(param0.getType()));
         var18.setDetail("Parameters", param0.writeToString());
         var18.setDetail("Position", () -> CrashReportCategory.formatLocation(this.level, param3, param5, param7));
         throw new ReportedException(var17);
      }
   }

   private <T extends ParticleOptions> void addParticle(T param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      this.addParticle(param0, param0.getType().getOverrideLimiter(), param1, param3, param5, param7, param9, param11);
   }

   @Nullable
   private Particle addParticleInternal(
      ParticleOptions param0, boolean param1, double param2, double param4, double param6, double param8, double param10, double param12
   ) {
      return this.addParticleInternal(param0, param1, false, param2, param4, param6, param8, param10, param12);
   }

   @Nullable
   private Particle addParticleInternal(
      ParticleOptions param0, boolean param1, boolean param2, double param3, double param5, double param7, double param9, double param11, double param13
   ) {
      Camera var16 = this.minecraft.gameRenderer.getMainCamera();
      if (this.minecraft != null && var16.isInitialized() && this.minecraft.particleEngine != null) {
         ParticleStatus var17 = this.calculateParticleLevel(param2);
         if (param1) {
            return this.minecraft.particleEngine.createParticle(param0, param3, param5, param7, param9, param11, param13);
         } else if (var16.getPosition().distanceToSqr(param3, param5, param7) > 1024.0) {
            return null;
         } else {
            return var17 == ParticleStatus.MINIMAL
               ? null
               : this.minecraft.particleEngine.createParticle(param0, param3, param5, param7, param9, param11, param13);
         }
      } else {
         return null;
      }
   }

   private ParticleStatus calculateParticleLevel(boolean param0) {
      ParticleStatus var2 = this.minecraft.options.particles().get();
      if (param0 && var2 == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
         var2 = ParticleStatus.DECREASED;
      }

      if (var2 == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
         var2 = ParticleStatus.MINIMAL;
      }

      return var2;
   }

   public void clear() {
   }

   public void globalLevelEvent(int param0, BlockPos param1, int param2) {
      switch (param0) {
         case 1023:
         case 1028:
         case 1038:
            Camera var4 = this.minecraft.gameRenderer.getMainCamera();
            if (var4.isInitialized()) {
               double var5 = param1.getX() - var4.getPosition().x;
               double var7 = param1.getY() - var4.getPosition().y;
               double var9 = param1.getZ() - var4.getPosition().z;
               double var11 = Math.sqrt(var5 * var5 + var7 * var7 + var9 * var9);
               double var13 = var4.getPosition().x;
               double var15 = var4.getPosition().y;
               double var17 = var4.getPosition().z;
               if (var11 > 0.0) {
                  var13 += var5 / var11 * 2.0;
                  var15 += var7 / var11 * 2.0;
                  var17 += var9 / var11 * 2.0;
               }

               if (param0 == 1023) {
                  this.level.playLocalSound(var13, var15, var17, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else if (param0 == 1038) {
                  this.level.playLocalSound(var13, var15, var17, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else {
                  this.level.playLocalSound(var13, var15, var17, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
               }
            }
      }
   }

   public void levelEvent(int param0, BlockPos param1, int param2) {
      RandomSource var4 = this.level.random;
      switch (param0) {
         case 1000:
            this.level.playLocalSound(param1, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1001:
            this.level.playLocalSound(param1, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1002:
            this.level.playLocalSound(param1, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1003:
            this.level.playLocalSound(param1, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            break;
         case 1004:
            this.level.playLocalSound(param1, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            break;
         case 1009:
            if (param2 == 0) {
               this.level
                  .playLocalSound(param1, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (var4.nextFloat() - var4.nextFloat()) * 0.8F, false);
            } else if (param2 == 1) {
               this.level
                  .playLocalSound(
                     param1, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (var4.nextFloat() - var4.nextFloat()) * 0.4F, false
                  );
            }
            break;
         case 1010:
            if (Item.byId(param2) instanceof RecordItem var7) {
               this.playStreamingMusic(var7.getSound(), param1);
            }
            break;
         case 1011:
            this.playStreamingMusic(null, param1);
            break;
         case 1015:
            this.level.playLocalSound(param1, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1016:
            this.level.playLocalSound(param1, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1017:
            this.level
               .playLocalSound(param1, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1018:
            this.level.playLocalSound(param1, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1019:
            this.level
               .playLocalSound(
                  param1, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1020:
            this.level
               .playLocalSound(
                  param1, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1021:
            this.level
               .playLocalSound(
                  param1, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1022:
            this.level
               .playLocalSound(param1, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1024:
            this.level.playLocalSound(param1, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1025:
            this.level.playLocalSound(param1, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1026:
            this.level.playLocalSound(param1, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false);
            break;
         case 1027:
            this.level
               .playLocalSound(
                  param1, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1029:
            this.level.playLocalSound(param1, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, var4.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1030:
            this.level.playLocalSound(param1, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, var4.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1031:
            this.level.playLocalSound(param1, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1032:
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, var4.nextFloat() * 0.4F + 0.8F, 0.25F));
            break;
         case 1033:
            this.level.playLocalSound(param1, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1034:
            this.level.playLocalSound(param1, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1035:
            this.level.playLocalSound(param1, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1039:
            this.level.playLocalSound(param1, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1040:
            this.level
               .playLocalSound(
                  param1, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1041:
            this.level
               .playLocalSound(
                  param1, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1042:
            this.level.playLocalSound(param1, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1043:
            this.level.playLocalSound(param1, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1044:
            this.level.playLocalSound(param1, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1045:
            this.level.playLocalSound(param1, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1046:
            this.level
               .playLocalSound(
                  param1, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false
               );
            break;
         case 1047:
            this.level
               .playLocalSound(
                  param1, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false
               );
            break;
         case 1048:
            this.level
               .playLocalSound(
                  param1, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (var4.nextFloat() - var4.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1500:
            ComposterBlock.handleFill(this.level, param1, param2 > 0);
            break;
         case 1501:
            this.level
               .playLocalSound(param1, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (var4.nextFloat() - var4.nextFloat()) * 0.8F, false);

            for (int var7 = 0; var7 < 8; var7++) {
               this.level
                  .addParticle(
                     ParticleTypes.LARGE_SMOKE, param1.getX() + var4.nextDouble(), param1.getY() + 1.2, param1.getZ() + var4.nextDouble(), 0.0, 0.0, 0.0
                  );
            }
            break;
         case 1502:
            this.level
               .playLocalSound(param1, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (var4.nextFloat() - var4.nextFloat()) * 0.8F, false);

            for (int var7 = 0; var7 < 5; var7++) {
               double var8 = param1.getX() + var4.nextDouble() * 0.6 + 0.2;
               double var10 = param1.getY() + var4.nextDouble() * 0.6 + 0.2;
               double var12 = param1.getZ() + var4.nextDouble() * 0.6 + 0.2;
               this.level.addParticle(ParticleTypes.SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
            break;
         case 1503:
            this.level.playLocalSound(param1, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

            for (int var7 = 0; var7 < 16; var7++) {
               double var8 = param1.getX() + (5.0 + var4.nextDouble() * 6.0) / 16.0;
               double var10 = param1.getY() + 0.8125;
               double var12 = param1.getZ() + (5.0 + var4.nextDouble() * 6.0) / 16.0;
               this.level.addParticle(ParticleTypes.SMOKE, var8, var10, var12, 0.0, 0.0, 0.0);
            }
            break;
         case 1504:
            PointedDripstoneBlock.spawnDripParticle(this.level, param1, this.level.getBlockState(param1));
            break;
         case 1505:
            BoneMealItem.addGrowthParticles(this.level, param1, param2);
            this.level.playLocalSound(param1, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 2000:
            Direction var5 = Direction.from3DDataValue(param2);
            int var6x = var5.getStepX();
            int var7 = var5.getStepY();
            int var8 = var5.getStepZ();
            double var9 = param1.getX() + var6x * 0.6 + 0.5;
            double var11 = param1.getY() + var7 * 0.6 + 0.5;
            double var13 = param1.getZ() + var8 * 0.6 + 0.5;

            for (int var15 = 0; var15 < 10; var15++) {
               double var16 = var4.nextDouble() * 0.2 + 0.01;
               double var18 = var9 + var6x * 0.01 + (var4.nextDouble() - 0.5) * var8 * 0.5;
               double var20 = var11 + var7 * 0.01 + (var4.nextDouble() - 0.5) * var7 * 0.5;
               double var22 = var13 + var8 * 0.01 + (var4.nextDouble() - 0.5) * var6x * 0.5;
               double var24 = var6x * var16 + var4.nextGaussian() * 0.01;
               double var26 = var7 * var16 + var4.nextGaussian() * 0.01;
               double var28 = var8 * var16 + var4.nextGaussian() * 0.01;
               this.addParticle(ParticleTypes.SMOKE, var18, var20, var22, var24, var26, var28);
            }
            break;
         case 2001:
            BlockState var5_3 = Block.stateById(param2);
            if (!var5_3.isAir()) {
               SoundType var6 = var5_3.getSoundType();
               this.level.playLocalSound(param1, var6.getBreakSound(), SoundSource.BLOCKS, (var6.getVolume() + 1.0F) / 2.0F, var6.getPitch() * 0.8F, false);
            }

            this.level.addDestroyBlockEffect(param1, var5_3);
            break;
         case 2002:
         case 2007:
            Vec3 var5_2 = Vec3.atBottomCenterOf(param1);

            for (int var6x_3 = 0; var6x_3 < 8; var6x_3++) {
               this.addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                  var5_2.x,
                  var5_2.y,
                  var5_2.z,
                  var4.nextGaussian() * 0.15,
                  var4.nextDouble() * 0.2,
                  var4.nextGaussian() * 0.15
               );
            }

            float var6x_2 = (param2 >> 16 & 0xFF) / 255.0F;
            float var7_9 = (param2 >> 8 & 0xFF) / 255.0F;
            float var8_5 = (param2 >> 0 & 0xFF) / 255.0F;
            ParticleOptions var9_6 = param0 == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

            for (int var10 = 0; var10 < 100; var10++) {
               double var11_5 = var4.nextDouble() * 4.0;
               double var13_3 = var4.nextDouble() * Math.PI * 2.0;
               double var15 = Math.cos(var13_3) * var11_5;
               double var17 = 0.01 + var4.nextDouble() * 0.5;
               double var19 = Math.sin(var13_3) * var11_5;
               Particle var21 = this.addParticleInternal(
                  var9_6, var9_6.getType().getOverrideLimiter(), var5_2.x + var15 * 0.1, var5_2.y + 0.3, var5_2.z + var19 * 0.1, var15, var17, var19
               );
               if (var21 != null) {
                  float var22 = 0.75F + var4.nextFloat() * 0.25F;
                  var21.setColor(var6x_2 * var22, var7_9 * var22, var8_5 * var22);
                  var21.setPower((float)var11_5);
               }
            }

            this.level.playLocalSound(param1, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, var4.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 2003:
            double var5_1 = param1.getX() + 0.5;
            double var7_8 = param1.getY();
            double var9_5 = param1.getZ() + 0.5;

            for (int var11_4 = 0; var11_4 < 8; var11_4++) {
               this.addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                  var5_1,
                  var7_8,
                  var9_5,
                  var4.nextGaussian() * 0.15,
                  var4.nextDouble() * 0.2,
                  var4.nextGaussian() * 0.15
               );
            }

            for (double var11_3 = 0.0; var11_3 < Math.PI * 2; var11_3 += Math.PI / 20) {
               this.addParticle(
                  ParticleTypes.PORTAL,
                  var5_1 + Math.cos(var11_3) * 5.0,
                  var7_8 - 0.4,
                  var9_5 + Math.sin(var11_3) * 5.0,
                  Math.cos(var11_3) * -5.0,
                  0.0,
                  Math.sin(var11_3) * -5.0
               );
               this.addParticle(
                  ParticleTypes.PORTAL,
                  var5_1 + Math.cos(var11_3) * 5.0,
                  var7_8 - 0.4,
                  var9_5 + Math.sin(var11_3) * 5.0,
                  Math.cos(var11_3) * -7.0,
                  0.0,
                  Math.sin(var11_3) * -7.0
               );
            }
            break;
         case 2004:
            for (int var6x_1 = 0; var6x_1 < 20; var6x_1++) {
               double var7_7 = param1.getX() + 0.5 + (var4.nextDouble() - 0.5) * 2.0;
               double var9_4 = param1.getY() + 0.5 + (var4.nextDouble() - 0.5) * 2.0;
               double var11_2 = param1.getZ() + 0.5 + (var4.nextDouble() - 0.5) * 2.0;
               this.level.addParticle(ParticleTypes.SMOKE, var7_7, var9_4, var11_2, 0.0, 0.0, 0.0);
               this.level.addParticle(ParticleTypes.FLAME, var7_7, var9_4, var11_2, 0.0, 0.0, 0.0);
            }
            break;
         case 2005:
            BoneMealItem.addGrowthParticles(this.level, param1, param2);
            break;
         case 2006:
            for (int var7_6 = 0; var7_6 < 200; var7_6++) {
               float var8_4 = var4.nextFloat() * 4.0F;
               float var9_3 = var4.nextFloat() * (float) (Math.PI * 2);
               double var10 = Mth.cos(var9_3) * var8_4;
               double var12 = 0.01 + var4.nextDouble() * 0.5;
               double var14 = Mth.sin(var9_3) * var8_4;
               Particle var16 = this.addParticleInternal(
                  ParticleTypes.DRAGON_BREATH, false, param1.getX() + var10 * 0.1, param1.getY() + 0.3, param1.getZ() + var14 * 0.1, var10, var12, var14
               );
               if (var16 != null) {
                  var16.setPower(var8_4);
               }
            }

            if (param2 == 1) {
               this.level.playLocalSound(param1, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, var4.nextFloat() * 0.1F + 0.9F, false);
            }
            break;
         case 2008:
            this.level.addParticle(ParticleTypes.EXPLOSION, param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5, 0.0, 0.0, 0.0);
            break;
         case 2009:
            for (int var7_5 = 0; var7_5 < 8; var7_5++) {
               this.level
                  .addParticle(ParticleTypes.CLOUD, param1.getX() + var4.nextDouble(), param1.getY() + 1.2, param1.getZ() + var4.nextDouble(), 0.0, 0.0, 0.0);
            }
            break;
         case 3000:
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, param1.getX() + 0.5, param1.getY() + 0.5, param1.getZ() + 0.5, 0.0, 0.0, 0.0);
            this.level
               .playLocalSound(
                  param1,
                  SoundEvents.END_GATEWAY_SPAWN,
                  SoundSource.BLOCKS,
                  10.0F,
                  (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                  false
               );
            break;
         case 3001:
            this.level.playLocalSound(param1, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
            break;
         case 3002:
            if (param2 >= 0 && param2 < Direction.Axis.VALUES.length) {
               ParticleUtils.spawnParticlesAlongAxis(
                  Direction.Axis.VALUES[param2], this.level, param1, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19)
               );
            } else {
               ParticleUtils.spawnParticlesOnBlockFaces(this.level, param1, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
            }
            break;
         case 3003:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, param1, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
            this.level.playLocalSound(param1, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 3004:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, param1, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
            break;
         case 3005:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, param1, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
            break;
         case 3006:
            int var6 = param2 >> 6;
            if (var6 > 0) {
               if (var4.nextFloat() < 0.3F + var6 * 0.1F) {
                  float var7_4 = 0.15F + 0.02F * var6 * var6 * var4.nextFloat();
                  float var8_3 = 0.4F + 0.3F * var6 * var4.nextFloat();
                  this.level.playLocalSound(param1, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, var7_4, var8_3, false);
               }

               byte var7_3 = (byte)(param2 & 63);
               IntProvider var8_2 = UniformInt.of(0, var6);
               float var9_2 = 0.005F;
               Supplier<Vec3> var10 = () -> new Vec3(
                  Mth.nextDouble(var4, -0.005F, 0.005F), Mth.nextDouble(var4, -0.005F, 0.005F), Mth.nextDouble(var4, -0.005F, 0.005F)
               );
               if (var7_3 == 0) {
                  for (Direction var14 : Direction.values()) {
                     float var15 = var14 == Direction.DOWN ? (float) Math.PI : 0.0F;
                     double var16 = var14.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                     ParticleUtils.spawnParticlesOnBlockFace(this.level, param1, new SculkChargeParticleOptions(var15), var8_2, var14, var10, var16);
                  }
               } else {
                  for (Direction var12 : MultifaceBlock.unpack(var7_3)) {
                     float var13_2 = var12 == Direction.UP ? (float) Math.PI : 0.0F;
                     double var14 = 0.35;
                     ParticleUtils.spawnParticlesOnBlockFace(this.level, param1, new SculkChargeParticleOptions(var13_2), var8_2, var12, var10, 0.35);
                  }
               }
            } else {
               this.level.playLocalSound(param1, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
               boolean var7_2 = this.level.getBlockState(param1).isCollisionShapeFullBlock(this.level, param1);
               int var8_1 = var7_2 ? 40 : 20;
               float var9_1 = var7_2 ? 0.45F : 0.25F;
               float var10 = 0.07F;

               for (int var11_1 = 0; var11_1 < var8_1; var11_1++) {
                  float var12 = 2.0F * var4.nextFloat() - 1.0F;
                  float var13_1 = 2.0F * var4.nextFloat() - 1.0F;
                  float var14 = 2.0F * var4.nextFloat() - 1.0F;
                  this.level
                     .addParticle(
                        ParticleTypes.SCULK_CHARGE_POP,
                        param1.getX() + 0.5 + var12 * var9_1,
                        param1.getY() + 0.5 + var13_1 * var9_1,
                        param1.getZ() + 0.5 + var14 * var9_1,
                        var12 * 0.07F,
                        var13_1 * 0.07F,
                        var14 * 0.07F
                     );
               }
            }
            break;
         case 3007:
            for (int var7_1 = 0; var7_1 < 10; var7_1++) {
               this.level
                  .addParticle(
                     new ShriekParticleOption(var7_1 * 5),
                     false,
                     param1.getX() + 0.5,
                     param1.getY() + SculkShriekerBlock.TOP_Y,
                     param1.getZ() + 0.5,
                     0.0,
                     0.0,
                     0.0
                  );
            }

            this.level
               .playLocalSound(
                  param1.getX() + 0.5,
                  param1.getY() + SculkShriekerBlock.TOP_Y,
                  param1.getZ() + 0.5,
                  SoundEvents.SCULK_SHRIEKER_SHRIEK,
                  SoundSource.BLOCKS,
                  2.0F,
                  0.6F + this.level.random.nextFloat() * 0.4F,
                  false
               );
            break;
         case 3008:
            this.level.playLocalSound(param1, SoundEvents.BRUSH_BRUSH_SAND_COMPLETED, SoundSource.PLAYERS, 1.0F, 1.0F, false);
            this.level.addDestroyBlockEffect(param1, Block.stateById(param2));
      }
   }

   public void destroyBlockProgress(int param0, BlockPos param1, int param2) {
      if (param2 >= 0 && param2 < 10) {
         BlockDestructionProgress var4 = this.destroyingBlocks.get(param0);
         if (var4 != null) {
            this.removeProgress(var4);
         }

         if (var4 == null || var4.getPos().getX() != param1.getX() || var4.getPos().getY() != param1.getY() || var4.getPos().getZ() != param1.getZ()) {
            var4 = new BlockDestructionProgress(param0, param1);
            this.destroyingBlocks.put(param0, var4);
         }

         var4.setProgress(param2);
         var4.updateTick(this.ticks);
         this.destructionProgress.computeIfAbsent(var4.getPos().asLong(), l_param0 -> Sets.newTreeSet()).add(var4);
      } else {
         BlockDestructionProgress var4 = this.destroyingBlocks.remove(param0);
         if (var4 != null) {
            this.removeProgress(var4);
         }
      }
   }

   public boolean hasRenderedAllChunks() {
      return this.chunkRenderDispatcher.isQueueEmpty();
   }

   public void needsUpdate() {
      this.needsFullRenderChunkUpdate = true;
      this.generateClouds = true;
   }

   public void updateGlobalBlockEntities(Collection<BlockEntity> param0, Collection<BlockEntity> param1) {
      synchronized (this.globalBlockEntities) {
         this.globalBlockEntities.removeAll(param0);
         this.globalBlockEntities.addAll(param1);
      }
   }

   public static int getLightColor(BlockAndTintGetter param0, BlockPos param1) {
      return getLightColor(param0, param0.getBlockState(param1), param1);
   }

   public static int getLightColor(BlockAndTintGetter param0, BlockState param1, BlockPos param2) {
      if (param1.emissiveRendering(param0, param2)) {
         return 15728880;
      }

      int var3 = param0.getBrightness(LightLayer.SKY, param2);
      int var4 = param0.getBrightness(LightLayer.BLOCK, param2);
      int var5 = param1.getLightEmission();
      if (var4 < var5) {
         var4 = var5;
      }

      return var3 << 20 | var4 << 4;
   }

   public boolean isChunkCompiled(BlockPos param0) {
      ChunkRenderDispatcher.RenderChunk var2 = this.viewArea.getRenderChunkAt(param0);
      return var2 != null && var2.compiled.get() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
   }

   @Nullable
   public RenderTarget entityTarget() {
      return this.entityTarget;
   }

   @Nullable
   public RenderTarget getTranslucentTarget() {
      return this.translucentTarget;
   }

   @Nullable
   public RenderTarget getItemEntityTarget() {
      return this.itemEntityTarget;
   }

   @Nullable
   public RenderTarget getParticlesTarget() {
      return this.particlesTarget;
   }

   @Nullable
   public RenderTarget getWeatherTarget() {
      return this.weatherTarget;
   }

   @Nullable
   public RenderTarget getCloudsTarget() {
      return this.cloudsTarget;
   }

   static class RenderChunkInfo {
      final ChunkRenderDispatcher.RenderChunk chunk;
      private byte sourceDirections;
      byte directions;
      final int step;

      RenderChunkInfo(ChunkRenderDispatcher.RenderChunk param0, @Nullable Direction param1, int param2) {
         this.chunk = param0;
         if (param1 != null) {
            this.addSourceDirection(param1);
         }

         this.step = param2;
      }

      public void setDirections(byte param0, Direction param1) {
         this.directions = (byte)(this.directions | param0 | 1 << param1.ordinal());
      }

      public boolean hasDirection(Direction param0) {
         return (this.directions & 1 << param0.ordinal()) > 0;
      }

      public void addSourceDirection(Direction param0) {
         this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << param0.ordinal());
      }

      public boolean hasSourceDirection(int param0) {
         return (this.sourceDirections & 1 << param0) > 0;
      }

      public boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }

      public boolean isAxisAlignedWith(int param0, int param1, int param2) {
         BlockPos var4 = this.chunk.getOrigin();
         return param0 == var4.getX() / 16 || param2 == var4.getZ() / 16 || param1 == var4.getY() / 16;
      }

      @Override
      public int hashCode() {
         return this.chunk.getOrigin().hashCode();
      }

      @Override
      public boolean equals(Object param0) {
         return !(param0 instanceof LevelRenderer.RenderChunkInfo var2) ? false : this.chunk.getOrigin().equals(var2.chunk.getOrigin());
      }
   }

   static class RenderChunkStorage {
      public final LevelRenderer.RenderInfoMap renderInfoMap;
      public final LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunks;

      public RenderChunkStorage(int param0) {
         this.renderInfoMap = new LevelRenderer.RenderInfoMap(param0);
         this.renderChunks = new LinkedHashSet<>(param0);
      }
   }

   static class RenderInfoMap {
      private final LevelRenderer.RenderChunkInfo[] infos;

      RenderInfoMap(int param0) {
         this.infos = new LevelRenderer.RenderChunkInfo[param0];
      }

      public void put(ChunkRenderDispatcher.RenderChunk param0, LevelRenderer.RenderChunkInfo param1) {
         this.infos[param0.index] = param1;
      }

      @Nullable
      public LevelRenderer.RenderChunkInfo get(ChunkRenderDispatcher.RenderChunk param0) {
         int var2 = param0.index;
         return var2 >= 0 && var2 < this.infos.length ? this.infos[var2] : null;
      }
   }

   public static class TransparencyShaderException extends RuntimeException {
      public TransparencyShaderException(String param0, Throwable param1) {
         super(param0, param1);
      }
   }
}
