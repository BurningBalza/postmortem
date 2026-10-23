package dev.burning.postmortem.platform;

import java.nio.file.Path;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
//? if >=1.20.2 {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?} else
import dev.burning.postmortem.legacy.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public interface Platform {
   Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst().orElseThrow();

   Path configDir();

   boolean isModLoaded(String id);

   <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory);

   void registerDataSerializer(String name, EntityDataSerializer<?> serializer);

   void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

   Collection<ItemEntity> capturedDrops(Entity entity);
}
