#!/usr/bin/env sh

set -eu

rm -rf -- build
mkdir -p -- build/app build/plugin

# The plugin class goes into its own output directory, so the application
# classpath cannot see it.
# Only the throwaway class loaders can.
javac -d build/plugin plugin/shop/OrderService.java
javac -d build/app    ClassMetaMap.java ClassMetaWeak.java ClassMetaValue.java \
                      LeakDemo.java RedeployLoopMap.java RedeployLoopClassValue.java \
                      ComputeCount.java LookupCost.java

echo "########## one deploy/undeploy cycle, three caches ##########"
for variant in map weak classvalue; do
    echo "==================== $variant ===================="
    java -Xlog:class+unload=info -cp build/app LeakDemo "$variant" \
        | grep -v -E "^\[.*\] unloading class (java|jdk|sun)\."
    echo
done

echo "########## 20,000 redeploys against a 32 MB metaspace cap ##########"
echo "==================== map ===================="
java -XX:MaxMetaspaceSize=32m -cp build/app RedeployLoopMap 20000
echo
echo "==================== classvalue ===================="
java -XX:MaxMetaspaceSize=32m -cp build/app RedeployLoopClassValue 20000
echo

echo "########## computeValue is not called exactly once ##########"
java -cp build/app ComputeCount
echo

echo "########## lookup cost, rough numbers only ##########"
java -cp build/app LookupCost
