package dev.burning.postmortem.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
//? if >=1.20.2 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else
//import dev.burning.postmortem.legacy.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The few things common code needs from the mod loader. Everything else a loader does - entrypoints, events,
 * payload registration - is wired up in the loader's own package and calls into common code.
 */
public interface Platform {
   // Each loader folder names its implementation in META-INF/services.
   Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst().orElseThrow();

   Path configDir();

   boolean isModLoaded(String id);

   /**
    * Registers {@code factory}'s value under {@code postmortem:name}. Fabric registers immediately; NeoForge defers until
    * its registry event, so the returned supplier must not be read before registration finishes.
    */
   <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory);

   void registerDataSerializer(String name, EntityDataSerializer<?> serializer);

   void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
