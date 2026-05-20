package kr.reborn.core.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

public final class Rand {
    private Rand() {}

    public static int range(int minIncl, int maxIncl) {
        if (maxIncl <= minIncl) return minIncl;
        return ThreadLocalRandom.current().nextInt(minIncl, maxIncl + 1);
    }

    public static double rangeD(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static boolean chance(double p) {
        return ThreadLocalRandom.current().nextDouble() < p;
    }

    public static <T> T weighted(List<T> items, ToDoubleFunction<T> weightFn) {
        double total = 0;
        for (T t : items) total += Math.max(0, weightFn.applyAsDouble(t));
        if (total <= 0) return items.get(range(0, items.size() - 1));
        double r = ThreadLocalRandom.current().nextDouble() * total;
        double acc = 0;
        for (T t : items) {
            acc += Math.max(0, weightFn.applyAsDouble(t));
            if (r < acc) return t;
        }
        return items.get(items.size() - 1);
    }
}
