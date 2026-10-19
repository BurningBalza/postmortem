package dev.burning.postmortem.entity;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import dev.burning.postmortem.compat.GraveMods;
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
//? if >=1.21.5
//import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class StatueCollectorItem extends Item {
   public StatueCollectorItem(Properties settings) {
      super(settings);
   }

   @Override
   @SuppressWarnings("deprecation")
   //? if >=1.21.5 {
   /*public void appendHoverText(
      ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> consumer, TooltipFlag type) {
   *///?} elif >=1.20.5 {
   public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag type) {
      Consumer<Component> consumer = tooltip::add;
   //?} else {
   /*public void appendHoverText(ItemStack stack, Level world, java.util.List<Component> tooltip, TooltipFlag type) {
      Consumer<Component> consumer = tooltip::add;
   *///?}
      consumer.accept(
         Component.translatable("item.postmortem.statue_collector.tooltip", (int) PostmortemConfig.get().collectRadius()).withStyle(ChatFormatting.GRAY));
   }

   @Override
   //? if >=1.21.2 {
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
   //?} else {
   /*public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      return new net.minecraft.world.InteractionResultHolder<>(this.useResult(world, player, hand), player.getItemInHand(hand));
   }

   private InteractionResult useResult(Level world, Player player, InteractionHand hand) {
   *///?}
      ItemStack stack = player.getItemInHand(hand);
      if (GraveMods.present()) {
         return InteractionResult.PASS;
      }

      if (world.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      List<MemorialStatueEntity> statues = world.getEntitiesOfClass(
         MemorialStatueEntity.class,
         player.getBoundingBox().inflate(PostmortemConfig.get().collectRadius()),
         statue -> statue.isOwner(player) && !statue.isEmpty() && !statue.isWaxed());

      if (statues.isEmpty()) {
         Postmortem.overlay(player, Component.translatable("message.postmortem.no_statue_in_range"));
         return InteractionResult.FAIL;
      }

      for (MemorialStatueEntity statue : statues) {
         statue.giveTo(player);
      }

      world.playSound(
         null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
      //? if >=1.21.2 {
      player.getCooldowns().addCooldown(stack, 20);
      //?} else
      //player.getCooldowns().addCooldown(stack.getItem(), 20);
      return InteractionResult.SUCCESS;
   }
}
