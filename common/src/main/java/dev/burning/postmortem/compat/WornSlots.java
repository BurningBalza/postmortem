package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface WornSlots {
   String id();

   boolean present();

   List<WornItem> takeDroppable(ServerPlayer player, DamageSource source);

   boolean restore(Player player, WornSlot slot, ItemStack stack);

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
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
               Postmortem.LOGGER.warn("[postmortem] could not start {}; its slots are left to it", name, e);
            }
         }
         return List.copyOf(loaded);
      }
   }
}
