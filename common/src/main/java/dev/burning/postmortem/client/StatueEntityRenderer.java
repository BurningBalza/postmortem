package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.entity.StatuePose;
import dev.burning.postmortem.entity.StatueVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
//? if >=1.21.9
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
//? if >=1.21.11
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StatueEntityRenderer<T extends StatueEntity>
   extends LivingEntityRenderer<T, StatueEntityRenderer.State, StatueEntityModel> {

   public static final Identifier MEMORIAL_TEXTURE =
      Identifier.fromNamespaceAndPath("postmortem", "textures/entity/memorial_statue.png");
   public static final Identifier COSMETIC_TEXTURE =
      Identifier.fromNamespaceAndPath("postmortem", "textures/entity/statue.png");
   public static final Identifier MEMORIAL_EYES_TEXTURE =
      Identifier.fromNamespaceAndPath("postmortem", "textures/entity/memorial_statue_eyes.png");

   private static final float PLAYER_SCALE = 0.9375F;

   private final ItemModelResolver itemModelManager;

   private final Identifier texture;

   public static class State extends HumanoidRenderState {
      public StatueEntity statue;

      public final ItemStackRenderState backslotItemState = new ItemStackRenderState();

      public StatueBackItemLayer.Fit backslotFit = StatueBackItemLayer.Fit.GENERIC;

      public StatuePose limbPose = StatuePose.DEFAULT;

      public StatueVariant variant = StatueVariant.DEFAULT;
   }

   public StatueEntityRenderer(EntityRendererProvider.Context context, Identifier texture) {
      super(context, new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
      this.itemModelManager = context.getItemModelResolver();
      this.texture = texture;
      //? if >=1.21.9 {
      this.addLayer(new HumanoidArmorLayer<>(
         this,
         ArmorModelSet.bake(
            ModelLayers.PLAYER_SLIM_ARMOR, context.getModelSet(), StatueEntityModel::new),
         context.getEquipmentRenderer()));
      this.addLayer(new CustomHeadLayer<>(
         this, context.getModelSet(), context.getPlayerSkinRenderCache()));
      //?} else {
      /*this.addLayer(new HumanoidArmorLayer<>(
         this,
         new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
         new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
         context.getEquipmentRenderer()));
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
      *///?}
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(new StatueBackItemLayer(this, context));
      this.addLayer(new StatueHighlightLayer(this));
   }

   /** A renderer whose statue also has glowing eyes. */
   public StatueEntityRenderer(
      EntityRendererProvider.Context context, Identifier texture, Identifier eyesTexture) {
      this(context, texture);
      RenderType eyes = RenderTypes.eyes(eyesTexture);
      this.addLayer(new EyesLayer<>(this) {
         @Override
         public RenderType renderType() {
            return eyes;
         }
      });
   }

   @Override
   public State createRenderState() {
      return new State();
   }

   @Override
   public void extractRenderState(T entity, State state, float tickDelta) {
      super.extractRenderState(entity, state, tickDelta);

      state.statue = entity;
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, tickDelta, this.itemModelManager);
      Pose pose = entity.getPose();
      state.pose = pose;
      state.bodyRot = entity.getVisualRotationYInDegrees();
      state.yRot = entity.getYHeadRot() - entity.getVisualRotationYInDegrees();
      state.xRot = entity.getXRot();
      state.walkAnimationPos = entity.getLimbAngle();
      state.walkAnimationSpeed = entity.getLimbAmplitude();
      //? if >=26.3 {
      /*state.swingAnimation = entity.getHandSwing();
      state.currentSwing = entity.getHandSwing() > 0.0F
         ? new net.minecraft.world.entity.LivingEntity.SwingDescription(
            net.minecraft.world.InteractionHand.MAIN_HAND, entity.getMainHandItem().getAttackAnimation(), 6)
         : null;
      *///?} else
      state.attackTime = entity.getHandSwing();
      state.swimAmount = entity.getLeaning();
      state.isCrouching = pose == Pose.CROUCHING;
      state.isDiscrete = state.isCrouching;
      state.isFallFlying = pose == Pose.FALL_FLYING;
      state.isVisuallySwimming = pose == Pose.SWIMMING;
      state.deathTime = 0.0F;
      state.hasRedOverlay = false;
      state.isUsingItem = false;
      state.isPassenger = entity.isSeated();
      state.ageInTicks = 0.0F;
      state.isFullyFrozen = false;
      state.limbPose = entity instanceof CosmeticStatueEntity cosmetic ? cosmetic.getLimbPose() : StatuePose.DEFAULT;
      state.variant = entity.getVariant();
      updateArmPoses(entity, state);
      ItemStack backslot = entity.getBackslotStack();
      if (backslot.isEmpty()) {
         state.backslotItemState.clear();
         state.backslotFit = StatueBackItemLayer.Fit.GENERIC;
      } else {
         state.backslotFit = StatueBackItemLayer.Fit.forItem(backslot);
         //? if >=1.21.9 {
         this.itemModelManager.updateForLiving(
            state.backslotItemState, backslot, state.backslotFit.displayContext(), entity);
         //?} else {
         /*this.itemModelManager.updateForLiving(
            state.backslotItemState, backslot, state.backslotFit.displayContext(), false, entity);
         *///?}
      }
   }

   private static void updateArmPoses(StatueEntity entity, State state) {
      ItemStack mainStack = entity.getItemBySlot(EquipmentSlot.MAINHAND);
      ItemStack offStack = entity.getItemBySlot(EquipmentSlot.OFFHAND);

      HumanoidModel.ArmPose mainPose = armPoseFor(mainStack);
      HumanoidModel.ArmPose offPose = mainPose.isTwoHanded()
         ? (offStack.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM)
         : armPoseFor(offStack);

      boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
      state.rightArmPose = rightHanded ? mainPose : offPose;
      state.leftArmPose = rightHanded ? offPose : mainPose;
   }

   private static HumanoidModel.ArmPose armPoseFor(ItemStack stack) {
      if (stack.isEmpty()) {
         return HumanoidModel.ArmPose.EMPTY;
      }

      if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
         return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      }

      //? if >=1.21.11 {
      if (stack.is(ItemTags.SPEARS)) {
         return HumanoidModel.ArmPose.SPEAR;
      }
      //?}

      return HumanoidModel.ArmPose.ITEM;
   }

   @Override
   protected void scale(State state, PoseStack matrices) {
      matrices.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
   }

   @Override
   protected boolean shouldShowName(T entity, double squaredDistance) {
      return super.shouldShowName(entity, squaredDistance)
         && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
   }

   @Override
   public Identifier getTextureLocation(State state) {
      Identifier carved = state.variant.texture();
      return carved != null ? carved : this.texture;
   }
}
