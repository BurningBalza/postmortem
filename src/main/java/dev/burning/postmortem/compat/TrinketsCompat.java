package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.burning.postmortem.Postmortem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class TrinketsCompat {
   private static final String MOD_ID = "trinkets";

   private static boolean resolved;
   private static boolean available;
   private static Method getTrinketComponent;
   private static Method getAllEquipped;
   private static Method getInventory;
   private static Method getSlotInventory;
   private static Method getSlotIndex;
   private static Method getSlotType;
   private static Method getGroup;
   private static Method getName;

   /** Where a trinket was worn, remembered so it can be put back in the same slot. */
   public record TrinketSlot(String group, String name, int index) {
      public static final Codec<TrinketSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.STRING.fieldOf("Group").forGetter(TrinketSlot::group),
         Codec.STRING.fieldOf("Name").forGetter(TrinketSlot::name),
         Codec.INT.fieldOf("Index").forGetter(TrinketSlot::index)
      ).apply(instance, TrinketSlot::new));
   }

   public record Equipped(TrinketSlot slot, ItemStack stack) {
   }

   private TrinketsCompat() {
   }

   public static List<Equipped> takeAll(LivingEntity player) {
      if (!resolve()) {
         return List.of();
      }

      List<Equipped> taken = new ArrayList<>();
      try {
         Object component = ((Optional<?>) getTrinketComponent.invoke(null, player)).orElse(null);
         if (component == null) {
            return List.of();
         }

         for (Object equipped : (List<?>) getAllEquipped.invoke(component)) {
            Class<?> pair = equipped.getClass();
            ItemStack stack = (ItemStack) pair.getMethod("getB").invoke(equipped);
            if (stack == null || stack.isEmpty()) {
               continue;
            }

            Object slotReference = pair.getMethod("getA").invoke(equipped);
            Object trinketInventory = getSlotInventory.invoke(slotReference);
            int index = (Integer) getSlotIndex.invoke(slotReference);

            Object slotType = getSlotType.invoke(trinketInventory);
            taken.add(new Equipped(
               new TrinketSlot((String) getGroup.invoke(slotType), (String) getName.invoke(slotType), index),
               stack.copy()));
            ((Container) trinketInventory).setItem(index, ItemStack.EMPTY);
         }
      } catch (ReflectiveOperationException | RuntimeException e) {
         available = false;
         Postmortem.LOGGER.warn("[postmortem] trinkets integration failed and is now disabled; trinkets will drop normally", e);
         return List.of();
      }

      return taken;
   }

   public static boolean restore(LivingEntity player, TrinketSlot slot, ItemStack stack) {
      if (!resolve()) {
         return false;
      }

      try {
         Object component = ((Optional<?>) getTrinketComponent.invoke(null, player)).orElse(null);
         if (component == null) {
            return false;
         }

         Map<?, ?> groups = (Map<?, ?>) getInventory.invoke(component);
         if (!(groups.get(slot.group()) instanceof Map<?, ?> slots)) {
            return false;
         }

         if (!(slots.get(slot.name()) instanceof Container inventory)) {
            return false;
         }

         int index = slot.index();
         if (index < 0 || index >= inventory.getContainerSize() || !inventory.getItem(index).isEmpty()) {
            return false;
         }

         inventory.setItem(index, stack);
         return true;
      } catch (ReflectiveOperationException | RuntimeException e) {
         available = false;
         Postmortem.LOGGER.warn(
            "[postmortem] trinkets integration failed and is now disabled; trinkets will go back to the inventory", e);
         return false;
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

      try {
         Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
         Class<?> component = Class.forName("dev.emi.trinkets.api.TrinketComponent");
         Class<?> slotReference = Class.forName("dev.emi.trinkets.api.SlotReference");
         Class<?> trinketInventory = Class.forName("dev.emi.trinkets.api.TrinketInventory");
         Class<?> slotType = Class.forName("dev.emi.trinkets.api.SlotType");

         getTrinketComponent = api.getMethod("getTrinketComponent", LivingEntity.class);
         getAllEquipped = component.getMethod("getAllEquipped");
         getInventory = component.getMethod("getInventory");
         getSlotInventory = slotReference.getMethod("inventory");
         getSlotIndex = slotReference.getMethod("index");
         getSlotType = trinketInventory.getMethod("getSlotType");
         getGroup = slotType.getMethod("getGroup");
         getName = slotType.getMethod("getName");

         available = true;
         Postmortem.LOGGER.info("[postmortem] trinkets detected; equipped trinkets will be stored on memorial statues");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] trinkets is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
