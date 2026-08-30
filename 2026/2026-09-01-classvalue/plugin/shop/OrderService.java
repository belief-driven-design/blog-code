package shop;

/**
 * Stands in for an application class loaded by a container's per-deployment
 * ClassLoader. Deliberately compiled into its own output directory, so it is
 * NOT on the application classpath.
 */
public class OrderService {

    public static final byte[] BALLAST = new byte[1024];
}
