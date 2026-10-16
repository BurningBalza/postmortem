package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CombatAmenitiesCompat {
   private static final String MOD_ID = "combatamenities";

   private static final String BACK_SLOT = "COMBATAMENITIES_BACKSLOT";
   private static final String BELT_SLOT = "COMBATAMENITIES_BELTSLOT";

   private static boolean resolved;
   private static boolean present;
   private static EquipmentSlot back;
   private static EquipmentSlot belt;

   private CombatAmenitiesCompat() {
   }

   public static boolean isPresent() {
      return resolve();
   }

   public static EquipmentSlot backSlot() {
      return resolve() ? back : null;
   }

   public static EquipmentSlot beltSlot() {
      return resolve() ? belt : null;
   }

   public static ItemStack getBack(Player player) {
      return read(player, backSlot());
   }

   public static ItemStack getBelt(Player player) {
      return read(player, beltSlot());
   }

   public static void clear(Player player) {
      write(player, backSlot(), ItemStack.EMPTY);
      write(player, beltSlot(), ItemStack.EMPTY);
   }

   public static boolean restoreBack(Player player, ItemStack stack) {
      return restore(player, backSlot(), stack);
   }

   public static boolean restoreBelt(Player player, ItemStack stack) {
      return restore(player, beltSlot(), stack);
   }

   private static boolean restore(Player player, EquipmentSlot slot, ItemStack stack) {
      if (slot == null || !player.getItemBySlot(slot).isEmpty()) {
         return false;
      }

      player.setItemSlot(slot, stack);
      return true;
   }

   private static ItemStack read(Player player, EquipmentSlot slot) {
      return slot == null ? ItemStack.EMPTY : player.getItemBySlot(slot).copy();
   }

   private static void write(Player player, EquipmentSlot slot, ItemStack stack) {
      if (slot != null) {
         player.setItemSlot(slot, stack);
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return present;
      }
      resolved = true;

      if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
         return false;
      }

      back = slotNamed(BACK_SLOT);
      belt = slotNamed(BELT_SLOT);
      present = back != null && belt != null;

      if (present) {
         Postmortem.LOGGER.info("[postmortem] combat amenities detected; statues will wear its back and belt items");
      } else {
         Postmortem.LOGGER.warn(
            "[postmortem] combat amenities is present but its back and belt slots were not found; skipping");
      }

      return present;
   }

   private static EquipmentSlot slotNamed(String name) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (slot.name().equals(name)) {
            return slot;
         }
      }

      return null;
   }
}
