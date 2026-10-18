package dev.burning.postmortem.mixin;

import dev.burning.postmortem.compat.GraveMods;
import dev.burning.postmortem.death.DeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Accessories drops its items at this same spot on Fabric; the lower priority puts this callback ahead of it, so what
// the player wore in other mods' slots is still there for the memorial to keep.
@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin {
   //? if >=1.21.2 {
   @Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V"))
   private void postmortem$takeWorn(ServerLevel world, DamageSource source, CallbackInfo ci) {
   //?} elif >=1.21 {
   /*@Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment()V"))
   private void postmortem$takeWorn(ServerLevel world, DamageSource source, CallbackInfo ci) {
   *///?} else {
   /*@Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment()V"))
   private void postmortem$takeWorn(DamageSource source, CallbackInfo ci) {
      // The death drop only runs on the server.
      ServerLevel world = (ServerLevel) ((LivingEntity) (Object) this).level();
   *///?}
      // The same conditions as the capture in PlayerEntityMixin, which takes these from DeathCapture.
      if ((Object) this instanceof ServerPlayer player
         && !GraveMods.present()
         && !world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         DeathCapture.takeWorn(player, source);
      }
   }
}
