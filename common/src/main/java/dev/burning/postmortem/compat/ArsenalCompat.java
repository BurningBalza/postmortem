package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.lang.reflect.Method;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArsenalCompat {
   private static final String MOD_ID = "arsenal";
   private static final String COMPONENT = "dev.doctor4t.arsenal.cca.BackWeaponComponent";

   public static final TagKey<Item> BIG_WEAPONS = tag("big_weapons");
   public static final TagKey<Item> SHIELDS = tag("shields");

   private static boolean resolved;
   private static boolean available;
   private static Method getBackWeapon;

   private ArsenalCompat() {
   }

   private static TagKey<Item> tag(String path) {
      //? if >=1.21 {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, path));
      //?} else
      //return TagKey.create(Registries.ITEM, new Identifier(MOD_ID, path));
   }

   public static boolean isPresent() {
      return resolve();
   }

   public static ItemStack get(Player player) {
      if (!resolve()) {
         return ItemStack.EMPTY;
      }

      try {
         ItemStack stack = (ItemStack) getBackWeapon.invoke(null, player);
         return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
      } catch (ReflectiveOperationException | RuntimeException e) {
         available = false;
         Postmortem.LOGGER.warn("[postmortem] arsenal integration failed and is now disabled", e);
         return ItemStack.EMPTY;
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return available;
      }
      resolved = true;

      if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
         return false;
      }

      Class<?> component;
      try {
         component = Class.forName(COMPONENT);
      } catch (ClassNotFoundException e) {
         return false;
      }

      try {
         getBackWeapon = component.getMethod("getBackWeapon", Player.class);
         available = true;
         Postmortem.LOGGER.info("[postmortem] arsenal detected; back weapons will appear on statues");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] arsenal is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
