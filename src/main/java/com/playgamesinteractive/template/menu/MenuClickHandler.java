package com.playgamesinteractive.template.menu;

import org.bukkit.entity.Player;

/**
 * Handles one custom {@code click_commands} tag (e.g. {@code toggle_open}, {@code purchase_border},
 * {@code claim_quest}) that a specific menu needs beyond the engine's generic
 * close/message/command/console_command/open_menu vocabulary. Registered via {@link
 * MenuListener#registerHandler} so {@code MenuListener} never has to import the feature managers
 * that own this behavior.
 */
public interface MenuClickHandler {
    void handle(Player player, MenuHolder holder, String argument);
}
