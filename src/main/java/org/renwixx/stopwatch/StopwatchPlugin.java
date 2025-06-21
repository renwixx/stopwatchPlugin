package org.renwixx.stopwatch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class StopwatchPlugin extends JavaPlugin implements Listener {
    private BukkitAudiences audiences;
    private int updateFrequency;
    private StopwatchSoundManager soundManager;
    private NamespacedKey stopwatchIdKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.updateFrequency = this.getConfig().getInt("update-frequency-ticks", 2);
        this.updateFrequency = Math.max(this.updateFrequency, 1);
        this.audiences = BukkitAudiences.create(this);
        this.soundManager = new StopwatchSoundManager(this);
        this.stopwatchIdKey = new NamespacedKey(this, "is_stopwatch");
        new StopwatchTask(this).runTaskTimer(this, 0L, 1L);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        createStopwatchRecipe();
        getLogger().info("Stopwatch plugin by Renwixx & Vahgard has been enabled!");
    }

    public StopwatchSoundManager getSoundManager() {
        return soundManager;
    }

    public BukkitAudiences getAudiences() {
        return audiences;
    }

    public NamespacedKey getStopwatchIdKey() { return stopwatchIdKey; }

    @Override
    public void onDisable() {
        if (this.audiences != null) {
            this.audiences.close();
            this.audiences = null;
        }
        getLogger().info("Stopwatch plugin has been disabled.");
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null)
            return;
        ItemStack resultItem = event.getRecipe().getResult();
        if (isStopwatch(resultItem)) {
            String uniqueId = UUID.randomUUID().toString();
            ItemMeta meta = resultItem.getItemMeta();
            if (meta == null)
                return;
            NamespacedKey uniqueKey = new NamespacedKey(this, "stopwatch_unique_id");
            meta.getPersistentDataContainer().set(uniqueKey, PersistentDataType.STRING, uniqueId);
            resultItem.setItemMeta(meta);
            event.getInventory().setResult(resultItem);
        }
    }

    private boolean isStopwatch(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.stopwatchIdKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        soundManager.stopTicking(event.getPlayer());
    }

    private void createStopwatchRecipe() {
        ItemStack stopwatchItem = new ItemStack(Material.CLOCK);
        ItemMeta meta = stopwatchItem.getItemMeta();
        if (meta != null) {
            Component displayNameComponent = LegacyComponentSerializer.legacySection().deserialize("§bStopwatch");
            List<Component> loreComponents = Arrays.asList(
                    Component.text("When underfoot through 1 block:", NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text().append(Component.text("Diamond Block ", NamedTextColor.AQUA), Component.text("- Start", NamedTextColor.DARK_GREEN)).decoration(TextDecoration.ITALIC, false).build(),
                    Component.text().append(Component.text("Emerald Block ", NamedTextColor.GREEN), Component.text("- Finish", NamedTextColor.DARK_GREEN)).decoration(TextDecoration.ITALIC, false).build(),
                    Component.text(" "),
                    Component.text("Must be held in the main hand", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            );
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(displayNameComponent));
            List<String> legacyLore = loreComponents.stream()
                    .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                    .collect(Collectors.toList());
            meta.setLore(legacyLore);
            meta.setCustomModelData(1);

            meta.setItemModel(new NamespacedKey("minecraft", "stopwatch"));
            meta.getPersistentDataContainer().set(this.stopwatchIdKey, PersistentDataType.BYTE, (byte) 1);
            stopwatchItem.setItemMeta(meta);
        }
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(this, "stopwatch_recipe"), stopwatchItem);
        shapedRecipe.shape(
                "Q#Q",
                "#X#",
                "Q#Q"
        );
        shapedRecipe.setIngredient('Q', Material.QUARTZ);
        shapedRecipe.setIngredient('#', Material.GOLD_INGOT);
        shapedRecipe.setIngredient('X', Material.REDSTONE);
        Bukkit.addRecipe(shapedRecipe);
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }
}