package dev.burning.postmortem.neoforge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemCommand;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.GraveMods;
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
//? if >=1.20.5 {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
*///?}

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

      //? if <1.20.5 {
      /*
      if (FMLEnvironment.dist == Dist.CLIENT) {
         new PostmortemNeoForgeClient(modBus);
      }
      *///?}
   }

   private static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(PostmortemRegistry.MEMORIAL_STATUE.get(), StatueEntity.createStatueAttributes().build());
      event.put(PostmortemRegistry.COSMETIC_STATUE.get(), StatueEntity.createStatueAttributes().build());
   }

   private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
      CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
      if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
         //? if >=1.21 {
         event.insertAfter(new ItemStack(Items.ARMOR_STAND), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()), visibility);
         //?} else
         //event.getEntries().putAfter(new ItemStack(Items.ARMOR_STAND), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()), visibility);
      } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES && !GraveMods.present()) {
         //? if >=1.21 {
         event.insertAfter(new ItemStack(Items.RECOVERY_COMPASS), new ItemStack(PostmortemRegistry.STATUE_COLLECTOR.get()), visibility);
         //?} else
         //event.getEntries().putAfter(new ItemStack(Items.RECOVERY_COMPASS), new ItemStack(PostmortemRegistry.STATUE_COLLECTOR.get()), visibility);
      }
   }

   //? if >=1.20.5 {
   private static void registerPayloads(RegisterPayloadHandlersEvent event) {
      //? if >=1.21.11 {
      event.registrar("1")
         .playToClient(PostmortemRegistry.OpenPoseScreen.ID, PostmortemRegistry.OpenPoseScreen.CODEC)
      //?} else {
      /*
      event.registrar("1")
         .playToClient(
            PostmortemRegistry.OpenPoseScreen.ID,
            PostmortemRegistry.OpenPoseScreen.CODEC,
            (payload, context) -> PostmortemClient.openPoseScreen(net.minecraft.client.Minecraft.getInstance(), payload.entityId()))
      *///?}
         .playToServer(
            PostmortemRegistry.SetPose.ID,
            PostmortemRegistry.SetPose.CODEC,
            (payload, context) -> PostmortemRegistry.applyPose((ServerPlayer) context.player(), payload));
   }
   //?} else {
   /*
   private static void registerPayloads(RegisterPayloadHandlerEvent event) {
      event.registrar(Postmortem.MOD_ID).versioned("1")
         .play(PostmortemRegistry.OpenPoseScreen.ID, buf -> PostmortemRegistry.OpenPoseScreen.CODEC.decode(buf), handler -> handler
            .client((payload, context) -> context.workHandler().execute(
               () -> PostmortemClient.openPoseScreen(net.minecraft.client.Minecraft.getInstance(), payload.entityId()))))
         .play(PostmortemRegistry.SetPose.ID, buf -> PostmortemRegistry.SetPose.CODEC.decode(buf), handler -> handler
            .server((payload, context) -> context.workHandler().execute(() -> context.player().ifPresent(
               player -> PostmortemRegistry.applyPose((ServerPlayer) player, payload)))));
   }
   *///?}

   private static void carryKeptItems(PlayerEvent.Clone event) {
      if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
         DeathCapture.carryKeptItems(oldPlayer, newPlayer, !event.isWasDeath());
      }
   }
}
