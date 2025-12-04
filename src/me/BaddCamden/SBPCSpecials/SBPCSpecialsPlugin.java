package me.BaddCamden.SBPCSpecials;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.BaddCamden.SBPC.api.SbpcAPI;
import me.BaddCamden.SBPCSpecials.ProgressSpeedService;
import me.BaddCamden.SBPCSpecials.SectionProgressListener;
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
public class SBPCSpecialsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final ProgressSpeedService progressSpeedService =
            new ProgressSpeedService(SbpcAPI::applyExternalTimeSkip);

    // ------------------------------------------------------------------------
    // Config-driven specials indexes
    // ------------------------------------------------------------------------

    private final Map<String, SpecialDefinition> specialsById = new HashMap<>();
    private final Map<EntityType, List<SpecialDefinition>> deathSpecials = new HashMap<>();
    private final List<SpecialDefinition> deathSpecialsAny = new ArrayList<>();
    private final Map<Material, List<SpecialDefinition>> pickupSpecials = new HashMap<>();
    private final Map<String, List<SpecialDefinition>> unlockEntrySpecials = new HashMap<>();
    private final Map<PotionEffectType, List<SpecialDefinition>> potionEffectSpecials = new HashMap<>();

    // ------------------------------------------------------------------------
    // Per-player & server state
    // ------------------------------------------------------------------------

    private final Map<UUID, PlayerSpecialData> playerData = new HashMap<>();
    private final Set<String> completedSpecialsServerWide = new HashSet<>();

    private static final String MURDER_SECTION_ID = "murder";
    private static final String MASSACRE_SECTION_ID = "massacre";
    private static final String MASSACRE_UNIQUE_KEY = "massacre_unique_kills";
    private static final int MASSACRE_MAX_UNIQUE_KILLS = 3;
    private static final int MASSACRE_ENTRY_SKIP_SECONDS = 3600;
    private static final String SERIAL_KILLER_SECTION_ID = "serial_killer";
    private static final int SERIAL_KILLER_KILL_SKIP_SECONDS = 1800;

    private static final String PERMISSION_ACTIVATE = "sbpcspecials.command.activate";
    private static final String PERMISSION_REMOVE = "sbpcspecials.command.remove";

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
        Bukkit.getPluginManager().registerEvents(new SectionProgressListener(), this);

        // Initialize hook API
        SpecialsAPI.init(this);

        PluginCommand command = getCommand("specials");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        } else {
            getLogger().warning("Could not register /specials command (not defined in plugin.yml)");
        }


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
        deathSpecialsAny.clear();
        pickupSpecials.clear();
        unlockEntrySpecials.clear();
        potionEffectSpecials.clear();

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

            boolean commandActivatable = trigSec.getBoolean("command-activatable", false);

            SpecialDefinition.TriggerDefinition triggerDef = new SpecialDefinition.TriggerDefinition(
                    triggerType,
                    entityType,
                    killerMustBePlayer,
                    itemType,
                    entryId,
                    commandActivatable
            );

            // --- Section condition ---
            ConfigurationSection secCond = sec.getConfigurationSection("section");
            String requireType = secCond != null ? secCond.getString("require-type", null) : null;
            Integer minIndex = (secCond != null && secCond.contains("min-index"))
                    ? secCond.getInt("min-index") : null;
            Integer maxIndex = (secCond != null && secCond.contains("max-index"))
                    ? secCond.getInt("max-index") : null;

            boolean appliesToAllSections = secCond != null && secCond.getBoolean("applies-to-all-sections", false);
            List<String> allowedSections = secCond != null ? secCond.getStringList("allowed-sections") : Collections.emptyList();
            allowedSections = new ArrayList<>(allowedSections);
            allowedSections.removeIf(s -> s == null || s.trim().isEmpty());

            if (!appliesToAllSections && allowedSections.isEmpty()) {
                getLogger().warning("Special " + id + " is missing allowed-sections and does not apply to all sections; skipping.");
                continue;
            }

            SpecialDefinition.SectionCondition sectionCondition =
                    new SpecialDefinition.SectionCondition(requireType, minIndex, maxIndex, appliesToAllSections, allowedSections);

            // --- Reward ---
            ConfigurationSection rewardSec = sec.getConfigurationSection("reward");
            double speedPercent = rewardSec != null ? rewardSec.getDouble("speed-bonus-percent", 0.0) : 0.0;
            int skipSeconds = rewardSec != null ? rewardSec.getInt("speed-bonus-skip-seconds", 0) : 0;
            int sessionSkip = rewardSec != null ? rewardSec.getInt("session-time-skip-seconds", 0) : 0;
            boolean autoCompleteSection = rewardSec != null && rewardSec.getBoolean("auto-complete-section", false);
            boolean defaultTimeSkip = rewardSec != null && rewardSec.getBoolean("default-time-skip", false);

            SpecialDefinition.RewardDefinition rewardDef =
                    new SpecialDefinition.RewardDefinition(speedPercent, skipSeconds, sessionSkip, autoCompleteSection, defaultTimeSkip);

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

            // --- Potion requirement ---
            ConfigurationSection potionSec = sec.getConfigurationSection("potion-requirement");
            SpecialDefinition.PotionRequirement potionRequirement = null;
            if (potionSec != null) {
                String effectName = potionSec.getString("effect", "");
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType == null) {
                    getLogger().warning("Special " + id + " has invalid potion effect: " + effectName);
                } else {
                    int minAmplifier = Math.max(0, potionSec.getInt("min-amplifier", 0));
                    potionRequirement = new SpecialDefinition.PotionRequirement(effectType, minAmplifier);
                }
            }

            SpecialDefinition def = new SpecialDefinition(
                    id,
                    triggerDef,
                    sectionCondition,
                    rewardDef,
                    scopeDef,
                    msgDef,
                    potionRequirement
            );
            specialsById.put(id, def);

            if (potionRequirement != null && potionRequirement.getEffectType() != null) {
                potionEffectSpecials.computeIfAbsent(potionRequirement.getEffectType(), k -> new ArrayList<>()).add(def);
            }

            // Index by trigger type
            switch (triggerType) {
                case ENTITY_DEATH:
                    if (entityType != null) {
                        deathSpecials.computeIfAbsent(entityType, k -> new ArrayList<>()).add(def);
                    } else {
                        // No entity specified: allow this special to trigger on any mob kill.
                        deathSpecialsAny.add(def);
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
                        data.markApplied(specialId); // bonuses imply the special was already applied
                    }
                }

                List<String> completed = cfg.getStringList("completed-specials");
                for (String s : completed) {
                    data.markCompleted(s);
                }

                ConfigurationSection uniqueSec = cfg.getConfigurationSection("unique-kills");
                if (uniqueSec != null) {
                    for (String key : uniqueSec.getKeys(false)) {
                        List<String> victimList = uniqueSec.getStringList(key);
                        Set<UUID> victims = new HashSet<>();
                        for (String victimId : victimList) {
                            try {
                                victims.add(UUID.fromString(victimId));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        data.setUniqueKills(key, victims);
                    }
                }

                playerData.put(uuid, data);
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid player UUID in Players folder: " + name);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerSpecialData data = playerData.get(uuid);
        if (data == null) {
            return;
        }

        // Reapply any stored bonuses on join so progress timers stay in sync after restarts.
        progressSpeedService.applySpeedBonuses(
                uuid,
                data,
                "SBPCSpecials persisted bonuses on join"
        );

        // Specials completed in past sections may become applicable now.
        applyPendingSpecialsForCurrentSection(player);
        applyPendingPotionRequirementSpecials(player, null);
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

            ConfigurationSection uniqueSec = cfg.createSection("unique-kills");
            for (Map.Entry<String, Set<UUID>> e : data.getUniqueKillsByKey().entrySet()) {
                List<String> victims = e.getValue().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList());
                uniqueSec.set(e.getKey(), victims);
            }

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

            if (!requirementsMet(def, player)) {
                continue;
            }

            // Apply reward now (no trigger context entity available here)
            applySpecialReward(def, player, null);
        }
    }

    private SpecialDefinition getSpecialDefinition(String specialId) {
        SpecialDefinition def = specialsById.get(specialId);
        if (def != null) {
            return def;
        }

        for (SpecialDefinition candidate : specialsById.values()) {
            if (candidate.getId().equalsIgnoreCase(specialId)) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"specials".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length < 2) {
            sendCommandUsage(player, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String specialId = args[1];
        SpecialDefinition def = getSpecialDefinition(specialId);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "Unknown special id: " + specialId);
            return true;
        }

        switch (sub) {
            case "activate" -> handleActivateCommand(player, def);
            case "remove" -> handleRemoveCommand(player, def);
            default -> sendCommandUsage(player, label);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!"specials".equalsIgnoreCase(command.getName())) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("activate", "remove");
        }

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String current = args[1].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            if ("activate".equals(sub) && player.hasPermission(PERMISSION_ACTIVATE)) {
                return specialsById.values().stream()
                        .map(SpecialDefinition::getId)
                        .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(current))
                        .sorted()
                        .toList();
            }

            if ("remove".equals(sub) && player.hasPermission(PERMISSION_REMOVE)) {
                PlayerSpecialData data = getOrCreatePlayerData(player.getUniqueId());
                return data.getAppliedSpecials().stream()
                        .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(current))
                        .sorted()
                        .toList();
            }
        }

        return Collections.emptyList();
    }

    private void handleActivateCommand(Player player, SpecialDefinition def) {
        if (!player.hasPermission(PERMISSION_ACTIVATE)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to activate specials.");
            return;
        }

        if (def.getTrigger() == null || !def.getTrigger().isCommandActivatable()) {
            player.sendMessage(ChatColor.RED + "This special cannot be activated via command.");
            return;
        }

        applyPendingSpecialsForCurrentSection(player);

        if (!sectionConditionMatches(def, player)) {
            player.sendMessage(ChatColor.RED + "You must be in the correct section or entry to activate this special.");
            return;
        }

        String id = def.getId();
        UUID uuid = player.getUniqueId();
        PlayerSpecialData data = getOrCreatePlayerData(uuid);

        if (data.isApplied(id)) {
            player.sendMessage(ChatColor.YELLOW + "That special is already active.");
            return;
        }

        SpecialDefinition.ScopeDefinition scope = def.getScope();
        if (scope != null) {
            if (scope.isOncePerServer() && completedSpecialsServerWide.contains(id)) {
                player.sendMessage(ChatColor.RED + "This special has already been completed server-wide.");
                return;
            }
            if (scope.isOncePerPlayer() && data.isCompleted(id)) {
                player.sendMessage(ChatColor.RED + "You have already completed this special.");
                return;
            }
        }

        if (scope != null && scope.isOncePerServer()) {
            completedSpecialsServerWide.add(id);
        }

        applySpecialReward(def, player, null);
        player.sendMessage(ChatColor.GREEN + "Special " + id + " activated.");
    }

    private void handleRemoveCommand(Player player, SpecialDefinition def) {
        if (!player.hasPermission(PERMISSION_REMOVE)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to remove specials.");
            return;
        }

        String id = def.getId();
        PlayerSpecialData data = getOrCreatePlayerData(player.getUniqueId());

        if (!data.isApplied(id)) {
            player.sendMessage(ChatColor.RED + "That special is not currently active for you.");
            return;
        }

        boolean clearCompletion = def.getScope() != null && !def.getScope().isOncePerPlayer();
        boolean removed = data.removeSpecial(id, clearCompletion);
        if (!removed) {
            player.sendMessage(ChatColor.RED + "Could not remove that special.");
            return;
        }

        progressSpeedService.applySpeedBonuses(
                player.getUniqueId(),
                data,
                "SBPCSpecials command removal (" + id + ")"
        );

        player.sendMessage(ChatColor.YELLOW + "Special " + id + " removed.");
    }

    private void sendCommandUsage(Player player, String label) {
        player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <activate|remove> <special-id>");
    }
    private void applyPendingPotionRequirementSpecials(Player player, PotionEffectType changedEffect) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerSpecialData data = playerData.get(uuid);
        if (data == null) {
            return;
        }

        for (String id : data.getCompletedSpecials()) {
            if (data.isApplied(id)) {
                continue;
            }

            SpecialDefinition def = specialsById.get(id);
            if (def == null) {
                continue;
            }

            SpecialDefinition.PotionRequirement potionRequirement = def.getPotionRequirement();
            if (potionRequirement == null) {
                continue;
            }

            PotionEffectType requiredEffect = potionRequirement.getEffectType();
            if (changedEffect != null && requiredEffect != null && !requiredEffect.equals(changedEffect)) {
                continue;
            }

            if (!sectionConditionMatches(def, player)) {
                continue;
            }

            if (!requirementsMet(def, player)) {
                continue;
            }

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

        if (reward.isDefaultTimeSkip()) {
            SbpcAPI.applyExternalTimeSkip(
                    uuid,
                    0,
                    0.0,
                    "SBPCSpecials default time skip (" + id + ")"
            );
        } else if (reward.getSpeedBonusPercent() != 0.0 || reward.getSpeedBonusSkipSeconds() != 0) {
            data.addOrUpdateBonus(id, reward.getSpeedBonusPercent(), reward.getSpeedBonusSkipSeconds());
        }

        progressSpeedService.applySpeedBonuses(
                uuid,
                data,
                "SBPCSpecials progress speed bonuses (" + id + ")"
        );

        if (reward.getSessionTimeSkipSeconds() > 0) {
            progressSpeedService.applySessionSkip(
                    uuid,
                    reward.getSessionTimeSkipSeconds(),
                    "SBPCSpecials special: " + id
            );
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
        sendSectionApplicabilityMessage(player, def.getSectionCondition());
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

        String sectionId = section.getId();
        int idx = SbpcAPI.getSectionIndex(sectionId);
        if (idx < 0) {
            logSectionMismatch(def, player, SectionMatchResult.denied("Could not determine section index for " + sectionId));
            return false;
        }

        SectionMatchContext context = new SectionMatchContext(sectionId, section.getType(), idx);
        SectionMatchResult result = SectionMatcher.evaluate(cond, context);
        if (!result.isAllowed()) {
            logSectionMismatch(def, player, result);
        }
        return result.isAllowed();
    }

    private void logSectionMismatch(SpecialDefinition def, Player player, SectionMatchResult result) {
        if (result == null || result.isAllowed()) {
            return;
        }
        String playerName = player != null ? player.getName() : "unknown";
        getLogger().info("Special " + def.getId() + " not applied for " + playerName + ": " + result.getReason());
    }

    private boolean requirementsMet(SpecialDefinition def, Player player) {
        SpecialDefinition.PotionRequirement potionReq = def.getPotionRequirement();
        if (potionReq == null) {
            return true;
        }

        PotionEffectType type = potionReq.getEffectType();
        if (type == null) {
            return false;
        }

        PotionEffect effect = player.getPotionEffect(type);
        return effect != null && effect.getAmplifier() >= potionReq.getMinAmplifier();
    }

    private void markSpecialCompletion(SpecialDefinition def,
                                       PlayerSpecialData data,
                                       SpecialDefinition.ScopeDefinition scope) {
        if (!data.isCompleted(def.getId())) {
            data.markCompleted(def.getId());
        }
        if (scope.isOncePerServer()) {
            completedSpecialsServerWide.add(def.getId());
        }
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
            markSpecialCompletion(def, data, scope);
            return;
        }

        // At this point, section matches. If once-per-player and already applied, avoid re-applying.
        if (scope.isOncePerPlayer() && data.isCompleted(id) && data.isApplied(id)) {
            return;
        }

        if (!requirementsMet(def, player)) {
            markSpecialCompletion(def, data, scope);
            return;
        }

        // Mark server-wide completion if needed
        if (scope.isOncePerServer()) {
            completedSpecialsServerWide.add(id);
        }

        // Apply reward now (marks completed + applied, sets bonuses, fires events)
        applySpecialReward(def, player, contextEntity);
    }

    private void handlePvpSectionSpecials(Player killer, Player victim) {
        if (killer == null || victim == null) {
            return;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return; // ignore self kills
        }

        SectionDefinition section = SbpcAPI.getCurrentSectionDefinition(killer.getUniqueId(), true);
        if (section == null) {
            return;
        }

        String sectionId = section.getId().toLowerCase(Locale.ROOT);

        if (MURDER_SECTION_ID.equals(sectionId)) {
            SbpcAPI.completeCurrentSection(killer.getUniqueId());
            killer.sendMessage(ChatColor.RED + "Killing a player completed the Murder section.");
            return;
        }

        if (MASSACRE_SECTION_ID.equals(sectionId)) {
            PlayerSpecialData data = getOrCreatePlayerData(killer.getUniqueId());
            if (data.recordUniqueKill(MASSACRE_UNIQUE_KEY, victim.getUniqueId())) {
                int count = data.getUniqueKillCount(MASSACRE_UNIQUE_KEY);
                if (count <= MASSACRE_MAX_UNIQUE_KILLS) {
                    progressSpeedService.applySessionSkip(
                            killer.getUniqueId(),
                            MASSACRE_ENTRY_SKIP_SECONDS,
                            "SBPCSpecials Massacre unique player kill " + count
                    );
                    killer.sendMessage(ChatColor.DARK_RED + "Unique player kill " + count
                            + " recorded for Massacre.");
                }
            }
            return;
        }

        if (SERIAL_KILLER_SECTION_ID.equals(sectionId)) {
            progressSpeedService.applySessionSkip(
                    killer.getUniqueId(),
                    SERIAL_KILLER_KILL_SKIP_SECONDS,
                    "SBPCSpecials Serial Killer player kill"
            );
            killer.sendMessage(ChatColor.DARK_PURPLE + "Player kill skipped 30 minutes in Serial Killer.");
        }
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void sendSectionApplicabilityMessage(Player player, SpecialDefinition.SectionCondition condition) {
        if (condition == null) {
            return;
        }

        if (condition.isAppliesToAllSections()) {
            player.sendMessage(ChatColor.YELLOW + "This special applies to all sections.");
            return;
        }

        if (condition.getAllowedSections() != null && !condition.getAllowedSections().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Allowed sections: " + String.join(", ", condition.getAllowedSections()));
        }
    }

    // ------------------------------------------------------------------------
    // Event listeners (generic)
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PotionEffectType changedType = event.getModifiedType();
        if (changedType != null && !potionEffectSpecials.containsKey(changedType)) {
            return;
        }

        applyPendingPotionRequirementSpecials(player, changedType);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        applyPendingPotionRequirementSpecials(killer, null);

        if (event.getEntity() instanceof Player victimPlayer) {
            handlePvpSectionSpecials(killer, victimPlayer);
        }

        EntityType type = event.getEntityType();
        List<SpecialDefinition> defs = new ArrayList<>();

        List<SpecialDefinition> typeDefs = deathSpecials.get(type);
        if (typeDefs != null) {
            defs.addAll(typeDefs);
        }
        // "Any" death specials should ignore player deaths so PVP doesn't trigger mob-based bonuses.
        if (type != EntityType.PLAYER) {
            defs.addAll(deathSpecialsAny);
        }

        if (defs.isEmpty()) {
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

        applyPendingPotionRequirementSpecials(player, null);

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
        applyPendingPotionRequirementSpecials(player, null);

        for (SpecialDefinition def : defs) {
            triggerSpecial(def, player, null);
        }
    }

}
