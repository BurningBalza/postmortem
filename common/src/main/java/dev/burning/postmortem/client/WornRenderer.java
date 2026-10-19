package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.entity.StatueEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Draws items worn in another mod's slots on a statue through that mod's own renderers, so the statue shows them the way
 * the player wore them. Found by name like {@link dev.burning.postmortem.compat.WornSlots}.
 */
public interface WornRenderer {
   /** The mod's id, matching {@link WornItem#mod()}. */
   String id();

   /** Whether the mod is installed; checked once, before {@link #render}. */
   boolean present();

   /** Whether this draws {@code item}; by default, the items worn in this mod's own slots. */
   default boolean draws(WornItem item) {
      return this.id().equals(item.mod());
   }

   /** Draws one item; does nothing if the mod has no renderer for it. */
   void render(WornItem item, Context context);

   /**
    * For a mod that draws from the render state through its own layer rather than in {@link StatueWornLayer}: hands
    * it the statue's shown items of this mod while the state is filled.
    */
   default void extract(List<WornItem> shown, StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
   }

   /**
    * Everything a mod's renderer may ask for. The renderer is the statue's own, so a mod that poses its model from the
    * wearer's renderer or render state follows the statue's pose. The limb values are what the layer was given; from
    * 1.21.2 they are also in the state.
    */
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
               // This loader has no such bridge.
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
               Postmortem.LOGGER.warn("[postmortem] could not start {}; statues will not show its items", name, e);
            }
         }
         return List.copyOf(loaded);
      }
   }
}
