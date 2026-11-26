package net.minecraft.world.level.block.entity;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockEntity extends BlockEntity {
   public static final String TAG_SKULL_OWNER = "SkullOwner";
   public static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
   @Nullable
   private static GameProfileCache profileCache;
   @Nullable
   private static MinecraftSessionService sessionService;
   @Nullable
   private static Executor mainThreadExecutor;
   @Nullable
   private GameProfile owner;
   @Nullable
   private ResourceLocation noteBlockSound;
   private int animationTickCount;
   private boolean isAnimating;

   public SkullBlockEntity(BlockPos param0, BlockState param1) {
      super(BlockEntityType.SKULL, param0, param1);
   }

   public static void setup(Services param0, Executor param1) {
      profileCache = param0.profileCache();
      sessionService = param0.sessionService();
      mainThreadExecutor = param1;
   }

   public static void clear() {
      profileCache = null;
      sessionService = null;
      mainThreadExecutor = null;
   }

   @Override
   protected void saveAdditional(CompoundTag param0) {
      super.saveAdditional(param0);
      if (this.owner != null) {
         CompoundTag var2 = new CompoundTag();
         NbtUtils.writeGameProfile(var2, this.owner);
         param0.put("SkullOwner", var2);
      }

      if (this.noteBlockSound != null) {
         param0.putString("note_block_sound", this.noteBlockSound.toString());
      }
   }

   @Override
   public void load(CompoundTag param0) {
      super.load(param0);
      if (param0.contains("SkullOwner", 10)) {
         this.setOwner(NbtUtils.readGameProfile(param0.getCompound("SkullOwner")));
      } else if (param0.contains("ExtraType", 8)) {
         String var2 = param0.getString("ExtraType");
         if (!StringUtil.isNullOrEmpty(var2)) {
            this.setOwner(new GameProfile(null, var2));
         }
      }

      if (param0.contains("note_block_sound", 8)) {
         this.noteBlockSound = ResourceLocation.tryParse(param0.getString("note_block_sound"));
      }
   }

   public static void animation(Level param0, BlockPos param1, BlockState param2, SkullBlockEntity param3) {
      if (param0.hasNeighborSignal(param1)) {
         param3.isAnimating = true;
         param3.animationTickCount++;
      } else {
         param3.isAnimating = false;
      }
   }

   public float getAnimation(float param0) {
      return this.isAnimating ? this.animationTickCount + param0 : this.animationTickCount;
   }

   @Nullable
   public GameProfile getOwnerProfile() {
      return this.owner;
   }

   @Nullable
   public ResourceLocation getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public void setOwner(@Nullable GameProfile param0) {
      synchronized (this) {
         this.owner = param0;
      }

      this.updateOwnerProfile();
   }

   private void updateOwnerProfile() {
      updateGameprofile(this.owner, l_param0 -> {
         this.owner = l_param0;
         this.setChanged();
      });
   }

   public static void updateGameprofile(@Nullable GameProfile param0, Consumer<GameProfile> param1) {
      if (param0 != null
         && !StringUtil.isNullOrEmpty(param0.getName())
         && (!param0.isComplete() || !param0.getProperties().containsKey("textures"))
         && profileCache != null
         && sessionService != null) {
         profileCache.getAsync(param0.getName(), l_param2 -> Util.backgroundExecutor().execute(() -> Util.ifElse(l_param2, l3_param1 -> {
            Property l3_var2 = Iterables.getFirst(l3_param1.getProperties().get("textures"), null);
            if (l3_var2 == null) {
               MinecraftSessionService l3_var3 = sessionService;
               if (l3_var3 == null) {
                  return;
               }

               l3_param1 = l3_var3.fillProfileProperties(l3_param1, true);
            }

            GameProfile l3_var3 = l3_param1;
            Executor l3_var4 = mainThreadExecutor;
            if (l3_var4 != null) {
               l3_var4.execute(() -> {
                  GameProfileCache l4_var2 = profileCache;
                  if (l4_var2 != null) {
                     l4_var2.add(l3_var3);
                     param1.accept(l3_var3);
                  }
               });
            }
         }, () -> {
            Executor l3_var2 = mainThreadExecutor;
            if (l3_var2 != null) {
               l3_var2.execute(() -> param1.accept(param0));
            }
         })));
      } else {
         param1.accept(param0);
      }
   }
}
