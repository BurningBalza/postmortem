package dev.burning.postmortem;

import net.minecraft.resources.Identifier;
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

   public static void init() {
      PostmortemConfig.load();
      PostmortemRegistry.init();
   }
}
