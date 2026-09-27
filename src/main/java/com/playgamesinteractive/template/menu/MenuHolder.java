package com.playgamesinteractive.template.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

/**
 * Tags an open inventory with the {@link Menu} that built it, its feature context, and the menu it
 * was opened from.
 */
public final class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private final Object context;
    private final MenuHolder previous;
    private Inventory inventory;
    private final Map<String, String> placeholders;

    public MenuHolder(
            Menu menu, Object context, MenuHolder previous, Map<String, String> placeholders) {
        this.menu = menu;
        this.context = context;
        this.previous = previous;
        this.placeholders = Map.copyOf(placeholders);
    }

    public Menu menu() {
        return menu;
    }

    /** The feature-owned snapshot supplied when the menu was opened. */
    public Object context() {
        return context;
    }

    /** The menu this one was opened from via {@code open_menu}, or null if opened directly. */
    public MenuHolder previous() {
        return previous;
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
