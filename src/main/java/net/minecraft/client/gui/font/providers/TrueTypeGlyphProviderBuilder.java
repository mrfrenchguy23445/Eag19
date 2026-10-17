package net.minecraft.client.gui.font.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

/**
 * Port note (LWJGL removal): this provider is refused, not ported.
 *
 * <p>Every method that used to do work drove {@code org.lwjgl.stb.STBTruetype} against a
 * native {@code STBTTFontinfo}: {@code stbtt_InitFont}, {@code stbtt_ScaleForPixelHeight},
 * {@code stbtt_GetFontVMetrics}, {@code stbtt_FindGlyphIndex},
 * {@code stbtt_GetGlyphHMetrics}, {@code stbtt_GetGlyphBitmapBoxSubpixel} and
 * {@code nstbtt_MakeGlyphBitmapSubpixel} (via NativeImage.copyFromFont). That is a TrueType
 * rasteriser. There is none in the TeaVM classlib, no stb_truetype to bind to, and writing
 * one is a project in itself -- hinting, glyph outlines, scan conversion.
 *
 * <p>The refusal is safe for reaching the main menu and loading a world: no font shipped in
 * the vanilla asset set declares a {@code "ttf"} provider. {@code font/default.json} uses
 * only {@code space}, {@code bitmap} and {@code legacy_unicode}, and the alt/illageralt/
 * uniform sets are likewise bitmap-only. A {@code ttf} provider only appears if a resource
 * pack asks for one, and in that case returning null lets the font manager fall back to the
 * next provider exactly as it does for any other unreadable font.
 *
 * <p>The JSON parsing above is kept intact so the error, when it happens, names the file and
 * the offending settings rather than being an "unknown provider type".
 */
public class TrueTypeGlyphProviderBuilder implements GlyphProviderBuilder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation location;
   private final float size;
   private final float oversample;
   private final float shiftX;
   private final float shiftY;
   private final String skip;

   public TrueTypeGlyphProviderBuilder(ResourceLocation param0, float param1, float param2, float param3, float param4, String param5) {
      this.location = param0;
      this.size = param1;
      this.oversample = param2;
      this.shiftX = param3;
      this.shiftY = param4;
      this.skip = param5;
   }

   public static GlyphProviderBuilder fromJson(JsonObject param0) {
      float var1 = 0.0F;
      float var2 = 0.0F;
      if (param0.has("shift")) {
         JsonArray var3 = param0.getAsJsonArray("shift");
         if (var3.size() != 2) {
            throw new JsonParseException("Expected 2 elements in 'shift', found " + var3.size());
         }

         var1 = GsonHelper.convertToFloat(var3.get(0), "shift[0]");
         var2 = GsonHelper.convertToFloat(var3.get(1), "shift[1]");
      }

      StringBuilder var3 = new StringBuilder();
      if (param0.has("skip")) {
         JsonElement var4 = param0.get("skip");
         if (var4.isJsonArray()) {
            JsonArray var5 = GsonHelper.convertToJsonArray(var4, "skip");

            for (int var6 = 0; var6 < var5.size(); var6++) {
               var3.append(GsonHelper.convertToString(var5.get(var6), "skip[" + var6 + "]"));
            }
         } else {
            var3.append(GsonHelper.convertToString(var4, "skip"));
         }
      }

      return new TrueTypeGlyphProviderBuilder(
         new ResourceLocation(GsonHelper.getAsString(param0, "file")),
         GsonHelper.getAsFloat(param0, "size", 11.0F),
         GsonHelper.getAsFloat(param0, "oversample", 1.0F),
         var1,
         var2,
         var3.toString()
      );
   }

   @Nullable
   @Override
   public GlyphProvider create(ResourceManager param0) {
      // Logged at warn, not error, and returned as null rather than thrown: a pack using a
      // ttf provider then loses that one provider instead of the whole font set.
      LOGGER.warn(
         "Ignoring TrueType font {}: this port has no TrueType rasteriser. Minecraft's own"
            + " fonts are bitmap-only, so this only affects resource packs that request a ttf"
            + " provider; swap it for a bitmap provider or use the vanilla fonts.",
         this.location
      );
      return null;
   }
}
