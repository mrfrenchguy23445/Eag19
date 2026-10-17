package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class GameRenderer implements AutoCloseable {
   private static final ResourceLocation NAUSEA_LOCATION = new ResourceLocation("textures/misc/nausea.png");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean DEPTH_BUFFER_DEBUG = false;
   public static final float PROJECTION_Z_NEAR = 0.05F;
   final Minecraft minecraft;
   private final ResourceManager resourceManager;
   private final RandomSource random = RandomSource.create();
   private float renderDistance;
   public final ItemInHandRenderer itemInHandRenderer;
   private final MapRenderer mapRenderer;
   private final RenderBuffers renderBuffers;
   private int tick;
   private float fov;
   private float oldFov;
   private float darkenWorldAmount;
   private float darkenWorldAmountO;
   private boolean renderHand = true;
   private boolean renderBlockOutline = true;
   private long lastScreenshotAttempt;
   private boolean hasWorldScreenshot;
   private long lastActiveTime = Util.getMillis();
   private final LightTexture lightTexture;
   private final OverlayTexture overlayTexture = new OverlayTexture();
   private boolean panoramicMode;
   private float zoom = 1.0F;
   private float zoomX;
   private float zoomY;
   public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
   @Nullable
   private ItemStack itemActivationItem;
   private int itemActivationTicks;
   private float itemActivationOffX;
   private float itemActivationOffY;
   @Nullable
   PostChain postEffect;
   static final ResourceLocation[] EFFECTS = new ResourceLocation[]{
      new ResourceLocation("shaders/post/notch.json"),
      new ResourceLocation("shaders/post/fxaa.json"),
      new ResourceLocation("shaders/post/art.json"),
      new ResourceLocation("shaders/post/bumpy.json"),
      new ResourceLocation("shaders/post/blobs2.json"),
      new ResourceLocation("shaders/post/pencil.json"),
      new ResourceLocation("shaders/post/color_convolve.json"),
      new ResourceLocation("shaders/post/deconverge.json"),
      new ResourceLocation("shaders/post/flip.json"),
      new ResourceLocation("shaders/post/invert.json"),
      new ResourceLocation("shaders/post/ntsc.json"),
      new ResourceLocation("shaders/post/outline.json"),
      new ResourceLocation("shaders/post/phosphor.json"),
      new ResourceLocation("shaders/post/scan_pincushion.json"),
      new ResourceLocation("shaders/post/sobel.json"),
      new ResourceLocation("shaders/post/bits.json"),
      new ResourceLocation("shaders/post/desaturate.json"),
      new ResourceLocation("shaders/post/green.json"),
      new ResourceLocation("shaders/post/blur.json"),
      new ResourceLocation("shaders/post/wobble.json"),
      new ResourceLocation("shaders/post/blobs.json"),
      new ResourceLocation("shaders/post/antialias.json"),
      new ResourceLocation("shaders/post/creeper.json"),
      new ResourceLocation("shaders/post/spider.json")
   };
   public static final int EFFECT_NONE = EFFECTS.length;
   int effectIndex = EFFECT_NONE;
   private boolean effectActive;
   private final Camera mainCamera = new Camera();
   public ShaderInstance blitShader;
   private final Map<String, ShaderInstance> shaders = Maps.newHashMap();
   @Nullable
   private static ShaderInstance positionShader;
   @Nullable
   private static ShaderInstance positionColorShader;
   @Nullable
   private static ShaderInstance positionColorTexShader;
   @Nullable
   private static ShaderInstance positionTexShader;
   @Nullable
   private static ShaderInstance positionTexColorShader;
   @Nullable
   private static ShaderInstance blockShader;
   @Nullable
   private static ShaderInstance newEntityShader;
   @Nullable
   private static ShaderInstance particleShader;
   @Nullable
   private static ShaderInstance positionColorLightmapShader;
   @Nullable
   private static ShaderInstance positionColorTexLightmapShader;
   @Nullable
   private static ShaderInstance positionTexColorNormalShader;
   @Nullable
   private static ShaderInstance positionTexLightmapColorShader;
   @Nullable
   private static ShaderInstance rendertypeSolidShader;
   @Nullable
   private static ShaderInstance rendertypeCutoutMippedShader;
   @Nullable
   private static ShaderInstance rendertypeCutoutShader;
   @Nullable
   private static ShaderInstance rendertypeTranslucentShader;
   @Nullable
   private static ShaderInstance rendertypeTranslucentMovingBlockShader;
   @Nullable
   private static ShaderInstance rendertypeTranslucentNoCrumblingShader;
   @Nullable
   private static ShaderInstance rendertypeArmorCutoutNoCullShader;
   @Nullable
   private static ShaderInstance rendertypeEntitySolidShader;
   @Nullable
   private static ShaderInstance rendertypeEntityCutoutShader;
   @Nullable
   private static ShaderInstance rendertypeEntityCutoutNoCullShader;
   @Nullable
   private static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
   @Nullable
   private static ShaderInstance rendertypeItemEntityTranslucentCullShader;
   @Nullable
   private static ShaderInstance rendertypeEntityTranslucentCullShader;
   @Nullable
   private static ShaderInstance rendertypeEntityTranslucentShader;
   @Nullable
   private static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
   @Nullable
   private static ShaderInstance rendertypeEntitySmoothCutoutShader;
   @Nullable
   private static ShaderInstance rendertypeBeaconBeamShader;
   @Nullable
   private static ShaderInstance rendertypeEntityDecalShader;
   @Nullable
   private static ShaderInstance rendertypeEntityNoOutlineShader;
   @Nullable
   private static ShaderInstance rendertypeEntityShadowShader;
   @Nullable
   private static ShaderInstance rendertypeEntityAlphaShader;
   @Nullable
   private static ShaderInstance rendertypeEyesShader;
   @Nullable
   private static ShaderInstance rendertypeEnergySwirlShader;
   @Nullable
   private static ShaderInstance rendertypeLeashShader;
   @Nullable
   private static ShaderInstance rendertypeWaterMaskShader;
   @Nullable
   private static ShaderInstance rendertypeOutlineShader;
   @Nullable
   private static ShaderInstance rendertypeArmorGlintShader;
   @Nullable
   private static ShaderInstance rendertypeArmorEntityGlintShader;
   @Nullable
   private static ShaderInstance rendertypeGlintTranslucentShader;
   @Nullable
   private static ShaderInstance rendertypeGlintShader;
   @Nullable
   private static ShaderInstance rendertypeGlintDirectShader;
   @Nullable
   private static ShaderInstance rendertypeEntityGlintShader;
   @Nullable
   private static ShaderInstance rendertypeEntityGlintDirectShader;
   @Nullable
   private static ShaderInstance rendertypeTextShader;
   @Nullable
   private static ShaderInstance rendertypeTextBackgroundShader;
   @Nullable
   private static ShaderInstance rendertypeTextIntensityShader;
   @Nullable
   private static ShaderInstance rendertypeTextSeeThroughShader;
   @Nullable
   private static ShaderInstance rendertypeTextBackgroundSeeThroughShader;
   @Nullable
   private static ShaderInstance rendertypeTextIntensitySeeThroughShader;
   @Nullable
   private static ShaderInstance rendertypeLightningShader;
   @Nullable
   private static ShaderInstance rendertypeTripwireShader;
   @Nullable
   private static ShaderInstance rendertypeEndPortalShader;
   @Nullable
   private static ShaderInstance rendertypeEndGatewayShader;
   @Nullable
   private static ShaderInstance rendertypeLinesShader;
   @Nullable
   private static ShaderInstance rendertypeCrumblingShader;

   public GameRenderer(Minecraft param0, ItemInHandRenderer param1, ResourceManager param2, RenderBuffers param3) {
      this.minecraft = param0;
      this.resourceManager = param2;
      this.itemInHandRenderer = param1;
      this.mapRenderer = new MapRenderer(param0.getTextureManager());
      this.lightTexture = new LightTexture(this, param0);
      this.renderBuffers = param3;
      this.postEffect = null;
   }

   @Override
   public void close() {
      this.lightTexture.close();
      this.mapRenderer.close();
      this.overlayTexture.close();
      this.shutdownEffect();
      this.shutdownShaders();
      if (this.blitShader != null) {
         this.blitShader.close();
      }
   }

   public void setRenderHand(boolean param0) {
      this.renderHand = param0;
   }

   public void setRenderBlockOutline(boolean param0) {
      this.renderBlockOutline = param0;
   }

   public void setPanoramicMode(boolean param0) {
      this.panoramicMode = param0;
   }

   public boolean isPanoramicMode() {
      return this.panoramicMode;
   }

   public void shutdownEffect() {
      if (this.postEffect != null) {
         this.postEffect.close();
      }

      this.postEffect = null;
      this.effectIndex = EFFECT_NONE;
   }

   public void togglePostEffect() {
      this.effectActive = !this.effectActive;
   }

   public void checkEntityPostEffect(@Nullable Entity param0) {
      if (this.postEffect != null) {
         this.postEffect.close();
      }

      this.postEffect = null;
      if (param0 instanceof Creeper) {
         this.loadEffect(new ResourceLocation("shaders/post/creeper.json"));
      } else if (param0 instanceof Spider) {
         this.loadEffect(new ResourceLocation("shaders/post/spider.json"));
      } else if (param0 instanceof EnderMan) {
         this.loadEffect(new ResourceLocation("shaders/post/invert.json"));
      }
   }

   public void cycleEffect() {
      if (this.minecraft.getCameraEntity() instanceof Player) {
         if (this.postEffect != null) {
            this.postEffect.close();
         }

         this.effectIndex = (this.effectIndex + 1) % (EFFECTS.length + 1);
         if (this.effectIndex == EFFECT_NONE) {
            this.postEffect = null;
         } else {
            this.loadEffect(EFFECTS[this.effectIndex]);
         }
      }
   }

   void loadEffect(ResourceLocation param0) {
      if (this.postEffect != null) {
         this.postEffect.close();
      }

      try {
         this.postEffect = new PostChain(this.minecraft.getTextureManager(), this.resourceManager, this.minecraft.getMainRenderTarget(), param0);
         this.postEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         this.effectActive = true;
      } catch (IOException var2) {
         LOGGER.warn("Failed to load shader: {}", param0, var2);
         this.effectIndex = EFFECT_NONE;
         this.effectActive = false;
      } catch (JsonSyntaxException var2) {
         LOGGER.warn("Failed to parse shader: {}", param0, var2);
         this.effectIndex = EFFECT_NONE;
         this.effectActive = false;
      }
   }

   public PreparableReloadListener createReloadListener() {
      return new SimplePreparableReloadListener<GameRenderer.ResourceCache>() {
         protected GameRenderer.ResourceCache prepare(ResourceManager l_param0, ProfilerFiller l_param1) {
            Map<ResourceLocation, Resource> l_var3 = l_param0.listResources(
               "shaders",
               l2_param0 -> {
                  String l2_var1 = l2_param0.getPath();
                  return l2_var1.endsWith(".json")
                     || l2_var1.endsWith(Program.Type.FRAGMENT.getExtension())
                     || l2_var1.endsWith(Program.Type.VERTEX.getExtension())
                     || l2_var1.endsWith(".glsl");
               }
            );
            Map<ResourceLocation, Resource> l_var4 = new HashMap<>();
            l_var3.forEach((l2_param1, l2_param2) -> {
               try (InputStream l2_var3 = l2_param2.open()) {
                  byte[] l2_var4 = l2_var3.readAllBytes();
                  l_var4.put(l2_param1, new Resource(l2_param2.source(), () -> new ByteArrayInputStream(l2_var4)));
               } catch (Exception l2_var3) {
                  GameRenderer.LOGGER.warn("Failed to read resource {}", l2_param1, l2_var3);
               }
            });
            return new GameRenderer.ResourceCache(l_param0, l_var4);
         }

         protected void apply(GameRenderer.ResourceCache l_param0, ResourceManager l_param1, ProfilerFiller l_param2) {
            GameRenderer.this.reloadShaders(l_param0);
            if (GameRenderer.this.postEffect != null) {
               GameRenderer.this.postEffect.close();
            }

            GameRenderer.this.postEffect = null;
            if (GameRenderer.this.effectIndex == GameRenderer.EFFECT_NONE) {
               GameRenderer.this.checkEntityPostEffect(GameRenderer.this.minecraft.getCameraEntity());
            } else {
               GameRenderer.this.loadEffect(GameRenderer.EFFECTS[GameRenderer.this.effectIndex]);
            }
         }

         @Override
         public String getName() {
            return "Shader Loader";
         }
      };
   }

   public void preloadUiShader(ResourceProvider param0) {
      if (this.blitShader != null) {
         throw new RuntimeException("Blit shader already preloaded");
      }

      try {
         this.blitShader = new ShaderInstance(param0, "blit_screen", DefaultVertexFormat.BLIT_SCREEN);
      } catch (IOException var2) {
         throw new RuntimeException("could not preload blit shader", var2);
      }

      positionShader = this.preloadShader(param0, "position", DefaultVertexFormat.POSITION);
      positionColorShader = this.preloadShader(param0, "position_color", DefaultVertexFormat.POSITION_COLOR);
      positionColorTexShader = this.preloadShader(param0, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX);
      positionTexShader = this.preloadShader(param0, "position_tex", DefaultVertexFormat.POSITION_TEX);
      positionTexColorShader = this.preloadShader(param0, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
      rendertypeTextShader = this.preloadShader(param0, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
   }

   private ShaderInstance preloadShader(ResourceProvider param0, String param1, VertexFormat param2) {
      try {
         ShaderInstance var4 = new ShaderInstance(param0, param1, param2);
         this.shaders.put(param1, var4);
         return var4;
      } catch (Exception var4) {
         throw new IllegalStateException("could not preload shader " + param1, var4);
      }
   }

   void reloadShaders(ResourceProvider param0) {
      RenderSystem.assertOnRenderThread();
      List<Program> var2 = Lists.newArrayList();
      var2.addAll(Program.Type.FRAGMENT.getPrograms().values());
      var2.addAll(Program.Type.VERTEX.getPrograms().values());
      var2.forEach(Program::close);
      List<Pair<ShaderInstance, Consumer<ShaderInstance>>> var3 = Lists.newArrayListWithCapacity(this.shaders.size());

      try {
         var3.add(Pair.of(new ShaderInstance(param0, "block", DefaultVertexFormat.BLOCK), l_param0 -> blockShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "new_entity", DefaultVertexFormat.NEW_ENTITY), l_param0 -> newEntityShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "particle", DefaultVertexFormat.PARTICLE), l_param0 -> particleShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "position", DefaultVertexFormat.POSITION), l_param0 -> positionShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "position_color", DefaultVertexFormat.POSITION_COLOR), l_param0 -> positionColorShader = l_param0));
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
               l_param0 -> positionColorLightmapShader = l_param0
            )
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX), l_param0 -> positionColorTexShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
               l_param0 -> positionColorTexLightmapShader = l_param0
            )
         );
         var3.add(Pair.of(new ShaderInstance(param0, "position_tex", DefaultVertexFormat.POSITION_TEX), l_param0 -> positionTexShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR), l_param0 -> positionTexColorShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),
               l_param0 -> positionTexColorNormalShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
               l_param0 -> positionTexLightmapColorShader = l_param0
            )
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_solid", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeSolidShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeCutoutMippedShader = l_param0)
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_cutout", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeCutoutShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_translucent", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeTranslucentShader = l_param0));
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK),
               l_param0 -> rendertypeTranslucentMovingBlockShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_translucent_no_crumbling", DefaultVertexFormat.BLOCK),
               l_param0 -> rendertypeTranslucentNoCrumblingShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeArmorCutoutNoCullShader = l_param0
            )
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEntitySolidShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEntityCutoutShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityCutoutNoCullShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityCutoutNoCullZOffsetShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeItemEntityTranslucentCullShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityTranslucentCullShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityTranslucentShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityTranslucentEmissiveShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntitySmoothCutoutShader = l_param0
            )
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeBeaconBeamShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEntityDecalShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY),
               l_param0 -> rendertypeEntityNoOutlineShader = l_param0
            )
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEntityShadowShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEntityAlphaShader = l_param0)
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEyesShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), l_param0 -> rendertypeEnergySwirlShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), l_param0 -> rendertypeLeashShader = l_param0)
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_water_mask", DefaultVertexFormat.POSITION), l_param0 -> rendertypeWaterMaskShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX), l_param0 -> rendertypeOutlineShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX), l_param0 -> rendertypeArmorGlintShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX),
               l_param0 -> rendertypeArmorEntityGlintShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX),
               l_param0 -> rendertypeGlintTranslucentShader = l_param0
            )
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_glint", DefaultVertexFormat.POSITION_TEX), l_param0 -> rendertypeGlintShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX), l_param0 -> rendertypeGlintDirectShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX), l_param0 -> rendertypeEntityGlintShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX),
               l_param0 -> rendertypeEntityGlintDirectShader = l_param0
            )
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), l_param0 -> rendertypeTextShader = l_param0)
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
               l_param0 -> rendertypeTextBackgroundShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
               l_param0 -> rendertypeTextIntensityShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
               l_param0 -> rendertypeTextSeeThroughShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
               l_param0 -> rendertypeTextBackgroundSeeThroughShader = l_param0
            )
         );
         var3.add(
            Pair.of(
               new ShaderInstance(param0, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
               l_param0 -> rendertypeTextIntensitySeeThroughShader = l_param0
            )
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR), l_param0 -> rendertypeLightningShader = l_param0)
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_tripwire", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeTripwireShader = l_param0));
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_end_portal", DefaultVertexFormat.POSITION), l_param0 -> rendertypeEndPortalShader = l_param0));
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_end_gateway", DefaultVertexFormat.POSITION), l_param0 -> rendertypeEndGatewayShader = l_param0)
         );
         var3.add(
            Pair.of(new ShaderInstance(param0, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL), l_param0 -> rendertypeLinesShader = l_param0)
         );
         var3.add(Pair.of(new ShaderInstance(param0, "rendertype_crumbling", DefaultVertexFormat.BLOCK), l_param0 -> rendertypeCrumblingShader = l_param0));
      } catch (IOException var4) {
         var3.forEach(l_param0 -> l_param0.getFirst().close());
         throw new RuntimeException("could not reload shaders", var4);
      }

      this.shutdownShaders();
      var3.forEach(l_param0 -> {
         ShaderInstance l_var2 = l_param0.getFirst();
         this.shaders.put(l_var2.getName(), l_var2);
         l_param0.getSecond().accept(l_var2);
      });
   }

   private void shutdownShaders() {
      RenderSystem.assertOnRenderThread();
      this.shaders.values().forEach(ShaderInstance::close);
      this.shaders.clear();
   }

   @Nullable
   public ShaderInstance getShader(@Nullable String param0) {
      return param0 == null ? null : this.shaders.get(param0);
   }

   public void tick() {
      this.tickFov();
      this.lightTexture.tick();
      if (this.minecraft.getCameraEntity() == null) {
         this.minecraft.setCameraEntity(this.minecraft.player);
      }

      this.mainCamera.tick();
      this.tick++;
      this.itemInHandRenderer.tick();
      this.minecraft.levelRenderer.tickRain(this.mainCamera);
      this.darkenWorldAmountO = this.darkenWorldAmount;
      if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen()) {
         this.darkenWorldAmount += 0.05F;
         if (this.darkenWorldAmount > 1.0F) {
            this.darkenWorldAmount = 1.0F;
         }
      } else if (this.darkenWorldAmount > 0.0F) {
         this.darkenWorldAmount -= 0.0125F;
      }

      if (this.itemActivationTicks > 0) {
         this.itemActivationTicks--;
         if (this.itemActivationTicks == 0) {
            this.itemActivationItem = null;
         }
      }
   }

   @Nullable
   public PostChain currentEffect() {
      return this.postEffect;
   }

   public void resize(int param0, int param1) {
      if (this.postEffect != null) {
         this.postEffect.resize(param0, param1);
      }

      this.minecraft.levelRenderer.resize(param0, param1);
   }

   public void pick(float param0) {
      Entity var2 = this.minecraft.getCameraEntity();
      if (var2 != null) {
         if (this.minecraft.level != null) {
            this.minecraft.getProfiler().push("pick");
            this.minecraft.crosshairPickEntity = null;
            double var3 = this.minecraft.gameMode.getPickRange();
            this.minecraft.hitResult = var2.pick(var3, param0, false);
            Vec3 var5 = var2.getEyePosition(param0);
            boolean var6 = false;
            int var7 = 3;
            double var8 = var3;
            if (this.minecraft.gameMode.hasFarPickRange()) {
               var8 = 6.0;
               var3 = var8;
            } else {
               if (var8 > 3.0) {
                  var6 = true;
               }

               var3 = var8;
            }

            var8 *= var8;
            if (this.minecraft.hitResult != null) {
               var8 = this.minecraft.hitResult.getLocation().distanceToSqr(var5);
            }

            Vec3 var10 = var2.getViewVector(1.0F);
            Vec3 var11 = var5.add(var10.x * var3, var10.y * var3, var10.z * var3);
            float var12 = 1.0F;
            AABB var13 = var2.getBoundingBox().expandTowards(var10.scale(var3)).inflate(1.0, 1.0, 1.0);
            EntityHitResult var14 = ProjectileUtil.getEntityHitResult(
               var2, var5, var11, var13, l_param0 -> !l_param0.isSpectator() && l_param0.isPickable(), var8
            );
            if (var14 != null) {
               Entity var15 = var14.getEntity();
               Vec3 var16 = var14.getLocation();
               double var17 = var5.distanceToSqr(var16);
               if (var6 && var17 > 9.0) {
                  this.minecraft.hitResult = BlockHitResult.miss(var16, Direction.getNearest(var10.x, var10.y, var10.z), BlockPos.containing(var16));
               } else if (var17 < var8 || this.minecraft.hitResult == null) {
                  this.minecraft.hitResult = var14;
                  if (var15 instanceof LivingEntity || var15 instanceof ItemFrame) {
                     this.minecraft.crosshairPickEntity = var15;
                  }
               }
            }

            this.minecraft.getProfiler().pop();
         }
      }
   }

   private void tickFov() {
      float var1 = 1.0F;
      if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer) {
         AbstractClientPlayer var2 = (AbstractClientPlayer)this.minecraft.getCameraEntity();
         var1 = var2.getFieldOfViewModifier();
      }

      this.oldFov = this.fov;
      this.fov = this.fov + (var1 - this.fov) * 0.5F;
      if (this.fov > 1.5F) {
         this.fov = 1.5F;
      }

      if (this.fov < 0.1F) {
         this.fov = 0.1F;
      }
   }

   private double getFov(Camera param0, float param1, boolean param2) {
      if (this.panoramicMode) {
         return 90.0;
      }

      double var4 = 70.0;
      if (param2) {
         var4 = this.minecraft.options.fov().get().intValue();
         var4 *= Mth.lerp(param1, this.oldFov, this.fov);
      }

      if (param0.getEntity() instanceof LivingEntity && ((LivingEntity)param0.getEntity()).isDeadOrDying()) {
         float var6 = Math.min(((LivingEntity)param0.getEntity()).deathTime + param1, 20.0F);
         var4 /= (1.0F - 500.0F / (var6 + 500.0F)) * 2.0F + 1.0F;
      }

      FogType var6 = param0.getFluidInCamera();
      if (var6 == FogType.LAVA || var6 == FogType.WATER) {
         var4 *= Mth.lerp(this.minecraft.options.fovEffectScale().get(), 1.0, 0.85714287F);
      }

      return var4;
   }

   private void bobHurt(PoseStack param0, float param1) {
      if (this.minecraft.getCameraEntity() instanceof LivingEntity) {
         LivingEntity var3 = (LivingEntity)this.minecraft.getCameraEntity();
         float var4 = var3.hurtTime - param1;
         if (var3.isDeadOrDying()) {
            float var5 = Math.min(var3.deathTime + param1, 20.0F);
            param0.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (var5 + 200.0F)));
         }

         if (var4 < 0.0F) {
            return;
         }

         var4 /= var3.hurtDuration;
         var4 = Mth.sin(var4 * var4 * var4 * var4 * (float) Math.PI);
         float var5 = var3.getHurtDir();
         param0.mulPose(Axis.YP.rotationDegrees(-var5));
         float var6 = (float)(-var4 * 14.0 * this.minecraft.options.damageTiltStrength().get());
         param0.mulPose(Axis.ZP.rotationDegrees(var6));
         param0.mulPose(Axis.YP.rotationDegrees(var5));
      }
   }

   private void bobView(PoseStack param0, float param1) {
      if (this.minecraft.getCameraEntity() instanceof Player) {
         Player var3 = (Player)this.minecraft.getCameraEntity();
         float var4 = var3.walkDist - var3.walkDistO;
         float var5 = -(var3.walkDist + var4 * param1);
         float var6 = Mth.lerp(param1, var3.oBob, var3.bob);
         param0.translate(Mth.sin(var5 * (float) Math.PI) * var6 * 0.5F, -Math.abs(Mth.cos(var5 * (float) Math.PI) * var6), 0.0F);
         param0.mulPose(Axis.ZP.rotationDegrees(Mth.sin(var5 * (float) Math.PI) * var6 * 3.0F));
         param0.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(var5 * (float) Math.PI - 0.2F) * var6) * 5.0F));
      }
   }

   public void renderZoomed(float param0, float param1, float param2) {
      this.zoom = param0;
      this.zoomX = param1;
      this.zoomY = param2;
      this.setRenderBlockOutline(false);
      this.setRenderHand(false);
      this.renderLevel(1.0F, 0L, new PoseStack());
      this.zoom = 1.0F;
   }

   private void renderItemInHand(PoseStack param0, Camera param1, float param2) {
      if (!this.panoramicMode) {
         this.resetProjectionMatrix(this.getProjectionMatrix(this.getFov(param1, param2, false)));
         param0.setIdentity();
         param0.pushPose();
         this.bobHurt(param0, param2);
         if (this.minecraft.options.bobView().get()) {
            this.bobView(param0, param2);
         }

         boolean var4 = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
         if (this.minecraft.options.getCameraType().isFirstPerson()
            && !var4
            && !this.minecraft.options.hideGui
            && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.lightTexture.turnOnLightLayer();
            this.itemInHandRenderer
               .renderHandsWithItems(
                  param2,
                  param0,
                  this.renderBuffers.bufferSource(),
                  this.minecraft.player,
                  this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, param2)
               );
            this.lightTexture.turnOffLightLayer();
         }

         param0.popPose();
         if (this.minecraft.options.getCameraType().isFirstPerson() && !var4) {
            ScreenEffectRenderer.renderScreenEffect(this.minecraft, param0);
            this.bobHurt(param0, param2);
         }

         if (this.minecraft.options.bobView().get()) {
            this.bobView(param0, param2);
         }
      }
   }

   public void resetProjectionMatrix(Matrix4f param0) {
      RenderSystem.setProjectionMatrix(param0);
   }

   public Matrix4f getProjectionMatrix(double param0) {
      PoseStack var3 = new PoseStack();
      var3.last().pose().identity();
      if (this.zoom != 1.0F) {
         var3.translate(this.zoomX, -this.zoomY, 0.0F);
         var3.scale(this.zoom, this.zoom, 1.0F);
      }

      var3.last()
         .pose()
         .mul(
            new Matrix4f()
               .setPerspective(
                  (float)(param0 * (float) (Math.PI / 180.0)),
                  (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
                  0.05F,
                  this.getDepthFar()
               )
         );
      return var3.last().pose();
   }

   public float getDepthFar() {
      return this.renderDistance * 4.0F;
   }

   public static float getNightVisionScale(LivingEntity param0, float param1) {
      MobEffectInstance var2 = param0.getEffect(MobEffects.NIGHT_VISION);
      return !var2.endsWithin(200) ? 1.0F : 0.7F + Mth.sin((var2.getDuration() - param1) * (float) Math.PI * 0.2F) * 0.3F;
   }

   public void render(float param0, long param1, boolean param3) {
      if (!this.minecraft.isWindowActive()
         && this.minecraft.options.pauseOnLostFocus
         && (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed())) {
         if (Util.getMillis() - this.lastActiveTime > 500L) {
            this.minecraft.pauseGame(false);
         }
      } else {
         this.lastActiveTime = Util.getMillis();
      }

      if (!this.minecraft.noRender) {
         int var5 = (int)(this.minecraft.mouseHandler.xpos() * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth());
         int var6 = (int)(this.minecraft.mouseHandler.ypos() * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight());
         RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         if (param3 && this.minecraft.level != null) {
            this.minecraft.getProfiler().push("level");
            this.renderLevel(param0, param1, new PoseStack());
            this.tryTakeScreenshotIfNeeded();
            this.minecraft.levelRenderer.doEntityOutline();
            if (this.postEffect != null && this.effectActive) {
               RenderSystem.disableBlend();
               RenderSystem.disableDepthTest();
               RenderSystem.resetTextureMatrix();
               this.postEffect.process(param0);
            }

            this.minecraft.getMainRenderTarget().bindWrite(true);
         }

         Window var7 = this.minecraft.getWindow();
         RenderSystem.clear(256, Minecraft.ON_OSX);
         Matrix4f var8 = new Matrix4f()
            .setOrtho(0.0F, (float)(var7.getWidth() / var7.getGuiScale()), (float)(var7.getHeight() / var7.getGuiScale()), 0.0F, 1000.0F, 3000.0F);
         RenderSystem.setProjectionMatrix(var8);
         PoseStack var9 = RenderSystem.getModelViewStack();
         var9.pushPose();
         var9.setIdentity();
         var9.translate(0.0F, 0.0F, -2000.0F);
         RenderSystem.applyModelViewMatrix();
         Lighting.setupFor3DItems();
         PoseStack var10 = new PoseStack();
         if (param3 && this.minecraft.level != null) {
            this.minecraft.getProfiler().popPush("gui");
            if (this.minecraft.player != null) {
               float var11 = Mth.lerp(param0, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
               float var12 = this.minecraft.options.screenEffectScale().get().floatValue();
               if (var11 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONFUSION) && var12 < 1.0F) {
                  this.renderConfusionOverlay(var11 * (1.0F - var12));
               }
            }

            if (!this.minecraft.options.hideGui || this.minecraft.screen != null) {
               this.renderItemActivationAnimation(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight(), param0);
               this.minecraft.gui.render(var10, param0);
               RenderSystem.clear(256, Minecraft.ON_OSX);
            }

            this.minecraft.getProfiler().pop();
         }

         if (this.minecraft.getOverlay() != null) {
            try {
               this.minecraft.getOverlay().render(var10, var5, var6, this.minecraft.getDeltaFrameTime());
            } catch (Throwable var11) {
               CrashReport var12 = CrashReport.forThrowable(var11, "Rendering overlay");
               CrashReportCategory var13 = var12.addCategory("Overlay render details");
               var13.setDetail("Overlay name", () -> this.minecraft.getOverlay().getClass().getCanonicalName());
               throw new ReportedException(var12);
            }
         } else if (this.minecraft.screen != null) {
            try {
               this.minecraft.screen.renderWithTooltip(var10, var5, var6, this.minecraft.getDeltaFrameTime());
            } catch (Throwable var11) {
               CrashReport var12 = CrashReport.forThrowable(var11, "Rendering screen");
               CrashReportCategory var13 = var12.addCategory("Screen render details");
               var13.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               var13.setDetail(
                  "Mouse location",
                  () -> String.format(
                     Locale.ROOT, "Scaled: (%d, %d). Absolute: (%f, %f)", var5, var6, this.minecraft.mouseHandler.xpos(), this.minecraft.mouseHandler.ypos()
                  )
               );
               var13.setDetail(
                  "Screen size",
                  () -> String.format(
                     Locale.ROOT,
                     "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %f",
                     this.minecraft.getWindow().getGuiScaledWidth(),
                     this.minecraft.getWindow().getGuiScaledHeight(),
                     this.minecraft.getWindow().getWidth(),
                     this.minecraft.getWindow().getHeight(),
                     this.minecraft.getWindow().getGuiScale()
                  )
               );
               throw new ReportedException(var12);
            }

            try {
               if (this.minecraft.screen != null) {
                  this.minecraft.screen.handleDelayedNarration();
               }
            } catch (Throwable var11) {
               CrashReport var12 = CrashReport.forThrowable(var11, "Narrating screen");
               CrashReportCategory var13 = var12.addCategory("Screen details");
               var13.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
               throw new ReportedException(var12);
            }
         }

         this.minecraft.getProfiler().push("toasts");
         this.minecraft.getToasts().render(var10);
         this.minecraft.getProfiler().pop();
         var9.popPose();
         RenderSystem.applyModelViewMatrix();
      }
   }

   private void tryTakeScreenshotIfNeeded() {
      if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
         long var1 = Util.getMillis();
         if (var1 - this.lastScreenshotAttempt >= 1000L) {
            this.lastScreenshotAttempt = var1;
            IntegratedServer var3 = this.minecraft.getSingleplayerServer();
            if (var3 != null && !var3.isStopped()) {
               var3.getWorldScreenshotFile().ifPresent(l_param0 -> {
                  if (Files.isRegularFile(l_param0)) {
                     this.hasWorldScreenshot = true;
                  } else {
                     this.takeAutoScreenshot(l_param0);
                  }
               });
            }
         }
      }
   }

   private void takeAutoScreenshot(Path param0) {
      if (this.minecraft.levelRenderer.countRenderedChunks() > 10 && this.minecraft.levelRenderer.hasRenderedAllChunks()) {
         NativeImage var2 = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
         Util.ioPool().execute(() -> {
            int l_var2 = var2.getWidth();
            int l_var3 = var2.getHeight();
            int l_var4 = 0;
            int l_var5 = 0;
            if (l_var2 > l_var3) {
               l_var4 = (l_var2 - l_var3) / 2;
               l_var2 = l_var3;
            } else {
               l_var5 = (l_var3 - l_var2) / 2;
               l_var3 = l_var2;
            }

            try (NativeImage l_var6 = new NativeImage(64, 64, false)) {
               var2.resizeSubRectTo(l_var4, l_var5, l_var2, l_var3, l_var6);
               l_var6.writeToFile(param0);
            } catch (IOException l_var6) {
               LOGGER.warn("Couldn't save auto screenshot", l_var6);
            } finally {
               var2.close();
            }
         });
      }
   }

   private boolean shouldRenderBlockOutline() {
      if (!this.renderBlockOutline) {
         return false;
      }

      Entity var1 = this.minecraft.getCameraEntity();
      boolean var2 = var1 instanceof Player && !this.minecraft.options.hideGui;
      if (var2 && !((Player)var1).getAbilities().mayBuild) {
         ItemStack var3 = ((LivingEntity)var1).getMainHandItem();
         HitResult var4 = this.minecraft.hitResult;
         if (var4 != null && var4.getType() == HitResult.Type.BLOCK) {
            BlockPos var5 = ((BlockHitResult)var4).getBlockPos();
            BlockState var6 = this.minecraft.level.getBlockState(var5);
            if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
               var2 = var6.getMenuProvider(this.minecraft.level, var5) != null;
            } else {
               BlockInWorld var7 = new BlockInWorld(this.minecraft.level, var5, false);
               Registry<Block> var8 = this.minecraft.level.registryAccess().registryOrThrow(Registries.BLOCK);
               var2 = !var3.isEmpty() && (var3.hasAdventureModeBreakTagForBlock(var8, var7) || var3.hasAdventureModePlaceTagForBlock(var8, var7));
            }
         }
      }

      return var2;
   }

   public void renderLevel(float param0, long param1, PoseStack param3) {
      this.lightTexture.updateLightTexture(param0);
      if (this.minecraft.getCameraEntity() == null) {
         this.minecraft.setCameraEntity(this.minecraft.player);
      }

      this.pick(param0);
      this.minecraft.getProfiler().push("center");
      boolean var5 = this.shouldRenderBlockOutline();
      this.minecraft.getProfiler().popPush("camera");
      Camera var6 = this.mainCamera;
      this.renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;
      PoseStack var7 = new PoseStack();
      double var8 = this.getFov(var6, param0, true);
      var7.mulPoseMatrix(this.getProjectionMatrix(var8));
      this.bobHurt(var7, param0);
      if (this.minecraft.options.bobView().get()) {
         this.bobView(var7, param0);
      }

      float var10 = this.minecraft.options.screenEffectScale().get().floatValue();
      float var11 = Mth.lerp(param0, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime) * (var10 * var10);
      if (var11 > 0.0F) {
         int var12 = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
         float var13 = 5.0F / (var11 * var11 + 5.0F) - var11 * 0.04F;
         var13 *= var13;
         Axis var14 = Axis.of(new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F));
         var7.mulPose(var14.rotationDegrees((this.tick + param0) * var12));
         var7.scale(1.0F / var13, 1.0F, 1.0F);
         float var15 = -(this.tick + param0) * var12;
         var7.mulPose(var14.rotationDegrees(var15));
      }

      Matrix4f var12 = var7.last().pose();
      this.resetProjectionMatrix(var12);
      var6.setup(
         this.minecraft.level,
         this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity(),
         !this.minecraft.options.getCameraType().isFirstPerson(),
         this.minecraft.options.getCameraType().isMirrored(),
         param0
      );
      param3.mulPose(Axis.XP.rotationDegrees(var6.getXRot()));
      param3.mulPose(Axis.YP.rotationDegrees(var6.getYRot() + 180.0F));
      Matrix3f var13 = new Matrix3f(param3.last().normal()).invert();
      RenderSystem.setInverseViewRotationMatrix(var13);
      this.minecraft
         .levelRenderer
         .prepareCullFrustum(param3, var6.getPosition(), this.getProjectionMatrix(Math.max(var8, this.minecraft.options.fov().get().intValue())));
      this.minecraft.levelRenderer.renderLevel(param3, param0, param1, var5, var6, this, this.lightTexture, var12);
      this.minecraft.getProfiler().popPush("hand");
      if (this.renderHand) {
         RenderSystem.clear(256, Minecraft.ON_OSX);
         this.renderItemInHand(param3, var6, param0);
      }

      this.minecraft.getProfiler().pop();
   }

   public void resetData() {
      this.itemActivationItem = null;
      this.mapRenderer.resetData();
      this.mainCamera.reset();
      this.hasWorldScreenshot = false;
   }

   public MapRenderer getMapRenderer() {
      return this.mapRenderer;
   }

   public void displayItemActivation(ItemStack param0) {
      this.itemActivationItem = param0;
      this.itemActivationTicks = 40;
      this.itemActivationOffX = this.random.nextFloat() * 2.0F - 1.0F;
      this.itemActivationOffY = this.random.nextFloat() * 2.0F - 1.0F;
   }

   private void renderItemActivationAnimation(int param0, int param1, float param2) {
      if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
         int var4 = 40 - this.itemActivationTicks;
         float var5 = (var4 + param2) / 40.0F;
         float var6 = var5 * var5;
         float var7 = var5 * var6;
         float var8 = 10.25F * var7 * var6 - 24.95F * var6 * var6 + 25.5F * var7 - 13.8F * var6 + 4.0F * var5;
         float var9 = var8 * (float) Math.PI;
         float var10 = this.itemActivationOffX * (param0 / 4);
         float var11 = this.itemActivationOffY * (param1 / 4);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         PoseStack var12 = new PoseStack();
         var12.pushPose();
         var12.translate(param0 / 2 + var10 * Mth.abs(Mth.sin(var9 * 2.0F)), param1 / 2 + var11 * Mth.abs(Mth.sin(var9 * 2.0F)), -50.0F);
         float var13 = 50.0F + 175.0F * Mth.sin(var9);
         var12.scale(var13, -var13, var13);
         var12.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(var9))));
         var12.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(var5 * 8.0F)));
         var12.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(var5 * 8.0F)));
         MultiBufferSource.BufferSource var14 = this.renderBuffers.bufferSource();
         this.minecraft
            .getItemRenderer()
            .renderStatic(this.itemActivationItem, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, var12, var14, this.minecraft.level, 0);
         var12.popPose();
         var14.endBatch();
         RenderSystem.enableCull();
         RenderSystem.disableDepthTest();
      }
   }

   private void renderConfusionOverlay(float param0) {
      int var2 = this.minecraft.getWindow().getGuiScaledWidth();
      int var3 = this.minecraft.getWindow().getGuiScaledHeight();
      double var4 = Mth.lerp(param0, 2.0, 1.0);
      float var6 = 0.2F * param0;
      float var7 = 0.4F * param0;
      float var8 = 0.2F * param0;
      double var9 = var2 * var4;
      double var11 = var3 * var4;
      double var13 = (var2 - var9) / 2.0;
      double var15 = (var3 - var11) / 2.0;
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(
         GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
      );
      RenderSystem.setShaderColor(var6, var7, var8, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, NAUSEA_LOCATION);
      Tesselator var17 = Tesselator.getInstance();
      BufferBuilder var18 = var17.getBuilder();
      var18.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var18.vertex(var13, var15 + var11, -90.0).uv(0.0F, 1.0F).endVertex();
      var18.vertex(var13 + var9, var15 + var11, -90.0).uv(1.0F, 1.0F).endVertex();
      var18.vertex(var13 + var9, var15, -90.0).uv(1.0F, 0.0F).endVertex();
      var18.vertex(var13, var15, -90.0).uv(0.0F, 0.0F).endVertex();
      var17.end();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public float getDarkenWorldAmount(float param0) {
      return Mth.lerp(param0, this.darkenWorldAmountO, this.darkenWorldAmount);
   }

   public float getRenderDistance() {
      return this.renderDistance;
   }

   public Camera getMainCamera() {
      return this.mainCamera;
   }

   public LightTexture lightTexture() {
      return this.lightTexture;
   }

   public OverlayTexture overlayTexture() {
      return this.overlayTexture;
   }

   @Nullable
   public static ShaderInstance getPositionShader() {
      return positionShader;
   }

   @Nullable
   public static ShaderInstance getPositionColorShader() {
      return positionColorShader;
   }

   @Nullable
   public static ShaderInstance getPositionColorTexShader() {
      return positionColorTexShader;
   }

   @Nullable
   public static ShaderInstance getPositionTexShader() {
      return positionTexShader;
   }

   @Nullable
   public static ShaderInstance getPositionTexColorShader() {
      return positionTexColorShader;
   }

   @Nullable
   public static ShaderInstance getBlockShader() {
      return blockShader;
   }

   @Nullable
   public static ShaderInstance getNewEntityShader() {
      return newEntityShader;
   }

   @Nullable
   public static ShaderInstance getParticleShader() {
      return particleShader;
   }

   @Nullable
   public static ShaderInstance getPositionColorLightmapShader() {
      return positionColorLightmapShader;
   }

   @Nullable
   public static ShaderInstance getPositionColorTexLightmapShader() {
      return positionColorTexLightmapShader;
   }

   @Nullable
   public static ShaderInstance getPositionTexColorNormalShader() {
      return positionTexColorNormalShader;
   }

   @Nullable
   public static ShaderInstance getPositionTexLightmapColorShader() {
      return positionTexLightmapColorShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeSolidShader() {
      return rendertypeSolidShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeCutoutMippedShader() {
      return rendertypeCutoutMippedShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeCutoutShader() {
      return rendertypeCutoutShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTranslucentShader() {
      return rendertypeTranslucentShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTranslucentMovingBlockShader() {
      return rendertypeTranslucentMovingBlockShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTranslucentNoCrumblingShader() {
      return rendertypeTranslucentNoCrumblingShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeArmorCutoutNoCullShader() {
      return rendertypeArmorCutoutNoCullShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntitySolidShader() {
      return rendertypeEntitySolidShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityCutoutShader() {
      return rendertypeEntityCutoutShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityCutoutNoCullShader() {
      return rendertypeEntityCutoutNoCullShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityCutoutNoCullZOffsetShader() {
      return rendertypeEntityCutoutNoCullZOffsetShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeItemEntityTranslucentCullShader() {
      return rendertypeItemEntityTranslucentCullShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityTranslucentCullShader() {
      return rendertypeEntityTranslucentCullShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityTranslucentShader() {
      return rendertypeEntityTranslucentShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityTranslucentEmissiveShader() {
      return rendertypeEntityTranslucentEmissiveShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntitySmoothCutoutShader() {
      return rendertypeEntitySmoothCutoutShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeBeaconBeamShader() {
      return rendertypeBeaconBeamShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityDecalShader() {
      return rendertypeEntityDecalShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityNoOutlineShader() {
      return rendertypeEntityNoOutlineShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityShadowShader() {
      return rendertypeEntityShadowShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityAlphaShader() {
      return rendertypeEntityAlphaShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEyesShader() {
      return rendertypeEyesShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEnergySwirlShader() {
      return rendertypeEnergySwirlShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeLeashShader() {
      return rendertypeLeashShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeWaterMaskShader() {
      return rendertypeWaterMaskShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeOutlineShader() {
      return rendertypeOutlineShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeArmorGlintShader() {
      return rendertypeArmorGlintShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeArmorEntityGlintShader() {
      return rendertypeArmorEntityGlintShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeGlintTranslucentShader() {
      return rendertypeGlintTranslucentShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeGlintShader() {
      return rendertypeGlintShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeGlintDirectShader() {
      return rendertypeGlintDirectShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityGlintShader() {
      return rendertypeEntityGlintShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEntityGlintDirectShader() {
      return rendertypeEntityGlintDirectShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextShader() {
      return rendertypeTextShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextBackgroundShader() {
      return rendertypeTextBackgroundShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextIntensityShader() {
      return rendertypeTextIntensityShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextSeeThroughShader() {
      return rendertypeTextSeeThroughShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextBackgroundSeeThroughShader() {
      return rendertypeTextBackgroundSeeThroughShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTextIntensitySeeThroughShader() {
      return rendertypeTextIntensitySeeThroughShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeLightningShader() {
      return rendertypeLightningShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeTripwireShader() {
      return rendertypeTripwireShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEndPortalShader() {
      return rendertypeEndPortalShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeEndGatewayShader() {
      return rendertypeEndGatewayShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeLinesShader() {
      return rendertypeLinesShader;
   }

   @Nullable
   public static ShaderInstance getRendertypeCrumblingShader() {
      return rendertypeCrumblingShader;
   }

   public record ResourceCache(ResourceProvider original, Map<ResourceLocation, Resource> cache) implements ResourceProvider {
      @Override
      public Optional<Resource> getResource(ResourceLocation param0) {
         Resource var2 = this.cache.get(param0);
         return var2 != null ? Optional.of(var2) : this.original.getResource(param0);
      }
   }
}
