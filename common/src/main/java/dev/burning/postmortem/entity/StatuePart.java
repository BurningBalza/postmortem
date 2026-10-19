package dev.burning.postmortem.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum StatuePart implements StringRepresentable {
   HEAD("head", "Head"),
   BODY("body", "Body"),
   RIGHT_ARM("right_arm", "RightArm"),
   LEFT_ARM("left_arm", "LeftArm"),
   RIGHT_LEG("right_leg", "RightLeg"),
   LEFT_LEG("left_leg", "LeftLeg"),
   RIGHT_HAND_ITEM("right_hand_item", "RightHandItem"),
   LEFT_HAND_ITEM("left_hand_item", "LeftHandItem"),
   BACK_ITEM("back_item", "BackItem"),
   BELT_ITEM("belt_item", "BeltItem");

   public static final Codec<StatuePart> CODEC = StringRepresentable.fromEnum(StatuePart::values);

   private final String id;

   private final String saveKey;

   StatuePart(String id, String saveKey) {
      this.id = id;
      this.saveKey = saveKey;
   }

   public String translationKey() {
      return "part.postmortem." + this.id;
   }

   @Override
   public String getSerializedName() {
      return this.saveKey;
   }
}
