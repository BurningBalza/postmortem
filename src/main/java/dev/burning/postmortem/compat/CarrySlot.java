package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A carried slot owned by another mod, remembered so a statue can hand the item back to it. */
public enum CarrySlot implements StringRepresentable {
   BACKSLOT("backslot"),
   AMENITIES_BACK("amenities_back"),
   AMENITIES_BELT("amenities_belt");

   public static final Codec<CarrySlot> CODEC = StringRepresentable.fromEnum(CarrySlot::values);

   private final String id;

   CarrySlot(String id) {
      this.id = id;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }

   /** Puts the stack back where it was worn, or returns false to let the caller drop it in the inventory. */
   public boolean restore(Player player, ItemStack stack) {
      return switch (this) {
         case BACKSLOT -> BackslotCompat.restore(player, stack);
         case AMENITIES_BACK -> CombatAmenitiesCompat.restoreBack(player, stack);
         case AMENITIES_BELT -> CombatAmenitiesCompat.restoreBelt(player, stack);
      };
   }
}
