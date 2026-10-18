package dev.burning.postmortem.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
*///?}

/**
 * An item worn in another mod's slot - Curios or Accessories - and where: that mod's slot name and index, and whether
 * it was the cosmetic copy. {@code visible} is the slot's own show/hide toggle, so a statue draws only what showed.
 */
public record WornItem(String mod, String slot, int index, boolean cosmetic, boolean visible, ItemStack stack) {
   public static final Codec<WornItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("Mod").forGetter(WornItem::mod),
      Codec.STRING.fieldOf("Slot").forGetter(WornItem::slot),
      Codec.INT.fieldOf("Index").forGetter(WornItem::index),
      Codec.BOOL.optionalFieldOf("Cosmetic", false).forGetter(WornItem::cosmetic),
      Codec.BOOL.optionalFieldOf("Visible", true).forGetter(WornItem::visible),
      ItemStack.CODEC.fieldOf("Item").forGetter(WornItem::stack)
   ).apply(instance, WornItem::new));

   // Statues sync what they wear to the client for drawing.
   //? if >=1.20.5 {
   /*private static final StreamCodec<RegistryFriendlyByteBuf, WornItem> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, WornItem::mod,
      ByteBufCodecs.STRING_UTF8, WornItem::slot,
      ByteBufCodecs.VAR_INT, WornItem::index,
      ByteBufCodecs.BOOL, WornItem::cosmetic,
      ByteBufCodecs.BOOL, WornItem::visible,
      ItemStack.OPTIONAL_STREAM_CODEC, WornItem::stack,
      WornItem::new);

   public static final EntityDataSerializer<List<WornItem>> LIST_SERIALIZER =
      EntityDataSerializer.forValueType(STREAM_CODEC.apply(ByteBufCodecs.list()));
   *///?} else {
   public static final EntityDataSerializer<List<WornItem>> LIST_SERIALIZER = EntityDataSerializer.simple(
      (buf, list) -> buf.writeCollection(list, (out, worn) -> {
         out.writeUtf(worn.mod());
         out.writeUtf(worn.slot());
         out.writeVarInt(worn.index());
         out.writeBoolean(worn.cosmetic());
         out.writeBoolean(worn.visible());
         out.writeItem(worn.stack());
      }),
      buf -> buf.readCollection(ArrayList::new, in -> new WornItem(
         in.readUtf(), in.readUtf(), in.readVarInt(), in.readBoolean(), in.readBoolean(), in.readItem())));
   //?}

   public WornSlot where() {
      return new WornSlot(this.mod, this.slot, this.index, this.cosmetic);
   }

   public WornItem withStack(ItemStack stack) {
      return new WornItem(this.mod, this.slot, this.index, this.cosmetic, this.visible, stack);
   }

   /** The items in {@code worn}, each copied, for a statue to show. */
   public static List<WornItem> copies(List<WornItem> worn) {
      List<WornItem> copies = new ArrayList<>(worn.size());
      for (WornItem item : worn) {
         copies.add(item.withStack(item.stack.copy()));
      }
      return copies;
   }
}
