package me.BaddCamden.SBPCSpecials;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired whenever a special is successfully triggered and applied.
 */
public class SpecialTriggeredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpecialDefinition specialDefinition;
    private final Player player;
    private final Entity contextEntity;

    /**
     * Create an event representing a newly triggered special.
     *
     * @param specialDefinition definition of the special that fired
     * @param player            player who earned the special
     * @param contextEntity     optional entity involved in the trigger (may be null)
     */
    public SpecialTriggeredEvent(SpecialDefinition specialDefinition,
                                 Player player,
                                 Entity contextEntity) {
        this.specialDefinition = specialDefinition;
        this.player = player;
        this.contextEntity = contextEntity;
    }

    /**
     * @return the immutable configuration for the triggered special.
     */
    public SpecialDefinition getSpecialDefinition() {
        return specialDefinition;
    }

    /**
     * @return the player who activated the special.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Optional context entity (e.g. mob that died, item entity that was picked up).
     * May be null depending on the trigger type.
     */
    public Entity getContextEntity() {
        return contextEntity;
    }

    /**
     * HandlerList required by the Bukkit event contract.
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Static helper demanded by Bukkit for custom events.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
