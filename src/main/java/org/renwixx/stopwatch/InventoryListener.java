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
        this.stopwatchKey = plugin.getStopwatchIdKey();
    }

    private boolean isActiveStopwatch(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!meta.getPersistentDataContainer().has(stopwatchKey, PersistentDataType.BYTE))
            return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == 2;
    }

    private void breakStopwatch(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled() || event.getClickedInventory() == null)
            return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        boolean clickedIsActive = isActiveStopwatch(clickedItem);
        boolean cursorIsActive = isActiveStopwatch(cursorItem);

        if (clickedIsActive || cursorIsActive) {
            event.setCancelled(true);
            if (clickedIsActive) {
                breakStopwatch(player);
                event.setCurrentItem(null);
            }
            if (cursorIsActive) {
                breakStopwatch(player);
                event.getView().setCursor(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (event.isCancelled())
            return;
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (isActiveStopwatch(droppedItem)) {
            Player player = event.getPlayer();
            breakStopwatch(player);
            event.getItemDrop().remove();
        }
    }
}