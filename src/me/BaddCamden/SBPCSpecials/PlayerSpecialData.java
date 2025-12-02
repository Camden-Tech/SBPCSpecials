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
        

        public SpeedBonus(double percent, int skipSeconds) {
            this.percent = percent;
            this.skipSeconds = skipSeconds;
        }

        public double getPercent() {
            return percent;
        }

        public int getSkipSeconds() {
            return skipSeconds;
        }
    }

    private final Map<String, SpeedBonus> bonusesBySpecialId = new HashMap<>();
    private final Set<String> completedSpecials = new HashSet<>();
    private final Set<String> appliedSpecials = new HashSet<>();
    private final Map<String, Set<UUID>> uniqueKillsByKey = new HashMap<>();


    public void addOrUpdateBonus(String specialId, double percent, int skipSeconds) {
        bonusesBySpecialId.put(specialId, new SpeedBonus(percent, skipSeconds));
    }

    public Map<String, SpeedBonus> getBonusesBySpecialId() {
        return Collections.unmodifiableMap(bonusesBySpecialId);
    }
    public void markApplied(String specialId) {
        appliedSpecials.add(specialId);
    }

    public boolean isApplied(String specialId) {
        return appliedSpecials.contains(specialId);
    }

    public Set<String> getAppliedSpecials() {
        return Collections.unmodifiableSet(appliedSpecials);
    }


    public void markCompleted(String specialId) {
        completedSpecials.add(specialId);
    }

    public boolean isCompleted(String specialId) {
        return completedSpecials.contains(specialId);
    }

    public Set<String> getCompletedSpecials() {
        return Collections.unmodifiableSet(completedSpecials);
    }

    public boolean recordUniqueKill(String key, UUID victimUuid) {
        if (key == null || victimUuid == null) {
            return false;
        }

        Set<UUID> kills = uniqueKillsByKey.computeIfAbsent(key, k -> new HashSet<>());
        return kills.add(victimUuid);
    }

    public int getUniqueKillCount(String key) {
        Set<UUID> kills = uniqueKillsByKey.get(key);
        return kills != null ? kills.size() : 0;
    }

    public Map<String, Set<UUID>> getUniqueKillsByKey() {
        return Collections.unmodifiableMap(uniqueKillsByKey);
    }

    public void setUniqueKills(String key, Set<UUID> kills) {
        if (key == null || kills == null) {
            return;
        }

        uniqueKillsByKey.put(key, new HashSet<>(kills));
    }

    public double getTotalSpeedPercent() {
        return bonusesBySpecialId.entrySet().stream()
                .filter(e -> appliedSpecials.contains(e.getKey()))
                .mapToDouble(e -> e.getValue().getPercent())
                .sum();
    }

    public int getTotalSkipSeconds() {
        return bonusesBySpecialId.entrySet().stream()
                .filter(e -> appliedSpecials.contains(e.getKey()))
                .mapToInt(e -> e.getValue().getSkipSeconds())
                .sum();
    }

}
