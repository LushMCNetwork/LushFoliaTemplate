# LushFoliaTemplate

Starting point for a new LushMC plugin. Fork or copy this repo instead of setting
up `pom.xml`/`plugin.yml` from scratch.

## Getting started

1. Rename the repo, `<artifactId>`/`<name>` in `pom.xml`, and `name`/`main` in
   `plugin.yml`.
2. Rename the `com.playgamesinteractive.template` package (and `TemplatePlugin`)
   to match your plugin.
3. `mvn package` - the shaded jar lands in `target/`.

## Folia safety rules

Folia runs each loaded region of the world on its own thread instead of one
global tick loop. These are the rules that keep a plugin from crashing or
corrupting state under that model - learned the hard way migrating LushRaft.

- **No `BukkitRunnable` / `Bukkit.getScheduler()`.** Use one of Folia's three
  scheduler categories instead:
  - `Bukkit.getGlobalRegionScheduler()` - pure bookkeeping only (config
    reload, DB loads, console commands). **Never touch a block or entity from
    here** - it isn't pinned to any region.
  - `Bukkit.getRegionScheduler().run(plugin, location, task -> ...)` -
    location-pinned work (block edits, area scans) at a *known* coordinate.
  - `entity.getScheduler().run(plugin, task -> ...)` - anything bound to an
    already-spawned entity (AI ticks, per-player timers). Prefer this
    whenever you already hold a live entity reference; it auto-cancels if the
    entity dies.
- **Never call `Bukkit.getEntity(uuid)` (or `World#getEntities()`) from an
  arbitrary thread**, including `Bukkit.getAsyncScheduler()` timers directly.
  It's a real chunk-lookup and throws `AsyncCatcher`/`IllegalStateException`
  off a region thread. If you only have a UUID and a rough location (e.g. "the
  mob belongs to this island"), resolve it via
  `Bukkit.getRegionScheduler().run(...)` pinned to that location and
  `world.getNearbyEntities(...)`, not a blind global lookup.
- **`Bukkit.getPlayer(uuid)` and `Bukkit.getOnlinePlayers()` are safe reads
  from any thread** - they're a simple online-player-list lookup, not a
  chunk/entity lookup. But once you have the `Player` object, still hop onto
  `player.getScheduler().run(...)` before touching their inventory, location,
  or sending messages/sounds - they may be ticking on a different region
  thread than whatever code resolved them.
- **Watch cross-entity access.** A method already running on entity/region A's
  thread (e.g. a per-island tick) must not directly touch entity/region B
  (e.g. a random online player, or an admin command's target) without its own
  scheduler hop first. This is the subtlest and easiest class of bug to miss
  - grep for `Bukkit.getPlayer(` / `Bukkit.getEntity(` inside any tick/handler
  method and check whether the result is touched directly or re-dispatched.
- **All Folia scheduler delay/period values must be `>= 1`** - no more
  `0L` immediate-next-tick pattern.
- **Shared mutable state needs concurrent-safe collections** (`ConcurrentHashMap`,
  `CopyOnWriteArrayList`, etc) or `volatile`/synchronized fields - two islands
  (or a live game action vs. an admin reload) can now genuinely run
  concurrently instead of being serialized by a single tick thread.
- **`onEnable`/`onDisable` are safe for direct entity/world access without a
  scheduler hop** - regions haven't started ticking yet in `onEnable`, and
  they've already been halted by `onDisable`, so there's no concurrent region
  activity to race with at either end of the plugin lifecycle.
- **Don't relocate native-backed dependencies** (sqlite-jdbc, snappy, etc) in
  the shade plugin - see the comment in `pom.xml`. Relocating the Java package
  breaks the bundled native library's JNI symbol binding.
