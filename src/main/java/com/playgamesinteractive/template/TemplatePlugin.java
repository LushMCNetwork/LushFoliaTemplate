package com.playgamesinteractive.template;

import org.bukkit.plugin.java.JavaPlugin;

public final class TemplatePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info(getName() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Safe to touch entities/players directly here without a scheduler hop -
        // Folia has already halted all region threads by the time onDisable runs,
        // so there's no concurrent region activity left to race with. The same is
        // true in reverse for onEnable: it runs before regions start ticking.
        getLogger().info(getName() + " disabled.");
    }
}
