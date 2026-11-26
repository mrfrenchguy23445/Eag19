package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class GameTestSequence {
   final GameTestInfo parent;
   private final List<GameTestEvent> events = Lists.newArrayList();
   private long lastTick;

   GameTestSequence(GameTestInfo param0) {
      this.parent = param0;
      this.lastTick = param0.getTick();
   }

   public GameTestSequence thenWaitUntil(Runnable param0) {
      this.events.add(GameTestEvent.create(param0));
      return this;
   }

   public GameTestSequence thenWaitUntil(long param0, Runnable param2) {
      this.events.add(GameTestEvent.create(param0, param2));
      return this;
   }

   public GameTestSequence thenIdle(int param0) {
      return this.thenExecuteAfter(param0, () -> {});
   }

   public GameTestSequence thenExecute(Runnable param0) {
      this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(param0)));
      return this;
   }

   public GameTestSequence thenExecuteAfter(int param0, Runnable param1) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + param0) {
            throw new GameTestAssertException("Waiting");
         }

         this.executeWithoutFail(param1);
      }));
      return this;
   }

   public GameTestSequence thenExecuteFor(int param0, Runnable param1) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + param0) {
            this.executeWithoutFail(param1);
            throw new GameTestAssertException("Waiting");
         }
      }));
      return this;
   }

   public void thenSucceed() {
      this.events.add(GameTestEvent.create(this.parent::succeed));
   }

   public void thenFail(Supplier<Exception> param0) {
      this.events.add(GameTestEvent.create(() -> this.parent.fail(param0.get())));
   }

   public GameTestSequence.Condition thenTrigger() {
      GameTestSequence.Condition var1 = new GameTestSequence.Condition();
      this.events.add(GameTestEvent.create(() -> var1.trigger(this.parent.getTick())));
      return var1;
   }

   public void tickAndContinue(long param0) {
      try {
         this.tick(param0);
      } catch (GameTestAssertException var4) {
      }
   }

   public void tickAndFailIfNotComplete(long param0) {
      try {
         this.tick(param0);
      } catch (GameTestAssertException var3) {
         this.parent.fail(var3);
      }
   }

   private void executeWithoutFail(Runnable param0) {
      try {
         param0.run();
      } catch (GameTestAssertException var2) {
         this.parent.fail(var2);
      }
   }

   private void tick(long param0) {
      Iterator<GameTestEvent> var3 = this.events.iterator();

      while (var3.hasNext()) {
         GameTestEvent var4 = var3.next();
         var4.assertion.run();
         var3.remove();
         long var5 = param0 - this.lastTick;
         long var7 = this.lastTick;
         this.lastTick = param0;
         if (var4.expectedDelay != null && var4.expectedDelay != var5) {
            this.parent
               .fail(new GameTestAssertException("Succeeded in invalid tick: expected " + (var7 + var4.expectedDelay) + ", but current tick is " + param0));
            break;
         }
      }
   }

   public class Condition {
      private static final long NOT_TRIGGERED = -1L;
      private long triggerTime = -1L;

      void trigger(long param0) {
         if (this.triggerTime != -1L) {
            throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
         }

         this.triggerTime = param0;
      }

      public void assertTriggeredThisTick() {
         long var1 = GameTestSequence.this.parent.getTick();
         if (this.triggerTime != var1) {
            if (this.triggerTime == -1L) {
               throw new GameTestAssertException("Condition not triggered (t=" + var1 + ")");
            } else {
               throw new GameTestAssertException("Condition triggered at " + this.triggerTime + ", (t=" + var1 + ")");
            }
         }
      }
   }
}
