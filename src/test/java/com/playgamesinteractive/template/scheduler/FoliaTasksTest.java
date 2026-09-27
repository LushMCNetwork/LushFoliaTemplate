package com.playgamesinteractive.template.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class FoliaTasksTest {
    private final AtomicInteger scheduled = new AtomicInteger();
    private final AtomicInteger cancelled = new AtomicInteger();
    private final AtomicReference<ScheduledTask.ExecutionState> state =
            new AtomicReference<>(ScheduledTask.ExecutionState.IDLE);
    private Consumer<ScheduledTask> callback;
    private final Plugin plugin = proxy(Plugin.class, (method, args) -> null);
    private final ScheduledTask handle =
            proxy(
                    ScheduledTask.class,
                    (method, args) ->
                            switch (method) {
                                case "getExecutionState" -> state.get();
                                case "cancel" -> {
                                    cancelled.incrementAndGet();
                                    state.set(ScheduledTask.ExecutionState.CANCELLED);
                                    yield null;
                                }
                                default -> null;
                            });

    interface Call {
        Object call(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(
                Proxy.newProxyInstance(
                        type.getClassLoader(),
                        new Class<?>[] {type},
                        (object, method, args) -> {
                            if (method.getName().equals("hashCode"))
                                return System.identityHashCode(object);
                            if (method.getName().equals("equals")) return object == args[0];
                            return call.call(method.getName(), args);
                        }));
    }

    @SuppressWarnings("unchecked")
    private Entity entity(boolean retired) {
        EntityScheduler scheduler =
                proxy(
                        EntityScheduler.class,
                        (method, args) -> {
                            scheduled.incrementAndGet();
                            assertSame(plugin, args[0]);
                            callback = (Consumer<ScheduledTask>) args[1];
                            return retired ? null : handle;
                        });
        return proxy(
                Entity.class, (method, args) -> method.equals("getScheduler") ? scheduler : null);
    }

    @Test
    void entityWorkUsesTheEntitySchedulerAndShutdownCancelsPendingWork() {
        var tasks = new FoliaTasks(plugin);
        AtomicInteger executed = new AtomicInteger();
        Entity entity = entity(false);
        assertSame(handle, tasks.entity(entity, task -> executed.incrementAndGet()));
        tasks.close();
        callback.accept(handle);
        assertEquals(0, executed.get());
        assertEquals(1, cancelled.get());
        assertNull(tasks.entity(entity, task -> fail("Closed dispatcher executed work")));
        assertEquals(1, scheduled.get());
        tasks.close();
        assertEquals(1, cancelled.get());
    }

    @Test
    void completedOneShotDoesNotRemainInShutdownRegistry() {
        var tasks = new FoliaTasks(plugin);
        tasks.entity(entity(false), task -> state.set(ScheduledTask.ExecutionState.FINISHED));
        callback.accept(handle);
        tasks.close();
        assertEquals(0, cancelled.get());
    }

    @Test
    void retiredEntitiesReturnNoTaskAndInvalidDelaysNeverReachScheduler() {
        var tasks = new FoliaTasks(plugin);
        assertNull(tasks.entity(entity(true), task -> fail("Retired entity executed work")));
        assertThrows(
                IllegalArgumentException.class,
                () -> tasks.entityLater(entity(false), 0, task -> {}));
        assertThrows(
                IllegalArgumentException.class,
                () -> tasks.entityTimer(entity(false), 1, 0, task -> {}));
        assertEquals(1, scheduled.get());
        tasks.close();
    }
}
