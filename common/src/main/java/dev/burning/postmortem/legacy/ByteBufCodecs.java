package dev.burning.postmortem.legacy;

//? if <1.20.5 {
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

public final class ByteBufCodecs {
   public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);

   public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
      (buf, value) -> new FriendlyByteBuf(buf).writeVarInt(value), buf -> new FriendlyByteBuf(buf).readVarInt());

   public static final StreamCodec<ByteBuf, Vector3f> VECTOR3F = StreamCodec.of(
      (buf, value) -> new FriendlyByteBuf(buf).writeVector3f(value), buf -> new FriendlyByteBuf(buf).readVector3f());

   private ByteBufCodecs() {
   }
}
//?}
