package com.aryston.kinetic.world.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;

class PacketCompressionTest {
    private static final int PAYLOAD_BYTES = 1 << 16;
    private static final int BUFFER_BYTES = 4096;

    @Test
    void compressedPacketsInflateToTheSameBytes() throws DataFormatException {
        byte[] payload = payload();
        assertArrayEquals(payload, inflate(deflate(PacketCompression.newDeflater(), payload)));
    }

    @Test
    void theFasterLevelStaysCloseToTheVanillaSize() {
        byte[] payload = payload();
        int vanilla = deflate(new Deflater(), payload).length;
        int faster = deflate(PacketCompression.newDeflater(), payload).length;
        assertTrue(faster < vanilla * 1.1, faster + " bytes against " + vanilla);
    }

    private static byte[] payload() {
        Random random = new Random(7L);
        byte[] payload = new byte[PAYLOAD_BYTES];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (random.nextInt(4) == 0 ? random.nextInt(256) : index % 7);
        }
        return payload;
    }

    private static byte[] deflate(Deflater deflater, byte[] input) {
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        while (!deflater.finished()) {
            output.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return output.toByteArray();
    }

    private static byte[] inflate(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        while (!inflater.finished()) {
            output.write(buffer, 0, inflater.inflate(buffer));
        }
        inflater.end();
        return output.toByteArray();
    }
}
