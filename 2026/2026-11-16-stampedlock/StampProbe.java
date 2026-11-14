import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;

/**
 * What the stamp obligation and the missing features actually look like,
 * rather than what the Javadoc says about them.
 */
public class StampProbe {

    public static void main(String[] args) {
        StampedLock lock = new StampedLock();

        // PROBE 1: unlocking with a stamp that isn't the current one
        long stamp = lock.writeLock();
        try {
            lock.unlockWrite(stamp + 1);
        }
        catch (Throwable t) {
            System.out.println("bogus stamp                   -> " + t);
        }
        lock.unlockWrite(stamp);

        // PROBE 2: reusing a stamp after the lock was released
        try {
            lock.unlockWrite(stamp);
        }
        catch (Throwable t) {
            System.out.println("stale stamp                   -> " + t);
        }

        // PROBE 3: read stamps don't identify the reader, so unlocking one twice
        //          silently releases somebody else's hold
        long reader1 = lock.readLock();
        long reader2 = lock.readLock();
        lock.unlockRead(reader1);
        lock.unlockRead(reader1);  // <== a bug, but nothing throws here
        System.out.println("double unlockRead             -> no exception, read locks held: "
                           + lock.getReadLockCount());
        try {
            lock.unlockRead(reader2);  // <== the innocent line is the one that throws
        }
        catch (Throwable t) {
            System.out.println("... then the other reader     -> " + t);
        }

        // PROBE 4: an optimistic read while a writer holds the lock returns 0,
        //          and 0 never validates. That's the "try" in tryOptimisticRead.
        long write = lock.writeLock();
        System.out.println("optimistic while write-locked -> " + lock.tryOptimisticRead());
        System.out.println("validate(0)                   -> " + lock.validate(0));

        // PROBE 5: what the lock will and won't tell us
        System.out.println("isWriteLocked                 -> " + lock.isWriteLocked());
        System.out.println("... but held by whom?         -> no such query exists");
        System.out.println("newCondition available        -> "
                           + Arrays.stream(StampedLock.class.getMethods())
                                   .anyMatch(m -> m.getName().equals("newCondition")));
        lock.unlockWrite(write);
    }
}
