package me.BaddCamden.SBPCSpecials;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

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

    public SpecialDefinition(String id,
                             TriggerDefinition trigger,
                             SectionCondition sectionCondition,
                             RewardDefinition reward,
                             ScopeDefinition scope,
                             MessagesDefinition messages) {
        this.id = id;
        this.trigger = trigger;
        this.sectionCondition = sectionCondition;
        this.reward = reward;
        this.scope = scope;
        this.messages = messages;
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

    // ------------------------------------------------------------------------
    // Nested DTOs
    // ------------------------------------------------------------------------

    public static class TriggerDefinition {
        private final TriggerType type;
        private final EntityType entityType;
        private final boolean killerMustBePlayer;
        private final Material itemType;
        private final String entryId;

        public TriggerDefinition(TriggerType type,
                                 EntityType entityType,
                                 boolean killerMustBePlayer,
                                 Material itemType,
                                 String entryId) {
            this.type = type;
            this.entityType = entityType;
            this.killerMustBePlayer = killerMustBePlayer;
            this.itemType = itemType;
            this.entryId = entryId;
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
    }

    public static class SectionCondition {
        private final String requireType; // e.g. "SPECIAL"
        private final Integer minIndex;
        private final Integer maxIndex;

        public SectionCondition(String requireType, Integer minIndex, Integer maxIndex) {
            this.requireType = requireType;
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
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
    }

    public static class RewardDefinition {
        private final double speedBonusPercent;
        private final int speedBonusSkipSeconds;
        private final int sessionTimeSkipSeconds;
        private final boolean autoCompleteSection; 


        public RewardDefinition(double speedBonusPercent,
                int speedBonusSkipSeconds,
                int sessionTimeSkipSeconds,
                boolean autoCompleteSection) {
				this.speedBonusPercent = speedBonusPercent;
				this.speedBonusSkipSeconds = speedBonusSkipSeconds;
				this.sessionTimeSkipSeconds = sessionTimeSkipSeconds;
				this.autoCompleteSection = autoCompleteSection;
        }
        public boolean isAutoCompleteSection() {
            return autoCompleteSection;
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
}
