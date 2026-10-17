package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.TextureUtil;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SimpleOptionsSubScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.lax1dude.eaglercraft.v1_8.Keyboard;
import net.lax1dude.eaglercraft.v1_8.KeyboardConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KeyboardHandler {
   public static final int DEBUG_CRASH_TIME = 10000;
   private final Minecraft minecraft;
   private final ClipboardManager clipboardManager = new ClipboardManager();

   /**
    * The window handle passed to {@link #setup(long)}. Eaglercraft has one implicit surface, so this
    * is {@code Window}'s synthetic handle; it is threaded into keyPress/charTyped so their existing
    * {@code window == this.minecraft.getWindow().getWindow()} guards keep working unchanged.
    */
   private long window;

   private long debugCrashKeyTime = -1L;
   private long debugCrashKeyReportedTime = -1L;
   private long debugCrashKeyReportedCount = -1L;
   private boolean handledDebugKey;

   public KeyboardHandler(Minecraft param0) {
      this.minecraft = param0;
   }

   private boolean handleChunkDebugKeys(int param0) {
      switch (param0) {
         case 69:
            this.minecraft.chunkPath = !this.minecraft.chunkPath;
            this.debugFeedback("ChunkPath: {0}", this.minecraft.chunkPath ? "shown" : "hidden");
            return true;
         case 76:
            this.minecraft.smartCull = !this.minecraft.smartCull;
            this.debugFeedback("SmartCull: {0}", this.minecraft.smartCull ? "enabled" : "disabled");
            return true;
         case 85:
            if (Screen.hasShiftDown()) {
               this.minecraft.levelRenderer.killFrustum();
               this.debugFeedback("Killed frustum");
            } else {
               this.minecraft.levelRenderer.captureFrustum();
               this.debugFeedback("Captured frustum");
            }

            return true;
         case 86:
            this.minecraft.chunkVisibility = !this.minecraft.chunkVisibility;
            this.debugFeedback("ChunkVisibility: {0}", this.minecraft.chunkVisibility ? "enabled" : "disabled");
            return true;
         case 87:
            this.minecraft.wireframe = !this.minecraft.wireframe;
            this.debugFeedback("WireFrame: {0}", this.minecraft.wireframe ? "enabled" : "disabled");
            return true;
         default:
            return false;
      }
   }

   private void debugComponent(ChatFormatting param0, Component param1) {
      this.minecraft
         .gui
         .getChat()
         .addMessage(
            Component.empty()
               .append(Component.translatable("debug.prefix").withStyle(param0, ChatFormatting.BOLD))
               .append(CommonComponents.SPACE)
               .append(param1)
         );
   }

   private void debugFeedbackComponent(Component param0) {
      this.debugComponent(ChatFormatting.YELLOW, param0);
   }

   private void debugFeedbackTranslated(String param0, Object... param1) {
      this.debugFeedbackComponent(Component.translatable(param0, param1));
   }

   private void debugWarningTranslated(String param0, Object... param1) {
      this.debugComponent(ChatFormatting.RED, Component.translatable(param0, param1));
   }

   private void debugFeedback(String param0, Object... param1) {
      this.debugFeedbackComponent(Component.literal(MessageFormat.format(param0, param1)));
   }

   private boolean handleDebugKeys(int param0) {
      if (this.debugCrashKeyTime > 0L && this.debugCrashKeyTime < Util.getMillis() - 100L) {
         return true;
      }

      switch (param0) {
         case 65:
            this.minecraft.levelRenderer.allChanged();
            this.debugFeedbackTranslated("debug.reload_chunks.message");
            return true;
         case 66:
            boolean var2 = !this.minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes();
            this.minecraft.getEntityRenderDispatcher().setRenderHitBoxes(var2);
            this.debugFeedbackTranslated(var2 ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
            return true;
         case 67:
            if (this.minecraft.player.isReducedDebugInfo()) {
               return false;
            } else {
               ClientPacketListener var7 = this.minecraft.player.connection;
               if (var7 == null) {
                  return false;
               }

               this.debugFeedbackTranslated("debug.copy_location.message");
               this.setClipboard(
                  String.format(
                     Locale.ROOT,
                     "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                     this.minecraft.player.level.dimension().location(),
                     this.minecraft.player.getX(),
                     this.minecraft.player.getY(),
                     this.minecraft.player.getZ(),
                     this.minecraft.player.getYRot(),
                     this.minecraft.player.getXRot()
                  )
               );
               return true;
            }
         case 68:
            if (this.minecraft.gui != null) {
               this.minecraft.gui.getChat().clearMessages(false);
            }

            return true;
         case 71:
            boolean var3 = this.minecraft.debugRenderer.switchRenderChunkborder();
            this.debugFeedbackTranslated(var3 ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            return true;
         case 72:
            this.minecraft.options.advancedItemTooltips = !this.minecraft.options.advancedItemTooltips;
            this.debugFeedbackTranslated(this.minecraft.options.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
            this.minecraft.options.save();
            return true;
         case 73:
            if (!this.minecraft.player.isReducedDebugInfo()) {
               this.copyRecreateCommand(this.minecraft.player.hasPermissions(2), !Screen.hasShiftDown());
            }

            return true;
         case 76:
            if (this.minecraft.debugClientMetricsStart(this::debugFeedbackComponent)) {
               this.debugFeedbackTranslated("debug.profiling.start", 10);
            }

            return true;
         case 78:
            if (!this.minecraft.player.hasPermissions(2)) {
               this.debugFeedbackTranslated("debug.creative_spectator.error");
            } else if (!this.minecraft.player.isSpectator()) {
               this.minecraft.player.connection.sendUnsignedCommand("gamemode spectator");
            } else {
               this.minecraft
                  .player
                  .connection
                  .sendUnsignedCommand("gamemode " + MoreObjects.firstNonNull(this.minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE).getName());
            }

            return true;
         case 80:
            this.minecraft.options.pauseOnLostFocus = !this.minecraft.options.pauseOnLostFocus;
            this.minecraft.options.save();
            this.debugFeedbackTranslated(this.minecraft.options.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
            return true;
         case 81:
            this.debugFeedbackTranslated("debug.help.message");
            ChatComponent var4 = this.minecraft.gui.getChat();
            var4.addMessage(Component.translatable("debug.reload_chunks.help"));
            var4.addMessage(Component.translatable("debug.show_hitboxes.help"));
            var4.addMessage(Component.translatable("debug.copy_location.help"));
            var4.addMessage(Component.translatable("debug.clear_chat.help"));
            var4.addMessage(Component.translatable("debug.chunk_boundaries.help"));
            var4.addMessage(Component.translatable("debug.advanced_tooltips.help"));
            var4.addMessage(Component.translatable("debug.inspect.help"));
            var4.addMessage(Component.translatable("debug.profiling.help"));
            var4.addMessage(Component.translatable("debug.creative_spectator.help"));
            var4.addMessage(Component.translatable("debug.pause_focus.help"));
            var4.addMessage(Component.translatable("debug.help.help"));
            var4.addMessage(Component.translatable("debug.dump_dynamic_textures.help"));
            var4.addMessage(Component.translatable("debug.reload_resourcepacks.help"));
            var4.addMessage(Component.translatable("debug.pause.help"));
            var4.addMessage(Component.translatable("debug.gamemodes.help"));
            return true;
         case 83:
            Path var5 = TextureUtil.getDebugTexturePath(Util.toPath(this.minecraft.gameDirectory)).toAbsolutePath();
            this.minecraft.getTextureManager().dumpAllSheets(var5);
            Component var6 = Component.literal(var5.toString())
               .withStyle(ChatFormatting.UNDERLINE)
               .withStyle(l_param1 -> l_param1.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var5.toFile().toString())));
            this.debugFeedbackTranslated("debug.dump_dynamic_textures", var6);
            return true;
         case 84:
            this.debugFeedbackTranslated("debug.reload_resourcepacks.message");
            this.minecraft.reloadResourcePacks();
            return true;
         case 293:
            if (!this.minecraft.player.hasPermissions(2)) {
               this.debugFeedbackTranslated("debug.gamemodes.error");
            } else {
               this.minecraft.setScreen(new GameModeSwitcherScreen());
            }

            return true;
         default:
            return false;
      }
   }

   private void copyRecreateCommand(boolean param0, boolean param1) {
      HitResult var3 = this.minecraft.hitResult;
      if (var3 != null) {
         switch (var3.getType()) {
            case BLOCK:
               BlockPos var4x = ((BlockHitResult)var3).getBlockPos();
               BlockState var5x = this.minecraft.player.level.getBlockState(var4x);
               if (param0) {
                  if (param1) {
                     this.minecraft.player.connection.getDebugQueryHandler().queryBlockEntityTag(var4x, l_param2 -> {
                        this.copyCreateBlockCommand(var5x, var4x, l_param2);
                        this.debugFeedbackTranslated("debug.inspect.server.block");
                     });
                  } else {
                     BlockEntity var6 = this.minecraft.player.level.getBlockEntity(var4x);
                     CompoundTag var7 = var6 != null ? var6.saveWithoutMetadata() : null;
                     this.copyCreateBlockCommand(var5x, var4x, var7);
                     this.debugFeedbackTranslated("debug.inspect.client.block");
                  }
               } else {
                  this.copyCreateBlockCommand(var5x, var4x, null);
                  this.debugFeedbackTranslated("debug.inspect.client.block");
               }
               break;
            case ENTITY:
               Entity var4 = ((EntityHitResult)var3).getEntity();
               ResourceLocation var5 = BuiltInRegistries.ENTITY_TYPE.getKey(var4.getType());
               if (param0) {
                  if (param1) {
                     this.minecraft.player.connection.getDebugQueryHandler().queryEntityTag(var4.getId(), l_param2 -> {
                        this.copyCreateEntityCommand(var5, var4.position(), l_param2);
                        this.debugFeedbackTranslated("debug.inspect.server.entity");
                     });
                  } else {
                     CompoundTag var6 = var4.saveWithoutId(new CompoundTag());
                     this.copyCreateEntityCommand(var5, var4.position(), var6);
                     this.debugFeedbackTranslated("debug.inspect.client.entity");
                  }
               } else {
                  this.copyCreateEntityCommand(var5, var4.position(), null);
                  this.debugFeedbackTranslated("debug.inspect.client.entity");
               }
         }
      }
   }

   private void copyCreateBlockCommand(BlockState param0, BlockPos param1, @Nullable CompoundTag param2) {
      StringBuilder var4 = new StringBuilder(BlockStateParser.serialize(param0));
      if (param2 != null) {
         var4.append(param2);
      }

      String var5 = String.format(Locale.ROOT, "/setblock %d %d %d %s", param1.getX(), param1.getY(), param1.getZ(), var4);
      this.setClipboard(var5);
   }

   private void copyCreateEntityCommand(ResourceLocation param0, Vec3 param1, @Nullable CompoundTag param2) {
      String var4;
      if (param2 != null) {
         param2.remove("UUID");
         param2.remove("Pos");
         param2.remove("Dimension");
         String var5 = NbtUtils.toPrettyComponent(param2).getString();
         var4 = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", param0.toString(), param1.x, param1.y, param1.z, var5);
      } else {
         var4 = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", param0.toString(), param1.x, param1.y, param1.z);
      }

      this.setClipboard(var4);
   }

   public void keyPress(long param0, int param2, int param3, int param4, int param5) {
      if (param0 == this.minecraft.getWindow().getWindow()) {
         if (this.debugCrashKeyTime > 0L) {
            if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 67)
               || !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292)) {
               this.debugCrashKeyTime = -1L;
            }
         } else if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 67)
            && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292)) {
            this.handledDebugKey = true;
            this.debugCrashKeyTime = Util.getMillis();
            this.debugCrashKeyReportedTime = Util.getMillis();
            this.debugCrashKeyReportedCount = 0L;
         }

         Screen var7 = this.minecraft.screen;
         if (var7 != null) {
            switch (param2) {
               case 258:
                  this.minecraft.setLastInputType(InputType.KEYBOARD_TAB);
               case 259:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
               case 264:
               case 265:
                  this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
            }
         }

         if (param4 == 1 && (!(this.minecraft.screen instanceof KeyBindsScreen) || ((KeyBindsScreen)var7).lastKeySelection <= Util.getMillis() - 20L)) {
            if (this.minecraft.options.keyFullscreen.matches(param2, param3)) {
               this.minecraft.getWindow().toggleFullScreen();
               this.minecraft.options.fullscreen().set(this.minecraft.getWindow().isFullscreen());
               return;
            }

            if (this.minecraft.options.keyScreenshot.matches(param2, param3)) {
               if (Screen.hasControlDown()) {
               }

               Screenshot.grab(
                  this.minecraft.gameDirectory,
                  this.minecraft.getMainRenderTarget(),
                  l_param0 -> this.minecraft.execute(() -> this.minecraft.gui.getChat().addMessage(l_param0))
               );
               return;
            }
         }

         if (this.minecraft.getNarrator().isActive()) {
            boolean var8 = var7 == null || !(var7.getFocused() instanceof EditBox) || !((EditBox)var7.getFocused()).canConsumeInput();
            if (param4 != 0 && param2 == 66 && Screen.hasControlDown() && var8) {
               boolean var9 = this.minecraft.options.narrator().get() == NarratorStatus.OFF;
               this.minecraft.options.narrator().set(NarratorStatus.byId(this.minecraft.options.narrator().get().getId() + 1));
               if (var7 instanceof SimpleOptionsSubScreen) {
                  ((SimpleOptionsSubScreen)var7).updateNarratorButton();
               }

               if (var9 && var7 != null) {
                  var7.narrationEnabled();
               }
            }
         }

         if (var7 != null) {
            boolean[] var8 = new boolean[]{false};
            Screen.wrapScreenError(() -> {
               if (param4 == 1 || param4 == 2) {
                  var7.afterKeyboardAction();
                  var8[0] = var7.keyPressed(param2, param3, param5);
               } else if (param4 == 0) {
                  var8[0] = var7.keyReleased(param2, param3, param5);
               }
            }, "keyPressed event handler", var7.getClass().getCanonicalName());
            if (var8[0]) {
               return;
            }
         }

         if (this.minecraft.screen == null || this.minecraft.screen.passEvents) {
            InputConstants.Key var8 = InputConstants.getKey(param2, param3);
            if (param4 == 0) {
               KeyMapping.set(var8, false);
               if (param2 == 292) {
                  if (this.handledDebugKey) {
                     this.handledDebugKey = false;
                  } else {
                     this.minecraft.options.renderDebug = !this.minecraft.options.renderDebug;
                     this.minecraft.options.renderDebugCharts = this.minecraft.options.renderDebug && Screen.hasShiftDown();
                     this.minecraft.options.renderFpsChart = this.minecraft.options.renderDebug && Screen.hasAltDown();
                  }
               }
            } else {
               if (param2 == 293 && this.minecraft.gameRenderer != null) {
                  this.minecraft.gameRenderer.togglePostEffect();
               }

               boolean var9 = false;
               if (this.minecraft.screen == null) {
                  if (param2 == 256) {
                     boolean var10 = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292);
                     this.minecraft.pauseGame(var10);
                  }

                  var9 = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292) && this.handleDebugKeys(param2);
                  this.handledDebugKey |= var9;
                  if (param2 == 290) {
                     this.minecraft.options.hideGui = !this.minecraft.options.hideGui;
                  }
               }

               if (var9) {
                  KeyMapping.set(var8, false);
               } else {
                  KeyMapping.set(var8, true);
                  KeyMapping.click(var8);
               }

               if (this.minecraft.options.renderDebugCharts && param2 >= 48 && param2 <= 57) {
                  this.minecraft.debugFpsMeterKeyPress(param2 - 48);
               }
            }
         }
      }
   }

   private void charTyped(long param0, int param2, int param3) {
      if (param0 == this.minecraft.getWindow().getWindow()) {
         GuiEventListener var5 = this.minecraft.screen;
         if (var5 != null && this.minecraft.getOverlay() == null) {
            if (Character.charCount(param2) == 1) {
               Screen.wrapScreenError(() -> var5.charTyped((char)param2, param3), "charTyped event handler", var5.getClass().getCanonicalName());
            } else {
               for (char var9 : Character.toChars(param2)) {
                  Screen.wrapScreenError(() -> var5.charTyped(var9, param3), "charTyped event handler", var5.getClass().getCanonicalName());
               }
            }
         }
      }
   }

   public void setup(long param0) {
      // Upstream hands two GLFW callbacks to InputConstants.setupKeyboardCallbacks and lets GLFW
      // invoke them. Eaglercraft is a pull model: Keyboard accumulates events and the game drains
      // them with Keyboard.next(). setup() therefore only records the window handle; the dispatch
      // happens in pumpEvents(), which Minecraft calls once per tick. See the note in
      // InputConstants.setupKeyboardCallbacks.
      this.window = param0;
      Keyboard.enableRepeatEvents(true);
   }

   /**
    * Drains Eaglercraft's keyboard event queue, feeding each event to the same handlers the GLFW
    * callbacks used to reach.
    *
    * <p>The mapping from Eaglercraft's event model to the 1.19.4 one:
    *
    * <ul>
    *   <li>key code -- identical numbering, both GLFW-derived (see {@code KeyboardConstants}).</li>
    *   <li>scancode -- {@code -1}, because Eaglercraft has no scancodes.
    *       {@code InputConstants.getKey} and {@code KeyMapping.matches} both branch on the key
    *       code first and only consult the scancode for {@code SCANCODE}-typed bindings, so
    *       {@code -1} disables exactly those instead of letting them match a same-numbered key
    *       code.</li>
    *   <li>action -- Eaglercraft's {@code getEventKeyState()} is already 0/1 and
    *       {@code isRepeatEvent()} maps onto {@link InputConstants#REPEAT} (2), which is the GLFW
    *       convention the switch in keyPress() already expects.</li>
    *   <li>modifiers -- Eaglercraft carries no modifier bitmask, so it is derived from the held
    *       modifier keys by {@link #modifierMask()}.</li>
    * </ul>
    *
    * <p>Each event is then dispatched the way GLFW's separate key and character callbacks were,
    * rather than one-or-the-other: every action (press, repeat and release) goes to
    * {@code keyPress}, because that is the only path by which {@code KeyMapping} learns a key is
    * held -- a printable key such as WASD must reach it too; and an event additionally carrying a
    * character goes to {@code charTyped}, but only on the way down. Eaglercraft tags a key-up with
    * the DOM event's {@code key} as well, so dispatching the character unconditionally typed every
    * character twice.
    *
    * <p>Events are wrapped in {@code minecraft.execute} exactly as the GLFW callbacks were, so they
    * are still applied on the game thread.
    */
   public void pumpEvents() {
      while (Keyboard.isCreated() && Keyboard.next()) {
         // Eaglercraft's Keyboard API speaks Eaglercraft's own key numbering (the
         // LWJGL 2 codes the 1.8 client used); every consumer below is 1.19.4 and
         // therefore speaks GLFW. Without this translation a press arrives one way
         // and is compared the other: Backspace (Eaglercraft 14) never equals
         // InputConstants.KEY_BACKSPACE (259), so EditBox.keyPressed fell through
         // its 259/261 cases and no text box could delete a character. Printable
         // keys appeared to work only because they arrive through the separate
         // character channel below, which is already plain text.
         int var1 = KeyboardConstants.getGLFWKeyFromEagler(Keyboard.getEventKey());
         int var2 = Keyboard.getEventCharacter() & 0xFFFF;
         boolean var3 = Keyboard.getEventKeyState();

         if (Keyboard.isRepeatEvent() && !var3) {
            // Auto-released repeats. Eaglercraft's own client discards these, and so must this:
            // forwarding them would emit a RELEASE for every repeated press and make held keys
            // flicker.
            continue;
         }

         int var4 = Keyboard.isRepeatEvent() ? InputConstants.REPEAT : (var3 ? InputConstants.PRESS : InputConstants.RELEASE);
         int var5 = this.modifierMask();
         // Deliver the key event for every action, press and release alike, exactly as the GLFW
         // key callback did. KeyMapping only learns a key is held from this path, so a printable
         // key (WASD above all) must not be swallowed by the character branch below.
         this.minecraft.execute(() -> this.keyPress(this.window, var1, -1, var4, var5));
         if (var3 && var2 != 0) {
            // The character exists only on the way down. Eaglercraft's queue carries the DOM
            // event's `key` for a key-up too, and treating that as input typed every character
            // twice, so a release never reaches charTyped.
            this.minecraft.execute(() -> this.charTyped(this.window, var2, var5));
         }
      }
   }

   private int modifierMask() {
      int var0 = 0;
      if (Keyboard.isKeyDown(InputConstants.KEY_LCONTROL) || Keyboard.isKeyDown(InputConstants.KEY_RCONTROL)) {
         var0 |= InputConstants.MOD_CONTROL;
      }

      if (Keyboard.isKeyDown(InputConstants.KEY_LSHIFT) || Keyboard.isKeyDown(InputConstants.KEY_RSHIFT)) {
         var0 |= 1;
      }

      if (Keyboard.isKeyDown(InputConstants.KEY_LALT) || Keyboard.isKeyDown(InputConstants.KEY_RALT)) {
         var0 |= 4;
      }

      return var0;
   }

   public String getClipboard() {
      // The error callback is retained but inert: Eaglercraft's clipboard has no error channel (see
      // ClipboardManager), so a denied read is indistinguishable from an empty one. It still routes
      // through Window.defaultErrorCallback so the logging path is unchanged if a future platform
      // does report something.
      return this.clipboardManager.getClipboard(this.minecraft.getWindow().getWindow(), (l_param0, l_param1) -> {
         if (l_param0 != 65545) {
            this.minecraft.getWindow().defaultErrorCallback(l_param0, l_param1);
         }
      });
   }

   public void setClipboard(String param0) {
      if (!param0.isEmpty()) {
         this.clipboardManager.setClipboard(this.minecraft.getWindow().getWindow(), param0);
      }
   }

   public void tick() {
      if (this.debugCrashKeyTime > 0L) {
         long var1 = Util.getMillis();
         long var3 = 10000L - (var1 - this.debugCrashKeyTime);
         long var5 = var1 - this.debugCrashKeyReportedTime;
         if (var3 < 0L) {
            if (Screen.hasControlDown()) {
               Blaze3D.youJustLostTheGame();
            }

            String var7 = "Manually triggered debug crash";
            CrashReport var8 = new CrashReport("Manually triggered debug crash", new Throwable("Manually triggered debug crash"));
            CrashReportCategory var9 = var8.addCategory("Manual crash details");
            NativeModuleLister.addCrashSection(var9);
            throw new ReportedException(var8);
         }

         if (var5 >= 1000L) {
            if (this.debugCrashKeyReportedCount == 0L) {
               this.debugFeedbackTranslated("debug.crash.message");
            } else {
               this.debugWarningTranslated("debug.crash.warning", Mth.ceil((float)var3 / 1000.0F));
            }

            this.debugCrashKeyReportedTime = var1;
            this.debugCrashKeyReportedCount++;
         }
      }
   }
}
