package me.BaddCamden.SBPCSpecials;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calculates and applies progress-speed modifiers for SBPC specials.
 *
 * Stacking rule: speed bonuses are treated additively on their percentage
 * values, and the resulting multiplier is {@code 1.0 + (sumPercent / 100)}.
 * Skip seconds are also summed across applied specials.
 */
public class ProgressSpeedService {

    @FunctionalInterface
    public interface TimeModifier {
        void apply(UUID playerId, int skipSeconds, double percentSpeedIncrease, String reason);
    }

    private final TimeModifier timeModifier;
    private final Map<UUID, Double> lastAppliedMultiplier = new HashMap<>();
    private final Map<UUID, Integer> lastAppliedSkipSeconds = new HashMap<>();

    public ProgressSpeedService(TimeModifier timeModifier) {
        this.timeModifier = timeModifier;
    }

    public double computeSpeedMultiplier(PlayerSpecialData data) {
        double percent = data.getTotalSpeedPercent();
        return 1.0 + (percent / 100.0);
    }

    public long applySpeedToDurationSeconds(long baseDurationSeconds, PlayerSpecialData data) {
        double multiplier = computeSpeedMultiplier(data);
        int skipSeconds = data.getTotalSkipSeconds();

        long reducedBase = Math.max(0L, baseDurationSeconds - skipSeconds);
        return (long) Math.ceil(reducedBase / multiplier);
    }

    /**
     * Push updated speed bonuses to SBPC. Deltas are sent so repeated calls do
     * not double-apply previous boosts when the underlying API is additive.
     */
    public void applySpeedBonuses(UUID playerId, PlayerSpecialData data, String reason) {
        double multiplier = computeSpeedMultiplier(data);
        int skipSeconds = data.getTotalSkipSeconds();

        double previousMultiplier = lastAppliedMultiplier.getOrDefault(playerId, 1.0);
        int previousSkip = lastAppliedSkipSeconds.getOrDefault(playerId, 0);

        double multiplierDelta = multiplier / previousMultiplier;
        int skipDelta = skipSeconds - previousSkip;

        double percentDelta = (multiplierDelta - 1.0) * 100.0;

        if (Double.compare(multiplierDelta, 1.0) == 0 && skipDelta == 0) {
            return;
        }

        timeModifier.apply(playerId, skipDelta, percentDelta, reason);
        lastAppliedMultiplier.put(playerId, multiplier);
        lastAppliedSkipSeconds.put(playerId, skipSeconds);
    }

    public void applySessionSkip(UUID playerId, int skipSeconds, String reason) {
        if (skipSeconds <= 0) {
            return;
        }
        timeModifier.apply(playerId, skipSeconds, 0.0, reason);
    }
}
