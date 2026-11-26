package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class EntitySelectorOptions {
   private static final Map<String, EntitySelectorOptions.Option> OPTIONS = Maps.newHashMap();
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.entity.options.unknown", param0)
   );
   public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.entity.options.inapplicable", param0)
   );
   public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.distance.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.level.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.limit.toosmall")
   );
   public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.entity.options.sort.irreversible", param0)
   );
   public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.entity.options.mode.invalid", param0)
   );
   public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType(
      param0 -> Component.translatable("argument.entity.options.type.invalid", param0)
   );

   private static void register(String param0, EntitySelectorOptions.Modifier param1, Predicate<EntitySelectorParser> param2, Component param3) {
      OPTIONS.put(param0, new EntitySelectorOptions.Option(param1, param2, param3));
   }

   public static void bootStrap() {
      if (OPTIONS.isEmpty()) {
         register("name", l_param0 -> {
            int l_var1 = l_param0.getReader().getCursor();
            boolean l_var2 = l_param0.shouldInvertValue();
            String l_var3 = l_param0.getReader().readString();
            if (l_param0.hasNameNotEquals() && !l_var2) {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(l_param0.getReader(), "name");
            }

            if (l_var2) {
               l_param0.setHasNameNotEquals(true);
            } else {
               l_param0.setHasNameEquals(true);
            }

            l_param0.addPredicate(l2_param2 -> l2_param2.getName().getString().equals(l_var3) != l_var2);
         }, l_param0 -> !l_param0.hasNameEquals(), Component.translatable("argument.entity.options.name.description"));
         register("distance", l_param0 -> {
            int l_var1 = l_param0.getReader().getCursor();
            MinMaxBounds.Doubles l_var2 = MinMaxBounds.Doubles.fromReader(l_param0.getReader());
            if ((l_var2.getMin() == null || !((Double)l_var2.getMin() < 0.0)) && (l_var2.getMax() == null || !((Double)l_var2.getMax() < 0.0))) {
               l_param0.setDistance(l_var2);
               l_param0.setWorldLimited();
            } else {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_RANGE_NEGATIVE.createWithContext(l_param0.getReader());
            }
         }, l_param0 -> l_param0.getDistance().isAny(), Component.translatable("argument.entity.options.distance.description"));
         register("level", l_param0 -> {
            int l_var1 = l_param0.getReader().getCursor();
            MinMaxBounds.Ints l_var2 = MinMaxBounds.Ints.fromReader(l_param0.getReader());
            if ((l_var2.getMin() == null || (Integer)l_var2.getMin() >= 0) && (l_var2.getMax() == null || (Integer)l_var2.getMax() >= 0)) {
               l_param0.setLevel(l_var2);
               l_param0.setIncludesEntities(false);
            } else {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_LEVEL_NEGATIVE.createWithContext(l_param0.getReader());
            }
         }, l_param0 -> l_param0.getLevel().isAny(), Component.translatable("argument.entity.options.level.description"));
         register("x", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setX(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getX() == null, Component.translatable("argument.entity.options.x.description"));
         register("y", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setY(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getY() == null, Component.translatable("argument.entity.options.y.description"));
         register("z", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setZ(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getZ() == null, Component.translatable("argument.entity.options.z.description"));
         register("dx", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setDeltaX(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
         register("dy", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setDeltaY(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
         register("dz", l_param0 -> {
            l_param0.setWorldLimited();
            l_param0.setDeltaZ(l_param0.getReader().readDouble());
         }, l_param0 -> l_param0.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
         register(
            "x_rotation",
            l_param0 -> l_param0.setRotX(WrappedMinMaxBounds.fromReader(l_param0.getReader(), true, Mth::wrapDegrees)),
            l_param0 -> l_param0.getRotX() == WrappedMinMaxBounds.ANY,
            Component.translatable("argument.entity.options.x_rotation.description")
         );
         register(
            "y_rotation",
            l_param0 -> l_param0.setRotY(WrappedMinMaxBounds.fromReader(l_param0.getReader(), true, Mth::wrapDegrees)),
            l_param0 -> l_param0.getRotY() == WrappedMinMaxBounds.ANY,
            Component.translatable("argument.entity.options.y_rotation.description")
         );
         register("limit", l_param0 -> {
            int l_var1 = l_param0.getReader().getCursor();
            int l_var2 = l_param0.getReader().readInt();
            if (l_var2 < 1) {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_LIMIT_TOO_SMALL.createWithContext(l_param0.getReader());
            }

            l_param0.setMaxResults(l_var2);
            l_param0.setLimited(true);
         }, l_param0 -> !l_param0.isCurrentEntity() && !l_param0.isLimited(), Component.translatable("argument.entity.options.limit.description"));
         register(
            "sort",
            l_param0 -> {
               int l_var1 = l_param0.getReader().getCursor();
               String l_var2 = l_param0.getReader().readUnquotedString();
               l_param0.setSuggestions(
                  (l2_param0, l2_param1) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), l2_param0)
               );

               l_param0.setOrder(switch (l_var2) {
                  case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
                  case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
                  case "random" -> EntitySelectorParser.ORDER_RANDOM;
                  case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
                  default -> {
                     l_param0.getReader().setCursor(l_var1);
                     throw ERROR_SORT_UNKNOWN.createWithContext(l_param0.getReader(), l_var2);
                  }
               });
               l_param0.setSorted(true);
            },
            l_param0 -> !l_param0.isCurrentEntity() && !l_param0.isSorted(),
            Component.translatable("argument.entity.options.sort.description")
         );
         register("gamemode", l_param0 -> {
            l_param0.setSuggestions((l2_param1, l2_param2) -> {
               String l2_var3 = l2_param1.getRemaining().toLowerCase(Locale.ROOT);
               boolean l2_var4 = !l_param0.hasGamemodeNotEquals();
               boolean l2_var5 = true;
               if (!l2_var3.isEmpty()) {
                  if (l2_var3.charAt(0) == '!') {
                     l2_var4 = false;
                     l2_var3 = l2_var3.substring(1);
                  } else {
                     l2_var5 = false;
                  }
               }

               for (GameType l2_var9 : GameType.values()) {
                  if (l2_var9.getName().toLowerCase(Locale.ROOT).startsWith(l2_var3)) {
                     if (l2_var5) {
                        l2_param1.suggest("!" + l2_var9.getName());
                     }

                     if (l2_var4) {
                        l2_param1.suggest(l2_var9.getName());
                     }
                  }
               }

               return l2_param1.buildFuture();
            });
            int l_var1 = l_param0.getReader().getCursor();
            boolean l_var2 = l_param0.shouldInvertValue();
            if (l_param0.hasGamemodeNotEquals() && !l_var2) {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(l_param0.getReader(), "gamemode");
            }

            String l_var3 = l_param0.getReader().readUnquotedString();
            GameType l_var4 = GameType.byName(l_var3, null);
            if (l_var4 == null) {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_GAME_MODE_INVALID.createWithContext(l_param0.getReader(), l_var3);
            }

            l_param0.setIncludesEntities(false);
            l_param0.addPredicate(l2_param2 -> {
               if (!(l2_param2 instanceof ServerPlayer)) {
                  return false;
               }

               GameType l2_var3 = ((ServerPlayer)l2_param2).gameMode.getGameModeForPlayer();
               return l_var2 ? l2_var3 != l_var4 : l2_var3 == l_var4;
            });
            if (l_var2) {
               l_param0.setHasGamemodeNotEquals(true);
            } else {
               l_param0.setHasGamemodeEquals(true);
            }
         }, l_param0 -> !l_param0.hasGamemodeEquals(), Component.translatable("argument.entity.options.gamemode.description"));
         register("team", l_param0 -> {
            boolean l_var1 = l_param0.shouldInvertValue();
            String l_var2 = l_param0.getReader().readUnquotedString();
            l_param0.addPredicate(l2_param2 -> {
               if (!(l2_param2 instanceof LivingEntity)) {
                  return false;
               }

               Team l2_var3 = l2_param2.getTeam();
               String l2_var4 = l2_var3 == null ? "" : l2_var3.getName();
               return l2_var4.equals(l_var2) != l_var1;
            });
            if (l_var1) {
               l_param0.setHasTeamNotEquals(true);
            } else {
               l_param0.setHasTeamEquals(true);
            }
         }, l_param0 -> !l_param0.hasTeamEquals(), Component.translatable("argument.entity.options.team.description"));
         register("type", l_param0 -> {
            l_param0.setSuggestions((l2_param1, l2_param2) -> {
               SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), l2_param1, String.valueOf('!'));
               SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTagNames().map(TagKey::location), l2_param1, "!#");
               if (!l_param0.isTypeLimitedInversely()) {
                  SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), l2_param1);
                  SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTagNames().map(TagKey::location), l2_param1, String.valueOf('#'));
               }

               return l2_param1.buildFuture();
            });
            int l_var1 = l_param0.getReader().getCursor();
            boolean l_var2 = l_param0.shouldInvertValue();
            if (l_param0.isTypeLimitedInversely() && !l_var2) {
               l_param0.getReader().setCursor(l_var1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(l_param0.getReader(), "type");
            }

            if (l_var2) {
               l_param0.setTypeLimitedInversely();
            }

            if (l_param0.isTag()) {
               TagKey<EntityType<?>> l_var3 = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.read(l_param0.getReader()));
               l_param0.addPredicate(l2_param2 -> l2_param2.getType().is(l_var3) != l_var2);
            } else {
               ResourceLocation l_var3 = ResourceLocation.read(l_param0.getReader());
               EntityType<?> l_var4 = BuiltInRegistries.ENTITY_TYPE.getOptional(l_var3).orElseThrow(() -> {
                  l_param0.getReader().setCursor(l_var1);
                  return ERROR_ENTITY_TYPE_INVALID.createWithContext(l_param0.getReader(), l_var3.toString());
               });
               if (Objects.equals(EntityType.PLAYER, l_var4) && !l_var2) {
                  l_param0.setIncludesEntities(false);
               }

               l_param0.addPredicate(l2_param2 -> Objects.equals(l_var4, l2_param2.getType()) != l_var2);
               if (!l_var2) {
                  l_param0.limitToType(l_var4);
               }
            }
         }, l_param0 -> !l_param0.isTypeLimited(), Component.translatable("argument.entity.options.type.description"));
         register("tag", l_param0 -> {
            boolean l_var1 = l_param0.shouldInvertValue();
            String l_var2 = l_param0.getReader().readUnquotedString();
            l_param0.addPredicate(l2_param2 -> "".equals(l_var2) ? l2_param2.getTags().isEmpty() != l_var1 : l2_param2.getTags().contains(l_var2) != l_var1);
         }, l_param0 -> true, Component.translatable("argument.entity.options.tag.description"));
         register("nbt", l_param0 -> {
            boolean l_var1 = l_param0.shouldInvertValue();
            CompoundTag l_var2 = new TagParser(l_param0.getReader()).readStruct();
            l_param0.addPredicate(l2_param2 -> {
               CompoundTag l2_var3 = l2_param2.saveWithoutId(new CompoundTag());
               if (l2_param2 instanceof ServerPlayer) {
                  ItemStack l2_var4 = ((ServerPlayer)l2_param2).getInventory().getSelected();
                  if (!l2_var4.isEmpty()) {
                     l2_var3.put("SelectedItem", l2_var4.save(new CompoundTag()));
                  }
               }

               return NbtUtils.compareNbt(l_var2, l2_var3, true) != l_var1;
            });
         }, l_param0 -> true, Component.translatable("argument.entity.options.nbt.description"));
         register("scores", l_param0 -> {
            StringReader l_var1 = l_param0.getReader();
            Map<String, MinMaxBounds.Ints> l_var2 = Maps.newHashMap();
            l_var1.expect('{');
            l_var1.skipWhitespace();

            while (l_var1.canRead() && l_var1.peek() != '}') {
               l_var1.skipWhitespace();
               String l_var3 = l_var1.readUnquotedString();
               l_var1.skipWhitespace();
               l_var1.expect('=');
               l_var1.skipWhitespace();
               MinMaxBounds.Ints l_var4 = MinMaxBounds.Ints.fromReader(l_var1);
               l_var2.put(l_var3, l_var4);
               l_var1.skipWhitespace();
               if (l_var1.canRead() && l_var1.peek() == ',') {
                  l_var1.skip();
               }
            }

            l_var1.expect('}');
            if (!l_var2.isEmpty()) {
               l_param0.addPredicate(l2_param1 -> {
                  Scoreboard l2_var2 = l2_param1.getServer().getScoreboard();
                  String l2_var3 = l2_param1.getScoreboardName();

                  for (Entry<String, MinMaxBounds.Ints> l2_var5 : l_var2.entrySet()) {
                     Objective l2_var6 = l2_var2.getObjective(l2_var5.getKey());
                     if (l2_var6 == null) {
                        return false;
                     }

                     if (!l2_var2.hasPlayerScore(l2_var3, l2_var6)) {
                        return false;
                     }

                     Score l2_var7 = l2_var2.getOrCreatePlayerScore(l2_var3, l2_var6);
                     int l2_var8 = l2_var7.getScore();
                     if (!l2_var5.getValue().matches(l2_var8)) {
                        return false;
                     }
                  }

                  return true;
               });
            }

            l_param0.setHasScores(true);
         }, l_param0 -> !l_param0.hasScores(), Component.translatable("argument.entity.options.scores.description"));
         register("advancements", l_param0 -> {
            StringReader l_var1 = l_param0.getReader();
            Map<ResourceLocation, Predicate<AdvancementProgress>> l_var2 = Maps.newHashMap();
            l_var1.expect('{');
            l_var1.skipWhitespace();

            while (l_var1.canRead() && l_var1.peek() != '}') {
               l_var1.skipWhitespace();
               ResourceLocation l_var3 = ResourceLocation.read(l_var1);
               l_var1.skipWhitespace();
               l_var1.expect('=');
               l_var1.skipWhitespace();
               if (l_var1.canRead() && l_var1.peek() == '{') {
                  Map<String, Predicate<CriterionProgress>> l_var4 = Maps.newHashMap();
                  l_var1.skipWhitespace();
                  l_var1.expect('{');
                  l_var1.skipWhitespace();

                  while (l_var1.canRead() && l_var1.peek() != '}') {
                     l_var1.skipWhitespace();
                     String l_var5 = l_var1.readUnquotedString();
                     l_var1.skipWhitespace();
                     l_var1.expect('=');
                     l_var1.skipWhitespace();
                     boolean l_var6 = l_var1.readBoolean();
                     l_var4.put(l_var5, l2_param1 -> l2_param1.isDone() == l_var6);
                     l_var1.skipWhitespace();
                     if (l_var1.canRead() && l_var1.peek() == ',') {
                        l_var1.skip();
                     }
                  }

                  l_var1.skipWhitespace();
                  l_var1.expect('}');
                  l_var1.skipWhitespace();
                  l_var2.put(l_var3, l2_param1 -> {
                     for (Entry<String, Predicate<CriterionProgress>> l2_var3 : l_var4.entrySet()) {
                        CriterionProgress l2_var4 = l2_param1.getCriterion(l2_var3.getKey());
                        if (l2_var4 == null || !l2_var3.getValue().test(l2_var4)) {
                           return false;
                        }
                     }

                     return true;
                  });
               } else {
                  boolean l_var4 = l_var1.readBoolean();
                  l_var2.put(l_var3, l2_param1 -> l2_param1.isDone() == l_var4);
               }

               l_var1.skipWhitespace();
               if (l_var1.canRead() && l_var1.peek() == ',') {
                  l_var1.skip();
               }
            }

            l_var1.expect('}');
            if (!l_var2.isEmpty()) {
               l_param0.addPredicate(l2_param1 -> {
                  if (!(l2_param1 instanceof ServerPlayer l2_var2)) {
                     return false;
                  } else {
                     PlayerAdvancements l2_var3 = l2_var2.getAdvancements();
                     ServerAdvancementManager l2_var4 = l2_var2.getServer().getAdvancements();

                     for (Entry<ResourceLocation, Predicate<AdvancementProgress>> l2_var6 : l_var2.entrySet()) {
                        Advancement l2_var7 = l2_var4.getAdvancement(l2_var6.getKey());
                        if (l2_var7 == null || !l2_var6.getValue().test(l2_var3.getOrStartProgress(l2_var7))) {
                           return false;
                        }
                     }

                     return true;
                  }
               });
               l_param0.setIncludesEntities(false);
            }

            l_param0.setHasAdvancements(true);
         }, l_param0 -> !l_param0.hasAdvancements(), Component.translatable("argument.entity.options.advancements.description"));
         register(
            "predicate",
            l_param0 -> {
               boolean l_var1 = l_param0.shouldInvertValue();
               ResourceLocation l_var2 = ResourceLocation.read(l_param0.getReader());
               l_param0.addPredicate(
                  l2_param2 -> {
                     if (!(l2_param2.level instanceof ServerLevel l2_var3)) {
                        return false;
                     } else {
                        LootItemCondition l2_var4 = l2_var3.getServer().getPredicateManager().get(l_var2);
                        if (l2_var4 == null) {
                           return false;
                        }

                        LootContext l2_var5 = new LootContext.Builder(l2_var3)
                           .withParameter(LootContextParams.THIS_ENTITY, l2_param2)
                           .withParameter(LootContextParams.ORIGIN, l2_param2.position())
                           .create(LootContextParamSets.SELECTOR);
                        return l_var1 ^ l2_var4.test(l2_var5);
                     }
                  }
               );
            },
            l_param0 -> true,
            Component.translatable("argument.entity.options.predicate.description")
         );
      }
   }

   public static EntitySelectorOptions.Modifier get(EntitySelectorParser param0, String param1, int param2) throws CommandSyntaxException {
      EntitySelectorOptions.Option var3 = OPTIONS.get(param1);
      if (var3 != null) {
         if (var3.canUse.test(param0)) {
            return var3.modifier;
         } else {
            throw ERROR_INAPPLICABLE_OPTION.createWithContext(param0.getReader(), param1);
         }
      } else {
         param0.getReader().setCursor(param2);
         throw ERROR_UNKNOWN_OPTION.createWithContext(param0.getReader(), param1);
      }
   }

   public static void suggestNames(EntitySelectorParser param0, SuggestionsBuilder param1) {
      String var2 = param1.getRemaining().toLowerCase(Locale.ROOT);

      for (Entry<String, EntitySelectorOptions.Option> var4 : OPTIONS.entrySet()) {
         if (var4.getValue().canUse.test(param0) && var4.getKey().toLowerCase(Locale.ROOT).startsWith(var2)) {
            param1.suggest(var4.getKey() + "=", var4.getValue().description);
         }
      }
   }

   public interface Modifier {
      void handle(EntitySelectorParser var1) throws CommandSyntaxException;
   }

   record Option(EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> canUse, Component description) {
   }
}
