package net.minecraft.commands;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;

public interface SharedSuggestionProvider {
   Collection<String> getOnlinePlayerNames();

   default Collection<String> getCustomTabSugggestions() {
      return this.getOnlinePlayerNames();
   }

   default Collection<String> getSelectedEntities() {
      return Collections.emptyList();
   }

   Collection<String> getAllTeams();

   Stream<ResourceLocation> getAvailableSounds();

   Stream<ResourceLocation> getRecipeNames();

   CompletableFuture<Suggestions> customSuggestion(CommandContext<?> var1);

   default Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   default Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   Set<ResourceKey<Level>> levels();

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default void suggestRegistryElements(Registry<?> param0, SharedSuggestionProvider.ElementSuggestionType param1, SuggestionsBuilder param2) {
      if (param1.shouldSuggestTags()) {
         suggestResource(param0.getTagNames().map(TagKey::location), param2, "#");
      }

      if (param1.shouldSuggestElements()) {
         suggestResource(param0.keySet(), param2);
      }
   }

   CompletableFuture<Suggestions> suggestRegistryElements(
      ResourceKey<? extends Registry<?>> var1, SharedSuggestionProvider.ElementSuggestionType var2, SuggestionsBuilder var3, CommandContext<?> var4
   );

   boolean hasPermission(int var1);

   static <T> void filterResources(Iterable<T> param0, String param1, Function<T, ResourceLocation> param2, Consumer<T> param3) {
      boolean var4 = param1.indexOf(58) > -1;

      for (T var6 : param0) {
         ResourceLocation var7 = param2.apply(var6);
         if (var4) {
            String var8 = var7.toString();
            if (matchesSubStr(param1, var8)) {
               param3.accept(var6);
            }
         } else if (matchesSubStr(param1, var7.getNamespace()) || var7.getNamespace().equals("minecraft") && matchesSubStr(param1, var7.getPath())) {
            param3.accept(var6);
         }
      }
   }

   static <T> void filterResources(Iterable<T> param0, String param1, String param2, Function<T, ResourceLocation> param3, Consumer<T> param4) {
      if (param1.isEmpty()) {
         param0.forEach(param4);
      } else {
         String var5 = Strings.commonPrefix(param1, param2);
         if (!var5.isEmpty()) {
            String var6 = param1.substring(var5.length());
            filterResources(param0, var6, param3, param4);
         }
      }
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<ResourceLocation> param0, SuggestionsBuilder param1, String param2) {
      String var3 = param1.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(param0, var3, param2, l_param0 -> l_param0, l_param2 -> param1.suggest(param2 + l_param2));
      return param1.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<ResourceLocation> param0, SuggestionsBuilder param1, String param2) {
      return suggestResource(param0::iterator, param1, param2);
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<ResourceLocation> param0, SuggestionsBuilder param1) {
      String var2 = param1.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(param0, var2, l_param0 -> l_param0, l_param1 -> param1.suggest(l_param1.toString()));
      return param1.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Iterable<T> param0, SuggestionsBuilder param1, Function<T, ResourceLocation> param2, Function<T, Message> param3
   ) {
      String var4 = param1.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(param0, var4, param2, l_param3 -> param1.suggest(param2.apply(l_param3).toString(), param3.apply(l_param3)));
      return param1.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<ResourceLocation> param0, SuggestionsBuilder param1) {
      return suggestResource(param0::iterator, param1);
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Stream<T> param0, SuggestionsBuilder param1, Function<T, ResourceLocation> param2, Function<T, Message> param3
   ) {
      return suggestResource(param0::iterator, param1, param2, param3);
   }

