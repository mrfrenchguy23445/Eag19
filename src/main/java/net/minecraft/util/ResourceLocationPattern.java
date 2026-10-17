package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationPattern {
   public static final Codec<ResourceLocationPattern> CODEC = RecordCodecBuilder.create(
      param0 -> param0.group(
            ExtraCodecs.PATTERN.optionalFieldOf("namespace").forGetter(l_param0 -> l_param0.namespacePattern),
            ExtraCodecs.PATTERN.optionalFieldOf("path").forGetter(l_param0 -> l_param0.pathPattern)
         )
         .apply(param0, ResourceLocationPattern::new)
   );
   private final Optional<Pattern> namespacePattern;
   private final Predicate<String> namespacePredicate;
   private final Optional<Pattern> pathPattern;
   private final Predicate<String> pathPredicate;
   private final Predicate<ResourceLocation> locationPredicate;

   private ResourceLocationPattern(Optional<Pattern> param0, Optional<Pattern> param1) {
      this.namespacePattern = param0;
      this.namespacePredicate = param0.map(ResourceLocationPattern::matchesFully).orElse(l_param0 -> true);
      this.pathPattern = param1;
      this.pathPredicate = param1.map(ResourceLocationPattern::matchesFully).orElse(l_param0 -> true);
      this.locationPredicate = l_param0 -> this.namespacePredicate.test(l_param0.getNamespace()) && this.pathPredicate.test(l_param0.getPath());
   }

   /**
    * What {@link Pattern#asPredicate()} is, written out.
    *
    * <p>The JDK defines asPredicate as {@code s -> matcher(s).matches()}, so this is the
    * definition rather than an approximation of it -- including {@code matches()} rather
    * than {@code find()}, which is the part that would actually change behaviour if it
    * were got wrong. A namespace or path pattern that matched only a substring of its
    * input would admit resource locations that vanilla rejects.
    *
    * <p>{@code asPredicate} itself is absent from the WASM-GC TeaVM classlib's Pattern,
    * which models compile and a subset of the Matcher API but no method reference
    * factories.
    */
   private static Predicate<String> matchesFully(Pattern param0) {
      return param1 -> param0.matcher(param1).matches();
   }

   public Predicate<String> namespacePredicate() {
      return this.namespacePredicate;
   }

   public Predicate<String> pathPredicate() {
      return this.pathPredicate;
   }

   public Predicate<ResourceLocation> locationPredicate() {
      return this.locationPredicate;
   }
}
