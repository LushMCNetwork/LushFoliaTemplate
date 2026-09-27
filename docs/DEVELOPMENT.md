# Developing from the template

## Scheduler ownership

`FoliaTasks` routes work to the correct scheduler and tracks returned tasks for shutdown. All engine/example scheduling goes through it; `close()` cancels tracked tasks and prevents queued callbacks from running. A callback already executing must finish safely; cancellation cannot interrupt it or make cross-region access safe. Entity retirement returns no task; never rely on that callback completing a required database transaction.

| Work | Dispatcher |
| --- | --- |
| Player inventory, messages, sounds, moving entity state | `tasks.entity(entity, callback)` |
| Player cooldown/timer that follows teleports | `entityLater` / `entityTimer` |
| Block/location work at known coordinates | `region` / `regionLater` / `regionTimer` |
| Global bookkeeping or console command dispatch | `global` |
| File/database/network I/O or detached immutable calculations | `async` / `asyncTimer` |

Entity and region delays/periods use ticks and must be at least one. Async intervals use the supplied `TimeUnit` and must be positive. The entity scheduler follows an entity as its owner changes. A region timer remains pinned to its coordinate.

Resolve an online player reference, then schedule against that player before reading their inventory, location or other live state. Do not resolve an arbitrary world entity through `Bukkit.getEntity` or scan world entities from an async/global callback. Holding a reference does not grant ownership. An entity event generally owns the actor; a different target still needs its own scheduler dispatch. For area operations, check `Bukkit.isOwnedByCurrentRegion` and coordinate cross-region work rather than assuming neighboring chunks share an owner.

Plugin enable/disable is not a blanket exemption for live world access. Keep lifecycle methods focused on assembly, detached configuration and task registration. Any work that touches a live entity/block should explicitly use its owner scheduler. Avoid blocking a region/global thread with I/O, `Future.get`, `join` or sleeps.

## Adding a feature

Give each feature its own package, injected manager/listener and immutable data records. Register listeners in the plugin class with `SharedMenus.registerEvents(listener, plugin)`, including gameplay listeners, matching LushRaft-v2. The bridge routes managed inventory events and preserves ordinary gameplay/non-managed inventory events. Do not register the same listener through both APIs. Route commands through `CommandRouter`; grant/check the actual action permission, including clicks and target-player operations. Never grant trust based on display names or lore; use PDC for plugin-owned item identity.

Register a `MenuPainter` for named dynamic groups instead of mixing feature logic into the generic loader. Keep item appearance in YAML templates. Use immutable context records in `MenuHolder`; do not move a live profile/inventory/entity through async work. If a feature fetches data asynchronously, return to the player scheduler and reject results after the player closes, changes menus or the configuration revision changes.

The generic back stack restores the previous holder's snapshot. A feature with changing data should register its own `back`/`open_menu` handler to reload fresh domain data while preserving the intended navigation target. Avoid making pagination or a toggle refresh add another history entry.

## Safe text

Use `TextStyle.render(template, values)` or `LangManager.get/send` for external data. Values are unparsed components; do not concatenate player text into MiniMessage markup. Only trusted configuration may define rich tags. Keep configured messages in the language catalog; `LangManager.send` safely dispatches to the recipient, while `get` returns a component for callers already on an appropriate owner thread.

Use the house palette: success `#00f396`, denial `#ff1155`, headings/clicks `#fdf700`, quick access/charges `#ff7200`, blue `#00a4fe`, purple `#9d73ff`, magenta `#ff3cfe`. Preserve rank colors when a future plugin integrates an established rank system. Allowed status/action icons include ✔ ✘ ⚠ ⌚ ⚡ ✦ ▶ ◀ ⚓ ♛ ⚔. Give clickable lore an Information section and final underlined CLICK instruction; read-only entries use a real quick-access command.

## Persistence and integrations

LushMenus is a required integration. Keep the API-classifier Maven dependency `provided` and the `depend: [LushMenus]` runtime descriptor. Use `SharedMenus.createInventory` for every domain GUI rather than `Bukkit.createInventory`. The default display policy blocks native movement; editable storage GUIs must explicitly select an `InventoryPolicy` and own item-return/transaction behavior. Validate reload candidates first, then invalidate the owner's shared sessions before publishing the replacement menus. LushMenus cleans up its managed sessions when the owner disables.

This starter has no database. Add one only when your feature needs durable data, with a dedicated bounded executor, immutable snapshots and explicit error handling. Register public integration interfaces through Bukkit services, keep API dependencies `provided`, and leave optional plugin hooks behind dependency checks. Do not add server-specific economy, claim, asset or licensing dependencies to the reusable base.
