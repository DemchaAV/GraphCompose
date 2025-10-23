package com.demcha.system.pdf_systems;

public final class StreamTracker {
    private static final java.util.concurrent.ConcurrentHashMap<AutoCloseable, RuntimeException> OPEN =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static <T extends AutoCloseable> T track(T resource) {
        OPEN.put(resource, new RuntimeException("Opened here"));
        return resource;
    }

    public static void close(AutoCloseable resource) {
        try { if (resource != null) resource.close(); }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { OPEN.remove(resource); }
    }

    public static void assertNoOpenStreams() {
        if (!OPEN.isEmpty()) {
            StringBuilder sb = new StringBuilder("Detected unclosed PDF streams:\n");
            OPEN.forEach((res, where) -> {
                sb.append("- ").append(res.getClass().getName()).append('\n');
                for (StackTraceElement el : where.getStackTrace()) {
                    sb.append("    at ").append(el).append('\n');
                }
            });
            throw new IllegalStateException(sb.toString());
        }
    }
}
