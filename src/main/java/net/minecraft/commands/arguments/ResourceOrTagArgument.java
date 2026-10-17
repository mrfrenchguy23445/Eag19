package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ResourceOrTagArgument<T> implements ArgumentType<ResourceOrTagArgument.Result<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
   private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_TAG = new Dynamic2CommandExceptionType(
      (param0, param1) -> Component.translatable("argument.resource_tag.not_found", param0, param1)
   );
   private static final Dynamic3CommandExceptionType ERROR_INVALID_TAG_TYPE = new Dynamic3CommandExceptionType(
      (param0, param1, param2) -> Component.translatable("argument.resource_tag.invalid_type", param0, param1, param2)
   );
   private final HolderLookup<T> registryLookup;
   final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceOrTagArgument(CommandBuildContext param0, ResourceKey<? extends Registry<T>> param1) {
      this.registryKey = param1;
      this.registryLookup = param0.holderLookup(param1);
   }

   public static <T> ResourceOrTagArgument<T> resourceOrTag(CommandBuildContext param0, ResourceKey<? extends Registry<T>> param1) {
      return new ResourceOrTagArgument<>(param0, param1);
   }

   public static <T> ResourceOrTagArgument.Result<T> getResourceOrTag(CommandContext<CommandSourceStack> param0, String param1, ResourceKey<Registry<T>> param2) throws CommandSyntaxException {
      ResourceOrTagArgument.Result<?> var3 = param0.getArgument(param1, ResourceOrTagArgument.Result.class);
      Optional<ResourceOrTagArgument.Result<T>> var4 = var3.cast(param2);
      return var4.orElseThrow(() -> var3.unwrap().map(l2_param1 -> {
         ResourceKey<?> l2_var2 = l2_param1.key();
         return ResourceArgument.ERROR_INVALID_RESOURCE_TYPE.create(l2_var2.location(), l2_var2.registry(), param2.location());
      }, l2_param1 -> {
         TagKey<?> l2_var2 = l2_param1.key();
         return ERROR_INVALID_TAG_TYPE.create(l2_var2.location(), l2_var2.registry(), param2.location());
      }));
   }

   public ResourceOrTagArgument.Result<T> parse(StringReader param0) throws CommandSyntaxException {
      if (param0.canRead() && param0.peek() == '#') {
         int var2 = param0.getCursor();

         try {
            param0.skip();
            ResourceLocation var3 = ResourceLocation.read(param0);
            TagKey<T> var4 = TagKey.create(this.registryKey, var3);
            HolderSet.Named<T> var5 = this.registryLookup.get(var4).orElseThrow(() -> ERROR_UNKNOWN_TAG.create(var3, this.registryKey.location()));
            return new ResourceOrTagArgument.TagResult<>(var5);
         } catch (CommandSyntaxException var3) {
            param0.setCursor(var2);
            throw var3;
         }
      } else {
         ResourceLocation var2 = ResourceLocation.read(param0);
         ResourceKey<T> var3 = ResourceKey.create(this.registryKey, var2);
         Holder.Reference<T> var4 = this.registryLookup
            .get(var3)
            .orElseThrow(() -> ResourceArgument.ERROR_UNKNOWN_RESOURCE.create(var2, this.registryKey.location()));
         return new ResourceOrTagArgument.ResourceResult<>(var4);
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> param0, SuggestionsBuilder param1) {
      SharedSuggestionProvider.suggestResource(this.registryLookup.listTagIds().map(TagKey::location), param1, "#");
      return SharedSuggestionProvider.suggestResource(this.registryLookup.listElementIds().map(ResourceKey::location), param1);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagArgument<T>, ResourceOrTagArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceOrTagArgument.Info<T>.Template param0, FriendlyByteBuf param1) {
         param1.writeResourceLocation(param0.registryKey.location());
      }

      public ResourceOrTagArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf param0) {
         ResourceLocation var2 = param0.readResourceLocation();
         return new ResourceOrTagArgument.Info.Template(ResourceKey.createRegistryKey(var2));
      }

      public void serializeToJson(ResourceOrTagArgument.Info<T>.Template param0, JsonObject param1) {
         param1.addProperty("registry", param0.registryKey.location().toString());
      }

      public ResourceOrTagArgument.Info<T>.Template unpack(ResourceOrTagArgument<T> param0) {
         return new ResourceOrTagArgument.Info.Template(param0.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(ResourceKey<? extends Registry<T>> param1) {
            this.registryKey = param1;
         }

         public ResourceOrTagArgument<T> instantiate(CommandBuildContext param0) {
            return new ResourceOrTagArgument<>(param0, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceOrTagArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }

   record ResourceResult<T>(Holder.Reference<T> value) implements ResourceOrTagArgument.Result<T> {
      @Override
      public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
         return Either.left(this.value);
      }

      @Override
      public <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> param0) {
         return this.value.key().isFor(param0) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
      }

      public boolean test(Holder<T> param0) {
         return param0.equals(this.value);
      }

      @Override
      public String asPrintable() {
         return this.value.key().location().toString();
      }
   }

   public interface Result<T> extends Predicate<Holder<T>> {
      Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap();

      <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> var1);

      String asPrintable();
   }

   record TagResult<T>(HolderSet.Named<T> tag) implements ResourceOrTagArgument.Result<T> {
      @Override
      public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
         return Either.right(this.tag);
      }

      @Override
      public <E> Optional<ResourceOrTagArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> param0) {
         return this.tag.key().isFor(param0) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
      }

      public boolean test(Holder<T> param0) {
         return this.tag.contains(param0);
      }

      @Override
      public String asPrintable() {
         return "#" + this.tag.key().location();
      }
   }
}
