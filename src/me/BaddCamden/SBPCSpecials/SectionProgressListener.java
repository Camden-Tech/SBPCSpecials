package me.BaddCamden.SBPCSpecials;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.BaddCamden.SBPC.api.SbpcAPI;
import me.BaddCamden.SBPC.progress.SectionDefinition;

/**
 * Custom progression hooks for sections that rely on world interactions rather
 * than related-material pickups. Currently supports:
 *
 * <ul>
 *     <li>Housing (custom-key: {@code housing_infrastructure}) – sped up by
 *     placing/breaking blocks, tilling dirt, or stripping logs.</li>
 *     <li>Farming (custom-key: {@code farming_progress}) – sped up by breaking
 *     fully-grown/non-player-placed crops or tilling dirt.</li>
 * </ul>
 */
public class SectionProgressListener implements Listener {

    private static final String HOUSING_SECTION_ID = "housing";
    private static final String FARMING_SECTION_ID = "farming";

    private static final double SPEED_MULTIPLIER = 1.05; // +5% speed per tick
    private static final int SKIP_SECONDS = 1;

    private final Map<UUID, Long> lastHousingTick = new HashMap<>();
    private final Map<UUID, Location> lastHousingLocation = new HashMap<>();
    private final Map<UUID, Long> lastFarmingTick = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        handleHousingProgress(player, block.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        handleHousingProgress(player, block.getLocation());
        handleFarmingProgress(player, block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null || event.getItem() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (isHoe(item) && isTillable(block.getType())) {
            // Tilling dirt counts for both sections.
            handleHousingProgress(player, block.getLocation());
            handleFarmingProgress(player, block);
            return;
        }

        if (isAxe(item) && Tag.LOGS.isTagged(block.getType())) {
            // Stripping wood counts for housing progression.
            handleHousingProgress(player, block.getLocation());
        }
    }

    private void handleHousingProgress(Player player, Location location) {
        if (!isInSection(player, HOUSING_SECTION_ID)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (isOnCooldown(lastHousingTick, uuid, now)) {
            return;
        }

        Location lastLoc = lastHousingLocation.get(uuid);
        if (lastLoc != null && lastLoc.equals(location)) {
            return; // prevent spam on the same block position
        }

        lastHousingTick.put(uuid, now);
        lastHousingLocation.put(uuid, location);
        applySkip(player, "Housing infrastructure activity");
    }

    private void handleFarmingProgress(Player player, Block block) {
        if (!isInSection(player, FARMING_SECTION_ID)) {
            return;
        }

        if (!isQualifyingPlant(block)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (isOnCooldown(lastFarmingTick, uuid, now)) {
            return;
        }

        lastFarmingTick.put(uuid, now);
        applySkip(player, "Farming harvest activity");
    }

    private boolean isOnCooldown(Map<UUID, Long> map, UUID uuid, long nowMillis) {
        Long last = map.get(uuid);
        return last != null && (nowMillis - last) < 1000L;
    }

    private void applySkip(Player player, String reason) {
        SbpcAPI.applyExternalTimeSkip(player.getUniqueId(), SKIP_SECONDS, SPEED_MULTIPLIER, reason);
    }

    private boolean isInSection(Player player, String sectionId) {
        SectionDefinition section = SbpcAPI.getCurrentSectionDefinition(player.getUniqueId(), true);
        return section != null && section.getId().equalsIgnoreCase(sectionId);
    }

    private boolean isQualifyingPlant(Block block) {
        // Fully grown crops always count. Otherwise, allow non player-placed plants.
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() >= ageable.getMaximumAge()) {
                return true;
            }
        }

        return !isPlayerPlaced(block) && isPlantLike(block.getType());
    }

    private boolean isPlantLike(Material type) {
        return Tag.CROPS.isTagged(type)
                || Tag.FLOWERS.isTagged(type)
                || Tag.SAPLINGS.isTagged(type)
                || Tag.CAVE_VINES.isTagged(type)
                || type.name().contains("VINE")
                || type.name().contains("SAPLING")
                || type.name().contains("STEM")
                || type.name().contains("BAMBOO")
                || type.name().contains("FUNGUS")
                || type.name().contains("WART")
                || type.name().contains("CACTUS")
                || type.name().contains("SUGAR_CANE")
                || type.name().contains("MUSHROOM");
    }

    private boolean isPlayerPlaced(Block block) {
        return block.hasMetadata("player_placed") || block.hasMetadata("placed_by_player");
    }

    private boolean isHoe(ItemStack item) {
        Material type = item.getType();
        return type != null && type.name().endsWith("_HOE");
    }

    private boolean isAxe(ItemStack item) {
        Material type = item.getType();
        return type != null && type.name().endsWith("_AXE");
    }

    private boolean isTillable(Material type) {
        return type == Material.DIRT
                || type == Material.GRASS_BLOCK
                || type == Material.DIRT_PATH
                || type == Material.ROOTED_DIRT
                || type == Material.COARSE_DIRT
                || type == Material.MYCELIUM;
    }

}
