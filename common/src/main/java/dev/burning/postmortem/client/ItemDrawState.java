package dev.burning.postmortem.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.4 {
/*import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
*///?} else {
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
//?}

public final class ItemDrawState {
   //? if >=1.21.4 {
   /*final ItemStackRenderState state = new ItemStackRenderState();

   public void update(ItemModelResolver resolver, ItemStack stack, ItemDisplayContext context, LivingEntity entity) {
      //? if >=1.21.5 {
      /^resolver.updateForLiving(this.state, stack, context, entity);
      ^///?} else
      resolver.updateForLiving(this.state, stack, context, false, entity);
   }

   public boolean isEmpty() {
      return this.state.isEmpty();
   }

   public void clear() {
      this.state.clear();
   }
   *///?} else {
   ItemStack stack = ItemStack.EMPTY;
   ItemDisplayContext context = ItemDisplayContext.NONE;
   BakedModel model;

   public void update(ItemRenderer resolver, ItemStack stack, ItemDisplayContext context, LivingEntity entity) {
      this.stack = stack.copy();
      this.context = context;
      //? if >=1.21.2 {
      this.model = resolver.resolveItemModel(stack, entity, context);
      //?} else
      //this.model = resolver.getModel(stack, entity.level(), entity, entity.getId() + context.ordinal());
   }

   public boolean isEmpty() {
      return this.stack.isEmpty() || this.model == null;
   }

   public void clear() {
      this.stack = ItemStack.EMPTY;
      this.model = null;
   }
   //?}
}
