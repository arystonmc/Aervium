package com.aryston.kinetic.core.cache;

public final class SpirvBinary {
    private static final int MAGIC = 0x07230203;
    private static final int WORD_BYTES = 4;
    private static final int HEADER_WORDS = 5;
    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;

    private SpirvBinary() {
    }

    public static boolean isValid(byte[] binary) {
        if (binary.length < HEADER_WORDS * WORD_BYTES || binary.length % WORD_BYTES != 0) {
            return false;
        }
        return littleEndianWord(binary) == MAGIC;
    }

    private static int littleEndianWord(byte[] binary) {
        int word = 0;
        for (int i = WORD_BYTES - 1; i >= 0; i--) {
            word = (word << BITS_PER_BYTE) | (binary[i] & BYTE_MASK);
        }
        return word;
    }
}
