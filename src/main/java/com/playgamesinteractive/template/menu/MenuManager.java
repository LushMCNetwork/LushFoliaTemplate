package com.playgamesinteractive.template.menu;

import com.playgamesinteractive.lushmenus.api.SharedMenus;
import com.playgamesinteractive.template.config.ResourceFiles;
import com.playgamesinteractive.template.lang.LangManager;
import com.playgamesinteractive.template.scheduler.FoliaTasks;
import com.playgamesinteractive.template.text.TextStyle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads MiniMessage YAML layouts and renders static items, decorations and registered dynamic
 * painters.
 */
public final class MenuManager {

    private static final Pattern CLICK_COMMAND_PATTERN = Pattern.compile("^\\[(\\w+)]\\s?(.*)$");
    private final Plugin plugin;
    private final FoliaTasks tasks;
    private final File menusDir;
    private final Map<String, MenuPainter> painters = new LinkedHashMap<>();
    private volatile Map<String, Menu> menus = Map.of();
    private BiPredicate<Player, String> accessCheck = (player, id) -> true;

    public void setAccessCheck(BiPredicate<Player, String> check) {
        accessCheck = check;
    }

    public MenuManager(Plugin plugin, FoliaTasks tasks) {
        this.plugin = plugin;
        this.tasks = tasks;
        this.menusDir = new File(plugin.getDataFolder(), "menus");
    }

    /**
     * Registers the painter responsible for every {@code dynamic_slots} group of this {@code type}.
     */
    public void registerPainter(String type, MenuPainter painter) {
        painters.put(type, painter);
    }

    public MenuPainter painter(String type) {
        return painters.get(type);
    }

    public void loadAll() {
        replaceAll(prepareAll());
    }

    public void replaceAll(Map<String, Menu> candidate) {
        menus = Map.copyOf(candidate);
    }

    public Map<String, Menu> prepareAll() {
        return prepare(readAll());
    }

    /** Reads/validates YAML without accessing inventories, worlds or item metadata. */
    public Map<String, YamlConfiguration> readAll() {
        Map<String, YamlConfiguration> loaded = new LinkedHashMap<>();
        File[] files =
                menusDir.listFiles(
                        (ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) throw new IllegalArgumentException("Cannot read menu directory");
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            YamlConfiguration yaml = ResourceFiles.read(file);
            MenuLayout.validate(yaml);
            for (String key : yaml.getKeys(true)) {
                if (yaml.isString(key)) TextStyle.validate(yaml.getString(key));
                else if (yaml.isList(key))
                    for (Object entry : yaml.getList(key))
                        if (entry instanceof String text) TextStyle.validate(text);
            }
            loaded.put(id, yaml);
        }
        return Map.copyOf(loaded);
    }

    /**
     * Builds detached menu icons on startup/the global scheduler before replacing the live
     * snapshot.
     */
    public Map<String, Menu> prepare(Map<String, YamlConfiguration> layouts) {
        Map<String, Menu> loaded = new LinkedHashMap<>();
        layouts.forEach((id, yaml) -> loaded.put(id, loadMenuFile(id, yaml)));
        if (!loaded.containsKey("template_menu"))
            throw new IllegalArgumentException("The starter template_menu.yml is missing");
        return Map.copyOf(loaded);
    }

