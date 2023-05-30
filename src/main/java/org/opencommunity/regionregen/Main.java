package org.opencommunity.regionregen;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.opencommunity.regionregen.commands.RegenCommands;
import org.opencommunity.regionregen.listeners.BlockBreakListener;
import org.opencommunity.regionregen.listeners.PluginDisableListener;

import java.util.*;

public class Main extends JavaPlugin implements Listener {
    public Map<String, List<Material>> regenRegions;
    private WorldGuardPlugin worldGuard;
    public Map<Location, Material> regenBlocks;
    private Map<Location, BukkitTask> regenTasks;
    private int blockRegenDelay;

    private final List<Material> cropsRequiringFarmland = new ArrayList<>(Arrays.asList(
            Material.WHEAT, Material.POTATOES, Material.CARROTS, Material.BEETROOTS));

    @Override
    public void onEnable() {
        if (!setupWorldGuard()) {
            getLogger().severe("WorldGuard plugin not found. Disabling BlockRegenPlugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        regenRegions = new HashMap<>();
        regenBlocks = new HashMap<>();
        regenTasks = new HashMap<>();

        loadConfiguration();
        loadRegenRegions();
        registerEventListeners();
        registerCommands();
        startBlockRegenScheduler();
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();
        blockRegenDelay = config.getInt("block_regen_delay");
    }

    private void loadRegenRegions() {
        ConfigurationSection regenRegionsSection = getConfig().getConfigurationSection("regen_regions");
        if (regenRegionsSection != null) {
            for (String regionId : regenRegionsSection.getKeys(false)) {
                List<String> blockTypes = regenRegionsSection.getStringList(regionId);
                if (blockTypes != null) {
                    List<Material> materials = new ArrayList<>();
                    for (String blockType : blockTypes) {
                        Material material = Material.matchMaterial(blockType);
                        if (material != null) {
                            materials.add(material);
                        } else {
                            getLogger().warning("Invalid block type '" + blockType + "' in regen region '" + regionId + "'.");
                        }
                    }
                    regenRegions.put(regionId, materials);
                } else {
                    getLogger().warning("Invalid block types for regen region '" + regionId + "'.");
                }
            }
        }
    }

    private void registerEventListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(new PluginDisableListener(this), this);
    }

    private void registerCommands() {
        getCommand("rr").setExecutor(new RegenCommands(this));
    }

    private void startBlockRegenScheduler() {
        getServer().getScheduler().runTaskTimer(this, this::checkRegenBlocks, 0L, 1L);
    }

    private boolean setupWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return false;
        }
        worldGuard = (WorldGuardPlugin) plugin;
        return true;
    }

    public void scheduleRegeneration(Location location) {
        BukkitTask task = regenTasks.get(location);
        if (task != null) {
            task.cancel();
        }

        task = Bukkit.getScheduler().runTaskLater(this, () -> regenerateBlock(location), blockRegenDelay * 20L);
        regenTasks.put(location, task);
    }

    private void checkRegenBlocks() {
        Iterator<Map.Entry<Location, BukkitTask>> iterator = regenTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, BukkitTask> entry = iterator.next();
            BukkitTask task = entry.getValue();
            if (task.isCancelled() || task.getTaskId() == -1) {
                iterator.remove();
            }
        }
    }

    public void regenerateBlock(Location location) {
        Material material = regenBlocks.remove(location);
        if (material != null) {
            if (location.getBlock().getType() == material) {
                return;
            }

            playBreakEffect(location, material);

            Block block = location.getBlock();
            if (cropsRequiringFarmland.contains(material)) {
                Block belowBlock = block.getLocation().subtract(0, 1, 0).getBlock();
                if (belowBlock.getType() != Material.FARMLAND && belowBlock.getType() == Material.DIRT) {
                    belowBlock.setType(Material.FARMLAND);
                }
            }

            block.setType(material);
        }
    }

    private void playBreakEffect(Location location, Material material) {
        if (cropsRequiringFarmland.contains(material)) {
            location.getWorld().playSound(location, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
            location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 100, 0, 0, 0, 1, material.createBlockData());
        }
    }

    public void regenAllBlocks() {
        for (Map.Entry<Location, Material> entry : regenBlocks.entrySet()) {
            Location location = entry.getKey();
            Material material = entry.getValue();
            location.getBlock().setType(material);
        }
        regenBlocks.clear();
    }
}
