package dev.burning.postmortem.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.entity.MemorialStatueEntity.StoredStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.5 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
*///?}

/**
 * Holds the items of every memorial statue, keyed by the statue's UUID. The entity itself stores nothing, so a statue
 * lost to a clear-lag mod, /kill or a corrupted chunk leaves its items here to be recovered. An entry only goes away
 * when its items are handed back, spilled or deliberately deleted.
 */
public final class MemorialRegistry extends SavedData {
   public record Entry(UUID owner, ResourceKey<Level> dimension, BlockPos pos, long createdAt, List<StoredStack> stacks) {
      public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         UUIDUtil.CODEC.fieldOf("Owner").forGetter(Entry::owner),
         Level.RESOURCE_KEY_CODEC.fieldOf("Dimension").forGetter(Entry::dimension),
         BlockPos.CODEC.fieldOf("Pos").forGetter(Entry::pos),
         Codec.LONG.fieldOf("CreatedAt").forGetter(Entry::createdAt),
         StoredStack.CODEC.listOf().fieldOf("Stored").forGetter(Entry::stacks)
      ).apply(instance, Entry::new));

      public Entry {
         stacks = List.copyOf(stacks);
      }

      public Entry movedTo(ResourceKey<Level> dimension, BlockPos pos) {
         return new Entry(this.owner, dimension, pos, this.createdAt, this.stacks);
      }
   }

   private static final Codec<MemorialRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC).fieldOf("Memorials").forGetter(registry -> registry.entries)
   ).apply(instance, MemorialRegistry::new));

   // A null DataFixTypes is let through; there is no vanilla fixer for mod data anyway.
   //? if >=1.21.5 {
   private static final SavedDataType<MemorialRegistry> TYPE =
      //? if >=26.1 {
      /*new SavedDataType<>(Postmortem.id("memorials"), MemorialRegistry::new, CODEC, null);
      *///?} else
      new SavedDataType<>("postmortem_memorials", MemorialRegistry::new, CODEC, null);
   //?} else {
   /*private static final String FILE = "postmortem_memorials";
   //? if >=1.20.2 {
   private static final SavedData.Factory<MemorialRegistry> TYPE =
      new SavedData.Factory<>(MemorialRegistry::new, MemorialRegistry::load, null);
   //?}

   //? if >=1.20.5 {
   private static MemorialRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
      return CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag).result().orElseGet(MemorialRegistry::new);
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), this).result()
         .ifPresent(encoded -> tag.merge((CompoundTag) encoded));
      return tag;
   }
   //?} else {
   /^private static MemorialRegistry load(CompoundTag tag) {
      return CODEC.parse(NbtOps.INSTANCE, tag).result().orElseGet(MemorialRegistry::new);
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CODEC.encodeStart(NbtOps.INSTANCE, this).result().ifPresent(encoded -> tag.merge((CompoundTag) encoded));
      return tag;
   }
   ^///?}
   *///?}

   private final Map<UUID, Entry> entries;

   private MemorialRegistry() {
      this(Map.of());
   }

   private MemorialRegistry(Map<UUID, Entry> entries) {
      this.entries = new HashMap<>(entries);
   }

   public static MemorialRegistry get(MinecraftServer server) {
      //? if >=26.1 {
      /*return server.getDataStorage().computeIfAbsent(TYPE);
      *///?} elif >=1.21.5 {
      return server.overworld().getDataStorage().computeIfAbsent(TYPE);
      //?} elif >=1.20.2 {
      /*return server.overworld().getDataStorage().computeIfAbsent(TYPE, FILE);
      *///?} else
      //return server.overworld().getDataStorage().computeIfAbsent(MemorialRegistry::load, MemorialRegistry::new, FILE);
   }

   public Entry get(UUID statue) {
      return this.entries.get(statue);
   }

   public void put(UUID statue, Entry entry) {
      this.entries.put(statue, entry);
      this.setDirty();
   }

   /** Removes the entry and returns its stacks, or an empty list if there was none. */
   public List<StoredStack> take(UUID statue) {
      Entry entry = this.entries.remove(statue);
      if (entry == null) {
         return List.of();
      }

      this.setDirty();
      return entry.stacks();
   }

   public Map<UUID, Entry> ownedBy(UUID owner) {
      Map<UUID, Entry> owned = new HashMap<>();
      this.entries.forEach((statue, entry) -> {
         if (entry.owner().equals(owner)) {
            owned.put(statue, entry);
         }
      });
      return owned;
   }
}
