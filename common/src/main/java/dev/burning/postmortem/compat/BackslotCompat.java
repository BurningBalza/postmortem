package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BackslotCompat {
   private static final String MOD_ID = "backslot";

   private static boolean resolved;
   private static boolean available;
   private static Method componentGet;
   private static Method componentGetStack;
   private static Method componentSetStack;
   private static Field shouldDropBackslotItem;
   private static Object componentKey;
   private static Method componentKeySync;

   private BackslotCompat() {
   }

   public static ItemStack get(Player player) {
      if (!resolve()) {
         return ItemStack.EMPTY;
      }

      try {
         Object component = componentGet.invoke(null, player);
         if (component == null) {
            return ItemStack.EMPTY;
         }

         ItemStack stack = (ItemStack) componentGetStack.invoke(component);
         return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
         return ItemStack.EMPTY;
      }
   }

   public static boolean dropsOnDeath() {
      if (!resolve()) {
         return false;
      }

      try {
         return shouldDropBackslotItem.getBoolean(null);
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
         return false;
      }
   }

   public static boolean restore(Player player, ItemStack stack) {
      if (!resolve() || !get(player).isEmpty()) {
         return false;
      }

      set(player, stack);
      return true;
   }

   public static void clear(Player player) {
      set(player, ItemStack.EMPTY);
   }

   public static void set(Player player, ItemStack stack) {
      write(player, stack, true);
   }

   public static void setLocal(Player player, ItemStack stack) {
      write(player, stack, false);
   }

   private static void write(Player player, ItemStack stack, boolean sync) {
      if (!resolve()) {
         return;
      }

      try {
         Object component = componentGet.invoke(null, player);
         if (component == null) {
            return;
         }

         componentSetStack.invoke(component, stack);
         if (sync) {
            componentKeySync.invoke(componentKey, player);
         }
      } catch (ReflectiveOperationException | RuntimeException e) {
         disable(e);
      }
   }

   private static void disable(Exception e) {
      available = false;
      Postmortem.LOGGER.warn("[postmortem] backslot integration failed and is now disabled", e);
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
         Class<?> component = Class.forName("net.medecoole.backslot.registry.BackslotItemComponent");
         Class<?> registry = Class.forName("net.medecoole.backslot.registry.ModComponentRegistry");
         Class<?> config = Class.forName("net.medecoole.backslot.registry.BackslotConfig");
         Class<?> key = Class.forName("org.ladysnake.cca.api.v3.component.ComponentKey");

         componentGet = component.getMethod("get", Player.class);
         componentGetStack = component.getMethod("getStack");
         componentSetStack = component.getMethod("setStack", ItemStack.class);
         shouldDropBackslotItem = config.getField("shouldDropBackslotItem");
         componentKey = registry.getField("BACKSLOT_ITEM_COMPONENT").get(null);
         componentKeySync = key.getMethod("sync", Object.class);

         available = true;
         Postmortem.LOGGER.info("[postmortem] backslot mod detected; back items will appear on statues");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] backslot mod is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
