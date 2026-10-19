package dev.burning.postmortem.compat.trinkets;

import dev.burning.postmortem.client.StatueEntityRenderer;
import dev.burning.postmortem.client.WornRenderer;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.platform.Platform;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Draws trinkets on statues. Trinkets Updated gives every living entity's renderer its own layer, the statue's
 * included, so this only fills that layer's state; Trinkets' own classes are only in {@link TrinketsClientBridge}.
 */
public final class TrinketsRenderer implements WornRenderer {
   // Only Trinkets Updated has this layer; the original Trinkets' renderers are not bridged.
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
      // Trinkets' layer draws them, from what extract left in the state.
   }

   @Override
   public void extract(List<WornItem> shown, StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
      TrinketsClientBridge.extract(shown, state, rendererContext);
   }
}
