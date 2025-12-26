# SBPCSpecials

SBPCSpecials is a config-driven extension for the SBPC progression plugin that unlocks, speeds up, or auto-completes sections when players hit custom milestones. All behaviour is defined in `config.yml`, so you can add or tune specials without recompiling.

## Key Features
- **Config-first specials:** Declare specials under `specials:` with trigger, section conditions, rewards, scope, and messages. The plugin indexes your config at startup and listens for matching events—no hardcoded switch statements.【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L1-L118】【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L200-L282】
- **Multiple trigger types:** React to mob kills (`ENTITY_DEATH`), item pickups (`ENTITY_PICKUP`), SBPC entry unlocks (`UNLOCK_ENTRY`), or potion effects. Each trigger can optionally be marked `command-activatable` so staff can fire it manually.【F:src/config.yml†L7-L38】【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L118-L199】
- **Section-aware rewards:** Gate specials by section type or ID ranges, then award speed boosts, time skips, or instant completion when conditions are met.【F:src/config.yml†L13-L37】【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L200-L244】
- **Per-player persistence:** Speed bonuses and completion flags are saved under `plugins/SBPCSpecials/Players/<uuid>.yml` and re-applied on join, keeping progress consistent across restarts.【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L284-L382】
- **Admin & player controls:** The `/specials` command lets authorized users activate or remove `command-activatable` specials while enforcing per-player/per-server limits.【F:src/plugin.yml†L9-L17】【F:src/me/BaddCamden/SBPCSpecials/SBPCSpecialsPlugin.java†L39-L70】

## Installation
1. Build the plugin with Maven:
   ```bash
   mvn package
   ```
   Copy the generated JAR from `target/` into your server's `plugins/` folder.
2. Ensure the required dependencies (`SBPC`, `SessionLibrary`, and the matching Spigot API version) are present on your server.【F:pom.xml†L6-L40】
3. Start the server once to generate the default `config.yml` and data folders.

## Usage
### Command
`/specials <activate|remove> <special-id>`

- `activate` fires a `command-activatable` special if the player currently meets its section constraints.
- `remove` clears the special and its bonuses for that player when the `sbpcspecials.command.remove` permission is granted.
- Permissions:
  - `sbpcspecials.command.activate` (default: OP)
  - `sbpcspecials.command.remove` (default: false)

### Event-driven activation
Most specials are triggered automatically by gameplay events you define. Examples from `config.yml`:
- **Instant section completion on item pickup:** Picking up a Pale Oak Log auto-completes the *Wood Tools + Leather Armor* section once per player. Useful for reward drops or admin grants.
  ```yaml
  wood_leather_pale_log_auto:
    trigger:
      type: ENTITY_PICKUP
      entity-type: PLAYER
      item-type: PALE_OAK_LOG
      command-activatable: true
    section:
      require-type: "SPECIAL"
      allowed-sections: [wood_leather]
    reward:
      auto-complete-section: true
    scope:
      once-per-player: true
  ```
- **Progress acceleration on mob kills:** Any mob kill in the *Meats* section applies the default time skip repeatedly, while an Enderman kill instantly completes the section.
  ```yaml
  meats_enderman_kill_auto:
    trigger:
      type: ENTITY_DEATH
      entity-type: ENDERMAN
      killer-must-be-player: true
      command-activatable: true
    section:
      allowed-sections: [meats]
    reward:
      auto-complete-section: true
    scope:
      once-per-player: true

  meats_mob_kill_bonus:
    trigger:
      type: ENTITY_DEATH
      killer-must-be-player: true
    section:
      allowed-sections: [meats]
    reward:
      default-time-skip: true
  ```
- **Potion-gated boosts:** Grant massive progress speed when a player gains Haste II and unlocks a hidden SBPC entry.
  ```yaml
  enchants_tier4_haste2_boost:
    trigger:
      type: UNLOCK_ENTRY
      entry-id: "haste2_special"
    potion-requirement:
      effect: HASTE
      min-amplifier: 1
    section:
      allowed-sections: [enchants_tier4]
    reward:
      speed-bonus-percent: 800.0
    scope:
      once-per-player: true
  ```

## Tips for Custom Specials
- Set `applies-to-all-sections: true` to make a special global; otherwise list specific `allowed-sections`.
- Combine `speed-bonus-percent` with `speed-bonus-skip-seconds` or `session-time-skip-seconds` to stack temporary and permanent time reductions.
- Use `once-per-server: true` for rare items (e.g., Dragon Egg) to prevent repeated activation across players.
- Leave `broadcast` empty to disable global messages for routine boosts.

With the fully declarative config and `/specials` controls, SBPCSpecials lets you craft tailored progression shortcuts, catch-up mechanics, or celebration rewards without touching code.
