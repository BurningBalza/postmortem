package dev.burning.postmortem.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.burning.postmortem.Postmortem;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class GreyscaleSkins {
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

   private static final Map<String, Optional<ResourceLocation>> SKINS = new ConcurrentHashMap<>();

   private GreyscaleSkins() {
   }

   public static ResourceLocation get(UUID owner, String textures) {
      String key = owner + ";" + textures;
      Optional<ResourceLocation> skin = SKINS.get(key);
      if (skin == null) {
         SKINS.put(key, Optional.empty());
         load(key, owner, textures);
         return null;
      }

      return skin.orElse(null);
   }

   private static void load(String key, UUID owner, String textures) {
      Skin skin = parse(textures);
      CompletableFuture<NativeImage> image = skin != null ? download(skin.url()) : defaultSkin(owner);
      boolean slim = skin != null ? skin.slim() : defaultIsSlim(owner);
      ResourceLocation id = Postmortem.id("statue_skins/" + owner + "_" + Integer.toHexString(textures.hashCode()));

      image.thenApply(loaded -> greyscale(slim ? loaded : toSlimArms(loaded))).whenComplete((grey, error) -> {
         if (error != null || grey == null) {
            Postmortem.LOGGER.warn("[postmortem] could not load the skin of {} for their statue", owner, error);
            return;
         }

         Minecraft.getInstance().execute(() -> {
            //? if >=1.21.5 {
            /*DynamicTexture texture = new DynamicTexture(id::toString, grey);
            *///?} else
            DynamicTexture texture = new DynamicTexture(grey);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            SKINS.put(key, Optional.of(id));
         });
      });
   }

   private record Skin(String url, boolean slim) {
   }

   private static Skin parse(String textures) {
      if (textures.isEmpty()) {
         return null;
      }

      try {
         JsonObject json = JsonParser.parseString(new String(Base64.getDecoder().decode(textures), StandardCharsets.UTF_8))
            .getAsJsonObject();
         JsonObject skin = json.getAsJsonObject("textures").getAsJsonObject("SKIN");
         if (skin == null) {
            return null;
         }

         URI url = URI.create(skin.get("url").getAsString());
         if (!"textures.minecraft.net".equals(url.getHost())
            || !("http".equals(url.getScheme()) || "https".equals(url.getScheme()))) {
            return null;
         }

         JsonObject metadata = skin.getAsJsonObject("metadata");
         boolean slim = metadata != null && metadata.has("model") && "slim".equals(metadata.get("model").getAsString());
         return new Skin(url.toString(), slim);
      } catch (RuntimeException e) {
         return null;
      }
   }

   private static CompletableFuture<NativeImage> download(String url) {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
      return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
         if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
         }

         try {
            return processSkin(NativeImage.read(response.body()));
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }
      });
   }

   private static CompletableFuture<NativeImage> defaultSkin(UUID owner) {
      //? if >=1.21.9 {
      /*ResourceLocation texture = DefaultPlayerSkin.get(owner).body().texturePath();
      *///?} elif >=1.20.2 {
      ResourceLocation texture = DefaultPlayerSkin.get(owner).texture();
      //?} else
      //ResourceLocation texture = DefaultPlayerSkin.getDefaultSkin(owner);
      return CompletableFuture.supplyAsync(() -> {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
         if (resource.isEmpty()) {
            throw new IllegalStateException("missing " + texture);
         }

         try (InputStream in = resource.get().open()) {
            return processSkin(NativeImage.read(in));
         } catch (IOException e) {
            throw new IllegalStateException(e);
         }
      });
   }

   private static boolean defaultIsSlim(UUID owner) {
      //? if >=1.21.9 {
      /*return DefaultPlayerSkin.get(owner).model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
      *///?} elif >=1.20.2 {
      return DefaultPlayerSkin.get(owner).model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
      //?} else
      //return "slim".equals(DefaultPlayerSkin.getSkinModelName(owner));
   }

   private static NativeImage processSkin(NativeImage image) {
      int width = image.getWidth();
      int height = image.getHeight();
      if (width != 64 || height != 32 && height != 64) {
         image.close();
         throw new IllegalStateException("unsupported skin size " + width + "x" + height);
      }

      boolean legacy = height == 32;
      if (legacy) {
         NativeImage full = new NativeImage(64, 64, true);
         copy(image, full, 0, 0, 0, 0, 64, 32, false);
         image.close();
         image = full;
         fill(image, 0, 32, 64, 32, 0);
         copy(image, image, 4, 16, 20, 48, 4, 4, true);
         copy(image, image, 8, 16, 24, 48, 4, 4, true);
         copy(image, image, 0, 20, 24, 52, 4, 12, true);
         copy(image, image, 4, 20, 20, 52, 4, 12, true);
         copy(image, image, 8, 20, 16, 52, 4, 12, true);
         copy(image, image, 12, 20, 28, 52, 4, 12, true);
         copy(image, image, 44, 16, 36, 48, 4, 4, true);
         copy(image, image, 48, 16, 40, 48, 4, 4, true);
         copy(image, image, 40, 20, 40, 52, 4, 12, true);
         copy(image, image, 44, 20, 36, 52, 4, 12, true);
         copy(image, image, 48, 20, 32, 52, 4, 12, true);
         copy(image, image, 52, 20, 44, 52, 4, 12, true);
      }

      setNoAlpha(image, 0, 0, 32, 16);
      if (legacy) {
         notchTransparencyHack(image, 32, 0, 64, 32);
      }
      setNoAlpha(image, 0, 16, 64, 32);
      setNoAlpha(image, 16, 48, 48, 64);
      return image;
   }

   private static void notchTransparencyHack(NativeImage image, int x0, int y0, int x1, int y1) {
      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            if ((getPixel(image, x, y) >>> 24) < 128) {
               return;
            }
         }
      }

      fill(image, x0, y0, x1 - x0, y1 - y0, 0);
   }

   private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            setPixel(image, x, y, getPixel(image, x, y) | 0xFF000000);
         }
      }
   }

   private static NativeImage toSlimArms(NativeImage wide) {
      NativeImage slim = new NativeImage(64, 64, true);
      copy(wide, slim, 0, 0, 0, 0, 64, 64, false);
      slimArm(wide, slim, 40, 16, true);
      slimArm(wide, slim, 40, 32, true);
      slimArm(wide, slim, 32, 48, false);
      slimArm(wide, slim, 48, 48, false);
      wide.close();
      return slim;
   }

   private static void slimArm(NativeImage wide, NativeImage slim, int u, int v, boolean right) {
      int front = right ? 1 : 0;
      int back = right ? 0 : 1;
      copy(wide, slim, u + 4 + front, v, u + 4, v, 3, 4, false);
      copy(wide, slim, u + 8 + front, v, u + 7, v, 3, 4, false);
      copy(wide, slim, u + 8, v + 4, u + 7, v + 4, 4, 12, false);
      copy(wide, slim, u + 4 + front, v + 4, u + 4, v + 4, 3, 12, false);
      copy(wide, slim, u + 12 + back, v + 4, u + 11, v + 4, 3, 12, false);
   }

   private static NativeImage greyscale(NativeImage image) {
      for (int x = 0; x < image.getWidth(); x++) {
         for (int y = 0; y < image.getHeight(); y++) {
            int pixel = getPixel(image, x, y);
            //? if >=1.21.2 {
            int red = pixel >> 16 & 0xFF;
            int blue = pixel & 0xFF;
            //?} else {
            /*int red = pixel & 0xFF;
            int blue = pixel >> 16 & 0xFF;
            *///?}
            int green = pixel >> 8 & 0xFF;
            int grey = Math.round(0.299F * red + 0.587F * green + 0.114F * blue);
            setPixel(image, x, y, pixel & 0xFF000000 | grey << 16 | grey << 8 | grey);
         }
      }

      return image;
   }

   private static void copy(NativeImage from, NativeImage to, int x, int y, int toX, int toY, int width, int height, boolean mirror) {
      int[] pixels = new int[width * height];
      for (int dy = 0; dy < height; dy++) {
         for (int dx = 0; dx < width; dx++) {
            pixels[dy * width + dx] = getPixel(from, x + dx, y + dy);
         }
      }

      for (int dy = 0; dy < height; dy++) {
         for (int dx = 0; dx < width; dx++) {
            setPixel(to, toX + (mirror ? width - 1 - dx : dx), toY + dy, pixels[dy * width + dx]);
         }
      }
   }

   private static void fill(NativeImage image, int x, int y, int width, int height, int pixel) {
      for (int dy = 0; dy < height; dy++) {
         for (int dx = 0; dx < width; dx++) {
            setPixel(image, x + dx, y + dy, pixel);
         }
      }
   }

   private static int getPixel(NativeImage image, int x, int y) {
      //? if >=1.21.2 {
      return image.getPixel(x, y);
      //?} else
      //return image.getPixelRGBA(x, y);
   }

   private static void setPixel(NativeImage image, int x, int y, int pixel) {
      //? if >=1.21.2 {
      image.setPixel(x, y, pixel);
      //?} else
      //image.setPixelRGBA(x, y, pixel);
   }
}
