package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;

/** Draws Curios items on statues. As with {@link CuriosSlots}, Curios' own classes are only in {@link CuriosClientBridge}. */
public final class CuriosRenderer implements WornRenderer {
   @Override
   public String id() {
      return CuriosSlots.MOD_ID;
   }

   @Override
   public boolean present() {
      return Platform.INSTANCE.isModLoaded(CuriosSlots.MOD_ID);
   }

   @Override
   public void render(WornItem item, Context context) {
      CuriosClientBridge.render(item, context);
   }
}
