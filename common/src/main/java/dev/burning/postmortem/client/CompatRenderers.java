package dev.burning.postmortem.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.compat.BackslotCompat;
import dev.burning.postmortem.compat.CombatAmenitiesCompat;
import dev.burning.postmortem.entity.LimbTransform;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.entity.StatuePart;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import dev.burning.postmortem.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
//? if >=1.21.9 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Drives other mods' player back-item renderers on a statue. Those renderers expect a player, so each bridge poses a
 * shared stand-in player and player model to match the statue, then hands them over.
 */
public abstract class CompatRenderers {
   private static final GameProfile STAND_IN_PROFILE = new GameProfile(new UUID(0L, 0L), "PostmortemStatue");

   private static final int STAND_IN_ID = 1 << 30;

   // Other mods' layers draw through RenderLayer's own method: submit from 1.21.9, render before it.
   //? if >=1.21.9 {
   /*private static final String SUBMIT = "submit";
   private static final Class<?>[] SUBMIT_SIGNATURE = {
      PoseStack.class, SubmitNodeCollector.class, int.class, PlayerRenderState.class, float.class, float.class
   };
   *///?} else {
   private static final String SUBMIT = "render";
   private static final Class<?>[] SUBMIT_SIGNATURE = {
      PoseStack.class, MultiBufferSource.class, int.class, PlayerRenderState.class, float.class, float.class
   };
   //?}

   private static RemotePlayer standIn;

   protected final PlayerModel model;

   protected final PlayerRenderState state = new PlayerRenderState();

   protected CompatRenderers(PlayerModel model) {
      this.model = model;
   }

   protected static PlayerModel bakeModel(EntityRendererProvider.Context factory) {
      return new PlayerModel(factory.bakeLayer(ModelLayers.PLAYER), false);
   }

   protected static RenderLayerParent<PlayerRenderState, PlayerModel> parentOf(PlayerModel model) {
      return () -> model;
   }

