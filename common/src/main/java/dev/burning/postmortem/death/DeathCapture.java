package dev.burning.postmortem.death;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.ArsenalCompat;
import dev.burning.postmortem.compat.BackslotCompat;
import dev.burning.postmortem.compat.CarrySlot;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
import dev.burning.postmortem.compat.CosmeticArmorCompat;
import dev.burning.postmortem.compat.GraveMods;
import dev.burning.postmortem.compat.PehkuiCompat;
import dev.burning.postmortem.compat.TrinketsCompat;
import dev.burning.postmortem.compat.WornItem;
import dev.burning.postmortem.compat.WornSlots;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import dev.burning.postmortem.entity.PoseSnapshot;
import dev.burning.postmortem.platform.Platform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
//? if >=1.21.2
//import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

public final class DeathCapture {
   private DeathCapture() {
   }

   public static boolean isKept(ItemStack stack) {
      return !stack.isEmpty() && stack.is(PostmortemRegistry.KEPT_ON_DEATH);
   }

   public static void carryKeptItems(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
      if (!alive) {
         announceMemorial(newPlayer);
      }

      if (alive
         || GraveMods.present()
         || oldPlayer.isSpectator()
         //? if >=1.21.6 {
         /*|| newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         *///?} else
         || newPlayer.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
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

   private static final Map<UUID, List<WornItem>> PENDING_WORN = new HashMap<>();

   private static Capture active;

   public static void takeWorn(ServerPlayer player, DamageSource source) {
      List<WornItem> worn = new ArrayList<>();
      for (WornSlots mod : WornSlots.loaded()) {
         worn.addAll(mod.takeDroppable(player, source));
      }
      if (!worn.isEmpty()) {
         PENDING_WORN.put(player.getUUID(), worn);
      }
   }

   public static boolean isSoulbound(ItemStack stack) {
      //? if >=1.21 {
      /*return EnchantmentHelper.hasTag(stack, PostmortemRegistry.SOULBOUND);
      *///?} elif >=1.20.5 {
      /*return stack.getEnchantments().keySet().stream().anyMatch(enchantment -> enchantment.is(PostmortemRegistry.SOULBOUND));
      *///?} else {
      return EnchantmentHelper.getEnchantments(stack).keySet().stream().anyMatch(enchantment ->
         net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment).is(PostmortemRegistry.SOULBOUND));
      //?}
   }

   public static void begin(ServerPlayer player, ServerLevel world) {
      active = null;

      List<WornItem> worn = PENDING_WORN.remove(player.getUUID());
      if (worn == null) {
         worn = List.of();
      }

      //? if >=1.21.2 {
      /*MemorialStatueEntity statue = PostmortemRegistry.MEMORIAL_STATUE.get().create(world, EntitySpawnReason.TRIGGERED);
      *///?} else
      MemorialStatueEntity statue = PostmortemRegistry.MEMORIAL_STATUE.get().create(world);
      if (statue == null) {
         Postmortem.LOGGER.error("[postmortem] could not create a memorial statue; falling back to vanilla drops");
         for (WornItem item : worn) {
            //? if >=26.3 {
            /*player.drop(item.stack(), true, net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else
            player.drop(item.stack(), true);
         }
         return;
      }

      statue.setOwner(player);

      Inventory inventory = player.getInventory();
      //? if >=1.21.5 {
      /*int selected = inventory.getSelectedSlot();
      *///?} else
      int selected = inventory.selected;
      Capture capture = new Capture(player, world, statue, selected, Platform.INSTANCE.capturedDrops(player));
      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.isEmpty()) {
            continue;
         }

         if (isKept(stack)) {
            capture.kept.add(new InventorySlot(slot, stack, stack));
            inventory.setItem(slot, ItemStack.EMPTY);
            continue;
         }

         capture.unclaimed.add(new InventorySlot(slot, stack, stack.copy()));
      }

      List<WornItem> shown = new ArrayList<>(WornItem.copies(worn));
      for (TrinketsCompat.Equipped trinket : TrinketsCompat.takeAll(player)) {
         statue.storeTrinket(trinket.stack(), trinket.slot());
         shown.add(trinket.shown());
      }

      statue.setWorn(shown);
      for (WornItem item : worn) {
         statue.storeWorn(item);
      }

      ItemStack backslot = BackslotCompat.get(player);
      if (!backslot.isEmpty()) {
         statue.setBackslotStack(backslot);
         if (BackslotCompat.dropsOnDeath()) {
            statue.storeCarried(backslot.copy(), CarrySlot.BACKSLOT);
            BackslotCompat.clear(player);
         }
      }

      ItemStack arsenal = ArsenalCompat.get(player);
      if (!arsenal.isEmpty()) {
         statue.setBackslotStack(arsenal);
      }

      if (CombatAmenitiesCompat.isPresent()) {
         ItemStack amenitiesBack = CombatAmenitiesCompat.getBack(player);
         if (!amenitiesBack.isEmpty()) {
            statue.setBackslotStack(amenitiesBack);
            statue.storeCarried(amenitiesBack.copy(), CarrySlot.AMENITIES_BACK);
         }

         ItemStack amenitiesBelt = CombatAmenitiesCompat.getBelt(player);
         statue.setBeltslotStack(amenitiesBelt);
         if (!amenitiesBelt.isEmpty()) {
            statue.storeCarried(amenitiesBelt.copy(), CarrySlot.AMENITIES_BELT);
         }

         if (!amenitiesBack.isEmpty() || !amenitiesBelt.isEmpty()) {
            CombatAmenitiesCompat.clear(player);
         }
      }

      if (CosmeticArmorCompat.isPresent()) {
         capture.look.putAll(CosmeticArmorCompat.appearance(player));
         if (CosmeticArmorCompat.dropsOnDeath()) {
            for (int index = 0; index < 4; index++) {
               statue.storeCarried(CosmeticArmorCompat.take(player, index), CarrySlot.cosmetic(index));
            }
         }
      }

      active = capture;
   }

   public static boolean claim(ItemEntity item, ServerLevel world) {
      Capture capture = active;
      if (capture == null || capture.world != world) {
         return false;
      }

      if (world.getGameTime() != capture.tick) {
         active = null;
         return false;
      }

      return capture.claim(item.getItem());
   }

   public static void finish(ServerPlayer player, ServerLevel world) {
      Capture capture = active;
      if (capture == null || capture.player != player) {
         return;
      }
      active = null;

      Collection<ItemEntity> drops = Platform.INSTANCE.capturedDrops(player);
      if (drops != null) {
         for (Iterator<ItemEntity> it = drops.iterator(); it.hasNext(); ) {
            ItemEntity drop = it.next();
            if (!capture.earlierDrops.contains(drop) && capture.claim(drop.getItem())) {
               it.remove();
            }
         }
      }

      Inventory inventory = player.getInventory();
      for (InventorySlot kept : capture.kept) {
         if (inventory.getItem(kept.slot()).isEmpty()) {
            inventory.setItem(kept.slot(), kept.stack());
         } else {
            //? if >=26.3 {
            /*inventory.placeItemBackInInventory(kept.stack(), net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else
            inventory.placeItemBackInInventory(kept.stack());
         }
      }

      MemorialStatueEntity statue = capture.statue;
      capture.look.forEach(statue::setItemSlot);

      if (statue.isEmpty()) {
         if (PostmortemConfig.get().debug()) {
            Postmortem.LOGGER.info("[postmortem] {} died carrying nothing; no statue placed", player.getName().getString());
         }
         return;
      }

      int experience = experienceToStore(player, world);
      statue.storeExperience(experience);

      if (experience > 0) {
         player.experienceLevel = 0;
         player.experienceProgress = 0.0F;
         player.totalExperience = 0;
      }

      PoseSnapshot pose = ((PoseSnapshot.Holder) player).postmortem$getLastLivingPose();

      Vec3 pos = SafePlacement.findFor(world, statue, player);
      //? if >=1.21.5 {
      /*statue.snapTo(pos, pose.bodyYaw(), pose.pitch());
      *///?} else
      statue.moveTo(pos.x, pos.y, pos.z, pose.bodyYaw(), pose.pitch());
      statue.applyPose(pose);
      PehkuiCompat.copyScales(player, statue);
      statue.register(world);
      world.addFreshEntity(statue);

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info(
            "[postmortem] captured pose {} limbAngle={} limbAmplitude={} handSwing={} leaning={}",
            pose.pose(), pose.limbAngle(), pose.limbAmplitude(), pose.handSwing(), pose.leaning());
      }
      world.playSound(null, statue.getX(), statue.getY(), statue.getZ(), SoundEvents.STONE_PLACE, SoundSource.NEUTRAL, 1.0F, 0.7F);

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] {} died at {}; statue placed at {}, {} stack(s) dropped by other mods went in too",
            player.getName().getString(), player.position(), pos, capture.loose);
      }

