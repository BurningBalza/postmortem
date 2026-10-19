package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import java.lang.reflect.Method;
import java.util.Map;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.world.entity.Entity;

public final class PehkuiCompat {
   private static final String MOD_ID = "pehkui";

   private static boolean resolved;
   private static boolean available;
   private static Map<?, ?> scaleTypes;
   private static Method getScaleData;
   private static Method fromScale;
   private static Method hasDefaultValues;
   private static Method setPersistence;

   private PehkuiCompat() {
   }

   public static void copyScales(Entity from, Entity to) {
      if (!resolve()) {
         return;
      }

      try {
         for (Object type : scaleTypes.values()) {
            Object source = getScaleData.invoke(type, from);
            Object target = getScaleData.invoke(type, to);
            fromScale.invoke(target, source);

            if (!(boolean) hasDefaultValues.invoke(target)) {
               setPersistence.invoke(target, Boolean.TRUE);
            }
         }

         to.refreshDimensions();
      } catch (ReflectiveOperationException | RuntimeException e) {
         available = false;
         Postmortem.LOGGER.warn("[postmortem] pehkui integration failed and is now disabled", e);
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
         Class<?> registries = Class.forName("virtuoel.pehkui.api.ScaleRegistries");
         Class<?> scaleType = Class.forName("virtuoel.pehkui.api.ScaleType");
         Class<?> scaleData = Class.forName("virtuoel.pehkui.api.ScaleData");

         scaleTypes = (Map<?, ?>) registries.getField("SCALE_TYPES").get(null);
         getScaleData = scaleType.getMethod("getScaleData", Entity.class);
         fromScale = scaleData.getMethod("fromScale", scaleData);
         hasDefaultValues = scaleData.getMethod("hasDefaultValues");
         setPersistence = scaleData.getMethod("setPersistence", Boolean.class);

         available = true;
         Postmortem.LOGGER.info("[postmortem] pehkui detected; statues will keep their owner's scale");
      } catch (ReflectiveOperationException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] pehkui is present but its API did not match what was expected; skipping", e);
         available = false;
      }

      return available;
   }
}
