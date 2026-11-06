package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class ParticleEngine implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private static final ResourceLocation PARTICLES_ATLAS_INFO = new ResourceLocation("particles");
   private static final int MAX_PARTICLES_PER_LAYER = 16384;
   private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(
      ParticleRenderType.TERRAIN_SHEET,
      ParticleRenderType.PARTICLE_SHEET_OPAQUE,
      ParticleRenderType.PARTICLE_SHEET_LIT,
      ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
      ParticleRenderType.CUSTOM
   );
   protected ClientLevel level;
   private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newIdentityHashMap();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final TextureManager textureManager;
   private final RandomSource random = RandomSource.create();
   private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap<>();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final TextureAtlas textureAtlas;
   private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap<>();
   // Cooperative-port: reused survivor buffer for tickParticleList; avoids per-tick
   // allocations while rebuilding a particle list after any removal or failure.
   private final List<Particle> particleRebuildBuffer = Lists.newArrayList();
   // Cooperative-port: reused survivor buffer for the trackingEmitters rebuild in tick().
   private final List<TrackingEmitter> trackingRebuildBuffer = Lists.newArrayList();

   public ParticleEngine(ClientLevel param0, TextureManager param1) {
      this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
      param1.register(this.textureAtlas.location(), this.textureAtlas);
      this.level = param0;
      this.textureManager = param1;
      this.registerProviders();
   }

   private void registerProviders() {
      this.register(ParticleTypes.AMBIENT_ENTITY_EFFECT, SpellParticle.AmbientMobProvider::new);
      this.register(ParticleTypes.ANGRY_VILLAGER, HeartParticle.AngryVillagerProvider::new);
      this.register(ParticleTypes.BLOCK_MARKER, new BlockMarker.Provider());
      this.register(ParticleTypes.BLOCK, new TerrainParticle.Provider());
      this.register(ParticleTypes.BUBBLE, BubbleParticle.Provider::new);
      this.register(ParticleTypes.BUBBLE_COLUMN_UP, BubbleColumnUpParticle.Provider::new);
      this.register(ParticleTypes.BUBBLE_POP, BubblePopParticle.Provider::new);
      this.register(ParticleTypes.CAMPFIRE_COSY_SMOKE, CampfireSmokeParticle.CosyProvider::new);
      this.register(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, CampfireSmokeParticle.SignalProvider::new);
      this.register(ParticleTypes.CLOUD, PlayerCloudParticle.Provider::new);
      this.register(ParticleTypes.COMPOSTER, SuspendedTownParticle.ComposterFillProvider::new);
      this.register(ParticleTypes.CRIT, CritParticle.Provider::new);
      this.register(ParticleTypes.CURRENT_DOWN, WaterCurrentDownParticle.Provider::new);
      this.register(ParticleTypes.DAMAGE_INDICATOR, CritParticle.DamageIndicatorProvider::new);
      this.register(ParticleTypes.DRAGON_BREATH, DragonBreathParticle.Provider::new);
      this.register(ParticleTypes.DOLPHIN, SuspendedTownParticle.DolphinSpeedProvider::new);
      this.register(ParticleTypes.DRIPPING_LAVA, DripParticle::createLavaHangParticle);
      this.register(ParticleTypes.FALLING_LAVA, DripParticle::createLavaFallParticle);
      this.register(ParticleTypes.LANDING_LAVA, DripParticle::createLavaLandParticle);
      this.register(ParticleTypes.DRIPPING_WATER, DripParticle::createWaterHangParticle);
      this.register(ParticleTypes.FALLING_WATER, DripParticle::createWaterFallParticle);
      this.register(ParticleTypes.DUST, DustParticle.Provider::new);
      this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
      this.register(ParticleTypes.EFFECT, SpellParticle.Provider::new);
      this.register(ParticleTypes.ELDER_GUARDIAN, new MobAppearanceParticle.Provider());
      this.register(ParticleTypes.ENCHANTED_HIT, CritParticle.MagicProvider::new);
      this.register(ParticleTypes.ENCHANT, EnchantmentTableParticle.Provider::new);
      this.register(ParticleTypes.END_ROD, EndRodParticle.Provider::new);
      this.register(ParticleTypes.ENTITY_EFFECT, SpellParticle.MobProvider::new);
      this.register(ParticleTypes.EXPLOSION_EMITTER, new HugeExplosionSeedParticle.Provider());
      this.register(ParticleTypes.EXPLOSION, HugeExplosionParticle.Provider::new);
      this.register(ParticleTypes.SONIC_BOOM, SonicBoomParticle.Provider::new);
      this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
      this.register(ParticleTypes.FIREWORK, FireworkParticles.SparkProvider::new);
      this.register(ParticleTypes.FISHING, WakeParticle.Provider::new);
      this.register(ParticleTypes.FLAME, FlameParticle.Provider::new);
      this.register(ParticleTypes.SCULK_SOUL, SoulParticle.EmissiveProvider::new);
      this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
      this.register(ParticleTypes.SCULK_CHARGE_POP, SculkChargePopParticle.Provider::new);
      this.register(ParticleTypes.SOUL, SoulParticle.Provider::new);
      this.register(ParticleTypes.SOUL_FIRE_FLAME, FlameParticle.Provider::new);
      this.register(ParticleTypes.FLASH, FireworkParticles.FlashProvider::new);
      this.register(ParticleTypes.HAPPY_VILLAGER, SuspendedTownParticle.HappyVillagerProvider::new);
      this.register(ParticleTypes.HEART, HeartParticle.Provider::new);
      this.register(ParticleTypes.INSTANT_EFFECT, SpellParticle.InstantProvider::new);
      this.register(ParticleTypes.ITEM, new BreakingItemParticle.Provider());
      this.register(ParticleTypes.ITEM_SLIME, new BreakingItemParticle.SlimeProvider());
      this.register(ParticleTypes.ITEM_SNOWBALL, new BreakingItemParticle.SnowballProvider());
      this.register(ParticleTypes.LARGE_SMOKE, LargeSmokeParticle.Provider::new);
      this.register(ParticleTypes.LAVA, LavaParticle.Provider::new);
      this.register(ParticleTypes.MYCELIUM, SuspendedTownParticle.Provider::new);
      this.register(ParticleTypes.NAUTILUS, EnchantmentTableParticle.NautilusProvider::new);
      this.register(ParticleTypes.NOTE, NoteParticle.Provider::new);
      this.register(ParticleTypes.POOF, ExplodeParticle.Provider::new);
      this.register(ParticleTypes.PORTAL, PortalParticle.Provider::new);
      this.register(ParticleTypes.RAIN, WaterDropParticle.Provider::new);
      this.register(ParticleTypes.SMOKE, SmokeParticle.Provider::new);
      this.register(ParticleTypes.SNEEZE, PlayerCloudParticle.SneezeProvider::new);
      this.register(ParticleTypes.SNOWFLAKE, SnowflakeParticle.Provider::new);
      this.register(ParticleTypes.SPIT, SpitParticle.Provider::new);
      this.register(ParticleTypes.SWEEP_ATTACK, AttackSweepParticle.Provider::new);
      this.register(ParticleTypes.TOTEM_OF_UNDYING, TotemParticle.Provider::new);
      this.register(ParticleTypes.SQUID_INK, SquidInkParticle.Provider::new);
      this.register(ParticleTypes.UNDERWATER, SuspendedParticle.UnderwaterProvider::new);
      this.register(ParticleTypes.SPLASH, SplashParticle.Provider::new);
      this.register(ParticleTypes.WITCH, SpellParticle.WitchProvider::new);
      this.register(ParticleTypes.DRIPPING_HONEY, DripParticle::createHoneyHangParticle);
      this.register(ParticleTypes.FALLING_HONEY, DripParticle::createHoneyFallParticle);
      this.register(ParticleTypes.LANDING_HONEY, DripParticle::createHoneyLandParticle);
      this.register(ParticleTypes.FALLING_NECTAR, DripParticle::createNectarFallParticle);
      this.register(ParticleTypes.FALLING_SPORE_BLOSSOM, DripParticle::createSporeBlossomFallParticle);
      this.register(ParticleTypes.SPORE_BLOSSOM_AIR, SuspendedParticle.SporeBlossomAirProvider::new);
      this.register(ParticleTypes.ASH, AshParticle.Provider::new);
      this.register(ParticleTypes.CRIMSON_SPORE, SuspendedParticle.CrimsonSporeProvider::new);
      this.register(ParticleTypes.WARPED_SPORE, SuspendedParticle.WarpedSporeProvider::new);
      this.register(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, DripParticle::createObsidianTearHangParticle);
      this.register(ParticleTypes.FALLING_OBSIDIAN_TEAR, DripParticle::createObsidianTearFallParticle);
      this.register(ParticleTypes.LANDING_OBSIDIAN_TEAR, DripParticle::createObsidianTearLandParticle);
      this.register(ParticleTypes.REVERSE_PORTAL, ReversePortalParticle.ReversePortalProvider::new);
      this.register(ParticleTypes.WHITE_ASH, WhiteAshParticle.Provider::new);
      this.register(ParticleTypes.SMALL_FLAME, FlameParticle.SmallFlameProvider::new);
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_WATER, DripParticle::createDripstoneWaterHangParticle);
      this.register(ParticleTypes.FALLING_DRIPSTONE_WATER, DripParticle::createDripstoneWaterFallParticle);
      this.register(ParticleTypes.DRIPPING_CHERRY_LEAVES, DripParticle::createCherryLeavesHangParticle);
      this.register(ParticleTypes.FALLING_CHERRY_LEAVES, DripParticle::createCherryLeavesFallParticle);
      this.register(ParticleTypes.LANDING_CHERRY_LEAVES, DripParticle::createCherryLeavesLandParticle);
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, DripParticle::createDripstoneLavaHangParticle);
      this.register(ParticleTypes.FALLING_DRIPSTONE_LAVA, DripParticle::createDripstoneLavaFallParticle);
      this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
      this.register(ParticleTypes.GLOW_SQUID_INK, SquidInkParticle.GlowInkProvider::new);
      this.register(ParticleTypes.GLOW, GlowParticle.GlowSquidProvider::new);
      this.register(ParticleTypes.WAX_ON, GlowParticle.WaxOnProvider::new);
      this.register(ParticleTypes.WAX_OFF, GlowParticle.WaxOffProvider::new);
      this.register(ParticleTypes.ELECTRIC_SPARK, GlowParticle.ElectricSparkProvider::new);
      this.register(ParticleTypes.SCRAPE, GlowParticle.ScrapeProvider::new);
      this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
   }

   private <T extends ParticleOptions> void register(ParticleType<T> param0, ParticleProvider<T> param1) {
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(param0), param1);
   }

   private <T extends ParticleOptions> void register(ParticleType<T> param0, ParticleProvider.Sprite<T> param1) {
      this.register(param0, l_param1 -> (l2_param2, l2_param3, l2_param4, l2_param6, l2_param8, l2_param10, l2_param12, l2_param14) -> {
         TextureSheetParticle l2_var16 = param1.createParticle(l2_param2, l2_param3, l2_param4, l2_param6, l2_param8, l2_param10, l2_param12, l2_param14);
         if (l2_var16 != null) {
            l2_var16.pickSprite(l_param1);
         }

         return l2_var16;
      });
   }

   private <T extends ParticleOptions> void register(ParticleType<T> param0, ParticleEngine.SpriteParticleRegistration<T> param1) {
      ParticleEngine.MutableSpriteSet var3 = new ParticleEngine.MutableSpriteSet();
      this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(param0), var3);
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(param0), param1.create(var3));
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier param0,
      ResourceManager param1,
      ProfilerFiller param2,
      ProfilerFiller param3,
      Executor param4,
      Executor param5
   ) {
      record ParticleDefinition(ResourceLocation id, Optional<List<ResourceLocation>> sprites) {
      }

      CompletableFuture<List<ParticleDefinition>> var7 = CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(
            () -> PARTICLE_LISTER.listMatchingResources(param1), param4
         )
         .thenCompose(l_param1 -> {
            List<CompletableFuture<ParticleDefinition>> l_var3 = new ArrayList<>(l_param1.size());
            l_param1.forEach((l2_param2, l2_param3) -> {
               ResourceLocation l2_var5 = PARTICLE_LISTER.fileToId(l2_param2);
               l_var3.add(CompletableFuture.supplyAsync(() -> new ParticleDefinition(l2_var5, this.loadParticleDescription(l2_var5, l2_param3)), param4));
            });
            return Util.sequence(l_var3);
         });
      CompletableFuture<SpriteLoader.Preparations> var8 = SpriteLoader.create(this.textureAtlas)
         .loadAndStitch(param1, PARTICLES_ATLAS_INFO, 0, param4)
         .thenCompose(SpriteLoader.Preparations::waitForUpload);
      return CompletableFuture.allOf(var8, var7).thenCompose(param0::wait).thenAcceptAsync(l_param3 -> {
         this.clearParticles();
         param3.startTick();
         param3.push("upload");
         SpriteLoader.Preparations l_var5 = var8.join();
         this.textureAtlas.upload(l_var5);
         param3.popPush("bindSpriteSets");
         Set<ResourceLocation> l_var6 = new HashSet<>();
         TextureAtlasSprite l_var7 = l_var5.missing();
         var7.join().forEach(l2_param3 -> {
            Optional<List<ResourceLocation>> l2_var5 = l2_param3.sprites();
            if (!l2_var5.isEmpty()) {
               List<TextureAtlasSprite> l2_var6 = new ArrayList<>();

               for (ResourceLocation l2_var8 : l2_var5.get()) {
                  TextureAtlasSprite l2_var9 = l_var5.regions().get(l2_var8);
                  if (l2_var9 == null) {
                     l_var6.add(l2_var8);
                     l2_var6.add(l_var7);
                  } else {
                     l2_var6.add(l2_var9);
                  }
               }

               if (l2_var6.isEmpty()) {
                  l2_var6.add(l_var7);
               }

               this.spriteSets.get(l2_param3.id()).rebind(l2_var6);
            }
         });
         if (!l_var6.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", l_var6.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         param3.pop();
         param3.endTick();
      }, param5);
   }

   public void close() {
      this.textureAtlas.clearTextureData();
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation param0, Resource param1) {
      if (!this.spriteSets.containsKey(param0)) {
         LOGGER.debug("Redundant texture list for particle: {}", param0);
         return Optional.empty();
      }

      try (Reader var3 = param1.openAsReader()) {
         ParticleDescription var4 = ParticleDescription.fromJson(GsonHelper.parse(var3));
         return Optional.of(var4.getTextures());
      } catch (IOException var3) {
         throw new IllegalStateException("Failed to load description for particle " + param0, var3);
      }
   }

   public void createTrackingEmitter(Entity param0, ParticleOptions param1) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, param0, param1));
   }

   public void createTrackingEmitter(Entity param0, ParticleOptions param1, int param2) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, param0, param1, param2));
   }

   @Nullable
   public Particle createParticle(ParticleOptions param0, double param1, double param3, double param5, double param7, double param9, double param11) {
      Particle var14 = this.makeParticle(param0, param1, param3, param5, param7, param9, param11);
      if (var14 != null) {
         this.add(var14);
         return var14;
      } else {
         return null;
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(
      T param0, double param1, double param3, double param5, double param7, double param9, double param11
   ) {
      ParticleProvider<T> var14 = (ParticleProvider<T>)this.providers.get(BuiltInRegistries.PARTICLE_TYPE.getId(param0.getType()));
      return var14 == null ? null : var14.createParticle(param0, this.level, param1, param3, param5, param7, param9, param11);
   }

   public void add(Particle param0) {
      Optional<ParticleGroup> var2 = param0.getParticleGroup();
      if (var2.isPresent()) {
         if (this.hasSpaceInParticleLimit(var2.get())) {
            this.particlesToAdd.add(param0);
            this.updateCount(var2.get(), 1);
         }
      } else {
         this.particlesToAdd.add(param0);
      }
   }

   public void tick() {
      this.particles.forEach((l_param0, l_param1) -> {
         this.level.getProfiler().push(l_param0.toString());
         this.tickParticleList(l_param1);
         this.level.getProfiler().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         // Cooperative-port hardening: same uncatchable wasm trap vector as
         // tickParticleList - removeAll() on an ArrayDeque lands in ArrayDeque.removeAt,
         // which is where RuntimeErrors escaped before. Snapshot + tick + rebuild via
         // clear()/add() only; never Iterator.remove() or removeAll() here.
         TrackingEmitter[] var1 = this.trackingEmitters.toArray(new TrackingEmitter[0]);
         List<TrackingEmitter> var2 = this.trackingRebuildBuffer;
         if (!var2.isEmpty()) {
            var2.clear();
         }
         boolean var3 = false;

         for (int var4 = 0; var4 < var1.length; var4++) {
            TrackingEmitter var5 = var1[var4];
            try {
               var5.tick();
               if (var5.isAlive()) {
                  var2.add(var5);
               } else {
                  var3 = true;
               }
            } catch (Throwable var6) {
               var3 = true;
               try {
                  LOGGER.error("Tracking emitter tick failed; removing emitter", var6);
               } catch (Throwable var7) {
               }
            }
         }

         if (var3) {
            try {
               this.trackingEmitters.clear();
               for (int var4 = 0; var4 < var2.size(); var4++) {
                  this.trackingEmitters.add(var2.get(var4));
               }
            } catch (Throwable var6) {
               try {
                  this.trackingEmitters.clear();
               } catch (Throwable var7) {
               }
            }
         }

         if (!var2.isEmpty()) {
            var2.clear();
         }
      }

      Particle var1;
      if (!this.particlesToAdd.isEmpty()) {
         while ((var1 = this.particlesToAdd.poll()) != null) {
            this.particles.computeIfAbsent(var1.getRenderType(), l_param0 -> EvictingQueue.create(16384)).add(var1);
         }
      }
   }

   private void tickParticleList(Collection<Particle> param0) {
      if (!param0.isEmpty()) {
         // Cooperative-port hardening: never use Iterator.remove() here. A wasm null
         // trap inside ArrayDeque.removeAt (fired while removing a dead particle) is a
         // RuntimeError that Java catch blocks cannot intercept and it kills the whole
         // VM. Snapshot the list instead, tick each particle, and if anything died or
         // failed, rebuild the collection once via clear()+add() - simple code paths
         // with no iterator removal involved.
         Particle[] var2 = param0.toArray(new Particle[0]);
         List<Particle> var3 = this.particleRebuildBuffer;
         if (var3.size() > 0) {
            var3.clear();
         }
         boolean var4 = false;

         for (int var5 = 0; var5 < var2.length; var5++) {
            Particle var6 = var2[var5];
            try {
               this.tickParticle(var6);
            } catch (Throwable var7) {
               // tickParticle's own handler normally absorbs failures; this only fires
               // if something escapes it. Treat the particle as removed.
               var4 = true;
               try {
                  LOGGER.error("Particle tick escaped for {}", var7.toString());
               } catch (Throwable var8) {
               }
            }
            try {
               if (!var6.isAlive()) {
                  var4 = true;
                  var6.getParticleGroup().ifPresent(l_param0 -> this.updateCount(l_param0, -1));
               } else {
                  var3.add(var6);
               }
            } catch (Throwable var9) {
               var4 = true;
            }
         }

         if (var4) {
            try {
               param0.clear();
               for (int var5 = 0; var5 < var3.size(); var5++) {
                  param0.add(var3.get(var5));
               }
            } catch (Throwable var10) {
               try {
                  LOGGER.error("Particle list rebuild failed; clearing list");
               } catch (Throwable var11) {
               }
               try {
                  param0.clear();
               } catch (Throwable var11) {
               }
            }
         }

         if (var3.size() > 0) {
            var3.clear();
         }
      }
   }

   private void updateCount(ParticleGroup param0, int param1) {
      this.trackedParticleCounts.addTo(param0, param1);
   }

   private void tickParticle(Particle param0) {
      try {
         param0.tick();
      } catch (Throwable var2) {
         // Cooperative-port hardening: during level teardown a particle can tick against
         // state that is already gone (e.g. an underwater particle left over when the
         // integrated server halts). Vanilla treats this as fatal and writes a crash
         // report; for the port it is disproportionately harmful, so log it and drop
         // the particle instead of killing the client. Every call inside this handler is
         // guarded so the handler itself can never throw.
         try {
            LOGGER.error("Particle tick failed; removing particle", var2);
         } catch (Throwable var3) {
            try {
               LOGGER.error("Particle tick failed for {}", param0.getClass().getName());
            } catch (Throwable var4) {
            }
         }
         try {
            param0.remove();
         } catch (Throwable var3) {
         }
      }
   }

   public void render(PoseStack param0, MultiBufferSource.BufferSource param1, LightTexture param2, Camera param3, float param4) {
      param2.turnOnLightLayer();
      RenderSystem.enableDepthTest();
      PoseStack var6 = RenderSystem.getModelViewStack();
      var6.pushPose();
      var6.mulPoseMatrix(param0.last().pose());
      RenderSystem.applyModelViewMatrix();

      for (ParticleRenderType var8 : RENDER_ORDER) {
         Iterable<Particle> var9 = this.particles.get(var8);
         if (var9 != null) {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator var10 = Tesselator.getInstance();
            BufferBuilder var11 = var10.getBuilder();
            var8.begin(var11, this.textureManager);

            for (Particle var13 : var9) {
               try {
                  var13.render(var11, param3, param4);
               } catch (Throwable var14) {
                  CrashReport var15 = CrashReport.forThrowable(var14, "Rendering Particle");
                  CrashReportCategory var16 = var15.addCategory("Particle being rendered");
                  var16.setDetail("Particle", var13::toString);
                  var16.setDetail("Particle Type", var8::toString);
                  throw new ReportedException(var15);
               }
            }

            var8.end(var10);
         }
      }

      var6.popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      param2.turnOffLightLayer();
   }

   public void setLevel(@Nullable ClientLevel param0) {
      this.level = param0;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public void destroy(BlockPos param0, BlockState param1) {
      if (!param1.isAir() && param1.shouldSpawnParticlesOnBreak()) {
         VoxelShape var3 = param1.getShape(this.level, param0);
         double var4 = 0.25;
         var3.forAllBoxes(
            (l_param2, l_param4, l_param6, l_param8, l_param10, l_param12) -> {
               double l_var15 = Math.min(1.0, l_param8 - l_param2);
               double l_var17 = Math.min(1.0, l_param10 - l_param4);
               double l_var19 = Math.min(1.0, l_param12 - l_param6);
               int l_var21 = Math.max(2, Mth.ceil(l_var15 / 0.25));
               int l_var22 = Math.max(2, Mth.ceil(l_var17 / 0.25));
               int l_var23 = Math.max(2, Mth.ceil(l_var19 / 0.25));

               for (int l_var24 = 0; l_var24 < l_var21; l_var24++) {
                  for (int l_var25 = 0; l_var25 < l_var22; l_var25++) {
                     for (int l_var26 = 0; l_var26 < l_var23; l_var26++) {
                        double l_var27 = (l_var24 + 0.5) / l_var21;
                        double l_var29 = (l_var25 + 0.5) / l_var22;
                        double l_var31 = (l_var26 + 0.5) / l_var23;
                        double l_var33 = l_var27 * l_var15 + l_param2;
                        double l_var35 = l_var29 * l_var17 + l_param4;
                        double l_var37 = l_var31 * l_var19 + l_param6;
                        this.add(
                           new TerrainParticle(
                              this.level,
                              param0.getX() + l_var33,
                              param0.getY() + l_var35,
                              param0.getZ() + l_var37,
                              l_var27 - 0.5,
                              l_var29 - 0.5,
                              l_var31 - 0.5,
                              param1,
                              param0
                           )
                        );
                     }
                  }
               }
            }
         );
      }
   }

   public void crack(BlockPos param0, Direction param1) {
      BlockState var3 = this.level.getBlockState(param0);
      if (var3.getRenderShape() != RenderShape.INVISIBLE) {
         int var4 = param0.getX();
         int var5 = param0.getY();
         int var6 = param0.getZ();
         float var7 = 0.1F;
         AABB var8 = var3.getShape(this.level, param0).bounds();
         double var9 = var4 + this.random.nextDouble() * (var8.maxX - var8.minX - 0.2F) + 0.1F + var8.minX;
         double var11 = var5 + this.random.nextDouble() * (var8.maxY - var8.minY - 0.2F) + 0.1F + var8.minY;
         double var13 = var6 + this.random.nextDouble() * (var8.maxZ - var8.minZ - 0.2F) + 0.1F + var8.minZ;
         if (param1 == Direction.DOWN) {
            var11 = var5 + var8.minY - 0.1F;
         }

         if (param1 == Direction.UP) {
            var11 = var5 + var8.maxY + 0.1F;
         }

         if (param1 == Direction.NORTH) {
            var13 = var6 + var8.minZ - 0.1F;
         }

         if (param1 == Direction.SOUTH) {
            var13 = var6 + var8.maxZ + 0.1F;
         }

         if (param1 == Direction.WEST) {
            var9 = var4 + var8.minX - 0.1F;
         }

         if (param1 == Direction.EAST) {
            var9 = var4 + var8.maxX + 0.1F;
         }

         this.add(new TerrainParticle(this.level, var9, var11, var13, 0.0, 0.0, 0.0, var3, param0).setPower(0.2F).scale(0.6F));
      }
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(Collection::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleGroup param0) {
      return this.trackedParticleCounts.getInt(param0) < param0.getLimit();
   }

   private void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }

   /** Cooperative-port: public hook so Minecraft.clearLevel can drop all particles at
    * disconnect time; otherwise leftovers keep ticking through the shutdown window and
    * reference a level that is being torn down underneath them. */
   public void clear() {
      this.clearParticles();
   }

   static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      @Override
      public TextureAtlasSprite get(int param0, int param1) {
         return this.sprites.get(param0 * (this.sprites.size() - 1) / param1);
      }

      @Override
      public TextureAtlasSprite get(RandomSource param0) {
         return this.sprites.get(param0.nextInt(this.sprites.size()));
      }

      public void rebind(List<TextureAtlasSprite> param0) {
         this.sprites = ImmutableList.copyOf(param0);
      }
   }

   @FunctionalInterface
   interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet var1);
   }
}
