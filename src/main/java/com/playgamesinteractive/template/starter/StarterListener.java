package com.playgamesinteractive.template.starter;

import com.playgamesinteractive.template.config.TemplateSettings;
import com.playgamesinteractive.template.lang.LangManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.function.Supplier;

/**
 * Replace this example feature with your own domain; keep lifecycle assembly in the plugin class.
 */
public final class StarterListener implements Listener {
    private final Supplier<TemplateSettings> settings;
    private final LangManager language;

    public StarterListener(Supplier<TemplateSettings> settings, LangManager language) {
        this.settings = settings;
        this.language = language;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (settings.get().welcomeMessage())
            language.send(
                    event.getPlayer(), "starter.welcome", "player", event.getPlayer().getName());
    }
}
