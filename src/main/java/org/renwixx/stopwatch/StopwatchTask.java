package org.renwixx.stopwatch;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import net.md_5.bungee.api.ChatMessageType;

public class StopwatchTask extends BukkitRunnable {
    private final HashSet<UUID> activeTimers = new HashSet<>();
    private final HashMap<UUID, Integer> playerTicks = new HashMap<>();
    private final BukkitAudiences audiences;
    private final NamespacedKey stopwatchKey;
    private final int updateFrequency;
    private final StopwatchSoundManager soundManager;
    private final int messageRadius;
    private final LocaleManager localeManager;

    public StopwatchTask(StopwatchPlugin plugin) {
        this.audiences = plugin.getAudiences();
        this.stopwatchKey = plugin.getStopwatchIdKey();
        this.updateFrequency = plugin.getUpdateFrequency();
        this.soundManager = plugin.getSoundManager();
        this.messageRadius = plugin.getMessageRadius();
        this.localeManager = plugin.getLocaleManager();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            if (!isStopwatch(mainHandItem)) {
                if(activeTimers.contains(player.getUniqueId()) && !hasActiveStopwatch(player))
                    handleExtraFinish(player);
                continue;
            }
            ItemMeta meta = mainHandItem.getItemMeta();
            if (meta == null)
                continue;

            Block blockBelow = player.getLocation().subtract(0, 1.5, 0).getBlock();
            boolean hasActiveTimer = activeTimers.contains(player.getUniqueId());

            if (meta.getCustomModelData() == 1 && !hasActiveTimer) {
                if (blockBelow.getType() == Material.DIAMOND_BLOCK)
                    handleStart(player, mainHandItem);
            } else if (meta.getCustomModelData() == 2 && hasActiveTimer)
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
            tick(player);
        }
    }

    private void tick(Player player) {
        int ticks = playerTicks.getOrDefault(player.getUniqueId(), 0) + 1;
        playerTicks.put(player.getUniqueId(), ticks);

        if (ticks % this.updateFrequency == 0) {
            String formattedTime = formatTime(ticks);
            Component timeComponent = Component.text(formattedTime, NamedTextColor.GREEN);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, BungeeComponentSerializer.get().serialize(timeComponent));
        }
    }

    private void broadcastMessage(Player centerPlayer, Component message) {
        this.audiences.player(centerPlayer).sendMessage(message);

        Collection<Entity> nearbyEntities = centerPlayer.getNearbyEntities(this.messageRadius, this.messageRadius, this.messageRadius);
        for (Entity entity : nearbyEntities)
            if (entity instanceof Player && !entity.equals(centerPlayer))
                this.audiences.player((Player) entity).sendMessage(message);
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

        Component startMessage = LegacyComponentSerializer.legacySection().deserialize(
                localeManager.getString("stopwatch.start",
                        "%player_name%", player.getDisplayName()
                )
        );
        broadcastMessage(player, startMessage);
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
        Component finishMessage = LegacyComponentSerializer.legacySection().deserialize(
                localeManager.getString("stopwatch.stop",
                        "%player_name%", player.getDisplayName(),
                        "%time%", formatTime(finalTicks),
                        "%ticks%", Integer.toString(finalTicks)
                )
        );

        broadcastMessage(player, finishMessage);
    }

    private void handleExtraFinish(Player player) {
        activeTimers.remove(player.getUniqueId());
        playerTicks.remove(player.getUniqueId());
        soundManager.stopTicking(player);

        for (ItemStack item : player.getInventory().getContents())
            if (isStopwatch(item) && Objects.requireNonNull(item.getItemMeta()).getCustomModelData() == 2) {
                player.getInventory().remove(item);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05);
                break;
            }
        Component breakMessage = LegacyComponentSerializer.legacySection().deserialize(
                localeManager.getString("stopwatch.broke",
                        "%player_name%", player.getDisplayName()
                )
        );
        broadcastMessage(player, breakMessage);
    }

    private boolean hasActiveStopwatch(Player player) {
        for (ItemStack item : player.getInventory().getContents())
            if (isStopwatch(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2)
                    return true;
            }
        return false;
    }

    private boolean isStopwatch(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        return meta.getPersistentDataContainer().has(stopwatchKey, PersistentDataType.BYTE);
    }

    private String formatTime(int ticks) {
        long totalMillis = (long) ticks * 50;
        long hours = totalMillis / 3600000;
        long minutes = (totalMillis % 3600000) / 60000;
        long seconds = (totalMillis % 60000) / 1000;
        long millis = totalMillis % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
}