package com.github.warrenhood;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Builds a small, deterministic rocky island under an otherwise-void meteorite. */
public final class FloatingIslandGenerator {
    private FloatingIslandGenerator() {
    }

    public static void generate(WorldGenLevel level, BlockPos meteoriteCenter, float meteoriteRadius,
                                BoundingBox bounds, ChunkPos chunkPos) {
        int coreRadius = (int) Math.ceil(meteoriteRadius);

        // AE2 normally places the meteorite center about (ceil(radius) + 1) blocks
        // below the terrain surface. Recreate that surface height so its ordinary
        // crater logic carves a bowl into this island, rather than burying the island
        // at the bottom of the crater.
        int islandTopY = meteoriteCenter.getY() + coreRadius + 1;
        int islandRadius = 20 + coreRadius;

        int minX = Math.max(Math.max(meteoriteCenter.getX() - islandRadius, bounds.minX()), chunkPos.getMinBlockX());
        int maxX = Math.min(Math.min(meteoriteCenter.getX() + islandRadius, bounds.maxX()), chunkPos.getMaxBlockX());
        int minZ = Math.max(Math.max(meteoriteCenter.getZ() - islandRadius, bounds.minZ()), chunkPos.getMinBlockZ());
        int maxZ = Math.min(Math.min(meteoriteCenter.getZ() + islandRadius, bounds.maxZ()), chunkPos.getMaxBlockZ());

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x - meteoriteCenter.getX();
                double dz = z - meteoriteCenter.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);

                double edgeNoise = noise(x >> 2, z >> 2, meteoriteCenter.asLong());
                double localRadius = islandRadius * (0.91 + edgeNoise * 0.09);
                if (distance > localRadius) {
                    continue;
                }

                double normalizedDistance = distance / localRadius;
                double heightNoise = noise(x, z, meteoriteCenter.asLong() ^ 0x5DEECE66DL);
                int columnTop = islandTopY + (int) Math.round(heightNoise * 1.25);
                int thickness = 4 + (int) Math.round(Math.pow(1.0 - normalizedDistance, 1.25) * 19.0);
                int columnBottom = Math.max(level.getMinBuildHeight() + 1, columnTop - thickness);

                for (int y = columnBottom; y <= columnTop; y++) {
                    if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight()) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    // Never overwrite an existing island or another generated structure.
                    if (!level.isEmptyBlock(pos)) {
                        continue;
                    }

                    level.setBlock(pos, chooseRock(x, y, z, columnTop, meteoriteCenter.asLong()), 2);
                }
            }
        }
    }

    private static BlockState chooseRock(int x, int y, int z, int topY, long seed) {
        long value = mix64(seed
                ^ (long) x * 0x9E3779B97F4A7C15L
                ^ (long) y * 0xC2B2AE3D27D4EB4FL
                ^ (long) z * 0x165667B19E3779F9L);
        int pick = (int) Long.remainderUnsigned(value, 100);
        int depth = topY - y;

        if (depth == 0) {
            return pick < 82 ? Blocks.STONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
        }
        if (depth <= 2) {
            if (pick < 45) return Blocks.STONE.defaultBlockState();
            if (pick < 75) return Blocks.ANDESITE.defaultBlockState();
            return Blocks.TUFF.defaultBlockState();
        }
        if (pick < 42) return Blocks.STONE.defaultBlockState();
        if (pick < 76) return Blocks.TUFF.defaultBlockState();
        if (pick < 94) return Blocks.ANDESITE.defaultBlockState();
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private static double noise(int x, int z, long seed) {
        long value = mix64(seed ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL);
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
