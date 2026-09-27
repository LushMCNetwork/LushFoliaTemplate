package com.playgamesinteractive.template.command;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

class CommandRouterTest {
    private static final class Router extends CommandRouter {
        private List<String> received;
        private boolean denied;

        Router() {
            register(
                    "admin",
                    "admin",
                    (sender, label, args) -> {
                        received = Arrays.asList(args);
                        return true;
                    },
                    (sender, args) -> args.length == 1 ? List.of("collector", "give") : List.of());
            register("levels", null, (sender, label, args) -> true, null);
        }

        protected boolean executeDefault(CommandSender sender, String label) {
            return true;
        }

        protected void denied(CommandSender sender) {
            denied = true;
        }
    }

    private CommandSender sender(boolean allowed) {
        return (CommandSender)
                Proxy.newProxyInstance(
                        CommandSender.class.getClassLoader(),
                        new Class<?>[] {CommandSender.class},
                        (proxy, method, args) ->
                                method.getName().equals("hasPermission") ? allowed : null);
    }

    @Test
    void deniedRoutesCannotRunOrAppearInCompletion() {
        var router = new Router();
        var sender = sender(false);
        assertEquals(
                List.of("levels"),
                router.onTabComplete(sender, null, "progression", new String[] {""}));
        assertEquals(
                List.of(),
                router.onTabComplete(sender, null, "progression", new String[] {"admin", ""}));
        router.onCommand(sender, null, "progression", new String[] {"admin", "collector"});
        assertTrue(router.denied);
        assertNull(router.received);
    }

    @Test
    void routerPassesRemainingArgumentsAndFiltersPrefixCaseInsensitively() {
        var router = new Router();
        var sender = sender(true);
        router.onCommand(sender, null, "progression", new String[] {"ADMIN", "collector", "give"});
        assertEquals(List.of("collector", "give"), router.received);
        assertEquals(
                List.of("collector"),
                router.onTabComplete(sender, null, "progression", new String[] {"admin", "COL"}));
        assertEquals(
                List.of(),
                router.onTabComplete(
                        sender,
                        null,
                        "progression",
                        new String[] {"admin", "collector", "give", ""}));
    }
}
