package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

public class DebugScreenOverlay extends GuiComponent {
   private static final int COLOR_GREY = 14737632;
   private static final int MARGIN_RIGHT = 2;
   private static final int MARGIN_LEFT = 2;
   private static final int MARGIN_TOP = 2;
   private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Util.make(new EnumMap<>(Heightmap.Types.class), param0 -> {
      param0.put(Heightmap.Types.WORLD_SURFACE_WG, "SW");
      param0.put(Heightmap.Types.WORLD_SURFACE, "S");
      param0.put(Heightmap.Types.OCEAN_FLOOR_WG, "OW");
      param0.put(Heightmap.Types.OCEAN_FLOOR, "O");
      param0.put(Heightmap.Types.MOTION_BLOCKING, "M");
      param0.put(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, "ML");
   });
   private final Minecraft minecraft;
   private final DebugScreenOverlay.AllocationRateCalculator allocationRateCalculator;
   private final Font font;
   private HitResult block;
   private HitResult liquid;
   @Nullable
   private ChunkPos lastPos;
   @Nullable
   private LevelChunk clientChunk;
   @Nullable
   private CompletableFuture<LevelChunk> serverChunk;
   private static final int RED = -65536;
   private static final int YELLOW = -256;
   private static final int GREEN = -16711936;

   public DebugScreenOverlay(Minecraft param0) {
      this.minecraft = param0;
      this.allocationRateCalculator = new DebugScreenOverlay.AllocationRateCalculator();
      this.font = param0.font;
   }

   public void clearChunkCache() {
      this.serverChunk = null;
      this.clientChunk = null;
   }

   public void render(PoseStack param0) {
      this.minecraft.getProfiler().push("debug");
      Entity var2 = this.minecraft.getCameraEntity();
      this.block = var2.pick(20.0, 0.0F, false);
      this.liquid = var2.pick(20.0, 0.0F, true);
      this.drawGameInformation(param0);
      this.drawSystemInformation(param0);
      if (this.minecraft.options.renderFpsChart) {
         int var3 = this.minecraft.getWindow().getGuiScaledWidth();
         this.drawChart(param0, this.minecraft.getFrameTimer(), 0, var3 / 2, true);
         IntegratedServer var4 = this.minecraft.getSingleplayerServer();
         if (var4 != null) {
            this.drawChart(param0, var4.getFrameTimer(), var3 - Math.min(var3 / 2, 240), var3 / 2, false);
         }
      }

      this.minecraft.getProfiler().pop();
   }

   protected void drawGameInformation(PoseStack param0) {
      List<String> var2 = this.getGameInformation();
      var2.add("");
      boolean var3 = this.minecraft.getSingleplayerServer() != null;
      var2.add(
         "Debug: Pie [shift]: "
            + (this.minecraft.options.renderDebugCharts ? "visible" : "hidden")
            + (var3 ? " FPS + TPS" : " FPS")
            + " [alt]: "
            + (this.minecraft.options.renderFpsChart ? "visible" : "hidden")
      );
      var2.add("For help: press F3 + Q");

      for (int var4 = 0; var4 < var2.size(); var4++) {
         String var5 = var2.get(var4);
         if (!Strings.isNullOrEmpty(var5)) {
            int var6 = 9;
            int var7 = this.font.width(var5);
            int var8 = 2;
            int var9 = 2 + var6 * var4;
            fill(param0, 1, var9 - 1, 2 + var7 + 1, var9 + var6 - 1, -1873784752);
            this.font.draw(param0, var5, 2.0F, var9, 14737632);
         }
      }
   }

