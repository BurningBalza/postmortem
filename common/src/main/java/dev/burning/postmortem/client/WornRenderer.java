package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.entity.StatueEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public interface WornRenderer {
   String id();

   boolean present();

   default boolean draws(WornItem item) {
      return this.id().equals(item.mod());
   }

   void render(WornItem item, Context context);

   default void extract(List<WornItem> shown, StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
   }

   record Context(
      StatueEntity statue,
      StatueEntityRenderer.State state,
      StatueEntityRenderer<?> renderer,
      EntityRendererProvider.Context rendererContext,
      PoseStack matrices,
      RenderSink sink,
      int light,
      float limbAngle,
      float limbDistance) {
   }

   static List<WornRenderer> loaded() {
      return Loaded.ALL;
   }

   final class Loaded {
      private static final List<String> RENDERERS = List.of(
         "dev.burning.postmortem.compat.curios.CuriosRenderer",
         "dev.burning.postmortem.compat.accessories.AccessoriesRenderer",
         "dev.burning.postmortem.compat.trinkets.TrinketsRenderer",
         "dev.burning.postmortem.client.TravelersBackpackRenderer");

      static final List<WornRenderer> ALL = load();

      private Loaded() {
      }

      private static List<WornRenderer> load() {
         List<WornRenderer> loaded = new ArrayList<>();
         for (String name : RENDERERS) {
            try {
               WornRenderer renderer = (WornRenderer) Class.forName(name).getDeclaredConstructor().newInstance();
               if (renderer.present()) {
                  loaded.add(renderer);
               }
            } catch (ClassNotFoundException e) {
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
               Postmortem.LOGGER.warn("[postmortem] could not start {}; statues will not show its items", name, e);
            }
         }
         return List.copyOf(loaded);
      }
   }
}
