package com.playgamesinteractive.template.command;

import com.playgamesinteractive.template.lang.LangManager;
import com.playgamesinteractive.template.menu.MenuManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

/** A working example of shared routing, permissions, protected menus and safe external text. */
public final class TemplateCommand extends CommandRouter {
    private final LangManager language;
    private final MenuManager menus;

    public TemplateCommand(
            LangManager language, MenuManager menus, Consumer<CommandSender> reload) {
        this.language = language;
        this.menus = menus;
        register(
                "menu",
                "lushtemplate.use",
                (sender, label, args) ->
                        args.length == 0 ? open(sender) : usage(sender, "/template menu"),
                null);
        register(
                "reload",
                "lushtemplate.admin",
                (sender, label, args) -> {
                    if (args.length != 0) return usage(sender, "/template reload");
                    reload.accept(sender);
                    return true;
                },
                null);
        register(
                "echo",
                "lushtemplate.use",
                (sender, label, args) -> {
                    if (args.length == 0) language.send(sender, "command.echo-usage");
                    else language.send(sender, "command.echo", "text", String.join(" ", args));
                    return true;
                },
                null);
    }

    @Override
    protected boolean executeDefault(CommandSender sender, String label) {
        return open(sender);
    }

    @Override
    protected boolean unknown(CommandSender sender, String label, String argument) {
        language.send(sender, "command.help");
        return true;
    }

    @Override
    protected void denied(CommandSender sender) {
        language.send(sender, "command.no-permission");
    }

    private boolean usage(CommandSender sender, String usage) {
        language.send(sender, "command.usage", "usage", usage);
        return true;
    }

    private boolean open(CommandSender sender) {
        if (!sender.hasPermission("lushtemplate.use")) {
            denied(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            language.send(sender, "command.player-only");
            return true;
        }
        menus.open(player, "template_menu", null, Map.of("plugin", "LushFoliaTemplate"));
        return true;
    }
}
