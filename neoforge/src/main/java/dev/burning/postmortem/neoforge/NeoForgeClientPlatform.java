package dev.burning.postmortem.neoforge;

import dev.burning.postmortem.platform.ClientPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class NeoForgeClientPlatform implements ClientPlatform {
   @Override
   public void sendToServer(CustomPacketPayload payload) {
      ClientPacketDistributor.sendToServer(payload);
   }
}
