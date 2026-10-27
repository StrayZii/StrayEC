package org.strayzii.strayEC;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class EnderChestHolder implements InventoryHolder {

    private final UUID owner;
    private final boolean wasOpenedByAnotherPlayer;

    public EnderChestHolder(UUID owner, boolean wasOpenedByAnotherPlayer) {
        this.owner = owner;
        this.wasOpenedByAnotherPlayer = wasOpenedByAnotherPlayer;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean wasOpenedByAnotherPlayer() {
        return wasOpenedByAnotherPlayer;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
