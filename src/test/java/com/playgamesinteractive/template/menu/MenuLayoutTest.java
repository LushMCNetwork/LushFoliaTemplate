package com.playgamesinteractive.template.menu;

import static org.junit.jupiter.api.Assertions.*;

import com.playgamesinteractive.template.config.ResourceFiles;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MenuLayoutTest {
    @Test
    void starterMenuUsesDisjointHouseStyleLayout() {
        var yaml = ResourceFiles.read(new File("src/main/resources/menus/template_menu.yml"));
        assertDoesNotThrow(() -> MenuLayout.validate(yaml));
        assertEquals("WHITE_STAINED_GLASS_PANE", yaml.getString("fill.material"));
        assertEquals("LIGHT_BLUE_STAINED_GLASS_PANE", yaml.getString("corner.material"));
        assertEquals(List.of(0, 1, 9, 17, 25, 26), MenuLayout.cornerSlots(27));
    }

    @Test
    void rangeAndDynamicCollisionsFailBeforeMenusArePublished() {
        var yaml = ResourceFiles.read(new File("src/main/resources/menus/template_menu.yml"));
        yaml.set("dynamic_slots.players", List.of("10-12"));
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.validate(yaml));
        yaml.set("dynamic_slots.players", List.of(0));
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.validate(yaml));
        yaml.set("dynamic_slots.players", List.of(27));
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.validate(yaml));
        assertEquals(List.of(10, 11, 12, 13), MenuLayout.slots(List.of(10, "11-13")));
    }

    @Test
    void dynamicSlotSnapshotDoesNotExposeMutableSourceLists() {
        var source = new ArrayList<>(List.of(10, 11));
        var menu =
                new Menu(
                        "test",
                        "Test",
                        27,
                        null,
                        null,
                        Map.of(),
                        null,
                        "",
                        Map.of("players", source),
                        Map.of());
        source.add(12);
        assertEquals(List.of(10, 11), menu.dynamicSlots().get("players"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> menu.dynamicSlots().get("players").add(12));
    }
}
