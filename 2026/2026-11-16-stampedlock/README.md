# A StampedLock That Never Used Its Stamp

Four things from the post, all runnable with JDK 25 or later and nothing else.

## Files

* `StampProbe.java`\
  What the stamp obligation and the missing features actually do:
  wrong stamp, stale stamp, a read stamp unlocked twice, an optimistic read while write-locked, and the ownership queries that don't exist.

* `SelfDeadlock.java`\
  Two write-lock acquisitions on one thread. Never terminates, which is the point.

* `BrokenCache.java` / `FixedCache.java`\
  The original shape (`StampedLock` plus a concurrent set plus a `volatile` flag) and the compute-then-publish replacement.
  `FixedCache` takes a staleness bound: a recorded call can be late by up to that long, but never lost.
  `UnguardedCopy` runs it with `Duration.ZERO`, its worst case.

* `LostInvalidation.java`\
  Forces the interleaving that loses a recorded call, on both.

* `UnguardedCopy.java`\
  No latches, just one recorder and four readers for two seconds.
  Counts the reads that came back smaller than the previous one, which can only happen
  when a reader copied the set while a recompute was emptying and refilling it.

* `run.sh`\
  Compiles everything and runs the two demos that terminate.

## Running it

```shell
./run.sh
```

## Expected output

```text
==================== stamp obligations ====================
bogus stamp                   -> java.lang.IllegalMonitorStateException
stale stamp                   -> java.lang.IllegalMonitorStateException
double unlockRead             -> no exception, read locks held: 0
... then the other reader     -> java.lang.IllegalMonitorStateException
optimistic while write-locked -> 0
validate(0)                   -> false
isWriteLocked                 -> true
... but held by whom?         -> no such query exists
newCondition available        -> false

==================== lost invalidation ====================
broken (StampedLock + volatile flag): call 4711 LOST, statistics = [1, 2]
cached (compute-then-publish):        call 4711 present, statistics = [1, 2, 4711]

==================== unguarded copy ====================
broken (StampedLock + volatile flag): 427 torn reads out of 1328, worst: 88894 calls missing
cached (compute-then-publish):        0 torn reads out of 2401
```

The unguarded-copy numbers differ on every run, since that race isn't forced.
The broken version has never come out at zero in my runs, and the fixed one has never come out above it.

The interleaving is forced with two `CountDownLatch` instances rather than hoping to hit it by luck.
The latches change the timing, not the outcome: the same interleaving is reachable without them, just not on demand.

## The self-deadlock

This one has to be run by hand, because it never finishes:

```shell
java -cp build SelfDeadlock
jcmd $(pgrep -f SelfDeadlock) Thread.print -l
```

The thread appears as `WAITING (parking)`, parked in `StampedLock.acquireWrite`.

What's worth noting is everything the dump does _not_ contain.

There is no `Found one Java-level deadlock` section.
And even with `-l`, which asks for `java.util.concurrent` locks explicitly, the thread's `Locked ownable synchronizers` reads `- None`, while it's holding the very lock it's waiting for.

`StampedLock` is not built on `AbstractQueuedSynchronizer` and never publishes an owner thread, so there is no ownership cycle for the JVM's deadlock detector to find.

For contrast, two threads deadlocking on a pair of `ReentrantLock` instances _are_ reported, naming both threads and both locks.
The difference isn't that one is a `java.util.concurrent` lock and the other isn't.
It's ownership tracking.

Verified with Temurin 25.0.3.
