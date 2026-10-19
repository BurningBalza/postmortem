package dev.burning.postmortem.forge;

import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.client.PostmortemClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class PostmortemForgeClient {
   private PostmortemForgeClient() {
   }

   static void init(IEventBus modBus) {
      modBus.addListener(PostmortemForgeClient::registerRenderers);
   }

   private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(PostmortemRegistry.MEMORIAL_STATUE.get(), PostmortemClient::memorialRenderer);
      event.registerEntityRenderer(PostmortemRegistry.COSMETIC_STATUE.get(), PostmortemClient::cosmeticRenderer);
   }
}
