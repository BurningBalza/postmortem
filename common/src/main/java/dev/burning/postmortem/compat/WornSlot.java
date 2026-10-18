package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Where a {@link WornItem} was worn, kept with a memorial's stored stack so it can be put back in the same slot. */
public record WornSlot(String mod, String slot, int index, boolean cosmetic) {
   public static final Codec<WornSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("Mod").forGetter(WornSlot::mod),
      Codec.STRING.fieldOf("Slot").forGetter(WornSlot::slot),
      Codec.INT.fieldOf("Index").forGetter(WornSlot::index),
      Codec.BOOL.optionalFieldOf("Cosmetic", false).forGetter(WornSlot::cosmetic)
   ).apply(instance, WornSlot::new));
}
