package com.playgamesinteractive.template.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

/**
 * One configured slot's static appearance and behavior, before placeholder substitution. {@code
 * showIf}, when set, names a placeholder token (no braces, e.g. {@code has_next_page}) that must
 * resolve to {@code "true"} for this item to be placed at all - lets a menu conditionally omit an
 * item (e.g. a "next page" button with no next page to go to) instead of always showing it.
 */
public record MenuItem(
        int slot,
        Material material,
        String displayName,
        List<String> lore,
        List<ClickCommand> clickCommands,
        String permission,
        List<ItemFlag> itemFlags,
        Boolean enchantmentGlintOverride,
        String showIf) {

    public MenuItem {
        lore = List.copyOf(lore);
        clickCommands = List.copyOf(clickCommands);
        itemFlags = List.copyOf(itemFlags);
    }
}
