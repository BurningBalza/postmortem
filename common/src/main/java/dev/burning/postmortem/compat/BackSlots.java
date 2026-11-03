package dev.burning.postmortem.compat;

import dev.burning.postmortem.entity.StatuePart;

public final class BackSlots {
   private BackSlots() {
   }

   public static boolean back() {
      return BackslotCompat.isPresent() || CombatAmenitiesCompat.isPresent() || ArsenalCompat.isPresent();
   }

   public static boolean belt() {
      return CombatAmenitiesCompat.isPresent();
   }

   public static boolean has(StatuePart part) {
      return switch (part) {
         case BACK_ITEM -> back();
         case BELT_ITEM -> belt();
         default -> true;
      };
   }
}
