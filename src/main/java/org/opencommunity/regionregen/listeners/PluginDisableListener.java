package org.opencommunity.regionregen.listeners;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.opencommunity.regionregen.Main;

public class PluginDisableListener implements Listener {
    private final Main plugin;

    public PluginDisableListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        plugin.regenAllBlocks();
    }
}