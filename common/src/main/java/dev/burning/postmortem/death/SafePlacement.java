package dev.burning.postmortem.death;

import dev.burning.postmortem.Postmortem;
import dev.burning.postmortem.PostmortemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

public final class SafePlacement {
   private SafePlacement() {
   }

   public static BlockPos findFor(Level world, Entity statue, BlockPos deathPos) {
      PostmortemConfig config = PostmortemConfig.get();
      //? if >=1.21.2 {
      int bottom = world.getMinY();
      //?} else
      //int bottom = world.getMinBuildHeight();
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

   private static BlockPos log(String tier, BlockPos pos) {
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
      //? if >=1.20.5 {
      AABB box = statue.getType().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      //?} else
      //AABB box = statue.getType().getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      return world.getWorldBorder().isWithinBounds(box) && world.noCollision(statue, box);
   }

   private static boolean isIn(Level world, BlockPos pos, TagKey<Fluid> fluid) {
      return world.getFluidState(pos).is(fluid);
   }
}
