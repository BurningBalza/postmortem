package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.WornItem;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;
//? if <1.21.4
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * Hands a statue's Curios items to the renderers Curios items register for themselves. The statue's renderer stands in
 * for the wearer's: through 1.21.1 Curios renderers pose themselves from the wearer's live model, after that from its
 * render state, and the statue's state is a humanoid one either way.
 */
final class CuriosClientBridge {
   private CuriosClientBridge() {
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   static void render(WornItem item, WornRenderer.Context context) {
      ItemStack stack = item.stack();
      SlotContext slot = new SlotContext(item.slot(), context.statue(), item.index(), item.cosmetic(), true);

      //? if >=1.21.4 {
      /*ICurioRenderer renderer = ICurioRenderer.getOrNull(stack);
      if (renderer == null) {
         return;
      }

      renderer.render(
         stack, slot, context.matrices(), context.sink().raw(), context.light(), context.state(), context.renderer(),
         context.rendererContext(), context.state().yRot, context.state().xRot);
      *///?} else {
      ICurioRenderer renderer = CuriosRendererRegistry.getRenderer(stack.getItem()).orElse(null);
      if (renderer == null) {
         return;
      }

      renderer.render(
         stack, slot, context.matrices(), (net.minecraft.client.renderer.entity.RenderLayerParent) context.renderer(),
         context.sink().raw(), context.light(), context.limbAngle(), context.limbDistance(), 1.0F,
         context.state().ageInTicks, context.state().yRot, context.state().xRot);
      //?}
   }
}
