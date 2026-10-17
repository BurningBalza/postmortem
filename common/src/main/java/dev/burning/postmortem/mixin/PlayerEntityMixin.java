package dev.burning.postmortem.mixin;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.death.DeathCapture;
import dev.burning.postmortem.entity.PoseSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = 1100)
public abstract class PlayerEntityMixin implements PoseSnapshot.Holder {
   @Shadow
   protected abstract void destroyVanishingCursedItems();

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
      this.postmortem$lastLivingPose = PoseSnapshot.of(
         self, this.postmortem$limbProgress, Math.min(this.postmortem$limbSpeed, 1.0F));
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

   //? if >=1.21.2 {
   @Inject(method = "dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), cancellable = true)
   private void postmortem$captureIntoStatue(ServerLevel world, CallbackInfo ci) {
   //?} else {
   /*// Before 1.21.2 the drop runs without being handed the level.
   @Inject(method = "dropEquipment()V", at = @At("HEAD"), cancellable = true)
   private void postmortem$captureIntoStatue(CallbackInfo ci) {
      if (!(((Player)(Object)this).level() instanceof ServerLevel world)) {
         return;
      }
   *///?}
      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] death drop intercepted; keepInventory={}",
            world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
      }

      if (world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         return;
      }

      this.destroyVanishingCursedItems();

      if (DeathCapture.capture((Player)(Object)this, world)) {
         ci.cancel();
      }
   }
}
