# ClassValue: The Cache That Dies With the Class

Everything the post claims, as code you can run.
[`./run.sh`](run.sh) does all of it in about three seconds and needs nothing but a JDK 21 or later on the `PATH`.

## The caches

* [`ClassMetaMap.java`](ClassMetaMap.java)\
  The obvious one: a `static ConcurrentHashMap` keyed by `Class`.

* [`ClassMetaWeak.java`](ClassMetaWeak.java)\
  The first reflex, a `WeakHashMap`, with a value that holds a back-reference to its own class the way a real cached value usually does.

* [`ClassMetaValue.java`](ClassMetaValue.java)\
  The same cache as a `ClassValue`, with the ownership inverted.

## The demos

* [`LeakDemo.java`](LeakDemo.java): one deploy/undeploy cycle against each of the three caches.
   A `WeakReference<Class<?>>` reports whether the class survived, and `-Xlog:class+unload=info` says the same thing from the JVM's side.

* [`RedeployLoopMap.java`](RedeployLoopMap.java) and [`RedeployLoopClassValue.java`](RedeployLoopClassValue.java): 20,000 redeploys against `-XX:MaxMetaspaceSize=32m`.
  The only line that behaves differently is the one that fills the cache.
  The first turns the leak from an argument into an `OutOfMemoryError`, the second survives all 20,000.

* [`ComputeCount.java`](ComputeCount.java): `computeValue` is not a "runs exactly once" guarantee.

* [`LookupCost.java`](LookupCost.java): rough lookup costs. Not JMH, and it says so.

* [`plugin/shop/OrderService.java`](plugin/shop/OrderService.java): stands in for an application class.
  Compiled into its own output directory, so only the throwaway loaders can see it.

## What the interesting runs show

One cycle, three caches. The weak key doesn't save the `WeakHashMap`, because the entry's
own value keeps its key alive:

```text
==================== weak ====================
cache implementation:  WeakHashMap, value references its key
weak cache entries:    1  (a cleared weak key would have removed it)
after undeploy + GC:   STILL REACHABLE -> class shop.OrderService via WebAppClassLoader #1

==================== classvalue ====================
[0.043s][info][class,unload] unloading class shop.OrderService 0x0000007001001000
after undeploy + GC:   unloaded
```

Then 20,000 redeploys with a 32 MB cap:

```text
==================== map ====================
  deploy          used     committed   class space
    4000        5.3 MB       24.4 MB        2.1 MB
    5308        6.7 MB       32.0 MB        2.7 MB  <== OutOfMemoryError: Metaspace

==================== classvalue ====================
    4000        5.3 MB       24.4 MB        2.1 MB
    6000        1.6 MB        5.0 MB        0.4 MB     <== classes unloading
   16000        1.0 MB        1.6 MB        0.1 MB
survived 20000 redeploys, usage rose and fell throughout
```

Two things about those numbers are worth knowing before quoting them.

**`used` is not what the cap limits, `committed` is.**

Metaspace is handed out to class loaders in chunks, so 5,000 loaders each holding one mostly-empty chunk commit roughly 4.8x what they use.
That's why the `OutOfMemoryError` arrives while `used` still reads a comfortable 6.7 MB.

**HotSpot's `Metaspace` memory pool already includes the compressed class space**, so the two must never be added together.
The class-space column is shown separately for that reason.

## Lookup cost

```text
  round         map.get computeIfAbsent ClassValue.get
  1             1.64 ns        6.16 ns        1.49 ns
```

[`LookupCost`](LookupCost.java) is a plain loop with warmup, one thread, one JVM. It is *not* a benchmark, and JMH is the right tool if a decision depends on the answer.
It's here only to support a negative claim: nothing in these numbers justifies switching cache implementations for throughput.
Note that the reflexive `computeIfAbsent` on every lookup costs more than either straight lookup, which is a separate and much less interesting bug.

## Poking at the internals

The ownership direction the post describes is visible without a JDK source checkout:

```shell
javap -p java.lang.Class | grep classValueMap
javap -p 'java.lang.ClassValue$ClassValueMap' | head -3
```

The first shows the package-private `classValueMap` field that every `Class` carries, and the second shows that it's a `WeakHashMap` keyed by the `ClassValue`'s identity.

## A caveat about `System.gc()`

[`LeakDemo`](LeakDemo.java) calls `System.gc()` because it needs a full GC to observe class unloading in a program that runs for 200 milliseconds.
That's fine for a demo and wrong in production code.

All numbers verified with Temurin 25.0.3 on M5 Pro (arm64) and the default collector (G1).
