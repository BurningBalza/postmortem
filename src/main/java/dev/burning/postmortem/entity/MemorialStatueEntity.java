package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.compat.CarrySlot;
import dev.burning.postmortem.compat.TrinketsCompat.TrinketSlot;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.death.MemorialRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class MemorialStatueEntity extends StatueEntity {
   public static final int NO_SLOT = -1;

   private static final long UNKNOWN_BIRTH = Long.MIN_VALUE;

   public record StoredStack(int slot, ItemStack stack, Optional<TrinketSlot> trinket, Optional<CarrySlot> carried) {
      public static final Codec<StoredStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.INT.fieldOf("Slot").forGetter(StoredStack::slot),
         ItemStack.CODEC.fieldOf("Item").forGetter(StoredStack::stack),
         TrinketSlot.CODEC.optionalFieldOf("Trinket").forGetter(StoredStack::trinket),
         CarrySlot.CODEC.optionalFieldOf("Carried").forGetter(StoredStack::carried)
      ).apply(instance, StoredStack::new));
   }

   // Only holds stacks until the statue is registered; after that they live in MemorialRegistry.
   private final List<StoredStack> pending = new ArrayList<>();

   private UUID ownerUuid;

   private long birthTime = UNKNOWN_BIRTH;

   private boolean checkedRegistry;

   public MemorialStatueEntity(EntityType<? extends MemorialStatueEntity> type, Level world) {
      super(type, world);
      this.setInvulnerable(true);
   }

   public void setOwner(Player player) {
      this.ownerUuid = player.getUUID();
      this.setCustomName(Component.translatable("entity.postmortem.memorial_statue.named", player.getName()));
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

      this.pending.add(new StoredStack(originSlot, stack, Optional.empty(), Optional.empty()));

      if (displaySlot != null && this.canUseSlot(displaySlot) && this.getItemBySlot(displaySlot).isEmpty()) {
         this.setItemSlot(displaySlot, stack.copy());
      }
   }

   public void storeTrinket(ItemStack stack, TrinketSlot slot) {
      if (stack.isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(NO_SLOT, stack, Optional.of(slot), Optional.empty()));
   }

   public void storeCarried(ItemStack stack, CarrySlot slot) {
      if (stack.isEmpty()) {
         return;
      }

      this.pending.add(new StoredStack(NO_SLOT, stack, Optional.empty(), Optional.of(slot)));
   }

   /** Moves the pending stacks into the registry. Call once the statue has its owner and position. */
   public void register(ServerLevel world) {
      MemorialRegistry.get(world.getServer()).put(this.getUUID(), new MemorialRegistry.Entry(
         this.ownerUuid, world.dimension(), this.blockPosition(), world.getGameTime(), this.pending));
      this.pending.clear();
   }

   @Override
   public void tick() {
      super.tick();

      if (this.level() instanceof ServerLevel world) {
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

      // A statue saved before the registry existed still carries its stacks.
      if (!this.pending.isEmpty() && this.ownerUuid != null) {
         this.register(world);
         if (PostmortemConfig.get().debug()) {
            Postmortem.LOGGER.info("[postmortem] moved statue {} into the memorial registry", this.getUUID());
         }
         return;
      }

      // Its items were recovered while it was unloaded, so what is left is an empty shell.
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
      return this.contents().isEmpty();
   }

   public int getStoredCount() {
      return this.contents().size();
   }

   public int giveTo(Player player) {
      int returned = restore(player, this.takeContents());

      this.clearDisplay();
      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
      this.discard();

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] returned {} stack(s) from statue {}", returned, this.getUUID());
      }

      return returned;
   }

   /** Deletes the statue along with the items it holds, returning how many stacks were destroyed. */
   public int destroy() {
      int destroyed = this.takeContents().size();
      this.discard();
      return destroyed;
   }

   /** Puts stacks back in the slots they came from where possible; shared by statues and orphaned registry entries. */
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

         int slot = entry.slot();
         if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
            inventory.setItem(slot, stack);
         } else {
            displaced.add(stack);
         }
      }

      for (ItemStack stack : displaced) {
         inventory.placeItemBackInInventory(stack);
      }

      return returned;
   }

   private void clearDisplay() {
      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         if (this.canUseSlot(slot)) {
            this.setItemSlot(slot, ItemStack.EMPTY);
         }
      }
   }

   @Override
   public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (!this.isOwner(player)) {
         player.sendOverlayMessage(Component.translatable("message.postmortem.not_your_statue"));
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
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      if (this.isRemoved() || !this.canBeBrokenBy(source)) {
         return false;
      }

      this.breakApart(world);
      return true;
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
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
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      if (!this.pending.isEmpty()) {
         view.store("Stored", StoredStack.CODEC.listOf(), this.pending);
      }
      view.putLong("BirthTime", this.birthTime == UNKNOWN_BIRTH ? this.level().getGameTime() : this.birthTime);
      if (this.ownerUuid != null) {
         view.store("Owner", UUIDUtil.CODEC, this.ownerUuid);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.pending.clear();
      view.read("Stored", StoredStack.CODEC.listOf()).ifPresent(this.pending::addAll);
      this.ownerUuid = view.read("Owner", UUIDUtil.CODEC).orElse(null);
      this.birthTime = view.getLongOr("BirthTime", this.level().getGameTime());
   }
}
