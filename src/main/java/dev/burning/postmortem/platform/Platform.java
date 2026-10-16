package dev.burning.postmortem.platform;

import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The few things common code needs from the mod loader. Everything else a loader does - entrypoints, events,
 * payload registration - is wired up in the loader's own package and calls into common code.
 */
public interface Platform {
   Platform INSTANCE =
      //? if fabric {
      new dev.burning.postmortem.fabric.FabricPlatform();
      //?} elif neoforge {
      /*new dev.burning.postmortem.neoforge.NeoForgePlatform();
      *///?}

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
