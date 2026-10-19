package dev.burning.postmortem.compat.trinkets;

import dev.burning.postmortem.client.StatueEntityRenderer;
import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class TrinketsRenderer implements WornRenderer {
   private static final String UPDATED_MOD_ID = "trinkets_updated";

   @Override
   public String id() {
      return TrinketsCompat.MOD_ID;
   }

   @Override
   public boolean present() {
      return Platform.INSTANCE.isModLoaded(UPDATED_MOD_ID);
   }

   @Override
   public void render(WornItem item, Context context) {
   }

   @Override
   public void extract(List<WornItem> shown, StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
      TrinketsClientBridge.extract(shown, state, rendererContext);
   }
}
