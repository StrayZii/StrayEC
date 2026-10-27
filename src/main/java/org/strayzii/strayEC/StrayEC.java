package org.strayzii.strayEC;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.strayzii.strayEC.Listener.EnderChestManager;

import java.util.List;

public final class StrayEC extends JavaPlugin {
    private DataManager dm;
    private EnderChestManager ecm;
    private BannedTags bannedTags;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.setBannedTags(new BannedTags(this));
        this.setDm(new DataManager(this));
        this.setEcManager(new EnderChestManager(this));

        Bukkit.getPluginManager().registerEvents(this.ecm, this);

        Commands cmds = new Commands(this);

        getCommand("strayec").setExecutor(cmds);
        getCommand("strayec").setTabCompleter(cmds);
    }

    public void setBannedTags(BannedTags bannedTags) {
        this.bannedTags = bannedTags;
    }

    public BannedTags getBannedTags() {
        return this.bannedTags;
    }

    public EnderChestManager getEcManager() {
        return ecm;
    }

    public boolean getBanned(Material material) {
        if (!getConfig().getBoolean("banned-items.enabled")) return false;

        List<String> bannedItems = getConfig().getStringList("banned-items.items");

        return bannedItems.contains(material.name().toUpperCase());
    }

    /**
     * 0 = чисто, 1 = сам предмет запрещён, 2 = запрещённое лежит внутри (шалкер/мешок)
     */
    public int checkBanned(ItemStack item) {
        return checkBanned(item, 0);
    }

    private int checkBanned(ItemStack item, int depth) {
        if (item == null || item.getType() == Material.AIR) return 0;
        if (getBanned(item.getType())) return 1;
        if (depth > 3) return 0; // защита от бесконечной вложенности
        if (!item.hasItemMeta()) return 0;
        int tagResult = bannedTags.check(item);
        if (tagResult != 0) return tagResult;

        ItemMeta meta = item.getItemMeta();

        if (meta instanceof BlockStateMeta bsm
                && bsm.getBlockState() instanceof org.bukkit.block.Container container) {
            for (ItemStack inner : container.getInventory().getContents()) {
                if (checkBanned(inner, depth + 1) != 0) return 2;
            }
        }

        if (meta instanceof BundleMeta bundle) {
            for (ItemStack inner : bundle.getItems()) {
                if (checkBanned(inner, depth + 1) != 0) return 2;
            }
        }

        return 0;
    }

    public void setEcManager(EnderChestManager ecm) {
        this.ecm = ecm;
    }

    public DataManager getDataManager() {
        return dm;
    }

    public void setDm(DataManager dm) {
        this.dm = dm;
    }

    public void reload() {
        reloadConfig();
        this.dm.reload();
        this.ecm.reload();
        this.bannedTags.reload();
    }

    @Override
    public void onDisable() {
    }
}
