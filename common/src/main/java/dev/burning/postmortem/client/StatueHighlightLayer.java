package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
import dev.burning.postmortem.entity.StatuePart;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import dev.burning.postmortem.entity.StatueEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.RenderType;
//? if >=1.21.11
//import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Glows the part being edited in the pose screen: a limb in the highlight colour, an item by drawing it again with a
 * white flash over its own model. Also owns the selection state the screen writes to, and the anchor points for item
 * parts, which the screen's part picker shares.
 */
//? if >=1.21.2 {
public class StatueHighlightLayer extends RenderLayer<StatueEntityRenderer.State, StatueEntityModel> {
//?} else
//public class StatueHighlightLayer extends RenderLayer<StatueEntity, StatueEntityModel> {
   private static final int SELECTED_TINT = 0xCCFFA526;
   private static final int SELECTED_GLOW = 0xFFFFBE45;
   private static final int HOVERED_TINT = 0x40FFA526;

   // Items flash white with vanilla's overlay instead (the creeper's flash): how far toward white, 0 to 1.
   private static final int SELECTED_ITEM_OVERLAY = OverlayTexture.pack(OverlayTexture.u(0.6F), OverlayTexture.v(false));
   private static final int HOVERED_ITEM_OVERLAY = OverlayTexture.pack(OverlayTexture.u(0.25F), OverlayTexture.v(false));

   //? if >=1.21.6 {
   /*private static final int GLOW_PASSES = 2;
   *///?} else {
   // Before 1.21.6 the preview is drawn straight onto the screen rather than into its own picture first, and the same
   // glow comes out far brighter there.
   private static final int GLOW_PASSES = 1;
   //?}

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

   private static int highlightedId = -1;
   private static StatuePart selected;
   private static StatuePart hovered;

   private final StatueEntityRenderer<?> renderer;
   private final ItemDrawState handItem = new ItemDrawState();

   public StatueHighlightLayer(StatueEntityRenderer<?> renderer) {
      super(renderer);
      this.renderer = renderer;
   }

   /** The item an item part holds; empty for body parts, and for item parts with nothing in them. */
   public static ItemStack itemFor(StatueEntityRenderer.State state, StatuePart part) {
      StatueEntity statue = state.statue;
      if (statue == null) {
         return ItemStack.EMPTY;
      }
      return switch (part) {
         case RIGHT_HAND_ITEM -> statue.getItemBySlot(handSlot(state, HumanoidArm.RIGHT));
         case LEFT_HAND_ITEM -> statue.getItemBySlot(handSlot(state, HumanoidArm.LEFT));
         case BACK_ITEM -> statue.getBackslotStack();
         case BELT_ITEM -> statue.getBeltslotStack();
         default -> ItemStack.EMPTY;
      };
   }

   /** The overlay an item part is drawn with: a flash while it is selected or hovered in the pose screen's preview. */
   public static int overlayFor(StatueEntityRenderer.State state, StatuePart part) {
      if (!state.preview || state.statue == null || state.statue.getId() != highlightedId) {
         return OverlayTexture.NO_OVERLAY;
      }
      if (part == selected) {
         return SELECTED_ITEM_OVERLAY;
      }
      return part == hovered ? HOVERED_ITEM_OVERLAY : OverlayTexture.NO_OVERLAY;
   }

   private static EquipmentSlot handSlot(StatueEntityRenderer.State state, HumanoidArm arm) {
      return arm == state.mainArm ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
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
   *///?} elif >=1.21.2 {
   public void render(
      PoseStack matrices,
      MultiBufferSource buffers,
      int light,
      StatueEntityRenderer.State state,
      float limbAngle,
      float limbDistance) {
      RenderSink sink = new RenderSink(buffers);
   //?} else {
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

      // Only the pose screen's preview: the statue itself, in the world behind the screen, stays as it is.
      if (!state.preview || state.statue == null || state.statue.getId() != highlightedId) {
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

      //? if >=1.21.2 {
      ResourceLocation texture = this.renderer.getTextureLocation(state);
      //?} else
      //ResourceLocation texture = this.renderer.getTextureLocation(state.statue);
      RenderType type = additive ? RenderType.eyes(texture) : RenderType.entityTranslucentEmissive(texture);

      ModelPart limb = this.limbFor(part);
      if (limb != null) {
         submit(limb, matrices, sink, type, colour);
         return;
      }

      // Back and belt items are drawn with their flash where they are drawn, by StatueBackItemLayer. A held item is
      // drawn by vanilla's own layer, so it is drawn again here, where that layer puts it, with the flash.
      HumanoidArm arm = part == StatuePart.RIGHT_HAND_ITEM ? HumanoidArm.RIGHT
         : part == StatuePart.LEFT_HAND_ITEM ? HumanoidArm.LEFT
         : null;
      ItemStack held = arm == null ? ItemStack.EMPTY : itemFor(state, part);
      if (held.isEmpty() || additive) {
         return;
      }

      this.handItem.update(
         this.renderer.itemModels(), held,
         arm == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
         state.statue);
      if (this.handItem.isEmpty()) {
         return;
      }

      matrices.pushPose();
      hand(matrices, this.getParentModel(), state, arm);
      sink.item(this.handItem, matrices, LightTexture.FULL_BRIGHT, overlayFor(state, part));
      matrices.popPose();
   }

   private static void submit(ModelPart part, PoseStack matrices, RenderSink sink, RenderType type, int colour) {
      sink.part(part, matrices, type, LightTexture.FULL_BRIGHT, colour);
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
