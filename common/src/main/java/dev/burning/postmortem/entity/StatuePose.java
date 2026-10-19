package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
//? if >=1.20.5 {
/*import net.minecraft.network.codec.StreamCodec;
*///?} else
import dev.burning.postmortem.legacy.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.util.StringRepresentable;

public record StatuePose(Map<StatuePart, LimbTransform> parts) {
   private static final StatuePart[] PARTS = StatuePart.values();

   public static final StatuePose DEFAULT = new StatuePose(Map.of());

   public static final Codec<StatuePose> CODEC = Codec.simpleMap(
         StatuePart.CODEC, LimbTransform.CODEC, StringRepresentable.keys(PARTS))
      .codec()
      .xmap(StatuePose::new, StatuePose::parts);

   public static final StreamCodec<ByteBuf, StatuePose> PACKET_CODEC = StreamCodec.of(
      (buf, pose) -> {
         for (StatuePart part : PARTS) {
            LimbTransform.PACKET_CODEC.encode(buf, pose.get(part));
         }
      },
      buf -> {
         Map<StatuePart, LimbTransform> parts = new EnumMap<>(StatuePart.class);
         for (StatuePart part : PARTS) {
            parts.put(part, LimbTransform.PACKET_CODEC.decode(buf));
         }
         return new StatuePose(parts);
      });

   //? if >=1.20.5 {
   /*public static final EntityDataSerializer<StatuePose> TRACKED_HANDLER = EntityDataSerializer.forValueType(PACKET_CODEC);
   *///?} else
   public static final EntityDataSerializer<StatuePose> TRACKED_HANDLER = EntityDataSerializer.simple(PACKET_CODEC::encode, PACKET_CODEC::decode);

   public StatuePose {
      Map<StatuePart, LimbTransform> kept = new EnumMap<>(StatuePart.class);
      parts.forEach((part, transform) -> {
         if (!transform.isNone()) {
            kept.put(part, transform);
         }
      });
      parts = Collections.unmodifiableMap(kept);
   }

   public boolean isDefault() {
      return this.parts.isEmpty();
   }

   public LimbTransform get(StatuePart part) {
      return this.parts.getOrDefault(part, LimbTransform.NONE);
   }

   public StatuePose with(StatuePart part, LimbTransform transform) {
      Map<StatuePart, LimbTransform> parts = new EnumMap<>(StatuePart.class);
      parts.putAll(this.parts);
      parts.put(part, transform);
      return new StatuePose(parts);
   }

   public StatuePose retain(java.util.function.Predicate<StatuePart> keep) {
      Map<StatuePart, LimbTransform> parts = new EnumMap<>(StatuePart.class);
      this.parts.forEach((part, transform) -> {
         if (keep.test(part)) {
            parts.put(part, transform);
         }
      });
      return new StatuePose(parts);
   }

   public StatuePose clamped() {
      Map<StatuePart, LimbTransform> parts = new EnumMap<>(StatuePart.class);
      this.parts.forEach((part, transform) -> parts.put(part, transform.clamped()));
      return new StatuePose(parts);
   }
}
