package net.minecraft.client.renderer.item;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightBlock;

public class ItemProperties {
   private static final Map<ResourceLocation, ItemPropertyFunction> GENERIC_PROPERTIES = Maps.newHashMap();
   private static final String TAG_CUSTOM_MODEL_DATA = "CustomModelData";
   private static final ResourceLocation DAMAGED = new ResourceLocation("damaged");
   private static final ResourceLocation DAMAGE = new ResourceLocation("damage");
   private static final ClampedItemPropertyFunction PROPERTY_DAMAGED = (param0, param1, param2, param3) -> param0.isDamaged() ? 1.0F : 0.0F;
   private static final ClampedItemPropertyFunction PROPERTY_DAMAGE = (param0, param1, param2, param3) -> Mth.clamp(
      (float)param0.getDamageValue() / param0.getMaxDamage(), 0.0F, 1.0F
   );
   private static final Map<Item, Map<ResourceLocation, ItemPropertyFunction>> PROPERTIES = Maps.newHashMap();

   private static ClampedItemPropertyFunction registerGeneric(ResourceLocation param0, ClampedItemPropertyFunction param1) {
      GENERIC_PROPERTIES.put(param0, param1);
      return param1;
   }

   private static void registerCustomModelData(ItemPropertyFunction param0) {
      GENERIC_PROPERTIES.put(new ResourceLocation("custom_model_data"), param0);
   }

   private static void register(Item param0, ResourceLocation param1, ClampedItemPropertyFunction param2) {
      PROPERTIES.computeIfAbsent(param0, l_param0 -> Maps.newHashMap()).put(param1, param2);
   }

   @Nullable
   public static ItemPropertyFunction getProperty(Item param0, ResourceLocation param1) {
      if (param0.getMaxDamage() > 0) {
         if (DAMAGE.equals(param1)) {
            return PROPERTY_DAMAGE;
         }

         if (DAMAGED.equals(param1)) {
            return PROPERTY_DAMAGED;
         }
      }

      ItemPropertyFunction var2 = GENERIC_PROPERTIES.get(param1);
      if (var2 != null) {
         return var2;
      }

      Map<ResourceLocation, ItemPropertyFunction> var3 = PROPERTIES.get(param0);
      return var3 == null ? null : var3.get(param1);
   }

