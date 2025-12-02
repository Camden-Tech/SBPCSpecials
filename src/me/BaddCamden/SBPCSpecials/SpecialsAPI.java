package me.BaddCamden.SBPCSpecials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static API for other plugins to hook SBPCSpecials.
 */
public final class SpecialsAPI {

    private static SBPCSpecialsPlugin plugin;
    private static final Map<String, List<SpecialHandler>> handlersById = new HashMap<>();

    private SpecialsAPI() {
    }

    static void init(SBPCSpecialsPlugin pl) {
        plugin = pl;
    }

    public static SBPCSpecialsPlugin getPlugin() {
        return plugin;
    }

    /**
     * Register a handler for a given special id.
     */
    public static void registerHandler(String specialId, SpecialHandler handler) {
        handlersById.computeIfAbsent(specialId, k -> new ArrayList<>()).add(handler);
    }

    static void fireHandlers(SpecialTriggeredEvent event) {
        SpecialDefinition def = event.getSpecialDefinition();
        if (def == null) return;
        List<SpecialHandler> list = handlersById.get(def.getId());
        if (list == null || list.isEmpty()) return;
        for (SpecialHandler h : list) {
            try {
                h.onSpecialTriggered(event);
            } catch (Throwable t) {
                if (plugin != null) {
                    plugin.getLogger().warning("Error in SpecialHandler for " + def.getId() + ": " + t.getMessage());
                }
            }
        }
    }
}
