package edu.princeton.safe.internal;

@FunctionalInterface
public interface Timeable<T> {
    T get() throws Exception;

    static void time(String label,
                     Runnable runnable,
                     int repeats) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < repeats; i++) {
            runnable.run();
        }
        long elapsedTime = System.currentTimeMillis() - start;
        System.err.printf("%s: %.2f ms\n", label, (double) elapsedTime / repeats);
    }

    static <T> T time(String label,
                      Timeable<T> supplier,
                      int repeats)
            throws Exception {
        long start = System.currentTimeMillis();
        T result = null;
        for (int i = 0; i < repeats; i++) {
            result = supplier.get();
        }
        long elapsedTime = System.currentTimeMillis() - start;
        System.err.printf("%s: %.2f ms\n", label, (double) elapsedTime / repeats);
        return result;
    }
}