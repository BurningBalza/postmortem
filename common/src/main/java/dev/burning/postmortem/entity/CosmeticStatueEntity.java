package dev.burning.postmortem.entity;

import com.mojang.authlib.GameProfile;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.BackSlots;
import java.util.UUID;
import java.util.function.Consumer;
import dev.burning.postmortem.platform.Platform;
//? if >=1.20.5
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import dev.burning.postmortem.legacy.ValueInput;
import dev.burning.postmortem.legacy.ValueOutput;
import net.minecraft.nbt.CompoundTag;
*///?}
import net.minecraft.world.phys.Vec3;

public class CosmeticStatueEntity extends StatueEntity {
   private static final EntityDataAccessor<StatuePose> LIMB_POSE =
      SynchedEntityData.defineId(CosmeticStatueEntity.class, StatuePose.TRACKED_HANDLER);

   public CosmeticStatueEntity(EntityType<? extends CosmeticStatueEntity> type, Level world) {
      super(type, world);
   }

   @Override
   //? if >=1.20.5 {
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
   //?} else {
   /*protected void defineSynchedData() {
      super.defineSynchedData();
      SynchedEntityData builder = this.entityData;
   *///?}
      builder.define(LIMB_POSE, StatuePose.DEFAULT);
   }

   public StatuePose getLimbPose() {
      return this.entityData.get(LIMB_POSE);
   }

   public void setLimbPose(StatuePose pose) {
      this.entityData.set(LIMB_POSE, pose);
   }

   private static final EquipmentSlot[] UNDRESS_ORDER = {
      EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };

   private static final int BEHIND_ARC_DEGREES = 120;
   private static final double BEHIND_COSINE = -Math.cos(Math.toRadians(BEHIND_ARC_DEGREES / 2.0));

   public static final double EDIT_RANGE_SQUARED = 12.0 * 12.0;

   private static final long BREAK_WINDOW_TICKS = 5L;
   private static final byte HIT_EVENT = 32;

   private long lastHit;

   private UUID skinOwner;

   private String skinTextures = "";

   private boolean skinScraped;

   private String pendingSkinName;

   @Override
   public void tick() {
      super.tick();

      if (!this.level().isClientSide()) {
         boolean shown = PostmortemConfig.get().namedStatueSkins() && !this.skinScraped;
         this.showSkin(shown ? this.skinOwner : null, this.skinTextures);
      }
   }

   @Override
   //? if >=26.1 {
   /*public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
   *///?} else
   public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      InteractionResult waxing = this.tryWaxing(player, hand);
      if (waxing != null) {
         return waxing;
      }

      InteractionResult chiselling = this.tryVariantCycle(player, hand);
      if (chiselling != null) {
         return chiselling;
      }

      InteractionResult scraping = this.tryScrapingSkin(player, hand);
      if (scraping != null) {
         return scraping;
      }

      if (player.isShiftKeyDown()) {
         if (this.isWaxed()) {
            return this.refuseWaxed(player);
         }

         if (BackSlots.belt() && this.isBehind(player)) {
            return this.swap(player, hand, this.getBeltslotStack(), this::setBeltslotStack, 1.0F);
         }

         this.openPoseMenu(player);
         return InteractionResult.SUCCESS;
      }

      InteractionResult renamed = this.renameFrom(player, hand);
      if (renamed != null) {
         return renamed;
      }

      if (this.isWaxed()) {
         return this.refuseWaxed(player);
      }

      if (BackSlots.back() && this.isBehind(player)) {
         return this.swap(player, hand, this.getBackslotStack(), this::setBackslotStack, 1.2F);
      }

      ItemStack held = player.getItemInHand(hand);
      EquipmentSlot slot = held.isEmpty() ? firstOccupiedSlot() : slotFor(held);
      if (slot == null) {
         return InteractionResult.PASS;
      }

      return this.swap(player, hand, this.getItemBySlot(slot), stack -> this.setItemSlot(slot, stack), 1.4F);
   }

   private InteractionResult renameFrom(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (!held.is(Items.NAME_TAG)) {
         return null;
      }

      //? if >=1.20.5 {
      Component name = held.get(DataComponents.CUSTOM_NAME);
      //?} else
      //Component name = held.hasCustomHoverName() ? held.getHoverName() : null;
      if (name == null) {
         return InteractionResult.PASS;
      }

      this.setCustomName(name);
      this.lookUpSkin(name.getString());
      //? if >=1.20.5 {
      held.consume(1, player);
      //?} else {
      /*if (!player.getAbilities().instabuild) {
         held.shrink(1);
      }
      *///?}
      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 0.7F, 1.6F);
      return InteractionResult.SUCCESS;
   }

   private void lookUpSkin(String name) {
      this.skinOwner = null;
      this.skinTextures = "";
      this.skinScraped = false;
      this.pendingSkinName = null;

      if (!PostmortemConfig.get().namedStatueSkins()
         || !ProfileLookup.isPlayerName(name)
         || !(this.level() instanceof ServerLevel world)) {
         return;
      }

      this.pendingSkinName = name;
      MinecraftServer server = world.getServer();
      ProfileLookup.byName(server, name).whenComplete((found, error) -> server.execute(() -> {
         if (this.isRemoved() || !name.equals(this.pendingSkinName)) {
            return;
         }
         this.pendingSkinName = null;

         if (error != null || found == null || found.isEmpty()) {
            if (PostmortemConfig.get().debug()) {
               Postmortem.LOGGER.info("[postmortem] no player named {} for statue {}", name, this.getUUID(), error);
            }
            return;
         }

         GameProfile profile = found.get();
         this.skinOwner = ProfileLookup.idOf(profile);
         this.skinTextures = StatueEntity.texturesOf(profile);
      }));
   }

   private InteractionResult tryScrapingSkin(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (!player.isShiftKeyDown() || !held.is(ItemTags.AXES) || this.getSkinOwner() == null) {
         return null;
      }

      if (this.isWaxed()) {
         return this.refuseWaxed(player);
      }

      this.skinScraped = true;
      this.showSkin(null, "");
      //? if >=1.21.9 {
      held.hurtAndBreak(1, player, hand);
      //?} elif >=1.20.5 {
      /*held.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
      *///?} else
      //held.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
      //? if >=26.3 {
      /*this.level().playSound(
         null, this.getX(), this.getY(), this.getZ(), SoundEvents.AXE_STRIP.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
      *///?} else {
      this.level().playSound(
         null, this.getX(), this.getY(), this.getZ(), SoundEvents.AXE_STRIP, SoundSource.NEUTRAL, 1.0F, 1.0F);
      //?}
      return InteractionResult.SUCCESS;
   }

   private boolean isBehind(Player player) {
      double dx = player.getX() - this.getX();
      double dz = player.getZ() - this.getZ();
      double distanceSquared = dx * dx + dz * dz;
      if (distanceSquared < 1.0E-4) {
         return false;
      }

      float yaw = this.getVisualRotationYInDegrees() * Mth.DEG_TO_RAD;
      double facingX = -Mth.sin(yaw);
      double facingZ = Mth.cos(yaw);
      double alignment = (facingX * dx + facingZ * dz) / Math.sqrt(distanceSquared);
      return alignment < BEHIND_COSINE;
   }

   private InteractionResult swap(
      Player player, InteractionHand hand, ItemStack current, Consumer<ItemStack> setter, float pitch) {
      ItemStack held = player.getItemInHand(hand);

      if (held.isEmpty()) {
         if (current.isEmpty()) {
            return InteractionResult.PASS;
         }
         setter.accept(ItemStack.EMPTY);
         player.setItemInHand(hand, current);
      } else if (held.getCount() <= 1) {
         setter.accept(held.copy());
         player.setItemInHand(hand, current);
      } else if (current.isEmpty()) {
         setter.accept(held.split(1));
      } else {
         return InteractionResult.PASS;
      }

      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 0.8F, pitch);
      return InteractionResult.SUCCESS;
   }

   private EquipmentSlot firstOccupiedSlot() {
      for (EquipmentSlot slot : UNDRESS_ORDER) {
         if (this.canUseSlot(slot) && !this.getItemBySlot(slot).isEmpty()) {
            return slot;
         }
      }

      return null;
   }

   private EquipmentSlot slotFor(ItemStack stack) {
      EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
      if (this.canUseSlot(slot) && slot != EquipmentSlot.MAINHAND) {
         return slot;
      }

      return this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() || !this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()
         ? EquipmentSlot.MAINHAND
         : EquipmentSlot.OFFHAND;
   }

   @Override
   //? if >=1.21.2 {
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
   //?} else {
   /*public boolean hurt(DamageSource source, float amount) {
      if (!(this.level() instanceof ServerLevel world)) {
         return false;
      }
   *///?}
      if (this.isRemoved() || this.isWaxed()) {
         return false;
      }

      if (source.is(DamageTypeTags.IS_FIRE)
         || source.is(DamageTypes.CACTUS)
         || source.is(DamageTypeTags.IS_DROWNING)) {
         return false;
      }

      if (source.is(DamageTypeTags.IS_EXPLOSION)
         || source.is(DamageTypeTags.IS_PROJECTILE)
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.breakApart(world, true);
         return true;
      }

      //? if >=1.20.5 {
      if (!source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
      //?} else
      //if (!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
         return false;
      }

      if (source.getEntity() instanceof Player player && !player.getAbilities().mayBuild) {
         return false;
      }

      if (source.isCreativePlayer()) {
         this.breakApart(world, false);
         return true;
      }

      long time = world.getGameTime();
      if (time - this.lastHit > BREAK_WINDOW_TICKS) {
         this.lastHit = time;
         world.broadcastEntityEvent(this, HIT_EVENT);
         world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.6F, 1.0F);
      } else {
         this.breakApart(world, true);
      }
      return true;
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == HIT_EVENT) {
         this.lastHit = this.level().getGameTime();
      } else {
         super.handleEntityEvent(id);
      }
   }

   public float sinceHit(float tickDelta) {
      return this.level().getGameTime() - this.lastHit + tickDelta;
   }

   private void openPoseMenu(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return;
      }

      Platform.INSTANCE.sendToPlayer(serverPlayer, new PostmortemRegistry.OpenPoseScreen(this.getId()));
      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.9F, 1.5F);
   }

   private void breakApart(ServerLevel world, boolean dropStatue) {
      if (dropStatue) {
         Containers.dropItemStack(world, this.getX(), this.getY(), this.getZ(), new ItemStack(PostmortemRegistry.COSMETIC_STATUE_ITEM.get()));
      }

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (this.canUseSlot(slot)) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
               this.setItemSlot(slot, ItemStack.EMPTY);
               Containers.dropItemStack(world, this.getX(), this.getY(), this.getZ(), stack);
            }
         }
      }

      ItemStack backslot = this.getBackslotStack();
      if (!backslot.isEmpty()) {
         this.setBackslotStack(ItemStack.EMPTY);
         Containers.dropItemStack(world, this.getX(), this.getY(), this.getZ(), backslot);
      }

      ItemStack beltslot = this.getBeltslotStack();
      if (!beltslot.isEmpty()) {
         this.setBeltslotStack(ItemStack.EMPTY);
         Containers.dropItemStack(world, this.getX(), this.getY(), this.getZ(), beltslot);
      }

      world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
      this.discard();
   }

   @Override
   public void setCustomName(Component name) {
      super.setCustomName(name);
      this.setCustomNameVisible(name != null);
   }

   @Override
   //? if >=1.21.6 {
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
   //?} else {
   /*public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      ValueOutput view = ValueOutput.of(tag, this);
   *///?}

      StatuePose pose = this.getLimbPose();
      if (!pose.isDefault()) {
         view.store("LimbPose", StatuePose.CODEC, pose);
      }

      if (this.skinOwner != null) {
         view.store("SkinOwner", UUIDUtil.CODEC, this.skinOwner);
      }
      if (!this.skinTextures.isEmpty()) {
         view.putString("SkinTextures", this.skinTextures);
      }
      if (this.skinScraped) {
         view.putBoolean("SkinScraped", true);
      }
   }

   @Override
   //? if >=1.21.6 {
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
   //?} else {
   /*public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      ValueInput view = ValueInput.of(tag, this);
   *///?}
      this.setLimbPose(view.read("LimbPose", StatuePose.CODEC).orElse(StatuePose.DEFAULT));
      this.skinOwner = view.read("SkinOwner", UUIDUtil.CODEC).orElse(null);
      this.skinTextures = view.getStringOr("SkinTextures", "");
      this.skinScraped = view.getBooleanOr("SkinScraped", false);
   }

   @Override
   //? if >=1.20.3 {
   public boolean ignoreExplosion(Explosion explosion) {
   //?} else
   //public boolean ignoreExplosion() {
      return false;
   }
}
