package dev.burning.postmortem.platform;

import java.util.ServiceLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-only counterpart of {@link Platform}, kept separate so a dedicated server never loads client classes. */
public interface ClientPlatform {
   ClientPlatform INSTANCE =
      ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst().orElseThrow();

   void sendToServer(CustomPacketPayload payload);
}
