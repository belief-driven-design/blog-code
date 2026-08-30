import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Redeploys the same class a few thousand times through a ClassValue cache and
 * watches Metaspace rise and fall forever.
 *
 * Every iteration defines shop.OrderService in a brand-new ClassLoader, which makes
 * it a genuinely distinct class with its own metadata, exactly like a container
 * redeploying an application. Nothing outside the loader ever references them.
 *
 * RedeployLoopMap is the same program with one line changed.
 *
 * Usage: java -XX:MaxMetaspaceSize=32m RedeployLoopClassValue [iterations]
 */
public class RedeployLoopClassValue {

    public static void main(String[] args) throws Exception {

        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;

        byte[] bytecode = Files.readAllBytes(Path.of("build/plugin/shop/OrderService.class"));

        // HotSpot's "Metaspace" pool already includes the compressed class space, so
        // these two must not be added together. What the cap actually limits is
        // COMMITTED, not used, which is why the OutOfMemoryError fires while "used"
        // still looks modest.
        MemoryPoolMXBean metaspace = pool("Metaspace");
        MemoryPoolMXBean classSpace = pool("Compressed Class Space");

        System.out.printf("cache: ClassValue<ClassMeta>%n");
        System.out.printf("  %6s  %12s  %12s  %12s%n",
                          "deploy",
                          "used",
                          "committed",
                          "class space");

        long start = metaspace.getUsage().getUsed();

        for (int i = 1; i <= iterations; i++) {
            try {
                deployOnce(bytecode);
            }
            catch (OutOfMemoryError oome) {
                System.out.printf("  %6d  %12s  %12s  %12s  <== OutOfMemoryError: %s%n",
                                  i,
                                  mb(metaspace.getUsage().getUsed()),
                                  mb(metaspace.getUsage().getCommitted()),
                                  mb(classSpace.getUsage().getUsed()),
                                  oome.getMessage());
                return;
            }

            if (i % (iterations / 10) == 0) {
                System.out.printf("  %6d  %12s  %12s  %12s%n",
                                  i,
                                  mb(metaspace.getUsage().getUsed()),
                                  mb(metaspace.getUsage().getCommitted()),
                                  mb(classSpace.getUsage().getUsed()));
            }
        }

        System.out.printf("survived %d redeploys, usage rose and fell throughout%n", iterations);
        System.out.printf("  (every drop is a batch of classes unloading, net change %s)%n",
                          mb(metaspace.getUsage().getUsed() - start));
    }

    private static void deployOnce(byte[] bytecode) {
        BytecodeLoader loader = new BytecodeLoader(bytecode);
        Class<?> deployed = loader.defineOrderService();

        // The whole fix, in one line: the value is stored on the class itself.
        ClassMetaValue.of(deployed);
    }

    private static MemoryPoolMXBean pool(String name) {
        return ManagementFactory.getMemoryPoolMXBeans()
                                .stream()
                                .filter(candidate -> candidate.getName().equals(name))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("no pool " + name));
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    /**
     * Defines the same bytes as a fresh, distinct class on every instance.
     */
    private static final class BytecodeLoader extends ClassLoader {

        private final byte[] bytecode;

        BytecodeLoader(byte[] bytecode) {
            super("WebAppClassLoader", RedeployLoopClassValue.class.getClassLoader());
            this.bytecode = bytecode;
        }

        Class<?> defineOrderService() {
            return defineClass("shop.OrderService", this.bytecode, 0, this.bytecode.length);
        }
    }
}
