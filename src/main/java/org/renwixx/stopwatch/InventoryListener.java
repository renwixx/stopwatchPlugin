package org.renwixx.stopwatch;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener implements Listener {
    private final NamespacedKey stopwatchKey;

    public InventoryListener(StopwatchPlugin plugin) {
        this.stopwatchKey = new NamespacedKey(plugin, "is_stopwatch");
    }

    private boolean isActiveStopwatch(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(stopwatchKey, PersistentDataType.BYTE))
            return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 2;
    }

    private void breakStopwatch(Player player, ItemStack itemToBreak) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
        player.getWorld().spawnParticle(Particle.ITEM, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05, itemToBreak);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || event.getClick().isKeyboardClick())
            return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (isActiveStopwatch(clickedItem) || isActiveStopwatch(cursorItem)) {
            event.setCancelled(true);
            if (isActiveStopwatch(clickedItem)) {
                breakStopwatch(player, clickedItem);
                event.setCurrentItem(null);
            }
            if (isActiveStopwatch(cursorItem)) {
                breakStopwatch(player, cursorItem);
                event.setCursor(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (isActiveStopwatch(droppedItem)) {
            Player player = event.getPlayer();
            breakStopwatch(player, droppedItem);
            event.getItemDrop().remove();
        }
    }
}