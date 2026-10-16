package dev.burning.postmortem;

import dev.burning.postmortem.entity.CosmeticStatueEntity;
import dev.burning.postmortem.entity.CosmeticStatueItem;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import dev.burning.postmortem.entity.StatueCollectorItem;
import dev.burning.postmortem.entity.StatueEntity;
import dev.burning.postmortem.entity.StatuePose;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Everything Postmortem registers: entities, items, the kept-on-death tag, and the pose screen's packets. */
public final class PostmortemRegistry {
   private static final float STATUE_WIDTH = 0.6F;
   private static final float STATUE_HEIGHT = 1.8F;

   public static final TagKey<Item> KEPT_ON_DEATH = TagKey.create(Registries.ITEM, Postmortem.id("kept_on_death"));

   public static final EntityType<MemorialStatueEntity> MEMORIAL_STATUE =
      registerEntity("memorial_statue", MemorialStatueEntity::new);
   public static final EntityType<CosmeticStatueEntity> COSMETIC_STATUE =
      registerEntity("cosmetic_statue", CosmeticStatueEntity::new);

   public static final Item COSMETIC_STATUE_ITEM =
      registerItem("cosmetic_statue", CosmeticStatueItem::new, new Properties().stacksTo(16));
   public static final Item STATUE_COLLECTOR =
      registerItem("statue_collector", StatueCollectorItem::new, new Properties().stacksTo(1));

   private PostmortemRegistry() {
   }

   public record OpenPoseScreen(int entityId) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<OpenPoseScreen> ID =
         new CustomPacketPayload.Type<>(Postmortem.id("open_pose_screen"));

      public static final StreamCodec<RegistryFriendlyByteBuf, OpenPoseScreen> CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, OpenPoseScreen::entityId,
         OpenPoseScreen::new);

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return ID;
      }
   }

   public record SetPose(int entityId, StatuePose pose) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<SetPose> ID =
         new CustomPacketPayload.Type<>(Postmortem.id("set_pose"));

      public static final StreamCodec<RegistryFriendlyByteBuf, SetPose> CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, SetPose::entityId,
         StatuePose.PACKET_CODEC, SetPose::pose,
         SetPose::new);

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return ID;
      }
   }

   public static void init() {
      FabricEntityDataRegistry.register(Postmortem.id("statue_pose"), StatuePose.TRACKED_HANDLER);

      FabricDefaultAttributeRegistry.register(MEMORIAL_STATUE, StatueEntity.createStatueAttributes());
      FabricDefaultAttributeRegistry.register(COSMETIC_STATUE, StatueEntity.createStatueAttributes());

      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
         .register(entries -> entries.insertAfter(Items.ARMOR_STAND, COSMETIC_STATUE_ITEM));
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
         .register(entries -> entries.insertAfter(Items.RECOVERY_COMPASS, STATUE_COLLECTOR));

      PayloadTypeRegistry.clientboundPlay().register(OpenPoseScreen.ID, OpenPoseScreen.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(SetPose.ID, SetPose.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(SetPose.ID, (payload, context) -> applyPose(context.player(), payload));
   }

   private static void applyPose(ServerPlayer player, SetPose payload) {
      if (!(player.level().getEntity(payload.entityId()) instanceof CosmeticStatueEntity statue)
         || player.distanceToSqr(statue) > CosmeticStatueEntity.EDIT_RANGE_SQUARED
         || statue.isWaxed()) {
         return;
      }

      statue.setLimbPose(payload.pose().clamped());

      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] {} posed statue {}", player.getName().getString(), statue.getUUID());
      }
   }

   private static <T extends StatueEntity> EntityType<T> registerEntity(String name, EntityType.EntityFactory<T> factory) {
      ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Postmortem.id(name));
      EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MISC)
         .sized(STATUE_WIDTH, STATUE_HEIGHT)
         .eyeHeight(STATUE_HEIGHT * 0.85F)
         .fireImmune()
         .clientTrackingRange(10)
         .build(key);
      return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
   }

   private static Item registerItem(String name, Function<Properties, Item> factory, Properties settings) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Postmortem.id(name));
      return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(settings.setId(key)));
   }
}
