package com.playgamesinteractive.template.command;

import com.playgamesinteractive.template.lang.LangManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reusable first-argument command router with permission-filtered completion. */
public abstract class CommandRouter implements TabExecutor {
    @FunctionalInterface
    public interface Executor {
        boolean execute(CommandSender sender, String label, String[] args);
    }

    @FunctionalInterface
    public interface Completer {
        List<String> complete(CommandSender sender, String[] args);
    }

    public record Route(String name, String permission, Executor executor, Completer completer) {}

    private final Map<String, Route> routes = new LinkedHashMap<>();

    protected final void register(
            String name, String permission, Executor executor, Completer completer) {
        routes.put(
                name.toLowerCase(Locale.ROOT),
                new Route(
                        name.toLowerCase(Locale.ROOT),
                        permission,
                        executor,
                        completer == null ? (sender, args) -> List.of() : completer));
    }

    protected final List<String> routeNames(CommandSender sender) {
        return routes.values().stream()
                .filter(route -> allowed(sender, route.permission))
                .map(Route::name)
                .toList();
    }

    @Override
    public final boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return executeDefault(sender, label);
        Route route = routes.get(args[0].toLowerCase(Locale.ROOT));
        if (route == null) return unknown(sender, label, args[0]);
        if (!allowed(sender, route.permission)) {
            denied(sender);
            return true;
        }
        return route.executor.execute(sender, label, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public final List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) return prefix(routeNames(sender), args.length == 0 ? "" : args[0]);
        Route route = routes.get(args[0].toLowerCase(Locale.ROOT));
        if (route == null || !allowed(sender, route.permission)) return List.of();
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        return prefix(route.completer.complete(sender, remaining), remaining[remaining.length - 1]);
    }

    protected void denied(CommandSender sender) {
        LangManager.instance().send(sender, "command.no-permission");
    }

    protected abstract boolean executeDefault(CommandSender sender, String label);

    protected boolean unknown(CommandSender sender, String label, String argument) {
        LangManager.instance().send(sender, "command.help");
        return true;
    }

    protected static List<String> prefix(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String value : values)
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) results.add(value);
        return List.copyOf(results);
    }

    private static boolean allowed(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }
}
