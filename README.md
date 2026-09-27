# LushFoliaTemplate 2

A working starter for new LushMC **Folia** plugins, targeting **Java 21** and **Folia API 1.21.11**. It replaces the original empty entry point with MiniMessage presentation, safe scheduler dispatch, commands, protected configurable menus, immutable settings, language fallback, reload validation and automated tests.

This template requires **LushMenus 1.0.0** at runtime, using the same managed-inventory integration as LushRaft-v2. It has no economy, database or licensing dependency. Adventure and MiniMessage come from the server API and are not shaded into the plugin. There is no legacy color translator, legacy Bukkit scheduler, string-based item presentation or compatibility migration layer.

## Copy and rename

1. Copy this directory without `target/` or an existing `.git/` directory.
2. Change the artifact/name/description in `pom.xml` and identity in `plugin.yml`.
3. Rename `com.playgamesinteractive.template`, `TemplatePlugin`, the `/template` command and `lushtemplate.*` permission nodes. Update references in Java, tests, YAML, docs and the required `template_menu` ID if renaming it.
4. Replace `starter/StarterListener` and `command/TemplateCommand` with your feature. Keep lifecycle setup, reload publication and shutdown in the main plugin.
5. Run `mvn clean verify`; install the resulting JAR on a staging Folia server.

Build/install LushMenus into your Maven repository before building a fresh copy of the template:

```sh
mvn -f /path/to/LushMenus/pom.xml clean install
mvn clean verify
```

The dependency is `com.playgamesinteractive:LushMenus:1.0.0:api` with `provided` scope. Install the **full** `LushMenus-1.0.0.jar` on the server alongside the template; the `-api.jar` is for compilation. `plugin.yml` declares `depend: [LushMenus]`, so the server loads LushMenus first. Never shade either its API or implementation into a consumer.

The bundled example builds to `target/LushFoliaTemplate-2.0.0.jar`. Maven 3.9+ is recommended. The server must expose Folia's regionized runtime; ordinary Paper is intentionally rejected.

## Included example

- `/template` or `/template menu` opens the starter menu (`lushtemplate.use`, granted by default).
- `/template echo <text>` demonstrates safe text placeholders. Player-supplied MiniMessage tags remain literal text.
- `/template reload` validates configuration, language and every menu before publishing replacements (`lushtemplate.admin`, operators by default).
- An optional join greeting demonstrates player-scheduler dispatch. Enable `starter.welcome-message` in `config.yml`.

Menu/reload accept no extra arguments. Completion hides routes the sender cannot use. Reload runs file reads asynchronously and constructs detached item metadata on the global scheduler. Invalid candidates keep the previous configuration active, and a successful reload closes old menus. Concurrent reload requests receive a busy message.

## Structure

```text
src/main/java/com/playgamesinteractive/template/
  TemplatePlugin.java         Lifecycle assembly and reload coordination
  command/                    Shared router and small starter command
  config/                     Strict YAML reads, default healing, immutable settings
  lang/                       Server-wide message catalog and recipient dispatch
  menu/                       Records, loader, holders, painters and click handling
  scheduler/                  Folia owner dispatch and tracked shutdown cancellation
  starter/                    Replaceable example feature listener
  text/                       MiniMessage components and safe data placeholders
src/main/resources/
  config.yml
  language/en_US.yml
  menus/template_menu.yml
  plugin.yml
src/test/java/                 Presentation, scheduler, routing and config/layout tests
```

Use four-space indentation, explicit imports, constructor injection and feature packages. Share immutable snapshots across threads; mutable world/entity state remains on its owner scheduler. See [development guidance](docs/DEVELOPMENT.md) and the [staging checklist](docs/STAGING.md).

## Presentation and menus

Use MiniMessage and Adventure `Component`s throughout: `<#00f396>`, `<bold>`, `<underlined>`, `<gray>` and `<white>`. Legacy `&`/section-sign formatting is rejected in configured text. Internal `{token}` placeholders become unparsed MiniMessage values, preventing inserted player text from injecting colors or click events. Items explicitly disable implicit italics.

The example follows LushMC house colors, status symbols, Information lore, underlined click instructions, white glass fill and blue corner clusters. Decoration tooltips are hidden. Clickable items have a real action; the read-only guide uses a Quick Access command instead of a fake click instruction.

As in LushRaft-v2, this plugin retains its domain holders, YAML layouts, painters and actions. Inventories are created through `SharedMenus.createInventory` with the display policy. Listeners register through `SharedMenus.registerEvents`; do not also register them directly with Bukkit, or managed clicks can be delivered twice. The shared service preserves non-menu events, handles session generations, click throttling and prohibited inventory movement, and publishes its menu events. Successful reloads call `SharedMenus.invalidate(this)` after candidate validation. LushMenus handles owner-disable session cleanup.

Menu YAML supports `display_name`, `lore`, `slot`/`slots` (including ranges), permissions, `show_if`, `click_commands`, `open_menu`, item flags and glint override. Generic tags are `[close]`, `[message]`, `[command]`, `[console_command]` and `[open_menu]`; register custom handlers for domain actions. The example adds `[greet]`. Player actions run on the entity scheduler; console commands move to the global scheduler.

`fill` paints unused slots. `corner` uses the fixed clusters `0, 1, 9, size-10, size-2, size-1`. Static/dynamic content may not overlap each other or corners. Dynamic lists use `dynamic_slots.<type>`, a registered `MenuPainter` and `templates.<type>`. Opened child menus remember the previous holder, and Escape returns to it. Both mouse buttons dispatch configured actions; inventory transfers and drags are cancelled.

Startup extracts absent bundled files and adds missing leaf keys without replacing configured values. A first extension write creates a `.bak` backup. Malformed YAML or scalar values where sections are expected are rejected without rewriting the file. Reload never heals or rewrites files. Partial `language/<name>.yml` catalogs fall back to English.

## Build choices

Only `plugin.yml` is Maven-filtered; MiniMessage/config/menu resources remain unchanged. Runtime API dependencies use `provided` scope. The starter does not run Maven Shade when it has nothing to bundle. If your plugin adds runtime libraries, add an explicit shade configuration, exclude signature metadata, preserve service descriptors and **never relocate native-backed packages** such as sqlite-jdbc: their JNI symbols cannot follow Java package relocation.

Automated tests verify component rendering, literal placeholder values, legacy-code rejection, strict configuration loading, translation fallback, healing/backups, immutable snapshots, menu collisions, permission routing, entity retirement and task shutdown. They do not replace live-server Folia and inventory verification.
