package com.playgamesinteractive.template.menu;

/** One {@code [tag] argument} entry from an item's {@code click_commands} list. */
public record ClickCommand(String type, String argument) {}
