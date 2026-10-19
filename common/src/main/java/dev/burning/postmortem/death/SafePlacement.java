package dev.burning.postmortem.death;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SafePlacement {
   private SafePlacement() {
   }

   private static final double SEAT_DEPTH = 0.6;

   public static Vec3 findFor(Level world, Entity statue, Entity player) {
      Vec3 kept = whereDied(world, statue, player);
      if (kept != null) {
         return log("death position", kept);
      }
      return Vec3.atBottomCenterOf(search(world, statue, player.blockPosition()));
   }

   private static Vec3 whereDied(Level world, Entity statue, Entity player) {
      AABB box = boxAt(statue, player.getX(), player.getY(), player.getZ());
      if (box.minY <= bottom(world) || !world.getWorldBorder().isWithinBounds(box)) {
         return null;
      }

      if (player.isPassenger() && !(player.getVehicle() instanceof LivingEntity)) {
         AABB aboveSeat = box.setMinY(box.minY + SEAT_DEPTH);
         return inBlocks(world, statue, aboveSeat) || inLava(world, aboveSeat) ? null : player.position();
      }

      if (inBlocks(world, statue, box)) {
         return null;
      }

      Vec3 spot = player.isInWater() ? player.position() : drop(world, statue, box);
      if (spot == null) {
         return null;
      }
      return inLava(world, box.move(0.0, spot.y - box.minY, 0.0)) ? null : spot;
   }

   private static Vec3 drop(Level world, Entity statue, AABB box) {
      int bottom = bottom(world);
      double floor = box.minY + Entity.collideBoundingBox(
         statue, new Vec3(0.0, bottom - box.minY, 0.0), box, world, List.of()).y;
      if (floor <= bottom + 1.0E-7) {
         return null;
      }

      Vec3 center = box.getCenter();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int y = Mth.floor(box.minY); y >= Mth.floor(floor); y--) {
         pos.set(Mth.floor(center.x), y, Mth.floor(center.z));
         FluidState fluid = world.getFluidState(pos);
         if (!fluid.isEmpty()) {
            floor = Math.max(floor, y + fluid.getHeight(world, pos));
            break;
         }
      }
      return new Vec3(center.x, floor, center.z);
   }

   private static boolean inBlocks(Level world, Entity statue, AABB box) {
      return world.getBlockCollisions(statue, box.deflate(1.0E-3)).iterator().hasNext();
   }

   private static boolean inLava(Level world, AABB box) {
      return world.getBlockStates(box.expandTowards(0.0, -0.5, 0.0).deflate(1.0E-3))
         .anyMatch(state -> state.getFluidState().is(FluidTags.LAVA));
   }

   private static int bottom(Level world) {
      //? if >=1.21.2 {
      /*return world.getMinY();
      *///?} else
      return world.getMinBuildHeight();
   }

   private static BlockPos search(Level world, Entity statue, BlockPos deathPos) {
      PostmortemConfig config = PostmortemConfig.get();
      int bottom = bottom(world);
      int minY = bottom + 1;
      int maxY = bottom + world.getHeight() - 2;
      BlockPos start = new BlockPos(deathPos.getX(), Mth.clamp(deathPos.getY(), minY, maxY), deathPos.getZ());

      WorldBorder border = world.getWorldBorder();
      if (!border.isWithinBounds(start)) {
         start = border.clampToBounds(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
         start = new BlockPos(start.getX(), Mth.clamp(start.getY(), minY, maxY), start.getZ());
         log("clamped inside world border", start);
      }

      for (int offset = 0; offset <= config.placementVerticalSearch(); offset++) {
         for (int direction : offset == 0 ? new int[]{0} : new int[]{-1, 1}) {
            int y = start.getY() + offset * direction;
            if (y < minY || y > maxY) {
               continue;
            }

            BlockPos found = searchLayer(world, statue, start, y, config.placementHorizontalSearch());
            if (found != null) {
               return log("nearby ground", found);
            }
         }
      }

      BlockPos lava = floatOnFluid(world, statue, start, maxY, FluidTags.LAVA);
      if (lava != null) {
         return log("lava surface", lava);
      }

      BlockPos water = floatOnFluid(world, statue, start, maxY, FluidTags.WATER);
      if (water != null) {
         return log("water surface", water);
      }

      BlockPos ground = firstGroundAbove(world, statue, start, maxY);
      if (ground != null) {
         return log("column floor", ground);
      }

      BlockPos terrain = nearestTerrain(
         world, statue, start, minY, maxY, config.placementVoidSearch(), config.placementVoidStep());
      if (terrain != null) {
         return log("nearest terrain", terrain);
      }

      return log("no safe spot found, using death position", start);
   }

   private static BlockPos nearestTerrain(
      Level world, Entity statue, BlockPos start, int minY, int maxY, int voidSearch, int voidStep) {
      for (int radius = voidStep; radius <= voidSearch; radius += voidStep) {
         for (int dx = -radius; dx <= radius; dx += voidStep) {
            for (int dz = -radius; dz <= radius; dz += voidStep) {
               if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                  continue;
               }

               int x = start.getX() + dx;
               int z = start.getZ() + dz;
               int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
               if (top <= minY || top > maxY) {
                  continue;
               }

               BlockPos candidate = new BlockPos(x, top, z);
               if (isSafe(world, statue, candidate)) {
                  return candidate;
               }
            }
         }
      }

      return null;
   }

   private static <T> T log(String tier, T pos) {
      if (PostmortemConfig.get().debug()) {
         Postmortem.LOGGER.info("[postmortem] placement via {} -> {}", tier, pos);
      }
      return pos;
   }

   private static BlockPos floatOnFluid(
      Level world, Entity statue, BlockPos start, int maxY, TagKey<Fluid> fluid) {
      if (!isIn(world, start, fluid) && !isIn(world, start.above(), fluid)) {
         return null;
      }

      int y = start.getY();
      while (y <= maxY && isIn(world, new BlockPos(start.getX(), y, start.getZ()), fluid)) {
         y++;
      }

      BlockPos surface = new BlockPos(start.getX(), y, start.getZ());
      return y <= maxY && !isIn(world, surface.above(), fluid) && fits(world, statue, surface) ? surface : null;
   }

   private static BlockPos firstGroundAbove(Level world, Entity statue, BlockPos start, int maxY) {
      for (int y = start.getY(); y <= maxY; y++) {
         BlockPos candidate = new BlockPos(start.getX(), y, start.getZ());
         if (isSafe(world, statue, candidate)) {
            return candidate;
         }
      }

      return null;
   }

   private static BlockPos searchLayer(
      Level world, Entity statue, BlockPos start, int y, int horizontalSearch) {
      for (int radius = 0; radius <= horizontalSearch; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (radius != 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                  continue;
               }

               BlockPos candidate = new BlockPos(start.getX() + dx, y, start.getZ() + dz);
               if (isSafe(world, statue, candidate)) {
                  return candidate;
               }
            }
         }
      }

      return null;
   }

   private static boolean isSafe(Level world, Entity statue, BlockPos pos) {
      if (!fits(world, statue, pos)) {
         return false;
      }

      if (isIn(world, pos, FluidTags.LAVA) || isIn(world, pos.above(), FluidTags.LAVA)) {
         return false;
      }

      return world.getBlockState(pos.below()).isRedstoneConductor(world, pos.below());
   }

   private static boolean fits(Level world, Entity statue, BlockPos pos) {
      AABB box = boxAt(statue, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      return world.getWorldBorder().isWithinBounds(box) && world.noCollision(statue, box);
   }

   private static AABB boxAt(Entity statue, double x, double y, double z) {
      //? if >=1.20.5 {
      /*return statue.getType().getSpawnAABB(x, y, z);
      *///?} else
      return statue.getType().getAABB(x, y, z);
   }

   private static boolean isIn(Level world, BlockPos pos, TagKey<Fluid> fluid) {
      return world.getFluidState(pos).is(fluid);
   }
}
