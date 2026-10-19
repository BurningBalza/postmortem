package dev.burning.postmortem.client;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a Traveler's Backpack a statue wore in a Curios, Accessories or Trinkets slot. Traveler's Backpack does not
 * register with those mods' renderers; it draws the worn backpack from its own player layer, through a public static
 * {@code BackpackLayer.renderBackpackLayer}. That method's parameters change between versions (entity or render state,
 * buffers or submit collector, with or without model objects of its own), so it is found by name and each parameter
 * is filled by type: the statue's model, poses, buffers, light, entity or state, and the backpack. Anything else is
 * one of Traveler's Backpack's own objects, made once - its shared static instance if the class has one, or a new one.
 */
public final class TravelersBackpackRenderer implements WornRenderer {
   private static final String MOD_ID = "travelersbackpack";
   private static final String LAYER = "com.tiviacz.travelersbackpack.client.renderer.BackpackLayer";

   private static boolean resolved;
   private static Method render;
   private static final Map<Class<?>, Object> OWN_OBJECTS = new HashMap<>();

   @Override
   public String id() {
      return MOD_ID;
   }

   @Override
   public boolean present() {
      return Platform.INSTANCE.isModLoaded(MOD_ID) && resolve();
   }

   @Override
   public boolean draws(WornItem item) {
      // Whichever mod's slot it was worn in; those mods have no renderer of their own for it.
      return BuiltInRegistries.ITEM.getKey(item.stack().getItem()).getNamespace().equals(MOD_ID);
   }

   @Override
   public void render(WornItem item, Context context) {
      Class<?>[] types = render.getParameterTypes();
      Object[] args = new Object[types.length];
      for (int i = 0; i < types.length; i++) {
         args[i] = argument(types[i], item.stack(), context);
      }

      try {
         render.invoke(null, args);
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("Traveler's Backpack could not draw " + item.stack(), e);
      }
   }

   private static Object argument(Class<?> type, ItemStack backpack, Context context) {
      if (type == int.class) {
         return context.light();
      }
      if (type == ItemStack.class) {
         return backpack;
      }
      for (Object candidate : new Object[] {
         context.renderer().getModel(), context.matrices(), context.sink().raw(), context.state(), context.statue()}) {
         if (type.isInstance(candidate)) {
            return candidate;
         }
      }
      return OWN_OBJECTS.computeIfAbsent(type, TravelersBackpackRenderer::ownObject);
   }

   private static Object ownObject(Class<?> type) {
      try {
         for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && type.isAssignableFrom(field.getType())) {
               return field.get(null);
            }
         }
         Constructor<?> constructor = type.getConstructor();
         return constructor.newInstance();
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException("no way to make a " + type.getName() + " for Traveler's Backpack", e);
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return render != null;
      }
      resolved = true;

      try {
         for (Method method : Class.forName(LAYER).getMethods()) {
            if (method.getName().equals("renderBackpackLayer") && Modifier.isStatic(method.getModifiers())) {
               render = method;
               break;
            }
         }
      } catch (ClassNotFoundException | LinkageError e) {
         render = null;
      }

      if (render == null) {
         Postmortem.LOGGER.info("[postmortem] this traveler's backpack has no backpack renderer to borrow; statues will not show worn backpacks");
      }
      return render != null;
   }
}
