package com.github.warrenhood.mixin;

import appeng.worldgen.meteorite.MeteoriteStructurePiece;
import com.github.warrenhood.FloatingIslandGenerator;
import com.github.warrenhood.FloatingMeteoriteMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MeteoriteStructurePiece.class, remap = false)
public abstract class MeteoriteStructurePieceMixin {
    @Unique private static final String ae2aetherial$FLAG = "ae2aetherial_floating_island";
    @Unique private static final String ae2aetherial$POS = "ae2aetherial_floating_island_pos";
    @Unique private static final String ae2aetherial$RADIUS = "ae2aetherial_floating_island_radius";

    @Unique private boolean ae2aetherial$hasFloatingIsland;
    @Unique private BlockPos ae2aetherial$floatingCenter;
    @Unique private float ae2aetherial$meteoriteRadius;

    @Inject(
            method = "<init>(Lnet/minecraft/core/BlockPos;FLappeng/worldgen/meteorite/CraterType;Lappeng/worldgen/meteorite/fallout/FalloutMode;ZZ)V",
            at = @At("TAIL"),
            remap = false
    )
    private void ae2aetherial$captureGeneratedIsland(BlockPos center, float coreRadius,
                                                       @Coerce Object craterType, @Coerce Object fallout,
                                                       boolean pureCrater, boolean craterLake, CallbackInfo ci) {
        FloatingMeteoriteMarker.Pending pending = FloatingMeteoriteMarker.consume();
        if (pending != null) {
            this.ae2aetherial$hasFloatingIsland = true;
            this.ae2aetherial$floatingCenter = pending.center();
            this.ae2aetherial$meteoriteRadius = pending.radius();
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    private void ae2aetherial$readIslandMarker(CompoundTag tag, CallbackInfo ci) {
        this.ae2aetherial$hasFloatingIsland = tag.getBoolean(ae2aetherial$FLAG);
        if (this.ae2aetherial$hasFloatingIsland) {
            this.ae2aetherial$floatingCenter = BlockPos.of(tag.getLong(ae2aetherial$POS));
            this.ae2aetherial$meteoriteRadius = tag.getFloat(ae2aetherial$RADIUS);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), remap = false)
    private void ae2aetherial$writeIslandMarker(StructurePieceSerializationContext context,
                                                 CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(ae2aetherial$FLAG, this.ae2aetherial$hasFloatingIsland);
        if (this.ae2aetherial$hasFloatingIsland && this.ae2aetherial$floatingCenter != null) {
            tag.putLong(ae2aetherial$POS, this.ae2aetherial$floatingCenter.asLong());
            tag.putFloat(ae2aetherial$RADIUS, this.ae2aetherial$meteoriteRadius);
        }
    }

    @Inject(method = "postProcess", at = @At("HEAD"), remap = false)
    private void ae2aetherial$generateIslandBeforeMeteorite(WorldGenLevel level,
                                                             StructureManager structureManager,
                                                             ChunkGenerator chunkGenerator,
                                                             RandomSource random,
                                                             BoundingBox bounds,
                                                             ChunkPos chunkPos,
                                                             BlockPos blockPos,
                                                             CallbackInfo ci) {
        if (this.ae2aetherial$hasFloatingIsland && this.ae2aetherial$floatingCenter != null) {
            FloatingIslandGenerator.generate(level, this.ae2aetherial$floatingCenter,
                    this.ae2aetherial$meteoriteRadius, bounds, chunkPos);
        }
    }
}
