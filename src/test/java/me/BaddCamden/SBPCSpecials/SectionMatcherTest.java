package me.BaddCamden.SBPCSpecials;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectionMatcherTest {

    @Test
    @DisplayName("Restricted sections must match allowed list")
    void restrictedSectionMustMatchAllowedList() {
        SpecialDefinition.SectionCondition condition = new SpecialDefinition.SectionCondition(
                "SPECIAL",
                null,
                null,
                false,
                Arrays.asList("special:wood", "special:stone")
        );

        SectionMatchResult allowed = SectionMatcher.evaluate(
                condition,
                new SectionMatchContext("special:wood", "SPECIAL", 3)
        );
        assertTrue(allowed.isAllowed(), "Expected section in allowed list to be accepted");

        SectionMatchResult denied = SectionMatcher.evaluate(
                condition,
                new SectionMatchContext("special:iron", "SPECIAL", 4)
        );
        assertFalse(denied.isAllowed(), "Expected section outside allowed list to be rejected");
    }

    @Test
    @DisplayName("Global flag bypasses section filtering")
    void appliesToAllSectionsBypassesFiltering() {
        SpecialDefinition.SectionCondition condition = new SpecialDefinition.SectionCondition(
                null,
                null,
                null,
                true,
                Collections.emptyList()
        );

        SectionMatchResult result = SectionMatcher.evaluate(
                condition,
                new SectionMatchContext("any-section", "OTHER", 1)
        );
        assertTrue(result.isAllowed(), "Global specials should ignore section filtering");
    }

    @Test
    @DisplayName("Misconfigured special without targets is rejected")
    void misconfiguredSpecialWithoutTargets() {
        SpecialDefinition.SectionCondition condition = new SpecialDefinition.SectionCondition(
                null,
                null,
                null,
                false,
                Collections.emptyList()
        );

        SectionMatchResult result = SectionMatcher.evaluate(
                condition,
                new SectionMatchContext("any-section", "OTHER", 1)
        );
        assertFalse(result.isAllowed(), "Missing allowed sections must fail validation");
    }
}
