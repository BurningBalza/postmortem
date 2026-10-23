package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum CarrySlot implements StringRepresentable {
   BACKSLOT("backslot"),
   AMENITIES_BACK("amenities_back"),
   AMENITIES_BELT("amenities_belt"),
   COSMETIC_FEET("cosmetic_feet"),
   COSMETIC_LEGS("cosmetic_legs"),
   COSMETIC_CHEST("cosmetic_chest"),
   COSMETIC_HEAD("cosmetic_head");

   private static final CarrySlot[] COSMETIC = {COSMETIC_FEET, COSMETIC_LEGS, COSMETIC_CHEST, COSMETIC_HEAD};

   public static final Codec<CarrySlot> CODEC = StringRepresentable.fromEnum(CarrySlot::values);

   private final String id;

   CarrySlot(String id) {
      this.id = id;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   public static CarrySlot cosmetic(int index) {
      return COSMETIC[index];
   }

   public boolean restore(Player player, ItemStack stack) {
      return switch (this) {
         case BACKSLOT -> BackslotCompat.restore(player, stack);
         case AMENITIES_BACK -> CombatAmenitiesCompat.restoreBack(player, stack);
         case AMENITIES_BELT -> CombatAmenitiesCompat.restoreBelt(player, stack);
         case COSMETIC_FEET, COSMETIC_LEGS, COSMETIC_CHEST, COSMETIC_HEAD ->
            CosmeticArmorCompat.restore(player, this.ordinal() - COSMETIC_FEET.ordinal(), stack);
      };
   }
}
