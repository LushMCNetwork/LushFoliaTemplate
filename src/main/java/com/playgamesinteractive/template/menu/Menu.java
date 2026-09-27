package com.playgamesinteractive.template.menu;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One loaded {@code menus/<id>.yml} layout: decorations, static item slots, named dynamic slot
 * groups (from {@code dynamic_slots}) that a registered {@link MenuPainter} fills at open time, and
 * named per-entry {@code templates} a painter can render its repeated items from instead of
 * hardcoding their appearance in Java. {@code corner} is a single decoration applied to the fixed
 * top-left/bottom-right corner clusters {@link MenuManager#cornerSlots} computes from {@code size}
 * - never configured per-file, unlike the old separate {@code corner}/{@code accent} pair it
 * replaced.
 */
public record Menu(
        String id,
        String title,
        int size,
        ItemStack fill,
        ItemStack corner,
        Map<Integer, MenuItem> items,
        String permission,
        String sound,
        Map<String, List<Integer>> dynamicSlots,
        Map<String, ItemTemplate> templates) {

    public Menu {
        items = Map.copyOf(items);
        Map<String, List<Integer>> copiedSlots = new LinkedHashMap<>();
        dynamicSlots.forEach((type, slots) -> copiedSlots.put(type, List.copyOf(slots)));
        dynamicSlots = Map.copyOf(copiedSlots);
        templates = Map.copyOf(templates);
    }

    /** The dynamic slot group (if any) that {@code slot} belongs to, keyed by its painter type. */
    public Optional<Map.Entry<String, List<Integer>>> dynamicGroupFor(int slot) {
        return dynamicSlots.entrySet().stream()
                .filter(entry -> entry.getValue().contains(slot))
                .findFirst();
    }

    public List<MenuItem> itemList() {
        return List.copyOf(items().values());
    }
}
