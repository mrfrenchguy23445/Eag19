package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantmentTableBlock;

public class EnchantmentMenu extends AbstractContainerMenu {
   private final Container enchantSlots = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         super.setChanged();
         EnchantmentMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;
   private final RandomSource random = RandomSource.create();
   private final DataSlot enchantmentSeed = DataSlot.standalone();
   public final int[] costs = new int[3];
   public final int[] enchantClue = new int[]{-1, -1, -1};
   public final int[] levelClue = new int[]{-1, -1, -1};

   public EnchantmentMenu(int param0, Inventory param1) {
      this(param0, param1, ContainerLevelAccess.NULL);
   }

   public EnchantmentMenu(int param0, Inventory param1, ContainerLevelAccess param2) {
      super(MenuType.ENCHANTMENT, param0);
      this.access = param2;
      this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return true;
         }

         @Override
         public int getMaxStackSize() {
            return 1;
         }
      });
      this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
         @Override
         public boolean mayPlace(ItemStack l_param0) {
            return l_param0.is(Items.LAPIS_LAZULI);
         }
      });

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var5 = 0; var5 < 9; var5++) {
            this.addSlot(new Slot(param1, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
         }
      }

      for (int var4 = 0; var4 < 9; var4++) {
         this.addSlot(new Slot(param1, var4, 8 + var4 * 18, 142));
      }

      this.addDataSlot(DataSlot.shared(this.costs, 0));
      this.addDataSlot(DataSlot.shared(this.costs, 1));
      this.addDataSlot(DataSlot.shared(this.costs, 2));
      this.addDataSlot(this.enchantmentSeed).set(param1.player.getEnchantmentSeed());
      this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
      this.addDataSlot(DataSlot.shared(this.levelClue, 0));
      this.addDataSlot(DataSlot.shared(this.levelClue, 1));
      this.addDataSlot(DataSlot.shared(this.levelClue, 2));
   }

   @Override
   public void slotsChanged(Container param0) {
      if (param0 == this.enchantSlots) {
         ItemStack var2 = param0.getItem(0);
         if (!var2.isEmpty() && var2.isEnchantable()) {
            this.access.execute((l_param1, l_param2) -> {
               int l_var4 = 0;

               for (BlockPos l_var6 : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
                  if (EnchantmentTableBlock.isValidBookShelf(l_param1, l_param2, l_var6)) {
                     l_var4++;
                  }
               }

               this.random.setSeed(this.enchantmentSeed.get());

               for (int l_var5 = 0; l_var5 < 3; l_var5++) {
                  this.costs[l_var5] = EnchantmentHelper.getEnchantmentCost(this.random, l_var5, l_var4, var2);
                  this.enchantClue[l_var5] = -1;
                  this.levelClue[l_var5] = -1;
                  if (this.costs[l_var5] < l_var5 + 1) {
                     this.costs[l_var5] = 0;
                  }
               }

               for (int l_var5 = 0; l_var5 < 3; l_var5++) {
                  if (this.costs[l_var5] > 0) {
                     List<EnchantmentInstance> l_var6 = this.getEnchantmentList(var2, l_var5, this.costs[l_var5]);
                     if (l_var6 != null && !l_var6.isEmpty()) {
                        EnchantmentInstance l_var7 = l_var6.get(this.random.nextInt(l_var6.size()));
                        this.enchantClue[l_var5] = BuiltInRegistries.ENCHANTMENT.getId(l_var7.enchantment);
                        this.levelClue[l_var5] = l_var7.level;
                     }
                  }
               }

               this.broadcastChanges();
            });
         } else {
            for (int var3 = 0; var3 < 3; var3++) {
               this.costs[var3] = 0;
               this.enchantClue[var3] = -1;
               this.levelClue[var3] = -1;
            }
         }
      }
   }

   @Override
   public boolean clickMenuButton(Player param0, int param1) {
      if (param1 >= 0 && param1 < this.costs.length) {
         ItemStack var3 = this.enchantSlots.getItem(0);
         ItemStack var4 = this.enchantSlots.getItem(1);
         int var5 = param1 + 1;
         if ((var4.isEmpty() || var4.getCount() < var5) && !param0.getAbilities().instabuild) {
            return false;
         }

         if (this.costs[param1] <= 0
            || var3.isEmpty()
            || (param0.experienceLevel < var5 || param0.experienceLevel < this.costs[param1]) && !param0.getAbilities().instabuild) {
            return false;
         }

         this.access
            .execute(
               (l_param5, l_param6) -> {
                  ItemStack l_var8 = var3;
                  List<EnchantmentInstance> l_var9 = this.getEnchantmentList(l_var8, param1, this.costs[param1]);
                  if (!l_var9.isEmpty()) {
                     param0.onEnchantmentPerformed(l_var8, var5);
                     boolean l_var10 = l_var8.is(Items.BOOK);
                     if (l_var10) {
                        l_var8 = new ItemStack(Items.ENCHANTED_BOOK);
                        CompoundTag l_var11 = var3.getTag();
                        if (l_var11 != null) {
                           l_var8.setTag(l_var11.copy());
                        }

                        this.enchantSlots.setItem(0, l_var8);
                     }

                     for (int l_var11 = 0; l_var11 < l_var9.size(); l_var11++) {
                        EnchantmentInstance l_var12 = l_var9.get(l_var11);
                        if (l_var10) {
                           EnchantedBookItem.addEnchantment(l_var8, l_var12);
                        } else {
                           l_var8.enchant(l_var12.enchantment, l_var12.level);
                        }
                     }

                     if (!param0.getAbilities().instabuild) {
                        var4.shrink(var5);
                        if (var4.isEmpty()) {
                           this.enchantSlots.setItem(1, ItemStack.EMPTY);
                        }
                     }

                     param0.awardStat(Stats.ENCHANT_ITEM);
                     if (param0 instanceof ServerPlayer) {
                        CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer)param0, l_var8, var5);
                     }

                     this.enchantSlots.setChanged();
                     this.enchantmentSeed.set(param0.getEnchantmentSeed());
                     this.slotsChanged(this.enchantSlots);
                     l_param5.playSound(
                        (Player)null, l_param6, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, l_param5.random.nextFloat() * 0.1F + 0.9F
                     );
                  }
               }
            );
         return true;
      } else {
         Util.logAndPauseIfInIde(param0.getName() + " pressed invalid button id: " + param1);
         return false;
      }
   }

   private List<EnchantmentInstance> getEnchantmentList(ItemStack param0, int param1, int param2) {
      this.random.setSeed(this.enchantmentSeed.get() + param1);
      List<EnchantmentInstance> var4 = EnchantmentHelper.selectEnchantment(this.random, param0, param2, false);
      if (param0.is(Items.BOOK) && var4.size() > 1) {
         var4.remove(this.random.nextInt(var4.size()));
      }

      return var4;
   }

   public int getGoldCount() {
      ItemStack var1 = this.enchantSlots.getItem(1);
      return var1.isEmpty() ? 0 : var1.getCount();
   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed.get();
   }

   @Override
   public void removed(Player param0) {
      super.removed(param0);
      this.access.execute((l_param1, l_param2) -> this.clearContainer(param0, this.enchantSlots));
   }

   @Override
   public boolean stillValid(Player param0) {
      return stillValid(this.access, param0, Blocks.ENCHANTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player param0, int param1) {
      ItemStack var3 = ItemStack.EMPTY;
      Slot var4 = this.slots.get(param1);
      if (var4 != null && var4.hasItem()) {
         ItemStack var5 = var4.getItem();
         var3 = var5.copy();
         if (param1 == 0) {
            if (!this.moveItemStackTo(var5, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (param1 == 1) {
            if (!this.moveItemStackTo(var5, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (var5.is(Items.LAPIS_LAZULI)) {
            if (!this.moveItemStackTo(var5, 1, 2, true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(var5)) {
               return ItemStack.EMPTY;
            }

            ItemStack var6 = var5.copy();
            var6.setCount(1);
            var5.shrink(1);
            this.slots.get(0).setByPlayer(var6);
         }

         if (var5.isEmpty()) {
            var4.setByPlayer(ItemStack.EMPTY);
         } else {
            var4.setChanged();
         }

         if (var5.getCount() == var3.getCount()) {
            return ItemStack.EMPTY;
         }

         var4.onTake(param0, var5);
      }

      return var3;
   }
}
