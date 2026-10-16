package dev.burning.postmortem.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-only counterpart of {@link Platform}, kept separate so a dedicated server never loads client classes. */
public interface ClientPlatform {
   ClientPlatform INSTANCE =
      //? if fabric {
      new dev.burning.postmortem.fabric.FabricClientPlatform();
      //?} elif neoforge {
      /*new dev.burning.postmortem.neoforge.NeoForgeClientPlatform();
      *///?}

   void sendToServer(CustomPacketPayload payload);
}
