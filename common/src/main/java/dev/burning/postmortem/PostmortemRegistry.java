package dev.burning.postmortem;

import dev.burning.postmortem.compat.BackSlots;
import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.CosmeticStatueItem;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import dev.burning.postmortem.entity.StatueCollectorItem;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.entity.StatuePose;
import dev.burning.postmortem.platform.Platform;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import io.netty.buffer.ByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//?} else {
/*import dev.burning.postmortem.legacy.ByteBufCodecs;
import dev.burning.postmortem.legacy.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
*///?}
//? if >=1.20.2 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else
//import dev.burning.postmortem.legacy.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item;

/**
 * Everything Postmortem registers: entities, items, the kept-on-death tag, and the pose screen's packets. The
 * registered values are suppliers because NeoForge only fills them in during its registry event; the loader packages
 * register the attributes, creative tab entries and payloads themselves.
 */
public final class PostmortemRegistry {
   private static final float STATUE_WIDTH = 0.6F;
   private static final float STATUE_HEIGHT = 1.8F;

   public static final TagKey<Item> KEPT_ON_DEATH = TagKey.create(Registries.ITEM, Postmortem.id("kept_on_death"));

   public static final Supplier<EntityType<MemorialStatueEntity>> MEMORIAL_STATUE =
      registerEntity("memorial_statue", MemorialStatueEntity::new);
   public static final Supplier<EntityType<CosmeticStatueEntity>> COSMETIC_STATUE =
      registerEntity("cosmetic_statue", CosmeticStatueEntity::new);

   public static final Supplier<Item> COSMETIC_STATUE_ITEM =
      registerItem("cosmetic_statue", CosmeticStatueItem::new, () -> new Properties().stacksTo(16));
   public static final Supplier<Item> STATUE_COLLECTOR =
      registerItem("statue_collector", StatueCollectorItem::new, () -> new Properties().stacksTo(1));

   private PostmortemRegistry() {
   }

   public record OpenPoseScreen(int entityId) implements CustomPacketPayload {
      //? if >=1.20.5 {
      public static final CustomPacketPayload.Type<OpenPoseScreen> ID =
         new CustomPacketPayload.Type<>(Postmortem.id("open_pose_screen"));
      //?} else
      //public static final Identifier ID = Postmortem.id("open_pose_screen");

      public static final StreamCodec<ByteBuf, OpenPoseScreen> CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, OpenPoseScreen::entityId,
         OpenPoseScreen::new);

      //? if >=1.20.5 {
      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return ID;
      }
      //?} else {
      /*@Override
      public Identifier id() {
         return ID;
      }

      @Override
      public void write(FriendlyByteBuf buf) {
         CODEC.encode(buf, this);
      }
      *///?}
   }

   public record SetPose(int entityId, StatuePose pose) implements CustomPacketPayload {
      //? if >=1.20.5 {
      public static final CustomPacketPayload.Type<SetPose> ID =
         new CustomPacketPayload.Type<>(Postmortem.id("set_pose"));
      //?} else
      //public static final Identifier ID = Postmortem.id("set_pose");

      public static final StreamCodec<ByteBuf, SetPose> CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, SetPose::entityId,
         StatuePose.PACKET_CODEC, SetPose::pose,
         SetPose::new);

      //? if >=1.20.5 {
      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return ID;
      }
      //?} else {
      /*@Override
      public Identifier id() {
         return ID;
      }

      @Override
      public void write(FriendlyByteBuf buf) {
         CODEC.encode(buf, this);
      }
      *///?}
   }

   /** Loading this class registers its entries; the entity data serializer has no field, so it goes here. */
   public static void init() {
      Platform.INSTANCE.registerDataSerializer("statue_pose", StatuePose.TRACKED_HANDLER);
   }

   /** Server handler for {@link SetPose}, called by each loader's payload receiver. */
   public static void applyPose(ServerPlayer player, SetPose payload) {
      if (!(player.level().getEntity(payload.entityId()) instanceof CosmeticStatueEntity statue)
         || player.distanceToSqr(statue) > CosmeticStatueEntity.EDIT_RANGE_SQUARED
         || statue.isWaxed()) {
         return;
      }

      // Back and belt items are only posable when a mod provides those slots.
      statue.setLimbPose(payload.pose().clamped().retain(BackSlots::has));

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] {} posed statue {}", player.getName().getString(), statue.getUUID());
      }
   }

   private static <T extends StatueEntity> Supplier<EntityType<T>> registerEntity(String name, EntityType.EntityFactory<T> factory) {
      ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Postmortem.id(name));
      return Platform.INSTANCE.register(BuiltInRegistries.ENTITY_TYPE, name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
         .sized(STATUE_WIDTH, STATUE_HEIGHT)
         // Before 1.20.5 eye height is 85% of the height without being asked.
         //? if >=1.20.5
         .eyeHeight(STATUE_HEIGHT * 0.85F)
         .fireImmune()
         .clientTrackingRange(10)
         //? if >=1.21.2 {
         .build(key));
         //?} else
         //.build(key.identifier().toString()));
   }

   private static Supplier<Item> registerItem(String name, Function<Properties, Item> factory, Supplier<Properties> settings) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Postmortem.id(name));
      // Items carry their own id from 1.21.2; before it the registry is the only place it lives.
      //? if >=1.21.2 {
      return Platform.INSTANCE.register(BuiltInRegistries.ITEM, name, () -> factory.apply(settings.get().setId(key)));
      //?} else
      //return Platform.INSTANCE.register(BuiltInRegistries.ITEM, name, () -> factory.apply(settings.get()));
   }
}
