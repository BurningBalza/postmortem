package dev.burning.postmortem.client;

import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.entity.CosmeticStatueEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.Entity;

public class PostmortemClient implements ClientModInitializer {

   @Override
   @SuppressWarnings("deprecation")
   public void onInitializeClient() {
      EntityRendererRegistry.register(
         PostmortemRegistry.MEMORIAL_STATUE,
         context -> new StatueEntityRenderer<>(
            context, StatueEntityRenderer.MEMORIAL_TEXTURE, StatueEntityRenderer.MEMORIAL_EYES_TEXTURE));
      EntityRendererRegistry.register(
         PostmortemRegistry.COSMETIC_STATUE,
         context -> new StatueEntityRenderer<>(context, StatueEntityRenderer.COSMETIC_TEXTURE));

      ClientPlayNetworking.registerGlobalReceiver(PostmortemRegistry.OpenPoseScreen.ID, (payload, context) -> {
         Entity entity = context.player().level().getEntity(payload.entityId());
         if (entity instanceof CosmeticStatueEntity statue) {
            context.client().setScreenAndShow(new StatuePoseScreen(statue));
         }
      });
   }
}
