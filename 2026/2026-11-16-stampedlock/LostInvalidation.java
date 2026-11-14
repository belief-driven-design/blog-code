import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forces the interleaving from the article instead of hoping to hit it by luck:
 *
 *   Thread 2 starts recomputing and reads the buffers
 *   Thread 1 records a new call and invalidates the cache
 *   Thread 2 finishes and marks the cache valid  <== the invalidation is lost
 *
 * The latches only control timing. Remove them and the same interleaving is still
 * reachable, just not on demand.
 */
public class LostInvalidation {

    private static final long     LATE_CALL     = 4711L;
    private static final Duration MAX_STALENESS = Duration.ofMillis(50);

    public static void main(String[] args) throws Exception {
        System.out.println("broken (StampedLock + volatile flag): " + runBroken());
        System.out.println("cached (compute-then-publish):        " + runFixed());
    }

    private static String runBroken() throws Exception {
        Interleaving timing = new Interleaving();

        BrokenCache cache = new BrokenCache() {
            @Override
            protected List<Long> readBuffer() {
                return timing.pauseAfterReading(super.readBuffer());
            }
        };

        cache.add(1L);
        cache.statistics();  // Prime the cache, unhooked
        cache.add(2L);

        return report(timing.run(cache::statistics, () -> cache.add(LATE_CALL), cache::statistics));
    }

    private static String runFixed() throws Exception {
        Interleaving timing = new Interleaving();

        FixedCache cache = new FixedCache(MAX_STALENESS) {
            @Override
            protected List<Long> readBuffer() {
                return timing.pauseAfterReading(super.readBuffer());
            }
        };

        cache.add(1L);
        cache.statistics();
        cache.add(2L);
        Thread.sleep(MAX_STALENESS);  // Let the primed snapshot age out, so the hooked read recomputes

        // Read right after the late call and 4711 is still missing: it's late, as allowed.
        // Read once the snapshot is older than MAX_STALENESS, and it's there: late, never lost.
        return report(timing.run(cache::statistics, () -> cache.add(LATE_CALL), () -> afterStaleness(cache::statistics)));
    }

    private static Set<Long> afterStaleness(java.util.function.Supplier<Set<Long>> read) {
        try {
            Thread.sleep(MAX_STALENESS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return read.get();
    }

    private static String report(Set<Long> stats) {
        boolean present = stats.contains(LATE_CALL);
        return (present ? "call " + LATE_CALL + " present" : "call " + LATE_CALL + " LOST")
               + ", statistics = " + stats.stream().sorted().toList();
    }

    /** Lets the recompute get as far as reading the buffers, then hands control back. */
    private static final class Interleaving {

        private final CountDownLatch buffersRead  = new CountDownLatch(1);
        private final CountDownLatch callRecorded = new CountDownLatch(1);
        private final AtomicBoolean  armed        = new AtomicBoolean(false);

        List<Long> pauseAfterReading(List<Long> values) {
            if (this.armed.compareAndSet(true, false)) {
                this.buffersRead.countDown();
                await(this.callRecorded);
            }
            return values;
        }

        Set<Long> run(Runnable recompute, Runnable lateCall, java.util.function.Supplier<Set<Long>> read)
                throws InterruptedException {

            this.armed.set(true);

            Thread reader = new Thread(recompute, "reader");
            reader.start();

            await(this.buffersRead);
            lateCall.run();               // Arrives after the buffers were read
            this.callRecorded.countDown();
            reader.join();

            return read.get();
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
