package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
   private final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo param0) {
      this.testInfo = param0;
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos param0) {
      return this.getLevel().getBlockState(this.absolutePos(param0));
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos param0) {
      return this.getLevel().getBlockEntity(this.absolutePos(param0));
   }

   public void killAllEntities() {
      AABB var1 = this.getBounds();
      List<Entity> var2 = this.getLevel().getEntitiesOfClass(Entity.class, var1.inflate(1.0), l_param0 -> !(l_param0 instanceof Player));
      var2.forEach(Entity::kill);
   }

   public ItemEntity spawnItem(Item param0, float param1, float param2, float param3) {
      ServerLevel var5 = this.getLevel();
      Vec3 var6 = this.absoluteVec(new Vec3(param1, param2, param3));
      ItemEntity var7 = new ItemEntity(var5, var6.x, var6.y, var6.z, new ItemStack(param0, 1));
      var7.setDeltaMovement(0.0, 0.0, 0.0);
      var5.addFreshEntity(var7);
      return var7;
   }

   public ItemEntity spawnItem(Item param0, BlockPos param1) {
      return this.spawnItem(param0, param1.getX(), param1.getY(), param1.getZ());
   }

   public <E extends Entity> E spawn(EntityType<E> param0, BlockPos param1) {
      return this.spawn(param0, Vec3.atBottomCenterOf(param1));
   }

   public <E extends Entity> E spawn(EntityType<E> param0, Vec3 param1) {
      ServerLevel var3 = this.getLevel();
      E var4 = param0.create(var3);
      if (var4 == null) {
         throw new NullPointerException("Failed to create entity " + param0.builtInRegistryHolder().key().location());
      }

      if (var4 instanceof Mob var5) {
         var5.setPersistenceRequired();
      }

      Vec3 var5 = this.absoluteVec(param1);
      var4.moveTo(var5.x, var5.y, var5.z, var4.getYRot(), var4.getXRot());
      var3.addFreshEntity(var4);
      return var4;
   }

   public <E extends Entity> E spawn(EntityType<E> param0, int param1, int param2, int param3) {
      return this.spawn(param0, new BlockPos(param1, param2, param3));
   }

   public <E extends Entity> E spawn(EntityType<E> param0, float param1, float param2, float param3) {
      return this.spawn(param0, new Vec3(param1, param2, param3));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> param0, BlockPos param1) {
      E var3 = (E)this.spawn(param0, param1);
      var3.removeFreeWill();
      return var3;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> param0, int param1, int param2, int param3) {
      return this.spawnWithNoFreeWill(param0, new BlockPos(param1, param2, param3));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> param0, Vec3 param1) {
      E var3 = (E)this.spawn(param0, param1);
      var3.removeFreeWill();
      return var3;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> param0, float param1, float param2, float param3) {
      return this.spawnWithNoFreeWill(param0, new Vec3(param1, param2, param3));
   }

   public GameTestSequence walkTo(Mob param0, BlockPos param1, float param2) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path l_var4 = param0.getNavigation().createPath(this.absolutePos(param1), 0);
         param0.getNavigation().moveTo(l_var4, param2);
      });
   }

   public void pressButton(int param0, int param1, int param2) {
      this.pressButton(new BlockPos(param0, param1, param2));
   }

   public void pressButton(BlockPos param0) {
      this.assertBlockState(param0, l_param0 -> l_param0.is(BlockTags.BUTTONS), () -> "Expected button");
      BlockPos var2 = this.absolutePos(param0);
      BlockState var3 = this.getLevel().getBlockState(var2);
      ButtonBlock var4 = (ButtonBlock)var3.getBlock();
      var4.press(var3, this.getLevel(), var2);
   }

   public void useBlock(BlockPos param0) {
      this.useBlock(param0, this.makeMockPlayer());
   }

   public void useBlock(BlockPos param0, Player param1) {
      BlockPos var3 = this.absolutePos(param0);
      this.useBlock(param0, param1, new BlockHitResult(Vec3.atCenterOf(var3), Direction.NORTH, var3, true));
   }

   public void useBlock(BlockPos param0, Player param1, BlockHitResult param2) {
      BlockPos var4 = this.absolutePos(param0);
      BlockState var5 = this.getLevel().getBlockState(var4);
      InteractionResult var6 = var5.use(this.getLevel(), param1, InteractionHand.MAIN_HAND, param2);
      if (!var6.consumesAction()) {
         UseOnContext var7 = new UseOnContext(param1, InteractionHand.MAIN_HAND, param2);
         param1.getItemInHand(InteractionHand.MAIN_HAND).useOn(var7);
      }
   }

   public void continuouslyUse(BlockPos param0, Player param1, BlockHitResult param2) {
      BlockPos var4 = this.absolutePos(param0);
      BlockState var5 = this.getLevel().getBlockState(var4);
      InteractionResult var6 = var5.use(this.getLevel(), param1, InteractionHand.MAIN_HAND, param2);
      if (!var6.consumesAction()) {
         UseOnContext var7 = new UseOnContext(param1, InteractionHand.MAIN_HAND, param2);
         ItemStack var8 = param1.getItemInHand(InteractionHand.MAIN_HAND);
         if (param1.isUsingItem()) {
            var8.onUseTick(this.getLevel(), param1, param1.getUseItemRemainingTicks());
         } else {
            var8.useOn(var7);
         }
      }
   }

   public LivingEntity makeAboutToDrown(LivingEntity param0) {
      param0.setAirSupply(0);
      param0.setHealth(0.25F);
      return param0;
   }

   public Player makeMockSurvivalPlayer() {
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public boolean isSpectator() {
            return false;
         }

         @Override
         public boolean isCreative() {
            return false;
         }
      };
   }

   public Player makeMockPlayer() {
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public boolean isSpectator() {
            return false;
         }

         @Override
         public boolean isCreative() {
            return true;
         }

         @Override
         public boolean isLocalPlayer() {
            return true;
         }
      };
   }

   public void pullLever(int param0, int param1, int param2) {
      this.pullLever(new BlockPos(param0, param1, param2));
   }

   public void pullLever(BlockPos param0) {
      this.assertBlockPresent(Blocks.LEVER, param0);
      BlockPos var2 = this.absolutePos(param0);
      BlockState var3 = this.getLevel().getBlockState(var2);
      LeverBlock var4 = (LeverBlock)var3.getBlock();
      var4.pull(var3, this.getLevel(), var2);
   }

   public void pulseRedstone(BlockPos param0, long param1) {
      this.setBlock(param0, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(param1, () -> this.setBlock(param0, Blocks.AIR));
   }

   public void destroyBlock(BlockPos param0) {
      this.getLevel().destroyBlock(this.absolutePos(param0), false, null);
   }

   public void setBlock(int param0, int param1, int param2, Block param3) {
      this.setBlock(new BlockPos(param0, param1, param2), param3);
   }

   public void setBlock(int param0, int param1, int param2, BlockState param3) {
      this.setBlock(new BlockPos(param0, param1, param2), param3);
   }

   public void setBlock(BlockPos param0, Block param1) {
      this.setBlock(param0, param1.defaultBlockState());
   }

   public void setBlock(BlockPos param0, BlockState param1) {
      this.getLevel().setBlock(this.absolutePos(param0), param1, 3);
   }

   public void setNight() {
      this.setDayTime(13000);
   }

   public void setDayTime(int param0) {
      this.getLevel().setDayTime(param0);
   }

   public void assertBlockPresent(Block param0, int param1, int param2, int param3) {
      this.assertBlockPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void assertBlockPresent(Block param0, BlockPos param1) {
      BlockState var3 = this.getBlockState(param1);
      this.assertBlock(param1, l_param2 -> var3.is(param0), "Expected " + param0.getName().getString() + ", got " + var3.getBlock().getName().getString());
   }

   public void assertBlockNotPresent(Block param0, int param1, int param2, int param3) {
      this.assertBlockNotPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void assertBlockNotPresent(Block param0, BlockPos param1) {
      this.assertBlock(param1, l_param2 -> !this.getBlockState(param1).is(param0), "Did not expect " + param0.getName().getString());
   }

   public void succeedWhenBlockPresent(Block param0, int param1, int param2, int param3) {
      this.succeedWhenBlockPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void succeedWhenBlockPresent(Block param0, BlockPos param1) {
      this.succeedWhen(() -> this.assertBlockPresent(param0, param1));
   }

   public void assertBlock(BlockPos param0, Predicate<Block> param1, String param2) {
      this.assertBlock(param0, param1, () -> param2);
   }

   public void assertBlock(BlockPos param0, Predicate<Block> param1, Supplier<String> param2) {
      this.assertBlockState(param0, l_param1 -> param1.test(l_param1.getBlock()), param2);
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos param0, Property<T> param1, T param2) {
      BlockState var4 = this.getBlockState(param0);
      boolean var5 = var4.hasProperty(param1);
      if (!var5 || !var4.getValue(param1).equals(param2)) {
         String var6 = var5 ? "was " + var4.getValue(param1) : "property " + param1.getName() + " is missing";
         String var7 = String.format(Locale.ROOT, "Expected property %s to be %s, %s", param1.getName(), param2, var6);
         throw new GameTestAssertPosException(var7, this.absolutePos(param0), param0, this.testInfo.getTick());
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos param0, Property<T> param1, Predicate<T> param2, String param3) {
      this.assertBlockState(param0, l_param2 -> param2.test(l_param2.getValue(param1)), () -> param3);
   }

   public void assertBlockState(BlockPos param0, Predicate<BlockState> param1, Supplier<String> param2) {
      BlockState var4 = this.getBlockState(param0);
      if (!param1.test(var4)) {
         throw new GameTestAssertPosException(param2.get(), this.absolutePos(param0), param0, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> param0) {
      List<? extends Entity> var2 = this.getLevel().getEntities(param0, this.getBounds(), Entity::isAlive);
      if (var2.isEmpty()) {
         throw new GameTestAssertException("Expected " + param0.toShortString() + " to exist");
      }
   }

   public void assertEntityPresent(EntityType<?> param0, int param1, int param2, int param3) {
      this.assertEntityPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void assertEntityPresent(EntityType<?> param0, BlockPos param1) {
      BlockPos var3 = this.absolutePos(param1);
      List<? extends Entity> var4 = this.getLevel().getEntities(param0, new AABB(var3), Entity::isAlive);
      if (var4.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + param0.toShortString(), var3, param1, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> param0, Vec3 param1, Vec3 param2) {
      List<? extends Entity> var4 = this.getLevel().getEntities(param0, new AABB(param1, param2), Entity::isAlive);
      if (var4.isEmpty()) {
         throw new GameTestAssertPosException(
            "Expected " + param0.toShortString() + " between ", BlockPos.containing(param1), BlockPos.containing(param2), this.testInfo.getTick()
         );
      }
   }

   public void assertEntitiesPresent(EntityType<?> param0, BlockPos param1, int param2, double param3) {
      BlockPos var6 = this.absolutePos(param1);
      List<? extends Entity> var7 = this.getEntities((EntityType<? extends Entity>)param0, param1, param3);
      if (var7.size() != param2) {
         throw new GameTestAssertPosException(
            "Expected " + param2 + " entities of type " + param0.toShortString() + ", actual number of entities found=" + var7.size(),
            var6,
            param1,
            this.testInfo.getTick()
         );
      }
   }

   public void assertEntityPresent(EntityType<?> param0, BlockPos param1, double param2) {
      List<? extends Entity> var5 = this.getEntities((EntityType<? extends Entity>)param0, param1, param2);
      if (var5.isEmpty()) {
         BlockPos var6 = this.absolutePos(param1);
         throw new GameTestAssertPosException("Expected " + param0.toShortString(), var6, param1, this.testInfo.getTick());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> param0, BlockPos param1, double param2) {
      BlockPos var5 = this.absolutePos(param1);
      return this.getLevel().getEntities(param0, new AABB(var5).inflate(param2), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity param0, int param1, int param2, int param3) {
      this.assertEntityInstancePresent(param0, new BlockPos(param1, param2, param3));
   }

   public void assertEntityInstancePresent(Entity param0, BlockPos param1) {
      BlockPos var3 = this.absolutePos(param1);
      List<? extends Entity> var4 = this.getLevel().getEntities(param0.getType(), new AABB(var3), Entity::isAlive);
      var4.stream()
         .filter(l_param1 -> l_param1 == param0)
         .findFirst()
         .orElseThrow(() -> new GameTestAssertPosException("Expected " + param0.getType().toShortString(), var3, param1, this.testInfo.getTick()));
   }

   public void assertItemEntityCountIs(Item param0, BlockPos param1, double param2, int param4) {
      BlockPos var6 = this.absolutePos(param1);
      List<ItemEntity> var7 = this.getLevel().getEntities(EntityType.ITEM, new AABB(var6).inflate(param2), Entity::isAlive);
      int var8 = 0;

      for (Entity var10 : var7) {
         ItemEntity var11 = (ItemEntity)var10;
         if (var11.getItem().getItem().equals(param0)) {
            var8 += var11.getItem().getCount();
         }
      }

      if (var8 != param4) {
         throw new GameTestAssertPosException(
            "Expected " + param4 + " " + param0.getDescription().getString() + " items to exist (found " + var8 + ")", var6, param1, this.testInfo.getTick()
         );
      }
   }

   public void assertItemEntityPresent(Item param0, BlockPos param1, double param2) {
      BlockPos var5 = this.absolutePos(param1);

      for (Entity var8 : this.getLevel().getEntities(EntityType.ITEM, new AABB(var5).inflate(param2), Entity::isAlive)) {
         ItemEntity var9 = (ItemEntity)var8;
         if (var9.getItem().getItem().equals(param0)) {
            return;
         }
      }

      throw new GameTestAssertPosException("Expected " + param0.getDescription().getString() + " item", var5, param1, this.testInfo.getTick());
   }

   public void assertItemEntityNotPresent(Item param0, BlockPos param1, double param2) {
      BlockPos var5 = this.absolutePos(param1);

      for (Entity var8 : this.getLevel().getEntities(EntityType.ITEM, new AABB(var5).inflate(param2), Entity::isAlive)) {
         ItemEntity var9 = (ItemEntity)var8;
         if (var9.getItem().getItem().equals(param0)) {
            throw new GameTestAssertPosException("Did not expect " + param0.getDescription().getString() + " item", var5, param1, this.testInfo.getTick());
         }
      }
   }

   public void assertEntityNotPresent(EntityType<?> param0) {
      List<? extends Entity> var2 = this.getLevel().getEntities(param0, this.getBounds(), Entity::isAlive);
      if (!var2.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + param0.toShortString() + " to exist");
      }
   }

   public void assertEntityNotPresent(EntityType<?> param0, int param1, int param2, int param3) {
      this.assertEntityNotPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void assertEntityNotPresent(EntityType<?> param0, BlockPos param1) {
      BlockPos var3 = this.absolutePos(param1);
      List<? extends Entity> var4 = this.getLevel().getEntities(param0, new AABB(var3), Entity::isAlive);
      if (!var4.isEmpty()) {
         throw new GameTestAssertPosException("Did not expect " + param0.toShortString(), var3, param1, this.testInfo.getTick());
      }
   }

   public void assertEntityTouching(EntityType<?> param0, double param1, double param3, double param5) {
      Vec3 var8 = new Vec3(param1, param3, param5);
      Vec3 var9 = this.absoluteVec(var8);
      Predicate<? super Entity> var10 = l_param1 -> l_param1.getBoundingBox().intersects(var9, var9);
      List<? extends Entity> var11 = this.getLevel().getEntities(param0, this.getBounds(), var10);
      if (var11.isEmpty()) {
         throw new GameTestAssertException("Expected " + param0.toShortString() + " to touch " + var9 + " (relative " + var8 + ")");
      }
   }

   public void assertEntityNotTouching(EntityType<?> param0, double param1, double param3, double param5) {
      Vec3 var8 = new Vec3(param1, param3, param5);
      Vec3 var9 = this.absoluteVec(var8);
      Predicate<? super Entity> var10 = l_param1 -> !l_param1.getBoundingBox().intersects(var9, var9);
      List<? extends Entity> var11 = this.getLevel().getEntities(param0, this.getBounds(), var10);
      if (var11.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + param0.toShortString() + " to touch " + var9 + " (relative " + var8 + ")");
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos param0, EntityType<E> param1, Function<? super E, T> param2, @Nullable T param3) {
      BlockPos var5 = this.absolutePos(param0);
      List<E> var6 = this.getLevel().getEntities(param1, new AABB(var5), Entity::isAlive);
      if (var6.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + param1.toShortString(), var5, param0, this.testInfo.getTick());
      }

      for (E var8 : var6) {
         T var9 = param2.apply(var8);
         if (var9 == null) {
            if (param3 != null) {
               throw new GameTestAssertException("Expected entity data to be: " + param3 + ", but was: " + var9);
            }
         } else if (!var9.equals(param3)) {
            throw new GameTestAssertException("Expected entity data to be: " + param3 + ", but was: " + var9);
         }
      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos param0, EntityType<E> param1, Item param2) {
      BlockPos var4 = this.absolutePos(param0);
      List<E> var5 = this.getLevel().getEntities(param1, new AABB(var4), Entity::isAlive);
      if (var5.isEmpty()) {
         throw new GameTestAssertPosException("Expected entity of type: " + param1, var4, param0, this.getTick());
      }

      for (E var7 : var5) {
         if (var7.isHolding(param2)) {
            return;
         }
      }

      throw new GameTestAssertPosException("Entity should be holding: " + param2, var4, param0, this.getTick());
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos param0, EntityType<E> param1, Item param2) {
      BlockPos var4 = this.absolutePos(param0);
      List<E> var5 = this.getLevel().getEntities(param1, new AABB(var4), l_param0 -> l_param0.isAlive());
      if (var5.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + param1.toShortString() + " to exist", var4, param0, this.getTick());
      }

      for (E var7 : var5) {
         if (var7.getInventory().hasAnyMatching(l_param1 -> l_param1.is(param2))) {
            return;
         }
      }

      throw new GameTestAssertPosException("Entity inventory should contain: " + param2, var4, param0, this.getTick());
   }

   public void assertContainerEmpty(BlockPos param0) {
      BlockPos var2 = this.absolutePos(param0);
      BlockEntity var3 = this.getLevel().getBlockEntity(var2);
      if (var3 instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)var3).isEmpty()) {
         throw new GameTestAssertException("Container should be empty");
      }
   }

   public void assertContainerContains(BlockPos param0, Item param1) {
      BlockPos var3 = this.absolutePos(param0);
      BlockEntity var4 = this.getLevel().getBlockEntity(var3);
      if (!(var4 instanceof BaseContainerBlockEntity)) {
         throw new GameTestAssertException("Expected a container at " + param0 + ", found " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(var4.getType()));
      }

      if (((BaseContainerBlockEntity)var4).countItem(param1) != 1) {
         throw new GameTestAssertException("Container should contain: " + param1);
      }
   }

   public void assertSameBlockStates(BoundingBox param0, BlockPos param1) {
      BlockPos.betweenClosedStream(param0).forEach(l_param2 -> {
         BlockPos l_var4 = param1.offset(l_param2.getX() - param0.minX(), l_param2.getY() - param0.minY(), l_param2.getZ() - param0.minZ());
         this.assertSameBlockState(l_param2, l_var4);
      });
   }

   public void assertSameBlockState(BlockPos param0, BlockPos param1) {
      BlockState var3 = this.getBlockState(param0);
      BlockState var4 = this.getBlockState(param1);
      if (var3 != var4) {
         this.fail("Incorrect state. Expected " + var4 + ", got " + var3, param0);
      }
   }

   public void assertAtTickTimeContainerContains(long param0, BlockPos param2, Item param3) {
      this.runAtTickTime(param0, () -> this.assertContainerContains(param2, param3));
   }

   public void assertAtTickTimeContainerEmpty(long param0, BlockPos param2) {
      this.runAtTickTime(param0, () -> this.assertContainerEmpty(param2));
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos param0, EntityType<E> param1, Function<E, T> param2, T param3) {
      this.succeedWhen(() -> this.assertEntityData(param0, param1, param2, param3));
   }

   public <E extends Entity> void assertEntityProperty(E param0, Predicate<E> param1, String param2) {
      if (!param1.test(param0)) {
         throw new GameTestAssertException("Entity " + param0 + " failed " + param2 + " test");
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E param0, Function<E, T> param1, String param2, T param3) {
      T var5 = param1.apply(param0);
      if (!var5.equals(param3)) {
         throw new GameTestAssertException("Entity " + param0 + " value " + param2 + "=" + var5 + " is not equal to expected " + param3);
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> param0, int param1, int param2, int param3) {
      this.succeedWhenEntityPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void succeedWhenEntityPresent(EntityType<?> param0, BlockPos param1) {
      this.succeedWhen(() -> this.assertEntityPresent(param0, param1));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> param0, int param1, int param2, int param3) {
      this.succeedWhenEntityNotPresent(param0, new BlockPos(param1, param2, param3));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> param0, BlockPos param1) {
      this.succeedWhen(() -> this.assertEntityNotPresent(param0, param1));
   }

   public void succeed() {
      this.testInfo.succeed();
   }

   private void ensureSingleFinalCheck() {
      if (this.finalCheckAdded) {
         throw new IllegalStateException("This test already has final clause");
      }

      this.finalCheckAdded = true;
   }

   public void succeedIf(Runnable param0) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, param0).thenSucceed();
   }

   public void succeedWhen(Runnable param0) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(param0).thenSucceed();
   }

   public void succeedOnTickWhen(int param0, Runnable param1) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(param0, param1).thenSucceed();
   }

   public void runAtTickTime(long param0, Runnable param2) {
      this.testInfo.setRunAtTickTime(param0, param2);
   }

   public void runAfterDelay(long param0, Runnable param2) {
      this.runAtTickTime(this.testInfo.getTick() + param0, param2);
   }

   public void randomTick(BlockPos param0) {
      BlockPos var2 = this.absolutePos(param0);
      ServerLevel var3 = this.getLevel();
      var3.getBlockState(var2).randomTick(var3, var2, var3.random);
   }

   public int getHeight(Heightmap.Types param0, int param1, int param2) {
      BlockPos var4 = this.absolutePos(new BlockPos(param1, 0, param2));
      return this.relativePos(this.getLevel().getHeightmapPos(param0, var4)).getY();
   }

   public void fail(String param0, BlockPos param1) {
      throw new GameTestAssertPosException(param0, this.absolutePos(param1), param1, this.getTick());
   }

   public void fail(String param0, Entity param1) {
      throw new GameTestAssertPosException(param0, param1.blockPosition(), this.relativePos(param1.blockPosition()), this.getTick());
   }

   public void fail(String param0) {
      throw new GameTestAssertException(param0);
   }

   public void failIf(Runnable param0) {
      this.testInfo.createSequence().thenWaitUntil(param0).thenFail(() -> new GameTestAssertException("Fail conditions met"));
   }

   public void failIfEver(Runnable param0) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(l_param1 -> this.testInfo.setRunAtTickTime(l_param1, param0::run));
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos param0) {
      BlockPos var2 = this.testInfo.getStructureBlockPos();
      BlockPos var3 = var2.offset(param0);
      return StructureTemplate.transform(var3, Mirror.NONE, this.testInfo.getRotation(), var2);
   }

   public BlockPos relativePos(BlockPos param0) {
      BlockPos var2 = this.testInfo.getStructureBlockPos();
      Rotation var3 = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos var4 = StructureTemplate.transform(param0, Mirror.NONE, var3, var2);
      return var4.subtract(var2);
   }

   public Vec3 absoluteVec(Vec3 param0) {
      Vec3 var2 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(var2.add(param0), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public Vec3 relativeVec(Vec3 param0) {
      Vec3 var2 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(param0.subtract(var2), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public void assertTrue(boolean param0, String param1) {
      if (!param0) {
         throw new GameTestAssertException(param1);
      }
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   private AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   private AABB getRelativeBounds() {
      AABB var1 = this.testInfo.getStructureBounds();
      return var1.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> param0) {
      AABB var2 = this.getRelativeBounds();
      BlockPos.MutableBlockPos.betweenClosedStream(var2.move(0.0, 1.0, 0.0)).forEach(param0);
   }

   public void onEachTick(Runnable param0) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(l_param1 -> this.testInfo.setRunAtTickTime(l_param1, param0::run));
   }

   public void placeAt(Player param0, ItemStack param1, BlockPos param2, Direction param3) {
      BlockPos var5 = this.absolutePos(param2.relative(param3));
      BlockHitResult var6 = new BlockHitResult(Vec3.atCenterOf(var5), param3, var5, false);
      UseOnContext var7 = new UseOnContext(param0, InteractionHand.MAIN_HAND, var6);
      param1.useOn(var7);
   }
}
