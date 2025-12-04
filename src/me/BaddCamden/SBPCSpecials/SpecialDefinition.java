package me.BaddCamden.SBPCSpecials;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

/**
 * Immutable description of a single special, as loaded from config.
 */
public class SpecialDefinition {

    private final String id;
    private final TriggerDefinition trigger;
    private final SectionCondition sectionCondition;
    private final RewardDefinition reward;
    private final ScopeDefinition scope;
    private final MessagesDefinition messages;
    private final PotionRequirement potionRequirement;

    public SpecialDefinition(String id,
                             TriggerDefinition trigger,
                             SectionCondition sectionCondition,
                             RewardDefinition reward,
                             ScopeDefinition scope,
                             MessagesDefinition messages,
                             PotionRequirement potionRequirement) {
        this.id = id;
        this.trigger = trigger;
        this.sectionCondition = sectionCondition;
        this.reward = reward;
        this.scope = scope;
        this.messages = messages;
        this.potionRequirement = potionRequirement;
    }

    public String getId() {
        return id;
    }

    public TriggerDefinition getTrigger() {
        return trigger;
    }

    public SectionCondition getSectionCondition() {
        return sectionCondition;
    }

    public RewardDefinition getReward() {
        return reward;
    }

    public ScopeDefinition getScope() {
        return scope;
    }

    public MessagesDefinition getMessages() {
        return messages;
    }

    public PotionRequirement getPotionRequirement() {
        return potionRequirement;
    }

    // ------------------------------------------------------------------------
    // Nested DTOs
    // ------------------------------------------------------------------------

    public static class TriggerDefinition {
        private final TriggerType type;
        private final EntityType entityType;
        private final boolean killerMustBePlayer;
        private final Material itemType;
        private final String entryId;
        private final boolean commandActivatable;

        public TriggerDefinition(TriggerType type,
                                 EntityType entityType,
                                 boolean killerMustBePlayer,
                                 Material itemType,
                                 String entryId,
                                 boolean commandActivatable) {
            this.type = type;
            this.entityType = entityType;
            this.killerMustBePlayer = killerMustBePlayer;
            this.itemType = itemType;
            this.entryId = entryId;
            this.commandActivatable = commandActivatable;
        }

        public TriggerType getType() {
            return type;
        }

        public EntityType getEntityType() {
            return entityType;
        }

        public boolean isKillerMustBePlayer() {
            return killerMustBePlayer;
        }

        public Material getItemType() {
            return itemType;
        }

        public String getEntryId() {
            return entryId;
        }

        public boolean isCommandActivatable() {
            return commandActivatable;
        }
    }

    public static class SectionCondition {
        private final String requireType; // e.g. "SPECIAL"
        private final Integer minIndex;
        private final Integer maxIndex;
        private final boolean appliesToAllSections;
        private final java.util.List<String> allowedSections;

        public SectionCondition(String requireType,
                                Integer minIndex,
                                Integer maxIndex,
                                boolean appliesToAllSections,
                                java.util.List<String> allowedSections) {
            this.requireType = requireType;
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            this.appliesToAllSections = appliesToAllSections;
            this.allowedSections = java.util.Collections.unmodifiableList(allowedSections);
        }

        public String getRequireType() {
            return requireType;
        }

        public Integer getMinIndex() {
            return minIndex;
        }

        public Integer getMaxIndex() {
            return maxIndex;
        }

        public boolean isAppliesToAllSections() {
            return appliesToAllSections;
        }

        public java.util.List<String> getAllowedSections() {
            return allowedSections;
        }
    }

    public static class RewardDefinition {
        private final double speedBonusPercent;
        private final int speedBonusSkipSeconds;
        private final int sessionTimeSkipSeconds;
        private final boolean autoCompleteSection;
        private final boolean defaultTimeSkip;


        public RewardDefinition(double speedBonusPercent,
                int speedBonusSkipSeconds,
                int sessionTimeSkipSeconds,
                boolean autoCompleteSection,
                boolean defaultTimeSkip) {
                                this.speedBonusPercent = speedBonusPercent;
                                this.speedBonusSkipSeconds = speedBonusSkipSeconds;
                                this.sessionTimeSkipSeconds = sessionTimeSkipSeconds;
                                this.autoCompleteSection = autoCompleteSection;
                                this.defaultTimeSkip = defaultTimeSkip;
        }
        public boolean isAutoCompleteSection() {
            return autoCompleteSection;
        }

        public boolean isDefaultTimeSkip() {
            return defaultTimeSkip;
        }


        public double getSpeedBonusPercent() {
            return speedBonusPercent;
        }

        public int getSpeedBonusSkipSeconds() {
            return speedBonusSkipSeconds;
        }

        public int getSessionTimeSkipSeconds() {
            return sessionTimeSkipSeconds;
        }
    }

    public static class ScopeDefinition {
        private final boolean oncePerPlayer;
        private final boolean oncePerServer;

        public ScopeDefinition(boolean oncePerPlayer, boolean oncePerServer) {
            this.oncePerPlayer = oncePerPlayer;
            this.oncePerServer = oncePerServer;
        }

        public boolean isOncePerPlayer() {
            return oncePerPlayer;
        }

        public boolean isOncePerServer() {
            return oncePerServer;
        }
    }

    public static class MessagesDefinition {
        private final String playerMessage;
        private final String broadcastMessage;

        public MessagesDefinition(String playerMessage, String broadcastMessage) {
            this.playerMessage = playerMessage;
            this.broadcastMessage = broadcastMessage;
        }

        public String getPlayerMessage() {
            return playerMessage;
        }

        public String getBroadcastMessage() {
            return broadcastMessage;
        }
    }

    public static class PotionRequirement {
        private final PotionEffectType effectType;
        private final int minAmplifier;

        public PotionRequirement(PotionEffectType effectType, int minAmplifier) {
            this.effectType = effectType;
            this.minAmplifier = minAmplifier;
        }

        public PotionEffectType getEffectType() {
            return effectType;
        }

        public int getMinAmplifier() {
            return minAmplifier;
        }
    }
}
