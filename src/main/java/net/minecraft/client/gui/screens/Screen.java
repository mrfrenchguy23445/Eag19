package net.minecraft.client.gui.screens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.slf4j.Logger;

public abstract class Screen extends AbstractContainerEventHandler implements Renderable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<String> ALLOWED_PROTOCOLS = Sets.newHashSet("http", "https");
   private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
   private static final Component USAGE_NARRATION = Component.translatable("narrator.screen.usage");
   protected final Component title;
   private final List<GuiEventListener> children = Lists.newArrayList();
   private final List<NarratableEntry> narratables = Lists.newArrayList();
   @Nullable
   protected Minecraft minecraft;
   private boolean initialized;
   protected ItemRenderer itemRenderer;
   public int width;
   public int height;
   private final List<Renderable> renderables = Lists.newArrayList();
   public boolean passEvents;
   protected Font font;
   @Nullable
   private URI clickedLink;
   private static final long NARRATE_SUPPRESS_AFTER_INIT_TIME = TimeUnit.SECONDS.toMillis(2L);
   private static final long NARRATE_DELAY_NARRATOR_ENABLED = NARRATE_SUPPRESS_AFTER_INIT_TIME;
   private static final long NARRATE_DELAY_MOUSE_MOVE = 750L;
   private static final long NARRATE_DELAY_MOUSE_ACTION = 200L;
   private static final long NARRATE_DELAY_KEYBOARD_ACTION = 200L;
   private final ScreenNarrationCollector narrationState = new ScreenNarrationCollector();
   private long narrationSuppressTime = Long.MIN_VALUE;
   private long nextNarrationTime = Long.MAX_VALUE;
   @Nullable
   private NarratableEntry lastNarratable;
   @Nullable
   private Screen.DeferredTooltipRendering deferredTooltipRendering;

   protected Screen(Component param0) {
      this.title = param0;
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getNarrationMessage() {
      return this.getTitle();
   }

   public final void renderWithTooltip(PoseStack param0, int param1, int param2, float param3) {
      this.render(param0, param1, param2, param3);
      if (this.deferredTooltipRendering != null) {
         this.renderTooltip(param0, this.deferredTooltipRendering, param1, param2);
         this.deferredTooltipRendering = null;
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      for (Renderable var6 : this.renderables) {
         var6.render(param0, param1, param2, param3);
      }
   }

   @Override
   public boolean keyPressed(int param0, int param1, int param2) {
      if (param0 == 256 && this.shouldCloseOnEsc()) {
         this.onClose();
         return true;
      }

      if (super.keyPressed(param0, param1, param2)) {
         return true;
      }

      FocusNavigationEvent var4 = switch (param0) {
         case 258 -> this.createTabEvent();
         default -> null;
         case 262 -> this.createArrowEvent(ScreenDirection.RIGHT);
         case 263 -> this.createArrowEvent(ScreenDirection.LEFT);
         case 264 -> this.createArrowEvent(ScreenDirection.DOWN);
         case 265 -> this.createArrowEvent(ScreenDirection.UP);
      };
      if (var4 != null) {
         ComponentPath var5 = super.nextFocusPath(var4);
         if (var5 == null && var4 instanceof FocusNavigationEvent.TabNavigation) {
            this.clearFocus();
            var5 = super.nextFocusPath(var4);
         }

         if (var5 != null) {
            this.changeFocus(var5);
         }
      }

      return false;
   }

   private FocusNavigationEvent.TabNavigation createTabEvent() {
      boolean var1 = !hasShiftDown();
      return new FocusNavigationEvent.TabNavigation(var1);
   }

   private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection param0) {
      return new FocusNavigationEvent.ArrowNavigation(param0);
   }

   protected void setInitialFocus(GuiEventListener param0) {
      ComponentPath var2 = ComponentPath.path(this, param0.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
      if (var2 != null) {
         this.changeFocus(var2);
      }
   }

   private void clearFocus() {
      ComponentPath var1 = this.getCurrentFocusPath();
      if (var1 != null) {
         var1.applyFocus(false);
      }
   }

   @VisibleForTesting
   protected void changeFocus(ComponentPath param0) {
      this.clearFocus();
      param0.applyFocus(true);
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void onClose() {
      this.minecraft.setScreen(null);
   }

   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T param0) {
      this.renderables.add(param0);
      return this.addWidget(param0);
   }

   protected <T extends Renderable> T addRenderableOnly(T param0) {
      this.renderables.add(param0);
      return param0;
   }

   protected <T extends GuiEventListener & NarratableEntry> T addWidget(T param0) {
      this.children.add(param0);
      this.narratables.add(param0);
      return param0;
   }

   protected void removeWidget(GuiEventListener param0) {
      if (param0 instanceof Renderable) {
         this.renderables.remove((Renderable)param0);
      }

      if (param0 instanceof NarratableEntry) {
         this.narratables.remove((NarratableEntry)param0);
      }

      this.children.remove(param0);
   }

   protected void clearWidgets() {
      this.renderables.clear();
      this.children.clear();
      this.narratables.clear();
   }

   protected void renderTooltip(PoseStack param0, ItemStack param1, int param2, int param3) {
      this.renderTooltip(param0, this.getTooltipFromItem(param1), param1.getTooltipImage(), param2, param3);
   }

   public void renderTooltip(PoseStack param0, List<Component> param1, Optional<TooltipComponent> param2, int param3, int param4) {
      List<ClientTooltipComponent> var6 = param1.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
      param2.ifPresent(l_param1 -> var6.add(1, ClientTooltipComponent.create(l_param1)));
      this.renderTooltipInternal(param0, var6, param3, param4, DefaultTooltipPositioner.INSTANCE);
   }

   public List<Component> getTooltipFromItem(ItemStack param0) {
      return param0.getTooltipLines(
         this.minecraft.player, this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
      );
   }

   public void renderTooltip(PoseStack param0, Component param1, int param2, int param3) {
      this.renderTooltip(param0, Arrays.asList(param1.getVisualOrderText()), param2, param3);
   }

   public void renderComponentTooltip(PoseStack param0, List<Component> param1, int param2, int param3) {
      this.renderTooltip(param0, Lists.transform(param1, Component::getVisualOrderText), param2, param3);
   }

   public void renderTooltip(PoseStack param0, List<? extends FormattedCharSequence> param1, int param2, int param3) {
      this.renderTooltipInternal(
         param0, param1.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), param2, param3, DefaultTooltipPositioner.INSTANCE
      );
   }

   private void renderTooltip(PoseStack param0, Screen.DeferredTooltipRendering param1, int param2, int param3) {
      this.renderTooltipInternal(
         param0, param1.tooltip().stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), param2, param3, param1.positioner()
      );
   }

   private void renderTooltipInternal(PoseStack param0, List<ClientTooltipComponent> param1, int param2, int param3, ClientTooltipPositioner param4) {
      if (!param1.isEmpty()) {
         int var6 = 0;
         int var7 = param1.size() == 1 ? -2 : 0;

         for (ClientTooltipComponent var9 : param1) {
            int var10 = var9.getWidth(this.font);
            if (var10 > var6) {
               var6 = var10;
            }

            var7 += var9.getHeight();
         }

         int var8 = var6;
         int var9 = var7;
         Vector2ic var10 = param4.positionTooltip(this, param2, param3, var8, var9);
         int var11 = var10.x();
         int var12 = var10.y();
         param0.pushPose();
         int var13 = 400;
         Tesselator var14 = Tesselator.getInstance();
         BufferBuilder var15 = var14.getBuilder();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         var15.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         Matrix4f var16 = param0.last().pose();
         TooltipRenderUtil.renderTooltipBackground(
            (l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, l_param6, l_param7, l_param8) -> GuiComponent.fillGradient(
               l_param0, l_param1, l_param2, l_param3, l_param4, l_param5, l_param6, l_param7, l_param8
            ),
            var16,
            var15,
            var11,
            var12,
            var8,
            var9,
            400
         );
         RenderSystem.enableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         BufferUploader.drawWithShader(var15.end());
         MultiBufferSource.BufferSource var17 = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         param0.translate(0.0F, 0.0F, 400.0F);
         int var18 = var12;

         for (int var19 = 0; var19 < param1.size(); var19++) {
            ClientTooltipComponent var20 = param1.get(var19);
            var20.renderText(this.font, var11, var18, var16, var17);
            var18 += var20.getHeight() + (var19 == 0 ? 2 : 0);
         }

         var17.endBatch();
         var18 = var12;

         for (int var19 = 0; var19 < param1.size(); var19++) {
            ClientTooltipComponent var20 = param1.get(var19);
            var20.renderImage(this.font, var11, var18, param0, this.itemRenderer);
            var18 += var20.getHeight() + (var19 == 0 ? 2 : 0);
         }

         param0.popPose();
      }
   }

   protected void renderComponentHoverEffect(PoseStack param0, @Nullable Style param1, int param2, int param3) {
      if (param1 != null && param1.getHoverEvent() != null) {
         HoverEvent var5 = param1.getHoverEvent();
         HoverEvent.ItemStackInfo var6 = var5.getValue(HoverEvent.Action.SHOW_ITEM);
         if (var6 != null) {
            this.renderTooltip(param0, var6.getItemStack(), param2, param3);
         } else {
            HoverEvent.EntityTooltipInfo var7 = var5.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (var7 != null) {
               if (this.minecraft.options.advancedItemTooltips) {
                  this.renderComponentTooltip(param0, var7.getTooltipLines(), param2, param3);
               }
            } else {
               Component var8 = var5.getValue(HoverEvent.Action.SHOW_TEXT);
               if (var8 != null) {
                  this.renderTooltip(param0, this.minecraft.font.split(var8, Math.max(this.width / 2, 200)), param2, param3);
               }
            }
         }
      }
   }

   protected void insertText(String param0, boolean param1) {
   }

   public boolean handleComponentClicked(@Nullable Style param0) {
      if (param0 == null) {
         return false;
      }

      ClickEvent var2 = param0.getClickEvent();
      if (hasShiftDown()) {
         if (param0.getInsertion() != null) {
            this.insertText(param0.getInsertion(), false);
         }
      } else if (var2 != null) {
         if (var2.getAction() == ClickEvent.Action.OPEN_URL) {
            if (!this.minecraft.options.chatLinks().get()) {
               return false;
            }

            try {
               URI var3 = new URI(var2.getValue());
               String var4 = var3.getScheme();
               if (var4 == null) {
                  throw new URISyntaxException(var2.getValue(), "Missing protocol");
               }

               if (!ALLOWED_PROTOCOLS.contains(var4.toLowerCase(Locale.ROOT))) {
                  throw new URISyntaxException(var2.getValue(), "Unsupported protocol: " + var4.toLowerCase(Locale.ROOT));
               }

               if (this.minecraft.options.chatLinksPrompt().get()) {
                  this.clickedLink = var3;
                  this.minecraft.setScreen(new ConfirmLinkScreen(this::confirmLink, var2.getValue(), false));
               } else {
                  this.openLink(var3);
               }
            } catch (URISyntaxException var3) {
               LOGGER.error("Can't open url for {}", var2, var3);
            }
         } else if (var2.getAction() == ClickEvent.Action.OPEN_FILE) {
            URI var3 = new File(var2.getValue()).toURI();
            this.openLink(var3);
         } else if (var2.getAction() == ClickEvent.Action.SUGGEST_COMMAND) {
            this.insertText(SharedConstants.filterText(var2.getValue()), true);
         } else if (var2.getAction() == ClickEvent.Action.RUN_COMMAND) {
            String var3 = SharedConstants.filterText(var2.getValue());
            if (var3.startsWith("/")) {
               if (!this.minecraft.player.connection.sendUnsignedCommand(var3.substring(1))) {
                  LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", var3);
               }
            } else {
               LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", var3);
            }
         } else if (var2.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
            this.minecraft.keyboardHandler.setClipboard(var2.getValue());
         } else {
            LOGGER.error("Don't know how to handle {}", var2);
         }

         return true;
      }

      return false;
   }

   public final void init(Minecraft param0, int param1, int param2) {
      this.minecraft = param0;
      this.itemRenderer = param0.getItemRenderer();
      this.font = param0.font;
      this.width = param1;
      this.height = param2;
      if (!this.initialized) {
         this.init();
      } else {
         this.repositionElements();
      }

      this.initialized = true;
      this.triggerImmediateNarration(false);
      this.suppressNarration(NARRATE_SUPPRESS_AFTER_INIT_TIME);
   }

   protected void rebuildWidgets() {
      this.clearWidgets();
      this.clearFocus();
      this.init();
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   protected void init() {
   }

   public void tick() {
   }

   public void removed() {
   }

   public void added() {
   }

   public void renderBackground(PoseStack param0) {
      if (this.minecraft.level != null) {
         fillGradient(param0, 0, 0, this.width, this.height, -1072689136, -804253680);
      } else {
         this.renderDirtBackground(param0);
      }
   }

   public void renderDirtBackground(PoseStack param0) {
      RenderSystem.setShaderTexture(0, BACKGROUND_LOCATION);
      RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
      int var2 = 32;
      blit(param0, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean isPauseScreen() {
      return true;
   }

   private void confirmLink(boolean param0) {
      if (param0) {
         this.openLink(this.clickedLink);
      }

      this.clickedLink = null;
      this.minecraft.setScreen(this);
   }

   private void openLink(URI param0) {
      Util.getPlatform().openUri(param0);
   }

   public static boolean hasControlDown() {
      return Minecraft.ON_OSX
         ? InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 343)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 347)
         : InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 341)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 345);
   }

   public static boolean hasShiftDown() {
      return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
         || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344);
   }

   public static boolean hasAltDown() {
      return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342)
         || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 346);
   }

   public static boolean isCut(int param0) {
      return param0 == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isPaste(int param0) {
      return param0 == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isCopy(int param0) {
      return param0 == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isSelectAll(int param0) {
      return param0 == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   protected void repositionElements() {
      this.rebuildWidgets();
   }

   public void resize(Minecraft param0, int param1, int param2) {
      this.width = param1;
      this.height = param2;
      this.repositionElements();
   }

   public static void wrapScreenError(Runnable param0, String param1, String param2) {
      try {
         param0.run();
      } catch (Throwable var3) {
         CrashReport var4 = CrashReport.forThrowable(var3, param1);
         CrashReportCategory var5 = var4.addCategory("Affected screen");
         var5.setDetail("Screen name", () -> param2);
         throw new ReportedException(var4);
      }
   }

   protected boolean isValidCharacterForName(String param0, char param1, int param2) {
      int var4 = param0.indexOf(58);
      int var5 = param0.indexOf(47);
      if (param1 == ':') {
         return (var5 == -1 || param2 <= var5) && var4 == -1;
      } else {
         return param1 == '/'
            ? param2 > var4
            : param1 == '_' || param1 == '-' || param1 >= 'a' && param1 <= 'z' || param1 >= '0' && param1 <= '9' || param1 == '.';
      }
   }

   @Override
   public boolean isMouseOver(double param0, double param2) {
      return true;
   }

   public void onFilesDrop(List<Path> param0) {
   }

   private void scheduleNarration(long param0, boolean param2) {
      this.nextNarrationTime = Util.getMillis() + param0;
      if (param2) {
         this.narrationSuppressTime = Long.MIN_VALUE;
      }
   }

   private void suppressNarration(long param0) {
      this.narrationSuppressTime = Util.getMillis() + param0;
   }

   public void afterMouseMove() {
      this.scheduleNarration(750L, false);
   }

   public void afterMouseAction() {
      this.scheduleNarration(200L, true);
   }

   public void afterKeyboardAction() {
      this.scheduleNarration(200L, true);
   }

   private boolean shouldRunNarration() {
      return this.minecraft.getNarrator().isActive();
   }

   public void handleDelayedNarration() {
      if (this.shouldRunNarration()) {
         long var1 = Util.getMillis();
         if (var1 > this.nextNarrationTime && var1 > this.narrationSuppressTime) {
            this.runNarration(true);
            this.nextNarrationTime = Long.MAX_VALUE;
         }
      }
   }

   public void triggerImmediateNarration(boolean param0) {
      if (this.shouldRunNarration()) {
         this.runNarration(param0);
      }
   }

   private void runNarration(boolean param0) {
      this.narrationState.update(this::updateNarrationState);
      String var2 = this.narrationState.collectNarrationText(!param0);
      if (!var2.isEmpty()) {
         this.minecraft.getNarrator().sayNow(var2);
      }
   }

   protected boolean shouldNarrateNavigation() {
      return true;
   }

   protected void updateNarrationState(NarrationElementOutput param0) {
      param0.add(NarratedElementType.TITLE, this.getNarrationMessage());
      if (this.shouldNarrateNavigation()) {
         param0.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }

      this.updateNarratedWidget(param0);
   }

   protected void updateNarratedWidget(NarrationElementOutput param0) {
      List<NarratableEntry> var2 = this.narratables.stream().filter(NarratableEntry::isActive).collect(Collectors.toList());
      Collections.sort(var2, Comparator.comparingInt(TabOrderedElement::getTabOrderGroup));
      Screen.NarratableSearchResult var3 = findNarratableWidget(var2, this.lastNarratable);
      if (var3 != null) {
         if (var3.priority.isTerminal()) {
            this.lastNarratable = var3.entry;
         }

         if (var2.size() > 1) {
            param0.add(NarratedElementType.POSITION, Component.translatable("narrator.position.screen", var3.index + 1, var2.size()));
            if (var3.priority == NarratableEntry.NarrationPriority.FOCUSED) {
               param0.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
            }
         }

         var3.entry.updateNarration(param0.nest());
      }
   }

   @Nullable
   public static Screen.NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> param0, @Nullable NarratableEntry param1) {
      Screen.NarratableSearchResult var2 = null;
      Screen.NarratableSearchResult var3 = null;
      int var4 = 0;

      for (int var5 = param0.size(); var4 < var5; var4++) {
         NarratableEntry var6 = param0.get(var4);
         NarratableEntry.NarrationPriority var7 = var6.narrationPriority();
         if (var7.isTerminal()) {
            if (var6 != param1) {
               return new Screen.NarratableSearchResult(var6, var4, var7);
            }

            var3 = new Screen.NarratableSearchResult(var6, var4, var7);
         } else if (var7.compareTo(var2 != null ? var2.priority : NarratableEntry.NarrationPriority.NONE) > 0) {
            var2 = new Screen.NarratableSearchResult(var6, var4, var7);
         }
      }

      return var2 != null ? var2 : var3;
   }

   public void narrationEnabled() {
      this.scheduleNarration(NARRATE_DELAY_NARRATOR_ENABLED, false);
   }

   public void setTooltipForNextRenderPass(List<FormattedCharSequence> param0) {
      this.setTooltipForNextRenderPass(param0, DefaultTooltipPositioner.INSTANCE, true);
   }

   public void setTooltipForNextRenderPass(List<FormattedCharSequence> param0, ClientTooltipPositioner param1, boolean param2) {
      if (this.deferredTooltipRendering == null || param2) {
         this.deferredTooltipRendering = new Screen.DeferredTooltipRendering(param0, param1);
      }
   }

   protected void setTooltipForNextRenderPass(Component param0) {
      this.setTooltipForNextRenderPass(Tooltip.splitTooltip(this.minecraft, param0));
   }

   public void setTooltipForNextRenderPass(Tooltip param0, ClientTooltipPositioner param1, boolean param2) {
      this.setTooltipForNextRenderPass(param0.toCharSequence(this.minecraft), param1, param2);
   }

   protected static void hideWidgets(AbstractWidget... param0) {
      for (AbstractWidget var4 : param0) {
         var4.visible = false;
      }
   }

   @Override
   public ScreenRectangle getRectangle() {
      return new ScreenRectangle(0, 0, this.width, this.height);
   }

   record DeferredTooltipRendering(List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner) {
   }

   public static class NarratableSearchResult {
      public final NarratableEntry entry;
      public final int index;
      public final NarratableEntry.NarrationPriority priority;

      public NarratableSearchResult(NarratableEntry param0, int param1, NarratableEntry.NarrationPriority param2) {
         this.entry = param0;
         this.index = param1;
         this.priority = param2;
      }
   }
}
