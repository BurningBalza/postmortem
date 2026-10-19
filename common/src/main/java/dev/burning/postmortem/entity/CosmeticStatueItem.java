package dev.burning.postmortem.entity;

import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.BackSlots;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
//? if >=1.21.2
//import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5
//import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CosmeticStatueItem extends Item {
   public CosmeticStatueItem(Properties settings) {
      super(settings);
   }

   @Override
   @SuppressWarnings("deprecation")
   //? if >=1.21.5 {
   /*public void appendHoverText(
      ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> consumer, TooltipFlag type) {
   *///?} elif >=1.20.5 {
   /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag type) {
      Consumer<Component> consumer = tooltip::add;
   *///?} else {
   public void appendHoverText(ItemStack stack, Level world, java.util.List<Component> tooltip, TooltipFlag type) {
      Consumer<Component> consumer = tooltip::add;
   //?}
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip").withStyle(ChatFormatting.GRAY));
      if (BackSlots.back()) {
         consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.back").withStyle(ChatFormatting.GRAY));
      }
      if (BackSlots.belt()) {
         consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.belt").withStyle(ChatFormatting.GRAY));
      }
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.name").withStyle(ChatFormatting.GRAY));
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.pose").withStyle(ChatFormatting.GRAY));
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.wax").withStyle(ChatFormatting.GRAY));
   }

   @Override
   //? if >=1.21.2 {
   /*public InteractionResult use(Level world, Player player, InteractionHand hand) {
   *///?} else {
   public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      return new net.minecraft.world.InteractionResultHolder<>(this.useResult(world, player, hand), player.getItemInHand(hand));
   }

   private InteractionResult useResult(Level world, Player player, InteractionHand hand) {
   //?}
      BlockHitResult hit = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);

      if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
         return InteractionResult.PASS;
      }

      if (!world.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
         return InteractionResult.PASS;
      }

      return this.useOn(new UseOnContext(player, hand, hit));
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getClickedFace() != Direction.UP) {
         return InteractionResult.PASS;
      }

      Level world = context.getLevel();
      BlockPlaceContext placement = new BlockPlaceContext(context);
      if (!placement.canPlace()) {
         return InteractionResult.FAIL;
      }

      Vec3 pos = standingSpot(world, context.getClickedPos());
      //? if >=1.20.5 {
      /*AABB spawnBox = PostmortemRegistry.COSMETIC_STATUE.get().getSpawnAABB(pos.x, pos.y, pos.z);
      *///?} else
      AABB spawnBox = PostmortemRegistry.COSMETIC_STATUE.get().getAABB(pos.x, pos.y, pos.z);
      if (!world.getWorldBorder().isWithinBounds(spawnBox)) {
         return InteractionResult.FAIL;
      }

      if (world instanceof ServerLevel serverWorld) {
         //? if >=1.21.2 {
         /*CosmeticStatueEntity statue = PostmortemRegistry.COSMETIC_STATUE.get().create(serverWorld, EntitySpawnReason.SPAWN_ITEM_USE);
         *///?} else
         CosmeticStatueEntity statue = PostmortemRegistry.COSMETIC_STATUE.get().create(serverWorld);
         if (statue == null) {
            return InteractionResult.FAIL;
         }

         float yaw = context.getRotation() + 180.0F;
         //? if >=1.21.5 {
         /*statue.snapTo(pos, yaw, 0.0F);
         *///?} else
         statue.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
         statue.setYHeadRot(yaw);
         statue.setYBodyRot(yaw);

         if (!serverWorld.noCollision(statue, statue.getBoundingBox())) {
            return InteractionResult.FAIL;
         }

         serverWorld.addFreshEntity(statue);
         serverWorld.playSound(
            null, statue.getX(), statue.getY(), statue.getZ(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);

         ItemStack stack = context.getItemInHand();
         //? if >=1.20.5 {
         /*if (context.getPlayer() == null || !context.getPlayer().hasInfiniteMaterials()) {
         *///?} else
         if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return InteractionResult.SUCCESS;
   }

   private static Vec3 standingSpot(Level world, BlockPos clicked) {
      VoxelShape shape = world.getBlockState(clicked).getCollisionShape(world, clicked);
      double x = clicked.getX() + 0.5;
      double z = clicked.getZ() + 0.5;
      double top = clicked.getY() + (shape.isEmpty() ? 1.0 : Math.max(1.0, shape.max(Direction.Axis.Y)));
      if (shape.isEmpty() && world.getFluidState(clicked).is(FluidTags.WATER)) {
         return new Vec3(x, top, z);
      }

      //? if >=1.20.5 {
      /*AABB box = PostmortemRegistry.COSMETIC_STATUE.get().getSpawnAABB(x, top, z);
      *///?} else
      AABB box = PostmortemRegistry.COSMETIC_STATUE.get().getAABB(x, top, z);
      double fall = Entity.collideBoundingBox((Entity) null, new Vec3(0.0, clicked.getY() - top, 0.0), box, world, List.of()).y;
      return new Vec3(x, top + fall, z);
   }
}
