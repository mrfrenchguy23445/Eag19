package net.minecraft.client;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.internal.PlatformRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

public class Options {
   static final Logger LOGGER = LogUtils.getLogger();
   static final Gson GSON = new Gson();
   // Port note: upstream writes new TypeToken<List<String>>() {}, which recovers its type argument
   // from Class.getGenericSuperclass(). That method is absent from this target's TeaVM classlib, so
   // the anonymous form cannot work; the explicit factory carries the same information.
   // See com.google.gson.reflect.TypeToken.
   private static final TypeToken<List<String>> RESOURCE_PACK_TYPE = TypeToken.getParameterized(List.class, String.class);
   public static final int RENDER_DISTANCE_TINY = 2;
   public static final int RENDER_DISTANCE_SHORT = 4;
   public static final int RENDER_DISTANCE_NORMAL = 8;
   public static final int RENDER_DISTANCE_FAR = 12;
   public static final int RENDER_DISTANCE_REALLY_FAR = 16;
   public static final int RENDER_DISTANCE_EXTREME = 32;
   private static final Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);
   private static final float DEFAULT_VOLUME = 1.0F;
   public static final String DEFAULT_SOUND_DEVICE = "";
   private static final Component ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND = Component.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
   private final OptionInstance<Boolean> darkMojangStudiosBackground = OptionInstance.createBoolean(
      "options.darkMojangStudiosBackgroundColor", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND), false
   );
   private static final Component ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES = Component.translatable("options.hideLightningFlashes.tooltip");
   private final OptionInstance<Boolean> hideLightningFlash = OptionInstance.createBoolean(
      "options.hideLightningFlashes", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES), false
   );
   private final OptionInstance<Double> sensitivity = new OptionInstance<>("options.sensitivity", OptionInstance.noTooltip(), (l_param0, l_param1) -> {
      if (l_param1 == 0.0) {
         return genericValueLabel(l_param0, Component.translatable("options.sensitivity.min"));
      } else {
         return l_param1 == 1.0 ? genericValueLabel(l_param0, Component.translatable("options.sensitivity.max")) : percentValueLabel(l_param0, 2.0 * l_param1);
      }
   }, OptionInstance.UnitDouble.INSTANCE, 0.5, l_param0 -> {});
   private final OptionInstance<Integer> renderDistance;
   private final OptionInstance<Integer> simulationDistance;
   private int serverRenderDistance = 0;
   private final OptionInstance<Double> entityDistanceScaling = new OptionInstance<>(
      "options.entityDistanceScaling",
      OptionInstance.noTooltip(),
      Options::percentValueLabel,
      new OptionInstance.IntRange(2, 20).xmap(l_param0 -> l_param0 / 4.0, l_param0 -> (int)(l_param0 * 4.0)),
      Codec.doubleRange(0.5, 5.0),
      1.0,
      l_param0 -> {}
   );
   public static final int UNLIMITED_FRAMERATE_CUTOFF = 260;
   private final OptionInstance<Integer> framerateLimit = new OptionInstance<>(
      "options.framerateLimit",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 == 260
         ? genericValueLabel(l_param0, Component.translatable("options.framerateLimit.max"))
         : genericValueLabel(l_param0, Component.translatable("options.framerate", l_param1)),
      new OptionInstance.IntRange(1, 26).xmap(l_param0 -> l_param0 * 10, l_param0 -> l_param0 / 10),
      Codec.intRange(10, 260),
      120,
      l_param0 -> Minecraft.getInstance().getWindow().setFramerateLimit(l_param0)
   );
   private final OptionInstance<CloudStatus> cloudStatus = new OptionInstance<>(
      "options.renderClouds",
      OptionInstance.noTooltip(),
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(
         Arrays.asList(CloudStatus.values()),
         Codec.either(Codec.BOOL, Codec.STRING).xmap(l_param0 -> l_param0.map(l2_param0 -> l2_param0 ? CloudStatus.FANCY : CloudStatus.OFF, l2_param0 -> {
            return switch (l2_param0) {
               case "true" -> CloudStatus.FANCY;
               case "fast" -> CloudStatus.FAST;
               default -> CloudStatus.OFF;
            };
         }), l_param0 -> {
            return Either.right(switch (l_param0) {
               case FANCY -> "true";
               case FAST -> "fast";
               case OFF -> "false";
            });
         })
      ),
      CloudStatus.FANCY,
      l_param0 -> {
         if (Minecraft.useShaderTransparency()) {
            RenderTarget l_var1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
            if (l_var1 != null) {
               l_var1.clear(Minecraft.ON_OSX);
            }
         }
      }
   );
   private static final Component GRAPHICS_TOOLTIP_FAST = Component.translatable("options.graphics.fast.tooltip");
   private static final Component GRAPHICS_TOOLTIP_FABULOUS = Component.translatable(
      "options.graphics.fabulous.tooltip", Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC)
   );
   private static final Component GRAPHICS_TOOLTIP_FANCY = Component.translatable("options.graphics.fancy.tooltip");
   private final OptionInstance<GraphicsStatus> graphicsMode = new OptionInstance<>(
      "options.graphics",
      l_param0 -> {
         return switch (l_param0) {
            case FANCY -> Tooltip.create(GRAPHICS_TOOLTIP_FANCY);
            case FAST -> Tooltip.create(GRAPHICS_TOOLTIP_FAST);
            case FABULOUS -> Tooltip.create(GRAPHICS_TOOLTIP_FABULOUS);
         };
      },
      (l_param0, l_param1) -> {
         MutableComponent l_var2 = Component.translatable(l_param1.getKey());
         return l_param1 == GraphicsStatus.FABULOUS ? l_var2.withStyle(ChatFormatting.ITALIC) : l_var2;
      },
      new OptionInstance.AltEnum<>(
         Arrays.asList(GraphicsStatus.values()),
         Stream.of(GraphicsStatus.values()).filter(l_param0 -> l_param0 != GraphicsStatus.FABULOUS).collect(Collectors.toList()),
         () -> Minecraft.getInstance().isRunning() && Minecraft.getInstance().getGpuWarnlistManager().isSkippingFabulous(),
         (l_param0, l_param1) -> {
            Minecraft l_var2 = Minecraft.getInstance();
            GpuWarnlistManager l_var3 = l_var2.getGpuWarnlistManager();
            if (l_param1 == GraphicsStatus.FABULOUS && l_var3.willShowWarning()) {
               l_var3.showWarning();
            } else {
               l_param0.set(l_param1);
               l_var2.levelRenderer.allChanged();
            }
         },
         Codec.INT.xmap(GraphicsStatus::byId, GraphicsStatus::getId)
      ),
      GraphicsStatus.FAST,
      l_param0 -> {}
   );
   private final OptionInstance<Boolean> ambientOcclusion = OptionInstance.createBoolean(
      "options.ao", false, l_param0 -> Minecraft.getInstance().levelRenderer.allChanged()
   );
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_NONE = Component.translatable("options.prioritizeChunkUpdates.none.tooltip");
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED = Component.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
   private static final Component PRIORITIZE_CHUNK_TOOLTIP_NEARBY = Component.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
   private final OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates = new OptionInstance<>(
      "options.prioritizeChunkUpdates",
      l_param0 -> {
         return switch (l_param0) {
            case NONE -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NONE);
            case PLAYER_AFFECTED -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED);
            case NEARBY -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NEARBY);
         };
      },
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(Arrays.asList(PrioritizeChunkUpdates.values()), Codec.INT.xmap(PrioritizeChunkUpdates::byId, PrioritizeChunkUpdates::getId)),
      PrioritizeChunkUpdates.NONE,
      l_param0 -> {}
   );
   public List<String> resourcePacks = Lists.newArrayList();
   public List<String> incompatibleResourcePacks = Lists.newArrayList();
   private final OptionInstance<ChatVisiblity> chatVisibility = new OptionInstance<>(
      "options.chat.visibility",
      OptionInstance.noTooltip(),
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(Arrays.asList(ChatVisiblity.values()), Codec.INT.xmap(ChatVisiblity::byId, ChatVisiblity::getId)),
      ChatVisiblity.FULL,
      l_param0 -> {}
   );
   private final OptionInstance<Double> chatOpacity = new OptionInstance<>(
      "options.chat.opacity",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> percentValueLabel(l_param0, l_param1 * 0.9 + 0.1),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> chatLineSpacing = new OptionInstance<>(
      "options.chat.line_spacing", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.0, l_param0 -> {}
   );
   private final OptionInstance<Double> textBackgroundOpacity = new OptionInstance<>(
      "options.accessibility.text_background_opacity",
      OptionInstance.noTooltip(),
      Options::percentValueLabel,
      OptionInstance.UnitDouble.INSTANCE,
      0.5,
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> panoramaSpeed = new OptionInstance<>(
      "options.accessibility.panorama_speed", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_CONTRAST_MODE = Component.translatable("options.accessibility.high_contrast.tooltip");
   private final OptionInstance<Boolean> highContrast = OptionInstance.createBoolean(
      "options.accessibility.high_contrast", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_CONTRAST_MODE), false, l_param0 -> {
         PackRepository l_var2 = Minecraft.getInstance().getResourcePackRepository();
         boolean l_var3 = l_var2.getSelectedIds().contains("high_contrast");
         if (!l_var3 && l_param0) {
            if (l_var2.addPack("high_contrast")) {
               this.updateResourcePacks(l_var2);
            }
         } else if (l_var3 && !l_param0 && l_var2.removePack("high_contrast")) {
            this.updateResourcePacks(l_var2);
         }
      }
   );
   @Nullable
   public String fullscreenVideoModeString;
   public boolean hideServerAddress;
   public boolean advancedItemTooltips;
   public boolean pauseOnLostFocus = true;
   private final Set<PlayerModelPart> modelParts = EnumSet.allOf(PlayerModelPart.class);
   private final OptionInstance<HumanoidArm> mainHand = new OptionInstance<>(
      "options.mainHand",
      OptionInstance.noTooltip(),
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(
         Arrays.asList(HumanoidArm.values()),
         Codec.STRING
            .xmap(l_param0 -> "left".equals(l_param0) ? HumanoidArm.LEFT : HumanoidArm.RIGHT, l_param0 -> l_param0 == HumanoidArm.LEFT ? "left" : "right")
      ),
      HumanoidArm.RIGHT,
      l_param0 -> this.broadcastOptions()
   );
   public int overrideWidth;
   public int overrideHeight;
   private final OptionInstance<Double> chatScale = new OptionInstance<>(
      "options.chat.scale",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 == 0.0 ? CommonComponents.optionStatus(l_param0, false) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> chatWidth = new OptionInstance<>(
      "options.chat.width",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> pixelValueLabel(l_param0, ChatComponent.getWidth(l_param1)),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> chatHeightUnfocused = new OptionInstance<>(
      "options.chat.height.unfocused",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> pixelValueLabel(l_param0, ChatComponent.getHeight(l_param1)),
      OptionInstance.UnitDouble.INSTANCE,
      ChatComponent.defaultUnfocusedPct(),
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> chatHeightFocused = new OptionInstance<>(
      "options.chat.height.focused",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> pixelValueLabel(l_param0, ChatComponent.getHeight(l_param1)),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> Minecraft.getInstance().gui.getChat().rescaleChat()
   );
   private final OptionInstance<Double> chatDelay = new OptionInstance<>(
      "options.chat.delay_instant",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 <= 0.0
         ? Component.translatable("options.chat.delay_none")
         : Component.translatable("options.chat.delay", String.format(Locale.ROOT, "%.1f", l_param1)),
      new OptionInstance.IntRange(0, 60).xmap(l_param0 -> l_param0 / 10.0, l_param0 -> (int)(l_param0 * 10.0)),
      Codec.doubleRange(0.0, 6.0),
      0.0,
      l_param0 -> Minecraft.getInstance().getChatListener().setMessageDelay(l_param0)
   );
   private static final Component ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME = Component.translatable("options.notifications.display_time.tooltip");
   private final OptionInstance<Double> notificationDisplayTime = new OptionInstance<>(
      "options.notifications.display_time",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME),
      (l_param0, l_param1) -> genericValueLabel(l_param0, Component.translatable("options.multiplier", l_param1)),
      new OptionInstance.IntRange(5, 100).xmap(l_param0 -> l_param0 / 10.0, l_param0 -> (int)(l_param0 * 10.0)),
      Codec.doubleRange(0.5, 10.0),
      1.0,
      l_param0 -> {}
   );
   private final OptionInstance<Integer> mipmapLevels = new OptionInstance<>(
      "options.mipmapLevels",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 == 0 ? CommonComponents.optionStatus(l_param0, false) : genericValueLabel(l_param0, l_param1),
      new OptionInstance.IntRange(0, 4),
      4,
      l_param0 -> {}
   );
   public boolean useNativeTransport = true;
   private final OptionInstance<AttackIndicatorStatus> attackIndicator = new OptionInstance<>(
      "options.attackIndicator",
      OptionInstance.noTooltip(),
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(Arrays.asList(AttackIndicatorStatus.values()), Codec.INT.xmap(AttackIndicatorStatus::byId, AttackIndicatorStatus::getId)),
      AttackIndicatorStatus.CROSSHAIR,
      l_param0 -> {}
   );
   public TutorialSteps tutorialStep = TutorialSteps.MOVEMENT;
   public boolean joinedFirstServer = false;
   public boolean hideBundleTutorial = false;
   private final OptionInstance<Integer> biomeBlendRadius = new OptionInstance<>(
      "options.biomeBlendRadius", OptionInstance.noTooltip(), (l_param0, l_param1) -> {
         int l_var2 = l_param1 * 2 + 1;
         return genericValueLabel(l_param0, Component.translatable("options.biomeBlendRadius." + l_var2));
      }, new OptionInstance.IntRange(0, 7), 0, l_param0 -> Minecraft.getInstance().levelRenderer.allChanged()
   );
   private final OptionInstance<Double> mouseWheelSensitivity = new OptionInstance<>(
      "options.mouseWheelSensitivity",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> genericValueLabel(l_param0, Component.literal(String.format(Locale.ROOT, "%.2f", l_param1))),
      new OptionInstance.IntRange(-200, 100).xmap(Options::logMouse, Options::unlogMouse),
      Codec.doubleRange(logMouse(-200), logMouse(100)),
      logMouse(0),
      l_param0 -> {}
   );
   private final OptionInstance<Boolean> rawMouseInput = OptionInstance.createBoolean("options.rawMouseInput", true, l_param0 -> {
      Window l_var1 = Minecraft.getInstance().getWindow();
      if (l_var1 != null) {
         l_var1.updateRawMouseInput(l_param0);
      }
   });
   public int glDebugVerbosity = 1;
   private final OptionInstance<Boolean> autoJump = OptionInstance.createBoolean("options.autoJump", false);
   private final OptionInstance<Boolean> operatorItemsTab = OptionInstance.createBoolean("options.operatorItemsTab", false);
   private final OptionInstance<Boolean> autoSuggestions = OptionInstance.createBoolean("options.autoSuggestCommands", true);
   private final OptionInstance<Boolean> chatColors = OptionInstance.createBoolean("options.chat.color", true);
   private final OptionInstance<Boolean> chatLinks = OptionInstance.createBoolean("options.chat.links", true);
   private final OptionInstance<Boolean> chatLinksPrompt = OptionInstance.createBoolean("options.chat.links.prompt", true);
   private final OptionInstance<Boolean> enableVsync = OptionInstance.createBoolean("options.vsync", true, l_param0 -> {
      if (Minecraft.getInstance().getWindow() != null) {
         Minecraft.getInstance().getWindow().updateVsync(l_param0);
      }
   });
   private final OptionInstance<Boolean> entityShadows = OptionInstance.createBoolean("options.entityShadows", false);
   private final OptionInstance<Boolean> forceUnicodeFont = OptionInstance.createBoolean("options.forceUnicodeFont", false, l_param0 -> {
      Minecraft l_var1 = Minecraft.getInstance();
      if (l_var1.getWindow() != null) {
         l_var1.selectMainFont(l_param0);
         l_var1.resizeDisplay();
      }
   });
   private final OptionInstance<Boolean> invertYMouse = OptionInstance.createBoolean("options.invertMouse", false);
   private final OptionInstance<Boolean> discreteMouseScroll = OptionInstance.createBoolean("options.discrete_mouse_scroll", false);
   private final OptionInstance<Boolean> realmsNotifications = OptionInstance.createBoolean("options.realmsNotifications", true);
   private static final Component ALLOW_SERVER_LISTING_TOOLTIP = Component.translatable("options.allowServerListing.tooltip");
   private final OptionInstance<Boolean> allowServerListing = OptionInstance.createBoolean(
      "options.allowServerListing", OptionInstance.cachedConstantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, l_param0 -> this.broadcastOptions()
   );
   private final OptionInstance<Boolean> reducedDebugInfo = OptionInstance.createBoolean("options.reducedDebugInfo", false);
   private final Map<SoundSource, OptionInstance<Double>> soundSourceVolumes = Util.make(new EnumMap<>(SoundSource.class), l_param0 -> {
      for (SoundSource l_var5 : SoundSource.values()) {
         l_param0.put(l_var5, this.createSoundSliderOptionInstance("soundCategory." + l_var5.getName(), l_var5));
      }
   });
   private final OptionInstance<Boolean> showSubtitles = OptionInstance.createBoolean("options.showSubtitles", false);
   private static final Component DIRECTIONAL_AUDIO_TOOLTIP_ON = Component.translatable("options.directionalAudio.on.tooltip");
   private static final Component DIRECTIONAL_AUDIO_TOOLTIP_OFF = Component.translatable("options.directionalAudio.off.tooltip");
   private final OptionInstance<Boolean> directionalAudio = OptionInstance.createBoolean(
      "options.directionalAudio",
      l_param0 -> l_param0 ? Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_ON) : Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_OFF),
      false,
      l_param0 -> {
         SoundManager l_var1 = Minecraft.getInstance().getSoundManager();
         l_var1.reload();
         l_var1.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   );
   private final OptionInstance<Boolean> backgroundForChatOnly = new OptionInstance<>(
      "options.accessibility.text_background",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1
         ? Component.translatable("options.accessibility.text_background.chat")
         : Component.translatable("options.accessibility.text_background.everywhere"),
      OptionInstance.BOOLEAN_VALUES,
      true,
      l_param0 -> {}
   );
   private final OptionInstance<Boolean> touchscreen = OptionInstance.createBoolean("options.touchscreen", false);
   private final OptionInstance<Boolean> fullscreen = OptionInstance.createBoolean("options.fullscreen", false, l_param0 -> {
      Minecraft l_var2 = Minecraft.getInstance();
      if (l_var2.getWindow() != null && l_var2.getWindow().isFullscreen() != l_param0) {
         l_var2.getWindow().toggleFullScreen();
         this.fullscreen().set(l_var2.getWindow().isFullscreen());
      }
   });
   private final OptionInstance<Boolean> bobView = OptionInstance.createBoolean("options.viewBobbing", true);
   private static final Component MOVEMENT_TOGGLE = Component.translatable("options.key.toggle");
   private static final Component MOVEMENT_HOLD = Component.translatable("options.key.hold");
   private final OptionInstance<Boolean> toggleCrouch = new OptionInstance<>(
      "key.sneak",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 ? MOVEMENT_TOGGLE : MOVEMENT_HOLD,
      OptionInstance.BOOLEAN_VALUES,
      false,
      l_param0 -> {}
   );
   private final OptionInstance<Boolean> toggleSprint = new OptionInstance<>(
      "key.sprint",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 ? MOVEMENT_TOGGLE : MOVEMENT_HOLD,
      OptionInstance.BOOLEAN_VALUES,
      false,
      l_param0 -> {}
   );
   public boolean skipMultiplayerWarning;
   public boolean skipRealms32bitWarning;
   private static final Component CHAT_TOOLTIP_HIDE_MATCHED_NAMES = Component.translatable("options.hideMatchedNames.tooltip");
   private final OptionInstance<Boolean> hideMatchedNames = OptionInstance.createBoolean(
      "options.hideMatchedNames", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_HIDE_MATCHED_NAMES), true
   );
   private final OptionInstance<Boolean> showAutosaveIndicator = OptionInstance.createBoolean("options.autosaveIndicator", true);
   private static final Component CHAT_TOOLTIP_ONLY_SHOW_SECURE = Component.translatable("options.onlyShowSecureChat.tooltip");
   private final OptionInstance<Boolean> onlyShowSecureChat = OptionInstance.createBoolean(
      "options.onlyShowSecureChat", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_ONLY_SHOW_SECURE), false
   );
   public final KeyMapping keyUp = new KeyMapping("key.forward", 87, "key.categories.movement");
   public final KeyMapping keyLeft = new KeyMapping("key.left", 65, "key.categories.movement");
   public final KeyMapping keyDown = new KeyMapping("key.back", 83, "key.categories.movement");
   public final KeyMapping keyRight = new KeyMapping("key.right", 68, "key.categories.movement");
   public final KeyMapping keyJump = new KeyMapping("key.jump", 32, "key.categories.movement");
   public final KeyMapping keyShift = new ToggleKeyMapping("key.sneak", 340, "key.categories.movement", this.toggleCrouch::get);
   public final KeyMapping keySprint = new ToggleKeyMapping("key.sprint", 341, "key.categories.movement", this.toggleSprint::get);
   public final KeyMapping keyInventory = new KeyMapping("key.inventory", 69, "key.categories.inventory");
   public final KeyMapping keySwapOffhand = new KeyMapping("key.swapOffhand", 70, "key.categories.inventory");
   public final KeyMapping keyDrop = new KeyMapping("key.drop", 81, "key.categories.inventory");
   public final KeyMapping keyUse = new KeyMapping("key.use", InputConstants.Type.MOUSE, 1, "key.categories.gameplay");
   public final KeyMapping keyAttack = new KeyMapping("key.attack", InputConstants.Type.MOUSE, 0, "key.categories.gameplay");
   public final KeyMapping keyPickItem = new KeyMapping("key.pickItem", InputConstants.Type.MOUSE, 2, "key.categories.gameplay");
   public final KeyMapping keyChat = new KeyMapping("key.chat", 84, "key.categories.multiplayer");
   public final KeyMapping keyPlayerList = new KeyMapping("key.playerlist", 258, "key.categories.multiplayer");
   public final KeyMapping keyCommand = new KeyMapping("key.command", 47, "key.categories.multiplayer");
   public final KeyMapping keySocialInteractions = new KeyMapping("key.socialInteractions", 80, "key.categories.multiplayer");
   public final KeyMapping keyScreenshot = new KeyMapping("key.screenshot", 291, "key.categories.misc");
   public final KeyMapping keyTogglePerspective = new KeyMapping("key.togglePerspective", 294, "key.categories.misc");
   public final KeyMapping keySmoothCamera = new KeyMapping("key.smoothCamera", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
   public final KeyMapping keyFullscreen = new KeyMapping("key.fullscreen", 300, "key.categories.misc");
   public final KeyMapping keySpectatorOutlines = new KeyMapping("key.spectatorOutlines", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
   public final KeyMapping keyAdvancements = new KeyMapping("key.advancements", 76, "key.categories.misc");
   public final KeyMapping[] keyHotbarSlots = new KeyMapping[]{
      new KeyMapping("key.hotbar.1", 49, "key.categories.inventory"),
      new KeyMapping("key.hotbar.2", 50, "key.categories.inventory"),
      new KeyMapping("key.hotbar.3", 51, "key.categories.inventory"),
      new KeyMapping("key.hotbar.4", 52, "key.categories.inventory"),
      new KeyMapping("key.hotbar.5", 53, "key.categories.inventory"),
      new KeyMapping("key.hotbar.6", 54, "key.categories.inventory"),
      new KeyMapping("key.hotbar.7", 55, "key.categories.inventory"),
      new KeyMapping("key.hotbar.8", 56, "key.categories.inventory"),
      new KeyMapping("key.hotbar.9", 57, "key.categories.inventory")
   };
   public final KeyMapping keySaveHotbarActivator = new KeyMapping("key.saveToolbarActivator", 67, "key.categories.creative");
   public final KeyMapping keyLoadHotbarActivator = new KeyMapping("key.loadToolbarActivator", 88, "key.categories.creative");
   public final KeyMapping[] keyMappings = ArrayUtils.addAll(
      new KeyMapping[]{
         this.keyAttack,
         this.keyUse,
         this.keyUp,
         this.keyLeft,
         this.keyDown,
         this.keyRight,
         this.keyJump,
         this.keyShift,
         this.keySprint,
         this.keyDrop,
         this.keyInventory,
         this.keyChat,
         this.keyPlayerList,
         this.keyPickItem,
         this.keyCommand,
         this.keySocialInteractions,
         this.keyScreenshot,
         this.keyTogglePerspective,
         this.keySmoothCamera,
         this.keyFullscreen,
         this.keySpectatorOutlines,
         this.keySwapOffhand,
         this.keySaveHotbarActivator,
         this.keyLoadHotbarActivator,
         this.keyAdvancements
      },
      this.keyHotbarSlots
   );
   protected Minecraft minecraft;
   private final File optionsFile;
   public boolean hideGui;
   private CameraType cameraType = CameraType.FIRST_PERSON;
   public boolean renderDebug;
   public boolean renderDebugCharts;
   public boolean renderFpsChart;
   public String lastMpIp = "";
   public boolean smoothCamera;
   private final OptionInstance<Integer> fov = new OptionInstance<>(
      "options.fov",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> {
         return switch (l_param1) {
            case 70 -> genericValueLabel(l_param0, Component.translatable("options.fov.min"));
            case 110 -> genericValueLabel(l_param0, Component.translatable("options.fov.max"));
            default -> genericValueLabel(l_param0, l_param1);
         };
      },
      new OptionInstance.IntRange(30, 110),
      Codec.DOUBLE.xmap(l_param0 -> (int)(l_param0 * 40.0 + 70.0), l_param0 -> (l_param0.intValue() - 70.0) / 40.0),
      70,
      l_param0 -> Minecraft.getInstance().levelRenderer.needsUpdate()
   );
   private static final MutableComponent TELEMETRY_TOOLTIP = Component.translatable(
      "options.telemetry.button.tooltip", Component.translatable("options.telemetry.state.minimal"), Component.translatable("options.telemetry.state.all")
   );
   private final OptionInstance<Boolean> telemetryOptInExtra = OptionInstance.createBoolean(
      "options.telemetry.button",
      OptionInstance.cachedConstantTooltip(TELEMETRY_TOOLTIP),
      (l_param0, l_param1) -> {
         Minecraft l_var2 = Minecraft.getInstance();
         if (!l_var2.allowsTelemetry()) {
            return Component.translatable("options.telemetry.state.none");
         } else {
            return l_param1 && l_var2.extraTelemetryAvailable()
               ? Component.translatable("options.telemetry.state.all")
               : Component.translatable("options.telemetry.state.minimal");
         }
      },
      false,
      l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT = Component.translatable("options.screenEffectScale.tooltip");
   private final OptionInstance<Double> screenEffectScale = new OptionInstance<>(
      "options.screenEffectScale",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_FOV_EFFECT = Component.translatable("options.fovEffectScale.tooltip");
   private final OptionInstance<Double> fovEffectScale = new OptionInstance<>(
      "options.fovEffectScale",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_FOV_EFFECT),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
      Codec.doubleRange(0.0, 1.0),
      1.0,
      l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT = Component.translatable("options.darknessEffectScale.tooltip");
   private final OptionInstance<Double> darknessEffectScale = new OptionInstance<>(
      "options.darknessEffectScale",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
      1.0,
      l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_GLINT_SPEED = Component.translatable("options.glintSpeed.tooltip");
   private final OptionInstance<Double> glintSpeed = new OptionInstance<>(
      "options.glintSpeed",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_SPEED),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE,
      0.5,
      l_param0 -> {}
   );
   private static final Component ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH = Component.translatable("options.glintStrength.tooltip");
   private final OptionInstance<Double> glintStrength = new OptionInstance<>(
      "options.glintStrength",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE,
      0.75,
      RenderSystem::setShaderGlintAlpha
   );
   private static final Component ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH = Component.translatable("options.damageTiltStrength.tooltip");
   private final OptionInstance<Double> damageTiltStrength = new OptionInstance<>(
      "options.damageTiltStrength",
      OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH),
      (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
      OptionInstance.UnitDouble.INSTANCE,
      1.0,
      l_param0 -> {}
   );
   private final OptionInstance<Double> gamma = new OptionInstance<>("options.gamma", OptionInstance.noTooltip(), (l_param0, l_param1) -> {
      int l_var2 = (int)(l_param1 * 100.0);
      if (l_var2 == 0) {
         return genericValueLabel(l_param0, Component.translatable("options.gamma.min"));
      } else if (l_var2 == 50) {
         return genericValueLabel(l_param0, Component.translatable("options.gamma.default"));
      } else {
         return l_var2 == 100 ? genericValueLabel(l_param0, Component.translatable("options.gamma.max")) : genericValueLabel(l_param0, l_var2);
      }
   }, OptionInstance.UnitDouble.INSTANCE, 0.5, l_param0 -> {});
   private static final int MAX_GUI_SCALE_INCLUSIVE = 2147483646;
   private final OptionInstance<Integer> guiScale = new OptionInstance<>(
      "options.guiScale",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> l_param1 == 0 ? Component.translatable("options.guiScale.auto") : Component.literal(Integer.toString(l_param1)),
      new OptionInstance.ClampingLazyMaxIntRange(0, () -> {
         Minecraft l_var0 = Minecraft.getInstance();
         return !l_var0.isRunning() ? 2147483646 : l_var0.getWindow().calculateScale(0, l_var0.isEnforceUnicode());
      }, 2147483646),
      0,
      l_param0 -> {}
   );
   private final OptionInstance<ParticleStatus> particles = new OptionInstance<>(
      "options.particles",
      OptionInstance.noTooltip(),
      OptionInstance.forOptionEnum(),
      new OptionInstance.Enum<>(Arrays.asList(ParticleStatus.values()), Codec.INT.xmap(ParticleStatus::byId, ParticleStatus::getId)),
      ParticleStatus.DECREASED,
      l_param0 -> {}
   );
   private final OptionInstance<NarratorStatus> narrator = new OptionInstance<>(
      "options.narrator",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> this.minecraft.getNarrator().isActive() ? l_param1.getName() : Component.translatable("options.narrator.notavailable"),
      new OptionInstance.Enum<>(Arrays.asList(NarratorStatus.values()), Codec.INT.xmap(NarratorStatus::byId, NarratorStatus::getId)),
      NarratorStatus.OFF,
      l_param0 -> this.minecraft.getNarrator().updateNarratorStatus(l_param0)
   );
   public String languageCode = "en_us";
   private final OptionInstance<String> soundDevice = new OptionInstance<>(
      "options.audioDevice",
      OptionInstance.noTooltip(),
      (l_param0, l_param1) -> {
         if ("".equals(l_param1)) {
            return Component.translatable("options.audioDevice.default");
         } else {
            return l_param1.startsWith("OpenAL Soft on ")
               ? Component.literal(l_param1.substring(SoundEngine.OPEN_AL_SOFT_PREFIX_LENGTH))
               : Component.literal(l_param1);
         }
      },
      new OptionInstance.LazyEnum<>(
         () -> Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).toList(),
         l_param0 -> Minecraft.getInstance().isRunning()
               && l_param0 != ""
               && !Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().contains(l_param0)
            ? Optional.empty()
            : Optional.of(l_param0),
         Codec.STRING
      ),
      "",
      l_param0 -> {
         SoundManager l_var1 = Minecraft.getInstance().getSoundManager();
         l_var1.reload();
         l_var1.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   );
   // PORT NOTE (Eag19 / TeaVM WASM-GC): the in-browser filesystem is rebuilt from the
   // EPK on every boot, so a "fresh profile" is every launch and this default would make
   // the accessibility onboarding screen, not the title screen, the first screen forever.
   // The port's target is the title screen, so a fresh profile now goes straight there.
   // Set back to true to restore the stock first-run flow.
   public boolean onboardAccessibility = false;
   public boolean syncWrites;

   public OptionInstance<Boolean> darkMojangStudiosBackground() {
      return this.darkMojangStudiosBackground;
   }

   public OptionInstance<Boolean> hideLightningFlash() {
      return this.hideLightningFlash;
   }

   public OptionInstance<Double> sensitivity() {
      return this.sensitivity;
   }

   public OptionInstance<Integer> renderDistance() {
      return this.renderDistance;
   }

   public OptionInstance<Integer> simulationDistance() {
      return this.simulationDistance;
   }

   public OptionInstance<Double> entityDistanceScaling() {
      return this.entityDistanceScaling;
   }

   public OptionInstance<Integer> framerateLimit() {
      return this.framerateLimit;
   }

   public OptionInstance<CloudStatus> cloudStatus() {
      return this.cloudStatus;
   }

   public OptionInstance<GraphicsStatus> graphicsMode() {
      return this.graphicsMode;
   }

   public OptionInstance<Boolean> ambientOcclusion() {
      return this.ambientOcclusion;
   }

   public OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates() {
      return this.prioritizeChunkUpdates;
   }

   public void updateResourcePacks(PackRepository param0) {
      List<String> var2 = ImmutableList.copyOf(this.resourcePacks);
      this.resourcePacks.clear();
      this.incompatibleResourcePacks.clear();

      for (Pack var4 : param0.getSelectedPacks()) {
         if (!var4.isFixedPosition()) {
            this.resourcePacks.add(var4.getId());
            if (!var4.getCompatibility().isCompatible()) {
               this.incompatibleResourcePacks.add(var4.getId());
            }
         }
      }

      this.save();
      List<String> var3 = ImmutableList.copyOf(this.resourcePacks);
      if (!var3.equals(var2)) {
         this.minecraft.reloadResourcePacks();
      }
   }

   public OptionInstance<ChatVisiblity> chatVisibility() {
      return this.chatVisibility;
   }

   public OptionInstance<Double> chatOpacity() {
      return this.chatOpacity;
   }

   public OptionInstance<Double> chatLineSpacing() {
      return this.chatLineSpacing;
   }

   public OptionInstance<Double> textBackgroundOpacity() {
      return this.textBackgroundOpacity;
   }

   public OptionInstance<Double> panoramaSpeed() {
      return this.panoramaSpeed;
   }

   public OptionInstance<Boolean> highContrast() {
      return this.highContrast;
   }

   public OptionInstance<HumanoidArm> mainHand() {
      return this.mainHand;
   }

   public OptionInstance<Double> chatScale() {
      return this.chatScale;
   }

   public OptionInstance<Double> chatWidth() {
      return this.chatWidth;
   }

   public OptionInstance<Double> chatHeightUnfocused() {
      return this.chatHeightUnfocused;
   }

   public OptionInstance<Double> chatHeightFocused() {
      return this.chatHeightFocused;
   }

   public OptionInstance<Double> chatDelay() {
      return this.chatDelay;
   }

   public OptionInstance<Double> notificationDisplayTime() {
      return this.notificationDisplayTime;
   }

   public OptionInstance<Integer> mipmapLevels() {
      return this.mipmapLevels;
   }

   public OptionInstance<AttackIndicatorStatus> attackIndicator() {
      return this.attackIndicator;
   }

   public OptionInstance<Integer> biomeBlendRadius() {
      return this.biomeBlendRadius;
   }

   private static double logMouse(int param0) {
      return Math.pow(10.0, param0 / 100.0);
   }

   private static int unlogMouse(double param0) {
      return Mth.floor(Math.log10(param0) * 100.0);
   }

   public OptionInstance<Double> mouseWheelSensitivity() {
      return this.mouseWheelSensitivity;
   }

   public OptionInstance<Boolean> rawMouseInput() {
      return this.rawMouseInput;
   }

   public OptionInstance<Boolean> autoJump() {
      return this.autoJump;
   }

   public OptionInstance<Boolean> operatorItemsTab() {
      return this.operatorItemsTab;
   }

   public OptionInstance<Boolean> autoSuggestions() {
      return this.autoSuggestions;
   }

   public OptionInstance<Boolean> chatColors() {
      return this.chatColors;
   }

   public OptionInstance<Boolean> chatLinks() {
      return this.chatLinks;
   }

   public OptionInstance<Boolean> chatLinksPrompt() {
      return this.chatLinksPrompt;
   }

   public OptionInstance<Boolean> enableVsync() {
      return this.enableVsync;
   }

   public OptionInstance<Boolean> entityShadows() {
      return this.entityShadows;
   }

   public OptionInstance<Boolean> forceUnicodeFont() {
      return this.forceUnicodeFont;
   }

   public OptionInstance<Boolean> invertYMouse() {
      return this.invertYMouse;
   }

   public OptionInstance<Boolean> discreteMouseScroll() {
      return this.discreteMouseScroll;
   }

   public OptionInstance<Boolean> realmsNotifications() {
      return this.realmsNotifications;
   }

   public OptionInstance<Boolean> allowServerListing() {
      return this.allowServerListing;
   }

   public OptionInstance<Boolean> reducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public final float getSoundSourceVolume(SoundSource param0) {
      return this.getSoundSourceOptionInstance(param0).get().floatValue();
   }

   public final OptionInstance<Double> getSoundSourceOptionInstance(SoundSource param0) {
      return Objects.requireNonNull(this.soundSourceVolumes.get(param0));
   }

   private OptionInstance<Double> createSoundSliderOptionInstance(String param0, SoundSource param1) {
      return new OptionInstance<>(
         param0,
         OptionInstance.noTooltip(),
         (l_param0, l_param1) -> l_param1 == 0.0 ? genericValueLabel(l_param0, CommonComponents.OPTION_OFF) : percentValueLabel(l_param0, l_param1),
         OptionInstance.UnitDouble.INSTANCE,
         1.0,
         l_param1 -> Minecraft.getInstance().getSoundManager().updateSourceVolume(param1, l_param1.floatValue())
      );
   }

   public OptionInstance<Boolean> showSubtitles() {
      return this.showSubtitles;
   }

   public OptionInstance<Boolean> directionalAudio() {
      return this.directionalAudio;
   }

   public OptionInstance<Boolean> backgroundForChatOnly() {
      return this.backgroundForChatOnly;
   }

   public OptionInstance<Boolean> touchscreen() {
      return this.touchscreen;
   }

   public OptionInstance<Boolean> fullscreen() {
      return this.fullscreen;
   }

   public OptionInstance<Boolean> bobView() {
      return this.bobView;
   }

   public OptionInstance<Boolean> toggleCrouch() {
      return this.toggleCrouch;
   }

   public OptionInstance<Boolean> toggleSprint() {
      return this.toggleSprint;
   }

   public OptionInstance<Boolean> hideMatchedNames() {
      return this.hideMatchedNames;
   }

   public OptionInstance<Boolean> showAutosaveIndicator() {
      return this.showAutosaveIndicator;
   }

   public OptionInstance<Boolean> onlyShowSecureChat() {
      return this.onlyShowSecureChat;
   }

   public OptionInstance<Integer> fov() {
      return this.fov;
   }

   public OptionInstance<Boolean> telemetryOptInExtra() {
      return this.telemetryOptInExtra;
   }

   public OptionInstance<Double> screenEffectScale() {
      return this.screenEffectScale;
   }

   public OptionInstance<Double> fovEffectScale() {
      return this.fovEffectScale;
   }

   public OptionInstance<Double> darknessEffectScale() {
      return this.darknessEffectScale;
   }

   public OptionInstance<Double> glintSpeed() {
      return this.glintSpeed;
   }

   public OptionInstance<Double> glintStrength() {
      return this.glintStrength;
   }

   public OptionInstance<Double> damageTiltStrength() {
      return this.damageTiltStrength;
   }

   public OptionInstance<Double> gamma() {
      return this.gamma;
   }

   public OptionInstance<Integer> guiScale() {
      return this.guiScale;
   }

   public OptionInstance<ParticleStatus> particles() {
      return this.particles;
   }

   public OptionInstance<NarratorStatus> narrator() {
      return this.narrator;
   }

   public OptionInstance<String> soundDevice() {
      return this.soundDevice;
   }

   public Options(Minecraft param0, File param1) {
      this.minecraft = param0;
      this.optionsFile = new File(param1, "options.txt");
      boolean var3 = param0.is64Bit();
   // Runtime.getRuntime().maxMemory() is absent from the WASM-GC TeaVM classlib. The
   // replacement is PlatformRuntime.maxMemory(), the same seam SystemReport's "Memory"
   // detail already uses, and the same fixed figure it reports: a WASM-GC module has one
   // fixed WebAssembly.Memory, so the heap ceiling and the heap size are one number rather
   // than a limit and a reading. See SystemReport's constructor for the full note.
      boolean var4 = var3 && PlatformRuntime.maxMemory() >= 1000000000L;
      this.renderDistance = new OptionInstance<>(
         "options.renderDistance",
         OptionInstance.noTooltip(),
         (l_param0, l_param1) -> genericValueLabel(l_param0, Component.translatable("options.chunks", l_param1)),
         new OptionInstance.IntRange(2, var4 ? 32 : 16),
         // Cooperative-port perf default: TeaVM worlds cost ~2s of main-thread work per
         // generated chunk, so vanilla's 6-8 chunk render distance folds the client in
         // chunk-gen lag. Default to 4.
         4,
         l_param0 -> Minecraft.getInstance().levelRenderer.needsUpdate()
      );
      this.simulationDistance = new OptionInstance<>(
         "options.simulationDistance",
         OptionInstance.noTooltip(),
         (l_param0, l_param1) -> genericValueLabel(l_param0, Component.translatable("options.chunks", l_param1)),
         new OptionInstance.IntRange(5, var4 ? 32 : 16),
         var3 ? 6 : 5,
         l_param0 -> {}
      );
      this.syncWrites = Util.getPlatform() == Util.OS.WINDOWS;
      this.load();
   }

   public float getBackgroundOpacity(float param0) {
      return this.backgroundForChatOnly.get() ? param0 : this.textBackgroundOpacity().get().floatValue();
   }

   public int getBackgroundColor(float param0) {
      return (int)(this.getBackgroundOpacity(param0) * 255.0F) << 24 & 0xFF000000;
   }

   public int getBackgroundColor(int param0) {
      return this.backgroundForChatOnly.get() ? param0 : (int)(this.textBackgroundOpacity.get() * 255.0) << 24 & 0xFF000000;
   }

   public void setKey(KeyMapping param0, InputConstants.Key param1) {
      param0.setKey(param1);
      this.save();
   }

   private void processOptions(Options.FieldAccess param0) {
      param0.process("autoJump", this.autoJump);
      param0.process("operatorItemsTab", this.operatorItemsTab);
      param0.process("autoSuggestions", this.autoSuggestions);
      param0.process("chatColors", this.chatColors);
      param0.process("chatLinks", this.chatLinks);
      param0.process("chatLinksPrompt", this.chatLinksPrompt);
      param0.process("enableVsync", this.enableVsync);
      param0.process("entityShadows", this.entityShadows);
      param0.process("forceUnicodeFont", this.forceUnicodeFont);
      param0.process("discrete_mouse_scroll", this.discreteMouseScroll);
      param0.process("invertYMouse", this.invertYMouse);
      param0.process("realmsNotifications", this.realmsNotifications);
      param0.process("reducedDebugInfo", this.reducedDebugInfo);
      param0.process("showSubtitles", this.showSubtitles);
      param0.process("directionalAudio", this.directionalAudio);
      param0.process("touchscreen", this.touchscreen);
      param0.process("fullscreen", this.fullscreen);
      param0.process("bobView", this.bobView);
      param0.process("toggleCrouch", this.toggleCrouch);
      param0.process("toggleSprint", this.toggleSprint);
      param0.process("darkMojangStudiosBackground", this.darkMojangStudiosBackground);
      param0.process("hideLightningFlashes", this.hideLightningFlash);
      param0.process("mouseSensitivity", this.sensitivity);
      param0.process("fov", this.fov);
      param0.process("screenEffectScale", this.screenEffectScale);
      param0.process("fovEffectScale", this.fovEffectScale);
      param0.process("darknessEffectScale", this.darknessEffectScale);
      param0.process("glintSpeed", this.glintSpeed);
      param0.process("glintStrength", this.glintStrength);
      param0.process("damageTiltStrength", this.damageTiltStrength);
      param0.process("highContrast", this.highContrast);
      param0.process("gamma", this.gamma);
      param0.process("renderDistance", this.renderDistance);
      param0.process("simulationDistance", this.simulationDistance);
      param0.process("entityDistanceScaling", this.entityDistanceScaling);
      param0.process("guiScale", this.guiScale);
      param0.process("particles", this.particles);
      param0.process("maxFps", this.framerateLimit);
      param0.process("graphicsMode", this.graphicsMode);
      param0.process("ao", this.ambientOcclusion);
      param0.process("prioritizeChunkUpdates", this.prioritizeChunkUpdates);
      param0.process("biomeBlendRadius", this.biomeBlendRadius);
      param0.process("renderClouds", this.cloudStatus);
      this.resourcePacks = param0.process("resourcePacks", this.resourcePacks, Options::readPackList, GSON::toJson);
      this.incompatibleResourcePacks = param0.process("incompatibleResourcePacks", this.incompatibleResourcePacks, Options::readPackList, GSON::toJson);
      this.lastMpIp = param0.process("lastServer", this.lastMpIp);
      this.languageCode = param0.process("lang", this.languageCode);
      param0.process("soundDevice", this.soundDevice);
      param0.process("chatVisibility", this.chatVisibility);
      param0.process("chatOpacity", this.chatOpacity);
      param0.process("chatLineSpacing", this.chatLineSpacing);
      param0.process("textBackgroundOpacity", this.textBackgroundOpacity);
      param0.process("backgroundForChatOnly", this.backgroundForChatOnly);
      this.hideServerAddress = param0.process("hideServerAddress", this.hideServerAddress);
      this.advancedItemTooltips = param0.process("advancedItemTooltips", this.advancedItemTooltips);
      this.pauseOnLostFocus = param0.process("pauseOnLostFocus", this.pauseOnLostFocus);
      this.overrideWidth = param0.process("overrideWidth", this.overrideWidth);
      this.overrideHeight = param0.process("overrideHeight", this.overrideHeight);
      param0.process("chatHeightFocused", this.chatHeightFocused);
      param0.process("chatDelay", this.chatDelay);
      param0.process("chatHeightUnfocused", this.chatHeightUnfocused);
      param0.process("chatScale", this.chatScale);
      param0.process("chatWidth", this.chatWidth);
      param0.process("notificationDisplayTime", this.notificationDisplayTime);
      param0.process("mipmapLevels", this.mipmapLevels);
      this.useNativeTransport = param0.process("useNativeTransport", this.useNativeTransport);
      param0.process("mainHand", this.mainHand);
      param0.process("attackIndicator", this.attackIndicator);
      param0.process("narrator", this.narrator);
      this.tutorialStep = param0.process("tutorialStep", this.tutorialStep, TutorialSteps::getByName, TutorialSteps::getName);
      param0.process("mouseWheelSensitivity", this.mouseWheelSensitivity);
      param0.process("rawMouseInput", this.rawMouseInput);
      this.glDebugVerbosity = param0.process("glDebugVerbosity", this.glDebugVerbosity);
      this.skipMultiplayerWarning = param0.process("skipMultiplayerWarning", this.skipMultiplayerWarning);
      this.skipRealms32bitWarning = param0.process("skipRealms32bitWarning", this.skipRealms32bitWarning);
      param0.process("hideMatchedNames", this.hideMatchedNames);
      this.joinedFirstServer = param0.process("joinedFirstServer", this.joinedFirstServer);
      this.hideBundleTutorial = param0.process("hideBundleTutorial", this.hideBundleTutorial);
      this.syncWrites = param0.process("syncChunkWrites", this.syncWrites);
      param0.process("showAutosaveIndicator", this.showAutosaveIndicator);
      param0.process("allowServerListing", this.allowServerListing);
      param0.process("onlyShowSecureChat", this.onlyShowSecureChat);
      param0.process("panoramaScrollSpeed", this.panoramaSpeed);
      param0.process("telemetryOptInExtra", this.telemetryOptInExtra);
      this.onboardAccessibility = param0.process("onboardAccessibility", this.onboardAccessibility);

      for (KeyMapping var5 : this.keyMappings) {
         String var6 = var5.saveString();
         String var7 = param0.process("key_" + var5.getName(), var6);
         if (!var6.equals(var7)) {
            var5.setKey(InputConstants.getKey(var7));
         }
      }

      for (SoundSource var5 : SoundSource.values()) {
         param0.process("soundCategory_" + var5.getName(), this.soundSourceVolumes.get(var5));
      }

      for (PlayerModelPart var5 : PlayerModelPart.values()) {
         boolean var6 = this.modelParts.contains(var5);
         boolean var7 = param0.process("modelPart_" + var5.getId(), var6);
         if (var7 != var6) {
            this.setModelPart(var5, var7);
         }
      }
   }

   public void load() {
      try {
         if (!this.optionsFile.exists()) {
            return;
         }

         CompoundTag var1 = new CompoundTag();

         try (BufferedReader var2 = Files.newReader(this.optionsFile, Charsets.UTF_8)) {
            var2.lines().forEach(l_param1 -> {
               try {
                  Iterator<String> l_var2 = OPTION_SPLITTER.split(l_param1).iterator();
                  var1.putString(l_var2.next(), l_var2.next());
               } catch (Exception l_var2) {
                  LOGGER.warn("Skipping bad option: {}", l_param1);
               }
            });
         }

         final CompoundTag var2 = this.dataFix(var1);
         if (!var2.contains("graphicsMode") && var2.contains("fancyGraphics")) {
            if (isTrue(var2.getString("fancyGraphics"))) {
               this.graphicsMode.set(GraphicsStatus.FANCY);
            } else {
               this.graphicsMode.set(GraphicsStatus.FAST);
            }
         }

         this.processOptions(
            new Options.FieldAccess() {
               @Nullable
               private String getValueOrNull(String l_param0) {
                  return var2.contains(l_param0) ? var2.getString(l_param0) : null;
               }

               @Override
               public <T> void process(String l_param0, OptionInstance<T> l_param1) {
                  String l_var3 = this.getValueOrNull(l_param0);
                  if (l_var3 != null) {
                     JsonReader l_var4 = new JsonReader(new StringReader(l_var3.isEmpty() ? "\"\"" : l_var3));
                     JsonElement l_var5 = JsonParser.parseReader(l_var4);
                     DataResult<T> l_var6 = l_param1.codec().parse(JsonOps.INSTANCE, l_var5);
                     l_var6.error()
                        .ifPresent(
                           l2_param2 -> Options.LOGGER.error("Error parsing option value " + l_var3 + " for option " + l_param1 + ": " + l2_param2.message())
                        );
                     l_var6.result().ifPresent(l_param1::set);
                  }
               }

               @Override
               public int process(String l_param0, int l_param1) {
                  String l_var3 = this.getValueOrNull(l_param0);
                  if (l_var3 != null) {
                     try {
                        return Integer.parseInt(l_var3);
                     } catch (NumberFormatException l_var4) {
                        Options.LOGGER.warn("Invalid integer value for option {} = {}", l_param0, l_var3, l_var4);
                     }
                  }

                  return l_param1;
               }

               @Override
               public boolean process(String l_param0, boolean l_param1) {
                  String l_var3 = this.getValueOrNull(l_param0);
                  return l_var3 != null ? Options.isTrue(l_var3) : l_param1;
               }

               @Override
               public String process(String l_param0, String l_param1) {
                  return MoreObjects.firstNonNull(this.getValueOrNull(l_param0), l_param1);
               }

               @Override
               public float process(String l_param0, float l_param1) {
                  String l_var3 = this.getValueOrNull(l_param0);
                  if (l_var3 == null) {
                     return l_param1;
                  }

                  if (Options.isTrue(l_var3)) {
                     return 1.0F;
                  }

                  if (Options.isFalse(l_var3)) {
                     return 0.0F;
                  }

                  try {
                     return Float.parseFloat(l_var3);
                  } catch (NumberFormatException l_var4) {
                     Options.LOGGER.warn("Invalid floating point value for option {} = {}", l_param0, l_var3, l_var4);
                     return l_param1;
                  }
               }

               @Override
               public <T> T process(String l_param0, T l_param1, Function<String, T> l_param2, Function<T, String> l_param3) {
                  String l_var5 = this.getValueOrNull(l_param0);
                  return l_var5 == null ? l_param1 : l_param2.apply(l_var5);
               }
            }
         );
         if (var2.contains("fullscreenResolution")) {
            this.fullscreenVideoModeString = var2.getString("fullscreenResolution");
         }

         if (this.minecraft.getWindow() != null) {
            this.minecraft.getWindow().setFramerateLimit(this.framerateLimit.get());
         }

         // Cooperative-port: one-time FPS auto-tune for profiles predating the performance
         // defaults. TeaVM worlds cost ~2s of main-thread work per generated chunk, so
         // vanilla's heavier rendering defaults (8 chunks, Fancy, AO, shadows, biome
         // blending) fold the client under chunk-gen lag. Versioned via the
         // "perfAutoTune" key so future tuning waves can re-apply; after this once, the
         // user's own changes are never touched again.
         if (!var2.contains("perfAutoTune")) {
            this.renderDistance.set(4);
            this.graphicsMode.set(GraphicsStatus.FAST);
            this.ambientOcclusion.set(false);
            this.entityShadows.set(false);
            this.biomeBlendRadius.set(0);
            this.particles.set(ParticleStatus.DECREASED);
            this.save();
         }

         KeyMapping.resetMapping();
      } catch (Exception var1) {
         LOGGER.error("Failed to load options", var1);
      }
   }

   static boolean isTrue(String param0) {
      return "true".equals(param0);
   }

   static boolean isFalse(String param0) {
      return "false".equals(param0);
   }

   private CompoundTag dataFix(CompoundTag param0) {
      int var2 = 0;

      try {
         var2 = Integer.parseInt(param0.getString("version"));
      } catch (RuntimeException var4) {
      }

      return DataFixTypes.OPTIONS.updateToCurrentVersion(this.minecraft.getFixerUpper(), param0, var2);
   }

   public void save() {
      try (final PrintWriter var1 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8))) {
         var1.println("version:" + SharedConstants.getCurrentVersion().getDataVersion().getVersion());
         var1.println("perfAutoTune:1");
         this.processOptions(new Options.FieldAccess() {
            public void writePrefix(String l_param0) {
               var1.print(l_param0);
               var1.print(':');
            }

            @Override
            public <T> void process(String l_param0, OptionInstance<T> l_param1) {
               DataResult<JsonElement> l_var3 = l_param1.codec().encodeStart(JsonOps.INSTANCE, l_param1.get());
               l_var3.error().ifPresent(l2_param1 -> Options.LOGGER.error("Error saving option " + l_param1 + ": " + l2_param1));
               l_var3.result().ifPresent(l2_param2 -> {
                  this.writePrefix(l_param0);
                  var1.println(Options.GSON.toJson(l2_param2));
               });
            }

            @Override
            public int process(String l_param0, int l_param1) {
               this.writePrefix(l_param0);
               var1.println(l_param1);
               return l_param1;
            }

            @Override
            public boolean process(String l_param0, boolean l_param1) {
               this.writePrefix(l_param0);
               var1.println(l_param1);
               return l_param1;
            }

            @Override
            public String process(String l_param0, String l_param1) {
               this.writePrefix(l_param0);
               var1.println(l_param1);
               return l_param1;
            }

            @Override
            public float process(String l_param0, float l_param1) {
               this.writePrefix(l_param0);
               var1.println(l_param1);
               return l_param1;
            }

            @Override
            public <T> T process(String l_param0, T l_param1, Function<String, T> l_param2, Function<T, String> l_param3) {
               this.writePrefix(l_param0);
               var1.println(l_param3.apply(l_param1));
               return l_param1;
            }
         });
         if (this.minecraft.getWindow().getPreferredFullscreenVideoMode().isPresent()) {
            var1.println("fullscreenResolution:" + this.minecraft.getWindow().getPreferredFullscreenVideoMode().get().write());
         }
      } catch (Exception var1) {
         LOGGER.error("Failed to save options", var1);
      }

      this.broadcastOptions();
   }

   public void broadcastOptions() {
      if (this.minecraft.player != null) {
         int var1 = 0;

         for (PlayerModelPart var3 : this.modelParts) {
            var1 |= var3.getMask();
         }

         this.minecraft
            .player
            .connection
            .send(
               new ServerboundClientInformationPacket(
                  this.languageCode,
                  this.renderDistance.get(),
                  this.chatVisibility.get(),
                  this.chatColors.get(),
                  var1,
                  this.mainHand.get(),
                  this.minecraft.isTextFilteringEnabled(),
                  this.allowServerListing.get()
               )
            );
      }
   }

   private void setModelPart(PlayerModelPart param0, boolean param1) {
      if (param1) {
         this.modelParts.add(param0);
      } else {
         this.modelParts.remove(param0);
      }
   }

   public boolean isModelPartEnabled(PlayerModelPart param0) {
      return this.modelParts.contains(param0);
   }

   public void toggleModelPart(PlayerModelPart param0, boolean param1) {
      this.setModelPart(param0, param1);
      this.broadcastOptions();
   }

   public CloudStatus getCloudsType() {
      return this.getEffectiveRenderDistance() >= 4 ? this.cloudStatus.get() : CloudStatus.OFF;
   }

   public boolean useNativeTransport() {
      return this.useNativeTransport;
   }

   public void loadSelectedResourcePacks(PackRepository param0) {
      Set<String> var2 = Sets.newLinkedHashSet();
      Iterator<String> var3 = this.resourcePacks.iterator();

      while (var3.hasNext()) {
         String var4 = var3.next();
         Pack var5 = param0.getPack(var4);
         if (var5 == null && !var4.startsWith("file/")) {
            var5 = param0.getPack("file/" + var4);
         }

         if (var5 == null) {
            LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", var4);
            var3.remove();
         } else if (!var5.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(var4)) {
            LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", var4);
            var3.remove();
         } else if (var5.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(var4)) {
            LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", var4);
            this.incompatibleResourcePacks.remove(var4);
         } else {
            var2.add(var5.getId());
         }
      }

      param0.setSelected(var2);
   }

   public CameraType getCameraType() {
      return this.cameraType;
   }

   public void setCameraType(CameraType param0) {
      this.cameraType = param0;
   }

   private static List<String> readPackList(String param0) {
      List<String> var1 = GsonHelper.fromNullableJson(GSON, param0, RESOURCE_PACK_TYPE);
      return var1 != null ? var1 : Lists.newArrayList();
   }

   public File getFile() {
      return this.optionsFile;
   }

   public String dumpOptionsForReport() {
      Stream<Pair<String, Object>> var1 = Stream.<Pair<String, Object>>builder()
         .add(Pair.of("ao", this.ambientOcclusion.get()))
         .add(Pair.of("biomeBlendRadius", this.biomeBlendRadius.get()))
         .add(Pair.of("enableVsync", this.enableVsync.get()))
         .add(Pair.of("entityDistanceScaling", this.entityDistanceScaling.get()))
         .add(Pair.of("entityShadows", this.entityShadows.get()))
         .add(Pair.of("forceUnicodeFont", this.forceUnicodeFont.get()))
         .add(Pair.of("fov", this.fov.get()))
         .add(Pair.of("fovEffectScale", this.fovEffectScale.get()))
         .add(Pair.of("darknessEffectScale", this.darknessEffectScale.get()))
         .add(Pair.of("glintSpeed", this.glintSpeed.get()))
         .add(Pair.of("glintStrength", this.glintStrength.get()))
         .add(Pair.of("prioritizeChunkUpdates", this.prioritizeChunkUpdates.get()))
         .add(Pair.of("fullscreen", this.fullscreen.get()))
         .add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenVideoModeString)))
         .add(Pair.of("gamma", this.gamma.get()))
         .add(Pair.of("glDebugVerbosity", this.glDebugVerbosity))
         .add(Pair.of("graphicsMode", this.graphicsMode.get()))
         .add(Pair.of("guiScale", this.guiScale.get()))
         .add(Pair.of("maxFps", this.framerateLimit.get()))
         .add(Pair.of("mipmapLevels", this.mipmapLevels.get()))
         .add(Pair.of("narrator", this.narrator.get()))
         .add(Pair.of("overrideHeight", this.overrideHeight))
         .add(Pair.of("overrideWidth", this.overrideWidth))
         .add(Pair.of("particles", this.particles.get()))
         .add(Pair.of("reducedDebugInfo", this.reducedDebugInfo.get()))
         .add(Pair.of("renderClouds", this.cloudStatus.get()))
         .add(Pair.of("renderDistance", this.renderDistance.get()))
         .add(Pair.of("simulationDistance", this.simulationDistance.get()))
         .add(Pair.of("resourcePacks", this.resourcePacks))
         .add(Pair.of("screenEffectScale", this.screenEffectScale.get()))
         .add(Pair.of("syncChunkWrites", this.syncWrites))
         .add(Pair.of("useNativeTransport", this.useNativeTransport))
         .add(Pair.of("soundDevice", this.soundDevice.get()))
         .build();
      return var1.<CharSequence>map(l_param0 -> l_param0.getFirst() + ": " + l_param0.getSecond()).collect(Collectors.joining(System.lineSeparator()));
   }

   public void setServerRenderDistance(int param0) {
      this.serverRenderDistance = param0;
   }

   public int getEffectiveRenderDistance() {
      return this.serverRenderDistance > 0 ? Math.min(this.renderDistance.get(), this.serverRenderDistance) : this.renderDistance.get();
   }

   private static Component pixelValueLabel(Component param0, int param1) {
      return Component.translatable("options.pixel_value", param0, param1);
   }

   private static Component percentValueLabel(Component param0, double param1) {
      return Component.translatable("options.percent_value", param0, (int)(param1 * 100.0));
   }

   public static Component genericValueLabel(Component param0, Component param1) {
      return Component.translatable("options.generic_value", param0, param1);
   }

   public static Component genericValueLabel(Component param0, int param1) {
      return genericValueLabel(param0, Component.literal(Integer.toString(param1)));
   }

   interface FieldAccess {
      <T> void process(String var1, OptionInstance<T> var2);

      int process(String var1, int var2);

      boolean process(String var1, boolean var2);

      String process(String var1, String var2);

      float process(String var1, float var2);

      <T> T process(String var1, T var2, Function<String, T> var3, Function<T, String> var4);
   }
}
