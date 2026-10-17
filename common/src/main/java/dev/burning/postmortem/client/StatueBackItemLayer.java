package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.entity.StatuePart;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
//import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

/**
 * Draws a statue's back (and belt) items. Combat Amenities' or Backslot's own renderers are used when present, so the
 * statue matches how players look; otherwise the item is placed by hand.
 */
public class StatueBackItemLayer extends RenderLayer<StatueEntityRenderer.State, StatueEntityModel> {
   private static final float DOWN_THE_BACK = 0.30F;
   private static final float OUT_FROM_BACK = 0.15F;
   private static final float SCALE = 0.9F;

   private static final float TRIDENT_X = 1.25F;
   private static final float TRIDENT_Y = 0.65F;
   private static final float TRIDENT_Z = 0.65F;
   private static final float TRIDENT_ANGLE = 45.0F;

   private static final float BANNER_TURN = 180.0F;
   private static final float BANNER_LIFT = -0.05F;
   private static final float BANNER_TILT = 5.0F;
   private static final float BANNER_DROP = -0.95F;
   private static final float BANNER_OUT = -0.13F;
   private static final float BANNER_SCALE = 1.4F;

   public enum Fit {
      GENERIC(ItemDisplayContext.FIXED),
      TRIDENT(ItemDisplayContext.NONE),
      BANNER(ItemDisplayContext.FIXED);

      private final ItemDisplayContext displayContext;

      Fit(ItemDisplayContext displayContext) {
         this.displayContext = displayContext;
      }

      public ItemDisplayContext displayContext() {
         return this.displayContext;
      }

      public static Fit forItem(ItemStack stack) {
         if (stack.getItem() instanceof TridentItem) {
            return TRIDENT;
         }
         if (stack.getItem() instanceof BannerItem) {
            return BANNER;
         }

         return GENERIC;
      }
   }

   private final CompatRenderers.Backslot backslot;

   private final CompatRenderers.Amenities amenities;

   public StatueBackItemLayer(
      RenderLayerParent<StatueEntityRenderer.State, StatueEntityModel> context, EntityRendererProvider.Context factory) {
      super(context);
      this.backslot = CompatRenderers.Backslot.create(factory);
      this.amenities = CompatRenderers.Amenities.create(factory);
   }

   @Override
   //? if >=1.21.9 {
   public void submit(
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(queue);
   //?} else {
   /*public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(buffers);
   *///?}

      if (this.amenities != null && state.statue != null) {
         matrices.pushPose();
         boolean drawn = this.amenities.render(matrices, sink, light, state, this.getParentModel(), state.statue);
         matrices.popPose();

         if (drawn) {
            return;
         }
      }

      if (state.backslotItemState.isEmpty()) {
         return;
      }

      if (this.backslot != null && state.statue != null) {
         matrices.pushPose();
         StatueEntityModel.applyToMatrix(matrices, state.limbPose.get(StatuePart.BACK_ITEM));
         boolean drawn = this.backslot.render(matrices, sink, light, state, this.getParentModel(), state.statue);
         matrices.popPose();

         if (drawn) {
            return;
         }
      }

      matrices.pushPose();
      this.getParentModel().body.translateAndRotate(matrices);
      StatueEntityModel.applyToMatrix(matrices, state.limbPose.get(StatuePart.BACK_ITEM));

      switch (state.backslotFit) {
         case TRIDENT -> {
            matrices.translate(TRIDENT_X, TRIDENT_Y, TRIDENT_Z);
            matrices.mulPose(Axis.ZN.rotationDegrees(TRIDENT_ANGLE));
         }
         case BANNER -> {
            matrices.mulPose(Axis.XN.rotationDegrees(BANNER_TURN));
            matrices.translate(0.0F, BANNER_LIFT, 0.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(BANNER_TILT));
            matrices.translate(0.0F, BANNER_DROP, BANNER_OUT);
            matrices.scale(BANNER_SCALE, BANNER_SCALE, BANNER_SCALE);
         }
         case GENERIC -> {
            matrices.translate(0.0F, DOWN_THE_BACK, OUT_FROM_BACK);
            matrices.scale(SCALE, SCALE, SCALE);
         }
      }

      sink.item(state.backslotItemState, matrices, light);
      matrices.popPose();
   }
}
