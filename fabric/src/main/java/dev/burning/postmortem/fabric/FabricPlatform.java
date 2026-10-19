package dev.burning.postmortem.fabric;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
*///?} elif >=1.21.5 {
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
//?} else
//import net.minecraft.network.syncher.EntityDataSerializers;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
//? if >=1.20.2 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else
//import dev.burning.postmortem.legacy.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
   @Override
   public Path configDir() {
      return FabricLoader.getInstance().getConfigDir();
   }

   @Override
   public boolean isModLoaded(String id) {
      return FabricLoader.getInstance().isModLoaded(id);
   }

   @Override
   public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
      T value = Registry.register(registry, Postmortem.id(name), factory.get());
      return () -> value;
   }

   @Override
   public void registerDataSerializer(String name, EntityDataSerializer<?> serializer) {
      //? if >=26.1 {
      /*FabricEntityDataRegistry.register(Postmortem.id(name), serializer);
      *///?} elif >=1.21.5 {
      FabricTrackedDataRegistry.register(Postmortem.id(name), serializer);
      //?} else
      //EntityDataSerializers.registerSerializer(serializer);
   }

   @Override
   public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      //? if >=1.20.5 {
      ServerPlayNetworking.send(player, payload);
      //?} else {
      /*
      net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
      payload.write(buf);
      ServerPlayNetworking.send(player, payload.id(), buf);
      *///?}
   }
}
