package me.BaddCamden.SBPCSpecials;

/**
 * Snapshot of the player's current section for matching against special requirements.
 */
public class SectionMatchContext {
    private final String sectionId;
    private final String sectionType;
    private final Integer sectionIndex;

    /**
     * @param sectionId    unique section identifier
     * @param sectionType  section type reported by SBPC (e.g., SPECIAL)
     * @param sectionIndex ordinal index of the current section, if known
     */
    public SectionMatchContext(String sectionId, String sectionType, Integer sectionIndex) {
        this.sectionId = sectionId;
        this.sectionType = sectionType;
        this.sectionIndex = sectionIndex;
    }

    /**
     * @return the current section id for the player.
     */
    public String getSectionId() {
        return sectionId;
    }

    /**
     * @return the section type associated with the current progress entry.
     */
    public String getSectionType() {
        return sectionType;
    }

    /**
     * @return the index of the player's current section, or null when unavailable.
     */
    public Integer getSectionIndex() {
        return sectionIndex;
    }
}
