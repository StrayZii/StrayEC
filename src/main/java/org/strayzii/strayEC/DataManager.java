package org.strayzii.strayEC;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {
    private StrayEC plugin;
    private FileConfiguration data;
    private final File file;
    public DataManager(StrayEC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public @Nullable ItemStack @NotNull [] getEnderChestContents(Player p) {
        String path = "players." + p.getUniqueId();

        if (!data.contains(path)) {
            data.set(path, new ItemStack[0]);
            return new ItemStack[0];
        }

        List<?> list = data.getList(path);

        if (list == null) {
            return new ItemStack[0];
        }

        ItemStack[] items = new ItemStack[list.size()];

        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);

            if (obj instanceof ItemStack item) {
                items[i] = item;
            }
        }

        return items;
    }

    public void reload() {
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEnderChestContents(UUID uuid, List<ItemStack> items) {
        String path = "players." + uuid;
        data.set(path, items);
        save();
    }
}
