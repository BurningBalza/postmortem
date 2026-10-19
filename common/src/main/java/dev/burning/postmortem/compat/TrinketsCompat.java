package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.burning.postmortem.Postmortem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trinkets comes in two shapes: the original (mod id {@code trinkets}, {@code dev.emi.trinkets}, through 1.21.11) and
 * Trinkets Updated (mod id {@code trinkets_updated}, {@code eu.pb4.trinkets}, from 26.1, on NeoForge too). Updated only
 * keeps the old id as an alias on Fabric, so both ids are checked and the API is picked by which classes exist.
 */
public final class TrinketsCompat {
   /** The id a statue's copies of trinkets are shown under; see {@link WornItem#mod()}. */
   public static final String MOD_ID = "trinkets";
   private static final String UPDATED_MOD_ID = "trinkets_updated";

   private static boolean resolved;
   private static boolean available;
   private static boolean updated;

   // The original API.
   private static Method getTrinketComponent;
   private static Method getInventory;
   private static Method getAllEquipped;
   private static Method getSlotInventory;
   private static Method getSlotIndex;
   private static Method getSlotType;
   private static Method getGroup;
   private static Method getName;

   // Trinkets Updated.
   private static Method getAttachment;
   private static Method forEach;
   private static Method getInventoryById;
   private static Method accessSlotType;
   private static Method accessIndex;
   private static Method accessCosmetic;
   private static Method accessSet;
   private static Method getCosmeticItem;
   private static Method setCosmeticItem;
   private static Method isVisible;

