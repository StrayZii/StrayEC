package org.strayzii.strayEC;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Commands implements TabExecutor {
    private StrayEC plugin;
    public Commands(StrayEC plugin) {
        this.plugin = plugin;
    }

    public void sendMessageByID(@NotNull Player p, @NotNull String message){
        String msg = plugin.getConfig().getString("messages." + message, "&cMessage not found!");

        p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
        Player p = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0) {
            if (p != null) {
                if (p.hasPermission("strayec.openself")) {
                    if (plugin.getConfig().getBoolean("ec-command", true)) {
                        Inventory inv = plugin.getEcManager().getEnderChest(p);
                        if (inv != null) {
                            p.openInventory(inv);
                        }
                    } else {
                        sendMessageByID(p, "ec-not-allowed");
                        return true;
                    }
                } else {
                    sendMessageByID(p, "no-permission");
                    return true;
                }
            }
            return true;
        }

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("reload")) {
            if (p == null) {
                if (!(sender instanceof ConsoleCommandSender)) return true;
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "Reload Complete!");
            } else {
                if (!p.hasPermission("strayec.reload")) {
                    sendMessageByID(p, "no-permission");
                    return true;
                }
                plugin.reload();
                sendMessageByID(p, "reload");
            }
            return true;
        }
        if (p == null) return true;

        if (Bukkit.getPlayer(args[0]) != null) {
            if (!p.hasPermission("strayec.lookothers")) {
                sendMessageByID(p, "no-permission");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);

            Inventory inv = plugin.getEcManager().getEnderChest(target);
            p.openInventory(inv);

            return true;
        } else {
            sendMessageByID(p, "player-doesnt-exist");
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player p)) return List.of();
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (p.hasPermission("strayec.reload")) {
                list.add("reload");
            }
            if (p.hasPermission("strayec.lookothers")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    list.add(player.getName());
                }
            }

            return list;
        }
        return List.of();
    }
}
