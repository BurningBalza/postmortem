package dev.burning.postmortem.mixin;

import dev.burning.postmortem.death.OwnerAccess;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 900)
public abstract class ServerGamePacketListenerMixin {
   @Shadow
   public ServerPlayer player;

   @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
   private void postmortem$ownerUsesMemorial(ServerboundInteractPacket packet, CallbackInfo ci) {
      // The packet first arrives on the network thread and is queued again for the server thread; act on the second.
      if (this.player.level().getServer().isSameThread() && OwnerAccess.handle(this.player, packet)) {
         ci.cancel();
      }
   }
}
