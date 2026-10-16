package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Rotations;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record LimbTransform(Rotations rotation, Vector3fc offset) {
   public static final Rotations NO_ROTATION = new Rotations(0.0F, 0.0F, 0.0F);
   public static final Vector3fc NO_OFFSET = new Vector3f();

   public static final LimbTransform NONE = new LimbTransform(NO_ROTATION, NO_OFFSET);

   public static final float MAX_ROTATION = 180.0F;
   public static final float MAX_OFFSET = 16.0F;

   public static final Codec<LimbTransform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Rotations.CODEC.optionalFieldOf("Rotation", NO_ROTATION).forGetter(LimbTransform::rotation),
      ExtraCodecs.VECTOR3F.optionalFieldOf("Offset", NO_OFFSET).forGetter(LimbTransform::offset)
   ).apply(instance, LimbTransform::new));

   public static final StreamCodec<ByteBuf, LimbTransform> PACKET_CODEC = StreamCodec.composite(
      Rotations.STREAM_CODEC, LimbTransform::rotation,
      ByteBufCodecs.VECTOR3F, LimbTransform::offset,
      LimbTransform::new);

   public boolean isNone() {
      return this.rotation.x() == 0.0F
         && this.rotation.y() == 0.0F
         && this.rotation.z() == 0.0F
         && this.offset.x() == 0.0F
         && this.offset.y() == 0.0F
         && this.offset.z() == 0.0F;
   }

   public LimbTransform withRotation(float pitch, float yaw, float roll) {
      return new LimbTransform(new Rotations(pitch, yaw, roll), this.offset);
   }

   public LimbTransform withOffset(float x, float y, float z) {
      return new LimbTransform(this.rotation, new Vector3f(x, y, z));
   }

   public LimbTransform clamped() {
      return new LimbTransform(
         new Rotations(
            Mth.clamp(this.rotation.x(), -MAX_ROTATION, MAX_ROTATION),
            Mth.clamp(this.rotation.y(), -MAX_ROTATION, MAX_ROTATION),
            Mth.clamp(this.rotation.z(), -MAX_ROTATION, MAX_ROTATION)),
         new Vector3f(
            Mth.clamp(this.offset.x(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(this.offset.y(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(this.offset.z(), -MAX_OFFSET, MAX_OFFSET)));
   }
}
