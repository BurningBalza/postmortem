package dev.burning.postmortem.compat.accessories;

import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.WornItem;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.5 {
/*import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotPath;
*///?} else {
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
//?}
//? if >=1.21.10 {
/*import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoryRenderState;
*///?}

/**
 * Hands a statue's Accessories items to the renderers Accessories items register. The statue's model and render state
 * stand in for the wearer's, so items follow the statue's pose.
 */
final class AccessoriesClientBridge {
   //? if >=1.21.10 {
   /*// The statue's copies are not in an Accessories inventory; with no containers to ask, every slot counts as shown.
   private static final AccessoriesStorageLookup NO_STORAGE = java.util.Map::of;
   *///?}

   private AccessoriesClientBridge() {
   }

   static void render(WornItem item, WornRenderer.Context context) {
      ItemStack stack = item.stack();
      //? if >=1.21.5 {
      /*AccessoryRenderer renderer = AccessoriesRendererRegistry.getRenderer(stack);
      *///?} else
      AccessoryRenderer renderer = AccessoriesRendererRegistry.getRender(stack);
      if (renderer == null) {
         return;
      }

      //? if >=1.21.10 {
      /*AccessoryRenderState state = renderer.createRenderState(
         stack, SlotPath.of(item.slot(), item.index()), NO_STORAGE, context.statue(), context.state());
      if (state != null) {
         renderer.render(state, context.state(), context.renderer().getModel(), context.matrices(), context.sink().raw());
      }
      *///?} elif >=1.21.5 {
      /*renderer.render(
         stack, SlotPath.of(item.slot(), item.index()), context.matrices(), context.renderer().getModel(),
         context.state(), context.sink().raw(), context.light(), 1.0F);
      *///?} elif >=1.21.2 {
      /*renderer.render(
         stack, SlotReference.of(context.statue(), item.slot(), item.index()), context.matrices(),
         context.renderer().getModel(), context.state(), context.sink().raw(), context.light(), 1.0F);
      *///?} else {
      renderer.render(
         stack, SlotReference.of(context.statue(), item.slot(), item.index()), context.matrices(),
         context.renderer().getModel(), context.sink().raw(), context.light(), context.limbAngle(), context.limbDistance(),
         1.0F, context.state().ageInTicks, context.state().yRot, context.state().xRot);
      //?}
   }
}
