package org.renwixx.stopwatch;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class StopwatchPlugin extends JavaPlugin implements Listener {
    private BukkitAudiences audiences;
    private StopwatchSoundManager soundManager;
    private LocaleManager localeManager;
    private BukkitTask stopwatchTask;

    private final NamespacedKey stopwatchIdKey = new NamespacedKey(this, "is_stopwatch");
    private final NamespacedKey stopwatchRecipeKey = new NamespacedKey(this, "stopwatch_recipe");

    private int updateFrequency;
    private int messageRadius;
    private String rpManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        createStopwatchRecipe();

        this.stopwatchTask = new StopwatchTask(this).runTaskTimer(this, 0L, 1L);

        getLogger().info("Stopwatch plugin by Renwixx & Vahgard has been enabled!");
    }

    @Override
    public void onDisable() {
        if (this.audiences != null) {
            this.audiences.close();
            this.audiences = null;
        }
        if (this.stopwatchTask != null && !this.stopwatchTask.isCancelled())
            this.stopwatchTask.cancel();
        getLogger().info("Stopwatch plugin has been disabled.");
    }

    private void loadConfiguration() {
        saveDefaultLangFiles();
        this.localeManager = new LocaleManager(this);

        this.updateFrequency = getConfig().getInt("update-frequency-ticks", 2);
        this.updateFrequency = Math.max(this.updateFrequency, 1);
        this.messageRadius = getConfig().getInt("message-distance", 32);
        this.rpManager = getConfig().getString("resource-pack-manager", "minecraft");

        // Инициализируем или пересоздаем компоненты, которые зависят от конфига
        this.audiences = BukkitAudiences.create(this);
        this.soundManager = new StopwatchSoundManager(this, this.rpManager);
    }

    private void saveDefaultLangFiles() {
        if (!new File(getDataFolder(), "lang").exists()) {
            getDataFolder().mkdir();
        }

        List<String> languages = Arrays.asList("en", "uk", "ru");
        for (String langCode : languages) {
            String fileName = "lang/" + langCode + ".yml";
            File langFile = new File(getDataFolder(), fileName);
            if (!langFile.exists())
                saveResource(fileName, false);
        }
    }

    private void createStopwatchRecipe() {
        if (Bukkit.getRecipe(stopwatchRecipeKey) != null)
            return;
        ItemStack stopwatchItem = new ItemStack(Material.CLOCK);
        ItemMeta meta = stopwatchItem.getItemMeta();
        if (meta != null) {
            Component displayNameComponent = LegacyComponentSerializer.legacySection().deserialize(localeManager.getString("lore.name"));
            List<Component> loreComponents = Arrays.asList(
                    Component.text(localeManager.getString("lore.text-1")),
                    Component.text().append(Component.text(localeManager.getString("lore.text-2-1")), Component.text(localeManager.getString("lore.text-2-2"))).build(),
                    Component.text().append(Component.text(localeManager.getString("lore.text-3-1")), Component.text(localeManager.getString("lore.text-3-2"))).build(),
                    Component.text(" "),
                    Component.text(localeManager.getString("lore.text-4"))
            );
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(displayNameComponent));
            List<String> legacyLore = loreComponents.stream()
                    .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                    .collect(Collectors.toList());
            meta.setLore(legacyLore);

            meta.setCustomModelData(1);
            meta.setItemModel(new NamespacedKey(getRPManager(), "stopwatch"));
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(localeManager.getString("command.usage"));
            return true;
        }

        if (!sender.hasPermission("stopwatch.admin")) {
            sender.sendMessage(localeManager.getString("command.no-permission"));
            return true;
        }
        if (this.stopwatchTask != null && !this.stopwatchTask.isCancelled())
            this.stopwatchTask.cancel();
        Bukkit.removeRecipe(this.stopwatchRecipeKey);
        reloadConfig();
        loadConfiguration();
        createStopwatchRecipe();
        this.stopwatchTask = new StopwatchTask(this).runTaskTimer(this, 0L, 1L);
        sender.sendMessage(localeManager.getString("command.reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("stopwatch.admin"))
            return StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), new ArrayList<>());
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasDiscoveredRecipe(this.stopwatchRecipeKey))
            player.discoverRecipe(this.stopwatchRecipeKey);
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
        assert meta != null;
        return meta.getPersistentDataContainer().has(this.stopwatchIdKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        soundManager.stopTicking(event.getPlayer());
    }

    public StopwatchSoundManager getSoundManager() { return soundManager; }
    public BukkitAudiences getAudiences() { return audiences; }
    public NamespacedKey getStopwatchIdKey() { return stopwatchIdKey; }
    public int getUpdateFrequency() { return updateFrequency; }
    public int getMessageRadius() { return messageRadius; }
    public String getRPManager() { return rpManager; }
    public LocaleManager getLocaleManager() { return localeManager; }
}