package org.opencommunity.regionregen.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.opencommunity.regionregen.Main;

import java.util.ArrayList;
import java.util.List;

public class RegenCommands implements CommandExecutor {
    private final Main plugin;

    public RegenCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("rr")) {
            if (args.length < 1) {
                sender.sendMessage("Usage: /rr <command> [arguments]");
                return true;
            }

            String subCommand = args[0];
            if (subCommand.equalsIgnoreCase("addblock")) {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /rr addblock <region> <block>");
                    return true;
                }

                String regionName = args[1];
                Material material = Material.matchMaterial(args[2]);
                if (material == null) {
                    sender.sendMessage("Invalid block type.");
                    return true;
                }

                List<String> regenBlocksList = plugin.getConfig().getStringList("regen_regions." + regionName);
                regenBlocksList.add(material.toString());
                plugin.getConfig().set("regen_regions." + regionName, regenBlocksList);
                plugin.saveConfig();

                // Update in-memory regenRegions map
                List<Material> materials = plugin.regenRegions.getOrDefault(regionName, new ArrayList<>());
                materials.add(material);
                plugin.regenRegions.put(regionName, materials);

                sender.sendMessage("Added " + material + " to the regen block list for region " + regionName + ".");
                return true;
            } else if (subCommand.equalsIgnoreCase("addregion")) {
                if (args.length != 3) {
                    sender.sendMessage("Usage: /rr addregion <region> <block>");
                    return true;
                }

                String regionName = args[1];
                Material material = Material.matchMaterial(args[2]);
                if (material == null) {
                    sender.sendMessage("Invalid block type.");
                    return true;
                }

                List<String> regenRegionsList = plugin.getConfig().getStringList("regen_regions");
                regenRegionsList.add(regionName);
                plugin.getConfig().set("regen_regions", regenRegionsList);

                List<String> regenBlocksList = plugin.getConfig().getStringList("regen_regions." + regionName);
                regenBlocksList.add(material.toString());
                plugin.getConfig().set("regen_regions." + regionName, regenBlocksList);
                plugin.saveConfig();

                // Update in-memory regenRegions map
                List<Material> materials = new ArrayList<>();
                materials.add(material);
                plugin.regenRegions.put(regionName, materials);

                sender.sendMessage("Added region " + regionName + " and block " + material + " to the regen list.");
                return true;
            } else {
                sender.sendMessage("Invalid command. Available commands: addblock, addregion");
                return true;
            }
        }

        return false;
    }
}
