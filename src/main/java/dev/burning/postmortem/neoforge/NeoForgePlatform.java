package dev.burning.postmortem.neoforge;

//? if neoforge {
/*import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.platform.Platform;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NeoForgePlatform implements Platform {
   // One DeferredRegister per registry, in the order first used; attached to the mod bus by attach().
   private final Map<Registry<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();
   private final DeferredRegister<EntityDataSerializer<?>> serializers =
      DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, Postmortem.MOD_ID);

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
      DeferredRegister<R> register =
         (DeferredRegister<R>) this.registers.computeIfAbsent(registry, key -> DeferredRegister.create(registry, Postmortem.MOD_ID));
      return register.register(name, factory);
   }

   @Override
   public void registerDataSerializer(String name, EntityDataSerializer<?> serializer) {
      this.serializers.register(name, () -> serializer);
   }

   @Override
   public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
      PacketDistributor.sendToPlayer(player, payload);
   }

   void attach(IEventBus modBus) {
      this.registers.values().forEach(register -> register.register(modBus));
      this.serializers.register(modBus);
   }
}
*///?}
