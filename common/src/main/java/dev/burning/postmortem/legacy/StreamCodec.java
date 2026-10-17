package dev.burning.postmortem.legacy;

//? if <1.20.5 {
/*import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// The part of 1.20.5's StreamCodec that Postmortem uses, for versions before it existed: packets and synced entity data
// are written with the same codecs on every version.
public interface StreamCodec<B, V> {
   V decode(B buf);

   void encode(B buf, V value);

   static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
      return new StreamCodec<>() {
         @Override
         public V decode(B buf) {
            return decoder.apply(buf);
         }

         @Override
         public void encode(B buf, V value) {
            encoder.accept(buf, value);
         }
      };
   }

   default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
      return of((buf, value) -> this.encode(buf, from.apply(value)), buf -> to.apply(this.decode(buf)));
   }

   static <B, C, T1> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1, Function<C, T1> getter1, Function<T1, C> factory) {
      return of((buf, value) -> codec1.encode(buf, getter1.apply(value)), buf -> factory.apply(codec1.decode(buf)));
   }

   static <B, C, T1, T2> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
      BiFunction<T1, T2, C> factory) {
      return of(
         (buf, value) -> {
            codec1.encode(buf, getter1.apply(value));
            codec2.encode(buf, getter2.apply(value));
         },
         buf -> factory.apply(codec1.decode(buf), codec2.decode(buf)));
   }

   static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
      Function3<T1, T2, T3, C> factory) {
      return of(
         (buf, value) -> {
            codec1.encode(buf, getter1.apply(value));
            codec2.encode(buf, getter2.apply(value));
            codec3.encode(buf, getter3.apply(value));
         },
         buf -> factory.apply(codec1.decode(buf), codec2.decode(buf), codec3.decode(buf)));
   }

   @FunctionalInterface
   interface Function3<T1, T2, T3, R> {
      R apply(T1 t1, T2 t2, T3 t3);
   }
}
*///?}
