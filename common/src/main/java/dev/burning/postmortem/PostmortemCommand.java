package dev.burning.postmortem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.burning.postmortem.death.MemorialRegistry;
import dev.burning.postmortem.entity.MemorialStatueEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.9 {
/*import net.minecraft.server.players.NameAndId;
*///?} else
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.Entity;

public final class PostmortemCommand {
   private PostmortemCommand() {
   }

   private record Memorial(UUID id, MemorialRegistry.Entry entry, MemorialStatueEntity statue, Component name, String owner) {
      int storedCount() {
         return this.statue != null ? this.statue.getStoredCount() : this.entry.stacks().size();
      }
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(Commands.literal("postmortem")
         //? if >=1.21.11 {
         /*.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
         *///?} else
         .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
         .then(Commands.literal("locate")
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
               .executes(context -> locate(context.getSource(), byOwner(context)))))
         .then(Commands.literal("retrieve")
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
               .executes(context -> retrieve(context.getSource(), byOwner(context), null))
               .then(Commands.literal("to")
                  .then(Commands.argument("receiver", EntityArgument.player())
                     .executes(context -> retrieve(context.getSource(), byOwner(context), EntityArgument.getPlayer(context, "receiver"))))))
            .then(Commands.literal("entity")
               .then(Commands.argument("targets", EntityArgument.entities())
                  .executes(context -> retrieve(context.getSource(), bySelector(context), null))
                  .then(Commands.literal("to")
                     .then(Commands.argument("receiver", EntityArgument.player())
                        .executes(context -> retrieve(context.getSource(), bySelector(context), EntityArgument.getPlayer(context, "receiver"))))))))
         .then(Commands.literal("delete")
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
               .executes(context -> delete(context.getSource(), byOwner(context))))
            .then(Commands.literal("entity")
               .then(Commands.argument("targets", EntityArgument.entities())
                  .executes(context -> delete(context.getSource(), bySelector(context)))))));
   }

   private static int locate(CommandSourceStack source, List<Memorial> found) {
      if (found.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("command.postmortem.none"), false);
         return 0;
      }

      for (Memorial memorial : found) {
         BlockPos pos = memorial.statue() != null ? memorial.statue().blockPosition() : memorial.entry().pos();
         String dimension = memorial.entry().dimension().location().toString();
         int stored = memorial.storedCount();

         if (memorial.statue() != null) {
            source.sendSuccess(
               () -> Component.translatable(
                     "command.postmortem.found", memorial.name(), dimension, pos.getX(), pos.getY(), pos.getZ(), stored)
                  //? if >=1.21.5 {
                  /*.withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/postmortem retrieve entity " + memorial.id()))),
                  *///?} else
                  .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/postmortem retrieve entity " + memorial.id()))),
               false);
         } else {
            source.sendSuccess(
               () -> Component.translatable(
                     "command.postmortem.found_unloaded", memorial.name(), dimension, pos.getX(), pos.getY(), pos.getZ(), stored)
                  //? if >=1.21.5 {
                  /*.withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/postmortem retrieve " + memorial.owner()))),
                  *///?} else
                  .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/postmortem retrieve " + memorial.owner()))),
               false);
         }
      }

      return found.size();
   }

   private static int retrieve(CommandSourceStack source, List<Memorial> found, ServerPlayer receiver) throws CommandSyntaxException {
      if (found.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("command.postmortem.none"), false);
         return 0;
      }

      ServerPlayer target = receiver != null ? receiver : source.getPlayerOrException();
      MemorialRegistry registry = MemorialRegistry.get(source.getServer());
      int returned = 0;
      for (Memorial memorial : found) {
         if (memorial.statue() != null) {
            returned += memorial.statue().giveTo(target);
         } else {
            int experience = registry.takeExperience(memorial.id());
            returned += MemorialStatueEntity.restore(target, registry.take(memorial.id()));
            if (experience > 0) {
               target.giveExperiencePoints(experience);
            }
         }
      }

      int stacks = returned;
      int statues = found.size();
      if (receiver != null) {
         source.sendSuccess(() -> Component.translatable("command.postmortem.retrieved_to", stacks, statues, target.getDisplayName()), true);
      } else {
         source.sendSuccess(() -> Component.translatable("command.postmortem.retrieved", stacks, statues), true);
      }
      return stacks;
   }

   private static int delete(CommandSourceStack source, List<Memorial> found) {
      if (found.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("command.postmortem.none"), false);
         return 0;
      }

      MemorialRegistry registry = MemorialRegistry.get(source.getServer());
      int destroyed = 0;
      for (Memorial memorial : found) {
         destroyed += memorial.statue() != null ? memorial.statue().destroy() : registry.take(memorial.id()).size();
      }

      int stacks = destroyed;
      int statues = found.size();
      Postmortem.LOGGER.info("[postmortem] {} deleted {} statue(s), destroying {} stack(s)", source.getTextName(), statues, stacks);
      source.sendSuccess(() -> Component.translatable("command.postmortem.deleted", statues, stacks), true);
      return statues;
   }

   private static List<Memorial> byOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      //? if >=1.21.9 {
      /*Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "player");
      *///?} else
      Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
      MinecraftServer server = context.getSource().getServer();
      MemorialRegistry registry = MemorialRegistry.get(server);

      List<Memorial> found = new ArrayList<>();
      //? if >=1.21.9 {
      /*for (NameAndId profile : profiles) {
         UUID owner = profile.id();
         String ownerName = profile.name();
      *///?} else {
      for (GameProfile profile : profiles) {
         UUID owner = profile.getId();
         String ownerName = profile.getName();
      //?}
         registry.ownedBy(owner).forEach((id, entry) -> {
            MemorialStatueEntity statue = loadedStatue(server, id, entry);
            Component name = statue != null
               ? statue.getName()
               : Component.translatable("entity.postmortem.memorial_statue.named", ownerName);
            found.add(new Memorial(id, entry, statue, name, ownerName));
         });
      }

      return found;
   }

   private static List<Memorial> bySelector(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      MemorialRegistry registry = MemorialRegistry.get(context.getSource().getServer());
      List<Memorial> found = new ArrayList<>();
      for (Entity entity : EntityArgument.getEntities(context, "targets")) {
         if (entity instanceof MemorialStatueEntity statue) {
            MemorialRegistry.Entry entry = registry.get(statue.getUUID());
            if (entry != null) {
               found.add(new Memorial(statue.getUUID(), entry, statue, statue.getName(), ""));
            }
         }
      }

      return found;
   }

   private static MemorialStatueEntity loadedStatue(MinecraftServer server, UUID id, MemorialRegistry.Entry entry) {
      ServerLevel world = server.getLevel(entry.dimension());
      if (world != null && world.getEntity(id) instanceof MemorialStatueEntity statue && !statue.isRemoved()) {
         return statue;
      }

      return null;
   }
}
