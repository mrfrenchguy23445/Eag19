package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.TextRenderingUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsSelectWorldTemplateScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation LINK_ICON = new ResourceLocation("realms", "textures/gui/realms/link_icons.png");
   static final ResourceLocation TRAILER_ICON = new ResourceLocation("realms", "textures/gui/realms/trailer_icons.png");
   static final ResourceLocation SLOT_FRAME_LOCATION = new ResourceLocation("realms", "textures/gui/realms/slot_frame.png");
   static final Component PUBLISHER_LINK_TOOLTIP = Component.translatable("mco.template.info.tooltip");
   static final Component TRAILER_LINK_TOOLTIP = Component.translatable("mco.template.trailer.tooltip");
   private final Consumer<WorldTemplate> callback;
   RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList worldTemplateObjectSelectionList;
   int selectedTemplate = -1;
   private Button selectButton;
   private Button trailerButton;
   private Button publisherButton;
   @Nullable
   Component toolTip;
   @Nullable
   String currentLink;
   private final RealmsServer.WorldType worldType;
   int clicks;
   @Nullable
   private Component[] warning;
   private String warningURL;
   boolean displayWarning;
   private boolean hoverWarning;
   @Nullable
   List<TextRenderingUtils.Line> noTemplatesMessage;

   public RealmsSelectWorldTemplateScreen(Component param0, Consumer<WorldTemplate> param1, RealmsServer.WorldType param2) {
      this(param0, param1, param2, null);
   }

   public RealmsSelectWorldTemplateScreen(
      Component param0, Consumer<WorldTemplate> param1, RealmsServer.WorldType param2, @Nullable WorldTemplatePaginatedList param3
   ) {
      super(param0);
      this.callback = param1;
      this.worldType = param2;
      if (param3 == null) {
         this.worldTemplateObjectSelectionList = new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList();
         this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
      } else {
         this.worldTemplateObjectSelectionList = new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList(Lists.newArrayList(param3.templates));
         this.fetchTemplatesAsync(param3);
      }
   }

   public void setWarning(Component... param0) {
      this.warning = param0;
      this.displayWarning = true;
   }

   @Override
   public boolean mouseClicked(double param0, double param2, int param4) {
      if (this.hoverWarning && this.warningURL != null) {
         Util.getPlatform().openUri("https://www.minecraft.net/realms/adventure-maps-in-1-9");
         return true;
      } else {
         return super.mouseClicked(param0, param2, param4);
      }
   }

   @Override
   public void init() {
      this.worldTemplateObjectSelectionList = new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList(
         this.worldTemplateObjectSelectionList.getTemplates()
      );
      this.trailerButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.template.button.trailer"), l_param0 -> this.onTrailer())
            .bounds(this.width / 2 - 206, this.height - 32, 100, 20)
            .build()
      );
      this.selectButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.template.button.select"), l_param0 -> this.selectTemplate())
            .bounds(this.width / 2 - 100, this.height - 32, 100, 20)
            .build()
      );
      Component var1 = this.worldType == RealmsServer.WorldType.MINIGAME ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_BACK;
      Button var2 = Button.builder(var1, l_param0 -> this.onClose()).bounds(this.width / 2 + 6, this.height - 32, 100, 20).build();
      this.addRenderableWidget(var2);
      this.publisherButton = this.addRenderableWidget(
         Button.builder(Component.translatable("mco.template.button.publisher"), l_param0 -> this.onPublish())
            .bounds(this.width / 2 + 112, this.height - 32, 100, 20)
            .build()
      );
      this.selectButton.active = false;
      this.trailerButton.visible = false;
      this.publisherButton.visible = false;
      this.addWidget(this.worldTemplateObjectSelectionList);
      this.magicalSpecialHackyFocus(this.worldTemplateObjectSelectionList);
   }

   @Override
   public Component getNarrationMessage() {
      List<Component> var1 = Lists.newArrayListWithCapacity(2);
      if (this.title != null) {
         var1.add(this.title);
      }

      if (this.warning != null) {
         var1.addAll(Arrays.asList(this.warning));
      }

      return CommonComponents.joinLines(var1);
   }

   void updateButtonStates() {
      this.publisherButton.visible = this.shouldPublisherBeVisible();
      this.trailerButton.visible = this.shouldTrailerBeVisible();
      this.selectButton.active = this.shouldSelectButtonBeActive();
   }

   private boolean shouldSelectButtonBeActive() {
      return this.selectedTemplate != -1;
   }

   private boolean shouldPublisherBeVisible() {
      return this.selectedTemplate != -1 && !this.getSelectedTemplate().link.isEmpty();
   }

   private WorldTemplate getSelectedTemplate() {
      return this.worldTemplateObjectSelectionList.get(this.selectedTemplate);
   }

   private boolean shouldTrailerBeVisible() {
      return this.selectedTemplate != -1 && !this.getSelectedTemplate().trailer.isEmpty();
   }

   @Override
   public void tick() {
      super.tick();
      this.clicks--;
      if (this.clicks < 0) {
         this.clicks = 0;
      }
   }

   @Override
   public void onClose() {
      this.callback.accept(null);
   }

   void selectTemplate() {
      if (this.hasValidTemplate()) {
         this.callback.accept(this.getSelectedTemplate());
      }
   }

   private boolean hasValidTemplate() {
      return this.selectedTemplate >= 0 && this.selectedTemplate < this.worldTemplateObjectSelectionList.getItemCount();
   }

   private void onTrailer() {
      if (this.hasValidTemplate()) {
         WorldTemplate var1 = this.getSelectedTemplate();
         if (!"".equals(var1.trailer)) {
            Util.getPlatform().openUri(var1.trailer);
         }
      }
   }

   private void onPublish() {
      if (this.hasValidTemplate()) {
         WorldTemplate var1 = this.getSelectedTemplate();
         if (!"".equals(var1.link)) {
            Util.getPlatform().openUri(var1.link);
         }
      }
   }

   private void fetchTemplatesAsync(final WorldTemplatePaginatedList param0) {
      (new Thread("realms-template-fetcher") {
            @Override
            public void run() {
               WorldTemplatePaginatedList l_var1 = param0;
               RealmsClient l_var2 = RealmsClient.create();

               while (l_var1 != null) {
                  Either<WorldTemplatePaginatedList, String> l_var3 = RealmsSelectWorldTemplateScreen.this.fetchTemplates(l_var1, l_var2);
                  l_var1 = RealmsSelectWorldTemplateScreen.this.minecraft
                     .submit(
                        () -> {
                           if (l_var3.right().isPresent()) {
                              RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates: {}", l_var3.right().get());
                              if (RealmsSelectWorldTemplateScreen.this.worldTemplateObjectSelectionList.isEmpty()) {
                                 RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(I18n.get("mco.template.select.failure"));
                              }

                              return null;
                           } else {
                              WorldTemplatePaginatedList l2_var2 = l_var3.left().get();

                              for (WorldTemplate l2_var4 : l2_var2.templates) {
                                 RealmsSelectWorldTemplateScreen.this.worldTemplateObjectSelectionList.addEntry(l2_var4);
                              }

                              if (l2_var2.templates.isEmpty()) {
                                 if (RealmsSelectWorldTemplateScreen.this.worldTemplateObjectSelectionList.isEmpty()) {
                                    String l2_var3 = I18n.get("mco.template.select.none", "%link");
                                    TextRenderingUtils.LineSegment l2_var4 = TextRenderingUtils.LineSegment.link(
                                       I18n.get("mco.template.select.none.linkTitle"), "https://aka.ms/MinecraftRealmsContentCreator"
                                    );
                                    RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(l2_var3, l2_var4);
                                 }

                                 return null;
                              } else {
                                 return l2_var2;
                              }
                           }
                        }
                     )
                     .join();
               }
            }
         })
         .start();
   }

   Either<WorldTemplatePaginatedList, String> fetchTemplates(WorldTemplatePaginatedList param0, RealmsClient param1) {
      try {
         return Either.left(param1.fetchWorldTemplates(param0.page + 1, param0.size, this.worldType));
      } catch (RealmsServiceException var3) {
         return Either.right(var3.getMessage());
      }
   }

   @Override
   public void render(PoseStack param0, int param1, int param2, float param3) {
      this.toolTip = null;
      this.currentLink = null;
      this.hoverWarning = false;
      this.renderBackground(param0);
      this.worldTemplateObjectSelectionList.render(param0, param1, param2, param3);
      if (this.noTemplatesMessage != null) {
         this.renderMultilineMessage(param0, param1, param2, this.noTemplatesMessage);
      }

      drawCenteredString(param0, this.font, this.title, this.width / 2, 13, 16777215);
      if (this.displayWarning) {
         Component[] var5 = this.warning;

         for (int var6 = 0; var6 < var5.length; var6++) {
            int var7 = this.font.width(var5[var6]);
            int var8 = this.width / 2 - var7 / 2;
            int var9 = row(-1 + var6);
            if (param1 >= var8 && param1 <= var8 + var7 && param2 >= var9 && param2 <= var9 + 9) {
               this.hoverWarning = true;
            }
         }

         for (int var6 = 0; var6 < var5.length; var6++) {
            Component var7 = var5[var6];
            int var8 = 10526880;
            if (this.warningURL != null) {
               if (this.hoverWarning) {
                  var8 = 7107012;
                  var7 = var7.copy().withStyle(ChatFormatting.STRIKETHROUGH);
               } else {
                  var8 = 3368635;
               }
            }

            drawCenteredString(param0, this.font, var7, this.width / 2, row(-1 + var6), var8);
         }
      }

      super.render(param0, param1, param2, param3);
      this.renderMousehoverTooltip(param0, this.toolTip, param1, param2);
   }

   private void renderMultilineMessage(PoseStack param0, int param1, int param2, List<TextRenderingUtils.Line> param3) {
      for (int var5 = 0; var5 < param3.size(); var5++) {
         TextRenderingUtils.Line var6 = param3.get(var5);
         int var7 = row(4 + var5);
         int var8 = var6.segments.stream().mapToInt(l_param0 -> this.font.width(l_param0.renderedText())).sum();
         int var9 = this.width / 2 - var8 / 2;

         for (TextRenderingUtils.LineSegment var11 : var6.segments) {
            int var12 = var11.isLink() ? 3368635 : 16777215;
            int var13 = this.font.drawShadow(param0, var11.renderedText(), var9, var7, var12);
            if (var11.isLink() && param1 > var9 && param1 < var13 && param2 > var7 - 3 && param2 < var7 + 8) {
               this.toolTip = Component.literal(var11.getLinkUrl());
               this.currentLink = var11.getLinkUrl();
            }

            var9 = var13;
         }
      }
   }

   protected void renderMousehoverTooltip(PoseStack param0, @Nullable Component param1, int param2, int param3) {
      if (param1 != null) {
         int var5 = param2 + 12;
         int var6 = param3 - 12;
         int var7 = this.font.width(param1);
         fillGradient(param0, var5 - 3, var6 - 3, var5 + var7 + 3, var6 + 8 + 3, -1073741824, -1073741824);
         this.font.drawShadow(param0, param1, var5, var6, 16777215);
      }
   }

   class Entry extends ObjectSelectionList.Entry<RealmsSelectWorldTemplateScreen.Entry> {
      final WorldTemplate template;

      public Entry(WorldTemplate param1) {
         this.template = param1;
      }

      @Override
      public void render(PoseStack param0, int param1, int param2, int param3, int param4, int param5, int param6, int param7, boolean param8, float param9) {
         this.renderWorldTemplateItem(param0, this.template, param3, param2, param6, param7);
      }

      private void renderWorldTemplateItem(PoseStack param0, WorldTemplate param1, int param2, int param3, int param4, int param5) {
         int var7 = param2 + 45 + 20;
         RealmsSelectWorldTemplateScreen.this.font.draw(param0, param1.name, var7, param3 + 2, 16777215);
         RealmsSelectWorldTemplateScreen.this.font.draw(param0, param1.author, var7, param3 + 15, 7105644);
         RealmsSelectWorldTemplateScreen.this.font
            .draw(param0, param1.version, var7 + 227 - RealmsSelectWorldTemplateScreen.this.font.width(param1.version), param3 + 1, 7105644);
         if (!"".equals(param1.link) || !"".equals(param1.trailer) || !"".equals(param1.recommendedPlayers)) {
            this.drawIcons(param0, var7 - 1, param3 + 25, param4, param5, param1.link, param1.trailer, param1.recommendedPlayers);
         }

         this.drawImage(param0, param2, param3 + 1, param4, param5, param1);
      }

      private void drawImage(PoseStack param0, int param1, int param2, int param3, int param4, WorldTemplate param5) {
         RenderSystem.setShaderTexture(0, RealmsTextureManager.worldTemplate(param5.id, param5.image));
         GuiComponent.blit(param0, param1 + 1, param2 + 1, 0.0F, 0.0F, 38, 38, 38, 38);
         RenderSystem.setShaderTexture(0, RealmsSelectWorldTemplateScreen.SLOT_FRAME_LOCATION);
         GuiComponent.blit(param0, param1, param2, 0.0F, 0.0F, 40, 40, 40, 40);
      }

      private void drawIcons(PoseStack param0, int param1, int param2, int param3, int param4, String param5, String param6, String param7) {
         if (!"".equals(param7)) {
            RealmsSelectWorldTemplateScreen.this.font.draw(param0, param7, param1, param2 + 4, 5000268);
         }

         int var9 = "".equals(param7) ? 0 : RealmsSelectWorldTemplateScreen.this.font.width(param7) + 2;
         boolean var10 = false;
         boolean var11 = false;
         boolean var12 = "".equals(param5);
         if (param3 >= param1 + var9
            && param3 <= param1 + var9 + 32
            && param4 >= param2
            && param4 <= param2 + 15
            && param4 < RealmsSelectWorldTemplateScreen.this.height - 15
            && param4 > 32) {
            if (param3 <= param1 + 15 + var9 && param3 > var9) {
               if (var12) {
                  var11 = true;
               } else {
                  var10 = true;
               }
            } else if (!var12) {
               var11 = true;
            }
         }

         if (!var12) {
            RenderSystem.setShaderTexture(0, RealmsSelectWorldTemplateScreen.LINK_ICON);
            float var13 = var10 ? 15.0F : 0.0F;
            GuiComponent.blit(param0, param1 + var9, param2, var13, 0.0F, 15, 15, 30, 15);
         }

         if (!"".equals(param6)) {
            RenderSystem.setShaderTexture(0, RealmsSelectWorldTemplateScreen.TRAILER_ICON);
            int var13 = param1 + var9 + (var12 ? 0 : 17);
            float var14 = var11 ? 15.0F : 0.0F;
            GuiComponent.blit(param0, var13, param2, var14, 0.0F, 15, 15, 30, 15);
         }

         if (var10) {
            RealmsSelectWorldTemplateScreen.this.toolTip = RealmsSelectWorldTemplateScreen.PUBLISHER_LINK_TOOLTIP;
            RealmsSelectWorldTemplateScreen.this.currentLink = param5;
         } else if (var11 && !"".equals(param6)) {
            RealmsSelectWorldTemplateScreen.this.toolTip = RealmsSelectWorldTemplateScreen.TRAILER_LINK_TOOLTIP;
            RealmsSelectWorldTemplateScreen.this.currentLink = param6;
         }
      }

      @Override
      public Component getNarration() {
         Component var1 = CommonComponents.joinLines(
            Component.literal(this.template.name),
            Component.translatable("mco.template.select.narrate.authors", this.template.author),
            Component.literal(this.template.recommendedPlayers),
            Component.translatable("mco.template.select.narrate.version", this.template.version)
         );
         return Component.translatable("narrator.select", var1);
      }
   }

   class WorldTemplateObjectSelectionList extends RealmsObjectSelectionList<RealmsSelectWorldTemplateScreen.Entry> {
      public WorldTemplateObjectSelectionList() {
         this(Collections.emptyList());
      }

      public WorldTemplateObjectSelectionList(Iterable<WorldTemplate> param1) {
         super(
            RealmsSelectWorldTemplateScreen.this.width,
            RealmsSelectWorldTemplateScreen.this.height,
            RealmsSelectWorldTemplateScreen.this.displayWarning ? RealmsSelectWorldTemplateScreen.row(1) : 32,
            RealmsSelectWorldTemplateScreen.this.height - 40,
            46
         );
         param1.forEach(this::addEntry);
      }

      public void addEntry(WorldTemplate param0) {
         this.addEntry(RealmsSelectWorldTemplateScreen.this.new Entry(param0));
      }

      @Override
      public boolean mouseClicked(double param0, double param2, int param4) {
         if (param4 == 0 && param2 >= this.y0 && param2 <= this.y1) {
            int var6 = this.width / 2 - 150;
            if (RealmsSelectWorldTemplateScreen.this.currentLink != null) {
               Util.getPlatform().openUri(RealmsSelectWorldTemplateScreen.this.currentLink);
            }

            int var7 = (int)Math.floor(param2 - this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
            int var8 = var7 / this.itemHeight;
            if (param0 >= var6 && param0 < this.getScrollbarPosition() && var8 >= 0 && var7 >= 0 && var8 < this.getItemCount()) {
               this.selectItem(var8);
               this.itemClicked(var7, var8, param0, param2, this.width, param4);
               if (var8 >= RealmsSelectWorldTemplateScreen.this.worldTemplateObjectSelectionList.getItemCount()) {
                  return super.mouseClicked(param0, param2, param4);
               }

               RealmsSelectWorldTemplateScreen.this.clicks += 7;
               if (RealmsSelectWorldTemplateScreen.this.clicks >= 10) {
                  RealmsSelectWorldTemplateScreen.this.selectTemplate();
               }

               return true;
            }
         }

         return super.mouseClicked(param0, param2, param4);
      }

      public void setSelected(@Nullable RealmsSelectWorldTemplateScreen.Entry param0) {
         super.setSelected(param0);
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.children().indexOf(param0);
         RealmsSelectWorldTemplateScreen.this.updateButtonStates();
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 46;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }

      @Override
      public void renderBackground(PoseStack param0) {
         RealmsSelectWorldTemplateScreen.this.renderBackground(param0);
      }

      public boolean isEmpty() {
         return this.getItemCount() == 0;
      }

      public WorldTemplate get(int param0) {
         return this.children().get(param0).template;
      }

      public List<WorldTemplate> getTemplates() {
         return this.children().stream().map(l_param0 -> l_param0.template).collect(Collectors.toList());
      }
   }
}
