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

    public SpecialTriggeredEvent(SpecialDefinition specialDefinition,
                                 Player player,
                                 Entity contextEntity) {
        this.specialDefinition = specialDefinition;
        this.player = player;
        this.contextEntity = contextEntity;
    }

    public SpecialDefinition getSpecialDefinition() {
        return specialDefinition;
    }

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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
