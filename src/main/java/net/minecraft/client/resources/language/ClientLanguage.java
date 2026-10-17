package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, String> storage;
   private final boolean defaultRightToLeft;

   private ClientLanguage(Map<String, String> param0, boolean param1) {
      this.storage = param0;
      this.defaultRightToLeft = param1;
   }

   public static ClientLanguage loadFrom(ResourceManager param0, List<String> param1, boolean param2) {
      Map<String, String> var3 = Maps.newHashMap();

      for (String var5 : param1) {
         String var6 = String.format(Locale.ROOT, "lang/%s.json", var5);

         for (String var8 : param0.getNamespaces()) {
            try {
               ResourceLocation var9 = new ResourceLocation(var8, var6);
               appendFrom(var5, param0.getResourceStack(var9), var3);
            } catch (Exception var9) {
               LOGGER.warn("Skipped language file: {}:{} ({})", var8, var6, var9.toString());
            }
         }
      }

      return new ClientLanguage(ImmutableMap.copyOf(var3), param2);
   }

   private static void appendFrom(String param0, List<Resource> param1, Map<String, String> param2) {
      for (Resource var4 : param1) {
         try (InputStream var5 = var4.open()) {
            Language.loadFromJson(var5, param2::put);
         } catch (IOException var5) {
            LOGGER.warn("Failed to load translations for {} from pack {}", param0, var4.sourcePackId(), var5);
         }
      }
   }

   @Override
   public String getOrDefault(String param0, String param1) {
      return this.storage.getOrDefault(param0, param1);
   }

   @Override
   public boolean has(String param0) {
      return this.storage.containsKey(param0);
   }

   @Override
   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   @Override
   public FormattedCharSequence getVisualOrder(FormattedText param0) {
      return FormattedBidiReorder.reorder(param0, this.defaultRightToLeft);
   }
}
