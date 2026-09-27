package com.playgamesinteractive.template.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;
import java.util.regex.Pattern;

/** MiniMessage-only presentation. External values are inserted as text, never parsed as markup. */
public final class TextStyle {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern LEGACY = Pattern.compile("(?i)(?:&[0-9a-fk-or]|&#[0-9a-f]{6}|§.)");

    private TextStyle() {}

    public static Component component(String template) {
        return render(template, Map.of());
    }

    public static Component render(String template, Map<String, String> values) {
        validate(template);
        TagResolver.Builder tags = TagResolver.builder();
        String resolved = template;
        for (var entry : values.entrySet()) {
            String key = entry.getKey();
            if (!key.matches("[a-z][a-z0-9_-]*"))
                throw new IllegalArgumentException("Invalid placeholder name: " + key);
            resolved = resolved.replace("{" + key + "}", "<" + key + ">");
            tags.resolver(Placeholder.unparsed(key, entry.getValue()));
        }
        return MINI.deserialize(resolved, tags.build())
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static void validate(String template) {
        if (LEGACY.matcher(template).find())
            throw new IllegalArgumentException(
                    "Use MiniMessage tags; legacy color codes are not supported");
    }
}
