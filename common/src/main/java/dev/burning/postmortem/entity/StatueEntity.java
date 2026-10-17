package dev.burning.postmortem.entity;

import dev.burning.postmortem.Postmortem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class StatueEntity extends LivingEntity {

   private static final EntityDataAccessor<Float> LIMB_ANGLE =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> LIMB_AMPLITUDE =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> HAND_SWING =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> LEANING =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.FLOAT);

   private static final EntityDataAccessor<ItemStack> BACKSLOT =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.ITEM_STACK);

   private static final EntityDataAccessor<ItemStack> BELTSLOT =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.ITEM_STACK);

   private static final EntityDataAccessor<Integer> VARIANT =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.INT);

   private static final EntityDataAccessor<Boolean> SEATED =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.BOOLEAN);

   private static final EntityDataAccessor<Boolean> WAXED =
      SynchedEntityData.defineId(StatueEntity.class, EntityDataSerializers.BOOLEAN);

   protected StatueEntity(EntityType<? extends StatueEntity> type, Level world) {
      super(type, world);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(LIMB_ANGLE, 0.0F);
      builder.define(LIMB_AMPLITUDE, 0.0F);
      builder.define(HAND_SWING, 0.0F);
      builder.define(LEANING, 0.0F);
      builder.define(BACKSLOT, ItemStack.EMPTY);
      builder.define(BELTSLOT, ItemStack.EMPTY);
      builder.define(VARIANT, StatueVariant.DEFAULT.ordinal());
      builder.define(SEATED, false);
      builder.define(WAXED, false);
   }

   public boolean isWaxed() {
      return this.entityData.get(WAXED);
   }

   public void setWaxed(boolean waxed) {
      this.entityData.set(WAXED, waxed);
   }

   protected InteractionResult tryWaxing(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);

      if (!this.isWaxed() && held.is(Items.HONEYCOMB)) {
         this.setWaxed(true);
         held.consume(1, player);
         this.playWaxEffect(SoundEvents.HONEYCOMB_WAX_ON, ParticleTypes.WAX_ON);
         return InteractionResult.SUCCESS;
      }

      if (this.isWaxed() && held.is(ItemTags.AXES)) {
         this.setWaxed(false);
         held.hurtAndBreak(1, player, hand);
         this.playWaxEffect(SoundEvents.AXE_WAX_OFF, ParticleTypes.WAX_OFF);
         return InteractionResult.SUCCESS;
      }

      return null;
   }

   protected InteractionResult refuseWaxed(Player player) {
      Postmortem.overlay(player, Component.translatable("message.postmortem.statue_waxed"));
      this.level().playSound(
         null,
         this.getX(),
         this.getY(),
         this.getZ(),
         SoundEvents.WAXED_SIGN_INTERACT_FAIL,
         SoundSource.NEUTRAL,
         1.0F,
         1.0F);
      return InteractionResult.CONSUME;
   }

   private void playWaxEffect(SoundEvent sound, SimpleParticleType particle) {
      Level world = this.level();
      world.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);

      if (world instanceof ServerLevel serverWorld) {
         serverWorld.sendParticles(
            particle, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 12, 0.3, 0.5, 0.3, 0.0);
      }
   }

   public ItemStack getBackslotStack() {
      return this.entityData.get(BACKSLOT);
   }

   public void setBackslotStack(ItemStack stack) {
      this.entityData.set(BACKSLOT, stack.copy());
   }

   public ItemStack getBeltslotStack() {
      return this.entityData.get(BELTSLOT);
   }

   public void setBeltslotStack(ItemStack stack) {
      this.entityData.set(BELTSLOT, stack.copy());
   }

   public void applyPose(PoseSnapshot snapshot) {
      this.setPose(snapshot.pose());
      this.setXRot(snapshot.pitch());
      this.setYRot(snapshot.bodyYaw());
      this.setYHeadRot(snapshot.headYaw());
      this.setYBodyRot(snapshot.bodyYaw());
      this.setLimbAngle(snapshot.limbAngle());
      this.setLimbAmplitude(snapshot.limbAmplitude());
      this.setHandSwing(snapshot.handSwing());
      this.setLeaning(snapshot.leaning());
      this.setSeated(snapshot.riding());
   }

   public float getLimbAngle() {
      return this.entityData.get(LIMB_ANGLE);
   }

   public void setLimbAngle(float value) {
      this.entityData.set(LIMB_ANGLE, value);
   }

   public float getLimbAmplitude() {
      return this.entityData.get(LIMB_AMPLITUDE);
   }

   public void setLimbAmplitude(float value) {
      this.entityData.set(LIMB_AMPLITUDE, value);
   }

   public float getHandSwing() {
      return this.entityData.get(HAND_SWING);
   }

   public void setHandSwing(float value) {
      this.entityData.set(HAND_SWING, value);
   }

   public StatueVariant getVariant() {
      return StatueVariant.byOrdinal(this.entityData.get(VARIANT));
   }

   public void setVariant(StatueVariant variant) {
      this.entityData.set(VARIANT, variant.ordinal());
   }

   protected InteractionResult tryVariantCycle(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (!player.isShiftKeyDown() || !held.is(ItemTags.PICKAXES)) {
         return null;
      }

      if (this.isWaxed()) {
         return this.refuseWaxed(player);
      }

      StatueVariant next = this.getVariant().next();
      this.setVariant(next);
      held.hurtAndBreak(1, player, hand);
      Postmortem.overlay(player, 
         Component.translatable("message.postmortem.variant", Component.translatable(next.translationKey())));
      this.level().playSound(
         null, this.getX(), this.getY(), this.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.NEUTRAL, 0.7F, 1.4F);
      return InteractionResult.SUCCESS;
   }

   public boolean isSeated() {
      return this.entityData.get(SEATED);
   }

   public void setSeated(boolean value) {
      this.entityData.set(SEATED, value);
   }

   public float getLeaning() {
      return this.entityData.get(LEANING);
   }

   public void setLeaning(float value) {
      this.entityData.set(LEANING, value);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      view.store("StatuePose", Pose.CODEC, this.getPose());
      view.putFloat("LimbAngle", this.getLimbAngle());
      view.putFloat("LimbAmplitude", this.getLimbAmplitude());
      view.putFloat("HandSwing", this.getHandSwing());
      view.putFloat("Leaning", this.getLeaning());
      if (!this.getBackslotStack().isEmpty()) {
         view.store("Backslot", ItemStack.CODEC, this.getBackslotStack());
      }
      if (!this.getBeltslotStack().isEmpty()) {
         view.store("Beltslot", ItemStack.CODEC, this.getBeltslotStack());
      }
      if (this.getVariant() != StatueVariant.DEFAULT) {
         view.putString("Variant", this.getVariant().getSerializedName());
      }
      if (this.isSeated()) {
         view.putBoolean("Seated", true);
      }
      if (this.isWaxed()) {
         view.putBoolean("Waxed", true);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      view.read("StatuePose", Pose.CODEC).ifPresent(this::setPose);
      this.setLimbAngle(view.getFloatOr("LimbAngle", 0.0F));
      this.setLimbAmplitude(view.getFloatOr("LimbAmplitude", 0.0F));
      this.setHandSwing(view.getFloatOr("HandSwing", 0.0F));
      this.setLeaning(view.getFloatOr("Leaning", 0.0F));
      this.setBackslotStack(view.read("Backslot", ItemStack.CODEC).orElse(ItemStack.EMPTY));
      this.setBeltslotStack(view.read("Beltslot", ItemStack.CODEC).orElse(ItemStack.EMPTY));
      this.setVariant(StatueVariant.byName(view.getStringOr("Variant", "")));
      this.setSeated(view.getBooleanOr("Seated", false));
      this.setWaxed(view.getBooleanOr("Waxed", false));
   }

   public static AttributeSupplier.Builder createStatueAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0);
   }

   @Override
   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   public boolean canUseSlot(EquipmentSlot slot) {
      return slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE;
   }

   // Same as vanilla, but without calling super: Enchancement's Veil hooks the
   // return of Entity.shouldRender and culls any non-player wearing it past
   // ~11-16 blocks. A statue in Veil armor should stay visible like any other.
   @Override
   public boolean shouldRender(double x, double y, double z) {
      return this.shouldRenderAtSqrDistance(this.distanceToSqr(x, y, z));
   }

   @Override
   public void travel(Vec3 movementInput) {
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void doPush(Entity entity) {
   }

   @Override
   protected void pushEntities() {
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean canBreatheUnderwater() {
      return true;
   }

   @Override
   public boolean isAffectedByPotions() {
      return false;
   }

   @Override
   public boolean ignoreExplosion(Explosion explosion) {
      return true;
   }
}
