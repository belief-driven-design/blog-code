import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The obvious version: a static Map keyed by Class.
 *
 * Correct, fast, thread-safe, and it keeps every key it has ever seen alive.
 */
record ClassMetaMap(String packageName, String simpleName) {

    private static final Map<Class<?>, ClassMetaMap> CACHE = new ConcurrentHashMap<>();

    static ClassMetaMap of(Class<?> c) {
        return CACHE.computeIfAbsent(c, k -> new ClassMetaMap(k.getPackageName(),
                                                              k.getSimpleName()));
    }
}