   /** Where a trinket was worn, remembered so it can be put back in the same slot. */
   public record TrinketSlot(String group, String name, int index, boolean cosmetic) {
      public static final Codec<TrinketSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.STRING.fieldOf("Group").forGetter(TrinketSlot::group),
         Codec.STRING.fieldOf("Name").forGetter(TrinketSlot::name),
         Codec.INT.fieldOf("Index").forGetter(TrinketSlot::index),
         Codec.BOOL.optionalFieldOf("Cosmetic", false).forGetter(TrinketSlot::cosmetic)
      ).apply(instance, TrinketSlot::new));
   }

   /** {@code visible} is the slot's own show/hide toggle; the original Trinkets has none, so it is always true there. */
   public record Equipped(TrinketSlot slot, ItemStack stack, boolean visible) {
      /** A copy for a statue to show, named the way Trinkets Updated names the slot. */
      public WornItem shown() {
         return new WornItem(
            MOD_ID, this.slot.group() + "/" + this.slot.name(), this.slot.index(), this.slot.cosmetic(), this.visible,
            this.stack.copy());
      }
   }

   private TrinketsCompat() {
   }

   /** Whether Trinkets, in either shape, is installed and its API matched. */
   public static boolean present() {
      return resolve();
   }

   public static List<Equipped> takeAll(LivingEntity player) {
      if (!resolve()) {
         return List.of();
      }

      try {
         Object component = component(player);
         if (component == null) {
            return List.of();
         }

         return updated ? takeAllUpdated(component) : takeAllOriginal(component);
      } catch (ReflectiveOperationException | RuntimeException e) {
         available = false;
         Postmortem.LOGGER.warn("[postmortem] trinkets integration failed and is now disabled; trinkets will drop normally", e);
         return List.of();
      }
   }

   private static List<Equipped> takeAllOriginal(Object component) throws ReflectiveOperationException {
      List<Equipped> taken = new ArrayList<>();
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
            new TrinketSlot((String) getGroup.invoke(slotType), (String) getName.invoke(slotType), index, false),
            stack.copy(), true));
         ((Container) trinketInventory).setItem(index, ItemStack.EMPTY);
      }
      return taken;
   }

   private static List<Equipped> takeAllUpdated(Object attachment) throws ReflectiveOperationException {
      // Gathered first and emptied after, so the slots aren't changed while Trinkets walks them. This walks cosmetic
      // slots too, the same as Trinkets' own death drop.
      List<Object> accesses = new ArrayList<>();
      forEach.invoke(attachment, (BiConsumer<Object, ItemStack>) (access, stack) -> {
         if (stack != null && !stack.isEmpty()) {
            accesses.add(access);
         }
      });

      List<Equipped> taken = new ArrayList<>();
      for (Object access : accesses) {
         Object slotType = accessSlotType.invoke(access);
         boolean cosmetic = (Boolean) accessCosmetic.invoke(access);
         int index = (Integer) accessIndex.invoke(access);
         Object inventory = getSlotInventory.invoke(access);
         ItemStack stack = cosmetic
            ? (ItemStack) getCosmeticItem.invoke(inventory, index)
            : ((Container) inventory).getItem(index);
         if (stack.isEmpty()) {
            continue;
         }

         taken.add(new Equipped(
            new TrinketSlot((String) getGroup.invoke(slotType), (String) getName.invoke(slotType), index, cosmetic),
            stack.copy(), (Boolean) isVisible.invoke(inventory, index)));
         accessSet.invoke(access, ItemStack.EMPTY);
      }
      return taken;
   }

   public static boolean restore(LivingEntity player, TrinketSlot slot, ItemStack stack) {
      if (!resolve()) {
         return false;
      }

      try {
         Object component = component(player);
         if (component == null) {
            return false;
         }

         if (!(inventory(component, slot) instanceof Container inventory)) {
            return false;
         }

         int index = slot.index();
         if (index < 0 || index >= inventory.getContainerSize()) {
            return false;
         }

         if (slot.cosmetic()) {
            // Only Trinkets Updated has cosmetic slots; anywhere else the item goes back to the inventory.
            if (!updated || !((ItemStack) getCosmeticItem.invoke(inventory, index)).isEmpty()) {
               return false;
            }
            return (Boolean) setCosmeticItem.invoke(inventory, index, stack);
         }

         if (!inventory.getItem(index).isEmpty()) {
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

   /** The inventory of the slot a trinket was taken from, or null when the player no longer has that slot. */
   private static Object inventory(Object component, TrinketSlot slot) throws ReflectiveOperationException {
      if (updated) {
         // Trinkets Updated names a slot "<group>/<name>".
         return getInventoryById.invoke(component, slot.group() + "/" + slot.name());
      }
      Map<?, ?> groups = (Map<?, ?>) getInventory.invoke(component);
      return groups.get(slot.group()) instanceof Map<?, ?> slots ? slots.get(slot.name()) : null;
   }

   /** The player's trinket holder, or null when it has none. */
   private static Object component(LivingEntity player) throws ReflectiveOperationException {
      if (updated) {
         return getAttachment.invoke(null, player);
      }
      return ((Optional<?>) getTrinketComponent.invoke(null, player)).orElse(null);
   }

   private static boolean resolve() {
      if (resolved) {
         return available;
      }
      resolved = true;

      if (!Platform.INSTANCE.isModLoaded(MOD_ID) && !Platform.INSTANCE.isModLoaded(UPDATED_MOD_ID)) {
         return false;
      }

      try {
         try {
            Class.forName("eu.pb4.trinkets.api.TrinketsApi");
            updated = true;
         } catch (ClassNotFoundException e) {
            updated = false;
         }

         if (updated) {
            Class<?> api = Class.forName("eu.pb4.trinkets.api.TrinketsApi");
            Class<?> attachment = Class.forName("eu.pb4.trinkets.api.TrinketAttachment");
            Class<?> access = Class.forName("eu.pb4.trinkets.api.TrinketSlotAccess");
            Class<?> trinketInventory = Class.forName("eu.pb4.trinkets.api.TrinketInventory");
            Class<?> slotType = Class.forName("eu.pb4.trinkets.api.SlotType");

            getAttachment = api.getMethod("getAttachment", LivingEntity.class);
            forEach = attachment.getMethod("forEach", BiConsumer.class);
            getInventoryById = attachment.getMethod("getInventory", String.class);
            getSlotInventory = access.getMethod("inventory");
            accessSlotType = access.getMethod("slotType");
            accessIndex = access.getMethod("index");
            accessCosmetic = access.getMethod("cosmetic");
            accessSet = access.getMethod("set", ItemStack.class);
            getCosmeticItem = trinketInventory.getMethod("getCosmeticItem", int.class);
            setCosmeticItem = trinketInventory.getMethod("setCosmeticItem", int.class, ItemStack.class);
            isVisible = trinketInventory.getMethod("isVisible", int.class);
            getGroup = slotType.getMethod("group");
            getName = slotType.getMethod("name");
         } else {
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
         }

         available = true;
         Postmortem.LOGGER.info("[postmortem] {} detected; equipped trinkets will be stored on memorial statues",
            updated ? "trinkets updated" : "trinkets");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] trinkets is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
