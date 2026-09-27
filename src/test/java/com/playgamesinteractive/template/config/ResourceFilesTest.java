package com.playgamesinteractive.template.config;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

class ResourceFilesTest {
    @TempDir Path folder;

    private Plugin plugin() {
        return (Plugin)
                Proxy.newProxyInstance(
                        Plugin.class.getClassLoader(),
                        new Class<?>[] {Plugin.class},
                        (object, method, args) ->
                                switch (method.getName()) {
                                    case "getDataFolder" -> folder.toFile();
                                    case "getResource" ->
                                            Files.newInputStream(
                                                    Path.of(
                                                            "src/main/resources",
                                                            (String) args[0]));
                                    default -> null;
                                });
    }

    @Test
    void healingKeepsConfiguredValuesAddsMissingKeysAndBacksUpOriginal() throws Exception {
        Path file = folder.resolve("config.yml");
        String original = "language: custom\n";
        Files.writeString(file, original);
        ResourceFiles.heal(plugin(), "config.yml");
        var yaml = ResourceFiles.read(file.toFile());
        assertEquals("custom", yaml.getString("language"));
        assertFalse(yaml.getBoolean("starter.welcome-message"));
        assertEquals(original, Files.readString(folder.resolve("config.yml.bak")));
        String healed = Files.readString(file);
        ResourceFiles.heal(plugin(), "config.yml");
        assertEquals(healed, Files.readString(file));
    }

    @Test
    void malformedYamlAndWrongSectionTypesAreNeverReplaced() throws Exception {
        Path file = folder.resolve("config.yml");
        for (String original :
                new String[] {"language: [invalid\n", "language: en_US\nstarter: wrong\n"}) {
            Files.writeString(file, original);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ResourceFiles.heal(plugin(), "config.yml"));
            assertEquals(original, Files.readString(file));
        }
    }
}
