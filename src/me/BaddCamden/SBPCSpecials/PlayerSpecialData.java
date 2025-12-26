package me.BaddCamden.SBPCSpecials;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player specials state:
 * - which specials are completed
 * - accumulated speed bonuses per special
 *
 * Stacking rule: individual speed bonus percents and skip seconds are summed
 * across applied specials.
 */
public class PlayerSpecialData {

    public static class SpeedBonus {
        private final double percent;
        private final int skipSeconds;


        /**
         * @param percent     percentage boost added to progress timers
         * @param skipSeconds flat seconds removed from section timers
         */
        public SpeedBonus(double percent, int skipSeconds) {
            this.percent = percent;
            this.skipSeconds = skipSeconds;
        }

        /**
         * @return additive percentage increase granted by this special.
         */
        public double getPercent() {
            return percent;
        }

        /**
         * @return seconds skipped on section timers when this bonus applies.
         */
        public int getSkipSeconds() {
            return skipSeconds;
        }
    }

    private final Map<String, SpeedBonus> bonusesBySpecialId = new HashMap<>();
    private final Set<String> completedSpecials = new HashSet<>();
    private final Set<String> appliedSpecials = new HashSet<>();
    private final Map<String, Set<UUID>> uniqueKillsByKey = new HashMap<>();


    /**
     * Store or replace a speed bonus for a special.
     */
    public void addOrUpdateBonus(String specialId, double percent, int skipSeconds) {
        bonusesBySpecialId.put(specialId, new SpeedBonus(percent, skipSeconds));
    }

    /**
     * @return immutable view of all recorded bonuses keyed by special id.
     */
    public Map<String, SpeedBonus> getBonusesBySpecialId() {
        return Collections.unmodifiableMap(bonusesBySpecialId);
    }

    /**
     * Remember that a special has been applied for this player.
     */
    public void markApplied(String specialId) {
        appliedSpecials.add(specialId);
    }

    /**
     * @return true if the player has already received the reward for the id.
     */
    public boolean isApplied(String specialId) {
        return appliedSpecials.contains(specialId);
    }

    /**
     * @return immutable set of applied special ids.
     */
    public Set<String> getAppliedSpecials() {
        return Collections.unmodifiableSet(appliedSpecials);
    }


    /**
     * Flag a special as completed so its reward can be applied later.
     */
    public void markCompleted(String specialId) {
        completedSpecials.add(specialId);
    }

    /**
     * @return whether the player has satisfied completion criteria for the id.
     */
    public boolean isCompleted(String specialId) {
        return completedSpecials.contains(specialId);
    }

    /**
     * @return immutable set of completed specials.
     */
    public Set<String> getCompletedSpecials() {
        return Collections.unmodifiableSet(completedSpecials);
    }

    /**
     * Track a unique kill for specials that require distinct victims.
     */
    public boolean recordUniqueKill(String key, UUID victimUuid) {
        if (key == null || victimUuid == null) {
            return false;
        }

        Set<UUID> kills = uniqueKillsByKey.computeIfAbsent(key, k -> new HashSet<>());
        return kills.add(victimUuid);
    }

    /**
     * @return number of unique kills recorded for the provided key.
     */
    public int getUniqueKillCount(String key) {
        Set<UUID> kills = uniqueKillsByKey.get(key);
        return kills != null ? kills.size() : 0;
    }

    /**
     * @return immutable view of per-key unique kill tracking.
     */
    public Map<String, Set<UUID>> getUniqueKillsByKey() {
        return Collections.unmodifiableMap(uniqueKillsByKey);
    }

    /**
     * Replace the tracked unique kills for a key, typically when loading from disk.
     */
    public void setUniqueKills(String key, Set<UUID> kills) {
        if (key == null || kills == null) {
            return;
        }

        uniqueKillsByKey.put(key, new HashSet<>(kills));
    }

    /**
     * @return combined percentage speed increase from all applied specials.
     */
    public double getTotalSpeedPercent() {
        return bonusesBySpecialId.entrySet().stream()
                .filter(e -> appliedSpecials.contains(e.getKey()))
                .mapToDouble(e -> e.getValue().getPercent())
                .sum();
    }

    /**
     * @return total skip seconds from all applied specials.
     */
    public int getTotalSkipSeconds() {
        return bonusesBySpecialId.entrySet().stream()
                .filter(e -> appliedSpecials.contains(e.getKey()))
                .mapToInt(e -> e.getValue().getSkipSeconds())
                .sum();
    }

    /**
     * Remove all recorded state for a special.
     *
     * @param specialId       identifier to purge
     * @param clearCompletion whether to also clear completed markers
     * @return true if any state changed
     */
    public boolean removeSpecial(String specialId, boolean clearCompletion) {
        boolean changed = appliedSpecials.remove(specialId);
        changed |= bonusesBySpecialId.remove(specialId) != null;
        if (clearCompletion) {
            changed |= completedSpecials.remove(specialId);
        }
        return changed;
    }

}
