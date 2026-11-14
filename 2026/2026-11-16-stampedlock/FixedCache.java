import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The same cache as compute-then-publish.
 *
 * One plain lock around the recomputation, an immutable snapshot published with a
 * single volatile store, and a generation counter captured BEFORE the buffers are
 * read, which is what closes the lost-invalidation window.
 *
 * The staleness bound is what makes it a cache at all: without it, every recorded
 * call forces the next read to recompute. A call can be late by up to maxStaleness,
 * but it can't be lost.
 */
class FixedCache {

    private final    long        maxStaleness;
    private final    List<Long>  buffer        = new CopyOnWriteArrayList<>();
    private final    Object      recomputeLock = new Object();
    private volatile Set<Long>   stats         = Set.of();
    private final    AtomicLong  generation    = new AtomicLong();
    private volatile long        computedAt    = -1;
    private volatile long        computedNanos;

    FixedCache(Duration maxStaleness) {
        this.maxStaleness = maxStaleness.toNanos();

        // nanoTime() can be negative, so 0 isn't a safe "never computed"
        this.computedNanos = System.nanoTime() - this.maxStaleness;
    }

    void add(long value) {
        this.buffer.add(value);

        // Many threads record, so this has to be atomic
        this.generation.incrementAndGet();
    }

    Set<Long> statistics() {
        if (needsRecompute(this.generation.get())) {
            recompute();
        }

        // Already immutable, so no defensive copy needed
        return this.stats;
    }

    private boolean needsRecompute(long current) {
        return this.computedAt != current                                       // something changed...
               && System.nanoTime() - this.computedNanos >= this.maxStaleness;  // ...and we're allowed to care
    }

    private void recompute() {
        synchronized (this.recomputeLock) {

            // STEP 1: Read the generation BEFORE the buffers, not after.
            long seen = this.generation.get();
            if (needsRecompute(seen) == false) {
                // Somebody else already did the work, recently enough
                return;
            }

            // STEP 2: Build a fresh immutable snapshot.
            Set<Long> snapshot = Set.copyOf(readBuffer());

            // STEP 3: Publish it, and record which generation it represents.
            this.stats = snapshot;
            this.computedNanos = System.nanoTime();
            this.computedAt = seen;
        }
    }

    protected List<Long> readBuffer() {
        return List.copyOf(this.buffer);
    }
}