    private Menu loadMenuFile(String id, YamlConfiguration yaml) {
        File file = new File(menusDir, id + ".yml");
        MenuLayout.validate(yaml);
        String title = yaml.getString("title", id);
        int size = yaml.getInt("size", 27);
        String menuPermission = yaml.getString("permission", null);

        ItemStack fill = parseDecoration(yaml, "fill.", file.getName());
        ItemStack corner = parseDecoration(yaml, "corner.", file.getName());

        Map<Integer, MenuItem> items = new LinkedHashMap<>();
        var itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "items." + key + ".";
                String materialName = yaml.getString(path + "material", "STONE");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem())
                    throw new IllegalArgumentException("Unknown or air material: " + materialName);
                String displayName = yaml.getString(path + "display_name", "");
                List<String> lore = yaml.getStringList(path + "lore");
                List<ClickCommand> clickCommands =
                        parseClickCommands(
                                yaml.getStringList(path + "click_commands"), file.getName());
                String openMenuTarget = yaml.getString(path + "open_menu", null);
                if (openMenuTarget != null && !openMenuTarget.isBlank()) {
                    clickCommands.add(new ClickCommand("open_menu", openMenuTarget.trim()));
                }
                String permission = yaml.getString(path + "permission", null);
                List<ItemFlag> itemFlags =
                        parseItemFlags(yaml.getStringList(path + "item_flags"), file.getName());
                Boolean glintOverride =
                        yaml.isSet(path + "enchantment_glint_override")
                                ? yaml.getBoolean(path + "enchantment_glint_override")
                                : null;
                String showIf = yaml.getString(path + "show_if", null);