   static CompletableFuture<Suggestions> suggestCoordinates(
      String param0, Collection<SharedSuggestionProvider.TextCoordinates> param1, SuggestionsBuilder param2, Predicate<String> param3
   ) {
      List<String> var4 = Lists.newArrayList();
      if (Strings.isNullOrEmpty(param0)) {
         for (SharedSuggestionProvider.TextCoordinates var6 : param1) {
            String var7 = var6.x + " " + var6.y + " " + var6.z;
            if (param3.test(var7)) {
               var4.add(var6.x);
               var4.add(var6.x + " " + var6.y);
               var4.add(var7);
            }
         }
      } else {
         String[] var5 = param0.split(" ");
         if (var5.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates var7 : param1) {
               String var8 = var5[0] + " " + var7.y + " " + var7.z;
               if (param3.test(var8)) {
                  var4.add(var5[0] + " " + var7.y);
                  var4.add(var8);
               }
            }
         } else if (var5.length == 2) {
            for (SharedSuggestionProvider.TextCoordinates var7 : param1) {
               String var8 = var5[0] + " " + var5[1] + " " + var7.z;
               if (param3.test(var8)) {
                  var4.add(var8);
               }
            }
         }
      }

      return suggest(var4, param2);
   }

   static CompletableFuture<Suggestions> suggest2DCoordinates(
      String param0, Collection<SharedSuggestionProvider.TextCoordinates> param1, SuggestionsBuilder param2, Predicate<String> param3
   ) {
      List<String> var4 = Lists.newArrayList();
      if (Strings.isNullOrEmpty(param0)) {
         for (SharedSuggestionProvider.TextCoordinates var6 : param1) {
            String var7 = var6.x + " " + var6.z;
            if (param3.test(var7)) {
               var4.add(var6.x);
               var4.add(var7);
            }
         }
      } else {
         String[] var5 = param0.split(" ");
         if (var5.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates var7 : param1) {
               String var8 = var5[0] + " " + var7.z;
               if (param3.test(var8)) {
                  var4.add(var8);
               }
            }
         }
      }

      return suggest(var4, param2);
   }

   static CompletableFuture<Suggestions> suggest(Iterable<String> param0, SuggestionsBuilder param1) {
      String var2 = param1.getRemaining().toLowerCase(Locale.ROOT);

      for (String var4 : param0) {
         if (matchesSubStr(var2, var4.toLowerCase(Locale.ROOT))) {
            param1.suggest(var4);
         }
      }

      return param1.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(Stream<String> param0, SuggestionsBuilder param1) {
      String var2 = param1.getRemaining().toLowerCase(Locale.ROOT);
      param0.filter(l_param1 -> matchesSubStr(var2, l_param1.toLowerCase(Locale.ROOT))).forEach(param1::suggest);
      return param1.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(String[] param0, SuggestionsBuilder param1) {
      String var2 = param1.getRemaining().toLowerCase(Locale.ROOT);

      for (String var6 : param0) {
         if (matchesSubStr(var2, var6.toLowerCase(Locale.ROOT))) {
            param1.suggest(var6);
         }
      }

      return param1.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggest(Iterable<T> param0, SuggestionsBuilder param1, Function<T, String> param2, Function<T, Message> param3) {
      String var4 = param1.getRemaining().toLowerCase(Locale.ROOT);

      for (T var6 : param0) {
         String var7 = param2.apply(var6);
         if (matchesSubStr(var4, var7.toLowerCase(Locale.ROOT))) {
            param1.suggest(var7, param3.apply(var6));
         }
      }

      return param1.buildFuture();
   }

   static boolean matchesSubStr(String param0, String param1) {
      for (int var2 = 0; !param1.startsWith(param0, var2); var2++) {
         var2 = param1.indexOf(95, var2);
         if (var2 < 0) {
            return false;
         }
      }

      return true;
   }

   enum ElementSuggestionType {
      TAGS,
      ELEMENTS,
      ALL;

      public boolean shouldSuggestTags() {
         return this == TAGS || this == ALL;
      }

      public boolean shouldSuggestElements() {
         return this == ELEMENTS || this == ALL;
      }
   }

   class TextCoordinates {
      public static final SharedSuggestionProvider.TextCoordinates DEFAULT_LOCAL = new SharedSuggestionProvider.TextCoordinates("^", "^", "^");
      public static final SharedSuggestionProvider.TextCoordinates DEFAULT_GLOBAL = new SharedSuggestionProvider.TextCoordinates("~", "~", "~");
      public final String x;
      public final String y;
      public final String z;

      public TextCoordinates(String param0, String param1, String param2) {
         this.x = param0;
         this.y = param1;
         this.z = param2;
      }
   }
}
