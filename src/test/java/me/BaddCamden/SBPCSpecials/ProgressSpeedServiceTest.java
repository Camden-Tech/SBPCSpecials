package me.BaddCamden.SBPCSpecials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProgressSpeedServiceTest {

    private static class RecordingTimeModifier implements ProgressSpeedService.TimeModifier {
        UUID playerId;
        int skipSeconds;
        double speedMultiplier;
        String reason;

        @Override
        public void apply(UUID playerId, int skipSeconds, double speedMultiplier, String reason) {
            this.playerId = playerId;
            this.skipSeconds = skipSeconds;
            this.speedMultiplier = speedMultiplier;
            this.reason = reason;
        }
    }

    @Test
    void emeraldBlockBoostProducesNineTimesMultiplier() {
        PlayerSpecialData data = new PlayerSpecialData();
        data.markApplied("enchants_tier1_emerald_block_boost");
        data.addOrUpdateBonus("enchants_tier1_emerald_block_boost", 800.0, 0);

        RecordingTimeModifier modifier = new RecordingTimeModifier();
        ProgressSpeedService service = new ProgressSpeedService(modifier);

        UUID playerId = UUID.randomUUID();
        service.applySpeedBonuses(playerId, data, "test reason");

        assertEquals(playerId, modifier.playerId);
        assertEquals(0, modifier.skipSeconds);
        assertEquals(9.0, modifier.speedMultiplier, 0.0001);
    }

    @Test
    void speedBonusesStackAdditivelyAcrossSpecials() {
        PlayerSpecialData data = new PlayerSpecialData();
        data.markApplied("enchants_tier1_emerald_block_boost");
        data.markApplied("enchants_tier2_shipwreck_trim_boost");
        data.addOrUpdateBonus("enchants_tier1_emerald_block_boost", 800.0, 2);
        data.addOrUpdateBonus("enchants_tier2_shipwreck_trim_boost", 50.0, 3);

        RecordingTimeModifier modifier = new RecordingTimeModifier();
        ProgressSpeedService service = new ProgressSpeedService(modifier);

        UUID playerId = UUID.randomUUID();
        service.applySpeedBonuses(playerId, data, "stacking test");

        assertEquals(5, modifier.skipSeconds);
        assertEquals(9.5, modifier.speedMultiplier, 0.0001);
    }

    @Test
    void applySpeedToDurationRespectsMultiplierAndSkips() {
        PlayerSpecialData data = new PlayerSpecialData();
        data.markApplied("enchants_tier1_emerald_block_boost");
        data.addOrUpdateBonus("enchants_tier1_emerald_block_boost", 800.0, 10);

        ProgressSpeedService service = new ProgressSpeedService((p, s, m, r) -> { });

        long adjusted = service.applySpeedToDurationSeconds(180, data);

        assertEquals(19, adjusted);
    }

    @Test
    void skipsApplyOnlyWhenPositive() {
        RecordingTimeModifier modifier = new RecordingTimeModifier();
        ProgressSpeedService service = new ProgressSpeedService(modifier);

        service.applySessionSkip(UUID.randomUUID(), 0, "no-op");

        assertNull(modifier.playerId);
    }
}
