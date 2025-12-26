package me.BaddCamden.SBPCSpecials;

/**
 * Callback contract for external plugins to react when a special completes.
 */
public interface SpecialHandler {

    /**
     * Invoked after a special has been fully applied, including rewards and events.
     * Implementors can use the event payload to award additional perks or trigger
     * other plugin-specific logic.
     */
    void onSpecialTriggered(SpecialTriggeredEvent event);
}
