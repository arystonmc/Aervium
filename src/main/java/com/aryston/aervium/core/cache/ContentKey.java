package com.aryston.aervium.core.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ContentKey {
    private static final String ALGORITHM = "SHA-256";
    private static final byte SEPARATOR = 0;

    private final MessageDigest digest;

    private ContentKey() {
        try {
            this.digest = MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(ALGORITHM + " is not available", missing);
        }
    }

    public static ContentKey create() {
        return new ContentKey();
    }

    public ContentKey add(String part) {
        digest.update(part.getBytes(StandardCharsets.UTF_8));
        digest.update(SEPARATOR);
        return this;
    }

    public ContentKey add(boolean part) {
        return add(Boolean.toString(part));
    }

    public String hex() {
        return HexFormat.of().formatHex(digest.digest());
    }
}
