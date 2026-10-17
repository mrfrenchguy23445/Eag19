package com.mojang.blaze3d.font;

/**
 * Port note (LWJGL removal): refused, not ported. See TrueTypeGlyphProviderBuilder for the
 * full reasoning; the short version is that this class was a thin wrapper over
 * {@code org.lwjgl.stb.STBTruetype}, and neither the rasteriser nor the native
 * {@code STBTTFontinfo} handle exists on this target.
 *
 * <p>The class is retained, and still constructible, purely so that any code referencing it
 * compiles; the constructor throws, and the builder that used to construct it now returns
 * null without trying. Nothing in the vanilla font set reaches this type, so nothing reaches
 * the throw either.
 */
public class TrueTypeGlyphProvider implements GlyphProvider {
   public TrueTypeGlyphProvider() {
      throw new UnsupportedOperationException(
         "TrueTypeGlyphProvider is not available on this target: it requires the stb_truetype"
            + " rasteriser, which this port does not include. No vanilla font uses a ttf"
            + " provider; resource packs that do will have that provider skipped."
      );
   }

   @Override
   public com.mojang.blaze3d.font.GlyphInfo getGlyph(int param0) {
      throw new UnsupportedOperationException("unreachable: the constructor always throws");
   }

   @Override
   public void close() {
   }

   @Override
   public it.unimi.dsi.fastutil.ints.IntSet getSupportedGlyphs() {
      return it.unimi.dsi.fastutil.ints.IntSets.emptySet();
   }
}
