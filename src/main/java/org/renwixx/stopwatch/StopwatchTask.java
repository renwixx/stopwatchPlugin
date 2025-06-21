package org.renwixx.stopwatch;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class StopwatchTask extends BukkitRunnable {
    private final HashSet<UUID> activeTimers = new HashSet<>();
    private final HashMap<UUID, Integer> playerTicks = new HashMap<>();
    private final BukkitAudiences audiences;
    private final NamespacedKey stopwatchKey;
    private final int updateFrequency;
    private final StopwatchSoundManager soundManager;

    public StopwatchTask(StopwatchPlugin plugin) {
        this.audiences = plugin.getAudiences();
        this.stopwatchKey = new NamespacedKey(plugin, "is_stopwatch");
        this.updateFrequency = plugin.getUpdateFrequency();
        this.soundManager = plugin.getSoundManager();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (!isStopwatch(mainHandItem))
                continue;
            ItemMeta meta = mainHandItem.getItemMeta();
            if (meta == null)
                continue;
            Block blockBelow = player.getLocation().subtract(0, 1.5, 0).getBlock();
            if (meta.getCustomModelData() == 1 && !activeTimers.contains(player.getUniqueId())) {
                if (blockBelow.getType() == Material.DIAMOND_BLOCK)
                    handleStart(player, mainHandItem);
            } else if (meta.getCustomModelData() == 2 && activeTimers.contains(player.getUniqueId()))
                if (blockBelow.getType() == Material.EMERALD_BLOCK)
                    handleFinish(player, mainHandItem);
        }
        for (UUID uuid : new HashSet<>(activeTimers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                activeTimers.remove(uuid);
                playerTicks.remove(uuid);
                continue;
            }
            if (!hasActiveStopwatch(player)) {
                handleExtraFinish(player);
                continue;
            }
            tick(player);
        }
    }

    private void tick(Player player) {
        int ticks = playerTicks.getOrDefault(player.getUniqueId(), 0) + 1;
        playerTicks.put(player.getUniqueId(), ticks);
        if (ticks % this.updateFrequency == 0) {
            Component timeComponent = formatTime(ticks);
            var baseComponents = BungeeComponentSerializer.get().serialize(timeComponent);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, baseComponents);
        }
    }

    private void handleStart(Player player, ItemStack item) {
        activeTimers.add(player.getUniqueId());
        playerTicks.put(player.getUniqueId(), 0);
        soundManager.startTicking(player);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(2);
            item.setItemMeta(meta);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.MASTER, 1f, 2f);
        var audience = this.audiences.player(player);
        audience.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName()).colorIfAbsent(NamedTextColor.GREEN))
                .append(Component.text(" запустил секундомер", TextColor.color(0x00AA00)))
                .build());
    }

    private void handleFinish(Player player, ItemStack item) {
        int finalTicks = playerTicks.getOrDefault(player.getUniqueId(), 0);
        activeTimers.remove(player.getUniqueId());
        playerTicks.remove(player.getUniqueId());
        soundManager.stopTicking(player);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1);
            item.setItemMeta(meta);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.MASTER, 1f, 2f);
        var audience = this.audiences.player(player);
        audience.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName()).colorIfAbsent(NamedTextColor.GREEN))
                .append(Component.text(" остановил секундомер на ", TextColor.color(0x00AA00)))
                .append(formatTime(finalTicks))
                .append(Component.text(" (" + finalTicks + " тиков)", TextColor.color(0x00AA00)))
                .build());
    }

    private void handleExtraFinish(Player player) {
        activeTimers.remove(player.getUniqueId());
        playerTicks.remove(player.getUniqueId());
        soundManager.stopTicking(player);
        for (ItemStack item : player.getInventory().getContents()) {
            if (isStopwatch(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.EGG_CRACK, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05, item);
                    player.getInventory().remove(item);
                    break;
                }
            }
        }
        var audience = this.audiences.player(player);
        audience.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName()).colorIfAbsent(NamedTextColor.GREEN))
                .append(Component.text(" сломал секундомер", TextColor.color(0x00AA00)))
                .build());
    }

    private boolean hasActiveStopwatch(Player player) {
        ItemStack cursorItem = player.getItemOnCursor();
        if (isStopwatch(cursorItem)) {
            ItemMeta meta = cursorItem.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2)
                return true;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isStopwatch(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2)
                    return true;
            }
        }
        return false;
    }

    private boolean isStopwatch(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(stopwatchKey, PersistentDataType.BYTE);
    }

    private Component formatTime(int ticks) {
        long totalMillis = (long) ticks * 50;
        long hours = totalMillis / 3600000;
        long minutes = (totalMillis % 3600000) / 60000;
        long seconds = (totalMillis % 60000) / 1000;
        long millis = totalMillis % 1000;
        String formatted = String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
        return Component.text(formatted, NamedTextColor.GREEN);
    }
}