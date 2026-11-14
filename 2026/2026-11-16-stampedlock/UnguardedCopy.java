import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * No latches this time, just the race as it happens:
 *
 *   Thread 1 reads the flag, sees a valid cache, and starts Set.copyOf()
 *   Thread 2 records a call, invalidates, and recomputes: clear(), then refill
 *   Thread 1's copy now walks a set that's being emptied and refilled  <== torn read
 *
 * Detecting a torn read is easy, because the demo only ever adds calls. Every
 * snapshot a recompute publishes is at least as large as the one before it, so
 * a reader that gets a SMALLER result than its previous one has copied a set
 * that was halfway through being rebuilt.
 */
public class UnguardedCopy {

    private static final int  PREFILL  = 5_000;
    private static final long DURATION = 2_000;  // ms
    private static final int  READERS  = 4;

    public static void main(String[] args) throws Exception {
        BrokenCache broken = new BrokenCache();
        // NO STALENESS: recompute on every change, the fixed version's worst case.
        //              A staleness bound would only make torn reads even less likely.
        FixedCache fixed = new FixedCache(Duration.ZERO);
        for (long i = 0; i < PREFILL; i++) {
            broken.add(i);
            fixed.add(i);
        }

        System.out.println("broken (StampedLock + volatile flag): " + hammer(broken::add, broken::statistics));
        System.out.println("cached (compute-then-publish):        " + hammer(fixed::add, fixed::statistics));
    }

    private interface Recorder {
        void add(long value);
    }

    private static String hammer(Recorder recorder, Supplier<Set<Long>> statistics) throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        LongAdder reads = new LongAdder();
        LongAdder torn = new LongAdder();
        int[] worstDrop = { 0 };

        // ONE RECORDER: keeps invalidating the cache, so recomputes keep happening
        Thread writer = new Thread(() -> {
            long next = PREFILL;
            while (running.get()) {
                recorder.add(next++);
                Thread.onSpinWait();
            }
        }, "recorder");

        // SEVERAL READERS: every one of them may trigger the recompute, or copy during one
        Thread[] readers = new Thread[READERS];
        for (int i = 0; i < READERS; i++) {
            readers[i] = new Thread(() -> {
                int previous = 0;
                while (running.get()) {
                    int size = statistics.get().size();
                    reads.increment();
                    if (size < previous) {
                        torn.increment();  // Shrunk, but nothing is ever removed
                        synchronized (worstDrop) {
                            worstDrop[0] = Math.max(worstDrop[0], previous - size);
                        }
                    }
                    previous = size;
                }
            }, "reader-" + i);
        }

        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }

        Thread.sleep(DURATION);
        running.set(false);

        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }

        String result = torn.sum() + " torn reads out of " + reads.sum();
        if (torn.sum() > 0) {
            result += ", worst: " + worstDrop[0] + " calls missing";
        }
        return result;
    }
}
