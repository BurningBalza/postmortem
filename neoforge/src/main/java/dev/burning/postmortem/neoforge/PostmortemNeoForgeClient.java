package dev.burning.postmortem.neoforge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.client.PostmortemClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = Postmortem.MOD_ID, dist = Dist.CLIENT)
public final class PostmortemNeoForgeClient {
   public PostmortemNeoForgeClient(IEventBus modBus) {
      modBus.addListener(PostmortemNeoForgeClient::registerRenderers);
      modBus.addListener(PostmortemNeoForgeClient::registerPayloadHandlers);
   }

   private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(PostmortemRegistry.MEMORIAL_STATUE.get(), PostmortemClient::memorialRenderer);
      event.registerEntityRenderer(PostmortemRegistry.COSMETIC_STATUE.get(), PostmortemClient::cosmeticRenderer);
   }

   private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
      event.register(
         PostmortemRegistry.OpenPoseScreen.ID,
         (payload, context) -> PostmortemClient.openPoseScreen(Minecraft.getInstance(), payload.entityId()));
   }
}
