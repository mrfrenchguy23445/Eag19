package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class SingleFile implements SpriteSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<SingleFile> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(l_param0 -> l_param0.resourceId),
            ResourceLocation.CODEC.optionalFieldOf("sprite").forGetter(l_param0 -> l_param0.spriteId)
         )
         .apply(param0, SingleFile::new)
   );
   private final ResourceLocation resourceId;
   private final Optional<ResourceLocation> spriteId;

   public SingleFile(ResourceLocation param0, Optional<ResourceLocation> param1) {
      this.resourceId = param0;
      this.spriteId = param1;
   }

   @Override
   public void run(ResourceManager param0, SpriteSource.Output param1) {
      ResourceLocation var3 = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);
      Optional<Resource> var4 = param0.getResource(var3);
      if (var4.isPresent()) {
         param1.add(this.spriteId.orElse(this.resourceId), var4.get());
      } else {
         LOGGER.warn("Missing sprite: {}", var3);
      }
   }

   @Override
   public SpriteSourceType type() {
      return SpriteSources.SINGLE_FILE;
   }
}
