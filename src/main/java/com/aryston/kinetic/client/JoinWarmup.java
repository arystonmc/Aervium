package com.aryston.kinetic.client;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.telemetry.Durations;
import io.netty.channel.DefaultChannelId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.client.Minecraft;

final class JoinWarmup {
    private static final String CLASS_LIST = "/assets/kinetic/warmup/join_classes.txt";
    private static final String THREAD_NAME = "Kinetic Join Warmup";

    private JoinWarmup() {
    }

    static void start() {
        Thread.ofPlatform().name(THREAD_NAME).daemon().priority(Thread.MIN_PRIORITY).start(JoinWarmup::run);
    }

    private static void run() {
        long startedAt = System.nanoTime();
        DefaultChannelId.newInstance();
        int loaded = 0;
        int missing = 0;
        ClassLoader loader = Minecraft.class.getClassLoader();
        try (InputStream stream = Objects.requireNonNull(JoinWarmup.class.getResourceAsStream(CLASS_LIST), CLASS_LIST);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String name;
            while ((name = reader.readLine()) != null) {
                if (load(name.strip(), loader)) {
                    loaded++;
                } else {
                    missing++;
                }
            }
        } catch (IOException failure) {
            Kinetic.LOGGER.warn("Kinetic could not read its join warm-up list", failure);
        }
        Kinetic.LOGGER.info("Kinetic warmed up {} classes for joining worlds in {} ms ({} not present)", loaded,
            Math.round(Durations.millis(System.nanoTime() - startedAt)), missing);
    }

    private static boolean load(String name, ClassLoader loader) {
        if (name.isEmpty()) {
            return false;
        }
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError | RuntimeException failure) {
            return false;
        }
    }
}
