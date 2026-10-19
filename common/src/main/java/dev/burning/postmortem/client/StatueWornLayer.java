package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.compat.WornItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
//import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.Item;
//? if <1.21.2
//import dev.burning.postmortem.entity.StatueEntity;

//? if >=1.21.2 {
public class StatueWornLayer extends RenderLayer<StatueEntityRenderer.State, StatueEntityModel> {
//?} else
//public class StatueWornLayer extends RenderLayer<StatueEntity, StatueEntityModel> {
   private static final Set<Item> FAILED = new HashSet<>();
   private static final Set<String> FAILED_EXTRACT = new HashSet<>();

   private final StatueEntityRenderer<?> renderer;
   private final EntityRendererProvider.Context rendererContext;

   public StatueWornLayer(StatueEntityRenderer<?> renderer, EntityRendererProvider.Context rendererContext) {
      super(renderer);
      this.renderer = renderer;
      this.rendererContext = rendererContext;
   }

   @Override
   //? if >=1.21.9 {
   public void submit(
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(queue);
   //?} elif >=1.21.2 {
   /*public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(buffers);
   *///?} else {
   /*public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntity entity,
      float limbAngle,
      float limbDistance,
      float tickDelta,
      float age,
      float headYaw,
      float headPitch) {
      RenderSink sink = new RenderSink(buffers);
      StatueEntityRenderer.State state = this.getParentModel().current;
   *///?}

      List<WornItem> worn = state.worn;
      if (worn.isEmpty() || state.statue == null || WornRenderer.loaded().isEmpty()) {
         return;
      }

      WornRenderer.Context context = new WornRenderer.Context(
         state.statue, state, this.renderer, this.rendererContext, matrices, sink, light, limbAngle, limbDistance);
      //? if >=1.21.5 {
      net.minecraft.world.entity.EntityType<?> ownType = state.entityType;
      //? if >=26.2 {
      /*state.entityType = net.minecraft.world.entity.EntityTypes.PLAYER;
      *///?} else
      state.entityType = net.minecraft.world.entity.EntityType.PLAYER;
      try {
      //?}
         for (WornItem item : shown(worn)) {
            for (WornRenderer renderer : WornRenderer.loaded()) {
               if (renderer.draws(item) && !FAILED.contains(item.stack().getItem())) {
                  draw(renderer, item, context);
               }
            }
         }
      //? if >=1.21.5 {
      } finally {
         state.entityType = ownType;
      }
      //?}
   }

   public static void extract(StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
      if (state.statue == null || state.worn.isEmpty() || WornRenderer.loaded().isEmpty()) {
         return;
      }

      List<WornItem> shown = shown(state.worn);
      for (WornRenderer renderer : WornRenderer.loaded()) {
         List<WornItem> mine = shown.stream().filter(renderer::draws).toList();
         if (mine.isEmpty()) {
            continue;
         }

         try {
            renderer.extract(mine, state, rendererContext);
         } catch (RuntimeException | LinkageError e) {
            if (FAILED_EXTRACT.add(renderer.id())) {
               Postmortem.LOGGER.warn("[postmortem] {} could not prepare a statue's items; they may not show", renderer.id(), e);
            }
         }
      }
   }

   private static List<WornItem> shown(List<WornItem> worn) {
      List<WornItem> shown = new ArrayList<>(worn.size());
      for (WornItem item : worn) {
         if (item.visible() && (item.cosmetic() || !coveredByCosmetic(worn, item))) {
            shown.add(item);
         }
      }
      return shown;
   }

   private static void draw(WornRenderer renderer, WornItem item, WornRenderer.Context context) {
      context.matrices().pushPose();
      try {
         renderer.render(item, context);
      } catch (RuntimeException | LinkageError e) {
         FAILED.add(item.stack().getItem());
         Postmortem.LOGGER.warn("[postmortem] {} could not draw {} on a statue; statues will stop showing it",
            renderer.id(), item.stack().getHoverName().getString(), e);
      } finally {
         context.matrices().popPose();
      }
   }

   private static boolean coveredByCosmetic(List<WornItem> worn, WornItem item) {
      for (WornItem other : worn) {
         if (other.cosmetic() && other.mod().equals(item.mod()) && other.slot().equals(item.slot()) && other.index() == item.index()) {
            return true;
         }
      }
      return false;
   }
}
