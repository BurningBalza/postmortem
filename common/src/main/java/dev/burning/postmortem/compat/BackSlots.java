package dev.burning.postmortem.compat;

import dev.burning.postmortem.entity.StatuePart;

/**
 * Whether a back- or belt-slot mod is installed. A statue's back and belt slots, their tooltip lines and their parts in
 * the pose screen exist only when one is; mod presence is fixed at launch, so this never changes mid-game.
 */
public final class BackSlots {
   private BackSlots() {
   }

   /** Backslot Mod or Combat Amenities: either gives players a back slot. */
   public static boolean back() {
      return BackslotCompat.isPresent() || CombatAmenitiesCompat.isPresent();
   }

   /** Only Combat Amenities has a belt slot. */
   public static boolean belt() {
      return CombatAmenitiesCompat.isPresent();
   }

   /** Whether the pose screen offers this part; body parts and hand items always exist. */
   public static boolean has(StatuePart part) {
      return switch (part) {
         case BACK_ITEM -> back();
         case BELT_ITEM -> belt();
         default -> true;
      };
   }
}