      PENDING_ANNOUNCEMENTS.put(player.getUUID(), GlobalPos.of(world.dimension(), statue.blockPosition()));
   }

   private record InventorySlot(int slot, ItemStack stack, ItemStack copy) {
   }

   private static final class Capture {
      private final ServerPlayer player;
      private final ServerLevel world;
      private final long tick;
      private final MemorialStatueEntity statue;
      private final int selected;
      private final Set<ItemEntity> earlierDrops = Collections.newSetFromMap(new IdentityHashMap<>());
      private final List<InventorySlot> unclaimed = new ArrayList<>();
      private final List<InventorySlot> kept = new ArrayList<>();
      private final Map<EquipmentSlot, ItemStack> look = new EnumMap<>(EquipmentSlot.class);
      private int loose;

      private Capture(ServerPlayer player, ServerLevel world, MemorialStatueEntity statue, int selected, Collection<ItemEntity> drops) {
         this.player = player;
         this.world = world;
         this.tick = world.getGameTime();
         this.statue = statue;
         this.selected = selected;
         if (drops != null) {
            this.earlierDrops.addAll(drops);
         }
      }

      private boolean claim(ItemStack stack) {
         if (stack.isEmpty() || isSoulbound(stack)) {
            return false;
         }

         int slot = this.takeSlot(stack);
         if (slot == MemorialStatueEntity.NO_SLOT) {
            this.loose++;
         }
         this.statue.store(stack.copy(), slot, slot == MemorialStatueEntity.NO_SLOT ? null : displaySlotFor(slot, this.selected));
         return true;
      }

      private int takeSlot(ItemStack stack) {
         for (Iterator<InventorySlot> it = this.unclaimed.iterator(); it.hasNext(); ) {
            InventorySlot entry = it.next();
            if (entry.stack() == stack) {
               it.remove();
               return entry.slot();
            }
         }

         for (Iterator<InventorySlot> it = this.unclaimed.iterator(); it.hasNext(); ) {
            InventorySlot entry = it.next();
            if (ItemStack.matches(entry.copy(), stack)) {
               it.remove();
               return entry.slot();
            }
         }

         return MemorialStatueEntity.NO_SLOT;
      }
   }

   private static final Map<UUID, GlobalPos> PENDING_ANNOUNCEMENTS = new HashMap<>();

   private static void announceMemorial(ServerPlayer player) {
      GlobalPos memorial = PENDING_ANNOUNCEMENTS.remove(player.getUUID());
      if (memorial == null || !PostmortemConfig.get().memorialLocationMessage()) {
         return;
      }

      BlockPos pos = memorial.pos();
      player.sendSystemMessage(Component.translatable(
            "message.postmortem.memorial_location", pos.getX(), pos.getY(), pos.getZ(), memorial.dimension().location().toString())
         .withStyle(ChatFormatting.GRAY));
   }

   private static int experienceToStore(Player player, ServerLevel world) {
      PostmortemConfig config = PostmortemConfig.get();
      if (!config.storeExperience() || player.isSpectator()) {
         return 0;
      }

      if (config.storedExperiencePercent() == PostmortemConfig.VANILLA_EXPERIENCE) {
         return Math.min(player.experienceLevel * 7, 100);
      }

      return (int) Math.min(Integer.MAX_VALUE, totalExperience(player) * config.storedExperiencePercent() / 100L);
   }

   private static long totalExperience(Player player) {
      long level = Math.max(0, player.experienceLevel);
      double upToLevel;
      if (level <= 16) {
         upToLevel = level * level + 6 * level;
      } else if (level <= 31) {
         upToLevel = 2.5 * level * level - 40.5 * level + 360;
      } else {
         upToLevel = 4.5 * level * level - 162.5 * level + 2220;
      }

      return (long) Math.min(Long.MAX_VALUE / 100, upToLevel + player.experienceProgress * player.getXpNeededForNextLevel());
   }

   private static EquipmentSlot displaySlotFor(int slot, int selectedSlot) {
      if (slot == selectedSlot) {
         return EquipmentSlot.MAINHAND;
      }

      //? if >=1.21.5 {
      /*return Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
      *///?} else {
      return switch (slot) {
         case 36 -> EquipmentSlot.FEET;
         case 37 -> EquipmentSlot.LEGS;
         case 38 -> EquipmentSlot.CHEST;
         case 39 -> EquipmentSlot.HEAD;
         case Inventory.SLOT_OFFHAND -> EquipmentSlot.OFFHAND;
         default -> null;
      };
      //?}
   }
}
