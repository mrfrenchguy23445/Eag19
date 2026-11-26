package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class BaseCommandBlock implements CommandSource {
   private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
   private static final Component DEFAULT_NAME = Component.literal("@");
   private long lastExecution = -1L;
   private boolean updateLastExecution = true;
   private int successCount;
   private boolean trackOutput = true;
   @Nullable
   private Component lastOutput;
   private String command = "";
   private Component name = DEFAULT_NAME;

   public int getSuccessCount() {
      return this.successCount;
   }

   public void setSuccessCount(int param0) {
      this.successCount = param0;
   }

   public Component getLastOutput() {
      return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
   }

   public CompoundTag save(CompoundTag param0) {
      param0.putString("Command", this.command);
      param0.putInt("SuccessCount", this.successCount);
      param0.putString("CustomName", Component.Serializer.toJson(this.name));
      param0.putBoolean("TrackOutput", this.trackOutput);
      if (this.lastOutput != null && this.trackOutput) {
         param0.putString("LastOutput", Component.Serializer.toJson(this.lastOutput));
      }

      param0.putBoolean("UpdateLastExecution", this.updateLastExecution);
      if (this.updateLastExecution && this.lastExecution > 0L) {
         param0.putLong("LastExecution", this.lastExecution);
      }

      return param0;
   }

   public void load(CompoundTag param0) {
      this.command = param0.getString("Command");
      this.successCount = param0.getInt("SuccessCount");
      if (param0.contains("CustomName", 8)) {
         this.setName(Component.Serializer.fromJson(param0.getString("CustomName")));
      }

      if (param0.contains("TrackOutput", 1)) {
         this.trackOutput = param0.getBoolean("TrackOutput");
      }

      if (param0.contains("LastOutput", 8) && this.trackOutput) {
         try {
            this.lastOutput = Component.Serializer.fromJson(param0.getString("LastOutput"));
         } catch (Throwable var2) {
            this.lastOutput = Component.literal(var2.getMessage());
         }
      } else {
         this.lastOutput = null;
      }

      if (param0.contains("UpdateLastExecution")) {
         this.updateLastExecution = param0.getBoolean("UpdateLastExecution");
      }

      if (this.updateLastExecution && param0.contains("LastExecution")) {
         this.lastExecution = param0.getLong("LastExecution");
      } else {
         this.lastExecution = -1L;
      }
   }

   public void setCommand(String param0) {
      this.command = param0;
      this.successCount = 0;
   }

   public String getCommand() {
      return this.command;
   }

   public boolean performCommand(Level param0) {
      if (param0.isClientSide || param0.getGameTime() == this.lastExecution) {
         return false;
      }

      if ("Searge".equalsIgnoreCase(this.command)) {
         this.lastOutput = Component.literal("#itzlipofutzli");
         this.successCount = 1;
         return true;
      }

      this.successCount = 0;
      MinecraftServer var2 = this.getLevel().getServer();
      if (var2.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
         try {
            this.lastOutput = null;
            CommandSourceStack var3 = this.createCommandSourceStack().withCallback((l_param0, l_param1, l_param2) -> {
               if (l_param1) {
                  this.successCount++;
               }
            });
            var2.getCommands().performPrefixedCommand(var3, this.command);
         } catch (Throwable var3) {
            CrashReport var4 = CrashReport.forThrowable(var3, "Executing command block");
            CrashReportCategory var5 = var4.addCategory("Command to be executed");
            var5.setDetail("Command", this::getCommand);
            var5.setDetail("Name", () -> this.getName().getString());
            throw new ReportedException(var4);
         }
      }

      if (this.updateLastExecution) {
         this.lastExecution = param0.getGameTime();
      } else {
         this.lastExecution = -1L;
      }

      return true;
   }

   public Component getName() {
      return this.name;
   }

   public void setName(@Nullable Component param0) {
      if (param0 != null) {
         this.name = param0;
      } else {
         this.name = DEFAULT_NAME;
      }
   }

   @Override
   public void sendSystemMessage(Component param0) {
      if (this.trackOutput) {
         this.lastOutput = Component.literal("[" + TIME_FORMAT.format(new Date()) + "] ").append(param0);
         this.onUpdated();
      }
   }

   public abstract ServerLevel getLevel();

   public abstract void onUpdated();

   public void setLastOutput(@Nullable Component param0) {
      this.lastOutput = param0;
   }

   public void setTrackOutput(boolean param0) {
      this.trackOutput = param0;
   }

   public boolean isTrackOutput() {
      return this.trackOutput;
   }

   public InteractionResult usedBy(Player param0) {
      if (!param0.canUseGameMasterBlocks()) {
         return InteractionResult.PASS;
      }

      if (param0.getCommandSenderWorld().isClientSide) {
         param0.openMinecartCommandBlock(this);
      }

      return InteractionResult.sidedSuccess(param0.level.isClientSide);
   }

   public abstract Vec3 getPosition();

   public abstract CommandSourceStack createCommandSourceStack();

   @Override
   public boolean acceptsSuccess() {
      return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
   }

   @Override
   public boolean acceptsFailure() {
      return this.trackOutput;
   }

   @Override
   public boolean shouldInformAdmins() {
      return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
   }
}
