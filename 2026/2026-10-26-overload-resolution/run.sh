#!/usr/bin/env sh
set -eu

rm -rf build
mkdir -p build

javac -d build Phase.java Remove.java

echo "==================== the three phases ===================="
java -cp build Phase

echo
echo "==================== the List.remove gotcha ===================="
java -cp build Remove

echo
echo "==================== 1. app compiled against v1, run against v1 ===================="
javac -d build/v1 v1/Lib.java
javac -cp build/v1 -d build/app-v1 App.java
java -cp build/v1:build/app-v1 App
javap -c -cp build/app-v1 App | grep 'invokestatic.*handle'

echo
echo "==================== 2. same App.class, run against v2 ===================="
javac -d build/v2 v2/Lib.java
java -cp build/v2:build/app-v1 App

echo
echo "==================== 3. same App.java, recompiled against v2 ===================="
javac -cp build/v2 -d build/app-v2 App.java
java -cp build/v2:build/app-v2 App
javap -c -cp build/app-v2 App | grep 'invokestatic.*handle'
