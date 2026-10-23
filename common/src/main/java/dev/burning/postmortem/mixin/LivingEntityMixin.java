package dev.burning.postmortem.mixin;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.compat.GraveMods;
import dev.burning.postmortem.death.DeathCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
      ServerLevel world = (ServerLevel) ((LivingEntity) (Object) this).level();
   *///?}
      if ((Object) this instanceof ServerPlayer player && postmortem$capturesDeath(world)) {
         DeathCapture.takeWorn(player, source);
         DeathCapture.begin(player, world);
      }
   }

   //? if >=1.21.2 {
   @Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
   private void postmortem$placeMemorial(ServerLevel world, DamageSource source, CallbackInfo ci) {
   //?} elif >=1.21 {
   /*@Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment()V", shift = At.Shift.AFTER))
   private void postmortem$placeMemorial(ServerLevel world, DamageSource source, CallbackInfo ci) {
   *///?} else {
   /*@Inject(
      method = "dropAllDeathLoot",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment()V", shift = At.Shift.AFTER))
   private void postmortem$placeMemorial(DamageSource source, CallbackInfo ci) {
      ServerLevel world = (ServerLevel) ((LivingEntity) (Object) this).level();
   *///?}
      if ((Object) this instanceof ServerPlayer player) {
         DeathCapture.finish(player, world);
      }
   }

   @Unique
   private static boolean postmortem$capturesDeath(ServerLevel world) {
      if (GraveMods.present()) {
         return false;
      }

      boolean keepInventory = world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] death drop intercepted; keepInventory={}", keepInventory);
      }

      return !keepInventory;
   }
}
