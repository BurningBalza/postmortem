package dev.burning.postmortem.neoforge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemCommand;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.client.PostmortemClient;
import dev.burning.postmortem.death.DeathCapture;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Postmortem.MOD_ID)
public final class PostmortemNeoForge {
   public PostmortemNeoForge(IEventBus modBus) {
      Postmortem.init();
      ((NeoForgePlatform) Platform.INSTANCE).attach(modBus);

      modBus.addListener(PostmortemNeoForge::registerAttributes);
      modBus.addListener(PostmortemNeoForge::fillCreativeTabs);
      modBus.addListener(PostmortemNeoForge::registerPayloads);

      NeoForge.EVENT_BUS.addListener(PostmortemNeoForge::carryKeptItems);
      NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PostmortemCommand.register(event.getDispatcher()));
   }

   private static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(PostmortemRegistry.MEMORIAL_STATUE.get(), StatueEntity.createStatueAttributes().build());
      event.put(PostmortemRegistry.COSMETIC_STATUE.get(), StatueEntity.createStatueAttributes().build());
   }

   private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
      CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
      if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
         //? if >=1.21 {
         /*event.insertAfter(new ItemStack(Items.ARMOR_STAND), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()), visibility);
         *///?} else
         event.getEntries().putAfter(new ItemStack(Items.ARMOR_STAND), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()), visibility);
      } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
         //? if >=1.21 {
         /*event.insertAfter(new ItemStack(Items.RECOVERY_COMPASS), new ItemStack(PostmortemRegistry.STATUE_COLLECTOR.get()), visibility);
         *///?} else
         event.getEntries().putAfter(new ItemStack(Items.RECOVERY_COMPASS), new ItemStack(PostmortemRegistry.STATUE_COLLECTOR.get()), visibility);
      }
   }

   private static void registerPayloads(RegisterPayloadHandlersEvent event) {
      //? if >=1.21.11 {
      /*// The client handler for OpenPoseScreen is attached in PostmortemNeoForgeClient.
      event.registrar("1")
         .playToClient(PostmortemRegistry.OpenPoseScreen.ID, PostmortemRegistry.OpenPoseScreen.CODEC)
      *///?} else {
      // Only the client receives this, so the handler's client classes never load on a dedicated server.
      event.registrar("1")
         .playToClient(
            PostmortemRegistry.OpenPoseScreen.ID,
            PostmortemRegistry.OpenPoseScreen.CODEC,
            (payload, context) -> PostmortemClient.openPoseScreen(net.minecraft.client.Minecraft.getInstance(), payload.entityId()))
      //?}
         .playToServer(
            PostmortemRegistry.SetPose.ID,
            PostmortemRegistry.SetPose.CODEC,
            (payload, context) -> PostmortemRegistry.applyPose((ServerPlayer) context.player(), payload));
   }

   private static void carryKeptItems(PlayerEvent.Clone event) {
      if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
         DeathCapture.carryKeptItems(oldPlayer, newPlayer, !event.isWasDeath());
      }
   }
}
