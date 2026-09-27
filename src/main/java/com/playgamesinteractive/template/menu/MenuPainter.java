package com.playgamesinteractive.template.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

/**
 * Fills a named group of dynamic slots (declared in a menu's {@code dynamic_slots} section) with
 * content the engine itself has no domain knowledge of - a live price list or receipt history - and
 * handles clicks landing in that group. Registered against a slot-group type via {@link
 * MenuManager#registerPainter}; a feature owns exactly the same business logic it always did, it
 * just paints into slots the engine assembled instead of building its own {@code Inventory}.
 */
public interface MenuPainter {

    /**
     * Paints {@code slots} (in the order configured) into {@code inventory} for {@code viewer}.
     * {@code menu} is the menu being opened - pass it plus this group's type to {@link
     * MenuManager#buildFromTemplate} to render each entry from a configured {@code templates}
     * section instead of hardcoding its appearance in Java.
     */
    void paint(
            Player viewer,
            Object context,
            Inventory inventory,
            List<Integer> slots,
            Map<String, String> placeholders,
            Menu menu);

    /**
     * {@code index} is the position of the clicked slot within this group's configured slot list.
     */
    void onClick(Player viewer, MenuHolder holder, List<Integer> slots, int index, ClickType click);
}
