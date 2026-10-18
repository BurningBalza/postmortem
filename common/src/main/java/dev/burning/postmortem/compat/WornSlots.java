package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Another mod's extra equipment slots: Curios or Accessories. The bridges live in common's {@code curios} and
 * {@code accessories} source sets, which only the loaders those mods run on compile in, so they are found by name: a
 * jar without one simply has no class to find.
 */
public interface WornSlots {
   /** The mod's id, as stored in {@link WornItem#mod()}. */
   String id();

   /** Whether the mod is installed and its API matched. Checked once, before any other method is called. */
   boolean present();

   /** Takes out of the player's slots every item the mod would drop on this death, and returns them. */
   List<WornItem> takeDroppable(ServerPlayer player, DamageSource source);

   /** Puts {@code stack} back in the slot it was worn in, or returns false if that slot is gone or already full. */
   boolean restore(Player player, WornSlot slot, ItemStack stack);

   /** The bridges whose mod is installed. */
   static List<WornSlots> loaded() {
      return Loaded.ALL;
   }

   static Optional<WornSlots> byId(String id) {
      return Loaded.ALL.stream().filter(slots -> slots.id().equals(id)).findFirst();
   }

   final class Loaded {
      private static final List<String> BRIDGES = List.of(
         "dev.burning.postmortem.compat.curios.CuriosSlots",
         "dev.burning.postmortem.compat.accessories.AccessoriesSlots");

      static final List<WornSlots> ALL = load();

      private Loaded() {
      }

      private static List<WornSlots> load() {
         List<WornSlots> loaded = new ArrayList<>();
         for (String name : BRIDGES) {
            try {
               WornSlots slots = (WornSlots) Class.forName(name).getDeclaredConstructor().newInstance();
               if (slots.present()) {
                  loaded.add(slots);
                  Postmortem.LOGGER.info("[postmortem] {} detected; memorials keep what was worn in its slots", slots.id());
               }
            } catch (ClassNotFoundException e) {
               // This loader has no such bridge.
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
               Postmortem.LOGGER.warn("[postmortem] could not start {}; its slots are left to it", name, e);
            }
         }
         return List.copyOf(loaded);
      }
   }
}
