package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import jdk_internal.bidi.Bidi;
import jdk_internal.icu.lang.UCharacter;
import net.lax1dude.eaglercraft.v1_8.EaglerBidiReorder;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Font {
   private static final float EFFECT_DEPTH = 0.01F;
   private static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, 0.03F);
   public static final int ALPHA_CUTOFF = 8;
   public final int lineHeight = 9;
   public final RandomSource random = RandomSource.create();
   private final Function<ResourceLocation, FontSet> fonts;
   final boolean filterFishyGlyphs;
   private final StringSplitter splitter;

   public Font(Function<ResourceLocation, FontSet> param0, boolean param1) {
      this.fonts = param0;
      this.filterFishyGlyphs = param1;
      this.splitter = new StringSplitter(
         (l_param0, l_param1) -> this.getFontSet(l_param1.getFont()).getGlyphInfo(l_param0, this.filterFishyGlyphs).getAdvance(l_param1.isBold())
      );
   }

   FontSet getFontSet(ResourceLocation param0) {
      return this.fonts.apply(param0);
   }

   public int drawShadow(PoseStack param0, String param1, float param2, float param3, int param4) {
      return this.drawInternal(param1, param2, param3, param4, param0.last().pose(), true, this.isBidirectional());
   }

   public int drawShadow(PoseStack param0, String param1, float param2, float param3, int param4, boolean param5) {
      return this.drawInternal(param1, param2, param3, param4, param0.last().pose(), true, param5);
   }

   public int draw(PoseStack param0, String param1, float param2, float param3, int param4) {
      return this.drawInternal(param1, param2, param3, param4, param0.last().pose(), false, this.isBidirectional());
   }

   public int drawShadow(PoseStack param0, FormattedCharSequence param1, float param2, float param3, int param4) {
      return this.drawInternal(param1, param2, param3, param4, param0.last().pose(), true);
   }

   public int drawShadow(PoseStack param0, Component param1, float param2, float param3, int param4) {
      return this.drawInternal(param1.getVisualOrderText(), param2, param3, param4, param0.last().pose(), true);
   }

   public int draw(PoseStack param0, FormattedCharSequence param1, float param2, float param3, int param4) {
      return this.drawInternal(param1, param2, param3, param4, param0.last().pose(), false);
   }

   public int draw(PoseStack param0, Component param1, float param2, float param3, int param4) {
      return this.drawInternal(param1.getVisualOrderText(), param2, param3, param4, param0.last().pose(), false);
   }

   /**
    * Reorders {@code param0} for display, right-to-left runs reversed and mirrored.
    *
    * <p>This was three icu4j calls: shape the Arabic, run the bidi algorithm, and write the
    * result out with mirroring on. All three now come from the bidi vendored in this
    * repository under {@code jdk_internal.bidi}, which is ICU's own code and is what
    * {@code EaglerBidiReorder} has always used.
    *
    * <p>What changed, and it is one thing: the Arabic shaping step is gone. See
    * {@code FormattedBidiReorder.reorder} for the full note -- there is no source for it to
    * be built from, it was the only remaining reason to keep icu4j on the linker, and both
    * call sites already had a fallback to the unshaped string. The consequence is that Arabic
    * letters are drawn in isolated forms rather than joined; the text is still ordered and
    * still mirrored correctly, so it reads in the right order.
    *
    * <p>The two dropped calls were lossless in the ways they look lossy in:
    * {@code setReorderingMode(0)} set the mode to its own default, which is what a freshly
    * constructed {@code Bidi} already has; and {@code writeReordered(2)} is
    * {@code DO_MIRRORING}, which is applied by the odd-level branch below through
    * {@link UCharacter#getMirror(int)} -- the same function the formatted path hands to
    * {@code SubStringSource}.
    */
   public String bidirectionalShaping(String param0) {
      Bidi var2 = new Bidi(param0, 127);

      // visualRuns gives {start, limit, level} per run in visual order; the level comes back
      // permuted with the runs, so var4[2] is the level of the run at that visual position.
      int[][] var3 = EaglerBidiReorder.visualRuns(var2);
      StringBuilder var4 = new StringBuilder(param0.length());

      for (int[] var5 : var3) {
         if ((var5[2] & 1) == 0) {
            var4.append(param0, var5[0], var5[1]);
         } else {
            // Reversed and mirrored. Reversal walks code points rather than chars, because a
            // char-at-a-time reverse would put the halves of a surrogate pair in the wrong
            // order and turn one astral character into two invalid ones. That costs an int
            // array per right-to-left run, and right-to-left runs are the rarer case.
            int var6 = var5[1];
            int[] var7 = new int[var6 - var5[0]];
            int var8 = 0;

            while (var6 > var5[0]) {
               int var9 = param0.offsetByCodePoints(var6, -1);
               var7[var8++] = UCharacter.getMirror(param0.codePointAt(var9));
               var6 = var9;
            }

            for (int var10 = var8 - 1; var10 >= 0; var10--) {
               var4.appendCodePoint(var7[var10]);
            }
         }
      }

      return var4.toString();
   }

   private int drawInternal(String param0, float param1, float param2, int param3, Matrix4f param4, boolean param5, boolean param6) {
      if (param0 == null) {
         return 0;
      }

      MultiBufferSource.BufferSource var8 = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
      int var9 = this.drawInBatch(param0, param1, param2, param3, param5, param4, var8, Font.DisplayMode.NORMAL, 0, 15728880, param6);
      var8.endBatch();
      return var9;
   }

   private int drawInternal(FormattedCharSequence param0, float param1, float param2, int param3, Matrix4f param4, boolean param5) {
      MultiBufferSource.BufferSource var7 = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
      int var8 = this.drawInBatch(param0, param1, param2, param3, param5, param4, var7, Font.DisplayMode.NORMAL, 0, 15728880);
      var7.endBatch();
      return var8;
   }

   public int drawInBatch(
      String param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      return this.drawInBatch(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9, this.isBidirectional());
   }

   public int drawInBatch(
      String param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9,
      boolean param10
   ) {
      return this.drawInternal(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
   }

   public int drawInBatch(
      Component param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      return this.drawInBatch(param0.getVisualOrderText(), param1, param2, param3, param4, param5, param6, param7, param8, param9);
   }

   public int drawInBatch(
      FormattedCharSequence param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      return this.drawInternal(param0, param1, param2, param3, param4, param5, param6, param7, param8, param9);
   }

   public void drawInBatch8xOutline(
      FormattedCharSequence param0, float param1, float param2, int param3, int param4, Matrix4f param5, MultiBufferSource param6, int param7
   ) {
      int var9 = adjustColor(param4);
      Font.StringRenderOutput var10 = new Font.StringRenderOutput(param6, 0.0F, 0.0F, var9, false, param5, Font.DisplayMode.NORMAL, param7);

      for (int var11 = -1; var11 <= 1; var11++) {
         for (int var12 = -1; var12 <= 1; var12++) {
            if (var11 != 0 || var12 != 0) {
               float[] var13 = new float[]{param1};
               int var14 = var11;
               int var15 = var12;
               param0.accept((l_param6, l_param7, l_param8) -> {
                  boolean l_var10 = l_param7.isBold();
                  FontSet l_var11 = this.getFontSet(l_param7.getFont());
                  GlyphInfo l_var12 = l_var11.getGlyphInfo(l_param8, this.filterFishyGlyphs);
                  var10.x = var13[0] + var14 * l_var12.getShadowOffset();
                  var10.y = param2 + var15 * l_var12.getShadowOffset();
                  var13[0] += l_var12.getAdvance(l_var10);
                  return var10.accept(l_param6, l_param7.withColor(var9), l_param8);
               });
            }
         }
      }

      Font.StringRenderOutput var11 = new Font.StringRenderOutput(
         param6, param1, param2, adjustColor(param3), false, param5, Font.DisplayMode.POLYGON_OFFSET, param7
      );
      param0.accept(var11);
      var11.finish(0, param1);
   }

   private static int adjustColor(int param0) {
      return (param0 & -67108864) == 0 ? param0 | 0xFF000000 : param0;
   }

   private int drawInternal(
      String param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9,
      boolean param10
   ) {
      if (param10) {
         param0 = this.bidirectionalShaping(param0);
      }

      param3 = adjustColor(param3);
      Matrix4f var12 = new Matrix4f(param5);
      if (param4) {
         this.renderText(param0, param1, param2, param3, true, param5, param6, param7, param8, param9);
         var12.translate(SHADOW_OFFSET);
      }

      param1 = this.renderText(param0, param1, param2, param3, false, var12, param6, param7, param8, param9);
      return (int)param1 + (param4 ? 1 : 0);
   }

   private int drawInternal(
      FormattedCharSequence param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      param3 = adjustColor(param3);
      Matrix4f var11 = new Matrix4f(param5);
      if (param4) {
         this.renderText(param0, param1, param2, param3, true, param5, param6, param7, param8, param9);
         var11.translate(SHADOW_OFFSET);
      }

      param1 = this.renderText(param0, param1, param2, param3, false, var11, param6, param7, param8, param9);
      return (int)param1 + (param4 ? 1 : 0);
   }

   private float renderText(
      String param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      Font.StringRenderOutput var11 = new Font.StringRenderOutput(param6, param1, param2, param3, param4, param5, param7, param9);
      StringDecomposer.iterateFormatted(param0, Style.EMPTY, var11);
      return var11.finish(param8, param1);
   }

   private float renderText(
      FormattedCharSequence param0,
      float param1,
      float param2,
      int param3,
      boolean param4,
      Matrix4f param5,
      MultiBufferSource param6,
      Font.DisplayMode param7,
      int param8,
      int param9
   ) {
      Font.StringRenderOutput var11 = new Font.StringRenderOutput(param6, param1, param2, param3, param4, param5, param7, param9);
      param0.accept(var11);
      return var11.finish(param8, param1);
   }

   void renderChar(
      BakedGlyph param0,
      boolean param1,
      boolean param2,
      float param3,
      float param4,
      float param5,
      Matrix4f param6,
      VertexConsumer param7,
      float param8,
      float param9,
      float param10,
      float param11,
      int param12
   ) {
      param0.render(param2, param4, param5, param6, param7, param8, param9, param10, param11, param12);
      if (param1) {
         param0.render(param2, param4 + param3, param5, param6, param7, param8, param9, param10, param11, param12);
      }
   }

   public int width(String param0) {
      return Mth.ceil(this.splitter.stringWidth(param0));
   }

   public int width(FormattedText param0) {
      return Mth.ceil(this.splitter.stringWidth(param0));
   }

   public int width(FormattedCharSequence param0) {
      return Mth.ceil(this.splitter.stringWidth(param0));
   }

   public String plainSubstrByWidth(String param0, int param1, boolean param2) {
      return param2 ? this.splitter.plainTailByWidth(param0, param1, Style.EMPTY) : this.splitter.plainHeadByWidth(param0, param1, Style.EMPTY);
   }

   public String plainSubstrByWidth(String param0, int param1) {
      return this.splitter.plainHeadByWidth(param0, param1, Style.EMPTY);
   }

   public FormattedText substrByWidth(FormattedText param0, int param1) {
      return this.splitter.headByWidth(param0, param1, Style.EMPTY);
   }

   public void drawWordWrap(PoseStack param0, FormattedText param1, int param2, int param3, int param4, int param5) {
      Matrix4f var7 = param0.last().pose();

      for (FormattedCharSequence var9 : this.split(param1, param4)) {
         this.drawInternal(var9, param2, param3, param5, var7, false);
         param3 += 9;
      }
   }

   public int wordWrapHeight(String param0, int param1) {
      return 9 * this.splitter.splitLines(param0, param1, Style.EMPTY).size();
   }

   public int wordWrapHeight(FormattedText param0, int param1) {
      return 9 * this.splitter.splitLines(param0, param1, Style.EMPTY).size();
   }

   public List<FormattedCharSequence> split(FormattedText param0, int param1) {
      return Language.getInstance().getVisualOrder(this.splitter.splitLines(param0, param1, Style.EMPTY));
   }

   public boolean isBidirectional() {
      return Language.getInstance().isDefaultRightToLeft();
   }

   public StringSplitter getSplitter() {
      return this.splitter;
   }

   public enum DisplayMode {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }

   class StringRenderOutput implements FormattedCharSink {
      final MultiBufferSource bufferSource;
      private final boolean dropShadow;
      private final float dimFactor;
      private final float r;
      private final float g;
      private final float b;
      private final float a;
      private final Matrix4f pose;
      private final Font.DisplayMode mode;
      private final int packedLightCoords;
      float x;
      float y;
      @Nullable
      private List<BakedGlyph.Effect> effects;

      private void addEffect(BakedGlyph.Effect param0) {
         if (this.effects == null) {
            this.effects = Lists.newArrayList();
         }

         this.effects.add(param0);
      }

      public StringRenderOutput(
         MultiBufferSource param1, float param2, float param3, int param4, boolean param5, Matrix4f param6, Font.DisplayMode param7, int param8
      ) {
         this.bufferSource = param1;
         this.x = param2;
         this.y = param3;
         this.dropShadow = param5;
         this.dimFactor = param5 ? 0.25F : 1.0F;
         this.r = (param4 >> 16 & 0xFF) / 255.0F * this.dimFactor;
         this.g = (param4 >> 8 & 0xFF) / 255.0F * this.dimFactor;
         this.b = (param4 & 0xFF) / 255.0F * this.dimFactor;
         this.a = (param4 >> 24 & 0xFF) / 255.0F;
         this.pose = param6;
         this.mode = param7;
         this.packedLightCoords = param8;
      }

      @Override
      public boolean accept(int param0, Style param1, int param2) {
         FontSet var4 = Font.this.getFontSet(param1.getFont());
         GlyphInfo var5 = var4.getGlyphInfo(param2, Font.this.filterFishyGlyphs);
         BakedGlyph var6 = param1.isObfuscated() && param2 != 32 ? var4.getRandomGlyph(var5) : var4.getGlyph(param2);
         boolean var7 = param1.isBold();
         float var11 = this.a;
         TextColor var12 = param1.getColor();
         float var8;
         float var9;
         float var10;
         if (var12 != null) {
            int var13 = var12.getValue();
            var8 = (var13 >> 16 & 0xFF) / 255.0F * this.dimFactor;
            var9 = (var13 >> 8 & 0xFF) / 255.0F * this.dimFactor;
            var10 = (var13 & 0xFF) / 255.0F * this.dimFactor;
         } else {
            var8 = this.r;
            var9 = this.g;
            var10 = this.b;
         }

         if (!(var6 instanceof EmptyGlyph)) {
            float var13 = var7 ? var5.getBoldOffset() : 0.0F;
            float var14 = this.dropShadow ? var5.getShadowOffset() : 0.0F;
            VertexConsumer var15 = this.bufferSource.getBuffer(var6.renderType(this.mode));
            Font.this.renderChar(
               var6, var7, param1.isItalic(), var13, this.x + var14, this.y + var14, this.pose, var15, var8, var9, var10, var11, this.packedLightCoords
            );
         }

         float var13 = var5.getAdvance(var7);
         float var14 = this.dropShadow ? 1.0F : 0.0F;
         if (param1.isStrikethrough()) {
            this.addEffect(
               new BakedGlyph.Effect(
                  this.x + var14 - 1.0F, this.y + var14 + 4.5F, this.x + var14 + var13, this.y + var14 + 4.5F - 1.0F, 0.01F, var8, var9, var10, var11
               )
            );
         }

         if (param1.isUnderlined()) {
            this.addEffect(
               new BakedGlyph.Effect(
                  this.x + var14 - 1.0F, this.y + var14 + 9.0F, this.x + var14 + var13, this.y + var14 + 9.0F - 1.0F, 0.01F, var8, var9, var10, var11
               )
            );
         }

         this.x += var13;
         return true;
      }

      public float finish(int param0, float param1) {
         if (param0 != 0) {
            float var3 = (param0 >> 24 & 0xFF) / 255.0F;
            float var4 = (param0 >> 16 & 0xFF) / 255.0F;
            float var5 = (param0 >> 8 & 0xFF) / 255.0F;
            float var6 = (param0 & 0xFF) / 255.0F;
            this.addEffect(new BakedGlyph.Effect(param1 - 1.0F, this.y + 9.0F, this.x + 1.0F, this.y - 1.0F, 0.01F, var4, var5, var6, var3));
         }

         if (this.effects != null) {
            BakedGlyph var3 = Font.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
            VertexConsumer var4 = this.bufferSource.getBuffer(var3.renderType(this.mode));

            for (BakedGlyph.Effect var6 : this.effects) {
               var3.renderEffect(var6, this.pose, var4, this.packedLightCoords);
            }
         }

         return this.x;
      }
   }
}
