package org.strayzii.strayEC;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BannedTags {
    private final StrayEC plugin;
    private final List<NamespacedKey> tags = new ArrayList<>();

    public BannedTags(StrayEC plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tags.clear();

        for (String raw : plugin.getConfig().getStringList("banned-tags")) {
            // NamespacedKey допускает только нижний регистр
            NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));

            if (key == null) {
                plugin.getLogger().warning("Неверный тег в banned-tags: " + raw);
                continue;
            }

            tags.add(key);
        }
    }

    /**
     * @return 0 - можно, 1 - сам предмет запрещён, 2 - запрещённый предмет лежит внутри контейнера
     */
    public int check(ItemStack item) {
        return check(item, 0);
    }

    private int check(ItemStack item, int depth) {
        if (tags.isEmpty() || item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();

        // Тип значения не важен, смотрим только наличие ключа
        var keys = meta.getPersistentDataContainer().getKeys();
        for (NamespacedKey banned : tags) {
            if (keys.contains(banned)) return 1;
        }

        if (depth >= 3) return 0;

        // Шалкеры и прочие контейнеры, а также бандлы
        List<ItemStack> inside = new ArrayList<>();

        if (meta instanceof BlockStateMeta bsm && bsm.hasBlockState()
                && bsm.getBlockState() instanceof Container container) {
            inside.addAll(Arrays.asList(container.getSnapshotInventory().getContents()));
        }

        if (meta instanceof BundleMeta bundle) {
            inside.addAll(bundle.getItems());
        }

        for (ItemStack in : inside) {
            if (check(in, depth + 1) != 0) return 2;
        }

        return 0;
    }
}