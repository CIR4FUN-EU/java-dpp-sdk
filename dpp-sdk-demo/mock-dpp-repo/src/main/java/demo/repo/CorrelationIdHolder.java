package demo.repo;

final class CorrelationIdHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdHolder() {
    }

    static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    static String get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
