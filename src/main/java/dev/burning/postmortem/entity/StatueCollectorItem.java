package dev.burning.postmortem.entity;

import dev.burning.postmortem.PostmortemConfig;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class StatueCollectorItem extends Item {
   public StatueCollectorItem(Properties settings) {
      super(settings);
   }

   @Override
   @SuppressWarnings("deprecation")
   public void appendHoverText(
      ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> consumer, TooltipFlag type) {
      consumer.accept(
         Component.translatable("item.postmortem.statue_collector.tooltip", (int) PostmortemConfig.get().collectRadius()).withStyle(ChatFormatting.GRAY));
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (world.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      List<MemorialStatueEntity> statues = world.getEntitiesOfClass(
         MemorialStatueEntity.class,
         player.getBoundingBox().inflate(PostmortemConfig.get().collectRadius()),
         statue -> statue.isOwner(player) && !statue.isEmpty() && !statue.isWaxed());

      if (statues.isEmpty()) {
         player.sendOverlayMessage(Component.translatable("message.postmortem.no_statue_in_range"));
         return InteractionResult.FAIL;
      }

      int returned = 0;
      for (MemorialStatueEntity statue : statues) {
         returned += statue.giveTo(player);
      }

      world.playSound(
         null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
      player.sendOverlayMessage(Component.translatable("message.postmortem.collected", returned));
      player.getCooldowns().addCooldown(stack, 20);
      return InteractionResult.SUCCESS;
   }
}
