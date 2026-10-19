package dev.burning.postmortem.compat.accessories;

import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlot;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//? if >=1.21.5 {
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.events.DropRule;
//?} else {
/*import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
*///?}
//? if >=1.21
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

final class AccessoriesBridge {
   private AccessoriesBridge() {
   }

   static List<WornItem> takeDroppable(ServerPlayer player, DamageSource source) {
      AccessoriesCapability capability = AccessoriesCapability.get(player);
      if (capability == null) {
         return List.of();
      }

      List<WornItem> taken = new ArrayList<>();
      capability.getContainers().forEach((name, container) -> {
         take(player, source, name, container, container.getAccessories(), false, taken);
         take(player, source, name, container, container.getCosmeticAccessories(), true, taken);
      });
      return taken;
   }

   static boolean restore(Player player, WornSlot slot, ItemStack stack) {
      AccessoriesCapability capability = AccessoriesCapability.get(player);
      AccessoriesContainer container = capability == null ? null : capability.getContainers().get(slot.slot());
      if (container == null) {
         return false;
      }

      Container stacks = slot.cosmetic() ? container.getCosmeticAccessories() : container.getAccessories();
      if (slot.index() >= stacks.getContainerSize() || !stacks.getItem(slot.index()).isEmpty()) {
         return false;
      }

      stacks.setItem(slot.index(), stack);
      return true;
   }

   private static void take(
      ServerPlayer player,
      DamageSource source,
      String name,
      AccessoriesContainer container,
      Container stacks,
      boolean cosmetic,
      List<WornItem> taken) {

      for (int index = 0; index < stacks.getContainerSize(); index++) {
         ItemStack stack = stacks.getItem(index);
         if (stack.isEmpty() || !drops(player, source, name, container, index, stack)) {
            continue;
         }

         taken.add(new WornItem(AccessoriesSlots.MOD_ID, name, index, cosmetic, container.shouldRender(index), stack.copy()));
         stacks.setItem(index, ItemStack.EMPTY);
      }
   }

   private static boolean drops(
      ServerPlayer player, DamageSource source, String name, AccessoriesContainer container, int index, ItemStack stack) {

      DropRule rule = container.slotType() != null ? container.slotType().dropRule() : DropRule.DEFAULT;
      if (rule == DropRule.DEFAULT) {
         Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
         if (accessory != null) {
            rule = accessory.getDropRule(stack, SlotReference.of(player, name, index), source);
         }
      }

      return rule != DropRule.KEEP && rule != DropRule.DESTROY && !vanishes(stack);
   }

   private static boolean vanishes(ItemStack stack) {
      //? if >=1.21 {
      return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
      //?} else
      //return EnchantmentHelper.hasVanishingCurse(stack);
   }
}
