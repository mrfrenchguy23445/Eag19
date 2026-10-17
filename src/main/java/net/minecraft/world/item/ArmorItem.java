package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class ArmorItem extends Item implements Equipable {
   private static final EnumMap<ArmorItem.Type, UUID> ARMOR_MODIFIER_UUID_PER_TYPE = Util.make(new EnumMap<>(ArmorItem.Type.class), param0 -> {
      param0.put(ArmorItem.Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"));
      param0.put(ArmorItem.Type.LEGGINGS, UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
      param0.put(ArmorItem.Type.CHESTPLATE, UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"));
      param0.put(ArmorItem.Type.HELMET, UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
   });
   public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
      @Override
      protected ItemStack execute(BlockSource param0, ItemStack param1) {
         return ArmorItem.dispenseArmor(param0, param1) ? param1 : super.execute(param0, param1);
      }
   };
   protected final ArmorItem.Type type;
   private final int defense;
   private final float toughness;
   protected final float knockbackResistance;
   protected final ArmorMaterial material;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public static boolean dispenseArmor(BlockSource param0, ItemStack param1) {
      BlockPos var2 = param0.getPos().relative(param0.getBlockState().getValue(DispenserBlock.FACING));
      List<LivingEntity> var3 = param0.getLevel()
         .getEntitiesOfClass(LivingEntity.class, new AABB(var2), EntitySelector.NO_SPECTATORS.and(new EntitySelector.MobCanWearArmorEntitySelector(param1)));
      if (var3.isEmpty()) {
         return false;
      }

      LivingEntity var4 = var3.get(0);
      EquipmentSlot var5 = Mob.getEquipmentSlotForItem(param1);
      ItemStack var6 = param1.split(1);
      var4.setItemSlot(var5, var6);
      if (var4 instanceof Mob) {
         ((Mob)var4).setDropChance(var5, 2.0F);
         ((Mob)var4).setPersistenceRequired();
      }

      return true;
   }

   public ArmorItem(ArmorMaterial param0, ArmorItem.Type param1, Item.Properties param2) {
      super(param2.defaultDurability(param0.getDurabilityForType(param1)));
      this.material = param0;
      this.type = param1;
      this.defense = param0.getDefenseForType(param1);
      this.toughness = param0.getToughness();
      this.knockbackResistance = param0.getKnockbackResistance();
      DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
      Builder<Attribute, AttributeModifier> var4 = ImmutableMultimap.builder();
      UUID var5 = ARMOR_MODIFIER_UUID_PER_TYPE.get(param1);
      var4.put(Attributes.ARMOR, new AttributeModifier(var5, "Armor modifier", this.defense, AttributeModifier.Operation.ADDITION));
      var4.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(var5, "Armor toughness", this.toughness, AttributeModifier.Operation.ADDITION));
      if (param0 == ArmorMaterials.NETHERITE) {
         var4.put(
            Attributes.KNOCKBACK_RESISTANCE,
            new AttributeModifier(var5, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION)
         );
      }

      this.defaultModifiers = var4.build();
   }

   public ArmorItem.Type getType() {
      return this.type;
   }

   @Override
   public int getEnchantmentValue() {
      return this.material.getEnchantmentValue();
   }

   public ArmorMaterial getMaterial() {
      return this.material;
   }

   @Override
   public boolean isValidRepairItem(ItemStack param0, ItemStack param1) {
      return this.material.getRepairIngredient().test(param1) || super.isValidRepairItem(param0, param1);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level param0, Player param1, InteractionHand param2) {
      return this.swapWithEquipmentSlot(this, param0, param1, param2);
   }

   @Override
   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot param0) {
      return param0 == this.type.getSlot() ? this.defaultModifiers : super.getDefaultAttributeModifiers(param0);
   }

   public int getDefense() {
      return this.defense;
   }

   public float getToughness() {
      return this.toughness;
   }

   @Override
   public EquipmentSlot getEquipmentSlot() {
      return this.type.getSlot();
   }

   @Override
   public SoundEvent getEquipSound() {
      return this.getMaterial().getEquipSound();
   }

   public enum Type {
      HELMET(EquipmentSlot.HEAD, "helmet"),
      CHESTPLATE(EquipmentSlot.CHEST, "chestplate"),
      LEGGINGS(EquipmentSlot.LEGS, "leggings"),
      BOOTS(EquipmentSlot.FEET, "boots");

      private final EquipmentSlot slot;
      private final String name;

      Type(EquipmentSlot param2, String param3) {
         this.slot = param2;
         this.name = param3;
      }

      public EquipmentSlot getSlot() {
         return this.slot;
      }

      public String getName() {
         return this.name;
      }
   }
}
