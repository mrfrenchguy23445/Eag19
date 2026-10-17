package net.minecraft.client;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.lax1dude.eaglercraft.v1_8.Mouse;

public class MouseHandler {
   private final Minecraft minecraft;
   private boolean isLeftPressed;
   private boolean isMiddlePressed;
   private boolean isRightPressed;
   private double xpos;
   private double ypos;
   private int fakeRightMouse;
   private int activeButton = -1;
   private boolean ignoreFirstMove = true;
   private int clickDepth;
   private double mousePressedTime;
   private final SmoothDouble smoothTurnX = new SmoothDouble();
   private final SmoothDouble smoothTurnY = new SmoothDouble();
   private double accumulatedDX;
   private double accumulatedDY;
   private double accumulatedScroll;
   private double lastMouseEventTime = Double.MIN_VALUE;
   private boolean mouseGrabbed;

   public MouseHandler(Minecraft param0) {
      this.minecraft = param0;
   }

   private void onPress(long param0, int param2, int param3, int param4) {
      if (param0 == this.minecraft.getWindow().getWindow()) {
         if (this.minecraft.screen != null) {
            this.minecraft.setLastInputType(InputType.MOUSE);
         }

         boolean var6 = param3 == 1;
         if (Minecraft.ON_OSX && param2 == 0) {
            if (var6) {
               if ((param4 & 2) == 2) {
                  param2 = 1;
                  this.fakeRightMouse++;
               }
            } else if (this.fakeRightMouse > 0) {
               param2 = 1;
               this.fakeRightMouse--;
            }
         }

         int var7 = param2;
         if (var6) {
            if (this.minecraft.options.touchscreen().get() && this.clickDepth++ > 0) {
               return;
            }

            this.activeButton = var7;
            this.mousePressedTime = Blaze3D.getTime();
         } else if (this.activeButton != -1) {
            if (this.minecraft.options.touchscreen().get() && --this.clickDepth > 0) {
               return;
            }

            this.activeButton = -1;
         }

         boolean[] var8 = new boolean[]{false};
         if (this.minecraft.getOverlay() == null) {
            if (this.minecraft.screen == null) {
               if (!this.mouseGrabbed && var6) {
                  this.grabMouse();
               }
            } else {
               double var9 = this.xpos * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double var11 = this.ypos * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               Screen var13 = this.minecraft.screen;
               if (var6) {
                  var13.afterMouseAction();
                  Screen.wrapScreenError(
                     () -> var8[0] = var13.mouseClicked(var9, var11, var7), "mouseClicked event handler", var13.getClass().getCanonicalName()
                  );
               } else {
                  Screen.wrapScreenError(
                     () -> var8[0] = var13.mouseReleased(var9, var11, var7), "mouseReleased event handler", var13.getClass().getCanonicalName()
                  );
               }
            }
         }

         if (!var8[0] && (this.minecraft.screen == null || this.minecraft.screen.passEvents) && this.minecraft.getOverlay() == null) {
            if (var7 == 0) {
               this.isLeftPressed = var6;
            } else if (var7 == 2) {
               this.isMiddlePressed = var6;
            } else if (var7 == 1) {
               this.isRightPressed = var6;
            }

            KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(var7), var6);
            if (var6) {
               if (this.minecraft.player.isSpectator() && var7 == 2) {
                  this.minecraft.gui.getSpectatorGui().onMouseMiddleClick();
               } else {
                  KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(var7));
               }
            }
         }
      }
   }

   private void onScroll(long param0, double param2, double param4) {
      if (param0 == Minecraft.getInstance().getWindow().getWindow()) {
         double var7 = (this.minecraft.options.discreteMouseScroll().get() ? Math.signum(param4) : param4)
            * this.minecraft.options.mouseWheelSensitivity().get();
         if (this.minecraft.getOverlay() == null) {
            if (this.minecraft.screen != null) {
               double var9 = this.xpos * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double var11 = this.ypos * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               this.minecraft.screen.mouseScrolled(var9, var11, var7);
               this.minecraft.screen.afterMouseAction();
            } else if (this.minecraft.player != null) {
               if (this.accumulatedScroll != 0.0 && Math.signum(var7) != Math.signum(this.accumulatedScroll)) {
                  this.accumulatedScroll = 0.0;
               }

               this.accumulatedScroll += var7;
               int var9 = (int)this.accumulatedScroll;
               if (var9 == 0) {
                  return;
               }

               this.accumulatedScroll -= var9;
               if (this.minecraft.player.isSpectator()) {
                  if (this.minecraft.gui.getSpectatorGui().isMenuActive()) {
                     this.minecraft.gui.getSpectatorGui().onMouseScrolled(-var9);
                  } else {
                     float var10 = Mth.clamp(this.minecraft.player.getAbilities().getFlyingSpeed() + var9 * 0.005F, 0.0F, 0.2F);
                     this.minecraft.player.getAbilities().setFlyingSpeed(var10);
                  }
               } else {
                  this.minecraft.player.getInventory().swapPaint(var9);
               }
            }
         }
      }
   }

   public void setup(long param0) {
      // Upstream registers four GLFW callbacks here. Eaglercraft is a pull model, so setup() only
      // records the window handle and pumpEvents() does the dispatch once per tick. See the note in
      // InputConstants.setupMouseCallbacks.
      //
      // The file-drop callback is dropped outright rather than translated: it produced
      // java.nio.file.Path values via Paths.get(GLFWDropCallback.getName(..)), and java.nio.file
      // does not exist in the WASM-GC classlib at all (see the "TEAVM CLASSLIB GAPS" notes in
      // build.gradle.kts). It is also unreachable in practice -- a browser canvas cannot be
      // dropped OS files onto, and Screen.onFilesDrop therefore becomes dead code.
      this.window = param0;
   }

   /**
    * The window handle passed to {@link #setup(long)}; see {@link KeyboardHandler#window}.
    */
   private long window;

   /**
    * Drains Eaglercraft's mouse event queue, feeding each event to the same handlers the GLFW
    * callbacks used to reach.
    *
    * <p>Eaglercraft presents a single undifferentiated event stream where every event carries a
    * cursor position, and additionally a button (with its new state) and/or a wheel delta. There is
    * no event-type tag, which is why this dispatches by inspecting those fields rather than by
    * switching on a kind:
    *
    * <ul>
    *   <li>{@code onMove} runs for every event. That is correct rather than merely tolerable, because
    *       a press or a scroll does have a meaningful cursor position and 1.19.4's
    *       {@code onPress}/{@code onScroll} read {@code this.xpos}/{@code this.ypos}, so the position
    *       has to be current before they run.</li>
    *   <li>{@code onPress} runs when {@code getEventButton()} is non-negative, which is how a
    *       button event is distinguished from the others.</li>
    *   <li>{@code onScroll} runs when {@code getEventDWheel()} is non-zero.</li>
    * </ul>
    *
    * <p>The GLFW callback took modifier bits; Eaglercraft reports none, so zeros are passed and the
    * only consumer, the macOS ctrl-click-to-right-click remap, is gated on
    * {@link Minecraft#ON_OSX} and cannot trigger here.
    */
   public void pumpEvents() {
      while (Mouse.isCreated() && Mouse.next()) {
         int var1 = Mouse.getEventX();
         int var2 = Mouse.getEventY();
         int var3 = Mouse.getEventButton();
         int var4 = Mouse.getEventDWheel();
         this.minecraft.execute(() -> this.onMove(this.window, var1, var2));
         if (var3 >= 0) {
            boolean var5 = Mouse.getEventButtonState();
            this.minecraft.execute(() -> this.onPress(this.window, var3, var5 ? 1 : 0, 0));
         }

         if (var4 != 0) {
            double var5 = var4;
            this.minecraft.execute(() -> this.onScroll(this.window, 0.0, var5));
         }
      }
   }

   private void onMove(long param0, double param2, double param4) {
      if (param0 == Minecraft.getInstance().getWindow().getWindow()) {
         if (this.ignoreFirstMove) {
            this.xpos = param2;
            this.ypos = param4;
            this.ignoreFirstMove = false;
         }

         Screen var7 = this.minecraft.screen;
         if (var7 != null && this.minecraft.getOverlay() == null) {
            double var8 = param2 * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
            double var10 = param4 * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
            Screen.wrapScreenError(() -> var7.mouseMoved(var8, var10), "mouseMoved event handler", var7.getClass().getCanonicalName());
            if (this.activeButton != -1 && this.mousePressedTime > 0.0) {
               double var12 = (param2 - this.xpos) * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double var14 = (param4 - this.ypos) * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               Screen.wrapScreenError(
                  () -> var7.mouseDragged(var8, var10, this.activeButton, var12, var14), "mouseDragged event handler", var7.getClass().getCanonicalName()
               );
            }

            var7.afterMouseMove();
         }

         this.minecraft.getProfiler().push("mouse");
         if (this.isMouseGrabbed() && this.minecraft.isWindowActive()) {
            this.accumulatedDX = this.accumulatedDX + (param2 - this.xpos);
            this.accumulatedDY = this.accumulatedDY + (param4 - this.ypos);
         }

         this.turnPlayer();
         this.xpos = param2;
         this.ypos = param4;
         this.minecraft.getProfiler().pop();
      }
   }

   public void turnPlayer() {
      double var1 = Blaze3D.getTime();
      double var3 = var1 - this.lastMouseEventTime;
      this.lastMouseEventTime = var1;
      if (this.isMouseGrabbed() && this.minecraft.isWindowActive()) {
         double var9 = this.minecraft.options.sensitivity().get() * 0.6F + 0.2F;
         double var11 = var9 * var9 * var9;
         double var13 = var11 * 8.0;
         double var5;
         double var7;
         if (this.minecraft.options.smoothCamera) {
            double var15 = this.smoothTurnX.getNewDeltaValue(this.accumulatedDX * var13, var3 * var13);
            double var17 = this.smoothTurnY.getNewDeltaValue(this.accumulatedDY * var13, var3 * var13);
            var5 = var15;
            var7 = var17;
         } else if (this.minecraft.options.getCameraType().isFirstPerson() && this.minecraft.player.isScoping()) {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            var5 = this.accumulatedDX * var11;
            var7 = this.accumulatedDY * var11;
         } else {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            var5 = this.accumulatedDX * var13;
            var7 = this.accumulatedDY * var13;
         }

         this.accumulatedDX = 0.0;
         this.accumulatedDY = 0.0;
         int var15 = 1;
         if (this.minecraft.options.invertYMouse().get()) {
            var15 = -1;
         }

         this.minecraft.getTutorial().onMouse(var5, var7);
         if (this.minecraft.player != null) {
            this.minecraft.player.turn(var5, var7 * var15);
         }
      } else {
         this.accumulatedDX = 0.0;
         this.accumulatedDY = 0.0;
      }
   }

   public boolean isLeftPressed() {
      return this.isLeftPressed;
   }

   public boolean isMiddlePressed() {
      return this.isMiddlePressed;
   }

   public boolean isRightPressed() {
      return this.isRightPressed;
   }

   public double xpos() {
      return this.xpos;
   }

   public double ypos() {
      return this.ypos;
   }

   public void setIgnoreFirstMove() {
      this.ignoreFirstMove = true;
   }

   public boolean isMouseGrabbed() {
      return this.mouseGrabbed;
   }

   public void grabMouse() {
      if (this.minecraft.isWindowActive()) {
         if (!this.mouseGrabbed) {
            if (!Minecraft.ON_OSX) {
               KeyMapping.setAll();
            }

            this.mouseGrabbed = true;
            this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
            this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212995, this.xpos, this.ypos);
            this.minecraft.setScreen(null);
            this.minecraft.missTime = 10000;
            this.ignoreFirstMove = true;
         }
      }
   }

   public void releaseMouse() {
      if (this.mouseGrabbed) {
         this.mouseGrabbed = false;
         this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
         this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
         InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, this.xpos, this.ypos);
      }
   }

   public void cursorEntered() {
      this.ignoreFirstMove = true;
   }
}
