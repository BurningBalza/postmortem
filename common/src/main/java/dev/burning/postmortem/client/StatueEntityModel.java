package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.entity.LimbTransform;
import dev.burning.postmortem.entity.StatuePart;
import dev.burning.postmortem.entity.StatuePose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Rotations;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3fc;

public class StatueEntityModel extends HumanoidModel<StatueEntityRenderer.State> {
   public StatueEntityModel(ModelPart root) {
      super(root);
   }

   @Override
   public void setupAnim(StatueEntityRenderer.State state) {
      super.setupAnim(state);

      StatuePose pose = state.limbPose;
      if (pose.isDefault()) {
         return;
      }

      apply(this.head, pose.get(StatuePart.HEAD));
      apply(this.body, pose.get(StatuePart.BODY));
      apply(this.rightArm, pose.get(StatuePart.RIGHT_ARM));
      apply(this.leftArm, pose.get(StatuePart.LEFT_ARM));
      apply(this.rightLeg, pose.get(StatuePart.RIGHT_LEG));
      apply(this.leftLeg, pose.get(StatuePart.LEFT_LEG));
   }

   @Override
   public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack matrices) {
      super.translateToHand(state, arm, matrices);

      if (state instanceof StatueEntityRenderer.State statueState) {
         applyToMatrix(
            matrices,
            statueState.limbPose.get(arm == HumanoidArm.RIGHT ? StatuePart.RIGHT_HAND_ITEM : StatuePart.LEFT_HAND_ITEM));
      }
   }

   public static void applyToMatrix(PoseStack matrices, LimbTransform transform) {
      if (transform.isNone()) {
         return;
      }

      Vector3fc offset = transform.offset();
      matrices.translate(offset.x() / 16.0F, offset.y() / 16.0F, offset.z() / 16.0F);

      Rotations rotation = transform.rotation();
      matrices.mulPose(Axis.XP.rotationDegrees(rotation.x()));
      matrices.mulPose(Axis.YP.rotationDegrees(rotation.y()));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
   }

   private static void apply(ModelPart part, LimbTransform transform) {
      Rotations rotation = transform.rotation();
      part.setRotation(
         rotation.x() * Mth.DEG_TO_RAD,
         rotation.y() * Mth.DEG_TO_RAD,
         rotation.z() * Mth.DEG_TO_RAD);

      Vector3fc offset = transform.offset();
      PartPose origin = part.getInitialPose();
      part.setPos(origin.x() + offset.x(), origin.y() + offset.y(), origin.z() + offset.z());
   }
}
