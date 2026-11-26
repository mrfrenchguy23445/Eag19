package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BehaviorBuilder<E extends LivingEntity, M> implements App<BehaviorBuilder.Mu<E>, M> {
   private final BehaviorBuilder.TriggerWithResult<E, M> trigger;

   public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(App<BehaviorBuilder.Mu<E>, M> param0) {
      return (BehaviorBuilder<E, M>)param0;
   }

   public static <E extends LivingEntity> BehaviorBuilder.Instance<E> instance() {
      return new BehaviorBuilder.Instance<>();
   }

   public static <E extends LivingEntity> OneShot<E> create(Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> param0) {
      final BehaviorBuilder.TriggerWithResult<E, Trigger<E>> var1 = get((App<BehaviorBuilder.Mu<E>, Trigger<E>>)param0.apply(instance()));
      return new OneShot<E>() {
         @Override
         public boolean trigger(ServerLevel l_param0, E l_param1, long l_param2) {
            Trigger<E> l_var5 = var1.tryTrigger(l_param0, l_param1, l_param2);
            return l_var5 == null ? false : l_var5.trigger(l_param0, l_param1, l_param2);
         }

         @Override
         public String debugString() {
            return "OneShot[" + var1.debugString() + "]";
         }

         @Override
         public String toString() {
            return this.debugString();
         }
      };
   }

   public static <E extends LivingEntity> OneShot<E> sequence(Trigger<? super E> param0, Trigger<? super E> param1) {
      return create(l_param2 -> l_param2.group(l_param2.ifTriggered(param0)).apply(l_param2, l2_param1 -> param1::trigger));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> param0, OneShot<? super E> param1) {
      return sequence(triggerIf(param0), param1);
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> param0) {
      return create(l_param1 -> l_param1.point((l2_param1, l2_param2, l2_param3) -> param0.test(l2_param2)));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(BiPredicate<ServerLevel, E> param0) {
      return create(l_param1 -> l_param1.point((l2_param1, l2_param2, l2_param3) -> param0.test(l2_param1, l2_param2)));
   }

   static <E extends LivingEntity, M> BehaviorBuilder.TriggerWithResult<E, M> get(App<BehaviorBuilder.Mu<E>, M> param0) {
      return unbox(param0).trigger;
   }

   BehaviorBuilder(BehaviorBuilder.TriggerWithResult<E, M> param0) {
      this.trigger = param0;
   }

   static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(BehaviorBuilder.TriggerWithResult<E, M> param0) {
      return new BehaviorBuilder<>(param0);
   }

   static final class Constant<E extends LivingEntity, A> extends BehaviorBuilder<E, A> {
      Constant(A param0) {
         this(param0, () -> "C[" + param0 + "]");
      }

      Constant(final A param0, final Supplier<String> param1) {
         super(new BehaviorBuilder.TriggerWithResult<E, A>() {
            @Override
            public A tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               return param0;
            }

            @Override
            public String debugString() {
               return param1.get();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   public static final class Instance<E extends LivingEntity> implements Applicative<BehaviorBuilder.Mu<E>, BehaviorBuilder.Instance.Mu<E>> {
      public <Value> Optional<Value> tryGet(MemoryAccessor<OptionalBox.Mu, Value> param0) {
         return OptionalBox.unbox(param0.value());
      }

      public <Value> Value get(MemoryAccessor<IdF.Mu, Value> param0) {
         return IdF.get(param0.value());
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<OptionalBox.Mu, Value>> registered(MemoryModuleType<Value> param0) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Registered<>(param0));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<IdF.Mu, Value>> present(MemoryModuleType<Value> param0) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Present<>(param0));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<Const.Mu<Unit>, Value>> absent(MemoryModuleType<Value> param0) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Absent<>(param0));
      }

      public BehaviorBuilder<E, Unit> ifTriggered(Trigger<? super E> param0) {
         return new BehaviorBuilder.TriggerWrapper<>(param0);
      }

      public <A> BehaviorBuilder<E, A> point(A param0) {
         return new BehaviorBuilder.Constant<>(param0);
      }

      public <A> BehaviorBuilder<E, A> point(Supplier<String> param0, A param1) {
         return new BehaviorBuilder.Constant<>(param1, param0);
      }

      @Override
      public <A, R> Function<App<BehaviorBuilder.Mu<E>, A>, App<BehaviorBuilder.Mu<E>, R>> lift1(App<BehaviorBuilder.Mu<E>, Function<A, R>> param0) {
         return l_param1 -> {
            final BehaviorBuilder.TriggerWithResult<E, A> l_var3 = BehaviorBuilder.get(l_param1);
            final BehaviorBuilder.TriggerWithResult<E, Function<A, R>> l_var4 = BehaviorBuilder.get(param0);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
               @Override
               public R tryTrigger(ServerLevel l_param0, E l_param1x, long l_param2) {
                  A l_var5 = (A)l_var3.tryTrigger(l_param0, l_param1x, l_param2);
                  if (l_var5 == null) {
                     return null;
                  }

                  Function<A, R> l_var6 = (Function<A, R>)l_var4.tryTrigger(l_param0, l_param1x, l_param2);
                  return (R)(l_var6 == null ? null : l_var6.apply(l_var5));
               }

               @Override
               public String debugString() {
                  return l_var4.debugString() + " * " + l_var3.debugString();
               }

               @Override
               public String toString() {
                  return this.debugString();
               }
            });
         };
      }

      public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> param0, App<BehaviorBuilder.Mu<E>, T> param1) {
         final BehaviorBuilder.TriggerWithResult<E, T> var3 = BehaviorBuilder.get(param1);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               T l_var5 = var3.tryTrigger(l_param0, l_param1, l_param2);
               return (R)(l_var5 == null ? null : param0.apply(l_var5));
            }

            @Override
            public String debugString() {
               return var3.debugString() + ".map[" + param0 + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <A, B, R> BehaviorBuilder<E, R> ap2(
         App<BehaviorBuilder.Mu<E>, BiFunction<A, B, R>> param0, App<BehaviorBuilder.Mu<E>, A> param1, App<BehaviorBuilder.Mu<E>, B> param2
      ) {
         final BehaviorBuilder.TriggerWithResult<E, A> var4 = BehaviorBuilder.get(param1);
         final BehaviorBuilder.TriggerWithResult<E, B> var5 = BehaviorBuilder.get(param2);
         final BehaviorBuilder.TriggerWithResult<E, BiFunction<A, B, R>> var6 = BehaviorBuilder.get(param0);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               A l_var5 = var4.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var5 == null) {
                  return null;
               }

               B l_var6 = var5.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var6 == null) {
                  return null;
               }

               BiFunction<A, B, R> l_var7 = var6.tryTrigger(l_param0, l_param1, l_param2);
               return l_var7 == null ? null : l_var7.apply(l_var5, l_var6);
            }

            @Override
            public String debugString() {
               return var6.debugString() + " * " + var4.debugString() + " * " + var5.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(
         App<BehaviorBuilder.Mu<E>, Function3<T1, T2, T3, R>> param0,
         App<BehaviorBuilder.Mu<E>, T1> param1,
         App<BehaviorBuilder.Mu<E>, T2> param2,
         App<BehaviorBuilder.Mu<E>, T3> param3
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> var5 = BehaviorBuilder.get(param1);
         final BehaviorBuilder.TriggerWithResult<E, T2> var6 = BehaviorBuilder.get(param2);
         final BehaviorBuilder.TriggerWithResult<E, T3> var7 = BehaviorBuilder.get(param3);
         final BehaviorBuilder.TriggerWithResult<E, Function3<T1, T2, T3, R>> var8 = BehaviorBuilder.get(param0);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               T1 l_var5 = var5.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var5 == null) {
                  return null;
               }

               T2 l_var6 = var6.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var6 == null) {
                  return null;
               }

               T3 l_var7 = var7.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var7 == null) {
                  return null;
               }

               Function3<T1, T2, T3, R> l_var8 = var8.tryTrigger(l_param0, l_param1, l_param2);
               return l_var8 == null ? null : l_var8.apply(l_var5, l_var6, l_var7);
            }

            @Override
            public String debugString() {
               return var8.debugString() + " * " + var5.debugString() + " * " + var6.debugString() + " * " + var7.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(
         App<BehaviorBuilder.Mu<E>, Function4<T1, T2, T3, T4, R>> param0,
         App<BehaviorBuilder.Mu<E>, T1> param1,
         App<BehaviorBuilder.Mu<E>, T2> param2,
         App<BehaviorBuilder.Mu<E>, T3> param3,
         App<BehaviorBuilder.Mu<E>, T4> param4
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> var6 = BehaviorBuilder.get(param1);
         final BehaviorBuilder.TriggerWithResult<E, T2> var7 = BehaviorBuilder.get(param2);
         final BehaviorBuilder.TriggerWithResult<E, T3> var8 = BehaviorBuilder.get(param3);
         final BehaviorBuilder.TriggerWithResult<E, T4> var9 = BehaviorBuilder.get(param4);
         final BehaviorBuilder.TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> var10 = BehaviorBuilder.get(param0);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               T1 l_var5 = var6.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var5 == null) {
                  return null;
               }

               T2 l_var6 = var7.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var6 == null) {
                  return null;
               }

               T3 l_var7 = var8.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var7 == null) {
                  return null;
               }

               T4 l_var8 = var9.tryTrigger(l_param0, l_param1, l_param2);
               if (l_var8 == null) {
                  return null;
               }

               Function4<T1, T2, T3, T4, R> l_var9 = var10.tryTrigger(l_param0, l_param1, l_param2);
               return l_var9 == null ? null : l_var9.apply(l_var5, l_var6, l_var7, l_var8);
            }

            @Override
            public String debugString() {
               return var10.debugString() + " * " + var6.debugString() + " * " + var7.debugString() + " * " + var8.debugString() + " * " + var9.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      static final class Mu<E extends LivingEntity> implements Applicative.Mu {
         private Mu() {
         }
      }
   }

   public static final class Mu<E extends LivingEntity> implements K1 {
   }

   static final class PureMemory<E extends LivingEntity, F extends K1, Value> extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
      PureMemory(final MemoryCondition<F, Value> param0) {
         super(new BehaviorBuilder.TriggerWithResult<E, MemoryAccessor<F, Value>>() {
            public MemoryAccessor<F, Value> tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               Brain<?> l_var5 = l_param1.getBrain();
               Optional<Value> l_var6 = l_var5.getMemoryInternal(param0.memory());
               return l_var6 == null ? null : param0.createAccessor(l_var5, l_var6);
            }

            @Override
            public String debugString() {
               return "M[" + param0 + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   interface TriggerWithResult<E extends LivingEntity, R> {
      @Nullable
      R tryTrigger(ServerLevel var1, E var2, long var3);

      String debugString();
   }

   static final class TriggerWrapper<E extends LivingEntity> extends BehaviorBuilder<E, Unit> {
      TriggerWrapper(final Trigger<? super E> param0) {
         super(new BehaviorBuilder.TriggerWithResult<E, Unit>() {
            @Nullable
            public Unit tryTrigger(ServerLevel l_param0, E l_param1, long l_param2) {
               return param0.trigger(l_param0, l_param1, l_param2) ? Unit.INSTANCE : null;
            }

            @Override
            public String debugString() {
               return "T[" + param0 + "]";
            }
         });
      }
   }
}
