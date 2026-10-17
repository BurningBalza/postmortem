package dev.burning.postmortem.fabric;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemCommand;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.death.DeathCapture;
import dev.burning.postmortem.entity.StatueEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//?} else
//import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//? if >=1.20.5
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public final class PostmortemFabric implements ModInitializer {
   @Override
   public void onInitialize() {
      Postmortem.init();

      FabricDefaultAttributeRegistry.register(PostmortemRegistry.MEMORIAL_STATUE.get(), StatueEntity.createStatueAttributes());
      FabricDefaultAttributeRegistry.register(PostmortemRegistry.COSMETIC_STATUE.get(), StatueEntity.createStatueAttributes());

      //? if >=26.1 {
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
         .register(entries -> entries.insertAfter(Items.ARMOR_STAND, PostmortemRegistry.COSMETIC_STATUE_ITEM.get()));
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
         .register(entries -> entries.insertAfter(Items.RECOVERY_COMPASS, PostmortemRegistry.STATUE_COLLECTOR.get()));

      PayloadTypeRegistry.clientboundPlay().register(PostmortemRegistry.OpenPoseScreen.ID, PostmortemRegistry.OpenPoseScreen.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(PostmortemRegistry.SetPose.ID, PostmortemRegistry.SetPose.CODEC);
      //?} else {
      /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
         .register(entries -> entries.addAfter(Items.ARMOR_STAND, PostmortemRegistry.COSMETIC_STATUE_ITEM.get()));
      ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
         .register(entries -> entries.addAfter(Items.RECOVERY_COMPASS, PostmortemRegistry.STATUE_COLLECTOR.get()));

      //? if >=1.20.5 {
      PayloadTypeRegistry.playS2C().register(PostmortemRegistry.OpenPoseScreen.ID, PostmortemRegistry.OpenPoseScreen.CODEC);
      PayloadTypeRegistry.playC2S().register(PostmortemRegistry.SetPose.ID, PostmortemRegistry.SetPose.CODEC);
      //?}
      *///?}
      //? if >=1.20.5 {
      ServerPlayNetworking.registerGlobalReceiver(
         PostmortemRegistry.SetPose.ID, (payload, context) -> PostmortemRegistry.applyPose(context.player(), payload));
      //?} else {
      /*// Raw channels before 1.20.5: decoded on the network thread, applied on the server's.
      ServerPlayNetworking.registerGlobalReceiver(PostmortemRegistry.SetPose.ID, (server, player, handler, buf, sender) -> {
         PostmortemRegistry.SetPose payload = PostmortemRegistry.SetPose.CODEC.decode(buf);
         server.execute(() -> PostmortemRegistry.applyPose(player, payload));
      });
      *///?}

      ServerPlayerEvents.COPY_FROM.register(DeathCapture::carryKeptItems);
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PostmortemCommand.register(dispatcher));
   }
}
