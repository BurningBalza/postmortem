package dev.burning.postmortem.forge;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.legacy.CustomPacketPayload;
import dev.burning.postmortem.platform.Platform;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgePlatform implements Platform {
   private final Map<Registry<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();
   private final DeferredRegister<EntityDataSerializer<?>> serializers =
      DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Postmortem.MOD_ID);

   @Override
   public Path configDir() {
      return FMLPaths.CONFIGDIR.get();
   }

   @Override
   public boolean isModLoaded(String id) {
      return ModList.get().isLoaded(id);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
      DeferredRegister<R> register = (DeferredRegister<R>) this.registers.computeIfAbsent(
         registry, key -> DeferredRegister.create(registry.key(), Postmortem.MOD_ID));
      return register.register(name, factory);
   }

   @Override
   public void registerDataSerializer(String name, EntityDataSerializer<?> serializer) {
      this.serializers.register(name, () -> serializer);
   }

   @Override
   public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
   }

   @Override
   public Collection<ItemEntity> capturedDrops(Entity entity) {
      return entity.captureDrops();
   }

   void attach(IEventBus modBus) {
      this.registers.values().forEach(register -> register.register(modBus));
      this.serializers.register(modBus);
   }
}
