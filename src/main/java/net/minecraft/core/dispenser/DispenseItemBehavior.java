package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public interface DispenseItemBehavior {
   Logger LOGGER = LogUtils.getLogger();
   DispenseItemBehavior NOOP = (param0, param1) -> param1;

   ItemStack dispense(BlockSource var1, ItemStack var2);

   static void bootStrap() {
      DispenserBlock.registerBehavior(Items.ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            Arrow l_var4 = new Arrow(l_param0, l_param1.x(), l_param1.y(), l_param1.z());
            l_var4.pickup = AbstractArrow.Pickup.ALLOWED;
            return l_var4;
         }
      });
      DispenserBlock.registerBehavior(Items.TIPPED_ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            Arrow l_var4 = new Arrow(l_param0, l_param1.x(), l_param1.y(), l_param1.z());
            l_var4.setEffectsFromItem(l_param2);
            l_var4.pickup = AbstractArrow.Pickup.ALLOWED;
            return l_var4;
         }
      });
      DispenserBlock.registerBehavior(Items.SPECTRAL_ARROW, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            AbstractArrow l_var4 = new SpectralArrow(l_param0, l_param1.x(), l_param1.y(), l_param1.z());
            l_var4.pickup = AbstractArrow.Pickup.ALLOWED;
            return l_var4;
         }
      });
      DispenserBlock.registerBehavior(Items.EGG, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            return Util.make(new ThrownEgg(l_param0, l_param1.x(), l_param1.y(), l_param1.z()), l2_param1 -> l2_param1.setItem(l_param2));
         }
      });
      DispenserBlock.registerBehavior(Items.SNOWBALL, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            return Util.make(new Snowball(l_param0, l_param1.x(), l_param1.y(), l_param1.z()), l2_param1 -> l2_param1.setItem(l_param2));
         }
      });
      DispenserBlock.registerBehavior(Items.EXPERIENCE_BOTTLE, new AbstractProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level l_param0, Position l_param1, ItemStack l_param2) {
            return Util.make(new ThrownExperienceBottle(l_param0, l_param1.x(), l_param1.y(), l_param1.z()), l2_param1 -> l2_param1.setItem(l_param2));
         }

         @Override
         protected float getUncertainty() {
            return super.getUncertainty() * 0.5F;
         }

         @Override
         protected float getPower() {
            return super.getPower() * 1.25F;
         }
      });
      DispenserBlock.registerBehavior(Items.SPLASH_POTION, new DispenseItemBehavior() {
         @Override
         public ItemStack dispense(BlockSource l_param0, ItemStack l_param1) {
            return (new AbstractProjectileDispenseBehavior() {
               @Override
               protected Projectile getProjectile(Level l2_param0, Position l2_param1, ItemStack l2_param2) {
                  return Util.make(new ThrownPotion(l2_param0, l2_param1.x(), l2_param1.y(), l2_param1.z()), l3_param1 -> l3_param1.setItem(l2_param2));
               }

               @Override
               protected float getUncertainty() {
                  return super.getUncertainty() * 0.5F;
               }

               @Override
               protected float getPower() {
                  return super.getPower() * 1.25F;
               }
            }).dispense(l_param0, l_param1);
         }
      });
      DispenserBlock.registerBehavior(Items.LINGERING_POTION, new DispenseItemBehavior() {
         @Override
         public ItemStack dispense(BlockSource l_param0, ItemStack l_param1) {
            return (new AbstractProjectileDispenseBehavior() {
               @Override
               protected Projectile getProjectile(Level l2_param0, Position l2_param1, ItemStack l2_param2) {
                  return Util.make(new ThrownPotion(l2_param0, l2_param1.x(), l2_param1.y(), l2_param1.z()), l3_param1 -> l3_param1.setItem(l2_param2));
               }

               @Override
               protected float getUncertainty() {
                  return super.getUncertainty() * 0.5F;
               }

               @Override
               protected float getPower() {
                  return super.getPower() * 1.25F;
               }
            }).dispense(l_param0, l_param1);
         }
      });
      DefaultDispenseItemBehavior var0 = new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Direction l_var3 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            EntityType<?> l_var4 = ((SpawnEggItem)l_param1.getItem()).getType(l_param1.getTag());

            try {
               l_var4.spawn(l_param0.getLevel(), l_param1, null, l_param0.getPos().relative(l_var3), MobSpawnType.DISPENSER, l_var3 != Direction.UP, false);
            } catch (Exception l_var5) {
               LOGGER.error("Error while dispensing spawn egg from dispenser at {}", l_param0.getPos(), l_var5);
               return ItemStack.EMPTY;
            }

            l_param1.shrink(1);
            l_param0.getLevel().gameEvent(null, GameEvent.ENTITY_PLACE, l_param0.getPos());
            return l_param1;
         }
      };

      for (SpawnEggItem var2 : SpawnEggItem.eggs()) {
         DispenserBlock.registerBehavior(var2, var0);
      }

      DispenserBlock.registerBehavior(Items.ARMOR_STAND, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Direction l_var3 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            BlockPos l_var4 = l_param0.getPos().relative(l_var3);
            ServerLevel l_var5 = l_param0.getLevel();
            Consumer<ArmorStand> l_var6 = EntityType.appendDefaultStackConfig(l2_param1 -> l2_param1.setYRot(l_var3.toYRot()), l_var5, l_param1, null);
            ArmorStand l_var7 = EntityType.ARMOR_STAND.spawn(l_var5, l_param1.getTag(), l_var6, l_var4, MobSpawnType.DISPENSER, false, false);
            if (l_var7 != null) {
               l_param1.shrink(1);
            }

            return l_param1;
         }
      });
      DispenserBlock.registerBehavior(
         Items.SADDLE,
         new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
               BlockPos l_var3 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
               List<LivingEntity> l_var4 = l_param0.getLevel()
                  .getEntitiesOfClass(
                     LivingEntity.class,
                     new AABB(l_var3),
                     l2_param0 -> !(l2_param0 instanceof Saddleable l2_var1) ? false : !l2_var1.isSaddled() && l2_var1.isSaddleable()
                  );
               if (!l_var4.isEmpty()) {
                  ((Saddleable)l_var4.get(0)).equipSaddle(SoundSource.BLOCKS);
                  l_param1.shrink(1);
                  this.setSuccess(true);
                  return l_param1;
               } else {
                  return super.execute(l_param0, l_param1);
               }
            }
         }
      );
      DefaultDispenseItemBehavior var1 = new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            BlockPos l_var3 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));

            for (AbstractHorse l_var6 : l_param0.getLevel()
               .getEntitiesOfClass(AbstractHorse.class, new AABB(l_var3), l2_param0 -> l2_param0.isAlive() && l2_param0.canWearArmor())) {
               if (l_var6.isArmor(l_param1) && !l_var6.isWearingArmor() && l_var6.isTamed()) {
                  l_var6.getSlot(401).set(l_param1.split(1));
                  this.setSuccess(true);
                  return l_param1;
               }
            }

            return super.execute(l_param0, l_param1);
         }
      };
      DispenserBlock.registerBehavior(Items.LEATHER_HORSE_ARMOR, var1);
      DispenserBlock.registerBehavior(Items.IRON_HORSE_ARMOR, var1);
      DispenserBlock.registerBehavior(Items.GOLDEN_HORSE_ARMOR, var1);
      DispenserBlock.registerBehavior(Items.DIAMOND_HORSE_ARMOR, var1);
      DispenserBlock.registerBehavior(Items.WHITE_CARPET, var1);
      DispenserBlock.registerBehavior(Items.ORANGE_CARPET, var1);
      DispenserBlock.registerBehavior(Items.CYAN_CARPET, var1);
      DispenserBlock.registerBehavior(Items.BLUE_CARPET, var1);
      DispenserBlock.registerBehavior(Items.BROWN_CARPET, var1);
      DispenserBlock.registerBehavior(Items.BLACK_CARPET, var1);
      DispenserBlock.registerBehavior(Items.GRAY_CARPET, var1);
      DispenserBlock.registerBehavior(Items.GREEN_CARPET, var1);
      DispenserBlock.registerBehavior(Items.LIGHT_BLUE_CARPET, var1);
      DispenserBlock.registerBehavior(Items.LIGHT_GRAY_CARPET, var1);
      DispenserBlock.registerBehavior(Items.LIME_CARPET, var1);
      DispenserBlock.registerBehavior(Items.MAGENTA_CARPET, var1);
      DispenserBlock.registerBehavior(Items.PINK_CARPET, var1);
      DispenserBlock.registerBehavior(Items.PURPLE_CARPET, var1);
      DispenserBlock.registerBehavior(Items.RED_CARPET, var1);
      DispenserBlock.registerBehavior(Items.YELLOW_CARPET, var1);
      DispenserBlock.registerBehavior(
         Items.CHEST,
         new OptionalDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
               BlockPos l_var3 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));

               for (AbstractChestedHorse l_var6 : l_param0.getLevel()
                  .getEntitiesOfClass(AbstractChestedHorse.class, new AABB(l_var3), l2_param0 -> l2_param0.isAlive() && !l2_param0.hasChest())) {
                  if (l_var6.isTamed() && l_var6.getSlot(499).set(l_param1)) {
                     l_param1.shrink(1);
                     this.setSuccess(true);
                     return l_param1;
                  }
               }

               return super.execute(l_param0, l_param1);
            }
         }
      );
      DispenserBlock.registerBehavior(Items.FIREWORK_ROCKET, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Direction l_var3 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            FireworkRocketEntity l_var4 = new FireworkRocketEntity(l_param0.getLevel(), l_param1, l_param0.x(), l_param0.y(), l_param0.x(), true);
            DispenseItemBehavior.setEntityPokingOutOfBlock(l_param0, l_var4, l_var3);
            l_var4.shoot(l_var3.getStepX(), l_var3.getStepY(), l_var3.getStepZ(), 0.5F, 1.0F);
            l_param0.getLevel().addFreshEntity(l_var4);
            l_param1.shrink(1);
            return l_param1;
         }

         @Override
         protected void playSound(BlockSource l_param0) {
            l_param0.getLevel().levelEvent(1004, l_param0.getPos(), 0);
         }
      });
      DispenserBlock.registerBehavior(Items.FIRE_CHARGE, new DefaultDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Direction l_var3 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            Position l_var4 = DispenserBlock.getDispensePosition(l_param0);
            double l_var5 = l_var4.x() + l_var3.getStepX() * 0.3F;
            double l_var7 = l_var4.y() + l_var3.getStepY() * 0.3F;
            double l_var9 = l_var4.z() + l_var3.getStepZ() * 0.3F;
            Level l_var11 = l_param0.getLevel();
            RandomSource l_var12 = l_var11.random;
            double l_var13 = l_var12.triangle(l_var3.getStepX(), 0.11485000000000001);
            double l_var15 = l_var12.triangle(l_var3.getStepY(), 0.11485000000000001);
            double l_var17 = l_var12.triangle(l_var3.getStepZ(), 0.11485000000000001);
            SmallFireball l_var19 = new SmallFireball(l_var11, l_var5, l_var7, l_var9, l_var13, l_var15, l_var17);
            l_var11.addFreshEntity(Util.make(l_var19, l2_param1 -> l2_param1.setItem(l_param1)));
            l_param1.shrink(1);
            return l_param1;
         }

         @Override
         protected void playSound(BlockSource l_param0) {
            l_param0.getLevel().levelEvent(1018, l_param0.getPos(), 0);
         }
      });
      DispenserBlock.registerBehavior(Items.OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.OAK));
      DispenserBlock.registerBehavior(Items.SPRUCE_BOAT, new BoatDispenseItemBehavior(Boat.Type.SPRUCE));
      DispenserBlock.registerBehavior(Items.BIRCH_BOAT, new BoatDispenseItemBehavior(Boat.Type.BIRCH));
      DispenserBlock.registerBehavior(Items.JUNGLE_BOAT, new BoatDispenseItemBehavior(Boat.Type.JUNGLE));
      DispenserBlock.registerBehavior(Items.DARK_OAK_BOAT, new BoatDispenseItemBehavior(Boat.Type.DARK_OAK));
      DispenserBlock.registerBehavior(Items.ACACIA_BOAT, new BoatDispenseItemBehavior(Boat.Type.ACACIA));
      DispenserBlock.registerBehavior(Items.CHERRY_BOAT, new BoatDispenseItemBehavior(Boat.Type.CHERRY));
      DispenserBlock.registerBehavior(Items.MANGROVE_BOAT, new BoatDispenseItemBehavior(Boat.Type.MANGROVE));
      DispenserBlock.registerBehavior(Items.BAMBOO_RAFT, new BoatDispenseItemBehavior(Boat.Type.BAMBOO));
      DispenserBlock.registerBehavior(Items.OAK_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.OAK, true));
      DispenserBlock.registerBehavior(Items.SPRUCE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.SPRUCE, true));
      DispenserBlock.registerBehavior(Items.BIRCH_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.BIRCH, true));
      DispenserBlock.registerBehavior(Items.JUNGLE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.JUNGLE, true));
      DispenserBlock.registerBehavior(Items.DARK_OAK_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.DARK_OAK, true));
      DispenserBlock.registerBehavior(Items.ACACIA_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.ACACIA, true));
      DispenserBlock.registerBehavior(Items.CHERRY_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.CHERRY, true));
      DispenserBlock.registerBehavior(Items.MANGROVE_CHEST_BOAT, new BoatDispenseItemBehavior(Boat.Type.MANGROVE, true));
      DispenserBlock.registerBehavior(Items.BAMBOO_CHEST_RAFT, new BoatDispenseItemBehavior(Boat.Type.BAMBOO, true));
      DispenseItemBehavior var2 = new DefaultDispenseItemBehavior() {
         private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            DispensibleContainerItem l_var3 = (DispensibleContainerItem)l_param1.getItem();
            BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            Level l_var5 = l_param0.getLevel();
            if (l_var3.emptyContents(null, l_var5, l_var4, null)) {
               l_var3.checkExtraContent(null, l_var5, l_param1, l_var4);
               return new ItemStack(Items.BUCKET);
            } else {
               return this.defaultDispenseItemBehavior.dispense(l_param0, l_param1);
            }
         }
      };
      DispenserBlock.registerBehavior(Items.LAVA_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.WATER_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.POWDER_SNOW_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.SALMON_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.COD_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.PUFFERFISH_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.TROPICAL_FISH_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.AXOLOTL_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.TADPOLE_BUCKET, var2);
      DispenserBlock.registerBehavior(Items.BUCKET, new DefaultDispenseItemBehavior() {
         private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            LevelAccessor l_var3 = l_param0.getLevel();
            BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            BlockState l_var5 = l_var3.getBlockState(l_var4);
            Block l_var6 = l_var5.getBlock();
            if (l_var6 instanceof BucketPickup) {
               ItemStack l_var8 = ((BucketPickup)l_var6).pickupBlock(l_var3, l_var4, l_var5);
               if (l_var8.isEmpty()) {
                  return super.execute(l_param0, l_param1);
               }

               l_var3.gameEvent(null, GameEvent.FLUID_PICKUP, l_var4);
               Item l_var7 = l_var8.getItem();
               l_param1.shrink(1);
               if (l_param1.isEmpty()) {
                  return new ItemStack(l_var7);
               }

               if (l_param0.<DispenserBlockEntity>getEntity().addItem(new ItemStack(l_var7)) < 0) {
                  this.defaultDispenseItemBehavior.dispense(l_param0, new ItemStack(l_var7));
               }

               return l_param1;
            } else {
               return super.execute(l_param0, l_param1);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Level l_var3 = l_param0.getLevel();
            this.setSuccess(true);
            Direction l_var4 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            BlockPos l_var5 = l_param0.getPos().relative(l_var4);
            BlockState l_var6 = l_var3.getBlockState(l_var5);
            if (BaseFireBlock.canBePlacedAt(l_var3, l_var5, l_var4)) {
               l_var3.setBlockAndUpdate(l_var5, BaseFireBlock.getState(l_var3, l_var5));
               l_var3.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, l_var5);
            } else if (CampfireBlock.canLight(l_var6) || CandleBlock.canLight(l_var6) || CandleCakeBlock.canLight(l_var6)) {
               l_var3.setBlockAndUpdate(l_var5, l_var6.setValue(BlockStateProperties.LIT, true));
               l_var3.gameEvent((Entity)null, GameEvent.BLOCK_CHANGE, l_var5);
            } else if (l_var6.getBlock() instanceof TntBlock) {
               TntBlock.explode(l_var3, l_var5);
               l_var3.removeBlock(l_var5, false);
            } else {
               this.setSuccess(false);
            }

            if (this.isSuccess() && l_param1.hurt(1, l_var3.random, null)) {
               l_param1.setCount(0);
            }

            return l_param1;
         }
      });
      DispenserBlock.registerBehavior(Items.BONE_MEAL, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            this.setSuccess(true);
            Level l_var3 = l_param0.getLevel();
            BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            if (!BoneMealItem.growCrop(l_param1, l_var3, l_var4) && !BoneMealItem.growWaterPlant(l_param1, l_var3, l_var4, null)) {
               this.setSuccess(false);
            } else if (!l_var3.isClientSide) {
               l_var3.levelEvent(1505, l_var4, 0);
            }

            return l_param1;
         }
      });
      DispenserBlock.registerBehavior(Blocks.TNT, new DefaultDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Level l_var3 = l_param0.getLevel();
            BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            PrimedTnt l_var5 = new PrimedTnt(l_var3, l_var4.getX() + 0.5, l_var4.getY(), l_var4.getZ() + 0.5, null);
            l_var3.addFreshEntity(l_var5);
            l_var3.playSound(null, l_var5.getX(), l_var5.getY(), l_var5.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            l_var3.gameEvent((Entity)null, GameEvent.ENTITY_PLACE, l_var4);
            l_param1.shrink(1);
            return l_param1;
         }
      });
      DispenseItemBehavior var3 = new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            this.setSuccess(ArmorItem.dispenseArmor(l_param0, l_param1));
            return l_param1;
         }
      };
      DispenserBlock.registerBehavior(Items.CREEPER_HEAD, var3);
      DispenserBlock.registerBehavior(Items.ZOMBIE_HEAD, var3);
      DispenserBlock.registerBehavior(Items.DRAGON_HEAD, var3);
      DispenserBlock.registerBehavior(Items.SKELETON_SKULL, var3);
      DispenserBlock.registerBehavior(Items.PIGLIN_HEAD, var3);
      DispenserBlock.registerBehavior(Items.PLAYER_HEAD, var3);
      DispenserBlock.registerBehavior(
         Items.WITHER_SKELETON_SKULL,
         new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
               Level l_var3 = l_param0.getLevel();
               Direction l_var4 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
               BlockPos l_var5 = l_param0.getPos().relative(l_var4);
               if (l_var3.isEmptyBlock(l_var5) && WitherSkullBlock.canSpawnMob(l_var3, l_var5, l_param1)) {
                  l_var3.setBlock(
                     l_var5, Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, RotationSegment.convertToSegment(l_var4)), 3
                  );
                  l_var3.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, l_var5);
                  BlockEntity l_var6 = l_var3.getBlockEntity(l_var5);
                  if (l_var6 instanceof SkullBlockEntity) {
                     WitherSkullBlock.checkSpawn(l_var3, l_var5, (SkullBlockEntity)l_var6);
                  }

                  l_param1.shrink(1);
                  this.setSuccess(true);
               } else {
                  this.setSuccess(ArmorItem.dispenseArmor(l_param0, l_param1));
               }

               return l_param1;
            }
         }
      );
      DispenserBlock.registerBehavior(Blocks.CARVED_PUMPKIN, new OptionalDispenseItemBehavior() {
         @Override
         protected ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Level l_var3 = l_param0.getLevel();
            BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            CarvedPumpkinBlock l_var5 = (CarvedPumpkinBlock)Blocks.CARVED_PUMPKIN;
            if (l_var3.isEmptyBlock(l_var4) && l_var5.canSpawnGolem(l_var3, l_var4)) {
               if (!l_var3.isClientSide) {
                  l_var3.setBlock(l_var4, l_var5.defaultBlockState(), 3);
                  l_var3.gameEvent((Entity)null, GameEvent.BLOCK_PLACE, l_var4);
               }

               l_param1.shrink(1);
               this.setSuccess(true);
            } else {
               this.setSuccess(ArmorItem.dispenseArmor(l_param0, l_param1));
            }

            return l_param1;
         }
      });
      DispenserBlock.registerBehavior(Blocks.SHULKER_BOX.asItem(), new ShulkerBoxDispenseBehavior());

      for (DyeColor var7 : DyeColor.values()) {
         DispenserBlock.registerBehavior(ShulkerBoxBlock.getBlockByColor(var7).asItem(), new ShulkerBoxDispenseBehavior());
      }

      DispenserBlock.registerBehavior(
         Items.GLASS_BOTTLE.asItem(),
         new OptionalDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            private ItemStack takeLiquid(BlockSource l_param0, ItemStack l_param1, ItemStack l_param2) {
               l_param1.shrink(1);
               if (l_param1.isEmpty()) {
                  l_param0.getLevel().gameEvent(null, GameEvent.FLUID_PICKUP, l_param0.getPos());
                  return l_param2.copy();
               }

               if (l_param0.<DispenserBlockEntity>getEntity().addItem(l_param2.copy()) < 0) {
                  this.defaultDispenseItemBehavior.dispense(l_param0, l_param2.copy());
               }

               return l_param1;
            }

            @Override
            public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
               this.setSuccess(false);
               ServerLevel l_var3 = l_param0.getLevel();
               BlockPos l_var4 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
               BlockState l_var5 = l_var3.getBlockState(l_var4);
               if (l_var5.is(BlockTags.BEEHIVES, l2_param0 -> l2_param0.hasProperty(BeehiveBlock.HONEY_LEVEL) && l2_param0.getBlock() instanceof BeehiveBlock)
                  && l_var5.getValue(BeehiveBlock.HONEY_LEVEL) >= 5) {
                  ((BeehiveBlock)l_var5.getBlock())
                     .releaseBeesAndResetHoneyLevel(l_var3, l_var5, l_var4, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                  this.setSuccess(true);
                  return this.takeLiquid(l_param0, l_param1, new ItemStack(Items.HONEY_BOTTLE));
               } else if (l_var3.getFluidState(l_var4).is(FluidTags.WATER)) {
                  this.setSuccess(true);
                  return this.takeLiquid(l_param0, l_param1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
               } else {
                  return super.execute(l_param0, l_param1);
               }
            }
         }
      );
      DispenserBlock.registerBehavior(Items.GLOWSTONE, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            Direction l_var3 = l_param0.getBlockState().getValue(DispenserBlock.FACING);
            BlockPos l_var4 = l_param0.getPos().relative(l_var3);
            Level l_var5 = l_param0.getLevel();
            BlockState l_var6 = l_var5.getBlockState(l_var4);
            this.setSuccess(true);
            if (l_var6.is(Blocks.RESPAWN_ANCHOR)) {
               if (l_var6.getValue(RespawnAnchorBlock.CHARGE) != 4) {
                  RespawnAnchorBlock.charge(null, l_var5, l_var4, l_var6);
                  l_param1.shrink(1);
               } else {
                  this.setSuccess(false);
               }

               return l_param1;
            } else {
               return super.execute(l_param0, l_param1);
            }
         }
      });
      DispenserBlock.registerBehavior(Items.SHEARS.asItem(), new ShearsDispenseItemBehavior());
      DispenserBlock.registerBehavior(Items.HONEYCOMB, new OptionalDispenseItemBehavior() {
         @Override
         public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
            BlockPos l_var3 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
            Level l_var4 = l_param0.getLevel();
            BlockState l_var5 = l_var4.getBlockState(l_var3);
            Optional<BlockState> l_var6 = HoneycombItem.getWaxed(l_var5);
            if (l_var6.isPresent()) {
               l_var4.setBlockAndUpdate(l_var3, l_var6.get());
               l_var4.levelEvent(3003, l_var3, 0);
               l_param1.shrink(1);
               this.setSuccess(true);
               return l_param1;
            } else {
               return super.execute(l_param0, l_param1);
            }
         }
      });
      DispenserBlock.registerBehavior(
         Items.POTION,
         new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            @Override
            public ItemStack execute(BlockSource l_param0, ItemStack l_param1) {
               if (PotionUtils.getPotion(l_param1) != Potions.WATER) {
                  return this.defaultDispenseItemBehavior.dispense(l_param0, l_param1);
               }

               ServerLevel l_var3 = l_param0.getLevel();
               BlockPos l_var4 = l_param0.getPos();
               BlockPos l_var5 = l_param0.getPos().relative(l_param0.getBlockState().getValue(DispenserBlock.FACING));
               if (!l_var3.getBlockState(l_var5).is(BlockTags.CONVERTABLE_TO_MUD)) {
                  return this.defaultDispenseItemBehavior.dispense(l_param0, l_param1);
               }

               if (!l_var3.isClientSide) {
                  for (int l_var6 = 0; l_var6 < 5; l_var6++) {
                     l_var3.sendParticles(
                        ParticleTypes.SPLASH,
                        l_var4.getX() + l_var3.random.nextDouble(),
                        l_var4.getY() + 1,
                        l_var4.getZ() + l_var3.random.nextDouble(),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        1.0
                     );
                  }
               }

               l_var3.playSound((Player)null, l_var4, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               l_var3.gameEvent(null, GameEvent.FLUID_PLACE, l_var4);
               l_var3.setBlockAndUpdate(l_var5, Blocks.MUD.defaultBlockState());
               return new ItemStack(Items.GLASS_BOTTLE);
            }
         }
      );
   }

   static void setEntityPokingOutOfBlock(BlockSource param0, Entity param1, Direction param2) {
      param1.setPos(
         param0.x() + param2.getStepX() * (0.5000099999997474 - param1.getBbWidth() / 2.0),
         param0.y() + param2.getStepY() * (0.5000099999997474 - param1.getBbHeight() / 2.0) - param1.getBbHeight() / 2.0,
         param0.z() + param2.getStepZ() * (0.5000099999997474 - param1.getBbWidth() / 2.0)
      );
   }
}
