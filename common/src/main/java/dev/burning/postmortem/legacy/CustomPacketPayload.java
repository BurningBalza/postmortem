package dev.burning.postmortem.legacy;

//? if <1.20.2 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

// 1.20.2's CustomPacketPayload, for 1.20.1: a payload that names its channel and writes itself, so the payload records
// and the platform's send methods are the same as on 1.20.2 to 1.20.4.
public interface CustomPacketPayload {
   void write(FriendlyByteBuf buf);

   ResourceLocation id();
}
//?}
