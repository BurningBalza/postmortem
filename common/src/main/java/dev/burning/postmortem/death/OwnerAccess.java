package dev.burning.postmortem.death;

import dev.burning.postmortem.entity.MemorialStatueEntity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class OwnerAccess {
   private static final double REACH_SQUARED = 6.0 * 6.0;

   private OwnerAccess() {
   }

   public static boolean handle(ServerPlayer player, ServerboundInteractPacket packet) {
      ServerLevel level = (ServerLevel) player.level();
      //? if >=26.1 {
      Entity target = level.getEntityOrPart(packet.entityId());
      //?} else
      //Entity target = packet.getTarget(level);
      if (!(target instanceof MemorialStatueEntity statue)
         || !statue.isOwner(player)
         || statue.getBoundingBox().distanceToSqr(player.getEyePosition()) >= REACH_SQUARED) {
         return false;
      }

      player.resetLastActionTime();
      //? if >=26.1 {
      player.setShiftKeyDown(packet.usingSecondaryAction());
      use(player, statue, packet.hand(), packet.location());
      return true;
      //?} else {
      /*player.setShiftKeyDown(packet.isUsingSecondaryAction());
      boolean[] interaction = {false};
      packet.dispatch(new ServerboundInteractPacket.Handler() {
         @Override
         public void onInteraction(InteractionHand hand) {
            interaction[0] = true;
         }

         @Override
         public void onInteraction(InteractionHand hand, Vec3 location) {
            interaction[0] = true;
            use(player, statue, hand, location);
         }

         @Override
         public void onAttack() {
         }
      });
      return interaction[0];
      *///?}
   }

   private static void use(ServerPlayer player, MemorialStatueEntity statue, InteractionHand hand, Vec3 location) {
      //? if >=26.1 {
      InteractionResult result = statue.interact(player, hand, location);
      //?} else
      //InteractionResult result = statue.interactAt(player, location, hand);
      if (result.consumesAction()) {
         //? if >=26.3 {
         player.swing(hand, player.getItemInHand(hand).getInteractAnimation(), true);
         //?} else
         //player.swing(hand, true);
      }
   }
}
