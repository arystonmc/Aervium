package com.aryston.kinetic.debug.benchmark;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record BenchmarkComparison(String previousStartedAt, List<SceneDelta> scenes) {
    private static final double PERCENT = 100.0;

    public static BenchmarkComparison between(BenchmarkReport previous, BenchmarkReport current) {
        Map<String, SceneResult> earlier = previous.scenes().stream()
            .collect(Collectors.toMap(SceneResult::scene, Function.identity(), (first, second) -> first));
        List<SceneDelta> deltas = current.scenes().stream()
            .filter(scene -> earlier.containsKey(scene.scene()))
            .map(scene -> SceneDelta.of(earlier.get(scene.scene()), scene))
            .toList();
        return new BenchmarkComparison(previous.startedAt(), deltas);
    }

    public double largestAverageFpsChangePercent() {
        return scenes.stream().mapToDouble(delta -> Math.abs(delta.averageFpsPercent())).max().orElse(0.0);
    }

    static double changePercent(double before, double after) {
        return before == 0.0 ? 0.0 : (after - before) / before * PERCENT;
    }

    public record SceneDelta(String scene, double averageFpsPercent, double onePercentLowPercent, double pointOnePercentLowPercent, double averageMsptPercent) {
        static SceneDelta of(SceneResult before, SceneResult after) {
            return new SceneDelta(
                after.scene(),
                changePercent(before.frames().averageFps(), after.frames().averageFps()),
                changePercent(before.frames().onePercentLowFps(), after.frames().onePercentLowFps()),
                changePercent(before.frames().pointOnePercentLowFps(), after.frames().pointOnePercentLowFps()),
                changePercent(before.ticks().averageMs(), after.ticks().averageMs())
            );
        }
    }
}
