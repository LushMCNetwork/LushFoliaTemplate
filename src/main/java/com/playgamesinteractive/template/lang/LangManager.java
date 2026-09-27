package com.playgamesinteractive.template.lang;

import com.playgamesinteractive.template.scheduler.FoliaTasks;
import com.playgamesinteractive.template.text.TextStyle;

import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/** One active server-wide MiniMessage catalog; sends always return to the recipient's scheduler. */
public final class LangManager {
    private static volatile LangManager instance;
    private final FoliaTasks tasks;
    private volatile Map<String, String> messages;

    public LangManager(FoliaTasks tasks, Map<String, String> messages) {
        this.tasks = tasks;
        use(messages);
        instance = this;
    }

    public static LangManager instance() {
        return instance;
    }

    public void use(Map<String, String> candidate) {
        messages = Map.copyOf(candidate);
    }

    public Component get(String key, Object... placeholders) {
        if (placeholders.length % 2 != 0)
            throw new IllegalArgumentException("Placeholders must be key/value pairs");
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < placeholders.length; index += 2)
            values.put(
                    String.valueOf(placeholders[index]), String.valueOf(placeholders[index + 1]));
        return TextStyle.render(messages.getOrDefault(key, key), values);
    }

    public void send(CommandSender recipient, String key, Object... placeholders) {
        Component message = get(key, placeholders);
        if (recipient instanceof Player player)
            tasks.entity(player, task -> player.sendMessage(message));
        else tasks.global(task -> recipient.sendMessage(message));
    }
}
