# Live-server verification

The unit tests use API interfaces and local YAML. No live server is included with this template.

1. Build with Java 21 and `mvn clean verify`. Install the full LushMenus runtime JAR alongside the template and start Folia 1.21.11; verify resources extract and the startup log has no errors. With LushMenus missing, the server should reject the template because of its hard dependency. With LushMenus installed, ordinary Paper should log the Folia requirement and disable the plugin cleanly.
2. Run `/template`, click the confirmation entry with either mouse button, and close the menu. Check hex colors, non-italic names/lore, white fill, blue corner clusters, hidden decoration tooltips and sound.
3. Try shift-clicking, dragging, hotbar swaps and double-clicking; no menu items may enter a player's inventory. Verify one greeting per accepted click, shared click throttling, and the template handlers in LushMenus' registry; listener registration must not duplicate clicks. Add a second menu using `[open_menu]`; Escape must return to its parent.
4. Deny `lushtemplate.use` and `lushtemplate.admin`; check command execution, completion and menu permissions. Extra arguments after `menu` or `reload` should show usage.
5. Echo `<click:run_command:'/op someone'><red>test</red></click>`; it must display as literal text without a click action. Legacy color codes in configuration should reject startup/reload rather than translate.
6. Change a message and reload. Break YAML, a menu slot, a material or an item flag; reload should retain the previous working snapshot. Try two reloads together, and confirm old open menus close after a successful reload through LushMenus session invalidation. Disable the template and confirm its managed sessions close.
7. Enable the join greeting, create a partial translation, and verify fallback messages. Delete a bundled leaf key before a restart; verify the key heals, existing values remain and the first write creates a backup.
8. Extend the example with entity/region timers, teleport across regions, retire the entity, then stop the server while tasks are pending. Check for owner-thread errors, stale callbacks and task leaks. Repeat with the actual feature's async I/O before deploying a derived plugin.
