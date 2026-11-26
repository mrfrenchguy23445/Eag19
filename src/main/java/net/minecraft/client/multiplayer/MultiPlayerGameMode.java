package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class MultiPlayerGameMode {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final ClientPacketListener connection;
   private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
   private ItemStack destroyingItem = ItemStack.EMPTY;
   private float destroyProgress;
   private float destroyTicks;
   private int destroyDelay;
   private boolean isDestroying;
   private GameType localPlayerMode = GameType.DEFAULT_MODE;
   @Nullable
   private GameType previousLocalPlayerMode;
   private int carriedIndex;

   public MultiPlayerGameMode(Minecraft param0, ClientPacketListener param1) {
      this.minecraft = param0;
      this.connection = param1;
   }

   public void adjustPlayer(Player param0) {
      this.localPlayerMode.updatePlayerAbilities(param0.getAbilities());
   }

   public void setLocalMode(GameType param0, @Nullable GameType param1) {
      this.localPlayerMode = param0;
      this.previousLocalPlayerMode = param1;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public void setLocalMode(GameType param0) {
      if (param0 != this.localPlayerMode) {
         this.previousLocalPlayerMode = this.localPlayerMode;
      }

      this.localPlayerMode = param0;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public boolean canHurtPlayer() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean destroyBlock(BlockPos param0) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, param0, this.localPlayerMode)) {
         return false;
      }

      Level var2 = this.minecraft.level;
      BlockState var3 = var2.getBlockState(param0);
      if (!this.minecraft.player.getMainHandItem().getItem().canAttackBlock(var3, var2, param0, this.minecraft.player)) {
         return false;
      }

      Block var4 = var3.getBlock();
      if (var4 instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
         return false;
      }

      if (var3.isAir()) {
         return false;
      }

      var4.playerWillDestroy(var2, param0, var3, this.minecraft.player);
      FluidState var5 = var2.getFluidState(param0);
      boolean var6 = var2.setBlock(param0, var5.createLegacyBlock(), 11);
      if (var6) {
         var4.destroy(var2, param0, var3);
      }

      return var6;
   }

   public boolean startDestroyBlock(BlockPos param0, Direction param1) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, param0, this.localPlayerMode)) {
         return false;
      }

      if (!this.minecraft.level.getWorldBorder().isWithinBounds(param0)) {
         return false;
      }

      if (this.localPlayerMode.isCreative()) {
         BlockState var3 = this.minecraft.level.getBlockState(param0);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, param0, var3, 1.0F);
         this.startPrediction(this.minecraft.level, l_param2 -> {
            this.destroyBlock(param0);
            return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, param0, param1, l_param2);
         });
         this.destroyDelay = 5;
      } else if (!this.isDestroying || !this.sameDestroyTarget(param0)) {
         if (this.isDestroying) {
            this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, param1));
         }

         BlockState var3 = this.minecraft.level.getBlockState(param0);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, param0, var3, 0.0F);
         this.startPrediction(this.minecraft.level, l_param3 -> {
            boolean l_var5 = !var3.isAir();
            if (l_var5 && this.destroyProgress == 0.0F) {
               var3.attack(this.minecraft.level, param0, this.minecraft.player);
            }

            if (l_var5 && var3.getDestroyProgress(this.minecraft.player, this.minecraft.player.level, param0) >= 1.0F) {
               this.destroyBlock(param0);
            } else {
               this.isDestroying = true;
               this.destroyBlockPos = param0;
               this.destroyingItem = this.minecraft.player.getMainHandItem();
               this.destroyProgress = 0.0F;
               this.destroyTicks = 0.0F;
               this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, (int)(this.destroyProgress * 10.0F) - 1);
            }

            return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, param0, param1, l_param3);
         });
      }

      return true;
   }

   public void stopDestroyBlock() {
      if (this.isDestroying) {
         BlockState var1 = this.minecraft.level.getBlockState(this.destroyBlockPos);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, var1, -1.0F);
         this.connection
            .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN));
         this.isDestroying = false;
         this.destroyProgress = 0.0F;
         this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
         this.minecraft.player.resetAttackStrengthTicker();
      }
   }

   public boolean continueDestroyBlock(BlockPos param0, Direction param1) {
      this.ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         this.destroyDelay--;
         return true;
      }

      if (this.localPlayerMode.isCreative() && this.minecraft.level.getWorldBorder().isWithinBounds(param0)) {
         this.destroyDelay = 5;
         BlockState var3 = this.minecraft.level.getBlockState(param0);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, param0, var3, 1.0F);
         this.startPrediction(this.minecraft.level, l_param2 -> {
            this.destroyBlock(param0);
            return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, param0, param1, l_param2);
         });
         return true;
      }

      if (this.sameDestroyTarget(param0)) {
         BlockState var3 = this.minecraft.level.getBlockState(param0);
         if (var3.isAir()) {
            this.isDestroying = false;
            return false;
         }

         this.destroyProgress = this.destroyProgress + var3.getDestroyProgress(this.minecraft.player, this.minecraft.player.level, param0);
         if (this.destroyTicks % 4.0F == 0.0F) {
            SoundType var4 = var3.getSoundType();
            this.minecraft
               .getSoundManager()
               .play(
                  new SimpleSoundInstance(
                     var4.getHitSound(),
                     SoundSource.BLOCKS,
                     (var4.getVolume() + 1.0F) / 8.0F,
                     var4.getPitch() * 0.5F,
                     SoundInstance.createUnseededRandom(),
                     param0
                  )
               );
         }

         this.destroyTicks++;
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, param0, var3, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
         if (this.destroyProgress >= 1.0F) {
            this.isDestroying = false;
            this.startPrediction(this.minecraft.level, l_param2 -> {
               this.destroyBlock(param0);
               return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, param0, param1, l_param2);
            });
            this.destroyProgress = 0.0F;
            this.destroyTicks = 0.0F;
            this.destroyDelay = 5;
         }

         this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, (int)(this.destroyProgress * 10.0F) - 1);
         return true;
      } else {
         return this.startDestroyBlock(param0, param1);
      }
   }

   private void startPrediction(ClientLevel param0, PredictiveAction param1) {
      try (BlockStatePredictionHandler var3 = param0.getBlockStatePredictionHandler().startPredicting()) {
         int var4 = var3.currentSequence();
         Packet<ServerGamePacketListener> var5 = param1.predict(var4);
         this.connection.send(var5);
      }
   }

   public float getPickRange() {
      return this.localPlayerMode.isCreative() ? 5.0F : 4.5F;
   }

   public void tick() {
      this.ensureHasSentCarriedItem();
      if (this.connection.getConnection().isConnected()) {
         this.connection.getConnection().tick();
      } else {
         this.connection.getConnection().handleDisconnection();
      }
   }

   private boolean sameDestroyTarget(BlockPos param0) {
      ItemStack var2 = this.minecraft.player.getMainHandItem();
      boolean var3 = this.destroyingItem.isEmpty() && var2.isEmpty();
      if (!this.destroyingItem.isEmpty() && !var2.isEmpty()) {
         var3 = var2.is(this.destroyingItem.getItem())
            && ItemStack.tagMatches(var2, this.destroyingItem)
            && (var2.isDamageableItem() || var2.getDamageValue() == this.destroyingItem.getDamageValue());
      }

      return param0.equals(this.destroyBlockPos) && var3;
   }

   private void ensureHasSentCarriedItem() {
      int var1 = this.minecraft.player.getInventory().selected;
      if (var1 != this.carriedIndex) {
         this.carriedIndex = var1;
         this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
      }
   }

   public InteractionResult useItemOn(LocalPlayer param0, InteractionHand param1, BlockHitResult param2) {
      this.ensureHasSentCarriedItem();
      if (!this.minecraft.level.getWorldBorder().isWithinBounds(param2.getBlockPos())) {
         return InteractionResult.FAIL;
      }

      MutableObject<InteractionResult> var4 = new MutableObject<>();
      this.startPrediction(this.minecraft.level, l_param4 -> {
         var4.setValue(this.performUseItemOn(param0, param1, param2));
         return new ServerboundUseItemOnPacket(param1, param2, l_param4);
      });
      return var4.getValue();
   }

   private InteractionResult performUseItemOn(LocalPlayer param0, InteractionHand param1, BlockHitResult param2) {
      BlockPos var4 = param2.getBlockPos();
      ItemStack var5 = param0.getItemInHand(param1);
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.SUCCESS;
      }

      boolean var6 = !param0.getMainHandItem().isEmpty() || !param0.getOffhandItem().isEmpty();
      boolean var7 = param0.isSecondaryUseActive() && var6;
      if (!var7) {
         BlockState var8 = this.minecraft.level.getBlockState(var4);
         if (!this.connection.isFeatureEnabled(var8.getBlock().requiredFeatures())) {
            return InteractionResult.FAIL;
         }

         InteractionResult var9 = var8.use(this.minecraft.level, param0, param1, param2);
         if (var9.consumesAction()) {
            return var9;
         }
      }

      if (!var5.isEmpty() && !param0.getCooldowns().isOnCooldown(var5.getItem())) {
         UseOnContext var9 = new UseOnContext(param0, param1, param2);
         InteractionResult var8;
         if (this.localPlayerMode.isCreative()) {
            int var10 = var5.getCount();
            var8 = var5.useOn(var9);
            var5.setCount(var10);
         } else {
            var8 = var5.useOn(var9);
         }

         return var8;
      } else {
         return InteractionResult.PASS;
      }
   }

   public InteractionResult useItem(Player param0, InteractionHand param1) {
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      }

      this.ensureHasSentCarriedItem();
      this.connection
         .send(new ServerboundMovePlayerPacket.PosRot(param0.getX(), param0.getY(), param0.getZ(), param0.getYRot(), param0.getXRot(), param0.isOnGround()));
      MutableObject<InteractionResult> var3 = new MutableObject<>();
      this.startPrediction(this.minecraft.level, l_param3 -> {
         ServerboundUseItemPacket l_var5 = new ServerboundUseItemPacket(param1, l_param3);
         ItemStack l_var6 = param0.getItemInHand(param1);
         if (param0.getCooldowns().isOnCooldown(l_var6.getItem())) {
            var3.setValue(InteractionResult.PASS);
            return l_var5;
         }

         InteractionResultHolder<ItemStack> l_var7 = l_var6.use(this.minecraft.level, param0, param1);
         ItemStack l_var8 = l_var7.getObject();
         if (l_var8 != l_var6) {
            param0.setItemInHand(param1, l_var8);
         }

         var3.setValue(l_var7.getResult());
         return l_var5;
      });
      return var3.getValue();
   }

   public LocalPlayer createPlayer(ClientLevel param0, StatsCounter param1, ClientRecipeBook param2) {
      return this.createPlayer(param0, param1, param2, false, false);
   }

   public LocalPlayer createPlayer(ClientLevel param0, StatsCounter param1, ClientRecipeBook param2, boolean param3, boolean param4) {
      return new LocalPlayer(this.minecraft, param0, this.connection, param1, param2, param3, param4);
   }

   public void attack(Player param0, Entity param1) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createAttackPacket(param1, param0.isShiftKeyDown()));
      if (this.localPlayerMode != GameType.SPECTATOR) {
         param0.attack(param1);
         param0.resetAttackStrengthTicker();
      }
   }

   public InteractionResult interact(Player param0, Entity param1, InteractionHand param2) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(param1, param0.isShiftKeyDown(), param2));
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : param0.interactOn(param1, param2);
   }

   public InteractionResult interactAt(Player param0, Entity param1, EntityHitResult param2, InteractionHand param3) {
      this.ensureHasSentCarriedItem();
      Vec3 var5 = param2.getLocation().subtract(param1.getX(), param1.getY(), param1.getZ());
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(param1, param0.isShiftKeyDown(), param3, var5));
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : param1.interactAt(param0, var5, param3);
   }

   public void handleInventoryMouseClick(int param0, int param1, int param2, ClickType param3, Player param4) {
      AbstractContainerMenu var6 = param4.containerMenu;
      if (param0 != var6.containerId) {
         LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", param0, var6.containerId);
      } else {
         NonNullList<Slot> var7 = var6.slots;
         int var8 = var7.size();
         List<ItemStack> var9 = Lists.newArrayListWithCapacity(var8);

         for (Slot var11 : var7) {
            var9.add(var11.getItem().copy());
         }

         var6.clicked(param1, param2, param3, param4);
         Int2ObjectMap<ItemStack> var10 = new Int2ObjectOpenHashMap<>();

         for (int var11 = 0; var11 < var8; var11++) {
            ItemStack var12 = var9.get(var11);
            ItemStack var13 = var7.get(var11).getItem();
            if (!ItemStack.matches(var12, var13)) {
               var10.put(var11, var13.copy());
            }
         }

         this.connection.send(new ServerboundContainerClickPacket(param0, var6.getStateId(), param1, param2, param3, var6.getCarried().copy(), var10));
      }
   }

   public void handlePlaceRecipe(int param0, Recipe<?> param1, boolean param2) {
      this.connection.send(new ServerboundPlaceRecipePacket(param0, param1, param2));
   }

   public void handleInventoryButtonClick(int param0, int param1) {
      this.connection.send(new ServerboundContainerButtonClickPacket(param0, param1));
   }

   public void handleCreativeModeItemAdd(ItemStack param0, int param1) {
      if (this.localPlayerMode.isCreative() && this.connection.isFeatureEnabled(param0.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(param1, param0));
      }
   }

   public void handleCreativeModeItemDrop(ItemStack param0) {
      if (this.localPlayerMode.isCreative() && !param0.isEmpty() && this.connection.isFeatureEnabled(param0.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, param0));
      }
   }

   public void releaseUsingItem(Player param0) {
      this.ensureHasSentCarriedItem();
      this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
      param0.releaseUsingItem();
   }

   public boolean hasExperience() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean hasMissTime() {
      return !this.localPlayerMode.isCreative();
   }

   public boolean hasInfiniteItems() {
      return this.localPlayerMode.isCreative();
   }

   public boolean hasFarPickRange() {
      return this.localPlayerMode.isCreative();
   }

   public boolean isServerControlledInventory() {
      return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
   }

   public boolean isAlwaysFlying() {
      return this.localPlayerMode == GameType.SPECTATOR;
   }

   @Nullable
   public GameType getPreviousPlayerMode() {
      return this.previousLocalPlayerMode;
   }

   public GameType getPlayerMode() {
      return this.localPlayerMode;
   }

   public boolean isDestroying() {
      return this.isDestroying;
   }

   public void handlePickItem(int param0) {
      this.connection.send(new ServerboundPickItemPacket(param0));
   }
}
