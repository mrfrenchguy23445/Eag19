package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public MerchantOffers() {
   }

   private MerchantOffers(int param0) {
      super(param0);
   }

   public MerchantOffers(CompoundTag param0) {
      ListTag var2 = param0.getList("Recipes", 10);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         this.add(new MerchantOffer(var2.getCompound(var3)));
      }
   }

   @Nullable
   public MerchantOffer getRecipeFor(ItemStack param0, ItemStack param1, int param2) {
      if (param2 > 0 && param2 < this.size()) {
         MerchantOffer var4 = this.get(param2);
         return var4.satisfiedBy(param0, param1) ? var4 : null;
      }

      for (int var4 = 0; var4 < this.size(); var4++) {
         MerchantOffer var5 = this.get(var4);
         if (var5.satisfiedBy(param0, param1)) {
            return var5;
         }
      }

      return null;
   }

   public void writeToStream(FriendlyByteBuf param0) {
      param0.writeCollection(this, (l_param0, l_param1) -> {
         l_param0.writeItem(l_param1.getBaseCostA());
         l_param0.writeItem(l_param1.getResult());
         l_param0.writeItem(l_param1.getCostB());
         l_param0.writeBoolean(l_param1.isOutOfStock());
         l_param0.writeInt(l_param1.getUses());
         l_param0.writeInt(l_param1.getMaxUses());
         l_param0.writeInt(l_param1.getXp());
         l_param0.writeInt(l_param1.getSpecialPriceDiff());
         l_param0.writeFloat(l_param1.getPriceMultiplier());
         l_param0.writeInt(l_param1.getDemand());
      });
   }

   public static MerchantOffers createFromStream(FriendlyByteBuf param0) {
      return param0.readCollection(MerchantOffers::new, l_param0 -> {
         ItemStack l_var1 = l_param0.readItem();
         ItemStack l_var2 = l_param0.readItem();
         ItemStack l_var3 = l_param0.readItem();
         boolean l_var4 = l_param0.readBoolean();
         int l_var5 = l_param0.readInt();
         int l_var6 = l_param0.readInt();
         int l_var7 = l_param0.readInt();
         int l_var8 = l_param0.readInt();
         float l_var9 = l_param0.readFloat();
         int l_var10 = l_param0.readInt();
         MerchantOffer l_var11 = new MerchantOffer(l_var1, l_var3, l_var2, l_var5, l_var6, l_var7, l_var9, l_var10);
         if (l_var4) {
            l_var11.setToOutOfStock();
         }

         l_var11.setSpecialPriceDiff(l_var8);
         return l_var11;
      });
   }

   public CompoundTag createTag() {
      CompoundTag var1 = new CompoundTag();
      ListTag var2 = new ListTag();

      for (int var3 = 0; var3 < this.size(); var3++) {
         MerchantOffer var4 = this.get(var3);
         var2.add(var4.createTag());
      }

      var1.put("Recipes", var2);
      return var1;
   }
}
