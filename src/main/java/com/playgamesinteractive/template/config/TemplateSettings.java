package com.playgamesinteractive.template.config;

import com.playgamesinteractive.template.text.TextStyle;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable runtime snapshot: parse and validate the complete candidate before publishing it. */
public record TemplateSettings(
        String language, boolean welcomeMessage, Map<String, String> messages) {
    public static final List<String> DEFAULTS =
            List.of("config.yml", "language/en_US.yml", "menus/template_menu.yml");

    public TemplateSettings {
        messages = Map.copyOf(messages);
    }

    public static void initialize(Plugin plugin) {
        for (String path : DEFAULTS) ResourceFiles.heal(plugin, path);
    }

    public static TemplateSettings load(File folder) {
        var config = ResourceFiles.read(new File(folder, "config.yml"));
        String language = config.getString("language", "en_US");
        if (!language.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalArgumentException("Invalid language file name");
        if (!config.isBoolean("starter.welcome-message"))
            throw new IllegalArgumentException("starter.welcome-message must be true or false");
        var english = ResourceFiles.read(new File(folder, "language/en_US.yml"));
        Map<String, String> messages = catalog(english);
        if (!language.equals("en_US")) {
            File translation = new File(folder, "language/" + language + ".yml");
            if (translation.exists()) messages.putAll(catalog(ResourceFiles.read(translation)));
        }
        return new TemplateSettings(
                language, config.getBoolean("starter.welcome-message"), messages);
    }

    private static Map<String, String> catalog(YamlConfiguration yaml) {
        Map<String, String> messages = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) continue;
            if (!yaml.isString(key))
                throw new IllegalArgumentException("Language entry must be text: " + key);
            String text = yaml.getString(key);
            TextStyle.validate(text);
            messages.put(key, text);
        }
        return messages;
    }
}
