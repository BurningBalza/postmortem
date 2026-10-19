package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.compat.CarrySlot;
import dev.burning.postmortem.compat.TrinketsCompat.TrinketSlot;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlot;
import dev.burning.postmortem.compat.WornSlots;
import dev.burning.postmortem.death.MemorialRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class MemorialStatueEntity extends StatueEntity {
   public static final int NO_SLOT = -1;

   private static final long UNKNOWN_BIRTH = Long.MIN_VALUE;

   public record StoredStack(
      int slot, ItemStack stack, Optional<TrinketSlot> trinket, Optional<CarrySlot> carried, Optional<WornSlot> worn) {
      public static final Codec<StoredStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.INT.fieldOf("Slot").forGetter(StoredStack::slot),
         ItemStack.CODEC.fieldOf("Item").forGetter(StoredStack::stack),
         TrinketSlot.CODEC.optionalFieldOf("Trinket").forGetter(StoredStack::trinket),
         CarrySlot.CODEC.optionalFieldOf("Carried").forGetter(StoredStack::carried),
         WornSlot.CODEC.optionalFieldOf("Worn").forGetter(StoredStack::worn)
      ).apply(instance, StoredStack::new));
   }

   private final List<StoredStack> pending = new ArrayList<>();

   private int pendingExperience;

   private String skinTextures = "";

   private static final EntityDataAccessor<String> SKIN_OWNER =
      SynchedEntityData.defineId(MemorialStatueEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> SKIN_TEXTURES =
      SynchedEntityData.defineId(MemorialStatueEntity.class, EntityDataSerializers.STRING);

   private UUID ownerUuid;

   private long birthTime = UNKNOWN_BIRTH;

   private boolean checkedRegistry;

   public MemorialStatueEntity(EntityType<? extends MemorialStatueEntity> type, Level world) {
      super(type, world);
      //? if >=26.3 {
      /*this.setPermanentlyInvulnerable(true);
      *///?} else
      this.setInvulnerable(true);
   }

   public void setOwner(Player player) {
      this.ownerUuid = player.getUUID();
      this.setCustomName(Component.translatable("entity.postmortem.memorial_statue.named", player.getName()));
      //? if >=1.21.9 {
      this.skinTextures = player.getGameProfile().properties().get("textures").stream()
      //?} else
      //this.skinTextures = player.getGameProfile().getProperties().get("textures").stream()
         .findFirst()
         //? if >=1.20.2 {
         .map(property -> property.value())
         //?} else
         //.map(property -> property.getValue())
         .orElse("");
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
      builder.define(SKIN_OWNER, "");
      builder.define(SKIN_TEXTURES, "");
   }

   public UUID getSkinOwner() {
      String owner = this.entityData.get(SKIN_OWNER);
      if (owner.isEmpty()) {
         return null;
      }

      try {
         return UUID.fromString(owner);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   public String getSkinTextures() {
      return this.entityData.get(SKIN_TEXTURES);
   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public boolean isOwner(Player player) {
      return this.ownerUuid != null && this.ownerUuid.equals(player.getUUID());
   }

   public void store(ItemStack stack, int originSlot, EquipmentSlot displaySlot) {
      if (stack.isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(originSlot, stack, Optional.empty(), Optional.empty(), Optional.empty()));

      if (displaySlot != null && this.canUseSlot(displaySlot) && this.getItemBySlot(displaySlot).isEmpty()) {
         this.setItemSlot(displaySlot, stack.copy());
      }
   }

   public void storeTrinket(ItemStack stack, TrinketSlot slot) {
      if (stack.isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(NO_SLOT, stack, Optional.of(slot), Optional.empty(), Optional.empty()));
   }

   public void storeCarried(ItemStack stack, CarrySlot slot) {
      if (stack.isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(NO_SLOT, stack, Optional.empty(), Optional.of(slot), Optional.empty()));
   }

   public void storeWorn(WornItem worn) {
      if (worn.stack().isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(NO_SLOT, worn.stack(), Optional.empty(), Optional.empty(), Optional.of(worn.where())));
   }

   public void storeExperience(int points) {
      this.pendingExperience += Math.max(0, points);
   }

   public void register(ServerLevel world) {
      MemorialRegistry.get(world.getServer()).put(this.getUUID(), new MemorialRegistry.Entry(
         this.ownerUuid, world.dimension(), this.blockPosition(), world.getGameTime(), this.pending, this.pendingExperience));
      this.pending.clear();
      this.pendingExperience = 0;
   }

   @Override
   protected void releaseUnsupported(ServerLevel world, ItemStack stack) {
   }

   @Override
   public void tick() {
      super.tick();

      if (this.level() instanceof ServerLevel world) {
         boolean skin = PostmortemConfig.get().greyscaleSkins() && this.ownerUuid != null;
         this.entityData.set(SKIN_OWNER, skin ? this.ownerUuid.toString() : "");
         this.entityData.set(SKIN_TEXTURES, skin ? this.skinTextures : "");

         if (this.birthTime == UNKNOWN_BIRTH) {
            this.birthTime = world.getGameTime();
         }

         if (!this.checkedRegistry) {
            this.checkedRegistry = true;
            this.checkRegistry(world);
         }
      }
   }

   private void checkRegistry(ServerLevel world) {
      MemorialRegistry registry = MemorialRegistry.get(world.getServer());
      MemorialRegistry.Entry entry = registry.get(this.getUUID());
      if (entry != null) {
         if (!entry.dimension().equals(world.dimension()) || !entry.pos().equals(this.blockPosition())) {
            registry.put(this.getUUID(), entry.movedTo(world.dimension(), this.blockPosition()));
         }
         return;
      }

      if (!this.pending.isEmpty() && this.ownerUuid != null) {
         this.register(world);
         if (PostmortemConfig.get().debug()) {
            Postmortem.LOGGER.info("[postmortem] moved statue {} into the memorial registry", this.getUUID());
         }
         return;
      }

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] statue {} has no registry entry; crumbling it", this.getUUID());
      }
      world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
      this.discard();
   }

   private List<StoredStack> contents() {
      if (this.level() instanceof ServerLevel world) {
         MemorialRegistry.Entry entry = MemorialRegistry.get(world.getServer()).get(this.getUUID());
         if (entry != null) {
            return entry.stacks();
         }
      }

      return this.pending;
   }

   private List<StoredStack> takeContents() {
      List<StoredStack> taken = new ArrayList<>(this.pending);
      this.pending.clear();
      if (this.level() instanceof ServerLevel world) {
         taken.addAll(MemorialRegistry.get(world.getServer()).take(this.getUUID()));
      }

      return taken;
   }

   private int takeExperience() {
      int taken = this.pendingExperience;
      this.pendingExperience = 0;
      if (this.level() instanceof ServerLevel world) {
         taken += MemorialRegistry.get(world.getServer()).takeExperience(this.getUUID());
      }

      return taken;
   }

   private int storedExperience() {
      if (this.level() instanceof ServerLevel world) {
         MemorialRegistry.Entry entry = MemorialRegistry.get(world.getServer()).get(this.getUUID());
         if (entry != null) {
            return entry.experience();
         }
      }

      return this.pendingExperience;
   }

   public long getStandingTicks() {
      if (this.birthTime == UNKNOWN_BIRTH) {
         return 0L;
      }

      return Math.max(0L, this.level().getGameTime() - this.birthTime);
   }

   public long getTicksUntilBreakable() {
      long delay = PostmortemConfig.get().breakAfterTicks();
      if (delay < 0L) {
         return -1L;
      }

      return Math.max(0L, delay - this.getStandingTicks());
   }

   public boolean isBreakable() {
      return this.getTicksUntilBreakable() == 0L;
   }

   public boolean isEmpty() {
      return this.contents().isEmpty() && this.storedExperience() <= 0;
   }

   public int getStoredCount() {
      return this.contents().size();
   }

   public int giveTo(Player player) {
      int experience = this.takeExperience();
      int returned = restore(player, this.takeContents());
      if (experience > 0) {
         player.giveExperiencePoints(experience);
      }

      this.clearDisplay();
      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
      this.discard();

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] returned {} stack(s) from statue {}", returned, this.getUUID());
      }

      return returned;
   }

   public int destroy() {
      int destroyed = this.takeContents().size();
      this.discard();
      return destroyed;
   }

   public static int restore(Player player, List<StoredStack> stacks) {
      Inventory inventory = player.getInventory();
      List<ItemStack> displaced = new ArrayList<>();
      int returned = 0;

      for (StoredStack entry : stacks) {
         ItemStack stack = entry.stack();
         if (stack.isEmpty()) {
            continue;
         }
         returned++;

         if (entry.trinket().isPresent() && TrinketsCompat.restore(player, entry.trinket().get(), stack)) {
            continue;
         }

         if (entry.carried().isPresent() && entry.carried().get().restore(player, stack)) {
            continue;
         }

         if (entry.worn().isPresent() && restoreWorn(player, entry.worn().get(), stack)) {
            continue;
         }

         int slot = entry.slot();
         if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
            inventory.setItem(slot, stack);
         } else {
            displaced.add(stack);
         }
      }

      for (ItemStack stack : displaced) {
         //? if >=26.3 {
         /*inventory.placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
         *///?} else
         inventory.placeItemBackInInventory(stack);
      }

      return returned;
   }

   private static boolean restoreWorn(Player player, WornSlot slot, ItemStack stack) {
      return WornSlots.byId(slot.mod()).map(mod -> mod.restore(player, slot, stack)).orElse(false);
   }

   private void clearDisplay() {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         if (this.canUseSlot(slot)) {
            this.setItemSlot(slot, ItemStack.EMPTY);
         }
      }
      this.setWorn(List.of());
   }

   @Override
   //? if >=26.1 {
   public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
   //?} else
   //public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (!this.isOwner(player)) {
         Postmortem.overlay(player, Component.translatable("message.postmortem.not_your_statue"));
         return InteractionResult.CONSUME;
      }

      InteractionResult waxing = this.tryWaxing(player, hand);
      if (waxing != null) {
         return waxing;
      }

      InteractionResult chiselling = this.tryVariantCycle(player, hand);
      if (chiselling != null) {
         return chiselling;
      }

      if (this.isWaxed()) {
         return this.refuseWaxed(player);
      }

      this.giveTo(player);
      return InteractionResult.SUCCESS;
   }

   @Override
   public boolean shouldShowName() {
      return this.hasCustomName();
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
      if (this.isRemoved() || !this.canBeBrokenBy(source)) {
         return false;
      }

      this.breakApart(world);
      return true;
   }

   @Override
   //? if >=1.21.2 {
   public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
   //?} else
   //public boolean isInvulnerableTo(DamageSource source) {
      return !this.canBeBrokenBy(source);
   }

   private boolean canBeBrokenBy(DamageSource source) {
      return !this.isWaxed()
         && source.getEntity() instanceof Player attacker
         && !this.isOwner(attacker)
         && this.isBreakable();
   }

   private void breakApart(ServerLevel world) {
      int spilled = 0;

      int experience = this.takeExperience();
      if (experience > 0) {
         ExperienceOrb.award(world, this.position(), experience);
      }

      for (StoredStack entry : this.takeContents()) {
         ItemStack stack = entry.stack();
         if (stack.isEmpty()) {
            continue;
         }

         spilled++;
         Containers.dropItemStack(world, this.getX(), this.getY(), this.getZ(), stack);
      }

      this.clearDisplay();
      world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
      this.discard();

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] statue {} was broken open, spilling {} stack(s)", this.getUUID(), spilled);
      }
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
      if (!this.pending.isEmpty()) {
         view.store("Stored", StoredStack.CODEC.listOf(), this.pending);
      }
      view.putLong("BirthTime", this.birthTime == UNKNOWN_BIRTH ? this.level().getGameTime() : this.birthTime);
      if (this.ownerUuid != null) {
         view.store("Owner", UUIDUtil.CODEC, this.ownerUuid);
      }
      if (!this.skinTextures.isEmpty()) {
         view.putString("SkinTextures", this.skinTextures);
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
      this.pending.clear();
      view.read("Stored", StoredStack.CODEC.listOf()).ifPresent(this.pending::addAll);
      this.ownerUuid = view.read("Owner", UUIDUtil.CODEC).orElse(null);
      this.skinTextures = view.getStringOr("SkinTextures", "");
      this.birthTime = view.getLongOr("BirthTime", this.level().getGameTime());
   }
}
