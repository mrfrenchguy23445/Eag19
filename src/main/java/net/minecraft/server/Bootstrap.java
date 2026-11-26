package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import org.slf4j.Logger;

public class Bootstrap {
   public static final PrintStream STDOUT = System.out;
   private static volatile boolean isBootstrapped;
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void bootStrap() {
      if (!isBootstrapped) {
         isBootstrapped = true;
         if (BuiltInRegistries.REGISTRY.keySet().isEmpty()) {
            throw new IllegalStateException("Unable to load registries");
         }

         FireBlock.bootStrap();
         ComposterBlock.bootStrap();
         if (EntityType.getKey(EntityType.PLAYER) == null) {
            throw new IllegalStateException("Failed loading EntityTypes");
         }

         PotionBrewing.bootStrap();
         EntitySelectorOptions.bootStrap();
         DispenseItemBehavior.bootStrap();
         CauldronInteraction.bootStrap();
         BuiltInRegistries.bootStrap();
         wrapStreams();
      }
   }

   private static <T> void checkTranslations(Iterable<T> param0, Function<T, String> param1, Set<String> param2) {
      Language var3 = Language.getInstance();
      param0.forEach(l_param3 -> {
         String l_var4 = param1.apply((T)l_param3);
         if (!var3.has(l_var4)) {
            param2.add(l_var4);
         }
      });
   }

   private static void checkGameruleTranslations(final Set<String> param0) {
      final Language var1 = Language.getInstance();
      GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
         @Override
         public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> l_param0, GameRules.Type<T> l_param1) {
            if (!var1.has(l_param0.getDescriptionId())) {
               param0.add(l_param0.getId());
            }
         }
      });
   }

   public static Set<String> getMissingTranslations() {
      Set<String> var0 = new TreeSet<>();
      checkTranslations(BuiltInRegistries.ATTRIBUTE, Attribute::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.ENTITY_TYPE, EntityType::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.MOB_EFFECT, MobEffect::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.ITEM, Item::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.ENCHANTMENT, Enchantment::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.BLOCK, Block::getDescriptionId, var0);
      checkTranslations(BuiltInRegistries.CUSTOM_STAT, l_param0 -> "stat." + l_param0.toString().replace(':', '.'), var0);
      checkGameruleTranslations(var0);
      return var0;
   }

   public static void checkBootstrapCalled(Supplier<String> param0) {
      if (!isBootstrapped) {
         throw createBootstrapException(param0);
      }
   }

   private static RuntimeException createBootstrapException(Supplier<String> param0) {
      try {
         String var1 = param0.get();
         return new IllegalArgumentException("Not bootstrapped (called from " + var1 + ")");
      } catch (Exception var1) {
         RuntimeException var2 = new IllegalArgumentException("Not bootstrapped (failed to resolve location)");
         var2.addSuppressed(var1);
         return var2;
      }
   }

   public static void validate() {
      checkBootstrapCalled(() -> "validate");
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         getMissingTranslations().forEach(l_param0 -> LOGGER.error("Missing translations: {}", l_param0));
         // Port note (wasm14): Commands.validate() removed. It is IDE-only (the branch above
         // never runs in a shipped build), and it builds a full command dispatcher purely to
         // check that every argument type is registered -- which reaches
         // ArgumentUtils.findUsedArgumentTypes and, through Guava's Doubles.asList, calls
         // Collection.stream on a double[] view. That lands on
         // Spliterators.spliterator(double[], int, int, int), a classlib method this target does
         // not provide. The check is a development-time guard against forgetting a serializer
         // registration; the command graph it walks is otherwise not part of this build at all.
         // Commands.validate();
      }

      DefaultAttributes.validate();
   }

   private static void wrapStreams() {
      if (LOGGER.isDebugEnabled()) {
         System.setErr(new DebugLoggedPrintStream("STDERR", System.err));
         System.setOut(new DebugLoggedPrintStream("STDOUT", STDOUT));
      } else {
         System.setErr(new LoggedPrintStream("STDERR", System.err));
         System.setOut(new LoggedPrintStream("STDOUT", STDOUT));
      }
   }

   public static void realStdoutPrintln(String param0) {
      STDOUT.println(param0);
   }
}
