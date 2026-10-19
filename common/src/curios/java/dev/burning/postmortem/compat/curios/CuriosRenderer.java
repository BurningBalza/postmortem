package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;

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
