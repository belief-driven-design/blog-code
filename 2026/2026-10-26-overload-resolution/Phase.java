public class Phase {

    // PHASE 1

    static String f(long x) {
        return "f(long)";
    }

    static String f(Integer x) {
        return "f(Integer)";
    }

    static String f(int... x) {
        return "f(int...)";
    }

    // PHASE 2

    static String g(Integer x) {
        return "g(Integer)";
    }

    static String g(int... x) {
        return "g(int...)";
    }

    // PHASE 3

    static String h(Object x) {
        return "h(Object)";
    }

    static String h(int... x) {
        return "h(int...)";
    }

    public static void main(String... args) {

        // PHASE 1: widening wins, the other two never compete
        System.out.println("f(1) -> " + f(1));

        // PHASE 2: no long overload, so boxing finally gets its turn
        System.out.println("g(1) -> " + g(1));

        // PHASE 2 again: Object beats int... by an entire phase
        System.out.println("h(1) -> " + h(1));
    }
}
