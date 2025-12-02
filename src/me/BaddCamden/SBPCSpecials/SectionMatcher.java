package me.BaddCamden.SBPCSpecials;

import java.util.Collections;
import java.util.List;

/**
 * Utility for evaluating whether a special applies to a player's current section.
 */
public final class SectionMatcher {

    private SectionMatcher() {
    }

    public static SectionMatchResult evaluate(SpecialDefinition.SectionCondition condition, SectionMatchContext context) {
        if (condition == null) {
            return SectionMatchResult.allowed("No section condition provided");
        }

        List<String> allowed = condition.getAllowedSections() == null
                ? Collections.emptyList()
                : condition.getAllowedSections();

        if (!condition.isAppliesToAllSections() && allowed.isEmpty()) {
            return SectionMatchResult.denied("No allowed sections configured");
        }

        if (condition.isAppliesToAllSections()) {
            return SectionMatchResult.allowed("Applies to all sections");
        }

        if (context == null) {
            return SectionMatchResult.denied("No active section available");
        }

        String currentId = context.getSectionId();
        if (currentId == null || allowed.stream().noneMatch(s -> s.equalsIgnoreCase(currentId))) {
            return SectionMatchResult.denied("Section '" + currentId + "' is not allowed for this special");
        }

        if (condition.getRequireType() != null && context.getSectionType() != null) {
            if (!context.getSectionType().equalsIgnoreCase(condition.getRequireType())) {
                return SectionMatchResult.denied(
                        "Section type '" + context.getSectionType() + "' does not match required '"
                                + condition.getRequireType() + "'");
            }
        }

        Integer minIndex = condition.getMinIndex();
        Integer maxIndex = condition.getMaxIndex();
        Integer currentIndex = context.getSectionIndex();
        if (currentIndex == null) {
            return SectionMatchResult.denied("No section index available for section '" + currentId + "'");
        }

        if (minIndex != null && currentIndex < minIndex) {
            return SectionMatchResult.denied(
                    "Section index " + currentIndex + " is below minimum required " + minIndex);
        }
        if (maxIndex != null && currentIndex > maxIndex) {
            return SectionMatchResult.denied(
                    "Section index " + currentIndex + " is above maximum allowed " + maxIndex);
        }

        return SectionMatchResult.allowed("Section matches configured constraints");
    }
}
