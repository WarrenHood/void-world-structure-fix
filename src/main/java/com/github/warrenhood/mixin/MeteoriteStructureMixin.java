package com.github.warrenhood.mixin;

import appeng.worldgen.meteorite.MeteoriteStructure;
import com.github.warrenhood.FloatingMeteoriteMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = MeteoriteStructure.class, remap = false)
public abstract class MeteoriteStructureMixin {
    @ModifyArgs(
            method = "generatePieces",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/worldgen/meteorite/MeteoriteStructurePiece;<init>(Lnet/minecraft/core/BlockPos;FLappeng/worldgen/meteorite/CraterType;Lappeng/worldgen/meteorite/fallout/FalloutMode;ZZ)V"
            ),
            remap = false
    )
    private static void ae2aetherial$adjustMeteorite(Args args,
                                                      StructurePiecesBuilder piecesBuilder,
                                                      Structure.GenerationContext context) {
        BlockPos original = (BlockPos) args.get(0);
        float radius = (Float) args.get(1);
        ChunkGenerator generator = context.chunkGenerator();
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight();
        int radiusOffset = (int) Math.ceil(radius) + 1;

        // OCEAN_FLOOR_WG intentionally ignores water. An empty void column returns
        // approximately minY, while a real floating island returns its solid surface.
        int surfaceY = generator.getBaseHeight(
                original.getX(), original.getZ(), Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());

        if (surfaceY > minY + 16 && surfaceY < maxY - 24) {
            // Use the actual column surface instead of AE2's average of solid and void columns.
            int correctedY = surfaceY - radiusOffset;
            args.set(0, new BlockPos(original.getX(), correctedY, original.getZ()));
        } else {
            // No island sits directly above this position. Put the meteorite at the
            // height of the nearest nearby island, or use a safe mid-sky fallback.
            // The selected value is the island surface (first free block), so AE2
            // receives the same center offset it would use on ordinary terrain.
            int islandTopY = findNearbyIslandHeight(generator, context, original, minY, maxY);
            islandTopY = Math.max(minY + 32, Math.min(maxY - 48, islandTopY));
            int floatingMeteoriteY = islandTopY - radiusOffset;
            BlockPos floatingCenter = new BlockPos(original.getX(), floatingMeteoriteY, original.getZ());

            args.set(0, floatingCenter);
            FloatingMeteoriteMarker.markNextPiece(floatingCenter, radius);
        }

        // AE2's lake heuristic interprets void columns as water and then generates
        // long support pillars. Floating-island worlds do not have a meaningful
        // single sea level, so omit this optional lake pass for these meteorites.
        args.set(5, false);
    }

    private static int findNearbyIslandHeight(ChunkGenerator generator,
                                               Structure.GenerationContext context,
                                               BlockPos center, int minY, int maxY) {
        int nearestHeight = Integer.MIN_VALUE;
        int nearestDistanceSquared = Integer.MAX_VALUE;
        int sampleRadius = 64;
        int sampleStep = 8;

        for (int dx = -sampleRadius; dx <= sampleRadius; dx += sampleStep) {
            for (int dz = -sampleRadius; dz <= sampleRadius; dz += sampleStep) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared >= nearestDistanceSquared) {
                    continue;
                }

                int height = generator.getBaseHeight(
                        center.getX() + dx, center.getZ() + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState());

                if (height > minY + 16 && height < maxY - 24) {
                    nearestHeight = height;
                    nearestDistanceSquared = distanceSquared;
                }
            }
        }

        if (nearestHeight != Integer.MIN_VALUE) {
            return nearestHeight;
        }

        // Aetherial Islands uses the overworld's normal sea-level anchor in most packs.
        return generator.getSeaLevel() + 40;
    }
}
