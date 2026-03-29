package cache.utils;

import java.util.ArrayDeque;
import java.util.Deque;

import common.LogUtil;
import server.EchoServer;

public final class Tracer {

    private static final Deque<Long> stack = new ArrayDeque<>();
    private static final Deque<String> names = new ArrayDeque<>();

    public static void start(String name, Object... printables) {
        if (!EchoServer.DEBUG_SERVER)
            return;

        stack.push(System.nanoTime());
        names.push(name);

        LogUtil.log("[LOG]", "Started", name, "Printables", printables + "");
    }

    public static void end(String... printables) {
        if (!EchoServer.DEBUG_SERVER)
            return;

        long start = stack.pop();
        String name = names.pop();

        long dur = System.nanoTime() - start;

        LogUtil.log("[LOG]", "Ended", name, "took_ns", dur, "Printables", printables + "");
    }
}