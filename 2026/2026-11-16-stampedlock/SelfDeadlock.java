import java.util.concurrent.locks.StampedLock;

/**
 * StampedLock is not reentrant. Taking the write lock while already holding it
 * on the same thread doesn't throw and doesn't succeed: it parks forever.
 *
 * Run it, then look at the thread from another shell:
 *
 *   jcmd $(pgrep -f SelfDeadlock) Thread.print -l
 *
 * Note what is NOT in that dump: no "Found one Java-level deadlock" section,
 * and "Locked ownable synchronizers" reads "- None", even though this thread holds
 * the lock. StampedLock isn't built on
 * AbstractQueuedSynchronizer and never publishes an owner thread, so there is no
 * cycle for the JVM's deadlock detector to find.
 */
public class SelfDeadlock {

    private static final StampedLock LOCK = new StampedLock();

    public static void main(String[] args) throws Exception {
        Thread worker = new Thread(SelfDeadlock::recompute, "stats-recompute");
        worker.start();

        System.out.println("waiting for a thread that will never finish, ctrl-c to stop");
        System.out.println("  jcmd " + ProcessHandle.current().pid() + " Thread.print -l");

        worker.join();
    }

    private static void recompute() {
        long stamp = LOCK.writeLock();
        try {
            clear();  // <== the innocent-looking call that parks the thread
        }
        finally {
            LOCK.unlockWrite(stamp);
        }
    }

    private static void clear() {
        long stamp = LOCK.writeLock();
        try {
            System.out.println("never printed");
        }
        finally {
            LOCK.unlockWrite(stamp);
        }
    }
}
