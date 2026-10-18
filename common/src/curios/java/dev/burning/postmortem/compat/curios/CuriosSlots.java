package dev.burning.postmortem.compat.curios;

import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlot;
import dev.burning.postmortem.compat.WornSlots;
import dev.burning.postmortem.platform.Platform;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Curios' slots. Nothing here touches Curios itself; {@link CuriosBridge} does, once Curios is known to be loaded. */
public final class CuriosSlots implements WornSlots {
   static final String MOD_ID = "curios";

   @Override
   public String id() {
      return MOD_ID;
   }

   @Override
   public boolean present() {
      return Platform.INSTANCE.isModLoaded(MOD_ID);
   }

   @Override
   public List<WornItem> takeDroppable(ServerPlayer player, DamageSource source) {
      return CuriosBridge.takeDroppable(player, source);
   }

   @Override
   public boolean restore(Player player, WornSlot slot, ItemStack stack) {
      return CuriosBridge.restore(player, slot, stack);
   }
}
