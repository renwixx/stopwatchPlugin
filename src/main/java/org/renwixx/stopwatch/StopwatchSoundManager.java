package org.renwixx.stopwatch;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StopwatchSoundManager {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    private final String TICK_SOUND;
    private static final SoundCategory SOUND_CATEGORY = SoundCategory.MASTER;
    private static final float VOLUME = 0.25f;
    private static final float PITCH = 1.0f;
    private static final long INTERVAL_TICKS = 8L;

    public StopwatchSoundManager(JavaPlugin plugin, String rpManager) {
        this.plugin = plugin;
        this.TICK_SOUND = rpManager + ":stopwatch";
    }

    public void startTicking(Player player) {
        stopTicking(player);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline())
                player.playSound(player.getLocation(), TICK_SOUND, SOUND_CATEGORY, VOLUME, PITCH);
            else
                stopTicking(player);
        }, 0L, INTERVAL_TICKS);

        activeTasks.put(player.getUniqueId(), task);
    }

    public void stopTicking(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null)
            task.cancel();
    }
}