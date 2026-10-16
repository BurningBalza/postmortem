package dev.burning.postmortem;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public record PostmortemConfig(
   double collectRadius,
   int breakAfterMinutes,
   int placementHorizontalSearch,
   int placementVerticalSearch,
   int placementVoidSearch,
   int placementVoidStep,
   boolean debug) {
   public static final double DEFAULT_COLLECT_RADIUS = 8.0;
   public static final double MIN_COLLECT_RADIUS = 1.0;
   public static final double MAX_COLLECT_RADIUS = 64.0;

   public static final int DEFAULT_BREAK_AFTER_MINUTES = 30;
   public static final int NEVER_BREAKABLE = -1;
   public static final int MAX_BREAK_AFTER_MINUTES = 43200;

   public static final int DEFAULT_PLACEMENT_HORIZONTAL_SEARCH = 8;
   public static final int MAX_PLACEMENT_HORIZONTAL_SEARCH = 64;

   public static final int DEFAULT_PLACEMENT_VERTICAL_SEARCH = 8;
   public static final int MAX_PLACEMENT_VERTICAL_SEARCH = 128;

   public static final int DEFAULT_PLACEMENT_VOID_SEARCH = 64;
   public static final int MAX_PLACEMENT_VOID_SEARCH = 512;

   public static final int DEFAULT_PLACEMENT_VOID_STEP = 8;
   public static final int MIN_PLACEMENT_VOID_STEP = 1;
   public static final int MAX_PLACEMENT_VOID_STEP = 64;

   public static final PostmortemConfig DEFAULT = new PostmortemConfig(
      DEFAULT_COLLECT_RADIUS,
      DEFAULT_BREAK_AFTER_MINUTES,
      DEFAULT_PLACEMENT_HORIZONTAL_SEARCH,
      DEFAULT_PLACEMENT_VERTICAL_SEARCH,
      DEFAULT_PLACEMENT_VOID_SEARCH,
      DEFAULT_PLACEMENT_VOID_STEP,
      false);
   public static final Codec<PostmortemConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.DOUBLE.optionalFieldOf("collect_radius", DEFAULT_COLLECT_RADIUS).forGetter(PostmortemConfig::collectRadius),
      Codec.INT.optionalFieldOf("break_after_minutes", DEFAULT_BREAK_AFTER_MINUTES).forGetter(PostmortemConfig::breakAfterMinutes),
      Codec.INT.optionalFieldOf("placement_horizontal_search", DEFAULT_PLACEMENT_HORIZONTAL_SEARCH).forGetter(PostmortemConfig::placementHorizontalSearch),
      Codec.INT.optionalFieldOf("placement_vertical_search", DEFAULT_PLACEMENT_VERTICAL_SEARCH).forGetter(PostmortemConfig::placementVerticalSearch),
      Codec.INT.optionalFieldOf("placement_void_search", DEFAULT_PLACEMENT_VOID_SEARCH).forGetter(PostmortemConfig::placementVoidSearch),
      Codec.INT.optionalFieldOf("placement_void_step", DEFAULT_PLACEMENT_VOID_STEP).forGetter(PostmortemConfig::placementVoidStep),
      Codec.BOOL.optionalFieldOf("debug", false).forGetter(PostmortemConfig::debug)
   ).apply(instance, PostmortemConfig::new));

   private static final List<String> KEYS = List.of(
      "collect_radius",
      "break_after_minutes",
      "placement_horizontal_search",
      "placement_vertical_search",
      "placement_void_search",
      "placement_void_step",
      "debug");

   private static PostmortemConfig loaded = DEFAULT;

   public static PostmortemConfig get() {
      return loaded;
   }

   public static void load() {
      Path path = FabricLoader.getInstance().getConfigDir().resolve("postmortem.json");

      if (!Files.exists(path)) {
         loaded = DEFAULT;
         write(path, DEFAULT);
         return;
      }

      try {
         JsonElement json = JsonParser.parseString(Files.readString(path));
         DataResult<PostmortemConfig> result = CODEC.parse(JsonOps.INSTANCE, json);
         boolean parsed = result.result().isPresent();
         loaded = result
            .resultOrPartial(error -> Postmortem.LOGGER.warn("[postmortem] {} is malformed, using defaults: {}", path, error))
            .orElse(DEFAULT)
            .clamped();

         if (parsed && json.isJsonObject() && !json.getAsJsonObject().keySet().containsAll(KEYS)) {
            write(path, loaded);
         }
      } catch (IOException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] could not read {}, using defaults", path, e);
         loaded = DEFAULT;
      }
   }

   private PostmortemConfig clamped() {
      double radius = Mth.clamp(this.collectRadius, MIN_COLLECT_RADIUS, MAX_COLLECT_RADIUS);
      int minutes = Mth.clamp(this.breakAfterMinutes, NEVER_BREAKABLE, MAX_BREAK_AFTER_MINUTES);
      int horizontal = Mth.clamp(this.placementHorizontalSearch, 0, MAX_PLACEMENT_HORIZONTAL_SEARCH);
      int vertical = Mth.clamp(this.placementVerticalSearch, 0, MAX_PLACEMENT_VERTICAL_SEARCH);
      int voidSearch = Mth.clamp(this.placementVoidSearch, 0, MAX_PLACEMENT_VOID_SEARCH);
      // A step of zero would make the void ring search loop forever.
      int voidStep = Mth.clamp(this.placementVoidStep, MIN_PLACEMENT_VOID_STEP, MAX_PLACEMENT_VOID_STEP);
      return radius == this.collectRadius
            && minutes == this.breakAfterMinutes
            && horizontal == this.placementHorizontalSearch
            && vertical == this.placementVerticalSearch
            && voidSearch == this.placementVoidSearch
            && voidStep == this.placementVoidStep
         ? this
         : new PostmortemConfig(radius, minutes, horizontal, vertical, voidSearch, voidStep, this.debug);
   }

   public long breakAfterTicks() {
      return this.breakAfterMinutes < 0 ? NEVER_BREAKABLE : this.breakAfterMinutes * 1200L;
   }

   private static void write(Path path, PostmortemConfig config) {
      try {
         Files.createDirectories(path.getParent());

         JsonObject json = new JsonObject();
         json.addProperty("collect_radius", config.collectRadius());
         json.addProperty("break_after_minutes", config.breakAfterMinutes());
         json.addProperty("placement_horizontal_search", config.placementHorizontalSearch());
         json.addProperty("placement_vertical_search", config.placementVerticalSearch());
         json.addProperty("placement_void_search", config.placementVoidSearch());
         json.addProperty("placement_void_step", config.placementVoidStep());
         json.addProperty("debug", config.debug());

         Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(json));
      } catch (IOException | RuntimeException e) {
         Postmortem.LOGGER.warn("[postmortem] could not write {}", path, e);
      }
   }
}
