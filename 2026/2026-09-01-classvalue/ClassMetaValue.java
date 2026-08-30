import java.lang.ClassValue;

/**
 * The same cache, with the ownership inverted: the value lives on the Class,
 * so nothing here references a Class at all.
 */
record ClassMetaValue(String packageName, String simpleName) {

    private static final ClassValue<ClassMetaValue> META = new ClassValue<>() {

        @Override
        protected ClassMetaValue computeValue(Class<?> c) {
            return new ClassMetaValue(c.getPackageName(),
                                      c.getSimpleName());
        }
    };

    static ClassMetaValue of(Class<?> c) {
        return META.get(c);
    }
}
