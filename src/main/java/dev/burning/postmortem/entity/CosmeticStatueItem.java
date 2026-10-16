package dev.burning.postmortem.entity;

import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CosmeticStatueItem extends Item {
   public CosmeticStatueItem(Properties settings) {
      super(settings);
   }

   @Override
   @SuppressWarnings("deprecation")
   public void appendHoverText(
      ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> consumer, TooltipFlag type) {
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip").withStyle(ChatFormatting.GRAY));
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.back").withStyle(ChatFormatting.GRAY));
      if (CombatAmenitiesCompat.isPresent()) {
         consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.belt").withStyle(ChatFormatting.GRAY));
      }
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.name").withStyle(ChatFormatting.GRAY));
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.pose").withStyle(ChatFormatting.GRAY));
      consumer.accept(Component.translatable("item.postmortem.cosmetic_statue.tooltip.wax").withStyle(ChatFormatting.GRAY));
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
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
      BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
      BlockPlaceContext placement = new BlockPlaceContext(context);
      if (!placement.canPlace()) {
         return InteractionResult.FAIL;
      }

      if (!world.getWorldBorder().isWithinBounds(PostmortemRegistry.COSMETIC_STATUE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
         return InteractionResult.FAIL;
      }

      if (world instanceof ServerLevel serverWorld) {
         CosmeticStatueEntity statue = PostmortemRegistry.COSMETIC_STATUE.create(serverWorld, EntitySpawnReason.SPAWN_ITEM_USE);
         if (statue == null) {
            return InteractionResult.FAIL;
         }

         float yaw = context.getRotation() + 180.0F;
         statue.snapTo(Vec3.atBottomCenterOf(pos), yaw, 0.0F);
         statue.setYHeadRot(yaw);
         statue.setYBodyRot(yaw);

         if (!serverWorld.noCollision(statue, statue.getBoundingBox())) {
            return InteractionResult.FAIL;
         }

         serverWorld.addFreshEntity(statue);
         serverWorld.playSound(
            null, statue.getX(), statue.getY(), statue.getZ(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);

         ItemStack stack = context.getItemInHand();
         if (context.getPlayer() == null || !context.getPlayer().hasInfiniteMaterials()) {
            stack.shrink(1);
         }
      }

      return InteractionResult.SUCCESS;
   }
}
