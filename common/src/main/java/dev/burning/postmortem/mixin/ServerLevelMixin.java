package dev.burning.postmortem.mixin;

import dev.burning.postmortem.death.DeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
   @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
   private void postmortem$claimDeathDrop(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof ItemEntity item && DeathCapture.claim(item, (ServerLevel) (Object) this)) {
         cir.setReturnValue(true);
      }
   }
}
