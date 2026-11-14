import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.StampedLock;

/**
 * The shape of the original code: a StampedLock, a concurrent collection, and a
 * volatile flag, guarding one cache between them.
 *
 * The write path takes no lock, the read path takes no lock, and the only locked
 * path is the recomputation, which was never the part at risk.
 */
class BrokenCache {

    private final List<Long>         buffer          = new CopyOnWriteArrayList<>();
    private final StampedLock        lock            = new StampedLock();
    private final Set<Long>          statsCache      = ConcurrentHashMap.newKeySet();
    private volatile boolean         statsCacheValid = false;

    /**
     * The recording path. Never touches the lock.
     */
    void add(long value) {
        this.buffer.add(value);
        this.statsCacheValid = false;
    }

    /**
     * The read path. Never touches the lock either.
     */
    Set<Long> statistics() {
        if (this.statsCacheValid == false) {
            recompute();
        }
        return Set.copyOf(this.statsCache);
    }

    private void recompute() {
        long stamp = this.lock.writeLock();
        try {
            if (this.statsCacheValid) {
                // Double-check, so only one racing reader does the work
                return;
            }

            this.statsCache.clear();
            this.statsCache.addAll(readBuffer());

            // The flag is set AFTER the buffers were read, so any add() that
            // happened in between is now marked as already accounted for.
            this.statsCacheValid = true;
        }
        finally {
            this.lock.unlockWrite(stamp);
        }
    }

    /**
     * Overridable purely so the demo can force a specific interleaving.
     */
    protected List<Long> readBuffer() {
        return List.copyOf(this.buffer);
    }
}
