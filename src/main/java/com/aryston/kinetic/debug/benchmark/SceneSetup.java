package com.aryston.kinetic.debug.benchmark;

import java.util.List;

public interface SceneSetup {
    SceneSetup NONE = new SiteFree();

    int size();

    int height();

    List<String> build(BlockPoint site);

    List<String> everyTick(BlockPoint site, int tick);

    List<String> clear(BlockPoint site);

    default boolean usesSite() {
        return size() > 0;
    }

    default BlockPoint center(BlockPoint site) {
        return site.offset(size() / 2, 0, size() / 2);
    }

    default BlockPoint far(BlockPoint site) {
        return site.offset(size() - 1, height(), size() - 1);
    }

    final class SiteFree implements SceneSetup {
        private SiteFree() {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int height() {
            return 0;
        }

        @Override
        public List<String> build(BlockPoint site) {
            return List.of();
        }

        @Override
        public List<String> everyTick(BlockPoint site, int tick) {
            return List.of();
        }

        @Override
        public List<String> clear(BlockPoint site) {
            return List.of();
        }
    }
}
