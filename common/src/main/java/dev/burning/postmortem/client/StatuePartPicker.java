package dev.burning.postmortem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.burning.postmortem.compat.BackSlots;
import dev.burning.postmortem.entity.StatuePart;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public final class StatuePartPicker {

   private static final float MODEL_LIFT = -1.501F;
   private static final float STATUE_SCALE = 0.9375F;

   public record Box(float left, float top, float right, float bottom) {
      public boolean contains(double x, double y) {
         return x >= this.left && x <= this.right && y >= this.top && y <= this.bottom;
      }

      public float area() {
         return (this.right - this.left) * (this.bottom - this.top);
      }
   }

   private final StatueEntityModel model;
   private final Map<StatuePart, Box> boxes = new EnumMap<>(StatuePart.class);

   public StatuePartPicker() {
      this.model = new StatueEntityModel(
         Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM));
   }

   public StatuePart pick(double mouseX, double mouseY) {
      StatuePart best = null;
      float bestArea = Float.MAX_VALUE;

      for (Map.Entry<StatuePart, Box> entry : this.boxes.entrySet()) {
         Box box = entry.getValue();
         if (box.contains(mouseX, mouseY) && box.area() < bestArea) {
            best = entry.getKey();
            bestArea = box.area();
         }
      }

      return best;
   }

   public void update(
      StatueEntityRenderer.State state,
      float centerX,
      float centerY,
      float scale,
      Vector3fc translation,
      Quaternionfc rotation,
      float bodyRot) {

      this.boxes.clear();
      this.model.setupAnim(state);

      PoseStack matrices = project(centerX, centerY, scale, translation, rotation, bodyRot);

      put(StatuePart.HEAD, this.model.head, matrices);
      put(StatuePart.BODY, this.model.body, matrices);
      put(StatuePart.RIGHT_ARM, this.model.rightArm, matrices);
      put(StatuePart.LEFT_ARM, this.model.leftArm, matrices);
      put(StatuePart.RIGHT_LEG, this.model.rightLeg, matrices);
      put(StatuePart.LEFT_LEG, this.model.leftLeg, matrices);

      putItem(StatuePart.RIGHT_HAND_ITEM, state, matrices);
      putItem(StatuePart.LEFT_HAND_ITEM, state, matrices);
      putItem(StatuePart.BACK_ITEM, state, matrices);
      putItem(StatuePart.BELT_ITEM, state, matrices);
   }

   private static PoseStack project(
      float centerX, float centerY, float scale, Vector3fc translation, Quaternionfc rotation, float bodyRot) {

      PoseStack matrices = new PoseStack();
      matrices.translate(centerX, centerY, 0.0F);
      matrices.scale(scale, scale, -scale);
      matrices.translate(translation.x(), translation.y(), translation.z());
      matrices.mulPose(new Quaternionf(rotation));
      matrices.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
      matrices.scale(STATUE_SCALE, STATUE_SCALE, STATUE_SCALE);
      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.translate(0.0F, MODEL_LIFT, 0.0F);
      return matrices;
   }
   private void putItem(StatuePart part, StatueEntityRenderer.State state, PoseStack matrices) {
      if (!BackSlots.has(part)) {
         return;
      }

      matrices.pushPose();
      Bounds bounds = new Bounds();
      if (StatueHighlightLayer.applyItemAnchor(matrices, this.model, state, part)) {
         bounds.acceptCube(matrices, StatueHighlightLayer.ITEM_RADIUS);
      }
      matrices.popPose();
      bounds.storeInto(part, this.boxes);
   }

   private void put(StatuePart part, ModelPart modelPart, PoseStack matrices) {
      Bounds bounds = new Bounds();
      //? if >=1.21.11 {
      /*modelPart.getExtentsForGui(matrices, bounds);
      *///?} elif >=1.21.9 {
      /*java.util.Set<org.joml.Vector3f> corners = new java.util.HashSet<>();
      modelPart.getExtentsForGui(matrices, corners);
      corners.forEach(bounds);
      *///?} elif >=1.21.2 {
      // What getExtentsForGui does from 1.21.9; vertex positions are still in pixels here.
      modelPart.visit(matrices, (pose, path, index, cube) -> {
         for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
               bounds.accept(pose.pose().transformPosition(
                  vertex.pos().x() / 16.0F, vertex.pos().y() / 16.0F, vertex.pos().z() / 16.0F, new org.joml.Vector3f()));
            }
         }
      });
      //?} else {
      /*// Polygons are private here; a cube's corners bound the same box, give or take its inflation.
      modelPart.visit(matrices, (pose, path, index, cube) -> {
         for (int corner = 0; corner < 8; corner++) {
            bounds.accept(pose.pose().transformPosition(
               ((corner & 1) == 0 ? cube.minX : cube.maxX) / 16.0F,
               ((corner & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
               ((corner & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F,
               new org.joml.Vector3f()));
         }
      });
      *///?}
      bounds.storeInto(part, this.boxes);
   }


   private static final class Bounds implements java.util.function.Consumer<Vector3fc> {
      private float minX = Float.MAX_VALUE;
      private float minY = Float.MAX_VALUE;
      private float maxX = -Float.MAX_VALUE;
      private float maxY = -Float.MAX_VALUE;

      @Override
      public void accept(Vector3fc corner) {
         this.minX = Math.min(this.minX, corner.x());
         this.minY = Math.min(this.minY, corner.y());
         this.maxX = Math.max(this.maxX, corner.x());
         this.maxY = Math.max(this.maxY, corner.y());
      }

      void acceptCube(PoseStack matrices, float radius) {
         org.joml.Matrix4f pose = matrices.last().pose();
         for (int corner = 0; corner < 8; corner++) {
            org.joml.Vector3f point = pose.transformPosition(
               (corner & 1) == 0 ? -radius : radius,
               (corner & 2) == 0 ? -radius : radius,
               (corner & 4) == 0 ? -radius : radius,
               new org.joml.Vector3f());
            this.accept(point);
         }
      }

      void storeInto(StatuePart part, Map<StatuePart, Box> boxes) {
         if (this.minX <= this.maxX) {
            boxes.put(part, new Box(this.minX, this.minY, this.maxX, this.maxY));
         }
      }
   }
}
