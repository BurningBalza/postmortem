package dev.burning.postmortem.forge;

import dev.burning.postmortem.legacy.CustomPacketPayload;
import dev.burning.postmortem.platform.ClientPlatform;

public final class ForgeClientPlatform implements ClientPlatform {
   @Override
   public void sendToServer(CustomPacketPayload payload) {
      ForgeNetwork.CHANNEL.sendToServer(payload);
   }
}
