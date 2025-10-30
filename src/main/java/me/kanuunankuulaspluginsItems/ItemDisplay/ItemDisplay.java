package me.kanuunankuulaspluginsItems.ItemDisplay;

import me.kanuunankuulaspluginsItems.ItemDisplay.Inventory.ViewInvAdmin;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionType;

import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.AttributeFormatter.*;
import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.Colors.*;
import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.EnchantmentFormatter.*;
import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.ItemNameFormatter.*;
import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.PotionFormatter.*;
import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.FireworkFormatter.*;

import static me.kanuunankuulaspluginsItems.ItemDisplay.Inventory.ViewInvNormal.*;

import static org.bukkit.ChatColor.*;


public class ItemDisplay extends JavaPlugin implements Listener, CommandExecutor {

    private static Chat vaultChat;
    private FileConfiguration config;
    public static NamespacedKey viewOnlyKey;
    public static NamespacedKey headOwnerKey;
    public static Plugin Itemdisplay;

    public static final String VIEW_ONLY_MARKER = "itemdisplay_viewonly";
    private static final String KANUUNANKUULA_NAME = "Kanuunankuula"; // 1239 Line, ( Only a joke if the server is using HeadHunter too)
    public static final String INVENTORY_TITLE_SUFFIX = "'s Inventory";

    public static final String VIEW_ONLY_MARKET_TWO = "itemdisplay_viewonly";
    private static final String[] ROMAN_NUMERALS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private boolean useMinecraftFormat;
    private boolean showAdvancedTooltips;
    private boolean debug;


    private boolean isFolia = false;

    public static final Map<String, Integer> enchantmentOrderCache = new HashMap<>();
    public static final Map<Material, Double> attackDamageCache = new EnumMap<>(Material.class);
    public static final Map<Material, Double> attackSpeedCache = new EnumMap<>(Material.class);
    private static final EnumMap<Material, String> categoryCache = new EnumMap<>(Material.class);

    public static Map<UUID, UUID> viewingInventories = new HashMap<>();
    public static Map<UUID, Long> sharedInventoryTimestamps = new HashMap<>();
    private Map<String, String> itemDescriptions = new HashMap<>();
    private Map<String, String> patternDescriptions = new HashMap<>();

    public static Set<UUID> sharedInventories = new HashSet<>();

