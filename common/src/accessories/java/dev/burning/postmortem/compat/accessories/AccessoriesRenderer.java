package dev.burning.postmortem.compat.accessories;

import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;

public final class AccessoriesRenderer implements WornRenderer {
   @Override
   public String id() {
      return AccessoriesSlots.MOD_ID;
   }

   @Override
   public boolean present() {
      return Platform.INSTANCE.isModLoaded(AccessoriesSlots.MOD_ID);
   }

   @Override
   public void render(WornItem item, Context context) {
      AccessoriesClientBridge.render(item, context);
   }
}
