package com.playgamesinteractive.template.text;

import static org.junit.jupiter.api.Assertions.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;

import java.util.Map;

class TextStyleTest {
    @Test
    void miniMessageRendersHexAndFormattingWithoutImplicitItalics() {
        var text = TextStyle.component("<#00f396><bold>Ready</bold>");
        assertEquals(TextColor.fromHexString("#00f396"), text.color());
        assertEquals(TextDecoration.State.TRUE, text.decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.FALSE, text.decoration(TextDecoration.ITALIC));
        assertEquals("Ready", PlainTextComponentSerializer.plainText().serialize(text));
    }

    @Test
    void externalTextCannotInjectClickEventsColorsOrNewPlaceholders() {
        String input = "<click:run_command:'/op x'><red>{player}</red></click> &a";
        Component result =
                TextStyle.render("<white>{text}", Map.of("text", input, "player", "Someone"));
        assertEquals(input, PlainTextComponentSerializer.plainText().serialize(result));
        assertNoClicks(result);
    }

    private void assertNoClicks(Component component) {
        assertNull(component.clickEvent());
        component.children().forEach(this::assertNoClicks);
    }

    @Test
    void configuredLegacyCodesAreRejectedRatherThanTranslated() {
        for (String value : new String[] {"&aLegacy", "&#00f396Legacy", "§cLegacy"})
            assertThrows(IllegalArgumentException.class, () -> TextStyle.component(value));
    }
}
