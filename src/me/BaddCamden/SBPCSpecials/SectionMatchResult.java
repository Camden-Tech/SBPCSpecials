package me.BaddCamden.SBPCSpecials;

/**
 * Result of evaluating a section constraint.
 */
public class SectionMatchResult {
    private final boolean allowed;
    private final String reason;

    private SectionMatchResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    /**
     * Build a result indicating the special can apply in the current section.
     */
    public static SectionMatchResult allowed(String reason) {
        return new SectionMatchResult(true, reason);
    }

    /**
     * Build a result indicating the special should not apply here.
     */
    public static SectionMatchResult denied(String reason) {
        return new SectionMatchResult(false, reason);
    }

    /**
     * @return whether the section constraints were satisfied.
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * @return explanation describing why the check passed or failed.
     */
    public String getReason() {
        return reason;
    }
}
