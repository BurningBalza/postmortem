package dev.burning.postmortem.compat.accessories;

import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlot;
import dev.burning.postmortem.compat.WornSlots;
import dev.burning.postmortem.platform.Platform;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Accessories' slots. Accessories' own classes are only touched by {@link AccessoriesBridge}, once it is loaded. */
public final class AccessoriesSlots implements WornSlots {
   static final String MOD_ID = "accessories";

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
      return AccessoriesBridge.takeDroppable(player, source);
   }

   @Override
   public boolean restore(Player player, WornSlot slot, ItemStack stack) {
      return AccessoriesBridge.restore(player, slot, stack);
   }
}
