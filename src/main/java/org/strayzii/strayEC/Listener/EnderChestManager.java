package org.strayzii.strayEC.Listener;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.strayzii.strayEC.EnderChestHolder;
import org.strayzii.strayEC.StrayEC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnderChestManager implements Listener {
    private StrayEC plugin;
    private FileConfiguration config;

    public EnderChestManager(StrayEC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        this.config = this.plugin.getConfig();
    }

    public String filter(String message, String playerName, String playerGroup, int slotNumber) {
        message = message.replace("$player_name", playerName);
        message = message.replace("$player_group", playerGroup != null ? playerGroup : "");
        message = message.replace("$slot_number", String.valueOf(slotNumber));

        return message;
    }

    private String findRequiredGroup(int slotNumber) {
        ConfigurationSection groups = config.getConfigurationSection("groups");
        if (groups == null) return "default";

        String best = null;
        int bestAllowed = Integer.MAX_VALUE;

        for (String groupName : groups.getKeys(false)) {
            int allowed = groups.getInt(groupName + ".allowed-slots", 0);
            if (allowed >= slotNumber && allowed < bestAllowed) {
                bestAllowed = allowed;
                best = groupName;
            }
        }

        return best != null ? best : "default";
    }

    public void unblock(Inventory inv, int unlockSlots, int totalSlots, String pName) {
        int blockedStart = 27 + unlockSlots;
        LuckPerms luckPerms = LuckPermsProvider.get();

        for (int i = blockedStart; i < totalSlots; i++) {
            int slotNumber = i - 27 + 1;

            String requiredGroup = findRequiredGroup(slotNumber);
            String groupPrefix = resolveGroupPrefix(luckPerms, requiredGroup);

            Material itemMaterial = Material.getMaterial(
                    config.getString("blocked-slot.material", "RED_STAINED_GLASS_PANE")
            );
            if (itemMaterial == null) itemMaterial = Material.RED_STAINED_GLASS_PANE;

            ItemStack item = new ItemStack(itemMaterial);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(ChatColor.translateAlternateColorCodes(
                    '&',
                    filter(config.getString("blocked-slot.displayName", "&cЗакрыто"), pName, groupPrefix, slotNumber)
            ));

            meta.getPersistentDataContainer().set(blockedKey(), PersistentDataType.BYTE, (byte) 1);

            List<String> lore = new ArrayList<>();
            for (String s : config.getStringList("blocked-slot.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', filter(s, pName, groupPrefix, slotNumber)));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
    }

    private String resolveGroupPrefix(LuckPerms luckPerms, String groupName) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) return "";

        String prefix = group.getCachedData()
                .getMetaData(QueryOptions.defaultContextualOptions())
                .getPrefix();

        return prefix != null ? prefix : "";
    }

    private NamespacedKey blockedKey() {
        return new NamespacedKey(plugin, "blocked_slot");
    }

    private boolean isBlockedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(blockedKey(), PersistentDataType.BYTE);
    }

    public Inventory buildEnderChestMenu(Player owner, boolean openedByOther) {
        int totalSlots = 27 + config.getInt("total-slots", 0);

        Inventory menu = Bukkit.createInventory(
                new EnderChestHolder(owner.getUniqueId(), openedByOther),
                totalSlots,
                ChatColor.translateAlternateColorCodes('&', "&5Эндер-Сундук")
        );

        ItemStack[] items = plugin.getDataManager().getEnderChestContents(owner);
        int worstResult = 0;

        for (int i = 0; i < items.length && i < menu.getSize(); i++) {
            ItemStack item = items[i];

            int result = plugin.checkBanned(item);
            if (result != 0) {
                owner.getWorld().dropItemNaturally(owner.getLocation(), item);
                worstResult = Math.max(worstResult, result);
                continue; // в меню не кладём, при закрытии сохранится уже без него
            }

            menu.setItem(i, item);
        }

        if (worstResult != 0) {
            notifyBanned(owner, worstResult);
        }

        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(owner.getUniqueId());
        String groupName = (user != null) ? user.getPrimaryGroup() : "default";

        int unlockSlots = config.getInt("groups." + groupName + ".allowed-slots", 0);

        unblock(menu, unlockSlots, totalSlots, owner.getName());

        return menu;
    }

    @EventHandler
    public void onEnderChestOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        Inventory menu = buildEnderChestMenu(p, false);

        p.openInventory(menu);
        e.setCancelled(true);
    }

    // ---------- Блокированные слоты ----------

    @EventHandler
    public void onEnderChestDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof EnderChestHolder)) return;

        for (int slot : e.getRawSlots()) {
            if (slot >= e.getInventory().getSize()) continue;

            if (isBlockedItem(e.getInventory().getItem(slot))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onItemClickEvent(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof EnderChestHolder)) return;

        if (isBlockedItem(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    // ---------- Запрещённые предметы ----------

    private void notifyBanned(Player p, int result) {
        String key = result == 1 ? "banned-item" : "banned-item-in-container";
        String msg = plugin.getConfig().getString("messages." + key, "message not found");
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBannedClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnderChestHolder)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory clicked = e.getClickedInventory();
        if (clicked == null) return;

        boolean topClicked = clicked.equals(top);
        ItemStack toPlace = null;

        switch (e.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                if (topClicked) toPlace = e.getCursor();
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                if (!topClicked) toPlace = e.getCurrentItem();
            }
            case HOTBAR_SWAP -> {
                if (topClicked) {
                    int button = e.getHotbarButton();
                    toPlace = button >= 0
                            ? p.getInventory().getItem(button)
                            : p.getInventory().getItemInOffHand();
                }
            }
            default -> {
            }
        }

        int result = plugin.checkBanned(toPlace);
        if (result != 0) {
            e.setCancelled(true);
            notifyBanned(p, result);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBannedDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnderChestHolder)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int result = plugin.checkBanned(e.getOldCursor());
        if (result == 0) return;

        for (int slot : e.getRawSlots()) {
            if (slot < top.getSize()) {
                e.setCancelled(true);
                notifyBanned(p, result);
                return;
            }
        }
    }

    // ---------- Сохранение ----------

    @EventHandler
    public void onEnderChestClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof EnderChestHolder holder)) return;

        boolean isOwner = holder.getOwner().equals(e.getPlayer().getUniqueId());

        if (!holder.wasOpenedByAnotherPlayer() && !isOwner) return;

        ItemStack[] contents = e.getInventory().getContents();
        List<ItemStack> toReturn = new ArrayList<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;

            if (isBlockedItem(item)) {
                contents[i] = null;
                continue;
            }

            // Страховка: например, мешок наполнили запрещённым прямо в меню.
            // Чистим только у владельца, чтобы админ не забирал чужие вещи.
            if (isOwner && plugin.checkBanned(item) != 0) {
                toReturn.add(item);
                contents[i] = null;
            }
        }

        if (!toReturn.isEmpty() && e.getPlayer() instanceof Player p) {
            for (ItemStack back : toReturn) {
                Map<Integer, ItemStack> left = p.getInventory().addItem(back);
                for (ItemStack rest : left.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), rest);
                }
            }
            notifyBanned(p, 2);
        }

        plugin.getDataManager().setEnderChestContents(
                holder.getOwner(),
                Arrays.asList(contents)
        );
    }

    public Inventory getEnderChest(Player target) {
        return buildEnderChestMenu(target, true);
    }
}
