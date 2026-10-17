package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;

public class ToastComponent extends GuiComponent {
   private static final int SLOT_COUNT = 5;
   private static final int NO_SPACE = -1;
   final Minecraft minecraft;
   private final List<ToastComponent.ToastInstance<?>> visible = new ArrayList<>();
   private final BitSet occupiedSlots = new BitSet(5);
   private final Deque<Toast> queued = Queues.newArrayDeque();

   public ToastComponent(Minecraft param0) {
      this.minecraft = param0;
   }

   public void render(PoseStack param0) {
      if (!this.minecraft.options.hideGui) {
         int var2 = this.minecraft.getWindow().getGuiScaledWidth();
         this.visible.removeIf(l_param2 -> {
            if (l_param2 != null && l_param2.render(var2, param0)) {
               this.occupiedSlots.clear(l_param2.index, l_param2.index + l_param2.slotCount);
               return true;
            } else {
               return false;
            }
         });
         if (!this.queued.isEmpty() && this.freeSlots() > 0) {
            this.queued.removeIf(l_param0 -> {
               int l_var2 = l_param0.slotCount();
               int l_var3 = this.findFreeIndex(l_var2);
               if (l_var3 != -1) {
                  this.visible.add(new ToastComponent.ToastInstance<>(l_param0, l_var3, l_var2));
                  this.occupiedSlots.set(l_var3, l_var3 + l_var2);
                  return true;
               } else {
                  return false;
               }
            });
         }
      }
   }

   private int findFreeIndex(int param0) {
      if (this.freeSlots() >= param0) {
         int var2 = 0;

         for (int var3 = 0; var3 < 5; var3++) {
            if (this.occupiedSlots.get(var3)) {
               var2 = 0;
            } else if (++var2 == param0) {
               return var3 + 1 - var2;
            }
         }
      }

      return -1;
   }

   private int freeSlots() {
      return 5 - this.occupiedSlots.cardinality();
   }

   @Nullable
   public <T extends Toast> T getToast(Class<? extends T> param0, Object param1) {
      for (ToastComponent.ToastInstance<?> var4 : this.visible) {
         if (var4 != null && param0.isAssignableFrom(var4.getToast().getClass()) && var4.getToast().getToken().equals(param1)) {
            return (T)var4.getToast();
         }
      }

      for (Toast var4 : this.queued) {
         if (param0.isAssignableFrom(var4.getClass()) && var4.getToken().equals(param1)) {
            return (T)var4;
         }
      }

      return null;
   }

   public void clear() {
      this.occupiedSlots.clear();
      this.visible.clear();
      this.queued.clear();
   }

   public void addToast(Toast param0) {
      this.queued.add(param0);
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public double getNotificationDisplayTimeMultiplier() {
      return this.minecraft.options.notificationDisplayTime().get();
   }

   class ToastInstance<T extends Toast> {
      private static final long ANIMATION_TIME = 600L;
      private final T toast;
      final int index;
      final int slotCount;
      private long animationTime = -1L;
      private long visibleTime = -1L;
      private Toast.Visibility visibility = Toast.Visibility.SHOW;

      ToastInstance(T param1, int param2, int param3) {
         this.toast = param1;
         this.index = param2;
         this.slotCount = param3;
      }

      public T getToast() {
         return this.toast;
      }

      private float getVisibility(long param0) {
         float var3 = Mth.clamp((float)(param0 - this.animationTime) / 600.0F, 0.0F, 1.0F);
         var3 *= var3;
         return this.visibility == Toast.Visibility.HIDE ? 1.0F - var3 : var3;
      }

      public boolean render(int param0, PoseStack param1) {
         long var3 = Util.getMillis();
         if (this.animationTime == -1L) {
            this.animationTime = var3;
            this.visibility.playSound(ToastComponent.this.minecraft.getSoundManager());
         }

         if (this.visibility == Toast.Visibility.SHOW && var3 - this.animationTime <= 600L) {
            this.visibleTime = var3;
         }

         param1.pushPose();
         param1.translate(param0 - this.toast.width() * this.getVisibility(var3), this.index * 32, 800.0F);
         Toast.Visibility var5 = this.toast.render(param1, ToastComponent.this, var3 - this.visibleTime);
         param1.popPose();
         if (var5 != this.visibility) {
            this.animationTime = var3 - (int)((1.0F - this.getVisibility(var3)) * 600.0F);
            this.visibility = var5;
            this.visibility.playSound(ToastComponent.this.minecraft.getSoundManager());
         }

         return this.visibility == Toast.Visibility.HIDE && var3 - this.animationTime > 600L;
      }
   }
}
