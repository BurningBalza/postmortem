package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.burning.postmortem.Postmortem;
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
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?} else
//import net.minecraft.client.renderer.MultiBufferSource;
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
   //? if >=1.21.2 {
   extends LivingEntityRenderer<T, StatueEntityRenderer.State, StatueEntityModel> {
   //?} else
   //extends LivingEntityRenderer<StatueEntity, StatueEntityModel> {

   public static final ResourceLocation MEMORIAL_TEXTURE =
      Postmortem.id("textures/entity/memorial_statue.png");
   public static final ResourceLocation COSMETIC_TEXTURE =
      Postmortem.id("textures/entity/statue.png");
   public static final ResourceLocation MEMORIAL_EYES_TEXTURE =
      Postmortem.id("textures/entity/memorial_statue_eyes.png");

   private static final float PLAYER_SCALE = 0.9375F;

   //? if >=1.21.4 {
   private final ItemModelResolver itemModelManager;
   //?} else
   //private final ItemRenderer itemModelManager;

   private final ResourceLocation texture;

   //? if >=1.21.2 {
   public static class State extends HumanoidRenderState {
   //?} else {
   /*// Before 1.21.2 renderers drew straight from the entity. These are the HumanoidRenderState fields the statue code
   // uses, filled from the entity each frame, so the model and layers read a state on every version.
   public static class State {
      public Pose pose = Pose.STANDING;
      public float bodyRot, yRot, xRot, walkAnimationPos, walkAnimationSpeed, attackTime, swimAmount, deathTime, ageInTicks;
      public boolean isCrouching, isDiscrete, isFallFlying, isVisuallySwimming, hasRedOverlay, isUsingItem, isPassenger;
      public boolean isFullyFrozen;
      public float boundingBoxWidth, boundingBoxHeight, scale = 1.0F;
      public HumanoidArm mainArm = HumanoidArm.RIGHT;
      public ItemStack headEquipment = ItemStack.EMPTY;
      public ItemStack chestEquipment = ItemStack.EMPTY;
      public ItemStack legsEquipment = ItemStack.EMPTY;
      public ItemStack feetEquipment = ItemStack.EMPTY;
   *///?}
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
         //? if >=1.21.2 {
         context.getEquipmentRenderer()));
         //?} else
         //context.getModelManager()));
      //? if >=1.21.4
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
      //?}
      //? if >=1.21.4 {
      this.addLayer(new ItemInHandLayer<>(this));
      //?} elif >=1.21.2 {
      /*this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemRenderer()));
      this.addLayer(new ItemInHandLayer<>(this, context.getItemRenderer()));
      *///?} else {
      /*this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
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

   //? if >=1.21.2 {
   @Override
   public State createRenderState() {
      return new State();
   }

   @Override
   public void extractRenderState(T entity, State state, float tickDelta) {
      super.extractRenderState(entity, state, tickDelta);
   //?} else {
   /*// The state this frame is drawn from; the model holds it while vanilla's render runs, and the layers read it there.
   private final State frame = new State();
   private State preview;

   /^* What 1.21.2's createRenderState(entity, partialTick) does. ^/
   public State createRenderState(StatueEntity entity, float tickDelta) {
      State state = new State();
      this.extractRenderState(entity, state, tickDelta);
      return state;
   }

   /^* Draws the statue from a state the caller has posed, rather than from the entity; the pose screen's preview. ^/
   public void renderPreview(State state, PoseStack matrices, MultiBufferSource buffers, int light) {
      this.preview = state;
      try {
         this.render(state.statue, 0.0F, 1.0F, matrices, buffers, light);
      } finally {
         this.preview = null;
      }
   }

   @Override
   public void render(
      StatueEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource buffers, int light) {
      State state = this.preview;
      if (state == null) {
         state = this.frame;
         this.extractRenderState(entity, state, tickDelta);
      }
      this.model.current = state;

      // Vanilla scales by the entity's own scale; the preview's state asks for its own.
      float rescale = state.scale / entity.getScale();
      matrices.pushPose();
      matrices.scale(rescale, rescale, rescale);
      super.render(entity, yaw, tickDelta, matrices, buffers, light);
      matrices.popPose();
   }

   @Override
   protected void setupRotations(
      StatueEntity entity, PoseStack matrices, float bob, float bodyRot, float tickDelta, float scale) {
      super.setupRotations(entity, matrices, bob, this.model.current.bodyRot, tickDelta, scale);
   }

   @Override
   protected float getAttackAnim(StatueEntity entity, float tickDelta) {
      return this.model.current.attackTime;
   }

   @Override
   protected float getBob(StatueEntity entity, float tickDelta) {
      return this.model.current.ageInTicks;
   }

   @Override
   protected boolean isShaking(StatueEntity entity) {
      return this.model.current.isFullyFrozen;
   }

   public void extractRenderState(StatueEntity entity, State state, float tickDelta) {
      // What LivingEntityRenderer's own extraction fills in from 1.21.2.
      state.boundingBoxWidth = entity.getBbWidth();
      state.boundingBoxHeight = entity.getBbHeight();
      state.scale = entity.getScale();
   *///?}

      state.statue = entity;
      //? if >=1.21.4 {
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, tickDelta, this.itemModelManager);
      //?} elif >=1.21.2 {
      /*HumanoidMobRenderer.extractHumanoidRenderState(entity, state, tickDelta);
      *///?} else {
      /*state.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
      state.chestEquipment = entity.getItemBySlot(EquipmentSlot.CHEST);
      state.legsEquipment = entity.getItemBySlot(EquipmentSlot.LEGS);
      state.feetEquipment = entity.getItemBySlot(EquipmentSlot.FEET);
      state.mainArm = entity.getMainArm();
      *///?}
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
   //? if >=1.21.2 {
   protected void scale(State state, PoseStack matrices) {
   //?} else
   //protected void scale(StatueEntity entity, PoseStack matrices, float tickDelta) {
      matrices.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
   }

   @Override
   //? if >=1.21.2 {
   protected boolean shouldShowName(T entity, double squaredDistance) {
      return super.shouldShowName(entity, squaredDistance)
   //?} else {
   /*protected boolean shouldShowName(StatueEntity entity) {
      return this.preview == null && super.shouldShowName(entity)
   *///?}
         && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
   }

   @Override
   //? if >=1.21.2 {
   public ResourceLocation getTextureLocation(State state) {
      ResourceLocation carved = state.variant.texture();
   //?} else {
   /*public ResourceLocation getTextureLocation(StatueEntity entity) {
      ResourceLocation carved = entity.getVariant().texture();
   *///?}
      return carved != null ? carved : this.texture;
   }
}
