package dev.burning.postmortem.fabric;

//? if fabric {
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.client.PostmortemClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class PostmortemFabricClient implements ClientModInitializer {
   @Override
   @SuppressWarnings("deprecation")
   public void onInitializeClient() {
      EntityRendererRegistry.register(PostmortemRegistry.MEMORIAL_STATUE.get(), PostmortemClient::memorialRenderer);
      EntityRendererRegistry.register(PostmortemRegistry.COSMETIC_STATUE.get(), PostmortemClient::cosmeticRenderer);

      ClientPlayNetworking.registerGlobalReceiver(
         PostmortemRegistry.OpenPoseScreen.ID,
         (payload, context) -> PostmortemClient.openPoseScreen(context.client(), payload.entityId()));
   }
}
//?}
