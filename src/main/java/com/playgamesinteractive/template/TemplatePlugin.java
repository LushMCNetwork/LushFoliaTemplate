package com.playgamesinteractive.template;

import com.playgamesinteractive.lushmenus.api.SharedMenus;
import com.playgamesinteractive.template.command.TemplateCommand;
import com.playgamesinteractive.template.config.TemplateSettings;
import com.playgamesinteractive.template.lang.LangManager;
import com.playgamesinteractive.template.menu.MenuListener;
import com.playgamesinteractive.template.menu.MenuManager;
import com.playgamesinteractive.template.scheduler.FoliaTasks;
import com.playgamesinteractive.template.starter.StarterListener;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Copy this project, rename its identity, then replace the small starter feature with your domain.
 */
public final class TemplatePlugin extends JavaPlugin {
    private volatile TemplateSettings settings;
    private final AtomicBoolean reloading = new AtomicBoolean();
    private FoliaTasks tasks;
    private LangManager language;
    private MenuManager menus;

    @Override
    public void onEnable() {
        if (!isFolia()) {
            getLogger().severe("This plugin requires a Folia-based server. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            SharedMenus
                    .service(); // Fail before initialization if the required service is
                                // unavailable.
            TemplateSettings.initialize(this);
            settings = TemplateSettings.load(getDataFolder());
            tasks = new FoliaTasks(this);
            language = new LangManager(tasks, settings.messages());
            menus = new MenuManager(this, tasks);
            menus.loadAll();
            var menuListener = new MenuListener(menus, tasks);
            menuListener.registerHandler(
                    "greet",
                    (player, holder, argument) ->
                            language.send(player, "starter.greeting", "player", player.getName()));
            SharedMenus.registerEvents(menuListener, this);
            SharedMenus.registerEvents(new StarterListener(() -> settings, language), this);
            var command = new TemplateCommand(language, menus, this::reload);
            Objects.requireNonNull(getCommand("template")).setExecutor(command);
            getCommand("template").setTabCompleter(command);
            getLogger().info(getName() + " enabled with MiniMessage and Folia scheduling.");
        } catch (Exception error) {
            getLogger().log(Level.SEVERE, "Invalid startup configuration; disabling", error);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void reload(CommandSender sender) {
        if (!reloading.compareAndSet(false, true)) {
            language.send(sender, "config.busy");
            return;
        }
        tasks.async(
                () -> {
                    try {
                        TemplateSettings candidate = TemplateSettings.load(getDataFolder());
                        var layouts = menus.readAll(); // File I/O only; ItemMeta is built on the
                        // global scheduler below.
                        tasks.global(
                                task -> {
                                    try {
                                        var candidateMenus = menus.prepare(layouts);
                                        SharedMenus.invalidate(this);
                                        settings = candidate;
                                        language.use(candidate.messages());
                                        menus.replaceAll(candidateMenus);
                                        language.send(sender, "config.reloaded");
                                    } catch (Exception error) {
                                        reloadFailed(sender, error);
                                    } finally {
                                        reloading.set(false);
                                    }
                                });
                    } catch (Exception error) {
                        reloadFailed(sender, error);
                        reloading.set(false);
                    }
                });
    }

    private void reloadFailed(CommandSender sender, Exception error) {
        getLogger()
                .log(
                        Level.WARNING,
                        "Reload rejected; previous configuration remains active",
                        error);
        language.send(sender, "config.invalid", "reason", String.valueOf(error.getMessage()));
    }

    @Override
    public void onDisable() {
        if (tasks != null) tasks.close();
    }

    private boolean isFolia() {
        try {
            Class.forName(
                    "io.papermc.paper.threadedregions.RegionizedServer", false, getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
