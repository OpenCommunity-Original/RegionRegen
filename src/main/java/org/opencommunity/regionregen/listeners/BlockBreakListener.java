package org.opencommunity.regionregen.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.opencommunity.regionregen.Main;

import java.util.List;

public class BlockBreakListener implements Listener {
    private Main plugin;

    public BlockBreakListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Block block = event.getBlock();
        World world = block.getWorld();

        // Check if the block is in a protected region
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            return;
        }

        // Check if the block should be regenerated based on the region
        boolean regenerateBlock = false;
        for (ProtectedRegion region : regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()))) {
            List<Material> regionBlocks = plugin.regenRegions.get(region.getId());

            if (regionBlocks != null) {
                if (regionBlocks.contains(block.getType())) {
                    regenerateBlock = true;
                }
            }
        }

        if (regenerateBlock) {
            plugin.regenBlocks.put(block.getLocation(), block.getType());
            plugin.scheduleRegeneration(block.getLocation());
        }
    }
}