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
import net.minecraft.world.level.saveddata.SavedDataType;

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

   // Fabric API lets a null DataFixTypes through; there is no vanilla fixer for mod data anyway.
   private static final SavedDataType<MemorialRegistry> TYPE =
      new SavedDataType<>(Postmortem.id("memorials"), MemorialRegistry::new, CODEC, null);

   private final Map<UUID, Entry> entries;

   private MemorialRegistry() {
      this(Map.of());
   }

   private MemorialRegistry(Map<UUID, Entry> entries) {
      this.entries = new HashMap<>(entries);
   }

   public static MemorialRegistry get(MinecraftServer server) {
      return server.getDataStorage().computeIfAbsent(TYPE);
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
