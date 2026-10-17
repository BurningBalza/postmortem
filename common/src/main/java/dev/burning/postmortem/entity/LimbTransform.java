package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//?} else {
/*import dev.burning.postmortem.legacy.ByteBufCodecs;
import dev.burning.postmortem.legacy.StreamCodec;
*///?}
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record LimbTransform(LimbTransform.Rotation rotation, Vector3fc offset) {
   public static final Rotation NO_ROTATION = new Rotation(0.0F, 0.0F, 0.0F);
   public static final Vector3fc NO_OFFSET = new Vector3f();

   public static final LimbTransform NONE = new LimbTransform(NO_ROTATION, NO_OFFSET);

   public static final float MAX_ROTATION = 180.0F;
   public static final float MAX_OFFSET = 16.0F;

   private static final Codec<Vector3fc> OFFSET_CODEC =
      ExtraCodecs.VECTOR3F.xmap(offset -> (Vector3fc) offset, Vector3f::new);
   private static final StreamCodec<ByteBuf, Vector3fc> OFFSET_STREAM_CODEC =
      ByteBufCodecs.VECTOR3F.map(offset -> (Vector3fc) offset, Vector3f::new);

   public static final Codec<LimbTransform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Rotation.CODEC.optionalFieldOf("Rotation", NO_ROTATION).forGetter(LimbTransform::rotation),
      OFFSET_CODEC.optionalFieldOf("Offset", NO_OFFSET).forGetter(LimbTransform::offset)
   ).apply(instance, LimbTransform::new));

   public static final StreamCodec<ByteBuf, LimbTransform> PACKET_CODEC = StreamCodec.composite(
      Rotation.STREAM_CODEC, LimbTransform::rotation,
      OFFSET_STREAM_CODEC, LimbTransform::offset,
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
      return new LimbTransform(new Rotation(pitch, yaw, roll), this.offset);
   }

   public LimbTransform withOffset(float x, float y, float z) {
      return new LimbTransform(this.rotation, new Vector3f(x, y, z));
   }

   public LimbTransform clamped() {
      return new LimbTransform(
         new Rotation(
            Mth.clamp(this.rotation.x(), -MAX_ROTATION, MAX_ROTATION),
            Mth.clamp(this.rotation.y(), -MAX_ROTATION, MAX_ROTATION),
            Mth.clamp(this.rotation.z(), -MAX_ROTATION, MAX_ROTATION)),
         new Vector3f(
            Mth.clamp(this.offset.x(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(this.offset.y(), -MAX_OFFSET, MAX_OFFSET),
            Mth.clamp(this.offset.z(), -MAX_OFFSET, MAX_OFFSET)));
   }

   /**
    * Pitch, yaw and roll in degrees. Encoded exactly like vanilla's {@code Rotations} (three floats), which changed
    * shape between versions, so saved poses and packets stay compatible.
    */
   public record Rotation(float x, float y, float z) {
      public static final Codec<Rotation> CODEC = Codec.FLOAT.listOf().comapFlatMap(
         list -> list.size() == 3
            ? DataResult.success(new Rotation(list.get(0), list.get(1), list.get(2)))
            : DataResult.error(() -> "Rotation needs 3 values, got " + list.size()),
         rotation -> List.of(rotation.x, rotation.y, rotation.z));

      public static final StreamCodec<ByteBuf, Rotation> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.FLOAT, Rotation::x,
         ByteBufCodecs.FLOAT, Rotation::y,
         ByteBufCodecs.FLOAT, Rotation::z,
         Rotation::new);

      public Rotation {
         x = Float.isFinite(x) ? x % 360.0F : 0.0F;
         y = Float.isFinite(y) ? y % 360.0F : 0.0F;
         z = Float.isFinite(z) ? z % 360.0F : 0.0F;
      }
   }
}
