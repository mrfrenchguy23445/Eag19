package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.lax1dude.eaglercraft.v1_8.profile.EaglerProfile;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962634F;
   private static final float DEFAULT_SNEAKING_MOVEMENT_FACTOR = 0.3F;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private int permissionLevel = 0;
   private double xLast;
   private double yLast1;
   private double zLast;
   private float yRotLast;
   private float xRotLast;
   private boolean lastOnGround;
   private boolean crouching;
   private boolean wasShiftKeyDown;
   private boolean wasSprinting;
   private int positionReminder;
   private boolean flashOnSetHealth;
   @Nullable
   private String serverBrand;
   public Input input;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float portalTime;
   public float oPortalTime;
   private boolean startedUsingItem;
   @Nullable
   private InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;

   public LocalPlayer(
      Minecraft param0, ClientLevel param1, ClientPacketListener param2, StatsCounter param3, ClientRecipeBook param4, boolean param5, boolean param6
   ) {
      super(param1, param2.getLocalGameProfile());
      this.minecraft = param0;
      this.connection = param2;
      this.stats = param3;
      this.recipeBook = param4;
      this.wasShiftKeyDown = param5;
      this.wasSprinting = param6;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, param0.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, param0.getSoundManager(), param1.getBiomeManager()));
   }

   @Override
   public boolean hurt(DamageSource param0, float param1) {
      return false;
   }

   @Override
   public void heal(float param0) {
   }

   // Eaglercraft has no Mojang session, so the local player's look comes from
   // EaglerProfile rather than a PlayerInfo. These three overrides are the whole
   // in-world integration: PlayerRenderer textures from getSkinTextureLocation,
   // CapeLayer draws from getCloakTextureLocation (null hides the cape), and
   // EntityRenderDispatcher selects the slim or default renderer from
   // getModelName.
   @Override
   public ResourceLocation getSkinTextureLocation() {
      return EaglerProfile.getSkinLocation();
   }

   @Nullable
   @Override
   public ResourceLocation getCloakTextureLocation() {
      return EaglerProfile.getCapeLocation();
   }

   @Override
   public String getModelName() {
      return EaglerProfile.getRenderModelName();
   }

   // The model-part mask synced from the server defaults to "nothing shown", and
   // Eaglercraft has no skin-customization UI to change it. The local player's
   // hat, jacket, sleeve and pant layers are therefore always drawn; the cape is
   // decided by the cape selection (a null cape location hides it in CapeLayer),
   // not by a model part.
   @Override
   public boolean isModelPartShown(PlayerModelPart part) {
      return true;
   }

   // PlayerInfo.isCapeLoaded only reports that the synced profile has arrived.
   // The local player's cape comes from EaglerProfile, so it is always ready.
   @Override
   public boolean isCapeLoaded() {
      return true;
   }

   @Override
   public boolean startRiding(Entity param0, boolean param1) {
      if (!super.startRiding(param0, param1)) {
         return false;
      }

      if (param0 instanceof AbstractMinecart) {
         this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)param0, true));
         this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)param0, false));
      }

      return true;
   }

   @Override
   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   @Override
   public float getViewXRot(float param0) {
      return this.getXRot();
   }

   @Override
   public float getViewYRot(float param0) {
      return this.isPassenger() ? super.getViewYRot(param0) : this.getYRot();
   }

   @Override
   public void tick() {
      if (this.level.hasChunkAt(this.getBlockX(), this.getBlockZ())) {
         super.tick();
         if (this.isPassenger()) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround));
            this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.shiftKeyDown));
            Entity var1 = this.getRootVehicle();
            if (var1 != this && var1.isControlledByLocalInstance()) {
               this.connection.send(new ServerboundMoveVehiclePacket(var1));
               this.sendIsSprintingIfNeeded();
            }
         } else {
            this.sendPosition();
         }

         for (AmbientSoundHandler var2 : this.ambientSoundHandlers) {
            var2.tick();
         }
      }
   }

   public float getCurrentMood() {
      for (AmbientSoundHandler var2 : this.ambientSoundHandlers) {
         if (var2 instanceof BiomeAmbientSoundsHandler) {
            return ((BiomeAmbientSoundsHandler)var2).getMoodiness();
         }
      }

      return 0.0F;
   }

   private void sendPosition() {
      this.sendIsSprintingIfNeeded();
      boolean var1 = this.isShiftKeyDown();
      if (var1 != this.wasShiftKeyDown) {
         ServerboundPlayerCommandPacket.Action var2 = var1
            ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY
            : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
         this.connection.send(new ServerboundPlayerCommandPacket(this, var2));
         this.wasShiftKeyDown = var1;
      }

      if (this.isControlledCamera()) {
         double var2 = this.getX() - this.xLast;
         double var4 = this.getY() - this.yLast1;
         double var6 = this.getZ() - this.zLast;
         double var8 = this.getYRot() - this.yRotLast;
         double var10 = this.getXRot() - this.xRotLast;
         this.positionReminder++;
         boolean var12 = Mth.lengthSquared(var2, var4, var6) > Mth.square(2.0E-4) || this.positionReminder >= 20;
         boolean var13 = var8 != 0.0 || var10 != 0.0;
         if (this.isPassenger()) {
            Vec3 var14 = this.getDeltaMovement();
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(var14.x, -999.0, var14.z, this.getYRot(), this.getXRot(), this.onGround));
            var12 = false;
         } else if (var12 && var13) {
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround));
         } else if (var12) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(this.getX(), this.getY(), this.getZ(), this.onGround));
         } else if (var13) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround));
         } else if (this.lastOnGround != this.onGround) {
            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround));
         }

         if (var12) {
            this.xLast = this.getX();
            this.yLast1 = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (var13) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround;
         this.autoJumpEnabled = this.minecraft.options.autoJump().get();
      }
   }

   private void sendIsSprintingIfNeeded() {
      boolean var1 = this.isSprinting();
      if (var1 != this.wasSprinting) {
         ServerboundPlayerCommandPacket.Action var2 = var1
            ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
            : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, var2));
         this.wasSprinting = var1;
      }
   }

   public boolean drop(boolean param0) {
      ServerboundPlayerActionPacket.Action var2 = param0 ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack var3 = this.getInventory().removeFromSelected(param0);
      this.connection.send(new ServerboundPlayerActionPacket(var2, BlockPos.ZERO, Direction.DOWN));
      return !var3.isEmpty();
   }

   @Override
   public void swing(InteractionHand param0) {
      super.swing(param0);
      this.connection.send(new ServerboundSwingPacket(param0));
   }

   @Override
   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
   }

   @Override
   protected void actuallyHurt(DamageSource param0, float param1) {
      if (!this.isInvulnerableTo(param0)) {
         this.setHealth(this.getHealth() - param1);
      }
   }

   @Override
   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.setScreen(null);
   }

   public void hurtTo(float param0) {
      if (this.flashOnSetHealth) {
         float var2 = this.getHealth() - param0;
         if (var2 <= 0.0F) {
            this.setHealth(param0);
            if (var2 < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = var2;
            this.invulnerableTime = 20;
            this.setHealth(param0);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(param0);
         this.flashOnSetHealth = true;
      }
   }

   @Override
   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   @Override
   public boolean isLocalPlayer() {
      return true;
   }

   @Override
   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   @Override
   public boolean canSpawnSoulSpeedParticle() {
      return !this.getAbilities().flying && super.canSpawnSoulSpeedParticle();
   }

   protected void sendRidingJump() {
      this.connection
         .send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
   }

   public void setServerBrand(@Nullable String param0) {
      this.serverBrand = param0;
   }

   @Nullable
   public String getServerBrand() {
      return this.serverBrand;
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(Recipe<?> param0) {
      if (this.recipeBook.willHighlight(param0)) {
         this.recipeBook.removeHighlight(param0);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(param0));
      }
   }

   @Override
   protected int getPermissionLevel() {
      return this.permissionLevel;
   }

   public void setPermissionLevel(int param0) {
      this.permissionLevel = param0;
   }

   @Override
   public void displayClientMessage(Component param0, boolean param1) {
      this.minecraft.getChatListener().handleSystemMessage(param0, param1);
   }

   private void moveTowardsClosestSpace(double param0, double param2) {
      BlockPos var5 = BlockPos.containing(param0, this.getY(), param2);
      if (this.suffocatesAt(var5)) {
         double var6 = param0 - var5.getX();
         double var8 = param2 - var5.getZ();
         Direction var10 = null;
         double var11 = Double.MAX_VALUE;
         Direction[] var13 = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for (Direction var17 : var13) {
            double var18 = var17.getAxis().choose(var6, 0.0, var8);
            double var20 = var17.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - var18 : var18;
            if (var20 < var11 && !this.suffocatesAt(var5.relative(var17))) {
               var11 = var20;
               var10 = var17;
            }
         }

         if (var10 != null) {
            Vec3 var14 = this.getDeltaMovement();
            if (var10.getAxis() == Direction.Axis.X) {
               this.setDeltaMovement(0.1 * var10.getStepX(), var14.y, var14.z);
            } else {
               this.setDeltaMovement(var14.x, var14.y, 0.1 * var10.getStepZ());
            }
         }
      }
   }

   private boolean suffocatesAt(BlockPos param0) {
      AABB var2 = this.getBoundingBox();
      AABB var3 = new AABB(param0.getX(), var2.minY, param0.getZ(), param0.getX() + 1.0, var2.maxY, param0.getZ() + 1.0).deflate(1.0E-7);
      return this.level.collidesWithSuffocatingBlock(this, var3);
   }

   public void setExperienceValues(float param0, int param1, int param2) {
      this.experienceProgress = param0;
      this.totalExperience = param1;
      this.experienceLevel = param2;
   }

   @Override
   public void sendSystemMessage(Component param0) {
      this.minecraft.gui.getChat().addMessage(param0);
   }

   @Override
   public void handleEntityEvent(byte param0) {
      if (param0 >= 24 && param0 <= 28) {
         this.setPermissionLevel(param0 - 24);
      } else {
         super.handleEntityEvent(param0);
      }
   }

   public void setShowDeathScreen(boolean param0) {
      this.showDeathScreen = param0;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   @Override
   public void playSound(SoundEvent param0, float param1, float param2) {
      this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), param0, this.getSoundSource(), param1, param2, false);
   }

   @Override
   public void playNotifySound(SoundEvent param0, SoundSource param1, float param2, float param3) {
      this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), param0, param1, param2, param3, false);
   }

   @Override
   public boolean isEffectiveAi() {
      return true;
   }

   @Override
   public void startUsingItem(InteractionHand param0) {
      ItemStack var2 = this.getItemInHand(param0);
      if (!var2.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(param0);
         this.startedUsingItem = true;
         this.usingItemHand = param0;
      }
   }

   @Override
   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   @Override
   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   @Override
   public InteractionHand getUsedItemHand() {
      return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> param0) {
      super.onSyncedDataUpdated(param0);
      if (DATA_LIVING_ENTITY_FLAGS.equals(param0)) {
         boolean var2 = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand var3 = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (var2 && !this.startedUsingItem) {
            this.startUsingItem(var3);
         } else if (!var2 && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(param0) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }
   }

   @Nullable
   public PlayerRideableJumping jumpableVehicle() {
      return this.getControlledVehicle() instanceof PlayerRideableJumping var1 && var1.canJump() ? var1 : null;
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   @Override
   public boolean isTextFilteringEnabled() {
      return this.minecraft.isTextFilteringEnabled();
   }

   @Override
   public void openTextEdit(SignBlockEntity param0) {
      if (param0 instanceof HangingSignBlockEntity var2) {
         this.minecraft.setScreen(new HangingSignEditScreen(var2, this.minecraft.isTextFilteringEnabled()));
      } else {
         this.minecraft.setScreen(new SignEditScreen(param0, this.minecraft.isTextFilteringEnabled()));
      }
   }

   @Override
   public void openMinecartCommandBlock(BaseCommandBlock param0) {
      this.minecraft.setScreen(new MinecartCommandBlockEditScreen(param0));
   }

   @Override
   public void openCommandBlock(CommandBlockEntity param0) {
      this.minecraft.setScreen(new CommandBlockEditScreen(param0));
   }

   @Override
   public void openStructureBlock(StructureBlockEntity param0) {
      this.minecraft.setScreen(new StructureBlockEditScreen(param0));
   }

   @Override
   public void openJigsawBlock(JigsawBlockEntity param0) {
      this.minecraft.setScreen(new JigsawBlockEditScreen(param0));
   }

   @Override
   public void openItemGui(ItemStack param0, InteractionHand param1) {
      if (param0.is(Items.WRITABLE_BOOK)) {
         this.minecraft.setScreen(new BookEditScreen(this, param0, param1));
      }
   }

   @Override
   public void crit(Entity param0) {
      this.minecraft.particleEngine.createTrackingEmitter(param0, ParticleTypes.CRIT);
   }

   @Override
   public void magicCrit(Entity param0) {
      this.minecraft.particleEngine.createTrackingEmitter(param0, ParticleTypes.ENCHANTED_HIT);
   }

   @Override
   public boolean isShiftKeyDown() {
      return this.input != null && this.input.shiftKeyDown;
   }

   @Override
   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   @Override
   public void serverAiStep() {
      super.serverAiStep();
      if (this.isControlledCamera()) {
         this.xxa = this.input.leftImpulse;
         this.zza = this.input.forwardImpulse;
         this.jumping = this.input.jumping;
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
         this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
      }
   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level != null) {
         for (double var1 = this.getY(); var1 > this.level.getMinBuildHeight() && var1 < this.level.getMaxBuildHeight(); var1++) {
            this.setPos(this.getX(), var1, this.getZ());
            if (this.level.noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }

   @Override
   public void aiStep() {
      if (this.sprintTriggerTime > 0) {
         this.sprintTriggerTime--;
      }

      this.handleNetherPortalClient();
      boolean var1 = this.input.jumping;
      boolean var2 = this.input.shiftKeyDown;
      boolean var3 = this.hasEnoughImpulseToStartSprinting();
      this.crouching = !this.getAbilities().flying
         && !this.isSwimming()
         && this.canEnterPose(Pose.CROUCHING)
         && (this.isShiftKeyDown() || !this.isSleeping() && !this.canEnterPose(Pose.STANDING));
      float var4 = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(this), 0.0F, 1.0F);
      this.input.tick(this.isMovingSlowly(), var4);
      this.minecraft.getTutorial().onInput(this.input);
      if (this.isUsingItem() && !this.isPassenger()) {
         this.input.leftImpulse *= 0.2F;
         this.input.forwardImpulse *= 0.2F;
         this.sprintTriggerTime = 0;
      }

      boolean var5 = false;
      if (this.autoJumpTime > 0) {
         this.autoJumpTime--;
         var5 = true;
         this.input.jumping = true;
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
      }

      if (var2) {
         this.sprintTriggerTime = 0;
      }

      boolean var6 = this.canStartSprinting();
      boolean var7 = this.isPassenger() ? this.getVehicle().isOnGround() : this.onGround;
      boolean var8 = !var2 && !var3;
      if ((var7 || this.isUnderWater()) && var8 && var6) {
         if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
            this.sprintTriggerTime = 7;
         } else {
            this.setSprinting(true);
         }
      }

      if ((!this.isInWater() || this.isUnderWater()) && var6 && this.minecraft.options.keySprint.isDown()) {
         this.setSprinting(true);
      }

      if (this.isSprinting()) {
         boolean var9 = !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting();
         boolean var10 = var9 || this.horizontalCollision && !this.minorHorizontalCollision || this.isInWater() && !this.isUnderWater();
         if (this.isSwimming()) {
            if (!this.onGround && !this.input.shiftKeyDown && var9 || !this.isInWater()) {
               this.setSprinting(false);
            }
         } else if (var10) {
            this.setSprinting(false);
         }
      }

      boolean var9 = false;
      if (this.getAbilities().mayfly) {
         if (this.minecraft.gameMode.isAlwaysFlying()) {
            if (!this.getAbilities().flying) {
               this.getAbilities().flying = true;
               var9 = true;
               this.onUpdateAbilities();
            }
         } else if (!var1 && this.input.jumping && !var5) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming()) {
               this.getAbilities().flying = !this.getAbilities().flying;
               var9 = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.jumping && !var9 && !var1 && !this.getAbilities().flying && !this.isPassenger() && !this.onClimbable()) {
         ItemStack var10 = this.getItemBySlot(EquipmentSlot.CHEST);
         if (var10.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(var10) && this.tryToStartFallFlying()) {
            this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
         }
      }

      this.wasFallFlying = this.isFallFlying();
      if (this.isInWater() && this.input.shiftKeyDown && this.isAffectedByFluids()) {
         this.goDownInWater();
      }

      if (this.isEyeInFluid(FluidTags.WATER)) {
         int var10 = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + var10, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (this.getAbilities().flying && this.isControlledCamera()) {
         int var10 = 0;
         if (this.input.shiftKeyDown) {
            var10--;
         }

         if (this.input.jumping) {
            var10++;
         }

         if (var10 != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, var10 * this.getAbilities().getFlyingSpeed() * 3.0F, 0.0));
         }
      }

      PlayerRideableJumping var10 = this.jumpableVehicle();
      if (var10 != null && var10.getJumpCooldown() == 0) {
         if (this.jumpRidingTicks < 0) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (var1 && !this.input.jumping) {
            this.jumpRidingTicks = -10;
            var10.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!var1 && this.input.jumping) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (var1) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround && this.getAbilities().flying && !this.minecraft.gameMode.isAlwaysFlying()) {
         this.getAbilities().flying = false;
         this.onUpdateAbilities();
      }
   }

   @Override
   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime == 20) {
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   private void handleNetherPortalClient() {
      this.oPortalTime = this.portalTime;
      if (this.isInsidePortal) {
         if (this.minecraft.screen != null
            && !this.minecraft.screen.isPauseScreen()
            && !(this.minecraft.screen instanceof DeathScreen)
            && !(this.minecraft.screen instanceof ReceivingLevelScreen)) {
            if (this.minecraft.screen instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.setScreen(null);
         }

         if (this.portalTime == 0.0F) {
            this.minecraft
               .getSoundManager()
               .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         this.portalTime += 0.0125F;
         if (this.portalTime >= 1.0F) {
            this.portalTime = 1.0F;
         }

         this.isInsidePortal = false;
      } else if (this.hasEffect(MobEffects.CONFUSION) && !this.getEffect(MobEffects.CONFUSION).endsWithin(60)) {
         this.portalTime += 0.006666667F;
         if (this.portalTime > 1.0F) {
            this.portalTime = 1.0F;
         }
      } else {
         if (this.portalTime > 0.0F) {
            this.portalTime -= 0.05F;
         }

         if (this.portalTime < 0.0F) {
            this.portalTime = 0.0F;
         }
      }

      this.processPortalCooldown();
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.handsBusy = false;
      if (this.getControlledVehicle() instanceof Boat var1) {
         var1.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
         this.handsBusy = this.handsBusy | (this.input.left || this.input.right || this.input.up || this.input.down);
      }
   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   @Nullable
   @Override
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect param0) {
      if (param0 == MobEffects.CONFUSION) {
         this.oPortalTime = 0.0F;
         this.portalTime = 0.0F;
      }

      return super.removeEffectNoUpdate(param0);
   }

   @Override
   public void move(MoverType param0, Vec3 param1) {
      double var3 = this.getX();
      double var5 = this.getZ();
      super.move(param0, param1);
      this.updateAutoJump((float)(this.getX() - var3), (float)(this.getZ() - var5));
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   protected void updateAutoJump(float param0, float param1) {
      if (this.canAutoJump()) {
         Vec3 var3 = this.position();
         Vec3 var4 = var3.add(param0, 0.0, param1);
         Vec3 var5 = new Vec3(param0, 0.0, param1);
         float var6 = this.getSpeed();
         float var7 = (float)var5.lengthSqr();
         if (var7 <= 0.001F) {
            Vec2 var8 = this.input.getMoveVector();
            float var9 = var6 * var8.x;
            float var10 = var6 * var8.y;
            float var11 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float var12 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            var5 = new Vec3(var9 * var12 - var10 * var11, var5.y, var10 * var12 + var9 * var11);
            var7 = (float)var5.lengthSqr();
            if (var7 <= 0.001F) {
               return;
            }
         }

         float var8 = Mth.invSqrt(var7);
         Vec3 var9 = var5.scale(var8);
         Vec3 var10 = this.getForward();
         float var11 = (float)(var10.x * var9.x + var10.z * var9.z);
         if (!(var11 < -0.15F)) {
            CollisionContext var12 = CollisionContext.of(this);
            BlockPos var13 = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState var14 = this.level.getBlockState(var13);
            if (var14.getCollisionShape(this.level, var13, var12).isEmpty()) {
               var13 = var13.above();
               BlockState var15 = this.level.getBlockState(var13);
               if (var15.getCollisionShape(this.level, var13, var12).isEmpty()) {
                  float var16 = 7.0F;
                  float var17 = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP)) {
                     var17 += (this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
                  }

                  float var18 = Math.max(var6 * 7.0F, 1.0F / var8);
                  Vec3 var19 = var3;
                  Vec3 var20 = var4.add(var9.scale(var18));
                  float var21 = this.getBbWidth();
                  float var22 = this.getBbHeight();
                  AABB var23 = new AABB(var19, var20.add(0.0, var22, 0.0)).inflate(var21, 0.0, var21);
                  var19 = var19.add(0.0, 0.51F, 0.0);
                  var20 = var20.add(0.0, 0.51F, 0.0);
                  Vec3 var24 = var9.cross(new Vec3(0.0, 1.0, 0.0));
                  Vec3 var25 = var24.scale(var21 * 0.5F);
                  Vec3 var26 = var19.subtract(var25);
                  Vec3 var27 = var20.subtract(var25);
                  Vec3 var28 = var19.add(var25);
                  Vec3 var29 = var20.add(var25);
                  Iterable<VoxelShape> var30 = this.level.getCollisions(this, var23);
                  Iterator<AABB> var31 = StreamSupport.stream(var30.spliterator(), false).flatMap(l_param0 -> l_param0.toAabbs().stream()).iterator();
                  float var33 = Float.MIN_VALUE;

                  while (var31.hasNext()) {
                     AABB var35 = var31.next();
                     if (var35.intersects(var26, var27) || var35.intersects(var28, var29)) {
                        var33 = (float)var35.maxY;
                        Vec3 var32 = var35.getCenter();
                        BlockPos var36 = BlockPos.containing(var32);

                        for (int var37 = 1; var37 < var17; var37++) {
                           BlockPos var38 = var36.above(var37);
                           BlockState var39 = this.level.getBlockState(var38);
                           VoxelShape var34;
                           if (!(var34 = var39.getCollisionShape(this.level, var38, var12)).isEmpty()) {
                              var33 = (float)var34.max(Direction.Axis.Y) + var38.getY();
                              if (var33 - this.getY() > var17) {
                                 return;
                              }
                           }

                           if (var37 > 1) {
                              var13 = var13.above();
                              BlockState var40 = this.level.getBlockState(var13);
                              if (!var40.getCollisionShape(this.level, var13, var12).isEmpty()) {
                                 return;
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (var33 != Float.MIN_VALUE) {
                     float var35 = (float)(var33 - this.getY());
                     if (!(var35 <= 0.5F) && !(var35 > var17)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected boolean isHorizontalCollisionMinor(Vec3 param0) {
      float var2 = this.getYRot() * (float) (Math.PI / 180.0);
      double var3 = Mth.sin(var2);
      double var5 = Mth.cos(var2);
      double var7 = this.xxa * var5 - this.zza * var3;
      double var9 = this.zza * var5 + this.xxa * var3;
      double var11 = Mth.square(var7) + Mth.square(var9);
      double var13 = Mth.square(param0.x) + Mth.square(param0.z);
      if (!(var11 < 1.0E-5F) && !(var13 < 1.0E-5F)) {
         double var15 = var7 * param0.x + var9 * param0.z;
         double var17 = Math.acos(var15 / Math.sqrt(var11 * var13));
         return var17 < 0.13962634F;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled()
         && this.autoJumpTime <= 0
         && this.onGround
         && !this.isStayingOnGroundSurface()
         && !this.isPassenger()
         && this.isMoving()
         && this.getBlockJumpFactor() >= 1.0;
   }

   private boolean isMoving() {
      Vec2 var1 = this.input.getMoveVector();
      return var1.x != 0.0F || var1.y != 0.0F;
   }

   private boolean canStartSprinting() {
      return !this.isSprinting()
         && this.hasEnoughImpulseToStartSprinting()
         && this.hasEnoughFoodToStartSprinting()
         && !this.isUsingItem()
         && !this.hasEffect(MobEffects.BLINDNESS)
         && (!this.isPassenger() || this.vehicleCanSprint(this.getVehicle()))
         && !this.isFallFlying();
   }

   private boolean vehicleCanSprint(Entity param0) {
      return param0.canSprint() && param0.isControlledByLocalInstance();
   }

   private boolean hasEnoughImpulseToStartSprinting() {
      double var1 = 0.8;
      return this.isUnderWater() ? this.input.hasForwardImpulse() : this.input.forwardImpulse >= 0.8;
   }

   private boolean hasEnoughFoodToStartSprinting() {
      return this.isPassenger() || this.getFoodData().getFoodLevel() > 6.0F || this.getAbilities().mayfly;
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      }

      float var1 = 600.0F;
      float var2 = 100.0F;
      if (this.waterVisionTime >= 600.0F) {
         return 1.0F;
      }

      float var3 = Mth.clamp(this.waterVisionTime / 100.0F, 0.0F, 1.0F);
      float var4 = this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp((this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
      return var3 * 0.6F + var4 * 0.39999998F;
   }

   @Override
   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   @Override
   protected boolean updateIsUnderwater() {
      boolean var1 = this.wasUnderwater;
      boolean var2 = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      }

      if (!var1 && var2) {
         this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
      }

      if (var1 && !var2) {
         this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }

      return this.wasUnderwater;
   }

   @Override
   public Vec3 getRopeHoldPosition(float param0) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float var2 = Mth.lerp(param0 * 0.5F, this.getYRot(), this.yRotO) * (float) (Math.PI / 180.0);
         float var3 = Mth.lerp(param0 * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
         double var4 = this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
         Vec3 var6 = new Vec3(0.39 * var4, -0.6, 0.3);
         return var6.xRot(-var3).yRot(-var2).add(this.getEyePosition(param0));
      } else {
         return super.getRopeHoldPosition(param0);
      }
   }

   @Override
   public void updateTutorialInventoryAction(ItemStack param0, ItemStack param1, ClickAction param2) {
      this.minecraft.getTutorial().onInventoryAction(param0, param1, param2);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }
}