    public static void Log(String Message, String type) {
        if (type == "warn") {
            Itemdisplay.getLogger().warning(Message);
        } else if (type == "error") {
            Itemdisplay.getLogger().warning("ERROR: "+ Message);
        } else {
            Itemdisplay.getLogger().info(Message);
        }
    }
    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected! Using region-aware scheduling.");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Running on standard Paper/Spigot.");
        }
        ViewInvAdmin.initialize(this);

        saveDefaultConfig();
        config = getConfig();
        saveDefaultItemDescriptions();
        loadItemDescriptions();

        if (!config.contains("debug")) {
            config.set("debug", false);
            saveConfig();
        }

        if (!config.contains("use-minecraft-format")) {
            config.set("use-minecraft-format", true);
            saveConfig();
        }

        if (!config.contains("show-advanced-tooltips")) {
            config.set("show-advanced-tooltips", true);
            saveConfig();
        }
        loadConfigCache();

        getServer().getPluginManager().registerEvents(this, this);

        initializeCaches();
        initializeCategories();
        Itemdisplay = this;

        if (!setupVaultChat()) {
            getLogger().warning("Vault Chat not found! Prefixes will not work.");
        } else {
            getLogger().info("Vault Chat successfully hooked!");
        }
        viewOnlyKey = new NamespacedKey(this, VIEW_ONLY_MARKER);
        viewOnlyKey = new NamespacedKey(this, VIEW_ONLY_MARKET_TWO);

        Plugin headHunterPlugin = getServer().getPluginManager().getPlugin("HeadHunterPlugin");
        if (headHunterPlugin != null) {
            headOwnerKey = new NamespacedKey(headHunterPlugin, "head_owner");
        }

        if (getCommand("viewinv") != null) {
            getCommand("viewinv").setExecutor(this);
        }

        scheduleRepeatingTask(null, this::cleanupViewOnlyItems, 600L, 600L);
        getLogger().info("ItemDisplay plugin enabled!");
    }

    private boolean setupVaultChat() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            vaultChat = rsp.getProvider();
            getLogger().info("Vault Chat provider: " + vaultChat.getClass().getSimpleName());
            return true;
        }

        getLogger().warning("No Vault Chat provider found!");
        return false;
    }


    private void initializeCategories() {
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (Stream.of("SWORD", "AXE", "ARROW", "MACE", "TRIDENT").anyMatch(name::contains)) {
                categoryCache.put(mat, "Combat");
            }
            else if (Stream.of("PICKAXE", "SHOVEL", "HOE", "ROCKET").anyMatch(name::contains)) {
                categoryCache.put(mat, "Tools");
            }
            else if (Stream.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD", "ELYTRA").anyMatch(name::contains)) {
                categoryCache.put(mat, "Combat");
            }
            else if (mat.isEdible()) {
                categoryCache.put(mat,"Food & Drinks");
            }
            else if (mat.isBlock() && !name.contains("SPAWN_EGG")) {
                categoryCache.put(mat,"Building Blocks");
            } else if (name.contains("SPAWN") && !(name.contains("ANCHOR") && !(name.contains("FROG")) )) {
                categoryCache.put(mat,"Spawn Eggs");
            }
            else {
                categoryCache.put(mat, "Miscellaneous");
            }
        }
    }
    private void loadConfigCache() {
        this.useMinecraftFormat = config.getBoolean("use-minecraft-format", true);
        this.showAdvancedTooltips = config.getBoolean("show-advanced-tooltips", true);
        this.debug = config.getBoolean("debug", false);
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemDisplay plugin disabled!");
    }

    private void scheduleTask(Entity entity, Runnable task, long delay) {
        if (isFolia) {
            if (entity != null) {
                entity.getScheduler().run(this, (t) -> task.run(), null);
            } else {
                Bukkit.getGlobalRegionScheduler().runDelayed(this, (t) -> task.run(), delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(this, task, delay);
        }
    }

    private void scheduleRepeatingTask(Entity entity, Runnable task, long delay, long period) {
        if (isFolia) {
            if (entity != null) {
                entity.getScheduler().runAtFixedRate(this, (t) -> task.run(), null, delay, period);
            } else {
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> task.run(), delay, period);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(this, task, delay, period);
        }
    }

    private void runEntityTask(Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().run(this, (t) -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("itemdisplay")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("itemdisplay.reload")) {
                    sender.sendMessage(RED + "You don't have permission to reload the plugin!");
                    return true;
                }

                reloadConfig();
                config = getConfig();
                loadConfigCache();
                loadItemDescriptions();
                sender.sendMessage(GREEN + "Item descriptions reloaded!");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("viewinv")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(RED + "Usage: /viewinv <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(RED + "Player not found!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("itemdisplay.viewinv")) {
                player.sendMessage(RED + "You don't have permission to view inventories!");
                return true;
            }

            if (!sharedInventories.contains(target.getUniqueId()) || isInventoryExpired(target.getUniqueId())) {
                if (isInventoryExpired(target.getUniqueId())) {
                    sharedInventories.remove(target.getUniqueId());
                    sharedInventoryTimestamps.remove(target.getUniqueId());
                }
                player.sendMessage(RED + target.getName() + " hasn't shared their inventory or it has expired!");
                return true;
            }

            CreateTheInv(target, player);

            return true;
        }

        if (command.getName().equalsIgnoreCase("invedit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            Player admin = (Player) sender;

            if (!admin.hasPermission("itemdisplay.admin.invedit")) {
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                admin.sendMessage(RED + "Usage: /invedit <player>");
                return true;
            }

            String targetName = args[0];

            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                admin.sendMessage(RED + "✗ " + GRAY + "Player " + GOLD + targetName + GRAY + " not found!");
                return true;
            }

            ViewInvAdmin.openPlayerInventory(admin, target);

            return true;
        }

        if (command.getName().equalsIgnoreCase("echestedit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            Player admin = (Player) sender;

            if (!admin.hasPermission("itemdisplay.admin.echest")) {
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                admin.sendMessage(RED + "Usage: /echestedit <player>");
                return true;
            }

            String targetName = args[0];

            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                admin.sendMessage(RED + "✗ " + GRAY + "Player " + GOLD + targetName + GRAY + " not found!");
                return true;
            }

            ViewInvAdmin.openPlayerEnderChest(admin, target);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("viewinv") ||
                command.getName().equalsIgnoreCase("invedit") ||
                command.getName().equalsIgnoreCase("echestedit")) {

            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private void saveDefaultItemDescriptions() {
        File itemDescFile = new File(getDataFolder(), "item-descriptions.yml");
        if (!itemDescFile.exists()) {
            try {
                itemDescFile.getParentFile().mkdirs();
                itemDescFile.createNewFile();

                FileConfiguration itemConfig = YamlConfiguration.loadConfiguration(itemDescFile);
                itemConfig.set("descriptions.BARRIER", "TBH, I will use this to let you know, these are just like a tab part extra to a item");
                itemConfig.set("descriptions.ELYTRA", "Allows flight when worn");
                itemConfig.set("descriptions.TOTEM_OF_UNDYING", "Saves you from death");
                itemConfig.set("descriptions.BEACON", "Provides area effects");
                itemConfig.set("descriptions.NETHER_STAR", "Dropped by the Wither");
                itemConfig.set("descriptions.DRAGON_EGG", "A trophy from the End");
                itemConfig.set("descriptions.OAK_BOAT", "Bro, why? It's a boat. Nothing special");
                itemConfig.set("descriptions.PLAYER_HEAD", "Bro, why? It's a player? Hopefully not Kanuunankuula");
                itemConfig.set("descriptions.COMMAND_BLOCK", "Alright a Illegal item ;) Probably eating it rn");
                itemConfig.set("descriptions.PORKCHOP", "The flesh who walk the fields");

                itemConfig.set("patterns.SHULKER_BOX", "Portable storage container");
                itemConfig.set("patterns.SPAWN_EGG", "Spawns a {mob_name}");

                itemConfig.save(itemDescFile);
                getLogger().info("Created default item-descriptions.yml");
            } catch (Exception e) {
                getLogger().warning("Could not create item-descriptions.yml: " + e.getMessage());
            }
        }
    }

    private void loadItemDescriptions() {
        File itemDescFile = new File(getDataFolder(), "item-descriptions.yml");
        if (itemDescFile.exists()) {
            try {
                FileConfiguration itemConfig = YamlConfiguration.loadConfiguration(itemDescFile);

                ConfigurationSection descriptions = itemConfig.getConfigurationSection("descriptions");
                if (descriptions != null) {
                    itemDescriptions.clear();
                    for (String key : descriptions.getKeys(false)) {
                        itemDescriptions.put(key, descriptions.getString(key));
                    }
                }

                ConfigurationSection patterns = itemConfig.getConfigurationSection("patterns");
                if (patterns != null) {
                    patternDescriptions.clear();
                    for (String key : patterns.getKeys(false)) {
                        patternDescriptions.put(key, patterns.getString(key));
                    }
                }

                getLogger().info("Loaded " + itemDescriptions.size() + " item descriptions and " + patternDescriptions.size() + " pattern descriptions");
            } catch (Exception e) {
                getLogger().warning("Could not load item-descriptions.yml: " + e.getMessage());
            }
        }
    }

    private boolean isInventoryExpired(UUID playerUUID) {
        if (!sharedInventoryTimestamps.containsKey(playerUUID)) {
            return true;
        }

        long sharedTime = sharedInventoryTimestamps.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        long fiveMinutesInMillis = 5 * 60 * 1000;

        return (currentTime - sharedTime) > fiveMinutesInMillis;
    }


    private boolean isViewOnly(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(viewOnlyKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (isPlayerMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(RED + "You are muted and cannot send messages!");
            return;
        }

        if (message.contains("[i]")) {
            event.setCancelled(true);
            runEntityTask(player, () -> handleItemDisplay(player, message));
            return;
        }

        if (message.contains("[inv]")) {
            event.setCancelled(true);
            runEntityTask(player, () -> handleInventoryDisplay(player, message));
        }
    }

    private boolean isPlayerMuted(Player player) {
        if (checkLibertyBansMute(player)) {
            return true;
        }

        try {
            org.bukkit.plugin.Plugin essentials = null;
            for (org.bukkit.plugin.Plugin plugin : getServer().getPluginManager().getPlugins()) {
                if (plugin.getName().startsWith("Essentials")) {
                    essentials = plugin;
                    break;
                }
            }

            if (essentials == null) {
                return false;
            }

            try {
                com.earth2me.essentials.Essentials ess = (com.earth2me.essentials.Essentials) essentials;
                com.earth2me.essentials.User user = ess.getUser(player);

                if (user == null) {
                    return false;
                }

                return user.isMuted();

            } catch (Exception castError) {
                try {
                    Class<?> essentialsClass = essentials.getClass();
                    Object essInstance = essentialsClass.cast(essentials);

                    java.lang.reflect.Method getUserMethod = essentialsClass.getMethod("getUser", Player.class);
                    Object user = getUserMethod.invoke(essInstance, player);

                    if (user == null) {
                        return false;
                    }

                    java.lang.reflect.Method isMutedMethod = user.getClass().getMethod("isMuted");
                    return (Boolean) isMutedMethod.invoke(user);

                } catch (Exception reflectionError) {
                    return false;
                }
            }

        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkLibertyBansMute(Player player) {
        try {
            org.bukkit.plugin.Plugin libertyBans = getServer().getPluginManager().getPlugin("LibertyBans");

            if (libertyBans == null) {
                return false;
            }

            try {
                Class.forName("space.arim.libertybans.core.Part");
                Class<?> apiProviderClass = Class.forName("space.arim.libertybans.api.LibertyBans");
                java.lang.reflect.Method getMethod = apiProviderClass.getMethod("get");
                Object libertyBansApi = getMethod.invoke(null);

                if (libertyBansApi != null) {
                    java.lang.reflect.Method getSelectorMethod = libertyBansApi.getClass().getMethod("getSelector");
                    Object selector = getSelectorMethod.invoke(libertyBansApi);

                    Class<?> victimClass = Class.forName("space.arim.libertybans.api.Victim");
                    Class<?> playerVictimClass = Class.forName("space.arim.libertybans.api.PlayerVictim");
                    java.lang.reflect.Method ofMethod = playerVictimClass.getMethod("of", java.util.UUID.class);
                    Object victim = ofMethod.invoke(null, player.getUniqueId());

                    java.lang.reflect.Method selectionBuilderMethod = selector.getClass().getMethod("selectionBuilder");
                    Object selectionBuilder = selectionBuilderMethod.invoke(selector);

                    Class<?> punishmentTypeClass = Class.forName("space.arim.libertybans.api.PunishmentType");
                    Object muteType = punishmentTypeClass.getField("MUTE").get(null);

                    java.lang.reflect.Method typeMethod = selectionBuilder.getClass().getMethod("type", punishmentTypeClass);
                    selectionBuilder = typeMethod.invoke(selectionBuilder, muteType);

                    java.lang.reflect.Method victimMethod = selectionBuilder.getClass().getMethod("victim", victimClass);
                    selectionBuilder = victimMethod.invoke(selectionBuilder, victim);

                    java.lang.reflect.Method buildMethod = selectionBuilder.getClass().getMethod("build");
                    Object selectionOrder = buildMethod.invoke(selectionBuilder);

                    java.lang.reflect.Method getFirstSpecificPunishmentMethod = selectionOrder.getClass().getMethod("getFirstSpecificPunishment");
                    Object futureOptional = getFirstSpecificPunishmentMethod.invoke(selectionOrder);

                    java.lang.reflect.Method joinMethod = futureOptional.getClass().getMethod("join");
                    Object optional = joinMethod.invoke(futureOptional);

                    java.lang.reflect.Method isPresentMethod = optional.getClass().getMethod("isPresent");
                    return (Boolean) isPresentMethod.invoke(optional);
                }

            } catch (ClassNotFoundException e) {
            }

        } catch (Exception e) {
        }

        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (isViewOnly(clickedItem)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (isViewOnly(cursorItem)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack oldCursor = event.getOldCursor();
        if (isViewOnly(oldCursor)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isViewOnly(droppedItem)) {
            event.setCancelled(true);
            scheduleTask(event.getPlayer(), () -> removeViewOnlyItems(event.getPlayer()), 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX )) {
                viewingInventories.remove(playerUUID);
                scheduleTask(player, () -> removeViewOnlyItems(player), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleTask(event.getPlayer(), () -> removeViewOnlyItems(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        viewingInventories.remove(playerUUID);
        removeViewOnlyItems(event.getPlayer());
    }

    @EventHandler
    public void onAdminInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player admin = (Player) event.getWhoClicked();
        UUID adminUUID = admin.getUniqueId();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {

            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            boolean isEnderChest = invTitle.contains(ViewInvAdmin.ADMIN_ENDER_SUFFIX);
            String requiredPerm = isEnderChest ? ViewInvAdmin.EchestAdminPerm : ViewInvAdmin.InventoryAdminPerm;

            if (!admin.hasPermission(requiredPerm)) {
                event.setCancelled(true);
                admin.closeInventory();
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit inventories!");
                ViewInvAdmin.cleanupAdminView(adminUUID);
                return;
            }

            int inventorySize = event.getInventory().getSize();

            if (slot >= inventorySize) {
                return;
            }

            if (!isEnderChest && inventorySize == 54) {
                if (slot >= 36 && slot <= 44) {
                    event.setCancelled(true);
                    return;
                }

                if (slot == 45 || slot == 46 || slot == 51 || slot == 53) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (clickedItem != null && ViewInvAdmin.isPlaceholderItem(clickedItem)) {
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    event.setCancelled(false);

                    scheduleTask(admin, () -> {
                        event.getInventory().setItem(slot, cursorItem.clone());
                        admin.setItemOnCursor(null);
                    }, 1L);

                    event.setCancelled(true);
                    return;
                } else {
                    event.setCancelled(true);
                    return;
                }
            }

            if (cursorItem != null && ViewInvAdmin.isPlaceholderItem(cursorItem)) {
                event.setCancelled(true);
                return;
            }

            if (!isEnderChest && inventorySize == 54) {
                if ((slot >= 47 && slot <= 50) || slot == 52) {
                    if (clickedItem != null && !ViewInvAdmin.isPlaceholderItem(clickedItem)) {
                        scheduleTask(admin, () -> {
                            ItemStack currentItem = event.getInventory().getItem(slot);
                            if (currentItem == null || currentItem.getType() == Material.AIR) {
                                Material placeholderMat;
                                String slotName;

                                switch (slot) {
                                    case 47:
                                        placeholderMat = Material.IRON_HELMET;
                                        slotName = "Helmet";
                                        break;
                                    case 48:
                                        placeholderMat = Material.IRON_CHESTPLATE;
                                        slotName = "Chestplate";
                                        break;
                                    case 49:
                                        placeholderMat = Material.IRON_LEGGINGS;
                                        slotName = "Leggings";
                                        break;
                                    case 50:
                                        placeholderMat = Material.IRON_BOOTS;
                                        slotName = "Boots";
                                        break;
                                    case 52:
                                        placeholderMat = Material.SHIELD;
                                        slotName = "Offhand";
                                        break;
                                    default:
                                        return;
                                }

                                event.getInventory().setItem(slot, ViewInvAdmin.createArmorPlaceholder(placeholderMat, slotName));
                            }
                        }, 1L);
                    }
                }
            }

            return;
        }

        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack clickedItem2 = event.getCurrentItem();
        if (isViewOnly(clickedItem2)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursorItem2 = event.getCursor();
        if (isViewOnly(cursorItem2)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onAdminInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player admin = (Player) event.getWhoClicked();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {
            for (int slot : event.getRawSlots()) {
                if ((slot >= 36 && slot <= 44) || slot == 45 || slot == 46 || slot == 51 || slot == 53) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (ViewInvAdmin.isPlaceholderItem(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        UUID adminUUID = admin.getUniqueId();
        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack oldCursor = event.getOldCursor();
        if (isViewOnly(oldCursor)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerJoinWhileBeingEdited(PlayerJoinEvent event) {
        UUID joinedPlayerUUID = event.getPlayer().getUniqueId();

        for (Map.Entry<UUID, UUID> entry : ViewInvAdmin.adminViewingPlayer.entrySet()) {
            UUID adminUUID = entry.getKey();
            UUID targetUUID = entry.getValue();

            if (targetUUID.equals(joinedPlayerUUID)) {
                Player admin = Bukkit.getPlayer(adminUUID);

                if (admin != null && admin.isOnline()) {
                    scheduleTask(admin, () -> {
                        admin.closeInventory();
                        admin.sendMessage(YELLOW + "⚠ " + GOLD + event.getPlayer().getName() +
                                GRAY + " came online!");
                        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Their offline inventory editor has been closed to prevent data loss.");
                        admin.sendMessage(GRAY + "Use " + GOLD + "/invedit " + event.getPlayer().getName() +
                                GRAY + " to edit their live inventory.");

                        ViewInvAdmin.cleanupAdminView(adminUUID);
                    }, 1L);

                    scheduleTask(event.getPlayer(), () -> {
                        event.getPlayer().sendMessage(YELLOW + "⚠ " + GRAY + "An admin was viewing your offline inventory.");
                        event.getPlayer().sendMessage(GRAY + "The editor has been closed to protect your items.");
                    }, 20L);
                }
            }
        }
    }

    @EventHandler
    public void onAdminInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player admin = (Player) event.getPlayer();
        UUID adminUUID = admin.getUniqueId();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {
            boolean isEnderChest = invTitle.contains(ViewInvAdmin.ADMIN_ENDER_SUFFIX);
            String requiredPerm = isEnderChest ? ViewInvAdmin.EchestAdminPerm : ViewInvAdmin.InventoryAdminPerm;

            if (!admin.hasPermission(requiredPerm)) {
                admin.closeInventory();
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit inventories!");
                ViewInvAdmin.cleanupAdminView(admin.getUniqueId());
                return;
            }


            ViewInvAdmin.saveInventoryChanges(admin, event.getInventory());
            ViewInvAdmin.cleanupAdminView(adminUUID);
            return;
        }

        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                viewingInventories.remove(adminUUID);
                scheduleTask(admin, () -> removeViewOnlyItems(admin), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuitWithAdminView(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        viewingInventories.remove(playerUUID);
        removeViewOnlyItems(event.getPlayer());

        ViewInvAdmin.cleanupAdminView(playerUUID);
    }

    private void removeViewOnlyItems(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isViewOnly(item)) {
                inv.setItem(i, null);
            }
        }

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isViewOnly(armor[i])) {
                armor[i] = null;
            }
        }
        inv.setArmorContents(armor);

        if (isViewOnly(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    private void cleanupViewOnlyItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeViewOnlyItems(player);
        }
    }

    private void handleItemDisplay(Player player, String originalMessage) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(RED + "You are not holding any item!");
            return;
        }

        String displayName = getCleanPlayerName(player);
        String prefix = getPlayerPrefix(player);

        String itemDisplayText = AQUA + "[" + getItemDisplayName(item) + "]";
        String modifiedMessage = originalMessage.replace("[i]", itemDisplayText);

        TextComponent itemComponent = new TextComponent(itemDisplayText);

        String hoverText = buildMinecraftTooltip(item);
        itemComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(hoverText)));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            TextComponent fullMessage = new TextComponent();

            if (!prefix.isEmpty()) {
                TextComponent prefixComponent = createColoredComponent(prefix);
                fullMessage.addExtra(prefixComponent);
            }

            TextComponent nameComponent = new TextComponent(displayName);
            nameComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            fullMessage.addExtra(nameComponent);

            TextComponent colonComponent = new TextComponent(": ");
            colonComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            fullMessage.addExtra(colonComponent);

            String beforeItem = modifiedMessage.substring(0, modifiedMessage.indexOf(itemDisplayText));
            if (!beforeItem.isEmpty()) {
                TextComponent beforeComponent = createColoredComponent(beforeItem);
                fullMessage.addExtra(beforeComponent);
            }

            fullMessage.addExtra(itemComponent);

            String afterItem = modifiedMessage.substring(modifiedMessage.indexOf(itemDisplayText) + itemDisplayText.length());
            if (!afterItem.isEmpty()) {
                TextComponent afterComponent = createColoredComponent(afterItem);
                fullMessage.addExtra(afterComponent);
            }

            onlinePlayer.spigot().sendMessage(fullMessage);
        }
    }


    private String buildMinecraftTooltip(ItemStack item) {
        if (!this.useMinecraftFormat) {
            return buildLegacyTooltip(item);
        }

        StringBuilder tooltip = new StringBuilder();

        String itemName = getItemDisplayName(item);
        boolean hasEnchants = !item.getEnchantments().isEmpty();
        boolean hasCustomName = item.hasItemMeta() && item.getItemMeta().hasDisplayName();

        if (hasCustomName) {
            tooltip.append("§b§l").append(itemName);
        } else if (hasEnchants) {
            tooltip.append("§b§l").append(formatItemName(item.getType().name()));
        } else {
            tooltip.append("§f§l").append(formatItemName(item.getType().name()));
        }

        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null) {
                for (String loreLine : lore) {
                    tooltip.append("\n§r").append(loreLine);
                }
            }
        }

        if ((item.getType() == Material.TIPPED_ARROW ||
                item.getType() == Material.POTION ||
                item.getType() == Material.SPLASH_POTION ||
                item.getType() == Material.LINGERING_POTION) && item.hasItemMeta()) {
            org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();

            boolean hasEffects = false;

            if (potionMeta.hasCustomEffects() && !potionMeta.getCustomEffects().isEmpty()) {
                for (org.bukkit.potion.PotionEffect effect : potionMeta.getCustomEffects()) {
                    tooltip.append("§9").append(formatPotionEffect(effect));
                    hasEffects = true;
                }
            }

            if (!hasEffects) {
                try {
                    PotionType potionType = potionMeta.getBasePotionType();
                    if (potionType != null && potionType != PotionType.WATER) {
                        tooltip.append("\n");
                        String effectText = formatPotionTypeWithDuration(potionType);
                        tooltip.append("§9").append(effectText);
                        hasEffects = true;
                    } else if (potionType == PotionType.WATER) {
                        tooltip.append("\n§5No Effects");
                        hasEffects = true;
                    }
                } catch (NoSuchMethodError | Exception legacyError) {
                    try {
                        org.bukkit.potion.PotionData data = potionMeta.getBasePotionData();
                        if (data != null && data.getType() != PotionType.WATER) {
                            tooltip.append("\n");
                            String effectText = formatPotionDataWithDuration(data);
                            tooltip.append("\n§9").append(effectText);
                            hasEffects = true;
                        } else if (data != null && data.getType() == PotionType.WATER) {
                            tooltip.append("\n§5No Effects");
                            hasEffects = true;
                        }
                    } catch (Exception e) {
                        if (debug) {
                            getLogger().warning("Error reading potion data: " + e.getMessage());
                        }
                    }
                }
            }

            if (!hasEffects) {
                tooltip.append("\n§5No Effects");
            }
            tooltip.append("\n");
        }

        if (item.getType() == Material.FIREWORK_ROCKET && item.hasItemMeta()) {
            org.bukkit.inventory.meta.FireworkMeta fireworkMeta = (org.bukkit.inventory.meta.FireworkMeta) item.getItemMeta();

            int power = fireworkMeta.getPower();

            if (power == 0) {
                tooltip.append("\n§7Flight Duration: §f1");
            } else {
                tooltip.append("\n§7Flight Duration: §f").append(power);
            }



            if (fireworkMeta.hasEffects()) {
                List<org.bukkit.FireworkEffect> effects = fireworkMeta.getEffects();
                if (effects != null && !effects.isEmpty()) {
                    for (org.bukkit.FireworkEffect effect : effects) {
                        if (effect != null) {
                            tooltip.append("\n§8").append(formatFireworkEffect(effect));
                        }
                    }
                }
            }
        } else if (item.getType() == Material.FIREWORK_ROCKET) {
            tooltip.append("\n§7Flight Duration: §f1");
        }

        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (!enchantments.isEmpty()) {
            List<Map.Entry<Enchantment, Integer>> sortedEnchants = new ArrayList<>(enchantments.entrySet());
            sortedEnchants.sort((a, b) -> {
                int aOrder = getEnchantmentOrder(a.getKey());
                int bOrder = getEnchantmentOrder(b.getKey());
                return Integer.compare(aOrder, bOrder);
            });

            for (Map.Entry<Enchantment, Integer> entry : sortedEnchants) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();
                String enchantName = formatEnchantmentName(enchant);
                String colorCode = enchant.isCursed() ? "§c" : "§7";

                tooltip.append("\n").append(colorCode).append(enchantName);

                if (level > 1 || enchant.getMaxLevel() > 1) {
                    tooltip.append(" ").append(toRomanNumeral(level));
                }
            }
        }

        String attributeText = getItemAttributes(item);
        if (!attributeText.isEmpty()) {
            tooltip.append("\n").append(attributeText);
        }

        if (item.getType().getMaxDurability() > 0 && item.getDurability() > 0) {
            int currentDurability = item.getType().getMaxDurability() - item.getDurability();
            int maxDurability = item.getType().getMaxDurability();
            tooltip.append("\n§7Durability: §f").append(currentDurability).append(" / ").append(maxDurability);
        }

        String category = getItemCategory(item.getType());
        if (!category.isEmpty()) {
            tooltip.append("\n§9§o").append(category);
        }
        tooltip.append(" | §9§oMinecraft");

        if (showAdvancedTooltips) {
            tooltip.append("\n§8minecraft:").append(item.getType().name().toLowerCase());

//            int componentCount = countItemComponents(item);
//            tooltip.append("\n§8").append(componentCount).append(" component(s)");
        }

        return tooltip.toString();
    }

    private int getEnchantmentOrder(Enchantment enchant) {
        return enchantmentOrderCache.getOrDefault(enchant.getKey().getKey(), 999);
    }

    private String buildLegacyTooltip(ItemStack item) {
        StringBuilder hoverText = new StringBuilder();

        String itemName = getItemDisplayName(item);
        org.bukkit.ChatColor nameColor = getItemRarityColor(item);

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            hoverText.append(itemName);
        } else {
            hoverText.append(nameColor).append(formatItemName(itemName));
        }

        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                for (String loreLine : lore) {
                    if (loreLine != null && !loreLine.trim().isEmpty()) {
                        hoverText.append("\n").append(loreLine);
                    }
                }
            }
        }

        if (item.getAmount() > 1) {
            hoverText.append("\n").append(GRAY).append("Amount: ")
                    .append(WHITE).append(item.getAmount());
        }

        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (!enchantments.isEmpty()) {
            List<Map.Entry<Enchantment, Integer>> sortedEnchantments = new ArrayList<>(enchantments.entrySet());
            sortedEnchantments.sort((a, b) -> {
                boolean aIsCurse = a.getKey().isCursed();
                boolean bIsCurse = b.getKey().isCursed();

                if (aIsCurse != bIsCurse) {
                    return aIsCurse ? 1 : -1;
                }

                return a.getKey().getKey().getKey().compareTo(b.getKey().getKey().getKey());
            });

            for (Map.Entry<Enchantment, Integer> entry : sortedEnchantments) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();

                String enchantName = formatEnchantmentName(enchant);
                org.bukkit.ChatColor enchantColor = getEnchantmentColor(enchant, level);

                hoverText.append("\n").append(enchantColor).append(enchantName);
                if (level > 1 || enchant.getMaxLevel() > 1) {
                    hoverText.append(" ").append(toRomanNumeral(level));
                }
            }
        }

        if (item.getType().getMaxDurability() > 0) {
            int currentDurability = item.getType().getMaxDurability() - item.getDurability();
            int maxDurability = item.getType().getMaxDurability();

            if (item.getDurability() > 0) {
                double durabilityPercent = (double) currentDurability / maxDurability * 100;
                org.bukkit.ChatColor durabilityColor = getDurabilityColor(durabilityPercent);

                hoverText.append("\n").append(GRAY).append("Durability: ")
                        .append(durabilityColor).append(currentDurability).append("/").append(maxDurability)
                        .append(" (").append(String.format("%.1f", durabilityPercent)).append("%)");
            }
        }

        hoverText.append("\n\n").append(DARK_GRAY).append(ITALIC)
                .append("minecraft:"+item.getType().toString().toLowerCase());

        String description = getItemDescription(item);
        if (!description.isEmpty()) {
            hoverText.append("\n").append(DARK_GRAY).append(BOLD)
                    .append(description);
        }

        return hoverText.toString();
    }

    private String getItemCategory(Material material) {
        return categoryCache.getOrDefault(material, "Miscellaneous");
    }

    private String getItemDescription(ItemStack item) {
        Material type = item.getType();
        String materialName = type.name();

        if (type == Material.PLAYER_HEAD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (headOwnerKey != null && meta.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING)) {
                String headOwner = meta.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING);

                if (KANUUNANKUULA_NAME.equalsIgnoreCase(headOwner)) {
                    return "NOOOO, Not Kanuunankuula you monster, He's our Saviour, and person who coded these plugins";
                }

                return "A head which belongings to " + headOwner;
            }
        }

        if (itemDescriptions.containsKey(materialName)) {
            return itemDescriptions.get(materialName);
        }

        for (Map.Entry<String, String> entry : patternDescriptions.entrySet()) {
            if (materialName.contains(entry.getKey())) {
                if (entry.getKey().equals("SHULKER_BOX")) {
                    return entry.getValue();
                } else if (entry.getKey().equals("SPAWN_EGG")) {
                    String mobName = materialName.replace("_SPAWN_EGG", "").toLowerCase().replace("_", " ");
                    return entry.getValue().replace("{mob_name}", mobName);
                }
            }
        }

        return "";
    }

    private String toRomanNumeral(int number) {
        if (number <= 0) return "";
        if (number < ROMAN_NUMERALS.length) {
            return ROMAN_NUMERALS[number];
        }
        return String.valueOf(number);
    }

    public static String getCleanPlayerName(Player player) {
        return player.getName();
    }

    public static String getPlayerPrefix(Player player) {
        if (vaultChat == null) {
            return "";
        }

        try {
            String world = player.getWorld().getName();
            String prefix = vaultChat.getPlayerPrefix(world, player);

            if (prefix == null || prefix.isEmpty()) {
                prefix = vaultChat.getPlayerPrefix(player);
            }

            if (prefix != null && !prefix.isEmpty()) {
                if (!prefix.endsWith(" ")) {
                    prefix += " ";
                }
                return prefix;
            }

        } catch (Exception e) {
        }

        if (player.hasMetadata("prefix")) {
            String prefix = player.getMetadata("prefix").get(0).asString();
            return prefix.endsWith(" ") ? prefix : prefix + " ";
        }

        return "";
    }
}
