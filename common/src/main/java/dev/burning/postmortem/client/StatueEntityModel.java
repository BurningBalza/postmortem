package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.entity.LimbTransform;
import dev.burning.postmortem.entity.StatuePart;
import dev.burning.postmortem.entity.StatuePose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?} else
//import dev.burning.postmortem.entity.StatueEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3fc;

//? if >=1.21.2 {
public class StatueEntityModel extends HumanoidModel<StatueEntityRenderer.State> {
//?} else
//public class StatueEntityModel extends HumanoidModel<StatueEntity> {
   //? if <1.21.9 {
   
   StatueEntityRenderer.State current;
   //?}

   //? if <1.21.2 {
   /*private final ModelPart jacket;
   private final ModelPart rightSleeve;
   private final ModelPart leftSleeve;
   private final ModelPart rightPants;
   private final ModelPart leftPants;
   *///?}

   public StatueEntityModel(ModelPart root) {
      super(root);
      //? if <1.21.2 {
      /*boolean outer = root.hasChild("jacket");
      this.jacket = outer ? root.getChild("jacket") : null;
      this.rightSleeve = outer ? root.getChild("right_sleeve") : null;
      this.leftSleeve = outer ? root.getChild("left_sleeve") : null;
      this.rightPants = outer ? root.getChild("right_pants") : null;
      this.leftPants = outer ? root.getChild("left_pants") : null;
      *///?}
   }

   //? if >=1.21.2 {
   @Override
   public void setupAnim(StatueEntityRenderer.State state) {
      super.setupAnim(state);
   //?} else {
   /*@Override
   public void setupAnim(
      StatueEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
      this.setupAnim(this.current);
   }

   public void setupAnim(StatueEntityRenderer.State state) {
      this.attackTime = state.attackTime;
      this.riding = state.isPassenger;
      this.young = false;
      this.crouching = state.isCrouching;
      this.swimAmount = state.swimAmount;
      this.rightArmPose = state.rightArmPose;
      this.leftArmPose = state.leftArmPose;
      super.setupAnim(
         state.statue, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot);
   *///?}
      //? if <1.21.9
      this.current = state;

      StatuePose pose = state.limbPose;
      if (!pose.isDefault()) {
         apply(this.head, pose.get(StatuePart.HEAD));
         apply(this.body, pose.get(StatuePart.BODY));
         apply(this.rightArm, pose.get(StatuePart.RIGHT_ARM));
         apply(this.leftArm, pose.get(StatuePart.LEFT_ARM));
         apply(this.rightLeg, pose.get(StatuePart.RIGHT_LEG));
         apply(this.leftLeg, pose.get(StatuePart.LEFT_LEG));
      }
      //? if <1.21.2
      //this.syncOuterLayers();
   }

   //? if <1.21.2 {
   /*private void syncOuterLayers() {
      this.hat.copyFrom(this.head);
      if (this.jacket == null) {
         return;
      }

      this.jacket.copyFrom(this.body);
      this.rightSleeve.copyFrom(this.rightArm);
      this.leftSleeve.copyFrom(this.leftArm);
      this.rightPants.copyFrom(this.rightLeg);
      this.leftPants.copyFrom(this.leftLeg);
   }

   @Override
   protected Iterable<ModelPart> bodyParts() {
      if (this.jacket == null) {
         return super.bodyParts();
      }

      return com.google.common.collect.Iterables.concat(
         super.bodyParts(),
         java.util.List.of(this.jacket, this.rightSleeve, this.leftSleeve, this.rightPants, this.leftPants));
   }

   public ModelPart outerLayer(ModelPart limb) {
      if (limb == this.head) {
         return this.hat;
      }
      if (limb == this.body) {
         return this.jacket;
      }
      if (limb == this.rightArm) {
         return this.rightSleeve;
      }
      if (limb == this.leftArm) {
         return this.leftSleeve;
      }
      if (limb == this.rightLeg) {
         return this.rightPants;
      }
      return limb == this.leftLeg ? this.leftPants : null;
   }
   *///?}

   @Override
   //? if >=1.21.9 {
   /*public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack matrices) {
      super.translateToHand(state, arm, matrices);
   *///?} else {
   public void translateToHand(HumanoidArm arm, PoseStack matrices) {
      super.translateToHand(arm, matrices);
      Object state = this.current;
   //?}

      if (state instanceof StatueEntityRenderer.State statueState) {
         applyToMatrix(
            matrices,
            statueState.limbPose.get(arm == HumanoidArm.RIGHT ? StatuePart.RIGHT_HAND_ITEM : StatuePart.LEFT_HAND_ITEM));
      }
   }

   //? if >=1.21.2 && <1.21.4 {
   @Override
   protected HumanoidModel.ArmPose getArmPose(StatueEntityRenderer.State state, HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose;
   }
   //?}

   public static void applyToMatrix(PoseStack matrices, LimbTransform transform) {
      if (transform.isNone()) {
         return;
      }

      Vector3fc offset = transform.offset();
      matrices.translate(offset.x() / 16.0F, offset.y() / 16.0F, offset.z() / 16.0F);

      LimbTransform.Rotation rotation = transform.rotation();
      matrices.mulPose(Axis.XP.rotationDegrees(rotation.x()));
      matrices.mulPose(Axis.YP.rotationDegrees(rotation.y()));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
   }

   private static void apply(ModelPart part, LimbTransform transform) {
      LimbTransform.Rotation rotation = transform.rotation();
      part.setRotation(
         rotation.x() * Mth.DEG_TO_RAD,
         rotation.y() * Mth.DEG_TO_RAD,
         rotation.z() * Mth.DEG_TO_RAD);

      Vector3fc offset = transform.offset();
      PartPose origin = part.getInitialPose();
      //? if >=1.21.2 {
      part.setPos(origin.x() + offset.x(), origin.y() + offset.y(), origin.z() + offset.z());
      //?} else
      //part.setPos(origin.x + offset.x(), origin.y + offset.y(), origin.z + offset.z());
   }
}
