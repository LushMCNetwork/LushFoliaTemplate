package com.playgamesinteractive.template.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Routes work to its owner and tracks plugin tasks, including entity tasks, for shutdown. */
public final class FoliaTasks implements AutoCloseable {
    private final Plugin plugin;
    private final Set<ScheduledTask> ownedTasks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public FoliaTasks(Plugin plugin) {
        this.plugin = plugin;
    }

    public ScheduledTask entity(Entity entity, Consumer<ScheduledTask> action) {
        if (closed.get()) return null;
        return track(entity.getScheduler().run(plugin, once(action), null));
    }

    public ScheduledTask entityLater(Entity entity, long ticks, Consumer<ScheduledTask> action) {
        requireTicks(ticks);
        if (closed.get()) return null;
        return track(entity.getScheduler().runDelayed(plugin, once(action), null, ticks));
    }

    public ScheduledTask entityTimer(
            Entity entity, long delay, long period, Consumer<ScheduledTask> action) {
        requireTicks(delay);
        requireTicks(period);
        if (closed.get()) return null;
        return track(
                entity.getScheduler().runAtFixedRate(plugin, guarded(action), null, delay, period));
    }

    public ScheduledTask region(Location location, Consumer<ScheduledTask> action) {
        if (closed.get()) return null;
        return track(Bukkit.getRegionScheduler().run(plugin, location.clone(), once(action)));
    }

    public ScheduledTask regionLater(
            Location location, long ticks, Consumer<ScheduledTask> action) {
        requireTicks(ticks);
        if (closed.get()) return null;
        return track(
                Bukkit.getRegionScheduler()
                        .runDelayed(plugin, location.clone(), once(action), ticks));
    }

    public ScheduledTask regionTimer(
            Location location, long delay, long period, Consumer<ScheduledTask> action) {
        requireTicks(delay);
        requireTicks(period);
        if (closed.get()) return null;
        return track(
                Bukkit.getRegionScheduler()
                        .runAtFixedRate(plugin, location.clone(), guarded(action), delay, period));
    }

    public ScheduledTask global(Consumer<ScheduledTask> action) {
        if (closed.get()) return null;
        return track(Bukkit.getGlobalRegionScheduler().run(plugin, once(action)));
    }

    /** I/O and immutable calculations only: never access inventories, entities or blocks here. */
    public ScheduledTask async(Runnable action) {
        if (closed.get()) return null;
        return track(Bukkit.getAsyncScheduler().runNow(plugin, once(task -> action.run())));
    }

    public ScheduledTask asyncTimer(long delay, long period, TimeUnit unit, Runnable action) {
        if (delay < 1 || period < 1)
            throw new IllegalArgumentException("Async intervals must be positive");
        if (closed.get()) return null;
        return track(
                Bukkit.getAsyncScheduler()
                        .runAtFixedRate(
                                plugin, guarded(task -> action.run()), delay, period, unit));
    }

    private Consumer<ScheduledTask> guarded(Consumer<ScheduledTask> action) {
        return task -> {
            if (!closed.get()) action.accept(task);
        };
    }

    private Consumer<ScheduledTask> once(Consumer<ScheduledTask> action) {
        return task -> {
            try {
                if (!closed.get()) action.accept(task);
            } finally {
                ownedTasks.remove(task);
            }
        };
    }

    private ScheduledTask track(ScheduledTask task) {
        ownedTasks.removeIf(this::finished);
        if (task == null) return null; // The entity already retired; no callback will run.
        ownedTasks.add(task);
        if (closed.get()) {
            task.cancel();
            ownedTasks.remove(task);
        } else if (finished(task))
            ownedTasks.remove(task); // Covers execution racing with registration.
        return task;
    }

    private boolean finished(ScheduledTask task) {
        return switch (task.getExecutionState()) {
            case FINISHED, CANCELLED, CANCELLED_RUNNING -> true;
            default -> false;
        };
    }

    private static void requireTicks(long ticks) {
        if (ticks < 1)
            throw new IllegalArgumentException(
                    "Folia delays and periods must be at least one tick");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        ownedTasks.forEach(ScheduledTask::cancel);
        ownedTasks.clear();
    }
}
