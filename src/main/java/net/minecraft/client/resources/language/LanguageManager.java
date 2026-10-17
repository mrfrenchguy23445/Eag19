package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.language.LanguageMetadataSection;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

public class LanguageManager implements ResourceManagerReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String DEFAULT_LANGUAGE_CODE = "en_us";
   private static final LanguageInfo DEFAULT_LANGUAGE = new LanguageInfo("US", "English", false);
   private Map<String, LanguageInfo> languages = ImmutableMap.of("en_us", DEFAULT_LANGUAGE);
   private String currentCode;

   public LanguageManager(String param0) {
      this.currentCode = param0;
   }

   private static Map<String, LanguageInfo> extractLanguages(Stream<PackResources> param0) {
      Map<String, LanguageInfo> var1 = Maps.newHashMap();
      param0.forEach(l_param1 -> {
         try {
            LanguageMetadataSection l_var2 = l_param1.getMetadataSection(LanguageMetadataSection.TYPE);
            if (l_var2 != null) {
               l_var2.languages().forEach(var1::putIfAbsent);
            }
         } catch (RuntimeException | IOException l_var2) {
            LOGGER.warn("Unable to parse language metadata section of resourcepack: {}", l_param1.packId(), l_var2);
         }
      });
      return ImmutableMap.copyOf(var1);
   }

   @Override
   public void onResourceManagerReload(ResourceManager param0) {
      this.languages = extractLanguages(param0.listPacks());
      List<String> var2 = new ArrayList<>(2);
      boolean var3 = DEFAULT_LANGUAGE.bidirectional();
      var2.add("en_us");
      if (!this.currentCode.equals("en_us")) {
         LanguageInfo var4 = this.languages.get(this.currentCode);
         if (var4 != null) {
            var2.add(this.currentCode);
            var3 = var4.bidirectional();
         }
      }

      ClientLanguage var4 = ClientLanguage.loadFrom(param0, var2, var3);
      I18n.setLanguage(var4);
      Language.inject(var4);
   }

   public void setSelected(String param0) {
      this.currentCode = param0;
   }

   public String getSelected() {
      return this.currentCode;
   }

   public SortedMap<String, LanguageInfo> getLanguages() {
      return new TreeMap<>(this.languages);
   }

   @Nullable
   public LanguageInfo getLanguage(String param0) {
      return this.languages.get(param0);
   }
}
