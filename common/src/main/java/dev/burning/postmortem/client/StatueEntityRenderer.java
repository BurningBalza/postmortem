package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.entity.StatuePose;
import dev.burning.postmortem.entity.StatueVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
//? if >=1.21.9
//import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//? if >=1.21.4 {
import net.minecraft.client.renderer.item.ItemModelResolver;
//?} else
//import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
//? if >=1.21.11
//import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StatueEntityRenderer<T extends StatueEntity>
   extends LivingEntityRenderer<T, StatueEntityRenderer.State, StatueEntityModel> {

   public static final ResourceLocation MEMORIAL_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("postmortem", "textures/entity/memorial_statue.png");
   public static final ResourceLocation COSMETIC_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("postmortem", "textures/entity/statue.png");
   public static final ResourceLocation MEMORIAL_EYES_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("postmortem", "textures/entity/memorial_statue_eyes.png");

   private static final float PLAYER_SCALE = 0.9375F;

   //? if >=1.21.4 {
   private final ItemModelResolver itemModelManager;
   //?} else
   //private final ItemRenderer itemModelManager;

   private final ResourceLocation texture;

   public static class State extends HumanoidRenderState {
      public StatueEntity statue;

      public final ItemDrawState backslotItemState = new ItemDrawState();

      public StatueBackItemLayer.Fit backslotFit = StatueBackItemLayer.Fit.GENERIC;

      public StatuePose limbPose = StatuePose.DEFAULT;

      public StatueVariant variant = StatueVariant.DEFAULT;

      //? if <1.21.4 {
      /*// Render states only carry arm poses from 1.21.4; StatueEntityModel hands these to HumanoidModel before that.
      public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
      public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
      *///?}
   }

   public StatueEntityRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
      super(context, new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
      //? if >=1.21.4 {
      this.itemModelManager = context.getItemModelResolver();
      //?} else
      //this.itemModelManager = context.getItemRenderer();
      this.texture = texture;
      //? if >=1.21.9 {
      /*this.addLayer(new HumanoidArmorLayer<>(
         this,
         ArmorModelSet.bake(
            ModelLayers.PLAYER_SLIM_ARMOR, context.getModelSet(), StatueEntityModel::new),
         context.getEquipmentRenderer()));
      this.addLayer(new CustomHeadLayer<>(
         this, context.getModelSet(), context.getPlayerSkinRenderCache()));
      *///?} else {
      this.addLayer(new HumanoidArmorLayer<>(
         this,
         new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
         new StatueEntityModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
         context.getEquipmentRenderer()));
      //? if >=1.21.4
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
      //?}
      //? if >=1.21.4 {
      this.addLayer(new ItemInHandLayer<>(this));
      //?} else {
      /*this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemRenderer()));
      this.addLayer(new ItemInHandLayer<>(this, context.getItemRenderer()));
      *///?}
      this.addLayer(new StatueBackItemLayer(this, context));
      this.addLayer(new StatueHighlightLayer(this));
   }

   /** A renderer whose statue also has glowing eyes. */
   public StatueEntityRenderer(
      EntityRendererProvider.Context context, ResourceLocation texture, ResourceLocation eyesTexture) {
      this(context, texture);
      RenderType eyes = RenderType.eyes(eyesTexture);
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
      //? if >=1.21.4 {
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, tickDelta, this.itemModelManager);
      //?} else
      //HumanoidMobRenderer.extractHumanoidRenderState(entity, state, tickDelta);
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
         state.backslotItemState.update(this.itemModelManager, backslot, state.backslotFit.displayContext(), entity);
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
      /*if (stack.is(ItemTags.SPEARS)) {
         return HumanoidModel.ArmPose.SPEAR;
      }
      *///?}

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
   public ResourceLocation getTextureLocation(State state) {
      ResourceLocation carved = state.variant.texture();
      return carved != null ? carved : this.texture;
   }
}