                List<Integer> resolvedSlots = resolveItemSlots(yaml, path, key, file.getName());
                if (resolvedSlots.isEmpty()) {
                    plugin.getLogger()
                            .warning(
                                    "Skipping item '"
                                            + key
                                            + "' in "
                                            + file.getName()
                                            + " - no valid slot/slots given and key isn't"
                                            + " numeric.");
                    continue;
                }
                for (int slot :
                        inBounds(resolvedSlots, size, "item '" + key + "'", file.getName())) {
                    items.put(
                            slot,
                            new MenuItem(
                                    slot,
                                    material,
                                    displayName,
                                    lore,
                                    clickCommands,
                                    permission,
                                    itemFlags,
                                    glintOverride,
                                    showIf));
                }
            }
        }

        Map<String, List<Integer>> dynamicSlots = new LinkedHashMap<>();
        var dynamicSection = yaml.getConfigurationSection("dynamic_slots");
        if (dynamicSection != null) {
            for (String type : dynamicSection.getKeys(false)) {
                List<Integer> slots =
                        inBounds(
                                parseSlots(
                                        yaml.getList("dynamic_slots." + type, List.of()),
                                        file.getName()),
                                size,
                                "dynamic_slots." + type,
                                file.getName());
                if (!slots.isEmpty()) {
                    dynamicSlots.put(type, slots);
                }
            }
        }

        Map<String, ItemTemplate> templates = new LinkedHashMap<>();
        var templatesSection = yaml.getConfigurationSection("templates");
        if (templatesSection != null) {
            for (String type : templatesSection.getKeys(false)) {
                templates.put(type, parseTemplate(yaml, "templates." + type + ".", file.getName()));
            }
        }

        return new Menu(
                id,
                title,
                size,
                fill,
                corner,
                items,
                menuPermission,
                yaml.getString("sound", ""),
                dynamicSlots,
                templates);
    }

    /**
     * The fixed top-left ({@code 0, 1, 9}) and bottom-right ({@code size-10, size-2, size-1})
     * corner clusters a menu's {@code corner} decoration is painted onto - computed from {@code
     * size} alone, never configured per-file. Out-of-range slots (only possible for a one-row menu
     * smaller than this pattern needs) are silently dropped rather than warned about, since this is
     * derived layout, not user input.
     */
    static List<Integer> cornerSlots(int size) {
        return MenuLayout.cornerSlots(size);
    }

    private ItemTemplate parseTemplate(YamlConfiguration yaml, String path, String fileName) {
        String materialName = yaml.getString(path + "material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem())
            throw new IllegalArgumentException("Unknown or air material: " + materialName);
        String displayName = yaml.getString(path + "display_name", "");
        List<String> lore = yaml.getStringList(path + "lore");
        List<ItemFlag> itemFlags =
                parseItemFlags(yaml.getStringList(path + "item_flags"), fileName);
        Boolean glintOverride =
                yaml.isSet(path + "enchantment_glint_override")
                        ? yaml.getBoolean(path + "enchantment_glint_override")
                        : null;
        return new ItemTemplate(material, displayName, lore, itemFlags, glintOverride);
    }

    private ItemStack parseDecoration(YamlConfiguration yaml, String path, String fileName) {
        if (!yaml.isSet(path + "material")) {
            return null;
        }
        String materialName = yaml.getString(path + "material");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem())
            throw new IllegalArgumentException("Unknown or air material: " + materialName);
        String displayName = yaml.getString(path + "display_name", " ");
        List<ItemFlag> itemFlags =
                parseItemFlags(yaml.getStringList(path + "item_flags"), fileName);
        Boolean glintOverride =
                yaml.isSet(path + "enchantment_glint_override")
                        ? yaml.getBoolean(path + "enchantment_glint_override")
                        : null;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(nonItalic(TextStyle.component(displayName)));
            applyCosmetics(meta, itemFlags, glintOverride);
            meta.setHideTooltip(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<ItemFlag> parseItemFlags(List<String> raw, String fileName) {
        List<ItemFlag> flags = new ArrayList<>();
        for (String name : raw) {
            try {
                flags.add(ItemFlag.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown item flag: " + name, e);
            }
        }
        return flags;
    }

    private List<ClickCommand> parseClickCommands(List<String> raw, String fileName) {
        List<ClickCommand> commands = new ArrayList<>();
        for (String line : raw) {
            Matcher matcher = CLICK_COMMAND_PATTERN.matcher(line);
            if (!matcher.matches()) {
                plugin.getLogger()
                        .warning(
                                "Invalid click_commands entry '"
                                        + line
                                        + "' in "
                                        + fileName
                                        + " - expected '[tag] argument'.");
                continue;
            }
            commands.add(
                    new ClickCommand(
                            matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2).trim()));
        }
        return commands;
    }

    private List<Integer> resolveItemSlots(
            YamlConfiguration yaml, String path, String key, String fileName) {
        List<?> rawSlots = yaml.getList(path + "slots");
        if (rawSlots != null && !rawSlots.isEmpty()) {
            return parseSlots(rawSlots, fileName);
        }
        if (yaml.isSet(path + "slot")) {
            Integer single = tryParseInt(String.valueOf(yaml.get(path + "slot")));
            return single != null ? List.of(single) : List.of();
        }
        Integer keyAsSlot = tryParseInt(key);
        return keyAsSlot != null ? List.of(keyAsSlot) : List.of();
    }

    private List<Integer> parseSlots(List<?> raw, String fileName) {
        return MenuLayout.slots(raw);
    }

    private List<Integer> inBounds(List<Integer> slots, int size, String context, String fileName) {
        List<Integer> valid = new ArrayList<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= size) {
                plugin.getLogger()
                        .warning(
                                "Slot "
                                        + slot
                                        + " for "
                                        + context
                                        + " in "
                                        + fileName
                                        + " is out of bounds for a size-"
                                        + size
                                        + " menu (valid range 0-"
                                        + (size - 1)
                                        + ") - skipping.");
            } else {
                valid.add(slot);
            }
        }
        return valid;
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Menu getMenu(String id) {
        return menus.get(id);
    }

    /** Close old holders so their actions cannot use a pre-reload layout or recipe index. */
    /**
     * Invalidates this owner's managed sessions, including custom menus outside the YAML loader.
     */
    public void invalidateOpenMenus() {
        SharedMenus.invalidate(plugin);
    }

    /** Opens {@code menuId} directly (no back-navigation target). */
    public void open(
            Player player, String menuId, Object context, Map<String, String> placeholders) {
        openWithBack(player, menuId, context, placeholders, null);
    }

    public void openWithBack(
            Player player,
            String menuId,
            Object context,
            Map<String, String> placeholders,
            MenuHolder previous) {
        tasks.entity(
                player, task -> openOnOwnerThread(player, menuId, context, placeholders, previous));
    }

    private void openOnOwnerThread(
            Player player,
            String menuId,
            Object context,
            Map<String, String> placeholders,
            MenuHolder previous) {
        if (!accessCheck.test(player, menuId)) return;
        Menu menu = menus.get(menuId);
        if (menu == null) {
            player.sendMessage(LangManager.instance().get("menu.not-configured", "menu", menuId));
            return;
        }
        if (menu.permission() != null && !player.hasPermission(menu.permission())) {
            player.sendMessage(LangManager.instance().get("command.no-permission"));
            return;
        }

        MenuHolder holder = new MenuHolder(menu, context, previous, placeholders);
        Component title = nonItalic(MenuPlaceholders.render(menu.title(), player, placeholders));
        Inventory inventory = SharedMenus.createInventory(plugin, holder, menu.size(), title);
        holder.setInventory(inventory);

        Set<Integer> dynamicSlotSet =
                menu.dynamicSlots().values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toUnmodifiableSet());
        paintDecorations(inventory, menu, dynamicSlotSet);
        for (MenuItem item : menu.itemList()) {
            if (item.permission() != null && !player.hasPermission(item.permission())) {
                continue;
            }
            if (item.showIf() != null
                    && !"true".equalsIgnoreCase(placeholders.get(item.showIf()))) {
                continue;
            }
            inventory.setItem(item.slot(), buildItemStack(item, player, placeholders));
        }
        for (var group : menu.dynamicSlots().entrySet()) {
            MenuPainter painter = painters.get(group.getKey());
            if (painter == null) {
                plugin.getLogger()
                        .warning(
                                "No painter registered for dynamic_slots type '"
                                        + group.getKey()
                                        + "' in menu '"
                                        + menu.id()
                                        + "'.");
                continue;
            }
            painter.paint(player, context, inventory, group.getValue(), placeholders, menu);
        }
        player.openInventory(inventory);
        if (!menu.sound().isEmpty()) player.playSound(player.getLocation(), menu.sound(), 1, 1);
    }

    private void paintDecorations(Inventory inventory, Menu menu, Set<Integer> excludedSlots) {
        if (menu.fill() != null) {
            for (int slot = 0; slot < menu.size(); slot++) {
                if (!excludedSlots.contains(slot)) {
                    inventory.setItem(slot, menu.fill().clone());
                }
            }
        }
        if (menu.corner() != null) {
            for (int slot : cornerSlots(menu.size())) {
                inventory.setItem(slot, menu.corner().clone());
            }
        }
    }

    private ItemStack buildItemStack(
            MenuItem item, Player viewer, Map<String, String> placeholders) {
        return buildFromTemplate(
                item.material(),
                item.displayName(),
                item.lore(),
                item.itemFlags(),
                item.enchantmentGlintOverride(),
                viewer,
                placeholders);
    }

    /**
     * Builds one repeated dynamic-slot entry from a configured {@code templates.<type>} section,
     * substituting {@code placeholders} the same way a static item does - lets a {@link
     * MenuPainter} keep its entries' appearance in YAML instead of hardcoding {@code Component}s in
     * Java. Returns {@code null} if no template is configured for {@code type}, so a painter can
     * fall back to its own default appearance.
     */
    public ItemStack buildFromTemplate(
            Menu menu, String type, Player viewer, Map<String, String> placeholders) {
        ItemTemplate template = menu.templates().get(type);
        if (template == null) {
            return null;
        }
        return buildFromTemplate(
                template.material(),
                template.displayName(),
                template.lore(),
                template.itemFlags(),
                template.enchantmentGlintOverride(),
                viewer,
                placeholders);
    }

    public ItemStack buildFromTemplate(
            Material material,
            String displayName,
            List<String> lore,
            List<ItemFlag> itemFlags,
            Boolean enchantmentGlintOverride,
            Player viewer,
            Map<String, String> placeholders) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.displayName(
                        nonItalic(MenuPlaceholders.render(displayName, viewer, placeholders)));
            }
            meta.lore(
                    lore.stream()
                            .<Component>map(
                                    line ->
                                            nonItalic(
                                                    MenuPlaceholders.render(
                                                            line, viewer, placeholders)))
                            .toList());
            applyCosmetics(meta, itemFlags, enchantmentGlintOverride);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyCosmetics(
            ItemMeta meta, List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride) {
        if (!itemFlags.isEmpty()) {
            meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        }
        if (enchantmentGlintOverride != null)
            meta.setEnchantmentGlintOverride(enchantmentGlintOverride);
    }

    private static Component nonItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