   static {
      registerGeneric(
         new ResourceLocation("lefthanded"), (param0, param1, param2, param3) -> param2 != null && param2.getMainArm() != HumanoidArm.RIGHT ? 1.0F : 0.0F
      );
      registerGeneric(
         new ResourceLocation("cooldown"),
         (param0, param1, param2, param3) -> param2 instanceof Player ? ((Player)param2).getCooldowns().getCooldownPercent(param0.getItem(), 0.0F) : 0.0F
      );
      ClampedItemPropertyFunction var0 = (param0, param1, param2, param3) -> {
         if (!param0.is(ItemTags.TRIMMABLE_ARMOR)) {
            return Float.NEGATIVE_INFINITY;
         } else if (param1 == null) {
            return 0.0F;
         } else {
            return !param1.enabledFeatures().contains(FeatureFlags.UPDATE_1_20)
               ? Float.NEGATIVE_INFINITY
               : ArmorTrim.getTrim(param1.registryAccess(), param0).map(ArmorTrim::material).map(Holder::value).map(TrimMaterial::itemModelIndex).orElse(0.0F);
         }
      };
      registerGeneric(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, var0);
      registerCustomModelData((param0, param1, param2, param3) -> param0.hasTag() ? param0.getTag().getInt("CustomModelData") : 0.0F);
      register(Items.BOW, new ResourceLocation("pull"), (param0, param1, param2, param3) -> {
         if (param2 == null) {
            return 0.0F;
         } else {
            return param2.getUseItem() != param0 ? 0.0F : (param0.getUseDuration() - param2.getUseItemRemainingTicks()) / 20.0F;
         }
      });
      register(
         Items.BRUSH,
         new ResourceLocation("brushing"),
         (param0, param1, param2, param3) -> param2 != null && param2.getUseItem() == param0 ? param2.getUseItemRemainingTicks() % 10 / 10.0F : 0.0F
      );
      register(
         Items.BOW,
         new ResourceLocation("pulling"),
         (param0, param1, param2, param3) -> param2 != null && param2.isUsingItem() && param2.getUseItem() == param0 ? 1.0F : 0.0F
      );
      register(Items.BUNDLE, new ResourceLocation("filled"), (param0, param1, param2, param3) -> BundleItem.getFullnessDisplay(param0));
      register(Items.CLOCK, new ResourceLocation("time"), new ClampedItemPropertyFunction() {
         private double rotation;
         private double rota;
         private long lastUpdateTick;

         @Override
         public float unclampedCall(ItemStack param0, @Nullable ClientLevel param1x, @Nullable LivingEntity param2, int param3) {
            Entity var5 = param2 != null ? param2 : param0.getEntityRepresentation();
            if (var5 == null) {
               return 0.0F;
            }

            // Vineflower emitted a bogus pattern variable here: the original bytecode casts
            // var5.level to ClientLevel and stores the result back into the level parameter.
            if (var5.level instanceof ClientLevel clientLevel) {
               param1x = clientLevel;
            }

            if (param1x == null) {
               return 0.0F;
            }

            double var6;
            if (param1x.dimensionType().natural()) {
               var6 = param1x.getTimeOfDay(1.0F);
            } else {
               var6 = Math.random();
            }

            var6 = this.wobble(param1x, var6);
            return (float)var6;
         }

         private double wobble(Level param0, double param1) {
            if (param0.getGameTime() != this.lastUpdateTick) {
               this.lastUpdateTick = param0.getGameTime();
               double var4 = param1 - this.rotation;
               var4 = Mth.positiveModulo(var4 + 0.5, 1.0) - 0.5;
               this.rota += var4 * 0.1;
               this.rota *= 0.9;
               this.rotation = Mth.positiveModulo(this.rotation + this.rota, 1.0);
            }

            return this.rotation;
         }
      });
      register(
         Items.COMPASS,
         new ResourceLocation("angle"),
         new CompassItemPropertyFunction(
            (param0, param1, param2) -> CompassItem.isLodestoneCompass(param1)
               ? CompassItem.getLodestonePosition(param1.getOrCreateTag())
               : CompassItem.getSpawnPosition(param0)
         )
      );
      register(
         Items.RECOVERY_COMPASS,
         new ResourceLocation("angle"),
         new CompassItemPropertyFunction((param0, param1, param2) -> param2 instanceof Player var3 ? var3.getLastDeathLocation().orElse(null) : null)
      );
      register(
         Items.CROSSBOW,
         new ResourceLocation("pull"),
         (param0, param1, param2, param3) -> {
            if (param2 == null) {
               return 0.0F;
            } else {
               return CrossbowItem.isCharged(param0)
                  ? 0.0F
                  : (float)(param0.getUseDuration() - param2.getUseItemRemainingTicks()) / CrossbowItem.getChargeDuration(param0);
            }
         }
      );
      register(
         Items.CROSSBOW,
         new ResourceLocation("pulling"),
         (param0, param1, param2, param3) -> param2 != null && param2.isUsingItem() && param2.getUseItem() == param0 && !CrossbowItem.isCharged(param0)
            ? 1.0F
            : 0.0F
      );
      register(Items.CROSSBOW, new ResourceLocation("charged"), (param0, param1, param2, param3) -> CrossbowItem.isCharged(param0) ? 1.0F : 0.0F);
      register(
         Items.CROSSBOW,
         new ResourceLocation("firework"),
         (param0, param1, param2, param3) -> CrossbowItem.isCharged(param0) && CrossbowItem.containsChargedProjectile(param0, Items.FIREWORK_ROCKET)
            ? 1.0F
            : 0.0F
      );
      register(Items.ELYTRA, new ResourceLocation("broken"), (param0, param1, param2, param3) -> ElytraItem.isFlyEnabled(param0) ? 0.0F : 1.0F);
      register(Items.FISHING_ROD, new ResourceLocation("cast"), (param0, param1, param2, param3) -> {
         if (param2 == null) {
            return 0.0F;
         }

         boolean var4 = param2.getMainHandItem() == param0;
         boolean var5 = param2.getOffhandItem() == param0;
         if (param2.getMainHandItem().getItem() instanceof FishingRodItem) {
            var5 = false;
         }

         return (var4 || var5) && param2 instanceof Player && ((Player)param2).fishing != null ? 1.0F : 0.0F;
      });
      register(
         Items.SHIELD,
         new ResourceLocation("blocking"),
         (param0, param1, param2, param3) -> param2 != null && param2.isUsingItem() && param2.getUseItem() == param0 ? 1.0F : 0.0F
      );
      register(
         Items.TRIDENT,
         new ResourceLocation("throwing"),
         (param0, param1, param2, param3) -> param2 != null && param2.isUsingItem() && param2.getUseItem() == param0 ? 1.0F : 0.0F
      );
      register(Items.LIGHT, new ResourceLocation("level"), (param0, param1, param2, param3) -> {
         CompoundTag var4 = param0.getTagElement("BlockStateTag");

         try {
            if (var4 != null) {
               Tag var5 = var4.get(LightBlock.LEVEL.getName());
               if (var5 != null) {
                  return Integer.parseInt(var5.getAsString()) / 16.0F;
               }
            }
         } catch (NumberFormatException var6) {
         }

         return 1.0F;
      });
      register(
         Items.GOAT_HORN,
         new ResourceLocation("tooting"),
         (param0, param1, param2, param3) -> param2 != null && param2.isUsingItem() && param2.getUseItem() == param0 ? 1.0F : 0.0F
      );
   }
}
