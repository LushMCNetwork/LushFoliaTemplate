package com.playgamesinteractive.template.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Extracts missing defaults and adds missing keys; malformed existing files are never replaced. */
public final class ResourceFiles {
    private ResourceFiles() {}

    public static YamlConfiguration read(File file) {
        var yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception error) {
            throw new IllegalArgumentException("Cannot load " + file.getName(), error);
        }
        return yaml;
    }

    public static YamlConfiguration bundled(Plugin plugin, String path) {
        try (var stream =
                        Objects.requireNonNull(
                                plugin.getResource(path), "Missing bundled resource: " + path);
                var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        } catch (Exception error) {
            throw new IllegalArgumentException("Cannot read bundled " + path, error);
        }
    }

    /** Startup only; reload validates files without rewriting them. */
    public static void heal(Plugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        try {
            if (!file.exists()) {
                plugin.saveResource(path, false);
                return;
            }
            var current = read(file);
            var defaults = bundled(plugin, path);
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || current.contains(key)) continue;
                String[] segments = key.split("\\.");
                String ancestor = "";
                for (int index = 0; index < segments.length - 1; index++) {
                    ancestor =
                            ancestor.isEmpty() ? segments[index] : ancestor + "." + segments[index];
                    if (current.contains(ancestor) && !current.isConfigurationSection(ancestor))
                        throw new IllegalArgumentException(
                                "Expected configuration section: " + ancestor);
                }
                current.set(key, defaults.get(key));
                changed = true;
            }
            if (!changed) return;
            // Preserve an operator's original before a first schema-extension write.
            var backup = file.toPath().resolveSibling(file.getName() + ".bak");
            if (!Files.exists(backup))
                Files.copy(file.toPath(), backup, StandardCopyOption.COPY_ATTRIBUTES);
            current.save(file);
        } catch (Exception error) {
            throw new IllegalArgumentException("Cannot initialize " + path, error);
        }
    }
}
