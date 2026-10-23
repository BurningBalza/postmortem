package dev.burning.postmortem.mixin;

import dev.burning.postmortem.entity.PoseSnapshot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1100)
public abstract class PlayerEntityMixin implements PoseSnapshot.Holder {
   @Unique
   private PoseSnapshot postmortem$lastLivingPose = PoseSnapshot.STANDING;

   @Unique
   private double postmortem$lastX;

   @Unique
   private double postmortem$lastZ;

   @Unique
   private boolean postmortem$hasLastPosition;

   @Unique
   private float postmortem$limbSpeed;

   @Unique
   private float postmortem$limbProgress;

   @Unique
   private double postmortem$stepX;

   @Unique
   private double postmortem$stepZ;

   @Unique
   private float postmortem$bodyYaw = Float.NaN;

   @Override
   public PoseSnapshot postmortem$getLastLivingPose() {
      return this.postmortem$lastLivingPose;
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void postmortem$recordLivingPose(CallbackInfo ci) {
      Player self = (Player)(Object)this;
      if (!self.isAlive()) {
         return;
      }

      this.postmortem$trackLimbs(self);
      this.postmortem$turnBody(self);
      this.postmortem$lastLivingPose = PoseSnapshot.of(
         self, this.postmortem$bodyYaw, this.postmortem$limbProgress, Math.min(this.postmortem$limbSpeed, 1.0F));
   }

   @Unique
   private void postmortem$turnBody(Player self) {
      if (self.level().isClientSide() || self.isPassenger() || Float.isNaN(this.postmortem$bodyYaw)) {
         this.postmortem$bodyYaw = self.yBodyRot;
         return;
      }

      float target = this.postmortem$bodyYaw;
      double dx = this.postmortem$stepX;
      double dz = this.postmortem$stepZ;
      if (dx * dx + dz * dz > 0.0025000002F) {
         float walk = (float) Mth.atan2(dz, dx) * (180.0F / (float) Math.PI) - 90.0F;
         float facing = Mth.abs(Mth.wrapDegrees(self.getYRot()) - walk);
         target = 95.0F < facing && facing < 265.0F ? walk - 180.0F : walk;
      }

      //? if >=26.3 {
      /*boolean swinging = self.getSwingAnimation(1.0F) > 0.0F;
      *///?} else
      boolean swinging = self.attackAnim > 0.0F;
      if (swinging) {
         target = self.getYRot();
      }

      this.postmortem$bodyYaw += Mth.wrapDegrees(target - this.postmortem$bodyYaw) * 0.3F;
      float head = Mth.wrapDegrees(self.getYRot() - this.postmortem$bodyYaw);
      float max = self.isBlocking() ? 15.0F : 50.0F;
      if (Math.abs(head) > max) {
         this.postmortem$bodyYaw += head - Math.signum(head) * max;
      }
   }

   @Unique
   private void postmortem$trackLimbs(Player self) {
      double dx = self.getX() - this.postmortem$lastX;
      double dz = self.getZ() - this.postmortem$lastZ;
      this.postmortem$lastX = self.getX();
      this.postmortem$lastZ = self.getZ();

      if (!this.postmortem$hasLastPosition) {
         this.postmortem$hasLastPosition = true;
         return;
      }

      this.postmortem$stepX = dx;
      this.postmortem$stepZ = dz;

      if (self.isPassenger()) {
         this.postmortem$limbSpeed = 0.0F;
         this.postmortem$limbProgress = 0.0F;
         return;
      }

      float step = (float) Math.sqrt(dx * dx + dz * dz);
      float target = Math.min(step * 4.0F, 1.0F);
      this.postmortem$limbSpeed += (target - this.postmortem$limbSpeed) * 0.4F;
      this.postmortem$limbProgress += this.postmortem$limbSpeed;
   }
}
