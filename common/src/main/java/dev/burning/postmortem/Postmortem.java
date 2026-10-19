package dev.burning.postmortem;

import dev.burning.postmortem.compat.GraveMods;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornSlots;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Postmortem {
   public static final String MOD_ID = "postmortem";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   private Postmortem() {
   }

   public static Identifier id(String path) {
      //? if >=1.21 {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
      //?} else
      //return new Identifier(MOD_ID, path);
   }

   public static void overlay(Player player, Component message) {
      //? if >=26.1 {
      /*player.sendOverlayMessage(message);
      *///?} else
      player.displayClientMessage(message, true);
   }

   public static void init() {
      PostmortemConfig.load();
      PostmortemRegistry.init();
      GraveMods.present();
      WornSlots.loaded();
      TrinketsCompat.present();
   }
}
