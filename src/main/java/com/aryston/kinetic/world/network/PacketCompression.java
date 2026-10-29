package com.aryston.kinetic.world.network;

import java.util.zip.Deflater;

public final class PacketCompression {
    public static final int LEVEL = 4;

    private PacketCompression() {
    }

    public static Deflater newDeflater() {
        return new Deflater(LEVEL);
    }
}
