package dev.burning.postmortem.fabric;

import dev.burning.postmortem.platform.ClientPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientPlatform implements ClientPlatform {
   @Override
   public void sendToServer(CustomPacketPayload payload) {
      //? if >=1.20.5 {
      ClientPlayNetworking.send(payload);
      //?} else {
      /*net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
      payload.write(buf);
      ClientPlayNetworking.send(payload.id(), buf);
      *///?}
   }
}
