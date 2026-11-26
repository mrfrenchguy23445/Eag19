package net.minecraft.client.resources.metadata.texture;

public class TextureMetadataSection {
   public static final TextureMetadataSectionSerializer SERIALIZER = new TextureMetadataSectionSerializer();
   public static final boolean DEFAULT_BLUR = false;
   public static final boolean DEFAULT_CLAMP = false;
   private final boolean blur;
   private final boolean clamp;

   public TextureMetadataSection(boolean param0, boolean param1) {
      this.blur = param0;
      this.clamp = param1;
   }

   public boolean isBlur() {
      return this.blur;
   }

   public boolean isClamp() {
      return this.clamp;
   }
}
