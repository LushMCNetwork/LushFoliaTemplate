package com.playgamesinteractive.template.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

/**
 * A configured appearance for one repeated entry inside a {@code dynamic_slots} group (a player
 * list, upgrade or history entry), declared under a menu's {@code templates} section. A {@link
 * MenuPainter} that wants its entries' look to live in YAML rather than hardcoded Java calls {@link
 * MenuManager#buildFromTemplate} with this plus a per-entry placeholder map.
 */
public record ItemTemplate(
        Material material,
        String displayName,
        List<String> lore,
        List<ItemFlag> itemFlags,
        Boolean enchantmentGlintOverride) {

    public ItemTemplate {
        lore = List.copyOf(lore);
        itemFlags = List.copyOf(itemFlags);
    }
}
