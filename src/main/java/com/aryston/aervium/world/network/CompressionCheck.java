package com.aryston.aervium.world.network;

import com.aryston.aervium.Aervium;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Random;
import java.util.zip.Deflater;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.VarInt;

public final class CompressionCheck {
    private static final int THRESHOLD = 256;
    private static final int PAYLOAD_BYTES = 1 << 17;
    private static final int RANDOM_SHARE = 4;
    private static final int PATTERN_LENGTH = 7;
    private static final int BYTE_VALUES = 256;
    private static final int BUFFER_BYTES = 8192;
    private static final long SEED = 0x6B727970L;

    private CompressionCheck() {
    }

    public static void run() {
        byte[] payload = payload();
        EmbeddedChannel encoder = new EmbeddedChannel(new CompressionEncoder(THRESHOLD));
        EmbeddedChannel decoder = new EmbeddedChannel(new CompressionDecoder(THRESHOLD, true));
        try {
            encoder.writeOutbound(Unpooled.wrappedBuffer(payload));
            ByteBuf compressed = encoder.readOutbound();
            int deflatedBytes = compressed.readableBytes() - VarInt.getByteSize(payload.length);
            decoder.writeInbound(compressed);
            ByteBuf restored = decoder.readInbound();
            boolean identical = ByteBufUtil.equals(restored, Unpooled.wrappedBuffer(payload));
            restored.release();
            Aervium.LOGGER.info("Aervium parity: packet compression round trip {}, {} bytes to {} ({})",
                identical ? "identical" : "DIFFERENT", payload.length, deflatedBytes, level(payload, deflatedBytes));
        } finally {
            encoder.finishAndReleaseAll();
            decoder.finishAndReleaseAll();
        }
    }

    private static String level(byte[] payload, int deflatedBytes) {
        if (deflatedBytes == deflatedSize(PacketCompression.newDeflater(), payload)) {
            return "level " + PacketCompression.LEVEL;
        }
        return deflatedBytes == deflatedSize(new Deflater(), payload) ? "vanilla level" : "unknown level";
    }

    private static int deflatedSize(Deflater deflater, byte[] payload) {
        deflater.setInput(payload);
        deflater.finish();
        byte[] buffer = new byte[BUFFER_BYTES];
        int size = 0;
        while (!deflater.finished()) {
            size += deflater.deflate(buffer);
        }
        deflater.end();
        return size;
    }

    private static byte[] payload() {
        Random random = new Random(SEED);
        byte[] payload = new byte[PAYLOAD_BYTES];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (random.nextInt(RANDOM_SHARE) == 0 ? random.nextInt(BYTE_VALUES) : index % PATTERN_LENGTH);
        }
        return payload;
    }
}
