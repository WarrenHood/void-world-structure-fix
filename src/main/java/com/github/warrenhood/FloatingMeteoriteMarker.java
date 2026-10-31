package com.github.warrenhood;

import net.minecraft.core.BlockPos;

/** Transfers the generated-island marker from AE2's structure factory to its piece constructor. */
public final class FloatingMeteoriteMarker {
    private static final ThreadLocal<Pending> NEXT_PIECE = new ThreadLocal<>();

    private FloatingMeteoriteMarker() {
    }

    public static void markNextPiece(BlockPos center, float radius) {
        NEXT_PIECE.set(new Pending(center, radius));
    }

    public static Pending consume() {
        Pending pending = NEXT_PIECE.get();
        NEXT_PIECE.remove();
        return pending;
    }

    public record Pending(BlockPos center, float radius) {
    }
}
