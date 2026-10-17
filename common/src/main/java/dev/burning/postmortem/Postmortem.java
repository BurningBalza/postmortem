package dev.burning.postmortem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-independent setup. Each loader's entrypoint calls {@link #init()} and then wires its own events. */
public final class Postmortem {
   public static final String MOD_ID = "postmortem";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   private Postmortem() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }

   /** Shows {@code message} above the hotbar. */
   public static void overlay(Player player, Component message) {
      //? if >=26.1 {
      player.sendOverlayMessage(message);
      //?} else
      //player.displayClientMessage(message, true);
   }

   public static void init() {
      PostmortemConfig.load();
      PostmortemRegistry.init();
   }
}
