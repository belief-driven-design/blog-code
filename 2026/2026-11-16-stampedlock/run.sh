#!/usr/bin/env sh
set -eu

rm -rf build
mkdir -p build
javac -d build BrokenCache.java FixedCache.java LostInvalidation.java UnguardedCopy.java StampProbe.java SelfDeadlock.java

echo "==================== stamp obligations ===================="
java -cp build StampProbe

echo
echo "==================== lost invalidation ===================="
java -cp build LostInvalidation

echo
echo "==================== unguarded copy ===================="
java -cp build UnguardedCopy

echo
echo "==================== self-deadlock ===================="
echo "Not run automatically, it never terminates. Start it yourself:"
echo
echo "  java -cp build SelfDeadlock"
echo "  jcmd \$(pgrep -f SelfDeadlock) Thread.print -l"
