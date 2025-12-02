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

    public static SectionMatchResult allowed(String reason) {
        return new SectionMatchResult(true, reason);
    }

    public static SectionMatchResult denied(String reason) {
        return new SectionMatchResult(false, reason);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }
}
