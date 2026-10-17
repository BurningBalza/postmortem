package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Where a statue layer draws: the submit queue from 1.21.9 on, or the buffer source before it. Layers hand their
 * drawing to this so the drawing code itself is the same on every version.
 */
public final class RenderSink {
   //? if >=1.21.9 {
   /*private final SubmitNodeCollector queue;

   public RenderSink(SubmitNodeCollector queue) {
      this.queue = queue;
   }

   /^* The loader's own sink, for other mods' renderers that are called directly. ^/
   public SubmitNodeCollector raw() {
      return this.queue;
   }

   public void part(ModelPart part, PoseStack matrices, RenderType type, int light, int colour) {
      //? if >=26.3 {
      /^this.queue.submitModelPart(part, matrices, type, light, OverlayTexture.NO_OVERLAY, null, colour);
      ^///?} else
      this.queue.submitModelPart(part, matrices, type, light, OverlayTexture.NO_OVERLAY, null, colour, null);
   }

   public void item(ItemDrawState item, PoseStack matrices, int light) {
      item.state.submit(matrices, this.queue, light, OverlayTexture.NO_OVERLAY, 0);
   }
   *///?} else {
   private final MultiBufferSource buffers;

   public RenderSink(MultiBufferSource buffers) {
      this.buffers = buffers;
   }

   public MultiBufferSource raw() {
      return this.buffers;
   }

   public void part(ModelPart part, PoseStack matrices, RenderType type, int light, int colour) {
      //? if >=1.21 {
      /*part.render(matrices, this.buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY, colour);
      *///?} else {
      // Before 1.21 the tint is four floats rather than one ARGB int.
      part.render(matrices, this.buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY,
         (colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F, (colour >>> 24) / 255.0F);
      //?}
   }

   public void item(ItemDrawState item, PoseStack matrices, int light) {
      //? if >=1.21.4 {
      /*item.state.render(matrices, this.buffers, light, OverlayTexture.NO_OVERLAY);
      *///?} else {
      net.minecraft.client.Minecraft.getInstance().getItemRenderer().render(
         item.stack, item.context, false, matrices, this.buffers, light, OverlayTexture.NO_OVERLAY, item.model);
      //?}
   }
   //?}
}
