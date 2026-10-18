package dev.burning.postmortem.death;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.BackslotCompat;
import dev.burning.postmortem.compat.CarrySlot;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
import dev.burning.postmortem.compat.GraveMods;
import dev.burning.postmortem.compat.PehkuiCompat;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlots;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import dev.burning.postmortem.entity.PoseSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
//? if >=1.21.2
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

public final class DeathCapture {
   private DeathCapture() {
   }

   /** Items tagged {@code postmortem:kept_on_death} stay with the player instead of going into the statue. */
   public static boolean isKept(ItemStack stack) {
      return !stack.isEmpty() && stack.is(PostmortemRegistry.KEPT_ON_DEATH);
   }

   /** Called by the loader when a player respawns, with {@code alive} false when the respawn follows a death. */
   public static void carryKeptItems(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
      if (alive
         || GraveMods.present()
         || oldPlayer.isSpectator()
         //? if >=1.21.6 {
         || newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         //?} else
         //|| newPlayer.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         return;
      }

      Inventory left = oldPlayer.getInventory();
      Inventory fresh = newPlayer.getInventory();
      int carried = 0;

      for (int slot = 0; slot < left.getContainerSize(); slot++) {
         ItemStack stack = left.getItem(slot);
         if (!isKept(stack)) {
            continue;
         }

         left.setItem(slot, ItemStack.EMPTY);
         carried++;

         if (slot < fresh.getContainerSize() && fresh.getItem(slot).isEmpty()) {
            fresh.setItem(slot, stack);
         } else {
            //? if >=26.3 {
            /*fresh.placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else
            fresh.placeItemBackInInventory(stack);
         }
      }

      if (PostmortemConfig.get().debug() && carried > 0) {
         Postmortem.LOGGER.info("[postmortem] carried {} kept item(s) through {}'s respawn", carried, newPlayer.getName().getString());
      }
   }

   // Items taken from other mods' slots just before the death drop, waiting for capture() to keep them.
   private static final Map<UUID, List<WornItem>> PENDING_WORN = new HashMap<>();

   /**
    * Takes what the player wore in Curios or Accessories slots, before those mods drop it. Runs ahead of the death
    * drop because Accessories drops its items on Fabric just before vanilla's.
    */
   public static void takeWorn(ServerPlayer player, DamageSource source) {
      List<WornItem> worn = new ArrayList<>();
      for (WornSlots mod : WornSlots.loaded()) {
         worn.addAll(mod.takeDroppable(player, source));
      }
      if (!worn.isEmpty()) {
         PENDING_WORN.put(player.getUUID(), worn);
      }
   }

   public static boolean capture(Player player, ServerLevel world) {
      List<WornItem> worn = PENDING_WORN.remove(player.getUUID());
      if (worn == null) {
         worn = List.of();
      }

      //? if >=1.21.2 {
      MemorialStatueEntity statue = PostmortemRegistry.MEMORIAL_STATUE.get().create(world, EntitySpawnReason.TRIGGERED);
      //?} else
      //MemorialStatueEntity statue = PostmortemRegistry.MEMORIAL_STATUE.get().create(world);
      if (statue == null) {
         Postmortem.LOGGER.error("[postmortem] could not create a memorial statue; falling back to vanilla drops");
         for (WornItem item : worn) {
            //? if >=26.3 {
            /*player.drop(item.stack(), true, net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else
            player.drop(item.stack(), true);
         }
         return false;
      }

      statue.setOwner(player);

      Inventory inventory = player.getInventory();
      //? if >=1.21.5 {
      int selected = inventory.getSelectedSlot();
      //?} else
      //int selected = inventory.selected;
      List<Integer> taken = new ArrayList<>();
      boolean keepingAnything = false;
      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.isEmpty()) {
            continue;
         }

         // Left in place; carryKeptItems moves it into the respawned player's inventory.
         if (isKept(stack)) {
            keepingAnything = true;
            continue;
         }

         statue.store(stack.copy(), slot, displaySlotFor(slot, selected));
         taken.add(slot);
      }

      for (TrinketsCompat.Equipped trinket : TrinketsCompat.takeAll(player)) {
         statue.storeTrinket(trinket.stack(), trinket.slot());
      }

      statue.setWorn(WornItem.copies(worn));
      for (WornItem item : worn) {
         statue.storeWorn(item);
      }

      ItemStack backslot = BackslotCompat.get(player);
      boolean backslotIsLoot = !backslot.isEmpty() && BackslotCompat.dropsOnDeath();
      if (!backslot.isEmpty()) {
         statue.setBackslotStack(backslot);
      }
      if (backslotIsLoot) {
         statue.storeCarried(backslot.copy(), CarrySlot.BACKSLOT);
      }

      boolean amenitiesIsLoot = false;
      if (CombatAmenitiesCompat.isPresent()) {
         ItemStack amenitiesBack = CombatAmenitiesCompat.getBack(player);
         if (!amenitiesBack.isEmpty()) {
            statue.setBackslotStack(amenitiesBack);
            statue.storeCarried(amenitiesBack.copy(), CarrySlot.AMENITIES_BACK);
            amenitiesIsLoot = true;
         }

         ItemStack amenitiesBelt = CombatAmenitiesCompat.getBelt(player);
         statue.setBeltslotStack(amenitiesBelt);
         if (!amenitiesBelt.isEmpty()) {
            statue.storeCarried(amenitiesBelt.copy(), CarrySlot.AMENITIES_BELT);
            amenitiesIsLoot = true;
         }
      }

      if (statue.isEmpty()) {
         if (PostmortemConfig.get().debug()) {
            Postmortem.LOGGER.info("[postmortem] {} died carrying nothing; no statue placed", player.getName().getString());
         }

         // Cancelling vanilla drops here keeps a lone kept item from hitting the floor.
         return keepingAnything;
      }

      for (int slot : taken) {
         inventory.setItem(slot, ItemStack.EMPTY);
      }

      if (backslotIsLoot) {
         BackslotCompat.clear(player);
      }

      if (amenitiesIsLoot) {
         CombatAmenitiesCompat.clear(player);
      }

      PoseSnapshot pose = ((PoseSnapshot.Holder) player).postmortem$getLastLivingPose();

      BlockPos pos = SafePlacement.findFor(world, statue, player.blockPosition());
      //? if >=1.21.5 {
      statue.snapTo(Vec3.atBottomCenterOf(pos), pose.bodyYaw(), pose.pitch());
      //?} else
      //statue.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pose.bodyYaw(), pose.pitch());
      statue.applyPose(pose);
      PehkuiCompat.copyScales(player, statue);
      // Registered before spawning so the items are safe even if the entity never makes it into the world.
      statue.register(world);
      world.addFreshEntity(statue);

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info(
            "[postmortem] captured pose {} limbAngle={} limbAmplitude={} handSwing={} leaning={}",
            pose.pose(), pose.limbAngle(), pose.limbAmplitude(), pose.handSwing(), pose.leaning());
      }
      world.playSound(null, statue.getX(), statue.getY(), statue.getZ(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 1.0F, 0.7F);

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] {} died at {}; statue placed at {}", player.getName().getString(), player.blockPosition(), pos);
      }

      return true;
   }

   private static EquipmentSlot displaySlotFor(int slot, int selectedSlot) {
      if (slot == selectedSlot) {
         return EquipmentSlot.MAINHAND;
      }

      //? if >=1.21.5 {
      return Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
      //?} else {
      /*return switch (slot) {
         case 36 -> EquipmentSlot.FEET;
         case 37 -> EquipmentSlot.LEGS;
         case 38 -> EquipmentSlot.CHEST;
         case 39 -> EquipmentSlot.HEAD;
         case Inventory.SLOT_OFFHAND -> EquipmentSlot.OFFHAND;
         default -> null;
      };
      *///?}
   }
}
