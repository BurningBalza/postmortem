package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CosmeticArmorCompat {
   private static final String MOD_ID = "cosmeticarmorreworked";

   private static final EquipmentSlot[] SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

   private static boolean resolved;
   private static boolean available;
   private static Method getCAStacks;
   private static Method getStackInSlot;
   private static Method setStackInSlot;
   private static Method isSkinArmor;
   private static Field keepThroughDeath;

   private CosmeticArmorCompat() {
   }

   public static boolean isPresent() {
      return resolve();
   }

   public static Map<EquipmentSlot, ItemStack> appearance(Player player) {
      Map<EquipmentSlot, ItemStack> look = new EnumMap<>(EquipmentSlot.class);
      if (!resolve()) {
         return look;
      }

      try {
         Object stacks = getCAStacks.invoke(null, player.getUUID());
         if (stacks == null) {
            return look;
         }

         for (int i = 0; i < SLOTS.length; i++) {
            if ((Boolean) isSkinArmor.invoke(stacks, i)) {
               look.put(SLOTS[i], ItemStack.EMPTY);
               continue;
            }

            ItemStack stack = (ItemStack) getStackInSlot.invoke(stacks, i);
            if (stack != null && !stack.isEmpty()) {
               look.put(SLOTS[i], stack.copy());
            }
         }
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
      }

      return look;
   }

   public static boolean dropsOnDeath() {
      if (!resolve()) {
         return false;
      }

      try {
         Object value = keepThroughDeath.get(null);
         if (value == null) {
            return false;
         }

         return !(Boolean) value.getClass().getMethod("get").invoke(value);
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
         return false;
      }
   }

   public static ItemStack take(Player player, int index) {
      if (!resolve()) {
         return ItemStack.EMPTY;
      }

      try {
         Object stacks = getCAStacks.invoke(null, player.getUUID());
         if (stacks == null) {
            return ItemStack.EMPTY;
         }

         ItemStack stack = (ItemStack) getStackInSlot.invoke(stacks, index);
         if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
         }

         ItemStack taken = stack.copy();
         setStackInSlot.invoke(stacks, index, ItemStack.EMPTY);
         return taken;
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
         return ItemStack.EMPTY;
      }
   }

   public static boolean restore(Player player, int index, ItemStack stack) {
      if (!resolve()) {
         return false;
      }

      try {
         Object stacks = getCAStacks.invoke(null, player.getUUID());
         if (stacks == null) {
            return false;
         }

         ItemStack current = (ItemStack) getStackInSlot.invoke(stacks, index);
         if (current != null && !current.isEmpty()) {
            return false;
         }

         setStackInSlot.invoke(stacks, index, stack);
         return true;
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
         return false;
      }
   }

   private static void disable(Exception e) {
      available = false;
      Postmortem.LOGGER.warn("[postmortem] cosmetic armor integration failed and is now disabled", e);
   }

   private static boolean resolve() {
      if (resolved) {
         return available;
      }
      resolved = true;

      if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
         return false;
      }

      try {
         Class<?> api = Class.forName("lain.mods.cos.api.CosArmorAPI");
         Class<?> stacks = Class.forName("lain.mods.cos.api.inventory.CAStacksBase");
         Class<?> configs = Class.forName("lain.mods.cos.impl.ModConfigs");

         getCAStacks = api.getMethod("getCAStacks", UUID.class);
         getStackInSlot = stacks.getMethod("getStackInSlot", int.class);
         setStackInSlot = stacks.getMethod("setStackInSlot", int.class, ItemStack.class);
         isSkinArmor = stacks.getMethod("isSkinArmor", int.class);
         keepThroughDeath = configs.getField("CosArmorKeepThroughDeath");

         available = true;
         Postmortem.LOGGER.info("[postmortem] cosmetic armor reworked detected; memorials will wear cosmetic armor");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] cosmetic armor reworked is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
