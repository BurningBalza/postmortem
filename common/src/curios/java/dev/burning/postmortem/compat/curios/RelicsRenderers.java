package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.item.Item;

final class RelicsRenderers {
   private static final String MOD_ID = "relics";
   private static final String REGISTRY = "it.hurts.sskirillss.relics.init.RelicsRelicRenderers";
   private static final String RENDERER = "it.hurts.sskirillss.relics.client.renderer.items.base.IRelicRenderer";

   private static boolean resolved;
   private static Method lookup;
   private static Method render;

   private RelicsRenderers() {
   }

   static Object find(Item item) {
      if (!resolve()) {
         return null;
      }

      try {
         return ((Optional<?>) lookup.invoke(null, item)).orElse(null);
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("Relics could not look up a renderer for " + item, e);
      }
   }

   static void render(Object renderer, Object... args) {
      try {
         render.invoke(renderer, args);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof RuntimeException runtime) {
            throw runtime;
         }
         throw new IllegalStateException("Relics could not draw an item", e.getCause());
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("Relics could not draw an item", e);
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return render != null;
      }
      resolved = true;

      if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
         return false;
      }

      try {
         lookup = Class.forName(REGISTRY).getMethod("getRenderer", Item.class);
         for (Method method : Class.forName(RENDERER).getMethods()) {
            if (method.getName().equals("render") && method.getParameterCount() == 12) {
               render = method;
               break;
            }
         }
      } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
         render = null;
      }

      if (render == null) {
         lookup = null;
         Postmortem.LOGGER.info("[postmortem] this relics has no renderer registry to borrow; statues show only relics drawn through curios");
      }
      return render != null;
   }
}
