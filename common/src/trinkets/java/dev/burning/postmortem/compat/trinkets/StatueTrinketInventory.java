package dev.burning.postmortem.compat.trinkets;

import dev.burning.postmortem.compat.WornItem;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class StatueTrinketInventory implements TrinketInventory {
   private final SlotType slotType;
   private final LivingEntity statue;
   private final NonNullList<ItemStack> stacks;
   private final NonNullList<ItemStack> cosmetics;
   private final WornItem item;

   StatueTrinketInventory(SlotType slotType, LivingEntity statue, WornItem item) {
      this.slotType = slotType;
      this.statue = statue;
      this.item = item;
      int size = Math.max(slotType.amount(), item.index() + 1);
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
      this.cosmetics = NonNullList.withSize(size, ItemStack.EMPTY);
      (item.cosmetic() ? this.cosmetics : this.stacks).set(item.index(), item.stack());
   }

   TrinketSlotAccess access() {
      return new TrinketSlotAccess(this, this.item.index(), this.item.cosmetic());
   }

   @Override
   public SlotType slotType() {
      return this.slotType;
   }

   @Override
   public TrinketSlotAccess getSlotAccess(int index) {
      return new TrinketSlotAccess(this, index, false);
   }

   @Override
   public TrinketSlotAccess getOrCreateSlotAccess(int index) {
      return this.getSlotAccess(index);
   }

   @Override
   public TrinketSlotAccess getCosmeticSlotAccess(int index) {
      return new TrinketSlotAccess(this, index, true);
   }

   @Override
   public TrinketSlotAccess getOrCreateCosmeticSlotAccess(int index) {
      return this.getCosmeticSlotAccess(index);
   }

   @Override
   public ItemStack getCosmeticItem(int index) {
      return this.isValidSlot(index) ? this.cosmetics.get(index) : ItemStack.EMPTY;
   }

   @Override
   public boolean setCosmeticItem(int index, ItemStack stack) {
      return false;
   }

   @Override
   public boolean hasCosmeticItems() {
      return this.item.cosmetic();
   }

   @Override
   public boolean isValidSlot(int index) {
      return index >= 0 && index < this.stacks.size();
   }

   @Override
   public TrinketAttachment getAttachment() {
      return TrinketsApi.getAttachment(this.statue);
   }

   @Override
   public boolean isVisible(int index) {
      return true;
   }

   @Override
   public void updateSlotCount() {
   }

   @Override
   public void addSlotCountModifier(AttributeModifier modifier) {
   }

   @Override
   public void removeSlotCountModifier(Identifier id) {
   }

   @Override
   public AttributeModifier getSlotCountModifier(Identifier id) {
      return null;
   }

   @Override
   @SuppressWarnings("removal")
   public void copyFrom(TrinketInventory other) {
   }

   @Override
   public int getContainerSize() {
      return this.stacks.size();
   }

   @Override
   public boolean isEmpty() {
      return this.stacks.stream().allMatch(ItemStack::isEmpty);
   }

   @Override
   public ItemStack getItem(int index) {
      return this.isValidSlot(index) ? this.stacks.get(index) : ItemStack.EMPTY;
   }

   @Override
   public ItemStack removeItem(int index, int count) {
      return ItemStack.EMPTY;
   }

   @Override
   public ItemStack removeItemNoUpdate(int index) {
      return ItemStack.EMPTY;
   }

   @Override
   public void setItem(int index, ItemStack stack) {
   }

   @Override
   public void setChanged() {
   }

   @Override
   public boolean stillValid(Player player) {
      return false;
   }

   @Override
   public void clearContent() {
   }
}
