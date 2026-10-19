package dev.burning.postmortem.forge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.client.PostmortemClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

final class ForgeNetwork {
   private static final String VERSION = "1";

   static final SimpleChannel CHANNEL =
      NetworkRegistry.newSimpleChannel(Postmortem.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

   private ForgeNetwork() {
   }

   static void register() {
      CHANNEL.messageBuilder(PostmortemRegistry.OpenPoseScreen.class, 0, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(PostmortemRegistry.OpenPoseScreen::write)
         .decoder(buf -> PostmortemRegistry.OpenPoseScreen.CODEC.decode(buf))
         .consumerMainThread((payload, context) -> PostmortemClient.openPoseScreen(Minecraft.getInstance(), payload.entityId()))
         .add();
      CHANNEL.messageBuilder(PostmortemRegistry.SetPose.class, 1, NetworkDirection.PLAY_TO_SERVER)
         .encoder(PostmortemRegistry.SetPose::write)
         .decoder(buf -> PostmortemRegistry.SetPose.CODEC.decode(buf))
         .consumerMainThread((payload, context) -> {
            if (context.get().getSender() != null) {
               PostmortemRegistry.applyPose(context.get().getSender(), payload);
            }
         })
         .add();
   }
}
