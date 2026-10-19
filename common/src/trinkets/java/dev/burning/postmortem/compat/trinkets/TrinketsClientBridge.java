package dev.burning.postmortem.compat.trinkets;

import dev.burning.postmortem.client.StatueEntityRenderer;
import dev.burning.postmortem.compat.WornItem;
import eu.pb4.trinkets.api.SlotType;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.renderer.TrinketRenderContext;
import eu.pb4.trinkets.api.client.renderer.element.TrinketRenderElement;
import eu.pb4.trinkets.impl.client.render.ClientTrinketsManager;
import eu.pb4.trinkets.impl.client.render.TrinketEntityRenderState;
import eu.pb4.trinkets.impl.client.render.TrinketRenderLayer;
import eu.pb4.trinkets.impl.client.render.TrinketRenderStateFullImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class TrinketsClientBridge {
   private static final Map<Item, Optional<TrinketRenderer>> CODE_RENDERERS = new HashMap<>();
   private static EntityRendererProvider.Context cachedFor;

   private TrinketsClientBridge() {
   }

   static void extract(List<WornItem> shown, StatueEntityRenderer.State state, EntityRendererProvider.Context rendererContext) {
      if (!((Object) state instanceof TrinketEntityRenderState trinketState)
         || !(state.statue.level() instanceof ClientLevel level)) {
         return;
      }

      List<TrinketEntityRenderState.CodeRenderCall> code = new ArrayList<>(trinketState.trinkets$getCodeRenderers());
      trinketState.trinkets$setCodeRenderers(code);
      trinketState.trinkets$setPartAttachedRenderers(new ArrayList<>(trinketState.trinkets$getPartAttachedRenderers()));
      TrinketRenderContext context = new TrinketRenderStateFullImpl(
         Minecraft.getInstance(), rendererContext.getItemModelResolver(), rendererContext.getBlockModelResolver(), trinketState);

      Map<String, SlotType> slots = SlotType.getPlayerSlots(level);
      for (WornItem item : shown) {
         SlotType type = slotType(slots, item.slot());
         if (type == null) {
            continue;
         }

         ItemStack stack = item.stack();
         TrinketSlotAccess access = new StatueTrinketInventory(type, state.statue, item).access();
         TrinketRenderer renderer = codeRenderer(stack.getItem(), rendererContext);
         if (renderer != null) {
            code.add(new TrinketEntityRenderState.CodeRenderCall(access, stack, renderer));
            continue;
         }

         for (TrinketRenderElement.Baked baked : ClientTrinketsManager.INSTANCE.getResolved(stack)) {
            baked.apply(state.statue, stack, access, level, context, state);
         }
      }
   }

   private static SlotType slotType(Map<String, SlotType> slots, String id) {
      SlotType type = slots.get(id);
      if (type != null) {
         return type;
      }
      for (SlotType candidate : slots.values()) {
         if (candidate.getId().equals(id)) {
            return candidate;
         }
      }
      return null;
   }

   private static TrinketRenderer codeRenderer(Item item, EntityRendererProvider.Context rendererContext) {
      if (cachedFor != rendererContext) {
         CODE_RENDERERS.clear();
         cachedFor = rendererContext;
      }
      return CODE_RENDERERS.computeIfAbsent(item, key -> {
         Function<EntityRendererProvider.Context, TrinketRenderer> factory = TrinketRenderLayer.RENDERERS.get(key);
         return Optional.ofNullable(factory == null ? null : factory.apply(rendererContext));
      }).orElse(null);
   }
}
