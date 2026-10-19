package dev.burning.postmortem.client;

import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;

public final class PostmortemClient {
   private PostmortemClient() {
   }

   public static StatueEntityRenderer<MemorialStatueEntity> memorialRenderer(EntityRendererProvider.Context context) {
      return new StatueEntityRenderer<>(
         context, StatueEntityRenderer.MEMORIAL_TEXTURE, StatueEntityRenderer.MEMORIAL_EYES_TEXTURE);
   }

   public static StatueEntityRenderer<CosmeticStatueEntity> cosmeticRenderer(EntityRendererProvider.Context context) {
      return new StatueEntityRenderer<>(context, StatueEntityRenderer.COSMETIC_TEXTURE);
   }

   public static void openPoseScreen(Minecraft client, int entityId) {
      if (client.player == null) {
         return;
      }

      Entity entity = client.player.level().getEntity(entityId);
      if (entity instanceof CosmeticStatueEntity statue) {
         //? if >=1.21.9 {
         /*client.setScreenAndShow(new StatuePoseScreen(statue));
         *///?} else
         client.setScreen(new StatuePoseScreen(statue));
      }
   }
}
