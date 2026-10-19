package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.9 {
/*import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.2
//import com.mojang.blaze3d.vertex.VertexConsumer;
//?}

public final class RenderSink {
   //? if >=1.21.9 {
   /*private final SubmitNodeCollector queue;

   public RenderSink(SubmitNodeCollector queue) {
      this.queue = queue;
   }

   public SubmitNodeCollector raw() {
      return this.queue;
   }

   public void part(ModelPart part, PoseStack matrices, RenderType type, int light, int colour) {
      //? if >=26.3 {
      /^this.queue.submitModelPart(part, matrices, type, light, OverlayTexture.NO_OVERLAY, null, colour);
      ^///?} else
      this.queue.submitModelPart(part, matrices, type, light, OverlayTexture.NO_OVERLAY, null, colour, null);
   }

   public void item(ItemDrawState item, PoseStack matrices, int light, int overlay) {
      item.state.submit(matrices, this.queue, light, overlay, 0);
   }

   public RenderSink withOverlay(int overlay) {
      return overlay == OverlayTexture.NO_OVERLAY ? this : new RenderSink(overlaid(SubmitNodeCollector.class, this.queue, overlay));
   }

   private static <T> T overlaid(Class<T> type, T target, int overlay) {
      return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
         Class<?>[] parameters = method.getParameterTypes();
         if (args != null && drawsWithOverlay(parameters)) {
            args = args.clone();
            int ints = 0;
            for (int i = 0; i < parameters.length; i++) {
               if (parameters[i] == int.class && ++ints == 2) {
                  args[i] = overlay;
                  break;
               }
            }
         }

         Object result;
         try {
            result = method.invoke(target, args);
         } catch (InvocationTargetException e) {
            throw e.getCause();
         }
         if (method.getReturnType() == OrderedSubmitNodeCollector.class && result instanceof OrderedSubmitNodeCollector ordered) {
            return overlaid(OrderedSubmitNodeCollector.class, ordered, overlay);
         }
         return result;
      }));
   }

   private static boolean drawsWithOverlay(Class<?>[] parameters) {
      for (Class<?> parameter : parameters) {
         if (parameter == ItemDisplayContext.class || parameter == ModelPart.class || Model.class.isAssignableFrom(parameter)) {
            return true;
         }
      }
      return false;
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
      
      part.render(matrices, this.buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY,
         (colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F, (colour >>> 24) / 255.0F);
      //?}
   }

   public void item(ItemDrawState item, PoseStack matrices, int light, int overlay) {
      //? if >=1.21.4 {
      /*item.state.render(matrices, this.buffers, light, overlay);
      *///?} else {
      boolean leftHand = item.context == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
      net.minecraft.client.Minecraft.getInstance().getItemRenderer().render(
         item.stack, item.context, leftHand, matrices, this.buffers, light, overlay, item.model);
      //?}
   }

   public RenderSink withOverlay(int overlay) {
      //? if >=1.21.2 {
      /*MultiBufferSource buffers = this.buffers;
      return overlay == OverlayTexture.NO_OVERLAY ? this : new RenderSink(type -> new Overlaid(buffers.getBuffer(type), overlay));
      *///?} else
      return this;
   }

   //? if >=1.21.2 {
   /*private record Overlaid(VertexConsumer target, int overlay) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.target.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.target.setColor(red, green, blue, alpha);
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.target.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.target.setOverlay(this.overlay);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.target.setUv2(u, v);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         this.target.setNormal(x, y, z);
         return this;
      }
   }
   *///?}
   //?}

   public void item(ItemDrawState item, PoseStack matrices, int light) {
      this.item(item, matrices, light, OverlayTexture.NO_OVERLAY);
   }
}