   /** Returns the stand-in player dressed in the statue's chest and legs, or null outside a world. */
   protected static Player dressStandIn(StatueEntity statue) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world == null) {
         return null;
      }

      if (standIn == null || standIn.level() != world) {
         standIn = new RemotePlayer(world, STAND_IN_PROFILE);
         standIn.setId(STAND_IN_ID);
      }

      standIn.setItemSlot(EquipmentSlot.CHEST, statue.getItemBySlot(EquipmentSlot.CHEST));
      standIn.setItemSlot(EquipmentSlot.LEGS, statue.getItemBySlot(EquipmentSlot.LEGS));
      return standIn;
   }

   /** Copies the statue's pose and worn armour onto the stand-in model and render state. */
   protected void match(StatueEntityModel from, StatueEntityRenderer.State source) {
      this.model.head.loadPose(from.head.storePose());
      this.model.hat.loadPose(from.hat.storePose());
      this.model.body.loadPose(from.body.storePose());
      this.model.rightArm.loadPose(from.rightArm.storePose());
      this.model.leftArm.loadPose(from.leftArm.storePose());
      this.model.rightLeg.loadPose(from.rightLeg.storePose());
      this.model.leftLeg.loadPose(from.leftLeg.storePose());

      this.state.headEquipment = source.headEquipment;
      this.state.chestEquipment = source.chestEquipment;
      this.state.legsEquipment = source.legsEquipment;
      this.state.feetEquipment = source.feetEquipment;
      this.state.mainArm = source.mainArm;
      this.state.skin = DefaultPlayerSkin.getDefaultSkin();
      this.state.showCape = false;
   }

   public abstract boolean render(
      PoseStack matrices, RenderSink sink, int light,
      StatueEntityRenderer.State source, StatueEntityModel sourceModel, StatueEntity statue);

   /** Backslot Mod's back-item renderer. */
   public static final class Backslot extends CompatRenderers {
      private static final String MOD_ID = "backslot";
      private static final String RENDERER = "net.medecoole.backslot.registry.BackslotFeatureRenderer";
      private static final String ADDON = "net.medecoole.backslot.registry.EntityRenderStateAddon";

      private static boolean resolved;
      private static boolean available;

      private static Constructor<?> rendererConstructor;
      private static Method submit;
      private static Method getStateEntity;
      private static Method setStateEntity;

      private final Object renderer;

      private Backslot(Object renderer, PlayerModel model) {
         super(model);
         this.renderer = renderer;
      }

      public static Backslot create(EntityRendererProvider.Context factory) {
         if (!resolve()) {
            return null;
         }

         try {
            PlayerModel model = bakeModel(factory);
            return new Backslot(rendererConstructor.newInstance(parentOf(model), factory), model);
         } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            Postmortem.LOGGER.warn("[postmortem] could not build the backslot mod's renderer; using Postmortem's own placement", e);
            return null;
         }
      }

      @Override
      public boolean render(
         PoseStack matrices, RenderSink sink, int light,
         StatueEntityRenderer.State source, StatueEntityModel sourceModel, StatueEntity statue) {

         if (!available) {
            return false;
         }

         Player standIn = dressStandIn(statue);
         if (standIn == null) {
            return false;
         }

         Entity previous = null;
         try {
            this.match(sourceModel, source);
            BackslotCompat.setLocal(standIn, statue.getBackslotStack());

            previous = (Entity) getStateEntity.invoke(this.state);
            setStateEntity.invoke(this.state, standIn);

            submit.invoke(this.renderer, matrices, sink.raw(), light, this.state, 0.0F, 0.0F);
            return true;
         } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            Postmortem.LOGGER.warn("[postmortem] the backslot mod's renderer failed and is now disabled; using Postmortem's own placement", e);
            return false;
         } finally {
            try {
               BackslotCompat.setLocal(standIn, ItemStack.EMPTY);
               if (previous != null) {
                  setStateEntity.invoke(this.state, previous);
               }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
         }
      }

      private static boolean resolve() {
         if (resolved) {
            return available;
         }
         resolved = true;

         if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
            return false;
         }

         try {
            Class<?> renderer = Class.forName(RENDERER);
            Class<?> addon = Class.forName(ADDON);

            rendererConstructor = renderer.getConstructors()[0];
            submit = renderer.getMethod(SUBMIT, SUBMIT_SIGNATURE);
            getStateEntity = addon.getMethod("entity");
            setStateEntity = addon.getMethod("backslotMod$entity", Entity.class);

            available = true;
            Postmortem.LOGGER.info("[postmortem] backslot mod detected; statues will be drawn by its own back renderer");
         } catch (ReflectiveOperationException | RuntimeException e) {
            Postmortem.LOGGER.warn("[postmortem] backslot mod present but its renderer did not match; using Postmortem's own placement", e);
            available = false;
         }

         return available;
      }
   }

   /** Combat Amenities' back and belt renderers. */
   public static final class Amenities extends CompatRenderers {
      private static final String MOD_ID = "combatamenities";
      private static final String BACK = "net.hollowed.combatamenities.renderer.BackSlotFeatureRenderer";
      private static final String BELT = "net.hollowed.combatamenities.renderer.BeltSlotFeatureRenderer";
      private static final String ACCESS = "net.hollowed.combatamenities.util.interfaces.PlayerEntityRenderStateAccess";

      private static boolean resolved;
      private static boolean available;

      private static Constructor<?> backConstructor;
      private static Constructor<?> beltConstructor;
      private static Method submitBack;
      private static Method submitBelt;
      private static Method setPlayer;

      private final Object backRenderer;
      private final Object beltRenderer;

      private Amenities(Object backRenderer, Object beltRenderer, PlayerModel model) {
         super(model);
         this.backRenderer = backRenderer;
         this.beltRenderer = beltRenderer;
      }

      public static Amenities create(EntityRendererProvider.Context factory) {
         if (!resolve()) {
            return null;
         }

         try {
            PlayerModel model = bakeModel(factory);
            RenderLayerParent<PlayerRenderState, PlayerModel> parent = parentOf(model);
            //? if >=26.3 {
            /*Object heldItemRenderer = null;
            *///?} else
            Object heldItemRenderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;

            return new Amenities(
               backConstructor.newInstance(parent, heldItemRenderer),
               beltConstructor.newInstance(parent, heldItemRenderer),
               model);
         } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            Postmortem.LOGGER.warn("[postmortem] could not build Combat Amenities' renderers; statues will not wear its items", e);
            return null;
         }
      }

      @Override
      public boolean render(
         PoseStack matrices, RenderSink sink, int light,
         StatueEntityRenderer.State source, StatueEntityModel sourceModel, StatueEntity statue) {

         EquipmentSlot backSlot = CombatAmenitiesCompat.backSlot();
         EquipmentSlot beltSlot = CombatAmenitiesCompat.beltSlot();
         if (!available || backSlot == null || beltSlot == null) {
            return false;
         }

         Player standIn = dressStandIn(statue);
         if (standIn == null) {
            return false;
         }

         try {
            this.match(sourceModel, source);
            setPlayer.invoke(this.state, standIn);

            standIn.setItemSlot(backSlot, statue.getBackslotStack());
            standIn.setItemSlot(beltSlot, statue.getBeltslotStack());

            this.submitIsolated(submitBack, this.backRenderer, matrices, sink, light, source.limbPose.get(StatuePart.BACK_ITEM));
            this.submitIsolated(submitBelt, this.beltRenderer, matrices, sink, light, source.limbPose.get(StatuePart.BELT_ITEM));
            return true;
         } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            Postmortem.LOGGER.warn("[postmortem] Combat Amenities' renderers failed and are now disabled", e);
            return false;
         } finally {
            standIn.setItemSlot(backSlot, ItemStack.EMPTY);
            standIn.setItemSlot(beltSlot, ItemStack.EMPTY);
         }
      }

      private void submitIsolated(
         Method submit, Object renderer, PoseStack parent, RenderSink sink, int light, LimbTransform transform)
         throws ReflectiveOperationException {

         PoseStack isolated = new PoseStack();
         isolated.last().pose().set(parent.last().pose());
         isolated.last().normal().set(parent.last().normal());
         StatueEntityModel.applyToMatrix(isolated, transform);
         submit.invoke(renderer, isolated, sink.raw(), light, this.state, 0.0F, 0.0F);
      }

      private static boolean resolve() {
         if (resolved) {
            return available;
         }
         resolved = true;

         if (!Platform.INSTANCE.isModLoaded(MOD_ID)) {
            return false;
         }

         try {
            Class<?> back = Class.forName(BACK);
            Class<?> belt = Class.forName(BELT);
            Class<?> access = Class.forName(ACCESS);

            backConstructor = back.getConstructors()[0];
            beltConstructor = belt.getConstructors()[0];
            submitBack = back.getMethod(SUBMIT, SUBMIT_SIGNATURE);
            submitBelt = belt.getMethod(SUBMIT, SUBMIT_SIGNATURE);
            setPlayer = access.getMethod("combat_Amenities$setPlayerEntity", Player.class);

            available = true;
            Postmortem.LOGGER.info("[postmortem] combat amenities detected; statues will be drawn by its own back and belt renderers");
         } catch (ReflectiveOperationException | RuntimeException e) {
            Postmortem.LOGGER.warn("[postmortem] combat amenities present but its renderers did not match; skipping", e);
            available = false;
         }
         return available;
      }
   }
}
