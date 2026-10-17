package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class GameTestRegistry {
   private static final Collection<TestFunction> TEST_FUNCTIONS = Lists.newArrayList();
   private static final Set<String> TEST_CLASS_NAMES = Sets.newHashSet();
   private static final Map<String, Consumer<ServerLevel>> BEFORE_BATCH_FUNCTIONS = Maps.newHashMap();
   private static final Map<String, Consumer<ServerLevel>> AFTER_BATCH_FUNCTIONS = Maps.newHashMap();
   private static final Collection<TestFunction> LAST_FAILED_TESTS = Sets.newHashSet();

   public static void register(Class<?> param0) {
      Arrays.stream(param0.getDeclaredMethods()).forEach(GameTestRegistry::register);
   }

   public static void register(Method param0) {
      String var1 = param0.getDeclaringClass().getSimpleName();
      GameTest var2 = param0.getAnnotation(GameTest.class);
      if (var2 != null) {
         TEST_FUNCTIONS.add(turnMethodIntoTestFunction(param0));
         TEST_CLASS_NAMES.add(var1);
      }

      GameTestGenerator var3 = param0.getAnnotation(GameTestGenerator.class);
      if (var3 != null) {
         TEST_FUNCTIONS.addAll(useTestGeneratorMethod(param0));
         TEST_CLASS_NAMES.add(var1);
      }

      registerBatchFunction(param0, BeforeBatch.class, BeforeBatch::batch, BEFORE_BATCH_FUNCTIONS);
      registerBatchFunction(param0, AfterBatch.class, AfterBatch::batch, AFTER_BATCH_FUNCTIONS);
   }

   private static <T extends Annotation> void registerBatchFunction(
      Method param0, Class<T> param1, Function<T, String> param2, Map<String, Consumer<ServerLevel>> param3
   ) {
      T var4 = param0.getAnnotation(param1);
      if (var4 != null) {
         String var5 = param2.apply(var4);
         Consumer<ServerLevel> var6 = param3.putIfAbsent(var5, (Consumer<ServerLevel>)turnMethodIntoConsumer(param0));
         if (var6 != null) {
            throw new RuntimeException("Hey, there should only be one " + param1 + " method per batch. Batch '" + var5 + "' has more than one!");
         }
      }
   }

   public static Collection<TestFunction> getTestFunctionsForClassName(String param0) {
      return TEST_FUNCTIONS.stream().filter(l_param1 -> isTestFunctionPartOfClass(l_param1, param0)).collect(Collectors.toList());
   }

   public static Collection<TestFunction> getAllTestFunctions() {
      return TEST_FUNCTIONS;
   }

   public static Collection<String> getAllTestClassNames() {
      return TEST_CLASS_NAMES;
   }

   public static boolean isTestClass(String param0) {
      return TEST_CLASS_NAMES.contains(param0);
   }

   @Nullable
   public static Consumer<ServerLevel> getBeforeBatchFunction(String param0) {
      return BEFORE_BATCH_FUNCTIONS.get(param0);
   }

   @Nullable
   public static Consumer<ServerLevel> getAfterBatchFunction(String param0) {
      return AFTER_BATCH_FUNCTIONS.get(param0);
   }

   public static Optional<TestFunction> findTestFunction(String param0) {
      return getAllTestFunctions().stream().filter(l_param1 -> l_param1.getTestName().equalsIgnoreCase(param0)).findFirst();
   }

   public static TestFunction getTestFunction(String param0) {
      Optional<TestFunction> var1 = findTestFunction(param0);
      if (!var1.isPresent()) {
         throw new IllegalArgumentException("Can't find the test function for " + param0);
      } else {
         return var1.get();
      }
   }

   private static Collection<TestFunction> useTestGeneratorMethod(Method param0) {
      try {
         Object var1 = param0.getDeclaringClass().newInstance();
         return (Collection<TestFunction>)param0.invoke(var1);
      } catch (ReflectiveOperationException var1) {
         throw new RuntimeException(var1);
      }
   }

   private static TestFunction turnMethodIntoTestFunction(Method param0) {
      GameTest var1 = param0.getAnnotation(GameTest.class);
      String var2 = param0.getDeclaringClass().getSimpleName();
      String var3 = var2.toLowerCase();
      String var4 = var3 + "." + param0.getName().toLowerCase();
      String var5 = var1.template().isEmpty() ? var4 : var3 + "." + var1.template();
      String var6 = var1.batch();
      Rotation var7 = StructureUtils.getRotationForRotationSteps(var1.rotationSteps());
      return new TestFunction(
         var6,
         var4,
         var5,
         var7,
         var1.timeoutTicks(),
         var1.setupTicks(),
         var1.required(),
         var1.requiredSuccesses(),
         var1.attempts(),
         (Consumer<GameTestHelper>)turnMethodIntoConsumer(param0)
      );
   }

   private static Consumer<?> turnMethodIntoConsumer(Method param0) {
      return l_param1 -> {
         try {
            Object l_var2 = param0.getDeclaringClass().newInstance();
            param0.invoke(l_var2, l_param1);
         } catch (InvocationTargetException l_var2) {
            if (l_var2.getCause() instanceof RuntimeException) {
               throw (RuntimeException)l_var2.getCause();
            } else {
               throw new RuntimeException(l_var2.getCause());
            }
         } catch (ReflectiveOperationException l_var2) {
            throw new RuntimeException(l_var2);
         }
      };
   }

   private static boolean isTestFunctionPartOfClass(TestFunction param0, String param1) {
      return param0.getTestName().toLowerCase().startsWith(param1.toLowerCase() + ".");
   }

   public static Collection<TestFunction> getLastFailedTests() {
      return LAST_FAILED_TESTS;
   }

   public static void rememberFailedTest(TestFunction param0) {
      LAST_FAILED_TESTS.add(param0);
   }

   public static void forgetFailedTests() {
      LAST_FAILED_TESTS.clear();
   }
}
