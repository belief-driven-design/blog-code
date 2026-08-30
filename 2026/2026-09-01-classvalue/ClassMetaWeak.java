import java.util.Map;
import java.util.WeakHashMap;

/**
 * The first reflex after hearing about the leak: make the key weak.
 *
 * This one holds a back-reference to its own class, the way a cached serializer,
 * MethodHandle, or resolved annotation model naturally would. That single field is
 * enough to defeat the weak key, because the entry's value keeps its own key alive.
 */
record ClassMetaWeak(Class<?> owner, String packageName, String simpleName) {

    private static final Map<Class<?>, ClassMetaWeak> CACHE = new WeakHashMap<>();

    static synchronized ClassMetaWeak of(Class<?> c) {
        return CACHE.computeIfAbsent(c, k -> new ClassMetaWeak(k,    // <== the fatal field
                                                               k.getPackageName(),
                                                               k.getSimpleName()));
    }

    static synchronized int cacheSize() {
        return CACHE.size();
    }

    @Override
    public String toString() {
        return "ClassMetaWeak[packageName=" + this.packageName
               + ", simpleName=" + this.simpleName + "]";
    }
}