   protected void drawSystemInformation(PoseStack param0) {
      List<String> var2 = this.getSystemInformation();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         String var4 = var2.get(var3);
         if (!Strings.isNullOrEmpty(var4)) {
            int var5 = 9;
            int var6 = this.font.width(var4);
            int var7 = this.minecraft.getWindow().getGuiScaledWidth() - 2 - var6;
            int var8 = 2 + var5 * var3;
            fill(param0, var7 - 1, var8 - 1, var7 + var6 + 1, var8 + var5 - 1, -1873784752);
            this.font.draw(param0, var4, var7, var8, 14737632);
         }
      }
   }

   protected List<String> getGameInformation() {
      IntegratedServer var2 = this.minecraft.getSingleplayerServer();
      Connection var3 = this.minecraft.getConnection().getConnection();
      float var4 = var3.getAverageSentPackets();
      float var5 = var3.getAverageReceivedPackets();
      String var1;
      if (var2 != null) {
         var1 = String.format(Locale.ROOT, "Integrated server @ %.0f ms ticks, %.0f tx, %.0f rx", var2.getAverageTickTime(), var4, var5);
      } else {
         var1 = String.format(Locale.ROOT, "\"%s\" server, %.0f tx, %.0f rx", this.minecraft.player.getServerBrand(), var4, var5);
      }

      BlockPos var6 = this.minecraft.getCameraEntity().blockPosition();
      if (this.minecraft.showOnlyReducedInfo()) {
         return Lists.newArrayList(
            "Minecraft "
               + SharedConstants.getCurrentVersion().getName()
               + " ("
               + this.minecraft.getLaunchedVersion()
               + "/"
               + ClientBrandRetriever.getClientModName()
               + ")",
            this.minecraft.fpsString,
            var1,
            this.minecraft.levelRenderer.getChunkStatistics(),
            this.minecraft.levelRenderer.getEntityStatistics(),
            "P: " + this.minecraft.particleEngine.countParticles() + ". T: " + this.minecraft.level.getEntityCount(),
            this.minecraft.level.gatherChunkSourceStats(),
            "",
            String.format(Locale.ROOT, "Chunk-relative: %d %d %d", var6.getX() & 15, var6.getY() & 15, var6.getZ() & 15)
         );
      }

      Entity var7 = this.minecraft.getCameraEntity();
      Direction var8 = var7.getDirection();

      String var9 = switch (var8) {
         case NORTH -> "Towards negative Z";
         case SOUTH -> "Towards positive Z";
         case WEST -> "Towards negative X";
         case EAST -> "Towards positive X";
         default -> "Invalid";
      };
      ChunkPos var10 = new ChunkPos(var6);
      if (!Objects.equals(this.lastPos, var10)) {
         this.lastPos = var10;
         this.clearChunkCache();
      }

      Level var11 = this.getLevel();
      LongSet var12 = var11 instanceof ServerLevel ? ((ServerLevel)var11).getForcedChunks() : LongSets.EMPTY_SET;
      List<String> var13 = Lists.newArrayList(
         "Minecraft "
            + SharedConstants.getCurrentVersion().getName()
            + " ("
            + this.minecraft.getLaunchedVersion()
            + "/"
            + ClientBrandRetriever.getClientModName()
            + ("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType())
            + ")",
         this.minecraft.fpsString,
         var1,
         this.minecraft.levelRenderer.getChunkStatistics(),
         this.minecraft.levelRenderer.getEntityStatistics(),
         "P: " + this.minecraft.particleEngine.countParticles() + ". T: " + this.minecraft.level.getEntityCount(),
         this.minecraft.level.gatherChunkSourceStats()
      );
      String var14 = this.getServerChunkStats();
      if (var14 != null) {
         var13.add(var14);
      }

      var13.add(this.minecraft.level.dimension().location() + " FC: " + var12.size());
      var13.add("");
      var13.add(
         String.format(
            Locale.ROOT,
            "XYZ: %.3f / %.5f / %.3f",
            this.minecraft.getCameraEntity().getX(),
            this.minecraft.getCameraEntity().getY(),
            this.minecraft.getCameraEntity().getZ()
         )
      );
      var13.add(
         String.format(Locale.ROOT, "Block: %d %d %d [%d %d %d]", var6.getX(), var6.getY(), var6.getZ(), var6.getX() & 15, var6.getY() & 15, var6.getZ() & 15)
      );
      var13.add(
         String.format(
            Locale.ROOT,
            "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
            var10.x,
            SectionPos.blockToSectionCoord(var6.getY()),
            var10.z,
            var10.getRegionLocalX(),
            var10.getRegionLocalZ(),
            var10.getRegionX(),
            var10.getRegionZ()
         )
      );
      var13.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", var8, var9, Mth.wrapDegrees(var7.getYRot()), Mth.wrapDegrees(var7.getXRot())));
      LevelChunk var15 = this.getClientChunk();
      if (var15.isEmpty()) {
         var13.add("Waiting for chunk...");
      } else {
         int var16 = this.minecraft.level.getChunkSource().getLightEngine().getRawBrightness(var6, 0);
         int var17 = this.minecraft.level.getBrightness(LightLayer.SKY, var6);
         int var18 = this.minecraft.level.getBrightness(LightLayer.BLOCK, var6);
         var13.add("Client Light: " + var16 + " (" + var17 + " sky, " + var18 + " block)");
         LevelChunk var19 = this.getServerChunk();
         StringBuilder var20 = new StringBuilder("CH");

         for (Heightmap.Types var24 : Heightmap.Types.values()) {
            if (var24.sendToClient()) {
               var20.append(" ").append(HEIGHTMAP_NAMES.get(var24)).append(": ").append(var15.getHeight(var24, var6.getX(), var6.getZ()));
            }
         }

         var13.add(var20.toString());
         var20.setLength(0);
         var20.append("SH");

         for (Heightmap.Types var24 : Heightmap.Types.values()) {
            if (var24.keepAfterWorldgen()) {
               var20.append(" ").append(HEIGHTMAP_NAMES.get(var24)).append(": ");
               if (var19 != null) {
                  var20.append(var19.getHeight(var24, var6.getX(), var6.getZ()));
               } else {
                  var20.append("??");
               }
            }
         }

         var13.add(var20.toString());
         if (var6.getY() >= this.minecraft.level.getMinBuildHeight() && var6.getY() < this.minecraft.level.getMaxBuildHeight()) {
            var13.add("Biome: " + printBiome(this.minecraft.level.getBiome(var6)));
            long var21 = 0L;
            float var23 = 0.0F;
            if (var19 != null) {
               var23 = var11.getMoonBrightness();
               var21 = var19.getInhabitedTime();
            }

            DifficultyInstance var24 = new DifficultyInstance(var11.getDifficulty(), var11.getDayTime(), var21, var23);
            var13.add(
               String.format(
                  Locale.ROOT,
                  "Local Difficulty: %.2f // %.2f (Day %d)",
                  var24.getEffectiveDifficulty(),
                  var24.getSpecialMultiplier(),
                  this.minecraft.level.getDayTime() / 24000L
               )
            );
         }

         if (var19 != null && var19.isOldNoiseGeneration()) {
            var13.add("Blending: Old");
         }
      }

      ServerLevel var16 = this.getServerLevel();
      if (var16 != null) {
         ServerChunkCache var17 = var16.getChunkSource();
         ChunkGenerator var18 = var17.getGenerator();
         RandomState var19 = var17.randomState();
         var18.addDebugScreenInfo(var13, var19, var6);
         Climate.Sampler var20 = var19.sampler();
         BiomeSource var21 = var18.getBiomeSource();
         var21.addDebugInfo(var13, var6, var20);
         NaturalSpawner.SpawnState var22 = var17.getLastSpawnState();
         if (var22 != null) {
            Object2IntMap<MobCategory> var23 = var22.getMobCategoryCounts();
            int var24 = var22.getSpawnableChunkCount();
            var13.add(
               "SC: "
                  + var24
                  + ", "
                  + Stream.of(MobCategory.values())
                     .map(l_param1 -> Character.toUpperCase(l_param1.getName().charAt(0)) + ": " + var23.getInt(l_param1))
                     .collect(Collectors.joining(", "))
            );
         } else {
            var13.add("SC: N/A");
         }
      }

      PostChain var17 = this.minecraft.gameRenderer.currentEffect();
      if (var17 != null) {
         var13.add("Shader: " + var17.getName());
      }

      var13.add(
         this.minecraft.getSoundManager().getDebugString()
            + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(this.minecraft.player.getCurrentMood() * 100.0F))
      );
      return var13;
   }

   private static String printBiome(Holder<Biome> param0) {
      return param0.unwrap().map(l_param0 -> l_param0.location().toString(), l_param0 -> "[unregistered " + l_param0 + "]");
   }

   @Nullable
   private ServerLevel getServerLevel() {
      IntegratedServer var1 = this.minecraft.getSingleplayerServer();
      return var1 != null ? var1.getLevel(this.minecraft.level.dimension()) : null;
   }

   @Nullable
   private String getServerChunkStats() {
      ServerLevel var1 = this.getServerLevel();
      return var1 != null ? var1.gatherChunkSourceStats() : null;
   }

   private Level getLevel() {
      return DataFixUtils.orElse(
         Optional.ofNullable(this.minecraft.getSingleplayerServer())
            .flatMap(l_param0 -> Optional.ofNullable(l_param0.getLevel(this.minecraft.level.dimension()))),
         this.minecraft.level
      );
   }

   @Nullable
   private LevelChunk getServerChunk() {
      if (this.serverChunk == null) {
         ServerLevel var1 = this.getServerLevel();
         if (var1 != null) {
            this.serverChunk = var1.getChunkSource()
               .getChunkFuture(this.lastPos.x, this.lastPos.z, ChunkStatus.FULL, false)
               .thenApply(l_param0 -> l_param0.map(l2_param0 -> (LevelChunk)l2_param0, l2_param0 -> null));
         }

         if (this.serverChunk == null) {
            this.serverChunk = CompletableFuture.completedFuture(this.getClientChunk());
         }
      }

      return this.serverChunk.getNow(null);
   }

   private LevelChunk getClientChunk() {
      if (this.clientChunk == null) {
         this.clientChunk = this.minecraft.level.getChunk(this.lastPos.x, this.lastPos.z);
      }

      return this.clientChunk;
   }

   protected List<String> getSystemInformation() {
   // Runtime.getRuntime().maxMemory() is absent from the WASM-GC TeaVM classlib. The
   // replacement is PlatformRuntime.maxMemory(), the same seam SystemReport's "Memory"
   // detail already uses, and the same fixed figure it reports: a WASM-GC module has one
   // fixed WebAssembly.Memory, so the heap ceiling and the heap size are one number rather
   // than a limit and a reading. See SystemReport's constructor for the full note.
      long var1 = PlatformRuntime.maxMemory();
      long var3 = Runtime.getRuntime().totalMemory();
      long var5 = Runtime.getRuntime().freeMemory();
      long var7 = var3 - var5;
      List<String> var9 = Lists.newArrayList(
         String.format(Locale.ROOT, "Java: %s %dbit", System.getProperty("java.version"), this.minecraft.is64Bit() ? 64 : 32),
         String.format(Locale.ROOT, "Mem: % 2d%% %03d/%03dMB", var7 * 100L / var1, bytesToMegabytes(var7), bytesToMegabytes(var1)),
         String.format(Locale.ROOT, "Allocation rate: %03dMB /s", bytesToMegabytes(this.allocationRateCalculator.bytesAllocatedPerSecond(var7))),
         String.format(Locale.ROOT, "Allocated: % 2d%% %03dMB", var3 * 100L / var1, bytesToMegabytes(var3)),
         "",
         String.format(Locale.ROOT, "CPU: %s", GlUtil.getCpuInfo()),
         "",
         String.format(
            Locale.ROOT,
            "Display: %dx%d (%s)",
            Minecraft.getInstance().getWindow().getWidth(),
            Minecraft.getInstance().getWindow().getHeight(),
            GlUtil.getVendor()
         ),
         GlUtil.getRenderer(),
         GlUtil.getOpenGLVersion()
      );
      if (this.minecraft.showOnlyReducedInfo()) {
         return var9;
      }

      if (this.block.getType() == HitResult.Type.BLOCK) {
         BlockPos var10 = ((BlockHitResult)this.block).getBlockPos();
         BlockState var11 = this.minecraft.level.getBlockState(var10);
         var9.add("");
         var9.add(ChatFormatting.UNDERLINE + "Targeted Block: " + var10.getX() + ", " + var10.getY() + ", " + var10.getZ());
         var9.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(var11.getBlock())));

         for (Entry<Property<?>, Comparable<?>> var13 : var11.getValues().entrySet()) {
            var9.add(this.getPropertyValueString(var13));
         }

         var11.getTags().map(l_param0 -> "#" + l_param0.location()).forEach(var9::add);
      }

      if (this.liquid.getType() == HitResult.Type.BLOCK) {
         BlockPos var10 = ((BlockHitResult)this.liquid).getBlockPos();
         FluidState var11 = this.minecraft.level.getFluidState(var10);
         var9.add("");
         var9.add(ChatFormatting.UNDERLINE + "Targeted Fluid: " + var10.getX() + ", " + var10.getY() + ", " + var10.getZ());
         var9.add(String.valueOf(BuiltInRegistries.FLUID.getKey(var11.getType())));

         for (Entry<Property<?>, Comparable<?>> var13 : var11.getValues().entrySet()) {
            var9.add(this.getPropertyValueString(var13));
         }

         var11.getTags().map(l_param0 -> "#" + l_param0.location()).forEach(var9::add);
      }

      Entity var10 = this.minecraft.crosshairPickEntity;
      if (var10 != null) {
         var9.add("");
         var9.add(ChatFormatting.UNDERLINE + "Targeted Entity");
         var9.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(var10.getType())));
      }

      return var9;
   }

   private String getPropertyValueString(Entry<Property<?>, Comparable<?>> param0) {
      Property<?> var2 = param0.getKey();
      Comparable<?> var3 = param0.getValue();
      String var4 = Util.getPropertyName(var2, var3);
      if (Boolean.TRUE.equals(var3)) {
         var4 = ChatFormatting.GREEN + var4;
      } else if (Boolean.FALSE.equals(var3)) {
         var4 = ChatFormatting.RED + var4;
      }

      return var2.getName() + ": " + var4;
   }

   private void drawChart(PoseStack param0, FrameTimer param1, int param2, int param3, boolean param4) {
      RenderSystem.disableDepthTest();
      int var6 = param1.getLogStart();
      int var7 = param1.getLogEnd();
      long[] var8 = param1.getLog();
      int var9 = var6;
      int var10 = param2;
      int var11 = Math.max(0, var8.length - param3);
      int var12 = var8.length - var11;
      var9 = param1.wrapIndex(var9 + var11);
      long var13 = 0L;
      int var15 = Integer.MAX_VALUE;
      int var16 = Integer.MIN_VALUE;

      for (int var17 = 0; var17 < var12; var17++) {
         int var18 = (int)(var8[param1.wrapIndex(var9 + var17)] / 1000000L);
         var15 = Math.min(var15, var18);
         var16 = Math.max(var16, var18);
         var13 += var18;
      }

      int var17 = this.minecraft.getWindow().getGuiScaledHeight();
      fill(param0, param2, var17 - 60, param2 + var12, var17, -1873784752);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder var18 = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      var18.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f var19 = Transformation.identity().getMatrix();

      while (var9 != var7) {
         int var20 = param1.scaleSampleTo(var8[var9], param4 ? 30 : 60, param4 ? 60 : 20);
         int var21 = param4 ? 100 : 60;
         int var22 = this.getSampleColor(Mth.clamp(var20, 0, var21), 0, var21 / 2, var21);
         int var23 = var22 >> 24 & 0xFF;
         int var24 = var22 >> 16 & 0xFF;
         int var25 = var22 >> 8 & 0xFF;
         int var26 = var22 & 0xFF;
         var18.vertex(var19, var10 + 1, var17, 0.0F).color(var24, var25, var26, var23).endVertex();
         var18.vertex(var19, var10 + 1, var17 - var20 + 1, 0.0F).color(var24, var25, var26, var23).endVertex();
         var18.vertex(var19, var10, var17 - var20 + 1, 0.0F).color(var24, var25, var26, var23).endVertex();
         var18.vertex(var19, var10, var17, 0.0F).color(var24, var25, var26, var23).endVertex();
         var10++;
         var9 = param1.wrapIndex(var9 + 1);
      }

      BufferUploader.drawWithShader(var18.end());
      RenderSystem.disableBlend();
      if (param4) {
         fill(param0, param2 + 1, var17 - 30 + 1, param2 + 14, var17 - 30 + 10, -1873784752);
         this.font.draw(param0, "60 FPS", param2 + 2, var17 - 30 + 2, 14737632);
         hLine(param0, param2, param2 + var12 - 1, var17 - 30, -1);
         fill(param0, param2 + 1, var17 - 60 + 1, param2 + 14, var17 - 60 + 10, -1873784752);
         this.font.draw(param0, "30 FPS", param2 + 2, var17 - 60 + 2, 14737632);
         hLine(param0, param2, param2 + var12 - 1, var17 - 60, -1);
      } else {
         fill(param0, param2 + 1, var17 - 60 + 1, param2 + 14, var17 - 60 + 10, -1873784752);
         this.font.draw(param0, "20 TPS", param2 + 2, var17 - 60 + 2, 14737632);
         hLine(param0, param2, param2 + var12 - 1, var17 - 60, -1);
      }

      hLine(param0, param2, param2 + var12 - 1, var17 - 1, -1);
      vLine(param0, param2, var17 - 60, var17, -1);
      vLine(param0, param2 + var12 - 1, var17 - 60, var17, -1);
      int var20 = this.minecraft.options.framerateLimit().get();
      if (param4 && var20 > 0 && var20 <= 250) {
         hLine(param0, param2, param2 + var12 - 1, var17 - 1 - (int)(1800.0 / var20), -16711681);
      }

      String var21 = var15 + " ms min";
      String var22 = var13 / var12 + " ms avg";
      String var23 = var16 + " ms max";
      this.font.drawShadow(param0, var21, param2 + 2, var17 - 60 - 9, 14737632);
      this.font.drawShadow(param0, var22, param2 + var12 / 2 - this.font.width(var22) / 2, var17 - 60 - 9, 14737632);
      this.font.drawShadow(param0, var23, param2 + var12 - this.font.width(var23), var17 - 60 - 9, 14737632);
      RenderSystem.enableDepthTest();
   }

   private int getSampleColor(int param0, int param1, int param2, int param3) {
      return param0 < param2
         ? this.colorLerp(-16711936, -256, (float)param0 / param2)
         : this.colorLerp(-256, -65536, (float)(param0 - param2) / (param3 - param2));
   }

   private int colorLerp(int param0, int param1, float param2) {
      int var4 = param0 >> 24 & 0xFF;
      int var5 = param0 >> 16 & 0xFF;
      int var6 = param0 >> 8 & 0xFF;
      int var7 = param0 & 0xFF;
      int var8 = param1 >> 24 & 0xFF;
      int var9 = param1 >> 16 & 0xFF;
      int var10 = param1 >> 8 & 0xFF;
      int var11 = param1 & 0xFF;
      int var12 = Mth.clamp((int)Mth.lerp(param2, var4, var8), 0, 255);
      int var13 = Mth.clamp((int)Mth.lerp(param2, var5, var9), 0, 255);
      int var14 = Mth.clamp((int)Mth.lerp(param2, var6, var10), 0, 255);
      int var15 = Mth.clamp((int)Mth.lerp(param2, var7, var11), 0, 255);
      return var12 << 24 | var13 << 16 | var14 << 8 | var15;
   }

   private static long bytesToMegabytes(long param0) {
      return param0 / 1024L / 1024L;
   }

   /**
    * Port note (no {@code java.lang.management}): gone.
    *
    * <p>This summed {@code getCollectionCount()} over
    * {@code ManagementFactory.getGarbageCollectorMXBeans()}, and only to decide whether to
    * recompute the allocation rate or keep showing the previous one -- an MXBean query whose
    * answer changes only when a collection has actually happened.
    *
    * <p>{@code java.lang.management} is not partly absent from the classlib, it is absent
    * outright: no {@code ManagementFactory}, no {@code GarbageCollectorMXBean}. The other four
    * places the game used that package are all dedicated-server code
    * ({@code MinecraftServer}, {@code ServerWatchdog}, {@code MinecraftServerStatistics}) or
    * were already ported ({@code Util}, which read {@code getRuntimeMXBean()} for the launch
    * arguments), so this was the only one on the client's path and it is gone rather than
    * shimmed. Even shimmed it would have had nothing to return: WebAssembly.GC exposes no
    * collection count to Java, so there is no value behind the accessor.
    *
    * <p>With the test gone the recompute is unconditional, which means the F3 line updates
    * every {@link #UPDATE_INTERVAL_MS} rather than only on a collection boundary. The number
    * is the same either way -- it was always computed from the heap delta the caller passes in
    * -- so this reads more responsively, not differently.
    */
   static class AllocationRateCalculator {
      private static final int UPDATE_INTERVAL_MS = 500;
      private long lastTime = 0L;
      private long lastHeapUsage = -1L;
      private long lastRate = 0L;

      long bytesAllocatedPerSecond(long param0) {
         long var3 = System.currentTimeMillis();
         if (var3 - this.lastTime < 500L) {
            return this.lastRate;
         }

         double var7 = (double)TimeUnit.SECONDS.toMillis(1L) / (var3 - this.lastTime);
         long var9 = param0 - this.lastHeapUsage;
         this.lastRate = Math.round(var9 * var7);
         this.lastTime = var3;
         this.lastHeapUsage = param0;
         return this.lastRate;
      }
   }
}
