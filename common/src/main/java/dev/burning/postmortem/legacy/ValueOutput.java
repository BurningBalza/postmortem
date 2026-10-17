package dev.burning.postmortem.legacy;

//? if <1.21.6 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

// Before 1.21.6 entities saved straight into a CompoundTag. This mirrors the part of 1.21.6's ValueOutput that
// Postmortem uses, so the statues' save code is the same on every version.
public final class ValueOutput {
   private final CompoundTag tag;
   private final DynamicOps<Tag> ops;

   private ValueOutput(CompoundTag tag, DynamicOps<Tag> ops) {
      this.tag = tag;
      this.ops = ops;
   }

   public static ValueOutput of(CompoundTag tag, net.minecraft.world.entity.Entity entity) {
      //? if >=1.20.5 {
      /*return new ValueOutput(tag, RegistryOps.create(NbtOps.INSTANCE, entity.registryAccess()));
      *///?} else
      return new ValueOutput(tag, NbtOps.INSTANCE);
   }

   public <T> void store(String key, Codec<T> codec, T value) {
      codec.encodeStart(this.ops, value).result().ifPresent(encoded -> this.tag.put(key, encoded));
   }

   public void putFloat(String key, float value) {
      this.tag.putFloat(key, value);
   }

   public void putLong(String key, long value) {
      this.tag.putLong(key, value);
   }

   public void putString(String key, String value) {
      this.tag.putString(key, value);
   }

   public void putBoolean(String key, boolean value) {
      this.tag.putBoolean(key, value);
   }
}
//?}
