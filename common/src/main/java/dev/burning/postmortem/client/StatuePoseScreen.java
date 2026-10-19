package dev.burning.postmortem.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.burning.postmortem.PostmortemRegistry;
import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.LimbTransform;
import dev.burning.postmortem.entity.StatuePart;
import dev.burning.postmortem.entity.StatuePose;
import dev.burning.postmortem.platform.ClientPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9
//import net.minecraft.client.input.MouseButtonEvent;
//? if >=1.21.2
//import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class StatuePoseScreen extends Screen {

   private static final int SEND_INTERVAL = 2;
   private static final int MIN_SCREEN_WIDTH = 320;
   private static final int CONTENT_WIDTH = 200;
   private static final int PREVIEW_HEIGHT = 120;
   private static final int ROW_HEIGHT = 22;
   private static final int WIDGET_HEIGHT = 20;
   private static final int SLIDER_COLUMNS = 2;
   private static final int SLIDER_ROWS = 3;
   private static final int SPACING = 4;
   private static final int LABEL_HEIGHT = 12;
   private static final int CONTENT_HEIGHT =
      PREVIEW_HEIGHT + LABEL_HEIGHT + SLIDER_ROWS * ROW_HEIGHT + SPACING + ROW_HEIGHT + SPACING + WIDGET_HEIGHT;
   private static final int TITLE_SPACE = 24;

   private static final float FRONT_YAW = 180.0F;
   private static final float BACK_YAW = 0.0F;
   private static final float TURN_SPEED = 0.25F;
   private static final int PREVIEW_SIZE = 45;
   private static final float PREVIEW_LIFT = 0.0625F;
   private static final float DRAG_DEGREES_PER_PIXEL = 1.0F;
   private static final float MAX_PITCH = 40.0F;

   private static final int PANEL_COLOUR = 0x66000000;

   static {
      if (CONTENT_WIDTH > MIN_SCREEN_WIDTH) {
         throw new AssertionError("Pose menu is " + CONTENT_WIDTH + " wide and will not fit a " + MIN_SCREEN_WIDTH + " screen");
      }
   }

   private final CosmeticStatueEntity statue;
   private final StatuePartPicker picker = new StatuePartPicker();
   private StatuePose working;

   private StatuePart selected = StatuePart.HEAD;
   private StatuePart hovered;
   private float previewYaw;
   private float targetYaw;
   private float previewPitch;
   private boolean turnedByHand;
   private boolean dirty;
   private int ticksSinceSend;

   private final AxisSlider[] sliders = new AxisSlider[Axis.values().length];

   private int contentLeft;
   private int contentTop;
   private int previewBottom;

   public StatuePoseScreen(CosmeticStatueEntity statue) {
      super(Component.translatable("screen.postmortem.pose"));
      this.statue = statue;
      this.working = statue.getLimbPose();
      this.previewYaw = previewYawFor(this.selected);
      this.targetYaw = this.previewYaw;
   }

   private enum Axis {
      PITCH("pitch", true),
      YAW("yaw", true),
      ROLL("roll", true),
      OFFSET_X("offset_x", false),
      OFFSET_Y("offset_y", false),
      OFFSET_Z("offset_z", false);

      private final String id;
      private final boolean rotation;

      Axis(String id, boolean rotation) {
         this.id = id;
         this.rotation = rotation;
      }

      String translationKey() {
         return "axis.postmortem." + this.id;
      }

      float limit() {
         return this.rotation ? LimbTransform.MAX_ROTATION : LimbTransform.MAX_OFFSET;
      }

      float read(LimbTransform transform) {
         return switch (this) {
            case PITCH -> transform.rotation().x();
            case YAW -> transform.rotation().y();
            case ROLL -> transform.rotation().z();
            case OFFSET_X -> transform.offset().x();
            case OFFSET_Y -> transform.offset().y();
            case OFFSET_Z -> transform.offset().z();
         };
      }

      LimbTransform write(LimbTransform transform, float value) {
         return switch (this) {
            case PITCH -> transform.withRotation(value, transform.rotation().y(), transform.rotation().z());
            case YAW -> transform.withRotation(transform.rotation().x(), value, transform.rotation().z());
            case ROLL -> transform.withRotation(transform.rotation().x(), transform.rotation().y(), value);
            case OFFSET_X -> transform.withOffset(value, transform.offset().y(), transform.offset().z());
            case OFFSET_Y -> transform.withOffset(transform.offset().x(), value, transform.offset().z());
            case OFFSET_Z -> transform.withOffset(transform.offset().x(), transform.offset().y(), value);
         };
      }
   }

   @Override
   protected void init() {
      this.contentLeft = Math.max(0, (this.width - CONTENT_WIDTH) / 2);
      this.contentTop = Math.max(TITLE_SPACE, (this.height - CONTENT_HEIGHT) / 2);
      this.previewBottom = this.contentTop + PREVIEW_HEIGHT;

      int sliderWidth = (CONTENT_WIDTH - (SLIDER_COLUMNS - 1) * SPACING) / SLIDER_COLUMNS;
      int slidersTop = this.previewBottom + LABEL_HEIGHT;
      for (Axis axis : Axis.values()) {
         int index = axis.ordinal();
         int x = this.contentLeft + (index / SLIDER_ROWS) * (sliderWidth + SPACING);
         int y = slidersTop + (index % SLIDER_ROWS) * ROW_HEIGHT;

         AxisSlider slider = new AxisSlider(x, y, sliderWidth, axis);
         this.sliders[index] = slider;
         this.addRenderableWidget(slider);
      }

      int resetTop = slidersTop + SLIDER_ROWS * ROW_HEIGHT + SPACING;
      int halfWidth = (CONTENT_WIDTH - SPACING) / 2;
      this.addRenderableWidget(Button.builder(Component.translatable("screen.postmortem.pose.reset_part"), b -> this.resetPart())
         .bounds(this.contentLeft, resetTop, halfWidth, WIDGET_HEIGHT)
         .build());
      this.addRenderableWidget(Button.builder(Component.translatable("screen.postmortem.pose.reset_all"), b -> this.resetAll())
         .bounds(this.contentLeft + halfWidth + SPACING, resetTop, halfWidth, WIDGET_HEIGHT)
         .build());

      int doneTop = resetTop + ROW_HEIGHT + SPACING;
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
         .bounds(this.contentLeft, doneTop, CONTENT_WIDTH, WIDGET_HEIGHT)
         .build());

      this.select(this.selected);
   }

   private void select(StatuePart part) {
      this.selected = part;
      if (!this.turnedByHand) {
         this.targetYaw = previewYawFor(part);
      }

      LimbTransform transform = this.working.get(part);
      for (AxisSlider slider : this.sliders) {
         slider.syncFrom(transform);
      }
   }

   //? if <1.21.9 {
   
   private record MouseButtonEvent(double x, double y, int button) {
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
   //?} else {
   /*@Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
   *///?}
      if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.inPreview(event.x(), event.y())) {
         StatuePart part = this.picker.pick(event.x(), event.y());
         if (part != null) {
            this.select(part);
            Minecraft.getInstance()
               .getSoundManager()
               .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         }
      }

      //? if >=1.21.9 {
      /*return super.mouseClicked(event, doubleClick);
      *///?} else
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   //? if >=1.21.9 {
   /*public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
   *///?} else {
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
   //?}
      if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && this.inPreview(event.x(), event.y())) {
         this.turnedByHand = true;
         this.previewYaw = Mth.wrapDegrees(this.previewYaw - (float) dragX * DRAG_DEGREES_PER_PIXEL);
         this.targetYaw = this.previewYaw;
         this.previewPitch = Mth.clamp(
            this.previewPitch - (float) dragY * DRAG_DEGREES_PER_PIXEL, -MAX_PITCH, MAX_PITCH);
         return true;
      }

      //? if >=1.21.9 {
      /*return super.mouseDragged(event, dragX, dragY);
      *///?} else
      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   private boolean inPreview(double mouseX, double mouseY) {
      return mouseX >= this.contentLeft
         && mouseX <= this.contentLeft + CONTENT_WIDTH
         && mouseY >= this.contentTop
         && mouseY <= this.previewBottom;
   }

   private void edit(LimbTransform transform) {
      this.working = this.working.with(this.selected, transform);
      this.statue.setLimbPose(this.working);
      this.dirty = true;
   }

   private void resetPart() {
      this.edit(LimbTransform.NONE);
      for (AxisSlider slider : this.sliders) {
         slider.syncFrom(LimbTransform.NONE);
      }
   }

   private void resetAll() {
      this.working = StatuePose.DEFAULT;
      this.statue.setLimbPose(this.working);
      this.dirty = true;
      this.select(this.selected);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.statue.isRemoved()
         || this.minecraft.player == null
         || this.minecraft.player.distanceToSqr(this.statue) > CosmeticStatueEntity.EDIT_RANGE_SQUARED) {
         this.onClose();
         return;
      }

      if (this.dirty && ++this.ticksSinceSend >= SEND_INTERVAL) {
         this.send();
      }
   }

   private void send() {
      ClientPlatform.INSTANCE.sendToServer(new PostmortemRegistry.SetPose(this.statue.getId(), this.working));
      this.dirty = false;
      this.ticksSinceSend = 0;
   }

   @Override
   public void removed() {
      StatueHighlightLayer.clear();
      if (this.dirty) {
         this.send();
      }

      super.removed();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private static float previewYawFor(StatuePart part) {
      return part == StatuePart.BACK_ITEM ? BACK_YAW : FRONT_YAW;
   }

   private void drawPreview(GuiGraphics context, float centreX) {
      //? if >=1.21.2 {
      /*EntityRenderState raw = Minecraft.getInstance()
         .getEntityRenderDispatcher()
         .getRenderer(this.statue)
         .createRenderState(this.statue, 1.0F);
      if (!(raw instanceof StatueEntityRenderer.State state)) {
         return;
      }
      *///?} else {
      Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this.statue);
      if (!(renderer instanceof StatueEntityRenderer<?> statueRenderer)) {
         return;
      }
      StatueEntityRenderer.State state = statueRenderer.createRenderState(this.statue, 1.0F);
      //?}

      //? if >=1.21.9 {
      /*state.lightCoords = LightTexture.FULL_BRIGHT;
      state.shadowPieces.clear();
      state.outlineColor = 0;
      *///?} elif >=1.21.2 {
      /*state.nameTag = null;
      *///?}
      state.preview = true;
      state.bodyRot = this.previewYaw;
      state.yRot = 0.0F;
      state.xRot = 0.0F;
      state.boundingBoxWidth /= state.scale;
      state.boundingBoxHeight /= state.scale;
      state.scale = 1.0F;

      Quaternionf cameraAngle = new Quaternionf().rotateX(this.previewPitch * Mth.DEG_TO_RAD);
      Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(cameraAngle);
      Vector3f translate = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + PREVIEW_LIFT, 0.0F);

      float centreY = this.contentTop + PREVIEW_HEIGHT / 2.0F;
      this.picker.update(state, centreX, centreY, PREVIEW_SIZE, translate, rotation, state.bodyRot);

      //? if >=26.1 {
      /*context.entity(
      *///?} elif >=1.21.6 {
      /*context.submitEntityRenderState(
      *///?} else
      drawEntity(context,
         state, PREVIEW_SIZE, translate, rotation, cameraAngle,
         this.contentLeft, this.contentTop, this.contentLeft + CONTENT_WIDTH, this.previewBottom);
   }

   //? if <1.21.6 {
   
   private void drawEntity(
      GuiGraphics context, StatueEntityRenderer.State state, float size, Vector3f translate,
      Quaternionf rotation, Quaternionf cameraAngle, int left, int top, int right, int bottom) {

      context.enableScissor(left, top, right, bottom);
      context.pose().pushPose();
      context.pose().translate((left + right) / 2.0F, (top + bottom) / 2.0F, 50.0F);
      //? if >=1.20.5 {
      /*context.pose().scale(size, size, -size);
      *///?} else {
      
      context.pose().mulPoseMatrix(new org.joml.Matrix4f().scaling(size, size, -size));
      //?}
      context.pose().translate(translate.x, translate.y, translate.z);
      context.pose().mulPose(rotation);
      context.flush();
      com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();

      var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      dispatcher.overrideCameraOrientation(cameraAngle.conjugate(new Quaternionf()).rotateY((float) Math.PI));
      dispatcher.setRenderShadow(false);
      var renderer = (StatueEntityRenderer<?>) (Object) dispatcher.getRenderer(this.statue);
      //? if >=1.21.2 {
      /*context.drawSpecial(buffers -> renderer.render(state, context.pose(), buffers, LightTexture.FULL_BRIGHT));
      *///?} else {
      com.mojang.blaze3d.systems.RenderSystem.runAsFancy(() -> renderer.renderPreview(
         state, context.pose(), context.bufferSource(), LightTexture.FULL_BRIGHT));
      //?}
      context.flush();
      dispatcher.setRenderShadow(true);

      context.pose().popPose();
      com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
      context.disableScissor();
   }
   //?}

   @Override
   //? if >=26.1 {
   /*public void extractRenderState(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
      super.extractRenderState(context, mouseX, mouseY, deltaTicks);
   *///?} else {
   public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
   //?}

      this.previewYaw = Mth.rotLerp(Math.min(1.0F, deltaTicks * TURN_SPEED), this.previewYaw, this.targetYaw);

      context.drawCenteredString(this.font, this.title, this.contentLeft + CONTENT_WIDTH / 2, this.contentTop - 16, 0xFFFFFF);
      context.fill(this.contentLeft, this.contentTop, this.contentLeft + CONTENT_WIDTH, this.previewBottom, PANEL_COLOUR);

      this.hovered = this.inPreview(mouseX, mouseY) ? this.picker.pick(mouseX, mouseY) : null;
      StatueHighlightLayer.set(this.statue.getId(), this.selected, this.hovered);

      this.drawPreview(context, this.contentLeft + CONTENT_WIDTH / 2.0F);

      boolean previewing = this.hovered != null && this.hovered != this.selected;
      StatuePart named = previewing ? this.hovered : this.selected;
      context.drawCenteredString(
         this.font,
         Component.translatable("screen.postmortem.pose.editing", Component.translatable(named.translationKey()))
            .withStyle(previewing ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
         this.contentLeft + CONTENT_WIDTH / 2,
         this.previewBottom + 2,
         0xFFFFFF);
   }

   private class AxisSlider extends AbstractSliderButton {
      private final Axis axis;

      AxisSlider(int x, int y, int width, Axis axis) {
         super(x, y, width, WIDGET_HEIGHT, Component.empty(), 0.5);
         this.axis = axis;
         this.syncFrom(StatuePoseScreen.this.working.get(StatuePoseScreen.this.selected));
      }

      void syncFrom(LimbTransform transform) {
         float limit = this.axis.limit();
         this.value = Mth.clamp((this.axis.read(transform) + limit) / (limit * 2.0F), 0.0, 1.0);
         this.updateMessage();
      }

      private int currentValue() {
         float limit = this.axis.limit();
         return (int) Math.round(this.value * limit * 2.0 - limit);
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.translatable(this.axis.translationKey())
            .append(": ")
            .append(String.valueOf(this.currentValue())));
      }

      @Override
      protected void applyValue() {
         StatuePoseScreen screen = StatuePoseScreen.this;
         screen.edit(this.axis.write(screen.working.get(screen.selected), this.currentValue()));
      }
   }
}
