package dev.burning.postmortem.platform;

import java.util.ServiceLoader;
//? if >=1.20.2 {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?} else
import dev.burning.postmortem.legacy.CustomPacketPayload;

/** Client-only counterpart of {@link Platform}, kept separate so a dedicated server never loads client classes. */
public interface ClientPlatform {
   ClientPlatform INSTANCE =
      ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst().orElseThrow();

   void sendToServer(CustomPacketPayload payload);
}
