package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
//? if >=1.21.4 {
/*import top.theillusivec4.curios.api.common.DropRule;
*///?} else
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
//? if >=1.21
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

/** Reads and writes a player's Curios slots. Only reached through {@link CuriosSlots} once Curios is loaded. */
final class CuriosBridge {
   private CuriosBridge() {
   }

   static List<WornItem> takeDroppable(ServerPlayer player, DamageSource source) {
      ICuriosItemHandler inventory = inventoryOf(player);
      if (inventory == null) {
         return List.of();
      }

      List<WornItem> taken = new ArrayList<>();
      inventory.getCurios().forEach((id, handler) -> {
         take(player, source, id, handler, handler.getStacks(), false, taken);
         take(player, source, id, handler, handler.getCosmeticStacks(), true, taken);
      });
      return taken;
   }

   static boolean restore(Player player, WornSlot slot, ItemStack stack) {
      ICuriosItemHandler inventory = inventoryOf(player);
      if (inventory == null) {
         return false;
      }

      return inventory.getStacksHandler(slot.slot()).map(handler -> {
         IDynamicStackHandler stacks = slot.cosmetic() ? handler.getCosmeticStacks() : handler.getStacks();
         if (slot.index() >= stacks.getSlots() || !stacks.getStackInSlot(slot.index()).isEmpty()) {
            return false;
         }

         stacks.setStackInSlot(slot.index(), stack);
         return true;
      }).orElse(false);
   }

   private static void take(
      ServerPlayer player,
      DamageSource source,
      String id,
      ICurioStacksHandler handler,
      IDynamicStackHandler stacks,
      boolean cosmetic,
      List<WornItem> taken) {

      for (int index = 0; index < stacks.getSlots(); index++) {
         ItemStack stack = stacks.getStackInSlot(index);
         if (stack.isEmpty()) {
            continue;
         }

         boolean visible = index >= handler.getRenders().size() || handler.getRenders().get(index);
         if (!drops(new SlotContext(id, player, index, cosmetic, visible), source, handler, stack)) {
            continue;
         }

         taken.add(new WornItem(CuriosSlots.MOD_ID, id, index, cosmetic, visible, stack.copy()));
         stacks.setStackInSlot(index, ItemStack.EMPTY);
      }
   }

   // Curios' own death rules: an item's rule, else its slot's. Kept items stay on the player, and destroyed or vanishing
   // ones are left for Curios to remove, as it would have.
   private static boolean drops(SlotContext context, DamageSource source, ICurioStacksHandler handler, ItemStack stack) {
      //? if >=1.21.1 {
      DropRule rule = curioOf(stack) == null ? DropRule.DEFAULT : curioOf(stack).getDropRule(context, source, true);
      //?} else
      //DropRule rule = curioOf(stack) == null ? DropRule.DEFAULT : curioOf(stack).getDropRule(context, source, 0, true);
      if (rule == DropRule.DEFAULT) {
         rule = handler.getDropRule();
      }

      return rule != DropRule.ALWAYS_KEEP && rule != DropRule.DESTROY && !vanishes(stack);
   }

   private static boolean vanishes(ItemStack stack) {
      //? if >=1.21 {
      return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
      //?} else
      //return EnchantmentHelper.hasVanishingCurse(stack);
   }

   private static top.theillusivec4.curios.api.type.capability.ICurio curioOf(ItemStack stack) {
      //? if <1.20.2 {
      /*return CuriosApi.getCurio(stack).resolve().orElse(null);
      *///?} else
      return CuriosApi.getCurio(stack).orElse(null);
   }

   private static ICuriosItemHandler inventoryOf(LivingEntity entity) {
      //? if <1.20.2 {
      /*return CuriosApi.getCuriosInventory(entity).resolve().orElse(null);
      *///?} else
      return CuriosApi.getCuriosInventory(entity).orElse(null);
   }
}
