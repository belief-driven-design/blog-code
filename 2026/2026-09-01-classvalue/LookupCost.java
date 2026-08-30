import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A rough sanity check on "is ClassValue faster?", NOT a benchmark.
 *
 * No JMH, one JVM, one thread, so treat the numbers as an order of magnitude and
 * nothing more. The point is only that both lookups are cheap enough that lifetime,
 * not throughput, should decide which one you pick.
 */
public class LookupCost {

    private static final Map<Class<?>, String> MAP = new ConcurrentHashMap<>();

    private static final ClassValue<String> VALUE = new ClassValue<>() {

        @Override
        protected String computeValue(Class<?> type) {
            return type.getSimpleName();
        }
    };

    private static final Class<?>[] KEYS = {
        String.class,
        Integer.class,
        Long.class,
        Double.class,
        Object.class,
        Thread.class,
        Math.class,
        System.class,
        Boolean.class,
        Byte.class
    };

    public static void main(String[] args) {

        int rounds = 5;
        int iterations = 20_000_000;

        // POPULATE both caches first, so every measured lookup is a hit.
        for (Class<?> key : KEYS) {
            MAP.put(key, key.getSimpleName());
            VALUE.get(key);
        }

        // WARMUP: let the JIT settle.
        for (int i = 0; i < 3; i++) {
            mapGet(1_000_000);
            map(1_000_000);
            classValue(1_000_000);
        }

        System.out.printf(Locale.ROOT,
                          "  %-6s %14s %14s %14s%n",
                          "round",
                          "map.get",
                          "computeIfAbsent",
                          "ClassValue.get");

        for (int round = 1; round <= rounds; round++) {
            long getNanos = mapGet(iterations);
            long computeNanos = map(iterations);
            long valueNanos = classValue(iterations);

            System.out.printf(Locale.ROOT, "  %-6d %11.2f ns %11.2f ns %11.2f ns%n",
                              round,
                              getNanos / (double) iterations,
                              computeNanos / (double) iterations,
                              valueNanos / (double) iterations);
        }
    }

    private static long mapGet(int iterations) {
        long start = System.nanoTime();
        int sink = 0;
        for (int i = 0; i < iterations; i++) {
            Class<?> key = KEYS[i & 7];
            sink += MAP.get(key).length();
        }

        long elapsed = System.nanoTime() - start;
        consume(sink);
        return elapsed;
    }

    private static long map(int iterations) {
        long start = System.nanoTime();
        int sink = 0;
        for (int i = 0; i < iterations; i++) {
            Class<?> key = KEYS[i & 7];
            sink += MAP.computeIfAbsent(key, Class::getSimpleName).length();
        }

        long elapsed = System.nanoTime() - start;
        consume(sink);
        return elapsed;
    }

    private static long classValue(int iterations) {
        long start = System.nanoTime();
        int sink = 0;
        for (int i = 0; i < iterations; i++) {
            Class<?> key = KEYS[i & 7];
            sink += VALUE.get(key).length();
        }

        long elapsed = System.nanoTime() - start;
        consume(sink);
        return elapsed;
    }

    private static int blackhole;

    private static void consume(int value) {
        blackhole ^= value;
    }
}
