package com.playgamesinteractive.template.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class TemplateSettingsTest {
    @TempDir Path folder;

    private void defaults() throws Exception {
        for (String path : TemplateSettings.DEFAULTS) {
            Path file = folder.resolve(path);
            Files.createDirectories(file.getParent());
            Files.copy(Path.of("src/main/resources", path), file);
        }
    }

    @Test
    void defaultsAreValidAndSnapshotCannotBeMutated() {
        var settings = TemplateSettings.load(Path.of("src/main/resources").toFile());
        assertEquals("en_US", settings.language());
        assertFalse(settings.welcomeMessage());
        assertThrows(
                UnsupportedOperationException.class,
                () -> settings.messages().put("starter.welcome", "Changed"));
    }

    @Test
    void invalidCandidateCannotChangePreviousSnapshot() throws Exception {
        defaults();
        var original = TemplateSettings.load(folder.toFile());
        Files.writeString(
                folder.resolve("config.yml"),
                "language: en_US\nstarter:\n  welcome-message: wrong\n");
        assertThrows(IllegalArgumentException.class, () -> TemplateSettings.load(folder.toFile()));
        assertFalse(original.welcomeMessage());
    }

    @Test
    void translationOverridesOnlyItsKeysAndCannotEscapeLanguageFolder() throws Exception {
        defaults();
        Path file = folder.resolve("config.yml");
        var yaml = ResourceFiles.read(file.toFile());
        yaml.set("language", "custom");
        yaml.save(file.toFile());
        Files.writeString(
                folder.resolve("language/custom.yml"),
                "starter:\n  greeting: '<#00f396>Custom message.'\n");
        var loaded = TemplateSettings.load(folder.toFile());
        assertEquals("<#00f396>Custom message.", loaded.messages().get("starter.greeting"));
        assertNotNull(loaded.messages().get("command.no-permission"));
        yaml.set("language", "../../other");
        yaml.save(file.toFile());
        assertThrows(IllegalArgumentException.class, () -> TemplateSettings.load(folder.toFile()));
    }
}
