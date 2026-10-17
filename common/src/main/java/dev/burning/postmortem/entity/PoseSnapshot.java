package dev.burning.postmortem.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public record PoseSnapshot(
   Pose pose,
   float pitch,
   float bodyYaw,
   float headYaw,
   float limbAngle,
   float limbAmplitude,
   float handSwing,
   float leaning,
   boolean riding) {

   public static final PoseSnapshot STANDING =
      new PoseSnapshot(Pose.STANDING, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

   public static PoseSnapshot of(LivingEntity entity, float limbAngle, float limbAmplitude) {
      return new PoseSnapshot(
         entity.getPose(),
         entity.getXRot(),
         entity.getVisualRotationYInDegrees(),
         entity.getYHeadRot(),
         limbAngle,
         limbAmplitude,
         entity.attackAnim,
         entity.getSwimAmount(1.0F),
         entity.isPassenger());
   }

   /** Mixed into Player to remember the last pose it held while alive. */
   public interface Holder {
      PoseSnapshot postmortem$getLastLivingPose();
   }
}
