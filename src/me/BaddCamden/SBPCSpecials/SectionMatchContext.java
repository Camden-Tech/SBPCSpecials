package me.BaddCamden.SBPCSpecials;

/**
 * Snapshot of the player's current section for matching against special requirements.
 */
public class SectionMatchContext {
    private final String sectionId;
    private final String sectionType;
    private final Integer sectionIndex;

    public SectionMatchContext(String sectionId, String sectionType, Integer sectionIndex) {
        this.sectionId = sectionId;
        this.sectionType = sectionType;
        this.sectionIndex = sectionIndex;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getSectionType() {
        return sectionType;
    }

    public Integer getSectionIndex() {
        return sectionIndex;
    }
}
