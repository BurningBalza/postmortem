package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
import dev.burning.postmortem.entity.StatuePart;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
//? if >=1.21.11
//import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Glows the part being edited in the pose screen. Also owns the selection state the screen writes to, and the anchor
 * points for item parts, which the screen's part picker shares.
 */
public class StatueHighlightLayer extends RenderLayer<StatueEntityRenderer.State, StatueEntityModel> {
   private static final int SELECTED_TINT = 0xCCFFA526;
   private static final int SELECTED_GLOW = 0xFFFFBE45;
   private static final int HOVERED_TINT = 0x40FFA526;

   private static final int GLOW_PASSES = 2;

   public static final float ITEM_RADIUS = 0.16F;
   private static final float HAND_SIDE = 1.0F / 16.0F;
   private static final float HAND_UP = 2.0F / 16.0F;
   private static final float HAND_FORWARD = -10.0F / 16.0F;

   private static final float BACK_ITEM_UP = 0.30F;
   private static final float BACK_ITEM_OUT = 0.15F;

   private static final float AMENITIES_BACK_UP = 0.35F;
   private static final float AMENITIES_BACK_OUT = 0.125F;
   private static final float AMENITIES_CHEST_NUDGE = 0.05F;

   private static final float BELT_ITEM_SIDE = 0.275F;
   private static final float BELT_ITEM_DOWN = 0.90F;
   private static final float BELT_ITEM_OUT = 0.10F;

   private static final ModelPart ITEM_MARKER = bakeItemMarker();

   private static int highlightedId = -1;
   private static StatuePart selected;
   private static StatuePart hovered;

   private final StatueEntityRenderer<?> renderer;

   public StatueHighlightLayer(StatueEntityRenderer<?> renderer) {
      super(renderer);
      this.renderer = renderer;
   }

   public static void set(int statueId, StatuePart selectedPart, StatuePart hoveredPart) {
      highlightedId = statueId;
      selected = selectedPart;
      hovered = hoveredPart;
   }

   public static void clear() {
      set(-1, null, null);
   }

   @Override
   //? if >=1.21.9 {
   /*public void submit(
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(queue);
   *///?} else {
   public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(buffers);
   //?}

      if (state.statue == null || state.statue.getId() != highlightedId) {
         return;
      }

      if (hovered != null && hovered != selected) {
         this.submitPart(matrices, sink, state, hovered, HOVERED_TINT, false);
      }
      if (selected != null) {
         this.submitPart(matrices, sink, state, selected, SELECTED_TINT, false);
         for (int pass = 0; pass < GLOW_PASSES; pass++) {
            this.submitPart(matrices, sink, state, selected, SELECTED_GLOW, true);
         }
      }
   }

   /** Moves the matrix to where an item part sits. Returns false for body parts, which have no anchor. */
   public static boolean applyItemAnchor(
      PoseStack matrices, StatueEntityModel model, StatueEntityRenderer.State state, StatuePart part) {

      switch (part) {
         case RIGHT_HAND_ITEM -> hand(matrices, model, state, HumanoidArm.RIGHT);
         case LEFT_HAND_ITEM -> hand(matrices, model, state, HumanoidArm.LEFT);
         case BACK_ITEM -> {
            StatueEntityModel.applyToMatrix(matrices, state.limbPose.get(StatuePart.BACK_ITEM));
            model.body.translateAndRotate(matrices);

            if (CombatAmenitiesCompat.isPresent()) {
               float out = AMENITIES_BACK_OUT + (state.chestItem.isEmpty() ? 0.0F : AMENITIES_CHEST_NUDGE);
               matrices.translate(0.0F, AMENITIES_BACK_UP, out);
            } else {
               matrices.translate(0.0F, BACK_ITEM_UP, BACK_ITEM_OUT);
            }
         }
         case BELT_ITEM -> {
            StatueEntityModel.applyToMatrix(matrices, state.limbPose.get(StatuePart.BELT_ITEM));
            model.body.translateAndRotate(matrices);
            matrices.translate(BELT_ITEM_SIDE, BELT_ITEM_DOWN, BELT_ITEM_OUT);
         }
         default -> {
            return false;
         }
      }

      return true;
   }

   private static void hand(PoseStack matrices, StatueEntityModel model, StatueEntityRenderer.State state, HumanoidArm arm) {
      //? if >=1.21.9 {
      /*model.translateToHand(state, arm, matrices);
      *///?} else
      model.translateToHand(arm, matrices);
      matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
      matrices.translate(arm == HumanoidArm.LEFT ? -HAND_SIDE : HAND_SIDE, HAND_UP, HAND_FORWARD);
   }

   private void submitPart(
      PoseStack matrices,
      RenderSink sink,
      StatueEntityRenderer.State state,
      StatuePart part,
      int colour,
      boolean additive) {

      ResourceLocation texture = this.renderer.getTextureLocation(state);
      RenderType type = additive ? RenderType.eyes(texture) : RenderType.entityTranslucentEmissive(texture);

      ModelPart limb = this.limbFor(part);
      if (limb != null) {
         submit(limb, matrices, sink, type, colour);
         return;
      }

      matrices.pushPose();
      if (applyItemAnchor(matrices, this.getParentModel(), state, part)) {
         submit(ITEM_MARKER, matrices, sink, type, colour);
      }
      matrices.popPose();
   }

   private static void submit(ModelPart part, PoseStack matrices, RenderSink sink, RenderType type, int colour) {
      sink.part(part, matrices, type, LightTexture.FULL_BRIGHT, colour);
   }

   private static ModelPart bakeItemMarker() {
      float size = ITEM_RADIUS * 2.0F * 16.0F;
      float origin = -size / 2.0F;

      MeshDefinition mesh = new MeshDefinition();
      mesh.getRoot().addOrReplaceChild(
         "marker",
         CubeListBuilder.create().texOffs(0, 0).addBox(origin, origin, origin, size, size, size),
         PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("marker");
   }

   private ModelPart limbFor(StatuePart part) {
      StatueEntityModel model = this.getParentModel();
      return switch (part) {
         case HEAD -> model.head;
         case BODY -> model.body;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_LEG -> model.rightLeg;
         case LEFT_LEG -> model.leftLeg;
         default -> null;
      };
   }
}
