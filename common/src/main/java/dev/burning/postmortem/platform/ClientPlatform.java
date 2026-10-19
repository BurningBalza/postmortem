package dev.burning.postmortem.platform;

import java.util.ServiceLoader;
//? if >=1.20.2 {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?} else
import dev.burning.postmortem.legacy.CustomPacketPayload;

public interface ClientPlatform {
   ClientPlatform INSTANCE =
      ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst().orElseThrow();

   void sendToServer(CustomPacketPayload payload);
}
