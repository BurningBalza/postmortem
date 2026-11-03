package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.compat.ArsenalCompat;
import dev.burning.postmortem.entity.StatuePart;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
//import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if <1.21.2
//import dev.burning.postmortem.entity.StatueEntity;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

//? if >=1.21.2 {
public class StatueBackItemLayer extends RenderLayer<StatueEntityRenderer.State, StatueEntityModel> {
//?} else
//public class StatueBackItemLayer extends RenderLayer<StatueEntity, StatueEntityModel> {
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

   private static final float ARSENAL_CHESTPLATE_OUT = 0.05F;
   private static final float ARSENAL_TILT = 6.0F;
   private static final float ARSENAL_TURN = 180.0F;
   private static final float ARSENAL_DOWN = 0.3F;
   private static final float ARSENAL_IN = -0.1F;
   private static final float ARSENAL_SCALE = 0.85F;
   private static final float ARSENAL_BIG_DOWN = 0.2F;
   private static final float ARSENAL_BIG_IN = -0.15F;
   private static final float ARSENAL_BIG_SCALE = 1.6F;
   private static final float ARSENAL_SHIELD_DOWN = 0.2F;
   private static final float ARSENAL_SHIELD_SCALE = 1.8F;

   public enum Fit {
      GENERIC(ItemDisplayContext.FIXED),
      TRIDENT(ItemDisplayContext.NONE),
      BANNER(ItemDisplayContext.FIXED),
      ARSENAL(ItemDisplayContext.FIXED),
      ARSENAL_BIG(ItemDisplayContext.FIXED),
      ARSENAL_SHIELD(ItemDisplayContext.FIXED),
      ARSENAL_BIG_SHIELD(ItemDisplayContext.FIXED);

      private final ItemDisplayContext displayContext;

      Fit(ItemDisplayContext displayContext) {
         this.displayContext = displayContext;
      }

      public ItemDisplayContext displayContext() {
         return this.displayContext;
      }

      public static Fit forItem(ItemStack stack) {
         if (ArsenalCompat.isPresent()) {
            boolean big = stack.is(ArsenalCompat.BIG_WEAPONS);
            boolean shield = stack.is(ArsenalCompat.SHIELDS);
            return shield ? (big ? ARSENAL_BIG_SHIELD : ARSENAL_SHIELD) : big ? ARSENAL_BIG : ARSENAL;
         }
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
      //? if >=1.21.2 {
      RenderLayerParent<StatueEntityRenderer.State, StatueEntityModel> context,
      //?} else
      //RenderLayerParent<StatueEntity, StatueEntityModel> context,
      EntityRendererProvider.Context factory) {
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
   //?} elif >=1.21.2 {
   /*public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(buffers);
   *///?} else {
   /*public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntity entity,
      float limbAngle,
      float limbDistance,
      float tickDelta,
      float age,
      float headYaw,
      float headPitch) {
      RenderSink sink = new RenderSink(buffers);
      StatueEntityRenderer.State state = this.getParentModel().current;
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
            matrices.rotate(Axis.ZN.rotationDegrees(TRIDENT_ANGLE));
         }
         case BANNER -> {
            matrices.rotate(Axis.XN.rotationDegrees(BANNER_TURN));
            matrices.translate(0.0F, BANNER_LIFT, 0.0F);
            matrices.rotate(Axis.XP.rotationDegrees(BANNER_TILT));
            matrices.translate(0.0F, BANNER_DROP, BANNER_OUT);
            matrices.scale(BANNER_SCALE, BANNER_SCALE, BANNER_SCALE);
         }
         case GENERIC -> {
            matrices.translate(0.0F, DOWN_THE_BACK, OUT_FROM_BACK);
            matrices.scale(SCALE, SCALE, SCALE);
         }
         case ARSENAL -> arsenal(matrices, state, ARSENAL_DOWN, ARSENAL_IN, ARSENAL_SCALE);
         case ARSENAL_BIG -> arsenal(matrices, state, ARSENAL_BIG_DOWN, ARSENAL_BIG_IN, ARSENAL_BIG_SCALE);
         case ARSENAL_SHIELD ->
            arsenal(matrices, state, ARSENAL_DOWN + ARSENAL_SHIELD_DOWN, ARSENAL_IN, ARSENAL_SHIELD_SCALE);
         case ARSENAL_BIG_SHIELD ->
            arsenal(matrices, state, ARSENAL_BIG_DOWN + ARSENAL_SHIELD_DOWN, ARSENAL_BIG_IN, ARSENAL_SHIELD_SCALE);
      }

      sink.item(state.backslotItemState, matrices, light, StatueHighlightLayer.overlayFor(state, StatuePart.BACK_ITEM));
      matrices.popPose();
   }

   private static void arsenal(PoseStack matrices, StatueEntityRenderer.State state, float down, float in, float scale) {
      if (!state.chestEquipment.isEmpty()) {
         matrices.translate(0.0F, 0.0F, ARSENAL_CHESTPLATE_OUT);
      }
      matrices.rotate(Axis.XP.rotationDegrees(ARSENAL_TILT));
      matrices.rotate(Axis.YP.rotationDegrees(ARSENAL_TURN));
      matrices.translate(0.0F, down, in);
      matrices.scale(scale, scale, scale);
   }
}
