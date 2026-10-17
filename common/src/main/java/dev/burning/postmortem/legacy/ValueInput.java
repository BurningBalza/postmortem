package dev.burning.postmortem.legacy;

//? if <1.21.6 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

// Read-side counterpart of ValueOutput: the part of 1.21.6's ValueInput that Postmortem uses, over a CompoundTag.
public final class ValueInput {
   private final CompoundTag tag;
   private final DynamicOps<Tag> ops;

   private ValueInput(CompoundTag tag, DynamicOps<Tag> ops) {
      this.tag = tag;
      this.ops = ops;
   }

   public static ValueInput of(CompoundTag tag, HolderLookup.Provider registries) {
      return new ValueInput(tag, RegistryOps.create(NbtOps.INSTANCE, registries));
   }

   public <T> Optional<T> read(String key, Codec<T> codec) {
      Tag value = this.tag.get(key);
      return value == null ? Optional.empty() : codec.parse(this.ops, value).result();
   }

   //? if >=1.21.5 {
   public float getFloatOr(String key, float fallback) {
      return this.tag.getFloatOr(key, fallback);
   }

   public long getLongOr(String key, long fallback) {
      return this.tag.getLongOr(key, fallback);
   }

   public String getStringOr(String key, String fallback) {
      return this.tag.getStringOr(key, fallback);
   }

   public boolean getBooleanOr(String key, boolean fallback) {
      return this.tag.getBooleanOr(key, fallback);
   }
   //?} else {
   /*public float getFloatOr(String key, float fallback) {
      return this.tag.contains(key) ? this.tag.getFloat(key) : fallback;
   }

   public long getLongOr(String key, long fallback) {
      return this.tag.contains(key) ? this.tag.getLong(key) : fallback;
   }

   public String getStringOr(String key, String fallback) {
      return this.tag.contains(key) ? this.tag.getString(key) : fallback;
   }

   public boolean getBooleanOr(String key, boolean fallback) {
      return this.tag.contains(key) ? this.tag.getBoolean(key) : fallback;
   }
   *///?}
}
//?}
