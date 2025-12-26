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

    /**
     * @param id                unique identifier configured in specials.yml
     * @param trigger           trigger conditions that activate the special
     * @param sectionCondition  section constraints that gate the reward
     * @param reward            rewards granted when activated
     * @param scope             limits on how often the special can apply
     * @param messages          player/broadcast messaging templates
     * @param potionRequirement optional potion effect requirement
     */
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

    /**
     * @return unique special identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return trigger metadata for this special.
     */
    public TriggerDefinition getTrigger() {
        return trigger;
    }

    /**
     * @return section condition describing where the reward can apply.
     */
    public SectionCondition getSectionCondition() {
        return sectionCondition;
    }

    /**
     * @return reward definition for the special.
     */
    public RewardDefinition getReward() {
        return reward;
    }

    /**
     * @return scope rules that control repeatability.
     */
    public ScopeDefinition getScope() {
        return scope;
    }

    /**
     * @return configured player/broadcast messages.
     */
    public MessagesDefinition getMessages() {
        return messages;
    }

    /**
     * @return potion effect requirement to qualify for the reward.
     */
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

        /**
         * @param type               trigger type being described
         * @param entityType         optional entity restriction
         * @param killerMustBePlayer whether the killer must be a player
         * @param itemType           optional item type for pickups
         * @param entryId            unlock entry identifier
         * @param commandActivatable true when the special can be invoked by command
         */
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

        /**
         * @return trigger type configured for the special.
         */
        public TriggerType getType() {
            return type;
        }

        /**
         * @return entity type constraint for applicable triggers.
         */
        public EntityType getEntityType() {
            return entityType;
        }

        /**
         * @return true if mob kills must come from a player.
         */
        public boolean isKillerMustBePlayer() {
            return killerMustBePlayer;
        }

        /**
         * @return item type required for pickup triggers.
         */
        public Material getItemType() {
            return itemType;
        }

        /**
         * @return unlock entry id for UNLOCK_ENTRY triggers.
         */
        public String getEntryId() {
            return entryId;
        }

        /**
         * @return whether the special can be run via the /specials command.
         */
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

        /**
         * @param requireType        required section type, if any
         * @param minIndex           minimum allowed section index
         * @param maxIndex           maximum allowed section index
         * @param appliesToAllSections true when the special is global across sections
         * @param allowedSections    whitelist of specific section ids
         */
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

        /**
         * @return required section type, or null if any type is allowed.
         */
        public String getRequireType() {
            return requireType;
        }

        /**
         * @return minimum section index permitted.
         */
        public Integer getMinIndex() {
            return minIndex;
        }

        /**
         * @return maximum section index permitted.
         */
        public Integer getMaxIndex() {
            return maxIndex;
        }

        /**
         * @return true if the special ignores specific section whitelists.
         */
        public boolean isAppliesToAllSections() {
            return appliesToAllSections;
        }

        /**
         * @return immutable list of section ids that are eligible.
         */
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


        /**
         * @param speedBonusPercent   percentage bonus applied to section timers
         * @param speedBonusSkipSeconds seconds skipped when the bonus is applied
         * @param sessionTimeSkipSeconds skip applied immediately to the session
         * @param autoCompleteSection   whether to auto-complete the current section
         * @param defaultTimeSkip       whether to apply the default SBPC skip hook
         */
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
        /**
         * @return true if the section should be auto-completed when triggered.
         */
        public boolean isAutoCompleteSection() {
            return autoCompleteSection;
        }

        /**
         * @return true when the generic SBPC time skip should be invoked.
         */
        public boolean isDefaultTimeSkip() {
            return defaultTimeSkip;
        }


        /**
         * @return percentage speed bonus for this special.
         */
        public double getSpeedBonusPercent() {
            return speedBonusPercent;
        }

        /**
         * @return skip seconds paired with the speed bonus.
         */
        public int getSpeedBonusSkipSeconds() {
            return speedBonusSkipSeconds;
        }

        /**
         * @return session-level skip to apply immediately.
         */
        public int getSessionTimeSkipSeconds() {
            return sessionTimeSkipSeconds;
        }
    }

    public static class ScopeDefinition {
        private final boolean oncePerPlayer;
        private final boolean oncePerServer;

        /**
         * @param oncePerPlayer whether the reward should only be applied once per player
         * @param oncePerServer whether the reward should only occur once per server
         */
        public ScopeDefinition(boolean oncePerPlayer, boolean oncePerServer) {
            this.oncePerPlayer = oncePerPlayer;
            this.oncePerServer = oncePerServer;
        }

        /**
         * @return true if the special is limited to a single grant per player.
         */
        public boolean isOncePerPlayer() {
            return oncePerPlayer;
        }

        /**
         * @return true if the special should only ever fire once across the server.
         */
        public boolean isOncePerServer() {
            return oncePerServer;
        }
    }

    public static class MessagesDefinition {
        private final String playerMessage;
        private final String broadcastMessage;

        /**
         * @param playerMessage     message sent directly to the triggering player
         * @param broadcastMessage  message broadcast to the server
         */
        public MessagesDefinition(String playerMessage, String broadcastMessage) {
            this.playerMessage = playerMessage;
            this.broadcastMessage = broadcastMessage;
        }

        /**
         * @return message shown to the player who triggered the special.
         */
        public String getPlayerMessage() {
            return playerMessage;
        }

        /**
         * @return message broadcast to all players when the special fires.
         */
        public String getBroadcastMessage() {
            return broadcastMessage;
        }
    }

    public static class PotionRequirement {
        private final PotionEffectType effectType;
        private final int minAmplifier;

        /**
         * @param effectType   potion effect required
         * @param minAmplifier minimum amplifier level needed
         */
        public PotionRequirement(PotionEffectType effectType, int minAmplifier) {
            this.effectType = effectType;
            this.minAmplifier = minAmplifier;
        }

        /**
         * @return required potion effect type.
         */
        public PotionEffectType getEffectType() {
            return effectType;
        }

        /**
         * @return minimum amplifier that satisfies the requirement.
         */
        public int getMinAmplifier() {
            return minAmplifier;
        }
    }
}
