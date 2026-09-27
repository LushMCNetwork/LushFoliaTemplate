package com.playgamesinteractive.template.menu;

import com.playgamesinteractive.template.scheduler.FoliaTasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches clicks inside any {@link MenuManager}-built inventory. Ported from the original the
 * shared {@code MenuListener}, trimmed to the click command vocabulary this engine's DSL supports
 * out of the box: {@code close}, {@code message}, {@code command}, {@code console_command}, and
 * {@code open_menu}. Feature-specific tags (purchasing an upgrade, claiming a quest, editing the
 * visitor allowlist, ...) register via {@link #registerHandler} instead of being baked in here, so
 * this class never has to import the managers that own that behavior. Clicks inside a {@code
 * dynamic_slots} group are routed to that group's {@link MenuPainter} instead.
 */
public final class MenuListener implements Listener {

    private final FoliaTasks tasks;
    private final MenuManager menuManager;
    private final Map<String, MenuClickHandler> handlers = new LinkedHashMap<>();

    public MenuListener(MenuManager menuManager, FoliaTasks tasks) {
        this.tasks = tasks;
        this.menuManager = menuManager;
    }

    /**
     * Registers the handler for one custom {@code click_commands} tag, e.g. {@code toggle_open}.
     */
    public void registerHandler(String tag, MenuClickHandler handler) {
        handlers.put(tag, handler);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }
        if (event.getReason() != InventoryCloseEvent.Reason.PLAYER) {
            return;
        }
        if (menuManager.getMenu(holder.menu().id()) != holder.menu()) return;
        MenuHolder previous = holder.previous();
        if (previous == null || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        MenuClickHandler back = handlers.get("back");
        if (back != null) {
            tasks.entity(player, task -> back.handle(player, previous, previous.menu().id()));
            return;
        }
        tasks.entity(
                player,
                task ->
                        menuManager.openWithBack(
                                player,
                                previous.menu().id(),
                                previous.context(),
                                previous.placeholders(),
                                previous.previous()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (menuManager.getMenu(holder.menu().id()) != holder.menu()) return;
        var dynamicGroup = holder.menu().dynamicGroupFor(event.getSlot());
        if (dynamicGroup.isPresent()) {
            MenuPainter painter = menuManager.painter(dynamicGroup.get().getKey());
            if (painter != null) {
                List<Integer> slots = dynamicGroup.get().getValue();
                tasks.entity(
                        player,
                        task -> {
                            if (menuManager.getMenu(holder.menu().id()) == holder.menu()
                                    && player.getOpenInventory().getTopInventory().getHolder(false)
                                            == holder)
                                painter.onClick(
                                        player,
                                        holder,
                                        slots,
                                        slots.indexOf(event.getSlot()),
                                        event.getClick());
                        });
            }
            return;
        }

        MenuItem clicked = holder.menu().items().get(event.getSlot());
        if (clicked == null) {
            return;
        }
        if (clicked.permission() != null && !player.hasPermission(clicked.permission())) {
            return;
        }
        if (clicked.showIf() != null
                && !"true".equalsIgnoreCase(holder.placeholders().get(clicked.showIf()))) return;
        tasks.entity(
                player,
                task -> {
                    if (menuManager.getMenu(holder.menu().id()) != holder.menu()
                            || player.getOpenInventory().getTopInventory().getHolder(false)
                                    != holder) return;
                    if (clicked.permission() != null && !player.hasPermission(clicked.permission()))
                        return;
                    for (ClickCommand command : clicked.clickCommands())
                        runClickCommand(player, holder, command);
                });
    }

    private void runClickCommand(Player player, MenuHolder holder, ClickCommand command) {
        switch (command.type()) {
            case "close" -> player.closeInventory();
            case "message" ->
                    player.sendMessage(
                            MenuPlaceholders.render(
                                    command.argument(), player, holder.placeholders()));
            case "command" -> player.performCommand(sanitize(command.argument()));
            case "console_command" -> {
                String line = sanitize(command.argument()).replace("%player%", player.getName());
                tasks.global(task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line));
            }
            case "open_menu" -> {
                MenuClickHandler opener = handlers.get("open_menu");
                if (opener != null) opener.handle(player, holder, command.argument());
                else
                    menuManager.openWithBack(
                            player,
                            command.argument(),
                            holder.context(),
                            holder.placeholders(),
                            holder);
            }
            default -> {
                MenuClickHandler handler = handlers.get(command.type());
                if (handler != null) {
                    handler.handle(player, holder, command.argument());
                }
                // else: unrecognized tag - already warned about malformed syntax at load time
            }
        }
    }

    private static String sanitize(String commandLine) {
        return commandLine.replaceAll("[\\n\\r\\t\\x00-\\x1F]", " ").trim();
    }
}
