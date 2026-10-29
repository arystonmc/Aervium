package com.aryston.kinetic.core.parity;

import java.nio.charset.StandardCharsets;

public final class StateHash {
    private static final long OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;

    private long value = OFFSET_BASIS;

    public StateHash add(int number) {
        for (int shift = 0; shift < Integer.SIZE; shift += BITS_PER_BYTE) {
            addByte(number >>> shift);
        }
        return this;
    }

    public StateHash add(long number) {
        for (int shift = 0; shift < Long.SIZE; shift += BITS_PER_BYTE) {
            addByte((int) (number >>> shift));
        }
        return this;
    }

    public StateHash add(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        add(bytes.length);
        for (byte b : bytes) {
            addByte(b);
        }
        return this;
    }

    public long value() {
        return value;
    }

    public String hex() {
        return String.format("%016x", value);
    }

    private void addByte(int b) {
        value ^= b & BYTE_MASK;
        value *= PRIME;
    }
}
