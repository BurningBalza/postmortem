package dev.burning.postmortem.entity;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
//? if >=1.21.11 {
/*import net.minecraft.util.Util;
*///?} else
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if <1.21.9
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public final class ProfileLookup {
   private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

   private ProfileLookup() {
   }

   public static boolean isPlayerName(String name) {
      return PLAYER_NAME.matcher(name).matches();
   }

   public static UUID idOf(GameProfile profile) {
      //? if >=1.21.9 {
      /*return profile.id();
      *///?} else
      return profile.getId();
   }

   public static CompletableFuture<Optional<GameProfile>> byName(MinecraftServer server, String name) {
      ServerPlayer online = server.getPlayerList().getPlayerByName(name);
      if (online != null) {
         return CompletableFuture.completedFuture(Optional.of(online.getGameProfile()));
      }

      //? if >=1.21.9 {
      /*return CompletableFuture.supplyAsync(() -> server.services().profileResolver().fetchByName(name), Util.backgroundExecutor());
      *///?} elif >=1.20.5 {
      /*return SkullBlockEntity.fetchGameProfile(name);
      *///?} elif >=1.20.2 {
      /*net.minecraft.server.players.GameProfileCache cache = server.getProfileCache();
      if (cache == null) {
         return CompletableFuture.completedFuture(Optional.empty());
      }

      return cache.getAsync(name).thenApplyAsync(found -> found.map(profile -> {
         com.mojang.authlib.yggdrasil.ProfileResult result = server.getSessionService().fetchProfile(profile.getId(), true);
         return result != null ? result.profile() : profile;
      }), Util.backgroundExecutor());
      *///?} else {
      CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
      SkullBlockEntity.updateGameprofile(new GameProfile(null, name),
         profile -> future.complete(Optional.ofNullable(profile).filter(found -> found.getId() != null)));
      return future;
      //?}
   }
}
