package com.playgamesinteractive.template.menu;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates configurable static items, dynamic groups and derived corner clusters before
 * publication.
 */
public final class MenuLayout {
    private MenuLayout() {}

    public static List<Integer> slots(List<?> raw) {
        List<Integer> slots = new ArrayList<>();
        for (Object entry : raw) {
            String value = String.valueOf(entry);
            if (value.matches("\\d+-\\d+")) {
                String[] range = value.split("-", 2);
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                if (Math.abs((long) start - end) > 54)
                    throw new IllegalArgumentException("Slot range exceeds inventory bounds");
                for (int slot = Math.min(start, end); slot <= Math.max(start, end); slot++)
                    slots.add(slot);
            } else slots.add(Integer.parseInt(value));
        }
        return List.copyOf(slots);
    }

    public static List<Integer> cornerSlots(int size) {
        return java.util.stream.Stream.of(0, 1, 9, size - 10, size - 2, size - 1)
                .filter(slot -> slot >= 0 && slot < size)
                .distinct()
                .toList();
    }

    public static void validate(YamlConfiguration yaml) {
        int size = yaml.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0)
            throw new IllegalArgumentException(
                    "Menu size must be a multiple of 9 between 9 and 54");
        String sound = yaml.getString("sound", "");
        if (!sound.isEmpty() && !sound.matches("[a-z0-9_]+:[a-z0-9_./]+"))
            throw new IllegalArgumentException("Menu sound must be a namespaced key");
        Set<Integer> occupied = new HashSet<>();
        if (yaml.contains("corner")) occupied.addAll(cornerSlots(size));
        var groups = yaml.getConfigurationSection("dynamic_slots");
        if (groups != null)
            for (String type : groups.getKeys(false)) {
                List<Integer> slots = slots(yaml.getList("dynamic_slots." + type, List.of()));
                if (slots.isEmpty())
                    throw new IllegalArgumentException("Dynamic slot groups must not be empty");
                for (int slot : slots) claim(occupied, slot, size);
            }
        var items = yaml.getConfigurationSection("items");
        if (items != null)
            for (String id : items.getKeys(false)) {
                String path = "items." + id;
                List<Integer> slots =
                        yaml.contains(path + ".slots")
                                ? slots(yaml.getList(path + ".slots", List.of()))
                                : List.of(yaml.getInt(path + ".slot", numericSlot(id)));
                for (int slot : slots) claim(occupied, slot, size);
                for (String command : yaml.getStringList(path + ".click_commands"))
                    if (!command.matches("^\\[\\w+]\\s?.*$"))
                        throw new IllegalArgumentException("Invalid click command: " + command);
            }
    }

    private static int numericSlot(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void claim(Set<Integer> occupied, int slot, int size) {
        if (slot < 0 || slot >= size)
            throw new IllegalArgumentException("Menu slot outside inventory: " + slot);
        if (!occupied.add(slot))
            throw new IllegalArgumentException("Menu content and corners overlap: " + slot);
    }
}
