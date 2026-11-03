package dev.burning.postmortem.forge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemCommand;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.death.DeathCapture;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Postmortem.MOD_ID)
public final class PostmortemForge {
   public PostmortemForge() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
      Postmortem.init();
      ((ForgePlatform) Platform.INSTANCE).attach(modBus);
      ForgeNetwork.register();

      modBus.addListener(PostmortemForge::registerAttributes);
      modBus.addListener(PostmortemForge::fillCreativeTabs);

      MinecraftForge.EVENT_BUS.addListener(PostmortemForge::carryKeptItems);
      MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PostmortemCommand.register(event.getDispatcher()));

      if (FMLEnvironment.dist == Dist.CLIENT) {
         PostmortemForgeClient.init(modBus);
      }
   }

   private static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(PostmortemRegistry.MEMORIAL_STATUE.get(), StatueEntity.createStatueAttributes().build());
      event.put(PostmortemRegistry.COSMETIC_STATUE.get(), StatueEntity.createStatueAttributes().build());
   }

   private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
      CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
      if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
         event.getEntries().putAfter(new ItemStack(Items.ARMOR_STAND), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()), visibility);
      }
   }

   private static void carryKeptItems(PlayerEvent.Clone event) {
      if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
         DeathCapture.carryKeptItems(oldPlayer, newPlayer, !event.isWasDeath());
      }
   }
}
