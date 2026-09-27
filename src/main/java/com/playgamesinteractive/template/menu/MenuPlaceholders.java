package com.playgamesinteractive.template.menu;

import com.playgamesinteractive.template.text.TextStyle;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/** Internal menu tokens use braces. External text is escaped before it reaches MiniMessage. */
public final class MenuPlaceholders {
    private MenuPlaceholders() {}

    public static Component render(String template, Player viewer, Map<String, String> values) {
        Map<String, String> tokens = new LinkedHashMap<>(values);
        tokens.put("player", viewer.getName());
        return TextStyle.render(template, tokens);
    }
}
