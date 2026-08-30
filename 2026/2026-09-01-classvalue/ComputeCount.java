import java.util.concurrent.atomic.AtomicInteger;

/**
 * computeValue is not a "runs exactly once" guarantee.
 *
 * The easy deterministic case is remove(): the next get() recomputes.
 * Under a race, several threads can also compute concurrently while only
 * one result gets published.
 * Either way, the computation has to be idempotent and free of side effects.
 */
public class ComputeCount {

    public static void main(String[] args) {

        AtomicInteger computations = new AtomicInteger();

        ClassValue<String> value = new ClassValue<>() {
            @Override
            protected String computeValue(Class<?> type) {
                computations.incrementAndGet();
                return type.getSimpleName();
            }
        };

        value.get(String.class);
        value.get(String.class);
        value.get(String.class);
        System.out.println("3 gets                 -> " + computations.get() + " computation(s)");

        value.remove(String.class);
        value.get(String.class);
        System.out.println("after remove() + get   -> " + computations.get() + " computation(s)");

        System.out.println();
        System.out.println("Which is why a computeValue that registers, counts, or opens");
        System.out.println("something is a bug waiting for a cache eviction to expose it.");
    }
}
