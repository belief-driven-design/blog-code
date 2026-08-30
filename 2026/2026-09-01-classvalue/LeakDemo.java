import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Simulates one deploy/undeploy cycle and reports whether the deployed class
 * could actually be unloaded afterwards.
 *
 * Usage: java LeakDemo [map|weak|classvalue]
 */
public class LeakDemo {

    public static void main(String[] args) throws Exception {

        String cache = args.length > 0 ? args[0] : "map";

        // STEP 1: Deploy. A throwaway loader, exactly like a container creates per deployment.
        URL[] classpath = { new File("build/plugin").toURI().toURL() };
        URLClassLoader loader = new URLClassLoader("WebAppClassLoader #1",
                                                   classpath,
                                                   LeakDemo.class.getClassLoader());
        Class<?> plugin = loader.loadClass("shop.OrderService");

        // STEP 2: Cache something derived from it. The ordinary library move.
        Object meta = switch (cache) {
            case "classvalue" -> ClassMetaValue.of(plugin);
            case "weak"       -> ClassMetaWeak.of(plugin);
            default           -> ClassMetaMap.of(plugin);
        };

        System.out.println("cache implementation:  " + switch (cache) {
            case "classvalue" -> "ClassValue";
            case "weak"       -> "WeakHashMap, value references its key";
            default           -> "static Map";
        });
        System.out.println("cached value:          " + meta);

        // STEP 3: Undeploy. Watch the class, then drop every reference we hold ourselves.
        WeakReference<Class<?>> watch = new WeakReference<>(plugin);
        plugin = null;
        loader.close();
        loader = null;

        // STEP 4: Give the collector every chance to unload it.
        //         Class unloading happens during a full GC, so ask for a few.
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(100);
        }

        if ("weak".equals(cache)) {
            System.out.println("weak cache entries:    " + ClassMetaWeak.cacheSize()
                               + "  (a cleared weak key would have removed it)");
        }

        Class<?> survivor = watch.get();
        System.out.println("after undeploy + GC:   "
                           + (survivor == null ? "unloaded"
                                               : "STILL REACHABLE -> " + survivor
                                                 + " via " + survivor.getClassLoader().getName()));
    }
}
