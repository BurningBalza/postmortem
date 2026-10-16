package dev.burning.postmortem;

import dev.burning.postmortem.death.DeathCapture;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Postmortem implements ModInitializer {
   public static final String MOD_ID = "postmortem";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }

   @Override
   public void onInitialize() {
      PostmortemConfig.load();
      PostmortemRegistry.init();
      DeathCapture.init();
      PostmortemCommand.init();
   }
}
