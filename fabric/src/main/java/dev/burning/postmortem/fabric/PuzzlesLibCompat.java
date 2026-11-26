package dev.burning.postmortem.fabric;

import dev.burning.postmortem.Postmortem;
import java.lang.reflect.Method;
import java.util.Collection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class PuzzlesLibCompat {
   private static final String MOD_ID = "puzzleslib";
   private static final String[] HOLDERS = {
      "fuzs.puzzleslib.fabric.impl.event.CapturedDropsEntity",
      "fuzs.puzzleslib.impl.event.CapturedDropsEntity",
   };

   private static boolean resolved;
   private static Class<?> holder;
   private static Method getCapturedDrops;

   private PuzzlesLibCompat() {
   }

   @SuppressWarnings("unchecked")
   public static Collection<ItemEntity> capturedDrops(Entity entity) {
      if (!resolve() || !holder.isInstance(entity)) {
         return null;
      }

      try {
         return (Collection<ItemEntity>) getCapturedDrops.invoke(entity);
      } catch (ReflectiveOperationException | RuntimeException e) {
         holder = null;
         Postmortem.LOGGER.warn("[postmortem] puzzleslib integration failed and is now disabled", e);
         return null;
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return holder != null;
      }
      resolved = true;

      if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
         return false;
      }

      for (String name : HOLDERS) {
         try {
            Class<?> type = Class.forName(name);
            getCapturedDrops = type.getMethod("puzzleslib$getCapturedDrops");
            holder = type;
            Postmortem.LOGGER.info("[postmortem] puzzleslib detected; drops it holds back on death will go into memorial statues");
            return true;
         } catch (ReflectiveOperationException | RuntimeException ignored) {
         }
      }

      Postmortem.LOGGER.warn("[postmortem] puzzleslib is present but its captured drops could not be found; death drops it holds back will scatter");
      return false;
   }
}
