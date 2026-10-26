# Widening Beats Boxing Beats Varargs

Runnable with a JDK 11 or later and nothing else.

## Files

* [`Phase.java`](./Phase.java): the three-phase ladder.
* [`Remove.java`](./Remove.java): the `List.remove` gotcha.
* [`v1/Lib.java`](./v1/Lib.java), [`v2/Lib.java`](./v2/Lib.java), [`App.java`](./App.java): a one-method library, its call site, and the
  overload added in v2
* [`run.sh`](./run.sh): compiles and runs everything

## Expected output

```text
==================== the three phases ====================
f(1) -> f(long)
g(1) -> g(Integer)
h(1) -> h(Object)

==================== the List.remove gotcha ====================
remove(1)                  -> [1, 3]
remove(Integer.valueOf(1)) -> [3]
remove((Integer) 1)        -> [2, 3]   fresh list

==================== 1. app compiled against v1, run against v1 ====================
Integer: 42
       8: invokestatic  #4                  // Method Lib.handle:(Ljava/lang/Integer;)Ljava/lang/String;

==================== 2. same App.class, run against v2 ====================
Integer: 42

==================== 3. same App.java, recompiled against v2 ====================
long: 42
       6: invokestatic  #5                  // Method Lib.handle:(J)Ljava/lang/String;
```

## What steps 1 to 3 show

Step 2 runs the `App.class` from step 1, unchanged, against v2.
It prints the same thing.
The call site recorded `Lib.handle:(Ljava/lang/Integer;)Ljava/lang/String;` in the constant pool, and that descriptor still resolves, so adding `handle(long)` broke nothing.

Step 3 recompiles the same `App.java` against v2.
Phase 1 now finds `handle(long)` applicable through widening, the descriptor in the constant pool becomes `Lib.handle:(J)`, and the program prints something else.
No line of source changed between steps 2 and 3.

The constant pool indices and column widths in the `javap` output are whatever your compiler produced, and differ between JDK versions.
The descriptors are the part that matters.
