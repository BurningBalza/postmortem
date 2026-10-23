package dev.burning.postmortem.entity;

import dev.burning.postmortem.Postmortem;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

public enum StatueVariant implements StringRepresentable {
   DEFAULT("default", null),
   ANGRY("angry", "angry_statue"),
   CONFUSED("confused", "confused_statue"),
   DISTRESS("distress", "distress_statue"),
   SHOCKED("shocked", "shocked_statue"),
   SCREAMING("screaming", "screaming_statue");

   private final String id;
   private final Identifier texture;

   StatueVariant(String id, String textureName) {
      this.id = id;
      this.texture = textureName == null ? null : Postmortem.id("textures/entity/" + textureName + ".png");
   }

   public Identifier texture() {
      return this.texture;
   }

   public String translationKey() {
      return "variant.postmortem." + this.id;
   }

   public StatueVariant next() {
      StatueVariant[] all = values();
      return all[(this.ordinal() + 1) % all.length];
   }

   public static StatueVariant byName(String name) {
      for (StatueVariant variant : values()) {
         if (variant.id.equals(name)) {
            return variant;
         }
      }

      return DEFAULT;
   }

   public static StatueVariant byOrdinal(int ordinal) {
      StatueVariant[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : DEFAULT;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
