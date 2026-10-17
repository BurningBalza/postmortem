package dev.burning.postmortem.neoforge;

import dev.burning.postmortem.platform.ClientPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?} else
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeClientPlatform implements ClientPlatform {
   @Override
   public void sendToServer(CustomPacketPayload payload) {
      //? if >=1.21.11 {
      /*ClientPacketDistributor.sendToServer(payload);
      *///?} else
      PacketDistributor.sendToServer(payload);
   }
}
