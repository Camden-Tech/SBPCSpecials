package me.BaddCamden.SBPCSpecials;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.BaddCamden.SBPC.api.SbpcAPI;
import me.BaddCamden.SBPC.events.UnlockItemEvent;
import me.BaddCamden.SBPC.progress.SectionDefinition;

/**
 * Config-driven specials implementation for SBPC.
 *
 * - All specials are declared in this plugin's config under "specials".
 * - onEntityDeath / onEntityPickup / UnlockItemEvent are routed through hash maps
 *   built from config, instead of hardcoded switch logic.
 * - Per-player speed bonuses are stored in Players/<uuid>.yml.
 * - Section checks are done via section type (SectionDefinition.getType()) and index.
 */
public class SBPCSpecialsPlugin extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    // Config-driven specials indexes
    // ------------------------------------------------------------------------

    private final Map<String, SpecialDefinition> specialsById = new HashMap<>();
    private final Map<EntityType, List<SpecialDefinition>> deathSpecials = new HashMap<>();
    private final Map<Material, List<SpecialDefinition>> pickupSpecials = new HashMap<>();
    private final Map<String, List<SpecialDefinition>> unlockEntrySpecials = new HashMap<>();

    // ------------------------------------------------------------------------
    // Per-player & server state
    // ------------------------------------------------------------------------

    private final Map<UUID, PlayerSpecialData> playerData = new HashMap<>();
    private final Set<String> completedSpecialsServerWide = new HashSet<>();

    private File playersFolder;
    private File specialsDataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        this.playersFolder = new File(getDataFolder(), "Players");
        this.specialsDataFile = new File(getDataFolder(), "specials-data.yml");

        loadSpecialsFromConfig();
        loadPlayerData();
        loadGlobalSpecialsData();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Initialize hook API
        SpecialsAPI.init(this);


        getLogger().info("SBPCSpecials enabled. Specials are now config-driven.");
    }

    @Override
    public void onDisable() {
        savePlayerData();
        saveGlobalSpecialsData();
        getLogger().info("SBPCSpecials disabled.");
    }

    // ------------------------------------------------------------------------
    // Config loading
    // ------------------------------------------------------------------------

    private void loadSpecialsFromConfig() {
        specialsById.clear();
        deathSpecials.clear();
        pickupSpecials.clear();
        unlockEntrySpecials.clear();

        ConfigurationSection root = getConfig().getConfigurationSection("specials");
        if (root == null) {
            getLogger().warning("No specials defined in SBPCSpecials config (specials section is missing).");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            // --- Trigger ---
            ConfigurationSection trigSec = sec.getConfigurationSection("trigger");
            if (trigSec == null) {
                getLogger().warning("Special " + id + " is missing trigger section.");
                continue;
            }

            TriggerType triggerType;
            try {
                triggerType = TriggerType.valueOf(
                        trigSec.getString("type", "UNLOCK_ENTRY").toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Special " + id + " has invalid trigger type: " + trigSec.getString("type"));
                continue;
            }

            EntityType entityType = null;
            boolean killerMustBePlayer = false;
            Material itemType = null;
            String entryId = null;

            if (triggerType == TriggerType.ENTITY_DEATH || triggerType == TriggerType.ENTITY_PICKUP) {
                String entName = trigSec.getString("entity-type", null);
                if (entName != null) {
                    try {
                        entityType = EntityType.valueOf(entName.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        getLogger().warning("Special " + id + " has invalid entity-type: " + entName);
                    }
                }
            }

            if (triggerType == TriggerType.ENTITY_DEATH) {
                killerMustBePlayer = trigSec.getBoolean("killer-must-be-player", true);
            }

            if (triggerType == TriggerType.ENTITY_PICKUP) {
                String itemName = trigSec.getString("item-type", null);
                if (itemName != null) {
                    try {
                        itemType = Material.valueOf(itemName.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        getLogger().warning("Special " + id + " has invalid item-type: " + itemName);
                    }
                }
            }

            if (triggerType == TriggerType.UNLOCK_ENTRY) {
                entryId = trigSec.getString("entry-id", null);
            }

            SpecialDefinition.TriggerDefinition triggerDef = new SpecialDefinition.TriggerDefinition(
                    triggerType,
                    entityType,
                    killerMustBePlayer,
                    itemType,
                    entryId
            );

            // --- Section condition ---
            ConfigurationSection secCond = sec.getConfigurationSection("section");
            String requireType = secCond != null ? secCond.getString("require-type", null) : null;
            Integer minIndex = (secCond != null && secCond.contains("min-index"))
                    ? secCond.getInt("min-index") : null;
            Integer maxIndex = (secCond != null && secCond.contains("max-index"))
                    ? secCond.getInt("max-index") : null;

            SpecialDefinition.SectionCondition sectionCondition =
                    new SpecialDefinition.SectionCondition(requireType, minIndex, maxIndex);

            // --- Reward ---
            ConfigurationSection rewardSec = sec.getConfigurationSection("reward");
            double speedPercent = rewardSec != null ? rewardSec.getDouble("speed-bonus-percent", 0.0) : 0.0;
            int skipSeconds = rewardSec != null ? rewardSec.getInt("speed-bonus-skip-seconds", 0) : 0;
            int sessionSkip = rewardSec != null ? rewardSec.getInt("session-time-skip-seconds", 0) : 0;
            boolean autoCompleteSection = rewardSec != null && rewardSec.getBoolean("auto-complete-section", false);

            SpecialDefinition.RewardDefinition rewardDef =
                    new SpecialDefinition.RewardDefinition(speedPercent, skipSeconds, sessionSkip, autoCompleteSection);

            // --- Scope ---
            ConfigurationSection scopeSec = sec.getConfigurationSection("scope");
            boolean oncePerPlayer = scopeSec != null && scopeSec.getBoolean("once-per-player", true);
            boolean oncePerServer = scopeSec != null && scopeSec.getBoolean("once-per-server", false);

            SpecialDefinition.ScopeDefinition scopeDef =
                    new SpecialDefinition.ScopeDefinition(oncePerPlayer, oncePerServer);

            // --- Messages ---
            ConfigurationSection msgSec = sec.getConfigurationSection("messages");
            String playerMsg = msgSec != null ? msgSec.getString("player", "") : "";
            String broadcastMsg = msgSec != null ? msgSec.getString("broadcast", "") : "";

            SpecialDefinition.MessagesDefinition msgDef =
                    new SpecialDefinition.MessagesDefinition(playerMsg, broadcastMsg);

            SpecialDefinition def = new SpecialDefinition(
                    id,
                    triggerDef,
                    sectionCondition,
                    rewardDef,
                    scopeDef,
                    msgDef
            );
            specialsById.put(id, def);

            // Index by trigger type
            switch (triggerType) {
                case ENTITY_DEATH:
                    if (entityType != null) {
                        deathSpecials.computeIfAbsent(entityType, k -> new ArrayList<>()).add(def);
                    }
                    break;
                case ENTITY_PICKUP:
                    if (itemType != null) {
                        pickupSpecials.computeIfAbsent(itemType, k -> new ArrayList<>()).add(def);
                    }
                    break;
                case UNLOCK_ENTRY:
                    if (entryId != null && !entryId.isEmpty()) {
                        unlockEntrySpecials.computeIfAbsent(entryId, k -> new ArrayList<>()).add(def);
                    }
                    break;
            }
        }

        getLogger().info("Loaded " + specialsById.size() + " specials from config.");
    }

    // ------------------------------------------------------------------------
    // Per-player persistence
    // ------------------------------------------------------------------------

    private void loadPlayerData() {
        playerData.clear();

        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            return;
        }

        File[] files = playersFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            String uuidStr = (dot == -1) ? name : name.substring(0, dot);

            try {
                UUID uuid = UUID.fromString(uuidStr);
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                PlayerSpecialData data = new PlayerSpecialData();

                ConfigurationSection bonusesSec = cfg.getConfigurationSection("speed-bonuses");
                if (bonusesSec != null) {
                    for (String specialId : bonusesSec.getKeys(false)) {
                        double percent = bonusesSec.getDouble(specialId + ".percent", 0.0);
                        int skip = bonusesSec.getInt(specialId + ".skip-seconds", 0);
                        data.addOrUpdateBonus(specialId, percent, skip);
                    }
                }

                List<String> completed = cfg.getStringList("completed-specials");
                for (String s : completed) {
                    data.markCompleted(s);
                }

                playerData.put(uuid, data);
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid player UUID in Players folder: " + name);
            }
        }
    }

    private void savePlayerData() {
        if (!playersFolder.exists() && !playersFolder.mkdirs()) {
            getLogger().warning("Could not create Players folder at " + playersFolder.getPath());
            return;
        }

        for (Map.Entry<UUID, PlayerSpecialData> entry : playerData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerSpecialData data = entry.getValue();

            File file = new File(playersFolder, uuid.toString() + ".yml");
            YamlConfiguration cfg = new YamlConfiguration();

            ConfigurationSection bonusesSec = cfg.createSection("speed-bonuses");
            for (Map.Entry<String, PlayerSpecialData.SpeedBonus> bonusEntry :
                    data.getBonusesBySpecialId().entrySet()) {
                String specialId = bonusEntry.getKey();
                PlayerSpecialData.SpeedBonus bonus = bonusEntry.getValue();
                bonusesSec.set(specialId + ".percent", bonus.getPercent());
                bonusesSec.set(specialId + ".skip-seconds", bonus.getSkipSeconds());
            }

            cfg.set("completed-specials", new ArrayList<>(data.getCompletedSpecials()));

            try {
                cfg.save(file);
            } catch (IOException ex) {
                getLogger().warning("Could not save specials data for " + uuid + ": " + ex.getMessage());
            }
        }
    }

    private void loadGlobalSpecialsData() {
        completedSpecialsServerWide.clear();

        if (!specialsDataFile.exists()) {
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(specialsDataFile);
        List<String> list = cfg.getStringList("completed-specials-server");
        completedSpecialsServerWide.addAll(list);
    }

    private void saveGlobalSpecialsData() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("completed-specials-server", new ArrayList<>(completedSpecialsServerWide));
        try {
            cfg.save(specialsDataFile);
        } catch (IOException ex) {
            getLogger().warning("Could not save specials-data.yml: " + ex.getMessage());
        }
    }

    private PlayerSpecialData getOrCreatePlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerSpecialData());
    }

    /**
     * Push the aggregated related-material bonus into SBPC for this player.
     * If no specials are recorded, we leave SBPC's defaults alone.
     */

    /**
     * Apply any specials that were completed earlier but not yet applied,
     * if their section condition now matches the player's current section.
     */
    private void applyPendingSpecialsForCurrentSection(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerSpecialData data = playerData.get(uuid);
        if (data == null) {
            return;
        }

        // We only care about specials that are completed but not applied yet
        for (String id : data.getCompletedSpecials()) {
            if (data.isApplied(id)) {
                continue;
            }

            SpecialDefinition def = specialsById.get(id);
            if (def == null) {
                continue;
            }

            // Only apply if the current section condition matches now
            if (!sectionConditionMatches(def, player)) {
                continue;
            }

            // Apply reward now (no trigger context entity available here)
            applySpecialReward(def, player, null);
        }
    }
    /**
     * Apply the reward for a special to the given player, mark it applied,
     * update bonuses, and fire events/messages.
     *
     * Assumes that scope checks (once-per-player/server) and section conditions
     * have already been handled by the caller.
     */
    private void applySpecialReward(SpecialDefinition def, Player player, Entity contextEntity) {
        String id = def.getId();
        UUID uuid = player.getUniqueId();

        PlayerSpecialData data = getOrCreatePlayerData(uuid);

        // Mark completed & applied on this player (if you have these)
        data.markCompleted(id);
        data.markApplied(id);

        SpecialDefinition.RewardDefinition reward = def.getReward();

        // (We no longer call applyAggregatedBonusToSbpc here.)

        if (reward.getSessionTimeSkipSeconds() > 0) {
            SbpcAPI.applyExternalTimeSkip(uuid, reward.getSessionTimeSkipSeconds(), 0.0,
                    "SBPCSpecials special: " + id);
        }

        // NEW: auto-complete current section if configured
        if (reward.isAutoCompleteSection()) {
            SbpcAPI.completeCurrentSection(uuid);
        }

        // Messages
        SpecialDefinition.MessagesDefinition msg = def.getMessages();
        if (msg.getPlayerMessage() != null && !msg.getPlayerMessage().isEmpty()) {
            player.sendMessage(color(msg.getPlayerMessage()
                    .replace("{player}", player.getName())
                    .replace("{special}", id)));
        }
        if (msg.getBroadcastMessage() != null && !msg.getBroadcastMessage().isEmpty()) {
            Bukkit.broadcastMessage(color(msg.getBroadcastMessage()
                    .replace("{player}", player.getName())
                    .replace("{special}", id)));
        }

        // Fire hook event & invoke registered handlers
        SpecialTriggeredEvent ev = new SpecialTriggeredEvent(def, player, contextEntity);
        Bukkit.getPluginManager().callEvent(ev);
        SpecialsAPI.fireHandlers(ev);
    }


    // ------------------------------------------------------------------------
    // Section condition helper
    // ------------------------------------------------------------------------

    private boolean sectionConditionMatches(SpecialDefinition def, Player player) {
        SpecialDefinition.SectionCondition cond = def.getSectionCondition();
        if (cond == null) return true;

        UUID uuid = player.getUniqueId();

        SectionDefinition section = SbpcAPI.getCurrentSectionDefinition(uuid, true);
        if (section == null) return false;

        // Match by section "type" string, if configured
        if (cond.getRequireType() != null) {
            String secType = section.getType();
            if (secType == null || !secType.equalsIgnoreCase(cond.getRequireType())) {
                return false;
            }
        }

        // Match by section index range, if configured
        String sectionId = section.getId();
        int idx = SbpcAPI.getSectionIndex(sectionId);
        if (idx < 0) {
            return false;
        }

        Integer minIndex = cond.getMinIndex();
        Integer maxIndex = cond.getMaxIndex();

        if (minIndex != null && idx < minIndex) {
            return false;
        }
        if (maxIndex != null && idx > maxIndex) {
            return false;
        }

        return true;
    }

    // ------------------------------------------------------------------------
    // Special triggering pipeline
    // ------------------------------------------------------------------------

    private void triggerSpecial(SpecialDefinition def, Player player, Entity contextEntity) {
        String id = def.getId();
        UUID uuid = player.getUniqueId();

        SpecialDefinition.ScopeDefinition scope = def.getScope();

        // Once-per-server check
        if (scope.isOncePerServer() && completedSpecialsServerWide.contains(id)) {
            return;
        }

        PlayerSpecialData data = getOrCreatePlayerData(uuid);

        // If already applied, nothing more to do
        if (data.isApplied(id)) {
            return;
        }

        // Section condition: determines whether we apply now or just record completion
        boolean sectionMatches = sectionConditionMatches(def, player);

        // If section does NOT match yet:
        // - record completion so we can apply later when the player reaches that section
        if (!sectionMatches) {
            if (!data.isCompleted(id)) {
                data.markCompleted(id);
                if (scope.isOncePerServer()) {
                    completedSpecialsServerWide.add(id);
                }
            }
            return;
        }

        // At this point, section matches. If once-per-player and already completed, avoid re-applying.
        if (scope.isOncePerPlayer() && data.isCompleted(id)) {
            return;
        }

        // Mark server-wide completion if needed
        if (scope.isOncePerServer()) {
            completedSpecialsServerWide.add(id);
        }

        // Apply reward now (marks completed + applied, sets bonuses, fires events)
        applySpecialReward(def, player, contextEntity);
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    // ------------------------------------------------------------------------
    // Event listeners (generic)
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        EntityType type = event.getEntityType();
        List<SpecialDefinition> defs = deathSpecials.get(type);
        if (defs == null || defs.isEmpty()) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        // First, apply any specials that were completed earlier but
        // only become valid in the killer's current section.
        applyPendingSpecialsForCurrentSection(killer);

        for (SpecialDefinition def : defs) {
            SpecialDefinition.TriggerDefinition trig = def.getTrigger();
            if (trig.isKillerMustBePlayer() && killer == null) {
                continue;
            }
            // Trigger this special; triggerSpecial will handle section matching vs. early completion.
            triggerSpecial(def, killer, event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Material type = event.getItem().getItemStack().getType();

        List<SpecialDefinition> defs = pickupSpecials.get(type);
        if (defs == null || defs.isEmpty()) {
            return;
        }

        // Apply any pending specials whose section condition now matches
        applyPendingSpecialsForCurrentSection(player);

        for (SpecialDefinition def : defs) {
            triggerSpecial(def, player, event.getItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnlockItem(UnlockItemEvent event) {
        String entryId = event.getEntry().getId();
        List<SpecialDefinition> defs = unlockEntrySpecials.get(entryId);
        if (defs == null || defs.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        // Apply any pending specials first
        applyPendingSpecialsForCurrentSection(player);

        for (SpecialDefinition def : defs) {
            triggerSpecial(def, player, null);
        }
    }

}
