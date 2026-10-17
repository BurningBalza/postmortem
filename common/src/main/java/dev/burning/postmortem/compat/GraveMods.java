package dev.burning.postmortem.compat;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Whether another mod already keeps players' items on death. When one is loaded Postmortem steps aside: deaths are left
 * to it, and only cosmetic statues remain. The ids live in {@code postmortem/grave_mods.txt}, which the build also reads.
 */
public final class GraveMods {
   private static final String LIST = "/postmortem/grave_mods.txt";

   private static boolean resolved;
   private static String loaded;

   private GraveMods() {
   }

   /** True when a grave mod is loaded, so memorial statues and the statue collector are off. */
   public static boolean present() {
      return resolve() != null;
   }

   private static String resolve() {
      if (resolved) {
         return loaded;
      }
      resolved = true;

      for (String id : read()) {
         if (Platform.INSTANCE.isModLoaded(id)) {
            loaded = id;
            Postmortem.LOGGER.info("[postmortem] {} handles deaths; memorial statues and the statue collector are off", id);
            break;
         }
      }
      return loaded;
   }

   private static List<String> read() {
      try (InputStream in = GraveMods.class.getResourceAsStream(LIST)) {
         if (in == null) {
            Postmortem.LOGGER.warn("[postmortem] {} is missing; no grave mods will be detected", LIST);
            return List.of();
         }
         return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .toList();
      } catch (IOException e) {
         Postmortem.LOGGER.warn("[postmortem] could not read {}; no grave mods will be detected", LIST, e);
         return List.of();
      }
   }
}
